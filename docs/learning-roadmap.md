# 学习路线图

## 学习目标

通过逐步实现 `interview-assistant`，掌握一个 Spring Boot + Spring AI 智能面试平台从 MVP 到完整项目的演进过程。学习重点不是“抄完 reference”，而是理解每个模块为什么这样拆、什么时候该引入复杂度、如何验证功能是否可靠。

## 总体学习路径

```text
后端 MVP 主链路
  -> 面试历史与答案模型
  -> 最小测试和工程质量
  -> AI 单题答案评估
  -> AI 出题与报告评估聚合验收
  -> 前端核心页面等价复刻
  -> 前后端主链路联调（已完成第一版）
  -> 前端体验完善与核心测试补强
  -> Skill 出题与核心测试补强
  -> Redis 缓存、限流、异步任务
  -> PDF 导出
  -> 知识库 / RAG
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
- 查询未完成会话。
- 面试历史列表。
- 面试历史详情。
- 删除面试会话。
- 独立答案表。
- 历史详情、当前会话和报告从答案表聚合用户答案。
- 规则版报告。
- 同步 AI 出题 + 规则出题兜底。
- AI 单题答案评估 + 规则评估兜底。
- 报告展示评分、反馈、参考答案和关键点。
- 服务拆分：会话、历史、报告。
- 题目答案聚合工具。

**接下来要做：**

1. 将已验收通过的后端主链路接入前端页面。
2. 补强核心 Service 单元测试。
3. 将同步 AI 出题演进为 Skill 驱动出题。
4. 最后考虑 Redis Stream 异步化。

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

- 同步 AI 出题和同步 AI 评估已经作为基础版接入。
- 当前先完成手工验收和测试补强。
- 结果稳定后再做 Skill 化和异步化。
- 保留规则版出题和规则版报告作为兜底。

## Phase 5：前端页面

**目标：** 参考 `interview-guide` 的前端结构，先完成可展示的核心业务页面。

**需要学习：**

- React + TypeScript。
- Vite。
- React Router。
- Axios 请求封装。
- API、types、components、pages 分层。
- 前后端联调和错误提示。

**常见联调细节：**

- 本地开发时，前端 Vite 默认运行在 `http://localhost:5173`，后端 Spring Boot 默认运行在 `http://localhost:8080`，端口不同会触发浏览器跨域限制。
- `interview-guide` 的处理方式是双保险：前端在 `frontend/vite.config.ts` 中配置 Vite dev server proxy，把 `/api` 请求代理到 `http://localhost:8080`；后端也通过 `CorsConfig` + `CorsProperties` 显式允许本地前端来源。
- 当前 `interview-assistant` 前端 MVP 阶段优先采用 Vite proxy：前端 Axios 只请求相对路径 `/api/...`，例如 `/api/resumes`，由 Vite 转发到后端，避免浏览器跨域问题。
- 面试表达：前端开发环境用 Vite 启动在 5173 端口，后端 Spring Boot 在 8080 端口。为了避免跨域，在 `vite.config.ts` 配置 dev server proxy，将 `/api` 请求代理到 `http://localhost:8080`。前端 Axios 请求只写相对路径 `/api/resumes`，由 Vite 转发到后端。

**参考项目：**

- `interview-guide/frontend/src/App.tsx`
- `interview-guide/frontend/src/components/Layout.tsx`
- `interview-guide/frontend/src/api/request.ts`
- `interview-guide/frontend/src/pages/UploadPage.tsx`
- `interview-guide/frontend/src/pages/HistoryPage.tsx`
- `interview-guide/frontend/src/pages/InterviewHubPage.tsx`

**推荐页面顺序：**

1. Layout 左侧导航和右侧内容区。
2. 简历管理页。
3. 上传简历页。
4. 简历详情和分析结果页。
5. 面试中心页。
6. 文字面试答题页。
7. 面试记录页。
8. 面试报告详情页。

**当前状态：**

- 已完成 React + TypeScript + Vite 前端工程。
- 已完成 `api/`、`types/`、`components/`、`pages/` 分层。
- 已完成左侧导航 + 右侧内容区 Layout。
- 已完成简历管理、上传简历、简历详情、面试中心、文字面试、面试记录和面试报告页。
- 已完成浏览器主链路验收：简历管理 -> 创建面试 -> 答题 -> 查看报告 -> 面试记录再次查看报告。

**已完成补充：**

- 面试记录页已根据会话状态区分“继续面试”和“查看报告”。
- 简历详情页已展示完整 AI 分析结果，包括综合评分、五维评分、分析总结、简历亮点和改进建议。

**下一步：**

1. 补充更友好的错误提示和空状态。
2. 统一前端时间格式展示。
3. 补强核心 Service 单元测试。

**当前阶段原则：**

- 先复刻 `interview-guide` 的核心结构和交互路径。
- 暂不做大横幅首页、聊天窗口改造、知识库、日程、语音面试和差异化 UI。
- 项目完整后再考虑个人特色和差异化。

## Phase 6：Redis 与异步任务

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

## Phase 7：PDF 导出

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

## Phase 8：知识库与 RAG

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
