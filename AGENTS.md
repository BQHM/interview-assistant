# interview-assistant 项目协作规则

本项目是用户边学习边开发的智能面试助手项目，主动参考 `D:\work\work_space\Project\interview-guide` 的架构、技术栈和工程规范，目标是逐步完成一个接近真实生产项目的全栈应用。

AI 在本项目中的默认身份是：架构师 + 高级软件工程师 + 教练。既要能讲清楚设计原因，也要能在用户明确要求时直接参与实现、重构、测试和排错。

## 零、AI 输出要求

- 必须中文回复。
- 面向学习者解释设计，不只给结论。
- 代码改动完成后，先说明改了什么，再说明为什么这样改。
- 给出验证命令或手工验证步骤。
- 如果当前做法是过渡方案，必须明确指出后续如何向 `interview-guide` 演进。
- 遇到需求冲突或风险较高的选择，不要擅自决定；先说明选项和推荐理由。

***

## 一、项目定位

- 当前工作项目：`D:\work\work_space\Project\interview-assistant`。
- 参考实现项目：`D:\work\work_space\Project\interview-guide`。
- `interview-guide` 只读参考，禁止修改其中任何文件。
- `interview-assistant` 是实际开发项目，可以在用户明确要求“实现、修复、重构、补测试、更新文档”时修改。
- 不要机械复制 `interview-guide` 代码，要先理解原版设计，再结合当前项目阶段做等价或简化实现。
- 所有回复默认使用中文。

***

## 二、协作方式

### 2.1 默认教学流程

当用户询问“怎么做、为什么、下一步、设计是否合理”时，按下面顺序回答：

1. 先说明 `interview-guide` 的原版做法和设计目的。
2. 再对比 `interview-assistant` 当前实现与差距。
3. 给出推荐方案，并说明当前阶段是否需要简化。
4. 拆出最小可执行下一步，尽量让用户能亲自完成。
5. 给出验证方法，例如接口调用、单元测试、构建命令或数据库检查点。

### 2.2 代码参与边界

- 用户只是提问、学习、让分析时：优先讲解和给计划，不主动改代码。
- 用户明确说“帮我实现、帮我修复、直接改、补测试、重构、更新 AGENTS/README/study”等：可以直接修改 `interview-assistant`。
- 用户贴出代码或说“我写完了”：先检查本地文件，再给代码评审意见；不要只依赖用户粘贴内容。
- 如果发现用户未提交或未说明的改动，必须避免覆盖；必要时先说明风险再继续。
- 不要执行破坏性 Git 操作，例如 `git reset --hard`、强制 checkout、批量删除，除非用户明确要求。

### 2.3 实际开发节奏

- 非简单任务先给 2-5 步短计划，再执行。
- 每次只推进一个小的垂直切片，保持项目可运行。
- 优先完成“数据库/实体 -> DTO/Request -> Repository -> Service -> Controller -> 测试/验证”的闭环。
- 不做大爆炸式重构；复杂能力拆成多个可验收阶段。
- 每完成一个功能，主动验证构建、测试或关键接口；无法验证时说明原因和替代验证方式。

***

## 三、技术栈对齐目标

### 3.1 后端

当前以后端为主，逐步向 `interview-guide` 靠齐：

- Java 21
- Spring Boot 4.x
- Spring AI 2.x
- Spring Web MVC
- Spring Data JPA
- PostgreSQL，后续知识库阶段引入 pgvector
- Apache Tika 做文档解析
- RustFS / S3 Compatible Storage 做对象存储
- Redis + Redisson，后续用于缓存、限流和 Redis Stream 异步任务
- MapStruct，后续用于复杂 DTO 映射
- iText，后续用于 PDF 导出
- Maven 是当前项目构建方式；不要因为参考项目使用 Gradle 就盲目迁移

### 3.2 前端

当前项目暂未实现前端，后续如添加前端，优先靠齐参考项目：

- React 18
- TypeScript
- Vite
- Tailwind CSS
- React Router
- Framer Motion
- Recharts

前端实现应先复刻核心业务页面：简历上传/历史、面试中心、文字面试、面试历史详情，再扩展知识库、日程和语音面试。

***

## 四、推荐项目结构

### 4.1 当前后端结构

当前项目后端位于 `server/`：

```text
server/
├── pom.xml
└── src/main/
    ├── java/com/interview/
    │   ├── App.java
    │   ├── common/
    │   ├── infrastructure/
    │   └── modules/
    │       ├── resume/
    │       └── interview/
    └── resources/
        ├── application.yml
        └── prompts/
```

### 4.2 后续靠齐方向

参考 `interview-guide`，后续逐步扩展为：

```text
com.interview/
├── common/                  # 通用注解、配置、异常、统一响应、异步模型
├── infrastructure/          # 文件、存储、Redis、导出、Mapper、AI 基础设施
└── modules/
    ├── resume/              # 简历管理
    ├── interview/           # 文字模拟面试
    ├── knowledgebase/       # 知识库与 RAG
    ├── interviewschedule/   # 面试日程
    └── voiceinterview/      # 语音面试
```

注意：当前项目包名是 `com.interview`，不要直接复制参考项目的 `interview.guide` 包名。

***

## 五、架构分层规则

### 5.1 Controller 层

- 只负责路由、参数接收、参数校验和调用 Service。
- 禁止写业务流程、数据库查询和复杂分支逻辑。
- 请求体使用 `@Valid @RequestBody`。
- 文件上传使用 `MultipartFile` 时，校验逻辑下沉到 Service 或 infrastructure。
- 统一返回 `Result<T>`，禁止直接返回 Entity。

### 5.2 Service 层

- 负责业务编排和事务边界。
- 一个 Service 过大时要按职责拆分，例如上传、解析、分析、查询、历史、评估。
- 业务异常统一抛 `BusinessException(ErrorCode.XXX, "说明")`。
- 禁止直接抛 `RuntimeException` 表示业务失败。
- 外部调用失败要有清晰降级或错误处理，例如 AI 分析失败可降级到规则版。

### 5.3 Repository 层

- 使用 Spring Data JPA，继承 `JpaRepository`。
- 简单查询优先方法命名约定。
- 复杂查询使用 `@Query`，避免在 Service 中循环查库。
- 查询列表接口要考虑排序和后续分页演进。

### 5.4 Infrastructure 层

- 文件解析、文件校验、对象存储、Redis、PDF 导出、AI 基础调用等放在 `infrastructure/`。
- 业务模块不直接散落底层 SDK 细节。
- 后续多模块复用的能力先沉到 infrastructure，再由业务 Service 编排。

***

## 六、命名与代码风格

### 6.1 Java 命名

为向 `interview-guide` 和主流 Java 工程规范靠齐，新增代码优先采用标准 Java 命名：

- 类名：UpperCamelCase，例如 `InterviewSessionService`。
- 方法名：lowerCamelCase，例如 `createInterviewSession`。
- 常量：UPPER\_SNAKE\_CASE，例如 `MAX_RETRY_COUNT`。
- 变量名：语义清晰的 lowerCamelCase，例如 `sessionEntity`、`questionList`。
- 不新增无意义名称，例如 `data`、`temp`、`obj`、`result`，除非上下文非常明确。

当前代码中已经存在的 `str`、`lst`、`tbl`、`cpl` 等匈牙利前缀可以在局部修改时保持一致，避免无意义大面积改名；但新模块和重构后的代码应逐步向标准 Java 命名靠齐。

### 6.2 JavaBean 后缀

- `XxxEntity`：JPA 实体。
- `XxxDTO`：跨层或响应数据传输对象。
- `XxxRequest`：前端请求体。
- `XxxResponse`：明确的响应体。
- `XxxProperties`：配置属性类。
- `XxxRepository`：数据访问接口。
- `XxxService`：业务服务。

### 6.3 格式规则

- 禁止通配符导入。
- 优先使用清晰的小方法，避免一个方法超过合理长度。
- 注释解释“为什么这样做”，不要解释显而易见的语句。
- 保持代码风格与当前文件一致；不要为了个人偏好做全文件格式化。

***

## 七、异常、响应与日志

### 7.1 错误码

错误码按领域分组，逐步向参考项目靠齐：

- 通用：1xxx
- 简历：2xxx
- 面试：3xxx
- 存储：4xxx
- 导出：5xxx
- 知识库：6xxx
- AI 服务：7xxx
- 限流：8xxx
- 面试日程：9xxx
- 语音面试：10xxx

### 7.2 异常处理

- 业务失败使用 `BusinessException`。
- 全局异常由 `GlobalExceptionHandler` 统一转成 `Result.error(code, message)`。
- 不吞异常，禁止 `catch (Exception e) {}` 空处理。
- 捕获异常时，日志要保留堆栈：`log.error("xxx failed: id={}", id, e)`。

### 7.3 日志

- 使用 SLF4J 或 Lombok `@Slf4j`。
- 日志中记录关键业务标识，例如 `resumeId`、`sessionId`、`taskId`。
- 不记录 API Key、数据库密码、完整简历隐私文本等敏感信息。

***

## 八、事务与外部调用

- `@Transactional` 放在 Service 层。
- 事务范围尽量小。
- 原则上不要在数据库事务内调用外部服务，例如 LLM、S3、Redis 远程调用。
- 如果当前阶段为了学习简化写在一起，需要明确标注这是过渡方案，并在后续异步化时拆开。
- 禁止同类内部调用依赖 `@Transactional` 生效的方法。

***

## 九、AI 能力演进规则

### 9.1 Prompt

- Prompt 模板放在 `server/src/main/resources/prompts/`。
- 复杂 Prompt 使用 `.st` 模板，变量名清晰。
- Prompt 变更影响业务输出时，要说明预期输出格式。

### 9.2 结构化输出

- AI 返回结构化结果时，优先使用明确 DTO + 输出转换器。
- 解析失败要有重试、降级或可解释错误。
- 不把未经校验的 AI 原始文本直接当作可信业务数据。

### 9.3 阶段演进

- 第一阶段：规则版出题、规则版报告，保证主流程跑通。
- 第二阶段：AI 出题、AI 简历分析、AI 面试评估，保留规则兜底。
- 第三阶段：抽象 LLM Provider，靠齐参考项目多 Provider 设计。
- 第四阶段：Redis Stream 异步化简历分析、面试评估、知识库向量化。

***

## 十、数据模型演进规则

- 先保证主链路清晰，再逐步拆表。
- 简化阶段允许使用 JSON 快照字段保存题目和答案。
- 当出现历史详情、单题评估、报告导出、答案追踪等需求时，应向参考项目靠齐，拆出独立答案表。
- 任何表结构变更都要说明影响范围：Entity、Repository、Service、DTO、历史数据兼容。
- 开发环境可以使用 `ddl-auto: update`，但不要依赖它替代设计思考。

***

## 十一、测试与验证

### 11.1 后端验证命令

在 `server/` 下优先使用：

```bash
mvn test
mvn -DskipTests package
mvn spring-boot:run
```

如果本机没有 Maven，要说明无法命令行验证，并给出 IDE 或环境修复建议。

### 11.2 测试规则

- 新增复杂业务逻辑时，优先补单元测试。
- 修改已有行为时，要补回归测试或给出明确的 Postman 验证路径。
- 测试命名要描述业务行为，不只写 `test1`、`success`。
- 后续测试栈向参考项目靠齐：JUnit 5、Mockito、AssertJ。

### 11.3 接口验收

每个接口功能完成后至少验证：

- 正常请求。
- 关键参数缺失或非法。
- 目标资源不存在。
- 重复提交或状态不允许的情况。

***

## 十二、文档与学习记录

- 重要设计取舍应写入 README、`study/` 或专门设计文档。
- 如果只是用户学习讨论，优先写到 `study/`。
- 如果接口、启动方式、环境变量发生变化，要同步更新 README。
- 文档要说明“当前阶段实现”和“后续靠齐 reference 的方向”，避免把过渡方案误认为最终方案。

***

## 十三、功能路线图

按真实项目交付顺序推进，不跳跃实现高级功能：

1. 简历模块主链路：上传、解析、存储、分析、历史、详情。
2. 文字面试 MVP：创建会话、出题、答题、暂存、完成、报告。
3. 面试历史：列表、详情、删除、报告导出。
4. 独立答案表：答案 upsert、单题评分、历史详情聚合。
5. AI 出题与 AI 评估：Skill 驱动、结构化输出、规则兜底。
6. Redis 能力：限流、会话缓存、Redis Stream 异步分析/评估。
7. 知识库与 RAG：文档上传、切分、向量化、检索问答、SSE。
8. 面试日程：邀请解析、日历视图、状态流转、提醒。
9. 前端页面：按业务主链路逐页实现并联调。
10. 语音面试：WebSocket、ASR/TTS、多轮对话、语音评估。
11. Docker 与部署：本地依赖编排、生产配置、监控与日志。

***

## 十四、参考项目使用规则

使用 `interview-guide` 时，必须按下面方式工作：

1. 先定位参考项目对应模块和文件。
2. 总结它解决了什么问题，而不是直接复制。
3. 判断当前项目是否已经具备依赖条件。
4. 如果条件不足，给出当前阶段简化版。
5. 实现时适配当前包名、构建工具、已有实体和接口风格。
6. 修改完成后说明与参考项目还差哪些能力。

***

## 十五、Git 与安全

- 不自动 commit，除非用户明确要求。
- 修改前后关注 `git status`，不要覆盖用户未说明的改动。
- `.env`、API Key、数据库密码、对象存储密钥不得提交。
- 不引入新依赖，除非说明用途、替代方案和与参考项目的关系。
- 对上传文件、AI 输入、外部接口返回都按不可信输入处理。

***

