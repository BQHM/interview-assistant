# 《智能面试助手》

基于 Spring Boot 和 Spring AI 的后端练手项目，当前聚焦简历分析与文字模拟面试两条主链路。

---

## 项目介绍

`interview-assistant` 是一个以 `interview-guide` 为参考实现、按教学式节奏逐步迭代的后端 MVP。

当前项目暂不追求一次性覆盖完整平台能力，而是优先把下面两条核心链路做扎实：

- 简历上传、解析、存储与分析
- 文字模拟面试的会话创建、答题推进、暂存答案与报告生成

和 `interview-guide` 相比，`interview-assistant` 当前仍处于分阶段演进阶段：

- 只保留后端，不包含前端页面
- 已实现 `resume` 和 `interview` 两个核心模块
- 简历模块已接入 AI 分析，并保留规则兜底
- 文字面试主链路已从 `questionsJson` 答案存储演进到独立答案表
- 当前正在接入 AI 单题答案评估，先采用同步评估 + 规则兜底，后续再异步化

## 当前定位

当前后端重点是先把简历模块和文字模拟面试模块的主链路跑通，再逐步向 `interview-guide` 的模块拆分、接口语义和业务语义靠拢。

现阶段更偏向：

- 能清楚展示后端分层设计
- 能完整演示业务主链路
- 能支持 Postman 级别的联调与验证
- 能为后续 AI 出题、Redis 缓存、异步评估和前端页面演进打基础

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
- 支持根据简历关键词生成规则版面试题目
- 支持查询面试会话详情
- 支持获取当前应该展示的题目
- 支持暂存答案但不推进下一题
- 支持正式提交答案并推进到下一题
- 支持提前交卷
- 支持生成规则版面试报告
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
- [x] AI 单题答案评估 DTO、Prompt 和 Service

### 进行中

- [ ] 提交答案后同步调用 AI 单题评估并写回答案表
- [ ] 面试报告优先读取答案表中的 AI 评估结果

### 未完成

- [ ] Skill 驱动 AI 出题
- [ ] Redis 会话缓存
- [ ] 异步任务流转
- [ ] 知识库 / RAG 问答
- [ ] 面试日程管理
- [ ] 语音面试
- [ ] PDF 报告导出
- [ ] 前端页面

## 当前实现的关键设计点

### 简历模块

- 使用文件内容哈希做去重，而不是只比较文件名
- 简历分析优先走 AI，失败时自动降级到规则版分析
- 分析结果持久化后，可直接通过查询接口返回前端

### 面试模块

- 一场面试对应一条 `InterviewSessionEntity`
- `currentQuestionIndex` 表示下一道待答题的索引，而不是最近一道已答题索引
- 创建面试时会优先复用同一简历最近一条未完成会话
- `questionsJson` 现在只承担题目快照职责，不再作为用户答案的唯一存储位置
- `saveAnswer` 只更新指定题目的草稿答案，不推进 `currentQuestionIndex`
- `submitAnswer` 要求按顺序作答，并在成功后推进到下一题
- 提前交卷会将状态改为 `COMPLETED`
- 只有 `COMPLETED` 状态才允许生成报告

## 项目结构

```text
interview-assistant/
├── README.md
└── server/
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

进入后端目录后启动：

```bash
cd server
mvn spring-boot:run
```

默认启动端口：

```text
http://localhost:8080
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

## 后续演进建议

当前项目仍处于教学式迭代阶段，建议按下面顺序继续推进：

1. 提交答案后写入 AI 单题评估结果
2. 面试报告优先使用答案表中的 AI 评分和反馈
3. AI 出题
4. Redis 会话缓存和接口限流
5. Redis Stream 异步化简历分析和面试评估
6. PDF 报告导出
7. 知识库 / RAG
8. 前端页面

## 参考项目

同一工作区下的 `interview-guide/` 是完整参考实现，后续扩展能力时应优先对照该项目的模块划分和演进顺序。
