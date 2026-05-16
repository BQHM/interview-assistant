# 学习路线图

## 学习目标

通过逐步实现 `interview-assistant`，掌握一个 Spring Boot + Spring AI 智能面试平台从 MVP 到完整项目的演进过程。学习重点不是“抄完 reference”，而是理解每个模块为什么这样拆、什么时候该引入复杂度、如何验证功能是否可靠。

## 总体学习路径

```text
后端 MVP 主链路
  -> 面试历史与答案模型
  -> 测试和工程质量
  -> AI 出题与 AI 评估
  -> Redis 缓存、限流、异步任务
  -> PDF 导出
  -> 知识库 / RAG
  -> 前端页面
  -> 面试日程
  -> 语音面试
  -> Docker 部署与上线准备
```

## Phase 0：环境与工程基线

**目标：** 确保项目能稳定启动、构建和验证。

**需要学习：**

- Maven 项目结构。
- Spring Boot 配置加载。
- `.env` 与 `application.yml` 的关系。
- PostgreSQL、RustFS / S3 的本地开发配置。
- Git 工作区状态管理。

**参考项目：**

- `interview-guide/README.md`
- `interview-guide/docker-compose.dev.yml`
- `interview-guide/app/src/main/resources/application.yml`

**当前项目关注文件：**

- `server/pom.xml`
- `server/src/main/resources/application.yml`
- `.gitignore`
- `AGENTS.md`

**验收标准：**

- 能在本地启动后端。
- 能连接 PostgreSQL。
- 能上传一份测试简历。
- `git status` 中没有误提交风险文件，例如 `.env`。

## Phase 1：简历模块巩固

**目标：** 理解简历上传、解析、存储、分析的完整链路。

**已经完成：**

- 文件上传。
- 文件校验。
- Tika 解析。
- 对象存储。
- hash 去重。
- AI 分析 + 规则兜底。
- 列表、详情、分析查询。

**需要补强：**

- 删除简历。
- 重新分析。
- PDF 导出。
- Redis Stream 异步分析。
- 失败重试。

**参考项目：**

- `interview-guide/app/src/main/java/interview/guide/modules/resume`
- `interview-guide/app/src/main/java/interview/guide/modules/resume/listener`
- `interview-guide/app/src/main/java/interview/guide/infrastructure/file`

**学习重点：**

- Controller 只做入口，复杂逻辑下沉 Service。
- 文件处理属于 infrastructure，不属于业务模块内部细节。
- AI 失败时必须有可解释的降级方案。
- 异步化之前先确保同步主链路正确。

## Phase 2：文字面试核心深化

**目标：** 把当前面试 MVP 发展成可查询历史、可展示详情、可扩展评估的模块。

**已经完成：**

- 创建会话。
- 规则版出题。
- 查询当前题。
- 暂存答案。
- 提交答案。
- 提前交卷。
- 规则版报告。
- 查询未完成会话。

**接下来要做：**

1. 面试历史列表。
2. 面试历史详情。
3. 删除面试会话。
4. 独立答案表。
5. 基于答案表重构暂存和提交。

**参考项目：**

- `interview-guide/app/src/main/java/interview/guide/modules/interview/InterviewController.java`
- `interview-guide/app/src/main/java/interview/guide/modules/interview/service/InterviewHistoryService.java`
- `interview-guide/app/src/main/java/interview/guide/modules/interview/model/InterviewAnswerEntity.java`
- `interview-guide/app/src/main/java/interview/guide/modules/interview/model/SessionListItemDTO.java`
- `interview-guide/app/src/main/java/interview/guide/modules/interview/model/InterviewDetailDTO.java`

**学习重点：**

- 为什么列表接口不能返回完整题目 JSON。
- 为什么历史详情要包含已答和未答题目。
- 为什么答案需要独立表。
- 为什么 `currentQuestionIndex` 应表示“下一道待答题”。

## Phase 3：测试与工程质量

**目标：** 让每次重构都有安全网。

**需要学习：**

- JUnit 5。
- Mockito。
- AssertJ。
- Spring Boot 测试配置。
- Service 单元测试和 Controller 集成测试的区别。

**优先测试对象：**

- `InterviewSessionService`
- `ResumeUploadService`
- `ResumeGradingService` 的规则兜底逻辑

**验收标准：**

- 创建会话、提交答案、提前交卷、生成报告都有测试。
- 非法状态和资源不存在都有测试。
- 每次改面试模块前后都能跑 `mvn test`。

## Phase 4：AI 出题与 AI 面试评估

**目标：** 从规则版面试升级到 AI 驱动面试。

**需要学习：**

- Spring AI ChatClient。
- Prompt 模板设计。
- 结构化输出转换。
- AI 失败降级。
- Skill 驱动出题。

**参考项目：**

- `interview-guide/app/src/main/java/interview/guide/modules/interview/skill`
- `interview-guide/app/src/main/java/interview/guide/modules/interview/service/InterviewQuestionService.java`
- `interview-guide/app/src/main/java/interview/guide/modules/interview/service/AnswerEvaluationService.java`

**当前阶段简化建议：**

- 先做同步 AI 出题。
- 再做同步 AI 评估。
- 结果稳定后再异步化。
- 保留规则版出题和规则版报告作为兜底。

## Phase 5：Redis 与异步任务

**目标：** 引入真实工程中的缓存、限流和异步任务流。

**需要学习：**

- Redisson 基础使用。
- Redis Stream。
- 消费组。
- 幂等处理。
- 失败重试和任务状态。
- 接口限流。

**参考项目：**

- `interview-guide/app/src/main/java/interview/guide/common/async`
- `interview-guide/app/src/main/java/interview/guide/infrastructure/redis`
- `interview-guide/app/src/main/java/interview/guide/common/annotation/RateLimit.java`

**推荐落地顺序：**

1. Redis 连接和基础配置。
2. 会话缓存。
3. 限流注解。
4. 简历分析异步化。
5. 面试评估异步化。
6. 知识库向量化异步化。

## Phase 6：PDF 导出

**目标：** 把简历分析和面试评估沉淀为可下载报告。

**需要学习：**

- iText PDF 生成。
- 中文字体处理。
- 文件下载响应头。
- 报告 DTO 与展示模型。

**参考项目：**

- `interview-guide/app/src/main/java/interview/guide/infrastructure/export`
- `interview-guide/app/src/main/java/interview/guide/modules/resume/ResumeController.java`
- `interview-guide/app/src/main/java/interview/guide/modules/interview/InterviewController.java`

## Phase 7：知识库与 RAG

**目标：** 实现知识库上传、向量化、检索增强问答和流式对话。

**需要学习：**

- 文档上传和分块。
- PostgreSQL pgvector。
- Embedding 模型。
- 向量检索。
- RAG Prompt。
- SSE 流式响应。

**参考项目：**

- `interview-guide/app/src/main/java/interview/guide/modules/knowledgebase`

**前置条件：**

- Redis 异步任务已经理解。
- 文件上传和解析链路已经熟悉。
- Spring AI 调用和结构化输出已经掌握。

## Phase 8：前端页面

**目标：** 为已稳定的后端 API 做可用的前端页面。

**需要学习：**

- React + TypeScript。
- Vite。
- Tailwind CSS。
- React Router。
- API Client 封装。
- 表单、上传、列表、详情页。

**推荐页面顺序：**

1. 简历上传页。
2. 简历历史页。
3. 简历分析详情页。
4. 面试中心页。
5. 文字面试页。
6. 面试历史页。
7. 面试详情页。
8. 知识库页面。
9. 日程页面。
10. 语音面试页面。

## Phase 9：面试日程

**目标：** 实现面试邀请解析、日历管理和状态流转。

**参考项目：**

- `interview-guide/app/src/main/java/interview/guide/modules/interviewschedule`
- `interview-guide/frontend/src/pages` 中面试日程相关页面

**学习重点：**

- 日期时间模型。
- 状态流转设计。
- AI 解析非结构化文本。
- 定时任务。

## Phase 10：语音面试

**目标：** 实现实时语音对话面试。

**参考项目：**

- `interview-guide/app/src/main/java/interview/guide/modules/voiceinterview`

**学习重点：**

- WebSocket。
- ASR/TTS。
- 实时会话状态。
- 音频流处理。
- 暂停/恢复和回声防护。

**建议：** 这是最后阶段，不要提前做。语音面试依赖 AI、面试评估、前端交互和工程稳定性。
