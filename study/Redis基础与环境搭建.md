# Redis 基础与环境搭建

本文记录 2026-10-24 引入 Redis 的背景、基础概念、实测命令和接入过程，作为后续 Redis 会话缓存、接口限流和 Stream 异步化的前置知识。

## 一、为什么这个项目要引入 Redis

引入 Redis 不是为了堆技术栈，而是当前实现有三个具体缺陷。

### 缺陷 1：简历分析是同步阻塞，而且卡在事务里

`server/src/main/java/com/interview/modules/resume/service/ResumeUploadService.java`：

```java
54:    @Transactional                                       // 整个方法开事务
55:    public ResumeUploadResponseDTO uploadAndSave(MultipartFile file) {
99:        ResumeEntity savedResumeEntity = resumeRepository.save(resumeEntity);
103:        ResumeAnalysisResultDTO resumeAnalysisResultDTO = resumeGradingService.analyzeResume(resumeText);
                                                          // ↑ LLM 调用，几秒到几十秒
106:        savedResumeEntity.setAnalyzeStatus(AsyncTaskStatus.COMPLETED);
```

| 问题 | 后果 |
| --- | --- |
| HTTP 请求同步等 LLM | 上传按钮转圈 10~30 秒，浏览器或网关可能超时 |
| LLM 调用在 `@Transactional` 内部 | 数据库事务长时间不释放，并发上传会占满连接池 |
| 进程中途崩溃 | 简历已入库但分析丢失，状态卡住，只能重新上传 |
| 失败只标 `FAILED` | 没有重试机制，也没有处理中的追踪记录 |

这违反 `AGENTS.md` 第八条：原则上不要在数据库事务内调用外部服务。

`AsyncTaskStatus` 枚举里已经有 `PENDING` 和 `PROCESSING`，说明设计意图本来就是异步，只是当前被同步实现顶替。

### 三个理由汇总

| 理由 | 解决什么 | Redis 能力 |
| --- | --- | --- |
| 把 LLM 调用移出请求线程和事务 | 请求阻塞、事务过长 | Stream 异步化 |
| 任务不丢且可重试 | 崩溃丢任务、失败无重试 | Stream 消费组 + pending 接管 |
| 限流保护 LLM 花费 | 接口可被刷，按 token 计费 | `INCR` + `EXPIRE` 原子计数 |

### 单机阶段的替代方案（诚实对比）

| 问题 | 不用 Redis 的方案 | 代价 |
| --- | --- | --- |
| LLM 阻塞请求 | `@Async` + 线程池 | 队列在内存，重启即丢 |
| 缓存 | Caffeine 本地缓存 | 多实例间不共享 |
| 限流 | Guava `RateLimiter` / Bucket4j 本地版 | 多实例各算各的，总量超标 |
| 分布式锁 | `synchronized` / `ReentrantLock` | 只在单 JVM 内有效 |

结论：不是没有 Redis 就跑不起来，而是上了 Redis 后异步可靠性、可扩展性和成本防护从“勉强可用”变成“工程上站得住”。会话缓存本身在单机小数据量下收益不明显，主要价值是为并发和多实例做准备。

## 二、Redis 是什么

一句话：把数据放在内存里的 key-value 服务器。

| 维度 | PostgreSQL | Redis |
| --- | --- | --- |
| 数据位置 | 磁盘（内存只有缓存页） | 内存 |
| 速度 | 毫秒级，受磁盘 I/O 影响 | 微秒到毫秒 |
| 查询方式 | SQL，可 JOIN、可聚合 | 无 SQL，只有 `GET`/`SET` 等命令 |
| 关系 | 外键、事务、约束 | 无关系 |
| 数据会丢吗 | 不会（WAL） | 会（可持久化，但通常当缓存） |
| 角色 | 唯一真相来源 | 加速层 / 中间件 |

### 三条关键性质

1. **能丢的数据才放 Redis**。放进 Redis 的东西必须能从 PostgreSQL 重建，或者本来就是短命的（限流计数、验证码）。
2. **单线程执行命令**。任何一条命令执行期间其他命令插不进来，所以 `INCR` 天生原子，限流不需要额外加锁。
3. **binary-safe**。Redis 只保管字节序列，不理解编码，所以能存图片、protobuf、压缩数据。

## 三、数据结构与本项目用途

| Redis 类型 | Java 类比 | 本项目用途 |
| --- | --- | --- |
| String | `String` | 缓存 JSON 快照、原子计数器、限流 |
| Hash | `Map<String, String>` | 会话字段的部分更新 |
| List | `List` / `Deque` | 简单队列（不适合正式任务队列） |
| Set | `Set<String>` | 去重，例如某题是否已提交 |
| ZSet | `TreeMap<score, member>` | 排行榜、按分数排序 |
| Stream | `BlockingQueue` + 消费组 | 异步任务队列（任务 18 使用） |

### Stream 相比 List 解决了什么

| 能力 | List | Stream |
| --- | --- | --- |
| 消费后保留历史 | 否，弹出即消失 | 是 |
| 处理确认（ACK） | 无 | 有，未 ACK 进入 pending |
| 多消费者分担 | 需自行实现 | 消费组自动分配 |
| 消费者崩溃后找回 | 任务永久丢失 | 其他消费者可 autoClaim |

参考项目 `infrastructure/redis/RedisService.java` 的 Stream 部分核心就是四件事：`streamAdd` 生产、`streamConsumeMessages` 阻塞读并先回收超时 pending、`streamAck` 确认、`createStreamGroup` 建组。阻塞读用 `BLOCK` 参数代替客户端轮询。

## 四、核心命令实验记录（2026-10-24 实测）

环境：`docker run -d --name redis -p 6379:6379 redis:7-alpine`，Redis 7.4.11。

进入交互式客户端：

```bash
docker exec -it redis redis-cli
```

实验命令与真实输出：

| 命令 | 输出 | 说明 |
| --- | --- | --- |
| `SET demo:name "张三"` | `OK` | 写入 |
| `GET demo:name` | `"\xe5\xbc\xa0\xe4\xb8\x89"` | 中文被转义显示，见第五节 |
| `INCR demo:count` | `(integer) 1` | 计数器从 0 开始 |
| `INCR demo:count` | `(integer) 2` | 累加，不是覆盖 |
| `EXPIRE demo:count 60` | `(integer) 1` | 设置过期 |
| `TTL demo:count` | `(integer) 57` | 每次查询都在减少 |
| `KEYS demo:*` | 列出匹配键 | 生产环境用 `SCAN` 替代 |
| `DEL demo:name` | `(integer) 1` | 返回删除的键数量 |
| `GET demo:name` | `(nil)` | 不存在的键返回空，不报错 |
| `STRLEN demo:name` | `(integer) 6` | 两个汉字各 3 字节 |

### 必须记住的返回值语义

| 命令 | 返回 | 说明 |
| --- | --- | --- |
| `DEL k` | 删除的键数 | `1` 存在并删除；`0` 本来就不存在 |
| `EXISTS k` | 存在的键数 | `1` / `0` |
| `STRLEN k` | 字节长度 | 不存在的键按空串处理，返回 `0` |
| `INCR k` | 自增后的值 | 不存在的键按 0 处理，返回 `1` |
| `EXPIRE k 60` | 是否设置成功 | `1` 成功；`0` 键不存在 |
| `TTL k` | 剩余秒数 | 正数 / `-1` / `-2` |

### TTL 的三个取值

| TTL 返回 | 含义 | 风险 |
| --- | --- | --- |
| `> 0` | 剩余秒数 | 正常 |
| `-1` | 键存在但没有过期时间 | 缓存忘记设 TTL，内存迟早被吃满 |
| `-2` | 键不存在 | 正常 |

`-1` 是缓存代码的自检信号：本该过期的缓存没有过期。

## 五、编码问题：中文为什么显示成 `\xe5\xbc\xa0`

容器 locale 是 `POSIX/C`，redis-cli 判断终端可能显示不了非 ASCII，于是把字节转义打印。实测：

```text
STRLEN demo:name → 6
hex              → E5 BC A0  E4 B8 89
                    张         三
```

存的是标准 UTF-8，只是显示被转义。在宿主执行 `docker exec redis redis-cli --raw GET demo:name` 就能看到完整的“张三”。

结论：Redis 存的是字节，编码由客户端决定。出现“乱码”时先问是谁在解码；Java 客户端双方都用 UTF-8 就不会有这个问题。

## 六、Spring Boot 接入（已完成部分）

### 参考项目做法（2026-10-24 读取 Gitee master）

| 项 | 内容 |
| --- | --- |
| 依赖 | `org.redisson:redisson-spring-boot-starter:4.0.0`（`app/build.gradle` 注释为 Boot 4 compatible） |
| 配置位置 | `spring.redis.redisson.config`，内联 Redisson 形式的 YAML |
| 分层 | `infrastructure/redis/RedisService.java`（通用封装） + `InterviewSessionCache.java`（业务缓存） |
| key 命名 | `interview:session:{sessionId}`、`interview:resume:{resumeId}` |
| TTL | 会话 24 小时 |
| 编码 | 显式 import `org.redisson.client.codec.StringCodec` |

### 当前项目实现（任务 16 阶段 0）

依赖（`server/pom.xml` 第 155~160 行）：

```xml
<!-- Redisson：Redis 客户端，提供分布式锁、原子计数、Stream 消费组 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>4.0.0</version>
</dependency>
```

版本必须显式声明，Redisson 不在 Spring Boot BOM 里。当前项目是 Spring Boot 4.0.1，参考项目是 4.1.0，实测兼容，无需退回到裸客户端手写 Bean。

配置（`server/src/main/resources/application.yml`）：

```yaml
spring:
  redis:
    redisson:
      config: |
        singleServerConfig:
          address: "redis://${REDIS_HOST:localhost}:${REDIS_PORT:6379}"
          database: ${REDIS_DATABASE:0}
          connectionMinimumIdleSize: 2
          connectionPoolSize: 16
```

`|` 后面的内容是 Redisson 自己的 YAML 格式，不是 Spring 配置；缩进错误不会报错，只会静默使用默认值。

环境变量（`server/.env` 与 `server/.env.example`）：`REDIS_HOST`、`REDIS_PORT`、`REDIS_DATABASE`。`.env` 已被 `.gitignore` 的 `**/.env` 忽略。

### 验证结果

`server/src/test/java/com/interview/infrastructure/redis/RedisConnectivityTest.java` 两个测试通过，日志证据：

```text
org.redisson.Version : Redisson 4.0.0
1 connections initialized for localhost/127.0.0.1:6379
2 connections initialized for localhost/127.0.0.1:6379
Started RedisConnectivityTest in 8.052 seconds
```

连接数是 **2** 而不是 Redisson 默认的 24，说明 `connectionMinimumIdleSize: 2` 真的被读到，可作为配置是否生效的免费验证点。

Redis 侧确认：

```text
KEYS *        → test:ping, test:ttl
TTL test:ping → 45
```

### Redisson 与 Spring Data Redis 的区别

| 维度 | Spring Data Redis（Lettuce） | Redisson |
| --- | --- | --- |
| 定位 | Spring 官方，`RedisTemplate` | 第三方，把 Redis 当分布式 Java 对象 |
| 基础 KV | 支持 | 支持 |
| 分布式锁 | 需自己写 Lua 保证原子性 | `RLock` 直接可用，带自动续期 |
| Stream 消费组 | 有，API 偏底层 | 封装更完整，带 autoClaim |
| 学习成本 | 低 | 中 |

选 Redisson 的原因是分布式锁和 Stream。注意 `redisson-spring-boot-starter` 会顺带引入 Spring Data Redis，导致启动时出现两段 INFO：

```text
Multiple Spring Data modules found, entering strict repository configuration mode
Spring Data Redis - Could not safely identify store assignment for repository candidate ... ResumeRepository
Finished Spring Data repository scanning in 12 ms. Found 0 Redis repository interfaces.
```

这只是 Spring 在判断仓库接口归 JPA 还是 Redis 管，结论是 0 个，属正常现象。如果觉得日志吵，可加 `spring.data.redis.repositories.enabled: false`。

## 七、踩坑记录

### 坑 1：Redisson 默认编码器存的是二进制，不是明文

测试通过后检查实际长度：

```text
STRLEN test:ping → 5     （"pong" 只有 4 个字符）
STRLEN test:ttl  → 10    （"temporary" 只有 9 个字符）
hex              → 03 70 6F 6E ...   ← 多出前缀字节 0x03
```

原因是 Redisson 默认的编码器是 `Kryo5Codec`，会先做二进制序列化再存。后果：

| 问题 | 后果 |
| --- | --- |
| redis-cli 里看不到内容 | 调试只能靠猜或写代码反序列化 |
| 其他语言无法读取 | 跨服务共享缓存不可行 |
| 更换编码器导致旧键作废 | 线上改 codec 只能等 TTL 自然过期 |

**决策（阶段 1 前必须定）**：跟参考项目一致，使用 `StringCodec`，序列化交给 Jackson，Redis 里存明文 JSON，便于 redis-cli 查看和跨语言读取。

### 坑 2：`@SpringBootTest` 依赖三个容器

一次测试失败的原因是 `Connection to localhost:5432 refused`，与 Redis 无关：`pgvector` 和 `rustfs` 容器在 48 分钟前被停掉。跑完整集成测试前先确认三个容器都在：

```bash
docker ps   # redis 6379 / pgvector 5432 / rustfs 9000
```

可以从启动日志判断具体是哪个依赖缺失；`RedissonAutoConfigurationV4 matched` 这行说明 Redisson 自动配置本身没有问题。

### 坑 3：redis-cli 的 Tab 补全会补出别的命令

输入 `GET` 后按 Tab 可能补成 `GETBIT`，报 `ERR wrong number of arguments for 'getbit' command`。补全后回车前先确认命令名。

## 八、下一步计划

| 阶段 | 内容 | 关键能力 | 验收 |
| --- | --- | --- | --- |
| 1. 缓存 | 面试会话或简历详情加缓存 | key 设计、TTL、缓存一致性 | 第二次请求命中缓存 |
| 2. 限流 | 简历上传按 IP 限流 | `INCR` + `EXPIRE` 原子计数 | 超限返回业务错误码 |
| 3. 异步 | 简历分析投递 Stream | 生产/消费、ACK、pending 接管 | 上传立即返回，状态流转 PENDING → PROCESSING → COMPLETED |

阶段 1 开工前需要先确定三件事：编码器（倾向 `StringCodec` + Jackson）、缓存对象、key 命名与 TTL。

## 九、命令速查

| 命令 | 用途 |
| --- | --- |
| `docker exec -it redis redis-cli` | 进入交互式客户端 |
| `docker exec redis redis-cli ping` | 连通性检查 |
| `docker exec redis redis-cli KEYS 'pattern*'` | 按模式查键 |
| `docker exec redis redis-cli --raw GET key` | 原样输出，可看中文 |
| `docker exec redis redis-cli TTL key` | 查看剩余过期时间 |
| `docker exec redis redis-cli FLUSHDB` | 清空当前库（谨慎） |
| `help <command>` / `help @string` | 客户端内查命令帮助 |
