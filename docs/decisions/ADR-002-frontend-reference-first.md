# ADR-002: Frontend Reference-First Implementation

## Status

Accepted

## Date

2026-07-03

## Context

`interview-assistant` 的后端简历和文字面试主链路已经通过 Postman 手工验收。为了面试展示和完整项目体验，前端需要提前进入开发。

项目目标是先模仿并完成 `interview-guide` 的完整能力，而不是过早做差异化。此前讨论过将前端做成类似对话窗口的 AI 助手形态，但这会偏离当前“先对齐参考项目”的阶段目标。

## Decision

前端当前采用 reference-first 策略：

- 以 `interview-guide/frontend` 为主要参考。
- 先复刻左侧导航栏 + 右侧业务内容区的管理台式布局。
- 工程结构按 `api/`、`types/`、`components/`、`pages/` 分层。
- 第一阶段只实现当前后端已完成的核心页面：简历管理、上传简历、简历详情、面试中心、文字面试、面试记录和报告详情。
- 暂不实现大横幅首页、聊天窗口式改造、知识库、日程、语音面试和差异化 UI。
- 等项目主要能力完整后，再考虑产品形态和视觉差异化。

## Alternatives Considered

### Chat-style AI Assistant UI

- Pros: 更贴近豆包、通义千问等 AI 助手产品形态。
- Cons: 与当前参考项目结构偏离较大，也会增加交互设计成本。
- Rejected: 当前阶段优先完成参考项目等价能力。

### Marketing-style Landing Page

- Pros: 首页展示效果明显。
- Cons: 不符合本项目工具型、业务型应用定位，也不能体现核心开发能力。
- Rejected: 不作为当前阶段入口。

### Full Copy of `interview-guide` Frontend

- Pros: 最接近参考项目。
- Cons: 会引入知识库、日程、语音、设置等当前后端尚未完成模块。
- Rejected: 只复制结构和核心业务路径，不复制未具备后端支撑的模块。

## Consequences

- 前端能更快形成可演示闭环。
- 面试时可以清楚说明项目如何从后端接口演进到前后端联调。
- 后续新增知识库、日程、语音面试时，可以继续沿用参考项目导航和页面结构。
- 短期内前端不会追求差异化 UI，重点是完成度和可讲解性。
