# 《智能面试助手》

基于 Spring Boot、Spring AI 和 React 的智能面试助手练手项目，当前聚焦简历分析与文字模拟面试两条主链路。

---

## 项目介绍

`interview-assistant` 是一个以 `interview-guide` 为参考实现、按教学式节奏逐步迭代的智能面试助手项目。

当前项目暂不追求一次性覆盖完整平台能力，而是优先把下面两条核心链路做扎实：

- 简历上传、解析、存储与分析
- 文字模拟面试的会话创建、答题推进、暂存答案与报告生成

和 `interview-guide` 相比，`interview-assistant` 当前仍处于分阶段演进阶段：

- 后端主链路已完成，前端核心页面已按 `interview-guide` 管理台式结构完成第一版
- 已实现 `resume` 和 `interview` 两个核心模块
- 简历模块已接入 AI 分析，并保留规则兜底
- 文字面试主链路已从 `questionsJson` 答案存储演进到独立答案表
- 当前已接入同步 AI 出题和 AI 单题答案评估，并保留规则兜底；AI 出题已完成 Skill 配置加载、分类配额、Persona/分类 Prompt 注入和 Skill 感知规则兜底

## 当前定位

当前重点是在简历模块和文字模拟面试模块的后端主链路已验收基础上，用前端页面对齐 `interview-guide` 的核心交互方式。

现阶段更偏向：

- 能清楚展示后端分层设计
- 能完整演示业务主链路
- 能支持 Postman 和浏览器页面两种联调与验证方式
- 能通过浏览器完成“简历 -> 面试 -> 报告”的端到端演示
- 能为后续 Redis 缓存、异步评估、知识库、日程和语音面试演进打基础

## 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
| ---- | ---- | ---- |
| Spring Boot | 4.0.1 | 应用框架 |
| Java | 21 | 开发语言 |
| Spring AI | 2.x | AI 集成框架 |
| PostgreSQL | 14+ | 关系型数据库 |
| Spring Data JPA | - | 数据访问层 |
| Apache Tika | - | 简历文本解析 |
| RustFS / S3 Compatible Storage | - | 对象存储 |
| Maven | 3.9+ | 构建工具 |

### 前端技术

| 技术 | 版本 | 说明 |
| ---- | ---- | ---- |
| React | 18.x | 前端 UI 框架 |
| TypeScript | - | 前端类型约束 |
| Vite | 5.x | 前端构建工具 |
| React Router | - | 页面路由 |
| Axios | - | 接口请求封装 |
| lucide-react | - | 图标库 |

## 当前可演示主链路

当前已通过浏览器验收的演示链路：

```text
简历管理
-> 上传 / 查看简历
-> 简历详情
-> 创建文字模拟面试
-> 逐题提交答案
-> 生成并查看面试报告
-> 面试记录中再次查看报告
```

默认本地访问地址：

```text
前端：http://localhost:5173/
后端：http://localhost:8080/
```

## 功能特性

### 简历模块

- 支持上传简历文件
- 支持文件类型与大小校验
- 支持基于文件内容哈希的重复检测
- 支持使用 Apache Tika 解析简历文本
- 支持将原始简历上传到 RustFS / S3 兼容存储
- 支持调用 AI 分析简历内容
- 支持 AI 失败时自动回退到规则版分析
- 支持简历列表、详情和分析结果查询

### 模拟面试模块

- 支持基于简历创建面试会话
- 支持自动复用同一简历最近一条未完成会话
- 支持优先使用 AI 根据简历生成面试题目
- 支持通过 `skillId` 指定面试方向，未指定时默认使用 `java-backend`
- 支持按照 `ALWAYS_ONE`、`CORE`、`NORMAL` 优先级分配分类题量
- 支持 AI 出题失败时按照当前 Skill 分类和配额回退到规则版题目
- 支持查询面试会话详情
- 支持获取当前应该展示的题目
- 支持暂存答案但不推进下一题
- 支持正式提交答案并推进到下一题
- 支持提前交卷
- 支持提交答案后生成单题评分、反馈、参考答案和关键点
- 支持生成面试报告，并聚合答案表中的评分、反馈、参考答案和关键点
- 支持按简历查询最近一条未完成面试会话

## 当前开发进度

### 已完成

- [x] 简历上传
- [x] 简历解析
- [x] 简历去重
- [x] 简历对象存储上传
- [x] AI 简历分析 + 规则兜底
- [x] 简历列表 / 详情 / 分析结果查询
- [x] 创建文字模拟面试会话
- [x] 自动复用未完成面试会话
- [x] 当前题查询
- [x] 暂存答案
- [x] 提交答案并推进进度
- [x] 提前交卷
- [x] 面试历史列表
- [x] 面试历史详情
- [x] 删除面试会话
- [x] 独立答案表
- [x] 历史详情 / 当前会话 / 报告聚合答案表
- [x] 规则版面试报告生成
- [x] 面试核心服务拆分为会话、历史、报告服务
- [x] 抽取题目与答案聚合工具
- [x] 最小 JUnit 单元测试
- [x] 同步 AI 出题服务 + 规则出题兜底
- [x] Skill 资源加载、Persona 注入和分类优先级配额计算
- [x] AI Prompt 注入 Skill 分类目标题量
- [x] 规则出题兜底按 Skill 分类和配额生成题目
- [x] Skill 加载、题量分配、Prompt 注入和规则兜底单元测试
- [x] AI 单题答案评估 DTO、Prompt 和 Service
- [x] 手工验证 AI 出题、提交答案、评估入库、报告读取评估结果的完整链路
- [x] 提交答案后同步调用 AI/规则单题评估并写回答案表
- [x] 面试报告优先读取答案表中的评分和反馈
- [x] 面试报告展示参考答案和关键点列表
- [x] 前端工程初始化与参考项目结构对齐
- [x] 前端简历管理、上传简历、简历详情页面
- [x] 前端面试中心、文字答题、面试记录、面试报告页面
- [x] 面试记录页根据状态区分“继续面试”和“查看报告”
- [x] 简历详情页展示完整 AI 分析结果
- [x] 前后端主链路浏览器验收通过
- [x] 会话持久化 `skillId`，按 `resumeId + skillId + status` 复用未完成会话
- [x] 两个 Skill 前端可选择：`java-backend` 和 `system-design`
- [x] 双方向端到端验收：创建会话、题目分配、逐题答题和报告生成
- [x] 历史题目查询、`topicSummary` 摘要、精确去重和 Prompt 注入
- [x] AI 结构化输出重试：出题、简历分析、单题评估三处统一接入 `StructuredOutputInvoker`
- [x] 全项目变量名去掉匈牙利前缀，统一标准 Java 命名
- [x] 前端后端枚举中文化：简历状态、面试状态、题目分类统一走 `src/constants/`
- [x] 抽取 `ResumeStatusBadge` 状态徽章组件，文案与配色分离
- [x] 前端时间与文件大小格式化统一走 `src/utils/format.ts`
- [x] Redis / Redisson 基础设施接入：依赖、配置、连通性测试通过（Redis 7.4.11 + Redisson 4.0.0）
- [x] 简历详情接口接入 Redis 缓存：`StringCodec` + Jackson 明文 JSON，TTL 30 分钟，二次请求零 SQL
- [x] 接口限流：`@RateLimit` 注解 + AOP 切面 + Lua 脚本滑动窗口（`ZSET` 存时间戳），key 按“方法 + 维度 + 客户端”生成，超限返回 8001
- [x] 数据库迁移基线：Flyway 接管表结构变更，`V1__init_schema.sql` 记录基线结构，存量库通过 baseline 平滑接入，`ddl-auto` 改为 `validate`
- [x] LLM Provider 多模型抽象：`LlmProviderProperties` + `LlmProviderRegistry` 配置驱动，出题与单题评估按名字获取 `ChatClient`
- [x] 创建面试支持 `provider` 参数，未知 Provider 返回 7007 并列出可用模型，不静默回退默认值
- [x] `interview_sessions.llm_provider` 落库，创建、会话查询、历史详情三处接口回显本场使用的模型
- [x] 单题评估使用会话记录的 Provider，避免改默认模型后同一场面试被两个模型评分
- [x] 关闭 Spring AI OpenAI 自动配置，`ChatClient` 只由注册表创建；显式设置连接/读取超时（10s/120s）

### 进行中

- [ ] 面试会话缓存（失效点 4 个；建议等异步化阶段再评估是否值得做）
- [ ] 补强核心 Service 单元测试
- [ ] 历史题目去重专项测试和双方向人工复验

### 未完成

- [ ] 异步任务流转（Redis Stream）
- [ ] 知识库 / RAG 问答
- [ ] 面试日程管理
- [ ] 语音面试
- [ ] PDF 报告导出

## 当前实现的关键设计点

### 简历模块

- 使用文件内容哈希做去重，而不是只比较文件名
- 简历分析优先走 AI，失败时自动降级到规则版分析
- 分析结果持久化后，可直接通过查询接口返回前端

### 面试模块

- 一场面试对应一条 `InterviewSessionEntity`
- `currentQuestionIndex` 表示下一道待答题的索引，而不是最近一道已答题索引
- 创建面试时按 `resumeId + skillId + status` 复用最近一条未完成会话，不同 Skill 可以创建独立会话
- 创建面试时优先调用 `InterviewQuestionService`，根据 Skill Persona、分类和题量配额进行 AI 出题
- 创建新面试时读取同一简历、同一 Skill 最近 10 场会话的题目快照，使用 `topicSummary` 作为历史考点上下文
- 历史题目按完整题面精确去重，最多保留 60 条摘要，并按题型分组后注入 AI Prompt
- AI 不可用或输出异常时，按照同一 Skill 分类配额生成规则兜底题
- `questionsJson` 现在只承担题目快照职责，不再作为用户答案的唯一存储位置
- `saveAnswer` 只更新指定题目的草稿答案，不推进 `currentQuestionIndex`
- `submitAnswer` 要求按顺序作答，并在成功后推进到下一题
- 提交答案后会同步调用 `InterviewAnswerEvaluationService` 生成单题评分、反馈、参考答案和关键点，并写回 `interview_answers`
- 面试报告优先读取 `interview_answers.score`、`feedback`、`referenceAnswer` 和 `keyPointsJson`，不再按答案长度现场评分
- 提前交卷会将状态改为 `COMPLETED`
- 只有 `COMPLETED` 状态才允许生成报告

### 数据库迁移（Flyway）

表结构变更由 Flyway 管理，脚本位于 `server/src/main/resources/db/migration`，启动时按版本号自动执行并记录到 `flyway_schema_history`。

核心约定：

- `ddl-auto` 固定为 `validate`，Hibernate 只校验实体与表结构是否一致，不再建表或改表。
- 改表先写脚本再改实体：新增 `V<版本号>__<英文描述>.sql`，版本号递增，描述用小写下划线。
- 已执行过的脚本只读：Flyway 会校验 checksum，修改它会直接导致启动失败；修正错误要新增更高版本的脚本。
- 禁止用 `psql` 直接执行迁移脚本：Flyway 无法记录执行历史，会导致账本与实际状态错位。
- 迁移脚本默认不写数据，但“新增列后回填历史数据”这类一次性修正是合理的，应当与结构变更写在同一个脚本内。
- 迁移脚本只负责表结构，数据安全依赖备份（`pg_dump`），两者职责不重叠。

存量库接入方式：当前数据库在引入 Flyway 前已有 4 张表，因此配置了 `baseline-on-migrate: true` 与 `baseline-version: 1`。首次启动会写入一条 `type=BASELINE` 记录并跳过 `V1`，从 `V2` 开始纳管；全新空库不会触发 baseline，会正常执行 `V1` 建表。

常用检查命令：

```bash
# 查看迁移历史
docker exec pgvector psql -U postgres -d postgres -c "SELECT installed_rank, version, description, type, success FROM flyway_schema_history ORDER BY installed_rank;"

# 查看当前库中的表
docker exec pgvector psql -U postgres -d postgres -c "\dt"

# 导出当前表结构（用于核对或重建 V1 素材）
docker exec pgvector pg_dump -U postgres -d postgres --schema-only
```

新增迁移的完整流程：

1. 在 `db/migration` 下新建 `V<下一个版本号>__<描述>.sql`。
2. 在脚本中写出变更 SQL，必要时加 `IF EXISTS` 保证可重放。
3. 同步修改实体，保持 `validate` 能通过。
4. 启动应用，日志确认出现 `Migrating schema ... to version ...`。
5. 查 `flyway_schema_history`，确认为新版本且 `success = t`。

### 多 Provider 模型配置（LLM Provider）

AI 调用不再直接注入单个 `ChatClient`，而是统一经过 `LlmProviderRegistry`。出题时用请求指定的模型，单题评估时用会话里记录的模型，两者都以 `interview_sessions.llm_provider` 为准。

配置位于 `application.yml` 的 `app.ai.llm`：

```yaml
app:
  ai:
    llm:
      default-provider: qwen          # 不指定 Provider 时使用的缺省值，必须是 providers 里的 key
      connect-timeout-seconds: 10
      read-timeout-seconds: 120
      providers:
        qwen:
          base-url: ${AI_BASE_URL}
          api-key: ${AI_API_KEY}
          model: ${AI_MODEL}
          temperature: 0.2
        qwen-plus:
          base-url: ${AI_BASE_URL}
          api-key: ${AI_API_KEY}
          model: ${AI_MODEL}
          temperature: 0.2
```

核心约定：

- `base-url` 不要带 `/v1`：Spring AI 的 `OpenAiApi` 默认补全 `/v1/chat/completions`，注册表会额外剥掉误写的结尾 `/v1`，避免出现 `/v1/v1`。
- 启动即校验：没有配置任何 Provider，或 `default-provider` 不在 `providers` 里，应用直接启动失败，而不是等第一次调用才暴露。
- `ChatClient` 懒加载 + 缓存：首次按名字调用时创建，之后复用同一实例，重复创建的成本不会落在每次请求上。
- 未知 Provider fail-fast：请求里写了不存在的名字，返回 7007 并列出可用模型，绝不静默回退默认值——静默回退会让用户拿 A 模型的结果误判 B 模型的能力。
- 超时必须显式设置：手工构建 `OpenAiApi` 不会继承 Spring Boot 的 HTTP 设置，默认只有约 10 秒读超时，简历分析这类长响应会被打断。当前使用 JDK `HttpClient` + `JdkClientHttpRequestFactory`，连接 10 秒、读取 120 秒，均为配置项。
- 关闭 Spring AI 自动配置：`spring.ai.model.chat` 与 `spring.ai.model.embedding` 都设为 `none`，避免框架按 `spring.ai.openai.*` 再建一套用不上的客户端。

模型选择链路：

```text
创建面试 请求 provider（可选）→ 校验存在性 → 落库 llm_provider
                                              ↓
出题        LlmProviderRegistry.getChatClient(providerId)
评估         LlmProviderRegistry.getChatClient(会话里的 llm_provider)
```

> 当前是配置驱动（A 期）。B 期会把 `providers` 搬到 `llm_providers` 表、API Key 加密存储并增加管理页面，届时只有 `LlmProviderProperties` 的数据来源改变，出题和评估代码不受影响。

## 项目结构

```text
interview-assistant/
├── README.md
├── docs/                                   # 设计文档、实施计划、完成度审计
├── study/                                  # 学习记录
├── server/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/interview/
│       │   ├── App.java
│       │   ├── common/                     # 注解、配置、常量、异常、统一响应
│       │   │   ├── annotation/
│       │   │   ├── config/
│       │   │   ├── constant/
│       │   │   ├── exception/
│       │   │   ├── model/
│       │   │   └── result/
│       │   ├── infrastructure/             # 文件解析、对象存储、AI 与缓存基础设施
│       │   │   ├── ai/
│       │   │   ├── file/
│       │   │   └── redis/
│       │   └── modules/
│       │       ├── resume/
│       │       │   ├── model/
│       │       │   ├── repository/
│       │       │   ├── service/
│       │       │   └── ResumeController.java
│       │       └── interview/
│       │           ├── model/
│       │           ├── repository/
│       │           ├── service/
│       │           ├── skill/              # Skill 元数据与资源加载
│       │           └── InterviewController.java
│       └── resources/
│           ├── application.yml
│           ├── prompts/                    # AI Prompt 模板
│           └── skills/                     # Skill 配置（java-backend / system-design）
└── frontend/
    ├── package.json
    └── src/
        ├── api/                            # 统一请求层与接口封装
        ├── assets/
        ├── components/                     # 通用组件（Layout、状态徽章等）
        ├── constants/                      # 枚举文案与路由常量
        ├── pages/                          # 页面
        ├── types/                          # 与后端 DTO 对齐的类型
        └── utils/                          # 格式化等纯函数
```

## 接口清单

### 简历模块

- `POST /api/resumes/upload`：上传简历
- `GET /api/resumes`：查询简历列表
- `GET /api/resumes/{id}`：查询简历详情
- `GET /api/resumes/{id}/analysis`：查询简历分析结果

### 面试模块

- `POST /api/interviews`：创建面试会话；若存在未完成会话则直接返回原会话；可选 `provider` 指定模型，不传用缺省值
- `GET /api/interviews/{sessionId}`：查询面试会话详情
- `GET /api/interviews/{sessionId}/question`：获取当前题目
- `PUT /api/interviews/{sessionId}/answers`：暂存答案但不推进下一题
- `POST /api/interviews/answer`：提交当前题答案
- `POST /api/interviews/{sessionId}/complete`：提前交卷
- `GET /api/interviews/{sessionId}/report`：生成面试报告
- `GET /api/interviews/unfinished/{resumeId}`：查询未完成面试会话
- `GET /api/interviews`：查询面试历史列表
- `GET /api/interviews/{sessionId}/details`：查询面试历史详情（含逐题答案与评估）
- `DELETE /api/interviews/{sessionId}`：删除面试会话

### Skill 模块

- `GET /api/interview/skills`：查询可选面试方向（`java-backend`、`system-design`）

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- Node.js 18+（前端构建与本地开发）
- PostgreSQL 17（推荐 `pgvector/pgvector:pg17` 镜像，后续知识库阶段需要向量扩展）
- RustFS 或其他兼容 S3 协议的对象存储
- Redis 7+（会话缓存、接口限流和异步任务）

本地依赖容器参考：

```bash
docker run -d --name pgvector -p 5432:5432 -e POSTGRES_PASSWORD=123456 pgvector/pgvector:pg17
docker run -d --name rustfs -p 9000:9000 rustfs/rustfs
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

跑完整集成测试（`@SpringBootTest`）前需要三个容器同时运行，否则会因为数据库或存储连接失败而中途退出。

### 配置说明

项目通过 `server/src/main/resources/application.yml` 和 `server/.env` 读取配置。

核心配置包括：

- PostgreSQL 连接信息
- Redis 连接信息（Redisson 单机配置与连接池大小）
- AI 模型地址和 API Key（`app.ai.llm.providers` 下可声明多个，`default-provider` 指定缺省项）
- RustFS / S3 对象存储配置
- 简历分析 Prompt 模板路径

常用环境变量示例：

```env
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=postgres
POSTGRES_USER=postgres
POSTGRES_PASSWORD=123456

AI_BASE_URL=http://your-model-host/v1
AI_API_KEY=your-api-key
AI_MODEL=qwen-plus

APP_STORAGE_ENDPOINT=http://localhost:9000
APP_STORAGE_ACCESS_KEY=your-access-key
APP_STORAGE_SECRET_KEY=your-secret-key
APP_STORAGE_BUCKET=interview-guide
APP_STORAGE_REGION=us-east-1

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_DATABASE=0
```

### 启动方式

进入后端目录后启动后端：

```bash
cd server
mvn spring-boot:run
```

如果当前终端没有配置 Maven，但 `target/` 下已有可执行 jar，可以先用 Java 21 直接启动：

```powershell
cd server
& 'C:\Users\Zh253\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2\bin\java.exe' -jar 'target\interview-assistant-1.0-SNAPSHOT.jar'
```

默认启动端口：

```text
http://localhost:8080
```

进入前端目录后启动前端：

```bash
cd frontend
npm run dev
```

默认前端启动端口：

```text
http://localhost:5173
```

## Postman 测试建议顺序

建议按下面顺序验证当前功能：

1. 上传简历：`POST /api/resumes/upload`
2. 查询简历列表：`GET /api/resumes`
3. 查询简历分析：`GET /api/resumes/{id}/analysis`
4. 创建面试：`POST /api/interviews`
5. 查询未完成面试：`GET /api/interviews/unfinished/{resumeId}`
6. 查询当前题目：`GET /api/interviews/{sessionId}/question`
7. 暂存答案：`PUT /api/interviews/{sessionId}/answers`
8. 提交答案：`POST /api/interviews/answer`
9. 提前交卷：`POST /api/interviews/{sessionId}/complete`
10. 生成报告：`GET /api/interviews/{sessionId}/report`

## 浏览器验收建议顺序

当前前端已经覆盖核心演示链路，建议按下面顺序做浏览器验收：

1. 打开 `http://localhost:5173/`。
2. 在简历管理页确认列表能加载。
3. 进入简历详情页，确认基础信息、简历正文、综合评分、五维评分、分析总结、简历亮点和改进建议可展示。
4. 从简历详情页或面试中心创建文字模拟面试。
5. 在文字面试页逐题提交答案。
6. 最后一题提交后进入面试报告页。
7. 确认报告页展示整体评价、题目数量、已答/未答数量、单题评分、评价反馈、参考答案和关键点。
8. 回到面试记录页，再次点击“查看报告”确认历史报告可访问。

## 后续演进建议

当前项目仍处于教学式迭代阶段，建议按下面顺序继续推进：

1. 为历史题目查询、摘要注入和去重补充专项单元测试与人工验收。
2. 补强 `ResumeGradingServiceTest` 等核心测试，继续完善前端空状态、错误提示和时间展示。
3. LLM Provider B 期：`llm_providers` / `llm_global_settings` 表、API Key 加密存储、管理接口与前端配置页、缓存热更新。
4. 降级可观测性：AI 失败降级到规则版时额外记录来源（如 `question_source`），避免 `llm_provider` 与真实生成方式不一致。
5. Redis 会话缓存与请求幂等。
6. Redis Stream 异步化简历分析和面试评估（同时解决“长事务里调 LLM”的问题）。
7. PDF 报告导出。
8. 知识库 / RAG。

当前参考项目 `interview-guide` 已进一步实现历史题目摘要去重、Redis 会话缓存、请求幂等、Redis Stream 异步评估、多维原子限流、Flyway 数据库迁移和前后端 CI。当前项目已完成同步调用链路、Redis 缓存与滑动窗口限流、Flyway 迁移基线、LLM Provider 配置驱动抽象，后续按“测试补强 -> Provider 数据库化 -> Redis/异步化”的顺序逐步靠齐。

## 参考项目

参考实现是远程仓库 [`SnailClimb/interview-guide`](https://gitee.com/SnailClimb/interview-guide)（默认分支 `master`），本地不保存其源码。后续扩展能力时，应通过远程仓库对照该项目的模块划分和演进顺序。
