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
- 当前已接入同步 AI 出题和 AI 单题答案评估，并保留规则兜底；前后端主链路已通过浏览器验收，后续再异步化

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
| Vite | 8.x | 前端构建工具 |
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
- 支持 AI 出题失败时自动回退到规则版面试题目
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

### 进行中

- [ ] 前端空状态、错误提示和时间格式展示继续完善
- [ ] 补强核心 Service 单元测试

### 未完成

- [ ] Skill 驱动 AI 出题
- [ ] Redis 会话缓存
- [ ] 异步任务流转
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
- 创建面试时会优先复用同一简历最近一条未完成会话
- 创建面试时优先调用 `InterviewQuestionService` 进行 AI 出题，AI 不可用或输出异常时回退到规则版出题
- `questionsJson` 现在只承担题目快照职责，不再作为用户答案的唯一存储位置
- `saveAnswer` 只更新指定题目的草稿答案，不推进 `currentQuestionIndex`
- `submitAnswer` 要求按顺序作答，并在成功后推进到下一题
- 提交答案后会同步调用 `InterviewAnswerEvaluationService` 生成单题评分、反馈、参考答案和关键点，并写回 `interview_answers`
- 面试报告优先读取 `interview_answers.score`、`feedback`、`referenceAnswer` 和 `keyPointsJson`，不再按答案长度现场评分
- 提前交卷会将状态改为 `COMPLETED`
- 只有 `COMPLETED` 状态才允许生成报告

## 项目结构

```text
interview-assistant/
├── README.md
├── server/
    ├── pom.xml
    └── src/main/java/com/interview/
        ├── App.java
        ├── common/
        │   ├── annotation/
        │   ├── config/
        │   ├── constant/
        │   ├── exception/
        │   ├── model/
        │   └── result/
        ├── infrastructure/
        │   └── file/
        └── modules/
            ├── resume/
            │   ├── model/
            │   ├── repository/
            │   ├── service/
            │   └── ResumeController.java
            └── interview/
                ├── model/
                ├── repository/
                ├── service/
                └── InterviewController.java
└── frontend/
    ├── package.json
    └── src/
        ├── api/
        ├── types/
        ├── components/
        └── pages/
```

## 接口清单

### 简历模块

- `POST /api/resumes/upload`：上传简历
- `GET /api/resumes`：查询简历列表
- `GET /api/resumes/{id}`：查询简历详情
- `GET /api/resumes/{id}/analysis`：查询简历分析结果

### 面试模块

- `POST /api/interviews`：创建面试会话；若存在未完成会话则直接返回原会话
- `GET /api/interviews/{sessionId}`：查询面试会话详情
- `GET /api/interviews/{sessionId}/question`：获取当前题目
- `PUT /api/interviews/{sessionId}/answers`：暂存答案但不推进下一题
- `POST /api/interviews/answer`：提交当前题答案
- `POST /api/interviews/{sessionId}/complete`：提前交卷
- `GET /api/interviews/{sessionId}/report`：生成面试报告
- `GET /api/interviews/unfinished/{resumeId}`：查询未完成面试会话

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- PostgreSQL
- RustFS 或其他兼容 S3 协议的对象存储

### 配置说明

项目通过 `server/src/main/resources/application.yml` 和 `server/.env` 读取配置。

核心配置包括：

- PostgreSQL 连接信息
- AI 模型地址和 API Key
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

1. 完善前端体验：补充更友好的空状态、错误提示和时间格式展示。
2. 补强核心 Service 单元测试，优先覆盖 AI 失败兜底、提交答案推进和报告聚合。
3. 将同步 AI 出题演进为 Skill 驱动出题，增加题目去重和更稳定的结构化输出。
4. Redis 会话缓存和接口限流。
5. Redis Stream 异步化简历分析和面试评估。
6. PDF 报告导出。
7. 知识库 / RAG。

## 参考项目

同一工作区下的 `interview-guide/` 是完整参考实现，后续扩展能力时应优先对照该项目的模块划分和演进顺序。
