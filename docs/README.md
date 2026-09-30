# interview-assistant docs

本目录用于记录 `interview-assistant` 的学习路线、完成度对比、实施计划和关键架构决策。项目以远程仓库 `https://gitee.com/SnailClimb/interview-guide` 为参考实现，但不会机械复制参考项目代码，而是按学习和真实开发节奏逐步靠齐。

## 文档阅读顺序

1. `project-completion-audit.md`：先了解当前已经完成什么、还缺什么、和参考项目差距在哪里。
2. `learning-roadmap.md`：再按阶段学习，每个阶段都有目标、参考文件和验收方式。
3. `implementation-plan.md`：实际开发时按任务推进，每个任务都有验收标准和验证方法。
4. `README.md`：查看当前启动方式、Postman 验收顺序和浏览器验收顺序。
5. `decisions/ADR-001-reference-guided-incremental-implementation.md`：理解为什么采用“参考项目驱动 + 分阶段演进”的方式。
6. `decisions/ADR-002-frontend-reference-first.md`：理解为什么前端先对齐 `interview-guide`，项目完整后再差异化。

## 当前项目定位

- 当前项目：`D:\work\work_space\Project\interview-assistant`
- 参考项目：远程仓库 `https://gitee.com/SnailClimb/interview-guide`（默认分支 `master`），本地不保存其源码
- 当前阶段：简历模块和文字面试后端主链路已完成；`java-backend` 和 `system-design` 两个 Skill 已完成资源加载、前端选择、独立会话和双方向端到端验收；AI 单题评估仍采用同步调用并保留规则兜底；前端核心页面已通过浏览器验收。
- 当前优先级：先完成历史题目去重、AI 结构化输出重试和核心测试补强，再推进 Redis 会话缓存、请求幂等、限流和异步任务。新版 `interview-guide` 已提供这些能力的参考实现，但当前项目暂不一次性复制语音、RAG、日程等完整模块。

## 使用原则

- 每次只做一个小任务，完成后验证。
- 先读参考项目，再看当前项目，再动手实现。
- 当前阶段允许简化，但文档中必须标明后续如何向参考项目演进。
- 代码实现前先确认任务的验收标准。
