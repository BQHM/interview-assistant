# Implementation Plan: interview-assistant 完整学习开发计划

## Overview

本计划用于指导 `interview-assistant` 从当前后端 MVP 演进到接近 `interview-guide` 的完整智能面试平台。计划采用垂直切片方式推进：每个任务都要能独立实现、独立验证，并让项目保持可运行。

## Architecture Decisions

- 当前继续保留 Maven 构建，不因为参考项目使用 Gradle 就迁移构建工具。
- 参考项目 `interview-guide` 作为架构和业务语义基线，但实现时适配当前包名 `com.interview` 和当前项目结构。
- 当前阶段先巩固后端，后端 API 稳定后再启动前端。
- 复杂能力按“同步可用 -> 工程化增强 -> 异步化/缓存化”演进，不直接跳到最终形态。
- 当前 `questionsJson` 只保留题目快照；用户答案已经拆到独立答案表。
- AI 出题先采用同步调用 + 规则兜底，后续再演进为 Skill 驱动、题目去重和缓存/异步策略。
- AI 面试评估先采用同步调用 + 规则兜底，链路跑通后再异步化。

## Phase 1: 工程基线整理

### Task 1: 整理 Git 工作区状态

**Description:** 明确当前工作区中哪些文件是有效变更、哪些是历史迁移残留、哪些应该忽略，避免后续学习开发时误提交旧路径或工具缓存。

**Acceptance criteria:**

- [ ] `git status` 中旧包路径迁移残留已确认处理策略。
- [ ] `.edp/` 是否需要忽略已明确。
- [ ] `.env`、密钥、构建产物不会被提交。

**Verification:**

- [ ] 运行 `git status --short`。
- [ ] 运行 `git diff --name-only`。
- [ ] 检查 `.gitignore` 是否覆盖本地环境文件。

**Dependencies:** None

**Files likely touched:**

- `.gitignore`
- Git index only, if cleaning staged/deleted state

**Estimated scope:** Small: 1-2 files

### Task 2: 确认本地构建和启动方式

**Description:** 确认 Maven、JDK 21、PostgreSQL、RustFS / S3 配置可用，形成稳定的本地启动方式。

**Acceptance criteria:**

- [ ] `server` 模块可以编译。
- [ ] 后端可以启动到 `http://localhost:8080`。
- [ ] 配置缺失时错误信息可理解。

**Verification:**

- [ ] Build succeeds: `cd server && mvn -DskipTests package`。
- [ ] Tests pass: `cd server && mvn test`。
- [ ] Manual check: 调用一个已有查询接口确认服务可访问。

**Dependencies:** Task 1

**Files likely touched:**

- `server/pom.xml`
- `server/src/main/resources/application.yml`
- `README.md`

**Estimated scope:** Small: 1-2 files

### Checkpoint: 工程基线

- [ ] 当前工作区状态干净或已解释。
- [ ] 后端能启动。
- [ ] 已明确环境依赖和启动步骤。

## Phase 2: 面试历史能力

### Task 3: 实现面试历史列表

**Description:** 增加轻量级面试历史列表接口，用于展示已有面试会话，不返回完整题目和答案大字段。

**Acceptance criteria:**

- [ ] 新增列表 DTO，包含 `sessionId`、`resumeId`、`totalQuestions`、`currentQuestionIndex`、`status`、`createdAt`。
- [ ] 新增按创建时间倒序查询面试会话的方法。
- [ ] 新增 `GET /api/interviews` 接口。

**Verification:**

- [ ] Build succeeds: `cd server && mvn -DskipTests package`。
- [ ] Manual check: 创建面试后调用 `GET /api/interviews` 能看到记录。
- [ ] Manual check: 列表响应不包含完整 `questionsJson`。

**Dependencies:** Phase 1

**Files likely touched:**

- `server/src/main/java/com/interview/modules/interview/InterviewController.java`
- `server/src/main/java/com/interview/modules/interview/service/InterviewSessionService.java`
- `server/src/main/java/com/interview/modules/interview/repository/InterviewSessionRepository.java`
- `server/src/main/java/com/interview/modules/interview/model/dto/SessionListItemDTO.java`

**Estimated scope:** Medium: 3-5 files

### Task 4: 实现面试历史详情

**Description:** 增加面试历史详情接口，展示会话基础信息、所有题目、用户答案和完成状态。当前实现已经从早期 `questionsJson` 聚合演进为题目快照 + 独立答案表聚合。

**Acceptance criteria:**

- [x] 新增详情 DTO，能表达题目和答案明细。
- [x] 新增 `GET /api/interviews/{sessionId}/details` 接口。
- [x] 已回答和未回答题目都能展示。

**Verification:**

- [ ] Build succeeds: `cd server && mvn -DskipTests package`。
- [ ] Manual check: 创建面试、提交部分答案后，详情能显示已答和未答。
- [ ] Manual check: 不存在的 `sessionId` 返回业务错误。

**Dependencies:** Task 3

**Files likely touched:**

- `server/src/main/java/com/interview/modules/interview/InterviewController.java`
- `server/src/main/java/com/interview/modules/interview/service/InterviewHistoryService.java`
- `server/src/main/java/com/interview/modules/interview/model/dto/InterviewDetailDTO.java`
- `server/src/main/java/com/interview/modules/interview/model/dto/InterviewAnswerDetailDTO.java`

**Estimated scope:** Medium: 3-5 files

### Task 5: 实现删除面试会话

**Description:** 增加删除面试会话接口，为历史列表提供管理能力。当前阶段删除会话即可；拆独立答案表后需要级联处理答案。

**Acceptance criteria:**

- [ ] 新增 `DELETE /api/interviews/{sessionId}` 接口。
- [ ] 删除不存在会话返回业务错误。
- [ ] 删除后历史列表不再返回该会话。

**Verification:**

- [ ] Build succeeds: `cd server && mvn -DskipTests package`。
- [ ] Manual check: 创建会话 -> 删除 -> 查询列表和详情。

**Dependencies:** Task 3

**Files likely touched:**

- `server/src/main/java/com/interview/modules/interview/InterviewController.java`
- `server/src/main/java/com/interview/modules/interview/service/InterviewSessionService.java`
- `server/src/main/java/com/interview/modules/interview/repository/InterviewSessionRepository.java`

**Estimated scope:** Small: 1-3 files

### Checkpoint: 面试历史

- [ ] 面试可以创建、查询列表、查询详情、删除。
- [ ] 项目构建通过。
- [ ] README 接口清单同步更新。

## Phase 3: 独立答案表

### Task 6: 设计并创建 InterviewAnswerEntity

**Description:** 参考 `interview-guide` 拆出独立答案表，为单题评分、详情聚合、PDF 导出和 AI 评估打基础。

**Acceptance criteria:**

- [ ] 新增 `InterviewAnswerEntity`。
- [ ] 增加 `session + questionIndex` 唯一约束。
- [ ] 新增 `InterviewAnswerRepository`。
- [ ] 保留 `questionsJson` 作为题目快照，不再把答案只依赖 JSON 存储。

**Verification:**

- [ ] Build succeeds: `cd server && mvn -DskipTests package`。
- [ ] Manual check: 启动后数据库生成答案表。
- [ ] Manual check: 重复提交同题答案不会产生多条重复记录。

**Dependencies:** Task 4

**Files likely touched:**

- `server/src/main/java/com/interview/modules/interview/model/entity/InterviewAnswerEntity.java`
- `server/src/main/java/com/interview/modules/interview/repository/InterviewAnswerRepository.java`
- `server/src/main/java/com/interview/modules/interview/model/entity/InterviewSessionEntity.java`

**Estimated scope:** Medium: 3 files

### Task 7: 用答案表改造暂存和提交

**Description:** 将 `saveAnswer` 和 `submitAnswer` 的答案写入逻辑改为 upsert 独立答案表，同时保持当前题推进语义不变。

**Acceptance criteria:**

- [ ] 暂存答案只保存答案，不推进 `currentQuestionIndex`。
- [ ] 提交答案保存答案并推进到下一题。
- [ ] 同一会话同一题多次保存/提交只更新一条答案记录。
- [ ] 完成状态下不允许继续保存或提交。

**Verification:**

- [ ] Tests pass: `cd server && mvn test`。
- [ ] Manual check: 暂存后详情能看到答案但当前题不变。
- [ ] Manual check: 提交后当前题推进。

**Dependencies:** Task 6

**Files likely touched:**

- `server/src/main/java/com/interview/modules/interview/service/InterviewSessionService.java`
- `server/src/main/java/com/interview/modules/interview/service/InterviewAnswerService.java`
- `server/src/main/java/com/interview/modules/interview/repository/InterviewAnswerRepository.java`
- `server/src/main/java/com/interview/modules/interview/model/entity/InterviewAnswerEntity.java`

**Estimated scope:** Medium: 4 files

### Task 8: 用答案表改造历史详情和报告

**Description:** 历史详情和规则版报告从答案表读取用户答案，题目仍从 `questionsJson` 快照读取，形成题目快照 + 答案记录的聚合模型。

**Acceptance criteria:**

- [ ] 历史详情展示所有题目，已答题合并答案表数据。
- [ ] 报告统计基于答案表计算已答和未答数量。
- [ ] 未答题不会导致空指针或错误报告。

**Verification:**

- [ ] Tests pass: `cd server && mvn test`。
- [ ] Manual check: 部分作答后提前交卷，报告统计正确。

**Dependencies:** Task 7

**Files likely touched:**

- `server/src/main/java/com/interview/modules/interview/service/InterviewHistoryService.java`
- `server/src/main/java/com/interview/modules/interview/service/InterviewSessionService.java`
- `server/src/main/java/com/interview/modules/interview/model/dto/InterviewReportDTO.java`
- `server/src/main/java/com/interview/modules/interview/model/dto/InterviewDetailDTO.java`

**Estimated scope:** Medium: 3-5 files

### Checkpoint: 答案模型

- [x] 答案已经独立持久化。
- [x] 暂存、提交、详情、报告行为一致。
- [x] 已抽取题目与答案聚合工具，避免多个 Service 重复写聚合逻辑。
- [x] 已创建 AI 单题评估 DTO、Prompt 和 Service。
- [x] 提交答案后写入 AI 单题评估结果。
- [x] 报告优先读取答案表中的 AI 评分和反馈。

## Phase 4: 测试体系

### Task 9: 为题目答案聚合工具补最小单元测试

**Description:** 已为 `InterviewQuestionAnswerAggregator` 编写一个最小 JUnit 测试，验证题目快照和答案表记录能按 `questionIndex` 聚合。

**Acceptance criteria:**

- [x] 测试位于 Maven 标准目录 `src/test/java`。
- [x] 能验证答案按 `questionIndex` 回填到对应题目。
- [x] 未匹配题目保持 `userAnswer == null`。

**Verification:**

- [x] Tests pass: `cd server && mvn -Dtest=InterviewQuestionAnswerAggregatorTest test` 或 IDE 运行该测试。

**Dependencies:** Task 8

**Files likely touched:**

- `server/src/test/java/com/interview/modules/interview/service/comm/InterviewQuestionAnswerAggregatorTest.java`
- `server/pom.xml`, if test dependencies need adjustment

**Estimated scope:** Small: 1-2 files

### Task 10: 为面试会话核心流程补单元测试

**Description:** 为创建会话、复用未完成会话、暂存、提交、提前交卷、报告生成补最小单元测试，建立重构安全网。

**Acceptance criteria:**

- [ ] 覆盖成功创建会话。
- [ ] 覆盖重复创建时复用未完成会话。
- [ ] 覆盖提交答案推进进度。
- [ ] 覆盖完成后不能继续提交。
- [ ] 覆盖报告只能在完成后生成。

**Verification:**

- [ ] Tests pass: `cd server && mvn test`。

**Dependencies:** Task 8

**Files likely touched:**

- `server/src/test/java/com/interview/modules/interview/service/InterviewSessionServiceTest.java`
- `server/pom.xml`, if test dependencies need adjustment

**Estimated scope:** Medium: 2 files

### Task 10: 为简历分析兜底补测试

**Description:** 确认 AI 分析失败时能回退到规则版分析，避免外部 AI 不可用时主流程不可用。

**Acceptance criteria:**

- [ ] AI 调用异常时返回规则版分析结果。
- [ ] 空简历文本返回业务错误。
- [ ] 规则版评分输出范围合理。

**Verification:**

- [ ] Tests pass: `cd server && mvn test`。

**Dependencies:** Task 9

**Files likely touched:**

- `server/src/test/java/com/interview/modules/resume/service/ResumeGradingServiceTest.java`

**Estimated scope:** Small: 1-2 files

### Checkpoint: 测试基线

- [ ] 面试模块核心流程有测试。
- [ ] 简历分析兜底有测试。
- [ ] 后续重构前先跑测试。

## Phase 5: AI 出题与评估

### Task 11: 引入 AI 出题服务

**Description:** 新增独立的面试出题服务，优先调用 AI 根据简历生成结构化题目，失败时回退到当前规则版出题。

**Acceptance criteria:**

- [x] 出题逻辑从 `InterviewSessionService` 中拆出。
- [x] AI 出题返回结构化题目列表。
- [x] AI 失败时使用规则版题目。
- [x] 题目数量符合请求参数。

**Verification:**

- [ ] Tests pass: `cd server && mvn test`。
- [ ] Manual check: 关闭或错误配置 AI 后仍能创建面试。
- [ ] Manual check: AI 配置可用时，创建面试返回的题目能结合简历内容。

**Dependencies:** Task 10

**Files likely touched:**

- `server/src/main/java/com/interview/modules/interview/service/InterviewQuestionService.java`
- `server/src/main/resources/prompts/interview-question-*.st`
- `server/src/main/java/com/interview/modules/interview/service/InterviewSessionService.java`

**Estimated scope:** Medium: 3-5 files

### Task 12: 实现同步版 AI 单题答案评估

**Description:** 基于独立答案表，对每道已提交答案进行 AI 评估，生成单题分数、反馈、参考答案和关键点；AI 失败时使用规则版评估结果兜底。

**Acceptance criteria:**

- [x] 新增 `InterviewAnswerEvaluationDTO`。
- [x] 新增面试答案评估 system/user prompt 模板。
- [x] 新增 `InterviewAnswerEvaluationService`。
- [x] 提交答案后调用评估服务。
- [x] 评估结果写回 `InterviewAnswerEntity.score`、`feedback`、`referenceAnswer`、`keyPointsJson`。
- [x] AI 失败时不影响提交答案主流程。
- [x] 面试报告优先读取答案表中的评分和反馈。
- [x] 面试报告返回参考答案和关键点列表。

**Verification:**

- [ ] Build succeeds: `cd server && mvn clean package -DskipTests`。
- [ ] Manual check: 提交答案后数据库 `interview_answers` 中出现评分、反馈、参考答案和关键点 JSON。
- [ ] Manual check: 错误配置 AI 后仍能提交答案，并写入规则版评估结果。
- [ ] Manual check: 报告接口返回单题 `referenceAnswer` 和 `keyPoints`。

**Dependencies:** Task 8

**Files likely touched:**

- `server/src/main/java/com/interview/modules/interview/service/InterviewAnswerEvaluationService.java`
- `server/src/main/java/com/interview/modules/interview/service/InterviewSessionService.java`
- `server/src/main/java/com/interview/modules/interview/model/dto/InterviewAnswerEvaluationDTO.java`
- `server/src/main/java/com/interview/modules/interview/model/entity/InterviewAnswerEntity.java`
- `server/src/main/resources/prompts/interview-answer-evaluation-system.st`
- `server/src/main/resources/prompts/interview-answer-evaluation-user.st`

**Estimated scope:** Medium: 4-6 files

### Checkpoint: AI 面试能力

- [x] AI 出题基础版可用，规则出题兜底可用。
- [x] 提交答案后可同步生成单题 AI 评估，规则评估兜底可用。
- [x] 报告优先读取答案表中的评估结果。
- [x] 报告接口展示参考答案和关键点列表。
- [ ] 完成 AI 出题与报告接口的手工验收。
- [ ] 可以开始 Skill 化和异步化。

## Phase 6: Redis、限流与异步任务

### Task 13: 接入 Redis / Redisson 基础设施

**Description:** 增加 Redis 配置和基础服务，为会话缓存、限流和异步任务做准备。

**Acceptance criteria:**

- [ ] 配置 Redis 连接属性。
- [ ] 应用启动时可以连接 Redis。
- [ ] Redis 不可用时错误可定位。

**Verification:**

- [ ] Build succeeds: `cd server && mvn -DskipTests package`。
- [ ] Manual check: 本地 Redis 启动后应用正常启动。

**Dependencies:** Task 12

**Files likely touched:**

- `server/pom.xml`
- `server/src/main/resources/application.yml`
- `server/src/main/java/com/interview/infrastructure/redis/RedisService.java`

**Estimated scope:** Medium: 3 files

### Task 14: 实现接口限流

**Description:** 参考 `interview-guide` 的 `@RateLimit`，为高成本接口增加限流能力，例如上传简历、创建面试、AI 查询。

**Acceptance criteria:**

- [ ] 新增 `@RateLimit` 注解。
- [ ] 新增 AOP 限流切面。
- [ ] 超出限制返回业务错误码。
- [ ] 高成本接口加上限流注解。

**Verification:**

- [ ] Tests pass: `cd server && mvn test`。
- [ ] Manual check: 连续请求超过限制时返回限流错误。

**Dependencies:** Task 13

**Files likely touched:**

- `server/src/main/java/com/interview/common/annotation/RateLimit.java`
- `server/src/main/java/com/interview/common/aspect/RateLimitAspect.java`
- `server/src/main/java/com/interview/common/exception/ErrorCode.java`
- Controller files requiring rate limit

**Estimated scope:** Medium: 4-5 files

### Task 15: 简历分析异步化

**Description:** 将上传简历和分析简历拆开，上传后返回任务状态，Redis Stream 消费者异步执行分析并更新状态。

**Acceptance criteria:**

- [ ] 上传接口不再阻塞等待 AI 分析完成。
- [ ] 简历状态包含 PENDING、PROCESSING、COMPLETED、FAILED。
- [ ] 分析失败最多重试 3 次。
- [ ] 查询接口能看到分析状态和错误信息。

**Verification:**

- [ ] Tests pass: `cd server && mvn test`。
- [ ] Manual check: 上传简历后状态从 PENDING 变为 COMPLETED 或 FAILED。

**Dependencies:** Task 13

**Files likely touched:**

- `server/src/main/java/com/interview/modules/resume/service/ResumeUploadService.java`
- `server/src/main/java/com/interview/modules/resume/listener/AnalyzeStreamProducer.java`
- `server/src/main/java/com/interview/modules/resume/listener/AnalyzeStreamConsumer.java`
- `server/src/main/java/com/interview/common/async/*`

**Estimated scope:** Large: 5-8 files

### Task 16: 面试评估异步化

**Description:** 将 AI 面试评估从同步接口改为异步任务，完成面试后提交评估任务，前端轮询或查询评估状态。

**Acceptance criteria:**

- [ ] 面试完成后可触发评估任务。
- [ ] 会话包含评估状态和错误信息。
- [ ] 评估完成后报告可查询。
- [ ] 失败任务有重试和最终失败状态。

**Verification:**

- [ ] Tests pass: `cd server && mvn test`。
- [ ] Manual check: 完成面试后评估状态最终变为 COMPLETED。

**Dependencies:** Task 15

**Files likely touched:**

- `server/src/main/java/com/interview/modules/interview/listener/EvaluateStreamProducer.java`
- `server/src/main/java/com/interview/modules/interview/listener/EvaluateStreamConsumer.java`
- `server/src/main/java/com/interview/modules/interview/model/entity/InterviewSessionEntity.java`
- `server/src/main/java/com/interview/modules/interview/service/AnswerEvaluationService.java`

**Estimated scope:** Large: 5-8 files

### Checkpoint: Redis 与异步

- [ ] Redis 基础设施可用。
- [ ] 限流可用。
- [ ] 简历分析异步化。
- [ ] 面试评估异步化。

## Phase 7: PDF 导出

### Task 17: 实现简历分析 PDF 导出

**Description:** 将简历分析结果导出为 PDF 文件，支持中文字体和浏览器下载。

**Acceptance criteria:**

- [ ] 新增导出服务。
- [ ] 新增 `GET /api/resumes/{id}/export`。
- [ ] 文件名和响应头正确。
- [ ] 中文显示正常。

**Verification:**

- [ ] Build succeeds: `cd server && mvn -DskipTests package`。
- [ ] Manual check: 浏览器或 Postman 下载 PDF 并打开。

**Dependencies:** Task 12

**Files likely touched:**

- `server/pom.xml`
- `server/src/main/java/com/interview/infrastructure/export/PdfExportService.java`
- `server/src/main/java/com/interview/modules/resume/ResumeController.java`

**Estimated scope:** Medium: 3-5 files

### Task 18: 实现面试报告 PDF 导出

**Description:** 将面试评估报告导出为 PDF 文件，内容包括整体评价、题目、答案、评分和建议。

**Acceptance criteria:**

- [ ] 新增 `GET /api/interviews/{sessionId}/export`。
- [ ] PDF 包含整体评价和题目明细。
- [ ] 未回答题目展示清晰。

**Verification:**

- [ ] Build succeeds: `cd server && mvn -DskipTests package`。
- [ ] Manual check: 完成面试后下载 PDF。

**Dependencies:** Task 17

**Files likely touched:**

- `server/src/main/java/com/interview/infrastructure/export/PdfExportService.java`
- `server/src/main/java/com/interview/modules/interview/InterviewController.java`
- `server/src/main/java/com/interview/modules/interview/service/InterviewHistoryService.java`

**Estimated scope:** Medium: 3-5 files

## Phase 8: 知识库 / RAG

### Task 19: 实现知识库文档上传和列表

**Description:** 新增知识库模块，先完成文档上传、元信息保存、列表、详情和下载，复用已有文件处理基础设施。

**Acceptance criteria:**

- [ ] 新增 knowledgebase 模块结构。
- [ ] 支持上传 PDF、DOCX、Markdown 或 TXT。
- [ ] 支持列表和详情。
- [ ] 支持下载原文件。

**Verification:**

- [ ] Build succeeds: `cd server && mvn -DskipTests package`。
- [ ] Manual check: 上传文档后列表可见并可下载。

**Dependencies:** Task 15

**Files likely touched:**

- `server/src/main/java/com/interview/modules/knowledgebase/*`
- `server/src/main/java/com/interview/infrastructure/file/*`

**Estimated scope:** Large: 5-8 files

### Task 20: 实现知识库向量化和查询

**Description:** 引入 pgvector 和 Embedding，将知识库文档切分、向量化并支持检索增强问答。

**Acceptance criteria:**

- [ ] 文档上传后能异步向量化。
- [ ] 查询时能检索相关片段。
- [ ] RAG 回答包含引用或来源信息。
- [ ] 支持基础流式响应或普通响应。

**Verification:**

- [ ] Tests pass: `cd server && mvn test`。
- [ ] Manual check: 上传知识文档后能根据内容问答。

**Dependencies:** Task 19

**Files likely touched:**

- `server/src/main/java/com/interview/modules/knowledgebase/service/*`
- `server/src/main/java/com/interview/modules/knowledgebase/model/*`
- `server/src/main/java/com/interview/modules/knowledgebase/repository/*`
- `server/src/main/resources/application.yml`

**Estimated scope:** Large: 8+ files; should be split further before implementation

## Phase 9: 前端

### Task 21: 初始化前端工程

**Description:** 添加 React + TypeScript + Vite 前端工程，并配置基础路由、API Client 和页面框架。

**Acceptance criteria:**

- [ ] `frontend` 可以本地启动。
- [ ] 配置 API 基础地址。
- [ ] 有基础布局和路由。

**Verification:**

- [ ] Build succeeds: `cd frontend && npm run build` 或对应包管理器命令。
- [ ] Manual check: 浏览器打开首页。

**Dependencies:** 后端核心 API 稳定后

**Files likely touched:**

- `frontend/*`

**Estimated scope:** Large: 8+ files; should be split further before implementation

### Task 22: 实现简历和文字面试页面

**Description:** 先实现最核心用户路径：上传简历、查看分析、创建面试、答题、查看报告和历史详情。

**Acceptance criteria:**

- [ ] 用户可以上传简历。
- [ ] 用户可以创建并完成文字面试。
- [ ] 用户可以查看面试历史和详情。
- [ ] API 错误有基本提示。

**Verification:**

- [ ] Build succeeds: `cd frontend && npm run build`。
- [ ] Manual check: 完成一条端到端流程。

**Dependencies:** Task 21

**Files likely touched:**

- `frontend/src/pages/*`
- `frontend/src/api/*`
- `frontend/src/types/*`
- `frontend/src/components/*`

**Estimated scope:** Large: 8+ files; should be split further before implementation

## Phase 10: 面试日程和语音面试

### Task 23: 实现面试日程管理

**Description:** 实现面试邀请解析、日程 CRUD、状态流转和列表/日历视图。

**Acceptance criteria:**

- [ ] 后端支持日程创建、查询、更新、删除。
- [ ] 支持 AI 或规则解析邀请文本。
- [ ] 支持状态流转。
- [ ] 前端有基础日历或列表视图。

**Verification:**

- [ ] 后端测试通过。
- [ ] 前端构建通过。
- [ ] Manual check: 创建并更新一条面试日程。

**Dependencies:** Phase 9

**Files likely touched:**

- `server/src/main/java/com/interview/modules/interviewschedule/*`
- `frontend/src/pages/*`

**Estimated scope:** Large: split into backend and frontend subtasks

### Task 24: 实现语音面试 MVP

**Description:** 在文字面试稳定后，实现语音面试的最小闭环：创建语音会话、WebSocket 通信、文本化消息记录、结束会话和评估。

**Acceptance criteria:**

- [ ] 可以创建语音面试会话。
- [ ] WebSocket 可以传输消息。
- [ ] 会话消息可查询。
- [ ] 结束后可以生成评估。

**Verification:**

- [ ] 后端测试通过。
- [ ] Manual check: 浏览器完成一次语音面试最小流程。

**Dependencies:** Phase 9, AI 评估能力

**Files likely touched:**

- `server/src/main/java/com/interview/modules/voiceinterview/*`
- `frontend/src/pages/*`

**Estimated scope:** Large: split into multiple milestones

## Risks and Mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| 一次性复制参考项目导致理解断层 | High | 每个能力先写当前阶段方案，再实现 |
| 没有测试就重构答案模型 | High | 先补核心流程测试，再拆表 |
| Redis 和异步任务过早引入 | Medium | 先完成同步 AI 出题/评估，再异步化 |
| 前端过早启动导致 API 频繁变动 | Medium | 后端核心 API 稳定后再启动前端 |
| AI 输出不稳定 | High | 结构化输出 + 重试 + 规则兜底 |
| 本地环境依赖复杂 | Medium | 使用 Docker Compose 统一 PostgreSQL、Redis、RustFS |

## Open Questions

- 是否要把当前接口路径逐步调整为和 `interview-guide` 完全一致，还是保持当前 `/api/interviews` 风格？
- 前端包管理器后续使用 npm、pnpm 还是和参考项目一致？
- 是否需要引入数据库迁移工具，还是继续学习阶段使用 JPA `ddl-auto: update`？
- AI Provider 是否只保留 OpenAI Compatible，还是尽早抽象多 Provider？
