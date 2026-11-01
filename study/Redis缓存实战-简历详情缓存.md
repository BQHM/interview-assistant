# Redis 缓存实战：简历详情缓存

## 这篇记录什么

2026-10-30 完成了第一个缓存切片：给 `GET /api/resumes/{id}` 加上 Redis 缓存。这篇文档记录**当时的取舍**、实现结构和验证证据，方便以后回看「为什么这么设计」。

范围严格限制在读接口，没有做写接口失效，也没有碰限流和 Stream。

## 一、为什么先做简历详情

四个候选对象，当时的对比：

| 对象 | 单次查询成本 | 变更频率 | 失效点 | 结论 |
| --- | --- | --- | --- | --- |
| 简历详情 | 2 次 SQL | 写完基本不变 | 1 个（阶段 3 分析状态变化） | 选它 |
| 简历分析结果 | 1 次 SQL | 不变 | 0 | 太简单，学不到东西 |
| 面试会话 | 2 次 SQL + JSON 反序列化 | 每答一题都变 | 4 个（暂存/提交/交卷/删除） | 下一刀 |
| 面试历史列表 | 分页 + 排序 | 高 | 多 | key 组合爆炸，不做 |

选简历详情的理由不是简单，而是**它只有一个失效点，且这个失效点正好落在阶段 3**。今天写好，异步化时能亲眼看到缓存如何因为业务变化而不安全。

## 二、三个设计决策

### 决策 1：编码器用 `StringCodec` + Jackson（有意偏离参考项目）

先看参考项目（提交 `ebe2697a`）的实际做法：

- `RedisService.set` / `get` / `getOrLoad` 调用 `redissonClient.getBucket(key)`，**不传 codec**，走默认 `Kryo5Codec`，存的是二进制。
- `StringCodec` 只在 Stream 相关方法里显式传入，因为 Stream 消息要求 `Map<String, String>`。
- `InterviewSessionCache` 把整个 `CachedSession` POJO 直接 `set` 进去（Kryo 序列化），只是 POJO 内部把题目列表转成了 JSON 字符串字段。

也就是说参考项目是「Kryo 存对象 + POJO 内嵌 JSON 字段」，不是全明文 JSON。

我们的选择是 `StringCodec` + 手动 Jackson，理由三条：

1. **可观测性**：`redis-cli --raw GET` 直接看到 JSON，排查时不用写临时反序列化代码。
2. **演进安全**：DTO 加字段、改字段名时，JSON 宽容（缺失即 null），Kryo 反序列化对类结构敏感。
3. **零新增依赖**：Jackson 已在项目里，注入的 `ObjectMapper` 已支持 `LocalDateTime`。

代价是每次读写多两行序列化代码，这部分封装在 `RedisService` 内部，业务层看不到。

> 这个偏离必须记住：半年后如果发现「和参考项目不一样」，原因是对调试可读性的需求高于序列化性能。

### 决策 2：key 格式 `resume:detail:{resumeId}`

`模块:对象:标识` 三段式。冒号在 Redis 里没有语法含义，但它是社区惯例：可视化工具会按冒号折叠成树形目录，按前缀做 `SCAN` / 批量清理也依赖它。

前缀常量 `KEY_PREFIX = "resume:detail:"` 末尾带冒号，避免拼接时漏写冒号产生 `resume:detail12` 这种「看着对、实际是另一批 key」的 bug。

### 决策 3：TTL 30 分钟

参考项目的会话缓存用 24 小时，因为会话是「用户正在答题」的临时态，丢了体验就断了。

简历详情不同：它是**数据库的投影**，丢了随时能重建，所以 TTL 应该短。30 分钟既能覆盖用户反复查看详情的行为，又能限制旧数据的存活窗口。

阶段 3 之后这个值可能还要再缩短，因为分析状态会在异步流程里变化。

## 三、实现结构

三层，职责单向依赖：

| 层 | 类 | 职责 | 不该出现的东西 |
| --- | --- | --- | --- |
| 业务编排 | `ResumeQueryService` | 调用缓存，不知道 Redis 存在 | `RBucket`、key 拼接 |
| 业务缓存 | `ResumeDetailCache` | key 命名、TTL、失效入口 | `getBucket`、Jackson |
| 通用封装 | `RedisService` | 纯读写 + 序列化 + 降级 | `resume` 这个词 |

### `RedisService` 提供 5 个方法

`set` / `get` / `delete` / `exists` / `getOrLoad`。

签名上刻意用 `String` 而不是泛型 `T`：

```java
public void set(String key, String value, Duration ttl)
```

因为参数类型是 `String`，调用方必须先 `writeValueAsString`，**「这里发生了序列化」这件事在代码里是可见的**。参考项目的泛型签名更灵活，但容易让人忘记背后存的是二进制。

### `getOrLoad` 的四个分支

| 分支 | 处理 | 原因 |
| --- | --- | --- |
| 命中且可解析 | 直接返回 | 正常路径 |
| 命中但解析失败 | 删 key，继续回源 | 缓存格式可能与新 DTO 不兼容，不能让坏缓存导致 500 |
| 未命中 | 调 `loader` 回源 | 正常路径 |
| 写缓存失败 | 只记 error 日志 | **缓存是加速层**，挂了要降级成「没缓存」，不是让接口失败 |

第 4 条是原则，写任何缓存代码都适用。

### 为什么参数是 `Supplier<T>` 而不是值

```java
public <T> T getOrLoad(String key, Duration ttl, Class<T> type, Supplier<T> loader)
```

`Supplier` 表示「一段还没执行的代码」。只有缓存未命中时才 `loader.get()`，所以：

- 命中时数据库查询**根本不会发生**
- 调用方无法「先查库再问缓存」——那样缓存就白加了

`Class<T>` 参数存在的原因是泛型擦除：运行时需要知道按哪个类解析 JSON。

参考项目这里用的是 `Function<String, T>`（把 key 也传给 loader），差别只在于 loader 能否拿到 key。我们的 lambda 已经捕获了 `resumeId`，所以用无参的 `Supplier` 更贴合。

## 四、验证证据（四条都要）

| 证据 | 命令 / 操作 | 实际结果 |
| --- | --- | --- |
| 日志命中 | 连续两次 `GET /api/resumes/12` | 第一次 `缓存未命中` + 两段 `Hibernate` SQL；第二次 `缓存命中` 且**零 SQL** |
| TTL 生效 | `docker exec redis redis-cli TTL resume:detail:12` | `1619`（30 分钟倒计时中，不是 -1） |
| 明文可读 | `docker exec redis redis-cli --raw GETRANGE resume:detail:12 0 200` | `{"analyses":[{"analyzedAt":"2026-07-06T22:14:16.058824",...` |
| 改库对比 | `UPDATE resumes SET original_filename='改名验证.pdf'` 后请求接口 | 仍返回**旧文件名**；`DEL` 缓存后返回**新文件名** |

第 4 条是最硬的证据：数据库里是 A，接口返回 B，删缓存后变成 A。只有这种「故意制造不一致」的验证，才能证明缓存真的在起作用。（`GETRANGE` 输出的末尾可能出现 `简历结�` 这类残缺字符，那是按字节截断切在多字节中文字符中间，不是数据损坏。）

## 五、已知问题与后续

| 问题 | 现状 | 计划 |
| --- | --- | --- |
| 缓存不一致 | 改库验证已暴露：DB 与缓存会短暂不同 | 阶段 3 分析状态变更时调用 `ResumeDetailCache.evict` |
| 缓存穿透 | 不存在的 resumeId 每次都回源数据库 | 当前数据量小、无公开接口，不做；后续可用空值缓存或布隆过滤器 |
| 命中率无监控 | 只有日志 | 后续接入 `INFO stats` 或 Actuator 指标 |
| 缓存击穿 | 热点 key 过期瞬间并发回源 | 当前无热点，不做；需要时加 `SETNX` 单飞 |
| `evict` 未接调用 | 方法已写好，无调用点 | 见阶段 3，届时加一行即可 |

## 六、与参考项目的差距

- 参考项目缓存的是**面试会话**，并且维护 `interview:resume:{resumeId}` → `sessionId` 的反向索引，用于「查找未完成会话」。我们目前只缓存单个对象，没有反向索引。
- 参考项目的会话缓存会在每次操作后刷新 TTL（滑动过期），我们的简历详情是固定 30 分钟。
- 参考项目有 `deleteByPattern` / `findKeysByPattern` 等批量能力，我们暂时只写了单 key 操作，等真正需要清理某类缓存时再加。

## 七、命令速查

```bash
# 看有哪些缓存 key
docker exec redis redis-cli KEYS 'resume:*'

# 看剩余存活时间（-1 无过期，-2 不存在）
docker exec redis redis-cli TTL resume:detail:12

# 看字节数
docker exec redis redis-cli STRLEN resume:detail:12

# 看前 200 字节内容（避免刷屏）
docker exec redis redis-cli --raw GETRANGE resume:detail:12 0 200

# 手动清除
docker exec redis redis-cli DEL resume:detail:12
```
