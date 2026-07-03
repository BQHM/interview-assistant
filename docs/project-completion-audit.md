# 项目完成度对比审计

## 审计日期

2026-06-29

## 参考基线

- 当前项目：`D:\work\work_space\Project\interview-assistant`
- 参考项目：`D:\work\work_space\Project\interview-guide`
- 对比方式：按模块、接口、数据模型、工程能力和学习价值进行对比。

## 总体结论

`interview-assistant` 当前已经完成后端 MVP 的两条核心主链路：简历处理和文字模拟面试。文字面试模块已经从单纯跑通流程，推进到历史查询、独立答案表、报告聚合、同步 AI 出题、AI 单题评估入库，以及报告读取评分、反馈、参考答案和关键点阶段。它还不是完整平台，但已经具备继续向 `interview-guide` 工程化能力演进的基础。

| 维度 | 当前完成度 | 判断 |
| --- | --- | --- |
| 按当前 README 定义的后端 MVP | 约 95% | 简历与文字面试主流程、历史、答案表、同步 AI 出题、AI 单题评估入库、报告读取评分/反馈/参考答案/关键点已完成手工验收 |
| 按 `interview-guide` 完整平台 | 约 35% | 参考项目包含完整前端、Redis、RAG、日程、语音、PDF、Docker 等能力；当前已开始补齐前端核心页面 |
| 工程学习价值 | 较高 | 当前阶段适合学习分层、JPA、文件处理、AI 调用、接口设计、服务拆分和测试入门 |

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
| 规则版出题 | 已完成第一版 | 根据简历关键词生成题目，作为 AI 出题失败时的兜底 |
| 同步 AI 出题 | 已完成基础版 | 创建面试时优先调用 AI 生成结构化题目，失败时回退规则题 |
| 查询会话详情 | 已完成 | `GET /api/interviews/{sessionId}`，题目快照和答案表聚合 |
| 查询当前题 | 已完成 | `GET /api/interviews/{sessionId}/question` |
| 暂存答案 | 已完成 | `PUT /api/interviews/{sessionId}/answers`，写入独立答案表 |
| 提交答案并推进 | 已完成 | 当前接口为 `POST /api/interviews/answer`，写入独立答案表，并同步生成单题评估结果 |
| 提前交卷 | 已完成 | `POST /api/interviews/{sessionId}/complete` |
| 面试历史列表 | 已完成 | `GET /api/interviews` |
| 面试历史详情 | 已完成 | `GET /api/interviews/{sessionId}/details` |
| 删除面试会话 | 已完成 | 删除答案记录后删除会话 |
| 独立答案表 | 已完成 | `interview_answers` 保存用户答案和评估字段 |
| 规则版报告 | 已完成第一版 | `GET /api/interviews/{sessionId}/report`，从答案表聚合 |
| 查询未完成会话 | 已完成 | `GET /api/interviews/unfinished/{resumeId}` |
| 服务拆分 | 已完成第一版 | 拆为会话、历史、报告 Service |
| AI 单题评估服务 | 已接入主链路 | DTO、Prompt、Service 已完成，提交答案后同步评估并写回答案表 |
| 报告读取单题评估 | 已完成第一版 | 报告优先读取 `interview_answers.score`、`feedback`、`referenceAnswer` 和 `keyPointsJson` |

与参考项目差距：

| 参考项目能力 | 当前状态 | 后续建议 |
| --- | --- | --- |
| 报告展示参考答案和关键点 | 已完成基础版 | 当前报告 DTO 已支持返回 `referenceAnswer` 和 `keyPoints`，需要继续做完整链路手工验收 |
| Skill 驱动出题 | 未完成 | 当前已有同步 AI 出题基础版，后续再引入 Skill、题目去重和更稳定的结构化输出 |
| 历史题目去重 | 未完成 | Skill 出题后加入 |
| 多轮追问 | 未完成 | AI 出题和评估稳定后再做 |
| Redis Stream 异步评估 | 未完成 | 同步 AI 评估跑通后再异步化 |
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
| Spring AI 接入 | 已完成第一版 | 简历分析、面试出题、面试答案评估已接入，并保留规则兜底 |
| 最小单元测试 | 已完成第一版 | 已为题目答案聚合工具补充 JUnit 测试 |
| 服务职责拆分 | 已完成第一版 | 面试模块已拆出历史服务和报告服务 |

未完成或待强化能力：

| 能力 | 当前状态 | 后续建议 |
| --- | --- | --- |
| 单元测试和集成测试 | 部分完成 | 已有 1 个最小单元测试，后续补核心 Service 测试 |
| API 文档 / OpenAPI | 未完成 | 接口稳定后加入 SpringDoc |
| Redis / Redisson | 未完成 | 先用于限流和会话缓存，再做 Stream |
| 限流注解 | 未完成 | 参考 `interview-guide` 的 `@RateLimit` |
| MapStruct | 未完成 | Converter 复杂后再引入 |
| Docker Compose | 未完成 | 引入 Redis/pgvector/MinIO 后统一编排 |
| 前端 | 进行中 | 已创建 Vite + React + TypeScript 工程，下一步按 `interview-guide` 复刻核心页面 |

## 4. 未开发模块

参考项目完整模块包括：

| 模块 | 参考项目状态 | 当前项目状态 | 建议学习顺序 |
| --- | --- | --- | --- |
| resume | 完整 | MVP 已完成 | 继续补异步、导出、删除、重分析 |
| interview | 完整 | MVP 已完成并进入 AI 链路验收和测试补强阶段 | 先验证 AI 出题、答案评估和报告展示闭环，再做 Skill 出题和 Redis 异步化 |
| frontend | 完整 | 进行中 | 当前优先，对齐参考项目左侧导航 + 内容区布局，先实现核心页面 |
| knowledgebase | 完整 | 未开始 | 放在前端核心主链路之后 |
| interviewschedule | 完整 | 未开始 | 放在知识库后 |
| voiceinterview | 完整 | 未开始 | 最后做，依赖较多 |

## 5. 当前最应该补的内容

优先级从高到低：

1. 按 `interview-guide` 复刻前端核心结构：Layout、路由、API Client、DTO 类型。
2. 完成前端核心页面：简历管理、上传简历、简历详情、面试中心、文字面试、面试记录和报告详情。
3. 完成浏览器端主链路联调：上传简历 -> 查看分析 -> 创建面试 -> 答题 -> 查看报告。
4. 后端核心 Service 测试补强，优先覆盖 AI 失败兜底、提交答案推进和报告聚合。
5. Skill 驱动 AI 出题，补充题目去重和更稳定的结构化输出。
6. Redis 会话缓存和接口限流。
7. Redis Stream 异步化简历分析和面试评估。
8. PDF 导出。
9. 知识库 / RAG、面试日程和语音面试。

## 6. 当前风险

| 风险 | 影响 | 建议 |
| --- | --- | --- |
| 没有完整测试 | 改动后容易回归 | 已有最小测试，后续从核心 Service 开始补单元测试 |
| 已接入同步 AI 出题 | 创建面试接口会受 AI 耗时和输出稳定性影响 | 当前阶段先保留规则出题兜底，后续演进为 Skill 出题、题目去重和异步/缓存策略 |
| 已接入同步 AI 单题评估 | 提交答案接口会受 AI 耗时影响 | 当前阶段先同步跑通并保留规则兜底，后续改为 Redis Stream 异步评估 |
| 上传流程里同步 AI 分析 | 上传接口耗时长，失败重试弱 | 后续引入 Redis Stream 异步分析 |
| 接口路径和参考项目不完全一致 | 前端对接时需要适配 | 先在前端 API 层适配当前接口，后续再评估是否统一路径 |
| 前端复制参考项目过深 | 学习成本上升且容易引入未完成模块 | 当前只复刻核心布局和已完成业务页面，不提前引入知识库、日程和语音 |
| 当前 Git 状态有迁移残留 | 可能误提交旧路径 | 正式开发前整理 `git status` |

## 7. 近期推荐结论

下一阶段不要马上做 Redis、RAG 或语音面试。当前后端主链路已经通过 Postman 验收，最适合当前学习和面试展示的是先补前端核心页面：

1. 参考 `interview-guide/frontend` 完成 Layout、路由、API Client 和 DTO 类型。
2. 实现简历管理、上传简历、简历详情、面试中心、文字面试、面试记录和报告详情。
3. 用浏览器完成一条端到端联调流程。
4. 链路稳定后补核心 Service 测试。
5. 再将同步 AI 出题演进为 Skill 驱动出题，之后考虑 Redis Stream 异步化。
