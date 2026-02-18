# 🚀 Ordish-AgentHub | 基于 Spring AI 的企业级多智能体聚合平台

![Java](https://img.shields.io/badge/Java-17%2B-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-green)
![Spring AI](https://img.shields.io/badge/Spring%20AI-Latest-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.0-orange)
![Redis](https://img.shields.io/badge/Redis-7.0-red)

## 📖 项目简介
Ordish-AgentHub 是一个基于 **Spring AI** 构建的**多智能体（Multi-Agent）交互平台**。本项目不仅实现了基础的大模型对话能力，更深度整合了 **RAG（检索增强生成）** 技术与 **Function Calling（函数调用）** 机制，打造了具备特定领域知识和实际业务执行能力的 AI 助手矩阵。

**🔥 本项目严格遵循企业级开发规范，不仅关注 AI 交互，更着眼于高并发、系统安全与稳定性。**

## 💡 核心企业级架构设计 (面试亮点)

### 1. 🛡️ 高并发防刷与 API 限流 (AOP + Redis Lua)
* 大模型 API 调用成本高昂，系统极易被恶意刷单。
* **解决方案**：引入 Redis，结合自定义注解 `@RateLimit` 与 AOP 切面编程，利用 **Redis Lua 脚本的原子性**，实现了精准的 IP 级别接口限流（如：限制某 IP 1分钟内最多请求10次）。一旦触发限流，直接拦截并抛出自定义异常。

### 2. 📦 统一 API 规范与全局异常拦截
* **解决方案**：重构所有的业务交互接口，使用统一的 `CommonResult<T>` 进行数据包装。引入 `@RestControllerAdvice` 实现全局异常兜底，无论是数据库报错还是触发了限流拦截，都能向前端返回结构化、优雅的 JSON 错误码，杜绝页面白屏或代码级报错外泄。

### 3. 🌊 响应式流式输出 (SSE)
* **解决方案**：针对 AI 核心打字机对话接口，打破传统的阻塞式 HTTP 请求，采用 `Flux<String>` 响应式流实现真正的 Server-Sent Events (SSE) 持续输出，极大降低了首字响应时间（TTFT），提升用户体验。

## ✨ 核心智能体矩阵

### 1. 🤖 模拟面试官 (Interview Agent)
* 定制化 Prompt 工程与 **意图路由 (Intent Routing)**。当用户处于“求教”状态时，直接调度模型先验知识；当用户准备好时，触发 RAG 检索候选人简历，进行硬核技术拷问。

### 2. 🚗 智能出行助手 (Didi Agent)
* 深度应用 **Function Calling (工具调用)**。大语言模型理解用户自然语言意图后，自动提取参数，回调底层 Java 代理方法（`DidiAgentTools`），并结合 MyBatis 操作 MySQL 订单表，实现“语言即交互”的完整业务闭环。

### 3. 📚 私有知识库问答 (RAG Doc Agent)
* 基于 **RAG（Retrieval-Augmented Generation）** 架构。包含 PDF 文档切片（TokenTextSplitter）、高维向量化（Embedding）、本地向量库检索。为解决多用户会话串联问题，创新性地在 Document Metadata 中引入 `chatId` 作为“防伪标签”，实现数据隔离。

### 4. 🧠 具备“记忆”的对话系统
* 打破 LLM 无状态限制，放弃脆弱的内存记忆，自定义实现 `MySqlChatMemory` 组件，将会话历史持久化至本地 MySQL 数据库，实现长线连续对话。

## 🛠️ 技术栈
* **后端**：Java 17, Spring Boot 3.x, Spring AI, MyBatis, AspectJ (AOP)
* **数据库 & 缓存**：MySQL 8.0, Redis (限流与缓存)
* **向量存储**：Local Vector Store (支持 RAG 检索)
* **前端**：HTML/CSS/JS (原生构建，轻量级动态交互与流式解析)

## 🚀 快速启动

1.  **克隆项目**
    ```bash
    git clone [https://github.com/Ordish-cell/Ordish-AgentHub.git](https://github.com/Ordish-cell/Ordish-AgentHub.git)
    ```
2.  **环境配置**
    * 启动本地或远程的 MySQL 与 Redis 服务。
    * 修改 `application.yaml` 中的数据库连接与 Redis 配置。
    * 填入你的大模型 API 密钥（如 DeepSeek/OpenAI）。
3.  **运行项目**
    * 启动 `OrdishAiApplication`，访问 `http://localhost:8080` 即可体验。
