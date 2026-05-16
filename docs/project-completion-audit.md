# 项目完成度对比审计

## 审计日期

2026-05-16

## 参考基线

- 当前项目：`D:\work\work_space\Project\interview-assistant`
- 参考项目：`D:\work\work_space\Project\interview-guide`
- 对比方式：按模块、接口、数据模型、工程能力和学习价值进行对比。

## 总体结论

`interview-assistant` 当前已经完成后端 MVP 的两条核心主链路：简历处理和文字模拟面试。它不是完整平台，但已经具备继续演进的基础。

| 维度 | 当前完成度 | 判断 |
| --- | --- | --- |
| 按当前 README 定义的后端 MVP | 约 75% - 80% | 简历与文字面试主流程已具备，缺历史、答案表、测试和异步能力 |
| 按 `interview-guide` 完整平台 | 约 25% - 30% | 参考项目包含前端、Redis、RAG、日程、语音、PDF、Docker 等完整能力 |
| 工程学习价值 | 较高 | 当前阶段适合学习分层、JPA、文件处理、AI 调用和接口设计 |

## 当前已经开发的内容

### 1. 简历模块

当前项目已有文件集中在：

- `server/src/main/java/com/interview/modules/resume`
- `server/src/main/java/com/interview/infrastructure/file`
- `server/src/main/resources/prompts`

已完成能力：

| 能力 | 当前状态 | 说明 |
| --- | --- | --- |
| 简历上传 | 已完成 | `POST /api/resumes/upload` |
| 文件类型和大小校验 | 已完成 | 通过文件基础设施服务完成 |
| 内容哈希去重 | 已完成 | 基于文件内容 hash，优于文件名去重 |
| 文档解析 | 已完成 | 使用 Apache Tika 抽取文本 |
| 文本清洗 | 已完成 | 有独立基础设施服务 |
| 对象存储上传 | 已完成 | 接入 RustFS / S3 Compatible Storage |
| AI 简历分析 | 已完成第一版 | Spring AI + Prompt 模板 |
| 规则兜底分析 | 已完成 | AI 调用失败时可降级 |
| 简历列表 | 已完成 | `GET /api/resumes` |
| 简历详情 | 已完成 | 当前项目为 `GET /api/resumes/{id}` |
| 分析结果查询 | 已完成 | `GET /api/resumes/{id}/analysis` |

与参考项目差距：

| 参考项目能力 | 当前状态 | 后续建议 |
| --- | --- | --- |
| Redis Stream 异步分析 | 未完成 | 当前上传后同步分析，后续拆成任务流 |
| 分析失败重试 | 未完成 | 异步化时加入最多 3 次重试 |
| PDF 简历报告导出 | 未完成 | 后续引入 iText 导出 |
| 删除简历 | 未完成 | 做历史和资源管理时补充 |
| 重新分析 | 未完成 | AI 分析稳定后补充 |
| 健康检查接口 | 未完成 | 可作为后期工程完善项 |

## 2. 文字模拟面试模块

当前项目已有文件集中在：

- `server/src/main/java/com/interview/modules/interview`

已完成能力：

| 能力 | 当前状态 | 说明 |
| --- | --- | --- |
| 创建面试会话 | 已完成 | `POST /api/interviews` |
| 复用未完成会话 | 已完成 | 同一简历存在未完成会话时返回旧会话 |
| 规则版出题 | 已完成第一版 | 根据简历关键词生成题目 |
| 查询会话详情 | 已完成基础版 | `GET /api/interviews/{sessionId}` |
| 查询当前题 | 已完成 | `GET /api/interviews/{sessionId}/question` |
| 暂存答案 | 已完成 | `PUT /api/interviews/{sessionId}/answers` |
| 提交答案并推进 | 已完成 | 当前接口为 `POST /api/interviews/answer` |
| 提前交卷 | 已完成 | `POST /api/interviews/{sessionId}/complete` |
| 规则版报告 | 已完成第一版 | `GET /api/interviews/{sessionId}/report` |
| 查询未完成会话 | 已完成 | `GET /api/interviews/unfinished/{resumeId}` |

与参考项目差距：

| 参考项目能力 | 当前状态 | 后续建议 |
| --- | --- | --- |
| 面试历史列表 | 未完成 | 下一步优先做 |
| 面试历史详情 | 未完成 | 列表后做，先基于 `questionsJson` 聚合 |
| 删除面试会话 | 未完成 | 历史列表稳定后补 |
| 独立答案表 | 未完成 | 历史详情后做，避免过早重构 |
| Skill 驱动出题 | 未完成 | AI 出题阶段引入 |
| 历史题目去重 | 未完成 | Skill 出题后加入 |
| 多轮追问 | 未完成 | AI 出题和评估稳定后再做 |
| AI 面试评估 | 未完成 | 先同步版，再 Redis Stream 异步版 |
| PDF 面试报告导出 | 未完成 | AI 评估报告稳定后做 |

## 3. 通用工程能力

已完成能力：

| 能力 | 当前状态 | 说明 |
| --- | --- | --- |
| 统一响应 `Result<T>` | 已完成 | Controller 统一返回 |
| 业务异常 `BusinessException` | 已完成 | 配合 `ErrorCode` 使用 |
| 全局异常处理 | 已完成 | `GlobalExceptionHandler` |
| 配置属性类 | 部分完成 | 存储和简历分析已有 Properties |
| 对象存储配置 | 已完成基础版 | `S3Config` |
| Prompt 模板目录 | 已完成 | `resources/prompts` |

未完成或待强化能力：

| 能力 | 当前状态 | 后续建议 |
| --- | --- | --- |
| 单元测试和集成测试 | 未完成 | 优先补面试 Service 测试 |
| API 文档 / OpenAPI | 未完成 | 接口稳定后加入 SpringDoc |
| Redis / Redisson | 未完成 | 先用于限流和会话缓存，再做 Stream |
| 限流注解 | 未完成 | 参考 `interview-guide` 的 `@RateLimit` |
| MapStruct | 未完成 | Converter 复杂后再引入 |
| Docker Compose | 未完成 | 引入 Redis/pgvector/MinIO 后统一编排 |
| 前端 | 未完成 | 后端核心稳定后再开始 |

## 4. 未开发模块

参考项目完整模块包括：

| 模块 | 参考项目状态 | 当前项目状态 | 建议学习顺序 |
| --- | --- | --- | --- |
| resume | 完整 | MVP 已完成 | 继续补异步、导出、删除、重分析 |
| interview | 完整 | MVP 已完成 | 当前最高优先级继续深化 |
| knowledgebase | 完整 | 未开始 | 放在面试 AI 评估后 |
| interviewschedule | 完整 | 未开始 | 放在知识库后或前端后 |
| voiceinterview | 完整 | 未开始 | 最后做，依赖较多 |
| frontend | 完整 | 未开始 | 后端主要 API 稳定后开始 |

## 5. 当前最应该补的内容

优先级从高到低：

1. 面试历史列表。
2. 面试历史详情。
3. 独立答案表。
4. 后端测试体系。
5. AI 出题和 AI 评估。
6. Redis 会话缓存和异步任务。
7. PDF 导出。
8. 知识库 / RAG。
9. 前端页面。
10. 面试日程和语音面试。

## 6. 当前风险

| 风险 | 影响 | 建议 |
| --- | --- | --- |
| 没有测试 | 改动后容易回归 | 从 `InterviewSessionService` 开始补单元测试 |
| 答案存储在 `questionsJson` | 历史详情、单题评分、导出会越来越复杂 | 做完历史详情后拆独立答案表 |
| 上传流程里同步 AI 分析 | 上传接口耗时长，失败重试弱 | 后续引入 Redis Stream 异步分析 |
| 接口路径和参考项目不完全一致 | 前端对接时需要适配 | 后续可以逐步统一或写清楚 API 规范 |
| 当前 Git 状态有迁移残留 | 可能误提交旧路径 | 正式开发前整理 `git status` |

## 7. 近期推荐结论

下一阶段不要马上做 Redis、RAG 或语音面试。最适合当前学习节奏的是继续深化文字面试模块：

1. 先做面试历史列表。
2. 再做面试历史详情。
3. 再把答案从 `questionsJson` 拆到独立 `InterviewAnswerEntity`。
4. 最后基于独立答案表做 AI 评估和报告导出。
