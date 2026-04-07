# AI Interview Assistant (智能面试助手)

## 📖 项目简介
本项目旨在打造一个基于大语言模型 (LLM) 的智能面试平台。核心功能包括：
1. **简历解析**：自动解析用户上传的 PDF/Word 简历，提取结构化信息。
2. **AI 简历评估**：利用大模型对简历进行打分，并给出优化建议。
3. **模拟面试**：根据简历内容，自动生成面试题并与用户进行多轮对话模拟。
4. **知识库检索**：结合 RAG (检索增强生成) 技术，提供专业的面试知识解答。

## 🏗 架构设计

本项目采用经典的**分层架构**，结合**领域驱动设计 (DDD)** 思想。

*   **Presentation Layer (接口层)**: `Controller`，负责暴露 RESTful API。
*   **Application Layer (应用层/服务层)**: `Service`，负责核心业务逻辑编排。
*   **Infrastructure Layer (基础设施层)**: 
    *   文件处理 (`FileStorageService`, `DocumentParseService`)
    *   AI 调用封装
    *   数据库持久化 (`Repository`)

## 🛠 技术栈
*   **核心框架**: Spring Boot 3.x + Java 17
*   **数据库**: PostgreSQL
*   **对象存储**: RustFS (兼容 AWS S3 协议)
*   **文档解析**: Apache Tika
*   **AI 交互**: Spring AI (对接各大模型)

## 🚀 启动说明
1.  确保本地 Docker 环境已启动（需运行 PostgreSQL 和 RustFS 容器）。
2.  进入 `server` 目录。
3.  运行 `mvn spring-boot:run` 启动后端服务。