# AI Interview Assistant

## 项目说明

`interview-assistant` 是一个智能面试助手后端练手项目，当前阶段主要聚焦两个核心模块：

- 简历上传、解析与分析
- 模拟面试会话创建、答题推进与报告生成

这个项目以 `interview-guide/` 作为参考实现，但目前还不是完整平台版本，而是一个逐步演进中的后端 MVP。

## 当前进度

### 已完成模块

#### 1. 简历模块

当前已经打通以下链路：

- 上传简历文件
- 校验文件类型与大小
- 基于文件哈希去重
- 使用 Apache Tika 解析简历文本
- 将原始文件上传到 RustFS / S3 兼容存储
- 调用 AI 进行简历分析
- AI 失败时回退到规则版分析
- 查询简历列表
- 查询简历详情
- 查询简历分析结果

#### 2. 模拟面试模块

当前已经打通以下链路：

- 基于简历创建面试会话
- 根据简历关键词生成规则版题目
- 查询整场面试会话详情
- 获取当前应该展示的题目
- 提交答案并推进到下一题
- 提前交卷
- 生成规则版面试报告
- 按简历查询最近一条未完成面试会话

## 当前未完成的能力

以下能力在参考实现中存在，但当前项目还没有完成：

- 前端页面
- 知识库 / RAG 问答
- 语音面试
- 面试日程管理
- Redis 会话缓存
- 异步任务流转
- Skill 驱动 AI 出题
- AI 面试评估报告
- 暂存答案但不推进下一题
- PDF 报告导出

## 当前技术栈

- Spring Boot 4.0.1
- Java 21
- Maven
- PostgreSQL
- Spring Data JPA
- Spring AI
- Apache Tika
- RustFS / S3 Compatible Storage

## 当前后端架构

项目当前采用较清晰的分层结构：

- Controller：对外暴露 HTTP 接口
- Service：编排业务流程
- Repository：负责数据库访问
- Infrastructure：文件解析、文件校验、对象存储等基础设施能力

## 目录结构

```text
interview-assistant/
├── README.md
└── server/
    ├── pom.xml
    └── src/main/java/com/interview/
        ├── App.java
        ├── common/
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

- `POST /api/interviews`：创建面试会话
- `GET /api/interviews/{sessionId}`：查询面试会话详情
- `GET /api/interviews/{sessionId}/question`：获取当前题目
- `POST /api/interviews/answer`：提交当前题答案
- `POST /api/interviews/{sessionId}/complete`：提前交卷
- `GET /api/interviews/{sessionId}/report`：生成面试报告
- `GET /api/interviews/unfinished/{resumeId}`：查询未完成面试会话

## 环境要求

- JDK 21+
- Maven 3.9+
- PostgreSQL
- RustFS 或其他兼容 S3 协议的对象存储

## 配置说明

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

## 启动方式

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
7. 提交答案：`POST /api/interviews/answer`
8. 提前交卷：`POST /api/interviews/{sessionId}/complete`
9. 生成报告：`GET /api/interviews/{sessionId}/report`

## 当前实现的几个关键设计点

### 简历模块

- 使用文件内容哈希做去重，而不是只看文件名
- 简历分析优先走 AI，失败时自动降级到规则版分析
- 分析结果落库后，可通过查询接口直接返回给前端

### 面试模块

- 一场面试对应一条 `InterviewSessionEntity`
- `currentQuestionIndex` 表示下一道待答题的索引
- 当前版本将题目与答案统一存储在 `questionsJson` 中
- 提前交卷会将状态改为 `COMPLETED`
- 只有 `COMPLETED` 状态才允许生成报告

## 开发备注

- 当前项目仍处于教学式迭代阶段，优先保证主链路可跑通
- 后续如果继续演进，建议优先补齐以下能力：
  - 暂存答案
  - 面试历史列表
  - AI 出题
  - 独立答案表
  - Redis 会话缓存

## 参考项目

同一工作区下的 `interview-guide/` 是完整参考实现，后续扩展能力时应优先对照该项目的模块划分和演进顺序。
