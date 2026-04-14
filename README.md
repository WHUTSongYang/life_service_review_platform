# 生活服务点评平台（Vue3 + Spring Boot 3）

## 1. 整体架构设计

本项目采用前后端分离架构：

- 前端：`frontend`（Vue3 + Element Plus + Axios + Vue Router）
- 后端：`backend`（Spring Boot 3 + Spring Web + MyBatis-Plus + MySQL）
- 存储：MySQL 8.0 + Redis（登录态 TTL + GEO 附近商户 + 秒杀分布式锁等）
- 可扩展：图片上传支持本地/OSS 两种模式；AI 帮写与 AI 客服均基于 OpenAI 兼容协议接入大模型（如通义千问），`app.ai.enabled=false` 时走本地兜底文案

核心模块关系：

1. 用户模块  
   前端登录页调用 `/api/auth/*` 完成注册、密码登录（含图形验证码，`GET /api/auth/captcha`）；后端使用 `BCryptPasswordEncoder` 加密密码，签发 JWT，并在 Redis 中维护会话 TTL。
2. 用户关系模块  
   前端关注页调用 `/api/users/{id}/follow`、`/api/users/me/following`、`/api/users/me/followers`。
3. 内容模块  
   前端博客页调用 `/api/blogs` 完成发布、编辑、删除；调用 `/api/blogs/{id}/like` 点赞或取消点赞。
4. 店铺点评模块  
   前端点评页调用 `/api/shops/{id}/reviews` 进行图文评价发布、编辑、删除；后端自动聚合店铺平均分并更新 `shops.avg_score`。  
   同时支持全站热门点评：`/api/reviews/hot`（按点赞数排行）和最新点评：`/api/reviews/latest`（按时间排序）。
5. 附近商户模块  
   前端调用 `/api/shops/nearby`，后端基于 Redis GEO 查询半径内店铺并按距离排序返回。
6. 商户服务申请模块  
   普通用户可提交新增商铺申请，管理员审批通过后商铺自动入库并可被普通用户查询。  
7. AI 帮写  
   前端在点评侧栏点击「生成文案」调用 `POST /api/ai/generate-review`，后端根据店铺名称与类型生成约 100～180 字评价草稿；未启用大模型时按店铺类型返回预设模板。  
8. AI 客服  
   前端在店铺点评页（`ShopReviewView`）通过悬浮入口打开「AI客服」面板，使用 **SSE 流式对话**：`POST /api/ai/chat/stream`（`Content-Type: application/json`，响应为 `text/event-stream`，事件 `chunk` 推送增量文本、`done` 表示结束）。  
   后端由 `AiChatService` 调用 LangChain4j 流式模型；可选开启 **RAG**：启动时加载 `backend/src/main/resources/rag/faq.md`，按段落向量化检索，将命中 FAQ 注入系统提示词以增强回答；`app.rag.enabled=false` 时仅使用基础客服提示词。AI 未启用或模型不可用时返回本地兜底话术，仍模拟流式输出。

数据流向（简化）：

- `Vue 页面 -> Axios -> Spring Controller -> Service -> Repository(MyBatis-Plus) -> MySQL`
- 登录时：`AuthService` 生成 token，`LoginInterceptor` 校验 `Authorization: Bearer <token>`
- 登录时：`AuthService` 生成 JWT 并写入 Redis（带过期时间），`LoginInterceptor` 同时校验 JWT 与 Redis 会话
- 点赞时：写入/删除 `like_records`，并同步更新 `blogs.like_count` 或 `reviews.like_count`，热门点评按 `reviews.like_count` 排序
- 评分时：新增/修改/删除评价后，聚合 `reviews.score` 并回写店铺平均分
- 地理查询时：店铺经纬度写入 Redis GEO（`geo:shops`），按距离升序查询附近商户
- 图片上传时：`/api/files/upload` 根据模式写入本地文件或返回 OSS URL，再写入 `blogs.images`
- AI 客服时：客户端对 `/api/ai/chat/stream` 建立 SSE 长连接，`AiChatService` 可选经 `FaqRetrieverService` 检索 FAQ 后调用流式大模型，逐段推送至前端

---

## 2. 数据库表结构

SQL 建表脚本见：`backend/src/main/resources/sql/schema.sql`（含初始化数据：默认超级管理员、`shop_categories` 分类种子数据等）。  
表清单如下（与脚本一致）：

| 表名 | 说明 |
|------|------|
| `users` | 普通用户：手机号/邮箱、BCrypt 密码、昵称、创建时间 |
| `admin_accounts` | 后台管理员：用户名、密码、昵称、是否超级管理员、是否启用；脚本可插入默认 `superadmin` |
| `follows` | 关注关系：`user_id` 关注 `follow_user_id`，唯一键 `(user_id, follow_user_id)` |
| `blogs` | 博客/动态：标题、正文、图片、点赞数、创建/更新时间 |
| `shops` | 店铺：名称、类型、店主、头图、是否推广、地址、经纬度、平均分、点评数；外键店主 → `users` |
| `shop_categories` | 店铺分类字典：编码 `code`、展示名、排序、是否启用；与业务白名单类型对应 |
| `shop_apply_requests` | 商户入驻申请：申请人、拟建店铺信息、经纬度、状态（如待审/通过/驳回）、审批人、备注、时间 |
| `reviews` | 店铺点评：关联 `shops` 与 `users`，图文、1～5 星、点赞数、时间 |
| `review_comments` | 点评下的回复：关联 `reviews` 与发表用户 |
| `shop_products` | 店铺商品：价格、库存、描述、图片、是否上架；外键 → `shops` |
| `product_orders` | 商品订单：数量、金额、状态（待支付/已支付/已取消等）；同一用户对同一商品唯一订单（`uk_user_product`） |
| `like_records` | 点赞明细：`target_type` + `target_id` 区分博客/点评等目标，唯一键防重复点赞 |

表之间的主要关联：`users` ← 关注 / 博客 / 申请 / 点评 / 订单；`shops` ← 点评 / 商品 / 订单；`shop_products` ← 订单。

---

## 3. 核心功能实现说明

### 后端核心代码位置

- 登录与注册：`backend/src/main/java/com/lifereview/controller/AuthController.java`
- 登录态校验：`backend/src/main/java/com/lifereview/config/LoginInterceptor.java`
- 用户关系：`backend/src/main/java/com/lifereview/controller/UserRelationController.java`
- 博客与点赞：`backend/src/main/java/com/lifereview/controller/BlogController.java`
- 店铺评价与评分：`backend/src/main/java/com/lifereview/controller/ShopReviewController.java`
- AI 功能（帮写点评 + 客服 SSE）：`backend/src/main/java/com/lifereview/controller/AiController.java`
- AI 帮写实现：`backend/src/main/java/com/lifereview/service/AiReviewService.java`（及 `impl/AiReviewServiceImpl.java`）
- AI 客服流式对话：`backend/src/main/java/com/lifereview/service/AiChatService.java`（及 `impl/AiChatServiceImpl.java`）
- RAG / FAQ 检索：`backend/src/main/java/com/lifereview/service/FaqRetrieverService.java`，知识文档：`backend/src/main/resources/rag/faq.md`
- 图片上传：`backend/src/main/java/com/lifereview/controller/FileController.java`

### 前端核心页面位置

- 登录页：`frontend/src/views/LoginView.vue`
- 博客页：`frontend/src/views/BlogView.vue`
- 店铺点评页（含 **AI 帮写** 与 **AI 客服** 浮层面板）：`frontend/src/views/ShopReviewView.vue`
- 关注关系页：`frontend/src/views/FollowView.vue`

### 关键实现点

1. 密码加密：`BCryptPasswordEncoder`
2. 登录态：JWT + Redis 过期机制（支持滑动续期）
3. 点赞逻辑：同一用户对同一目标“二次点击即取消”
4. 评分聚合：每次评价变更后重算店铺平均分和评价数
5. 图片上传：前端 `el-upload` 直传，后端存储策略按 `app.upload.mode` 切换
6. 热门点评：全站点评按点赞数降序（同点赞数按时间倒序）
7. 最新点评：全站点评按创建时间倒序
8. 附近商户：基于 Redis GEO 半径查询，按距离排序
9. 商铺申请审批：普通用户提交，管理员审核通过后上线
10. AI 客服：SSE 流式回复 + 可选 FAQ 向量检索（RAG）；关闭 AI 或模型失败时使用本地兜底话术

---

## 4. AI 功能（帮写点评 + 智能客服）

当前后端使用 **LangChain4j**，通过 OpenAI 兼容协议调用第三方大模型 API，默认配置为阿里云通义千问（DashScope）：

### 4.1 AI 帮写评价

- 接口：`POST /api/ai/generate-review`  
- 请求体：`shopName`、`shopType`（见 `AiReviewRequest`）  
- 实现：`AiReviewService` / `AiReviewServiceImpl`（非流式）  
- 默认模型：`qwen-plus`  
- 默认网关：`https://dashscope.aliyuncs.com/compatible-mode/v1`  
- `app.ai.enabled=false` 或未配置有效 API Key 时：按店铺类型返回内置模板文案，不请求远程模型。

### 4.2 AI 智能客服

- 接口：`POST /api/ai/chat/stream`（响应 `Content-Type: text/event-stream`）  
- 请求体：`message`（必填）、`history`（可选，多轮 `role` + `content`）  
- 实现：`AiChatService` / `AiChatServiceImpl`（LangChain4j `OpenAiStreamingChatModel`，SSE 推送）  
- **RAG**：`app.rag.enabled=true` 时，`FaqRetrieverService` 读取 `classpath:rag/faq.md`，分段嵌入并检索 Top-K，将结果注入系统提示词；`app.rag.enabled=false` 时仅用固定客服人设提示词。  
- `app.ai.enabled=false` 或模型不可用：返回固定兜底说明，并以分块方式模拟流式输出。

### 4.3 配置说明

公共项在 `backend/src/main/resources/application.yml`；环境相关在 **`application-dev.yml`**（本机开发）与 **`application-prod.yml`**（Docker/生产）。

- `app.ai.*`：总开关、网关、模型名、超时、API Key；**推荐**通过环境变量注入：`APP_AI_ENABLED`、`APP_AI_API_KEY`、`APP_AI_BASE_URL`、`APP_AI_MODEL` 等（见 `application.yml` 占位符）
- `app.rag.*`：RAG 开关、Top-K、相似度阈值、FAQ 文件路径、向量嵌入模型名等  

---


## 5. 快速启动

### 后端环境（Spring Profile：dev / prod）

| Profile | 用途 | 配置文件 |
|--------|------|----------|
| **dev**（默认） | Windows 本机：MySQL 在 `127.0.0.1`，Redis 默认指向虚拟机 `192.168.88.101`，秒杀 **不** 使用 Kafka（同步建单） | `application.yml` + `application-dev.yml` |
| **prod** | CentOS Docker：MySQL/Redis/Kafka 使用 Compose 服务名 `mysql` / `redis` / `kafka`，秒杀 **开启** Kafka | `application.yml` + `application-prod.yml` |

未设置 `spring.profiles.active` 时，默认激活 **dev**（`spring.profiles.default: dev`）。生产或容器内需显式指定 **`SPRING_PROFILES_ACTIVE=prod`**，否则会沿用 dev 的 `127.0.0.1` 数据库地址。

**常用环境变量**

- 数据库：`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`
- Redis：`SPRING_DATA_REDIS_HOST`、`SPRING_DATA_REDIS_PASSWORD`（Spring Boot 3 使用 `SPRING_DATA_REDIS_*`，不是 `SPRING_REDIS_HOST`）
- Kafka：`KAFKA_BOOTSTRAP_SERVERS`（dev 默认 `localhost:9092`；prod 默认在 `application-prod.yml` 中为 `kafka:9092`）
- 安全与 AI：`APP_JWT_SECRET`、`APP_AI_ENABLED`、`APP_AI_API_KEY` 等

### 后端

1. 创建 MySQL 数据库（默认库名：`life_service_review_platform`，与 `application-dev.yml` / `application-prod.yml` 中 URL 一致）
2. 启动 Redis（dev：虚拟机或本机；地址用 `SPRING_DATA_REDIS_HOST` 覆盖 `application-dev.yml` 中的默认主机）
3. **Kafka（仅 prod 或本地要测异步秒杀时）**：dev 默认 `app.kafka.seckill.enabled=false`，无需 Kafka。prod 中 `app.kafka.seckill.enabled=true`，需可访问的 Kafka；Topic 默认为 `seckill-purchase`（见 `app.kafka.seckill.topic`），需自动建 topic 或事先创建。

4. 配置敏感信息（**不要**提交到 Git）：在系统/IDE 环境变量或脚本中设置 `SPRING_DATASOURCE_PASSWORD`、`SPRING_DATA_REDIS_PASSWORD`、`APP_JWT_SECRET`；若使用 AI：`APP_AI_ENABLED=true` 与 `APP_AI_API_KEY`。
5. 配置 AI（千问）与可选 RAG（可选）：
   - 环境变量：`APP_AI_ENABLED=true`、`APP_AI_API_KEY`（DashScope 等）
   - 需要 **AI 客服 RAG** 时：`APP_RAG_ENABLED=true`，并确保嵌入模型与网关可用
   - Windows PowerShell 示例：
     ```powershell
     setx APP_AI_API_KEY "你的百炼API-KEY"
     ```
     设置后请新开一个终端再启动后端
7. 启动后端：

```bash
cd backend
mvn spring-boot:run
```

显式指定 profile（可选）：`mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"`；生产 JAR：`java -jar life-review-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod`。

默认访问：`http://localhost:8080`

### 前端

首次启动先安装依赖：

```bash
cd frontend
npm install
```

启动开发服务器：

```bash
npm run dev
```

默认访问：`http://localhost:5173`

> 如果 PowerShell 出现 `npm.ps1` 执行策略错误，可改用：`npm.cmd run dev`

### 打包部署到 CentOS（前后端）

- 说明与 Nginx / systemd 示例：`deploy/centos/README.md`
- Windows 一键打出上传目录：`.\scripts\package-for-centos.ps1`（生成 `dist-package/`）
- 生产环境前端 API 基地址：复制 `frontend/.env.production.example` 为 `frontend/.env.production`，同域 Nginx 反代时保持 `VITE_API_BASE=` 为空

---

## 6. 已完成功能清单（对应需求）

- 用户模块：密码登录（图形验证码 + 密码）、JWT + Redis 登录态过期校验、密码加密存储、关注/取消关注、关注列表、粉丝列表
- 内容模块：图文博客发布、编辑、删除、博客点赞/取消点赞、图片上传（本地/OSS 模式）
- 店铺点评模块：1-5 星评分、平均分展示、图文评价发布/编辑/删除、评价点赞/取消点赞、全站热门点评排行（按点赞数）、AI 帮写评价、**AI 智能客服**（SSE 流式对话，可选 FAQ 知识库 RAG）
- 附近商户模块：店铺经纬度管理、Redis GEO 附近商户查询（按距离排序）

---

## 7. 主要接口说明（点评排行 + 附近商户 + AI）

1. 获取热门点评（公开）
   - `GET /api/reviews/hot?page=0&size=10`
   - 排序：`likeCount DESC, createdAt DESC`
2. 获取最新点评（公开）
   - `GET /api/reviews/latest?page=0&size=10`
   - 排序：`createdAt DESC`
3. 查询附近商户（公开）
   - `GET /api/shops/nearby?longitude=116.397128&latitude=39.916527&radiusKm=5&limit=20`
   - 排序：距离升序
4. 创建或更新店铺（需登录）
   - `POST /api/shops`（可携带 `longitude`、`latitude`）
   - `PUT /api/shops/{shopId}`（可更新经纬度并同步 GEO）
5. 发布或更新评价（需登录）
   - `POST /api/shops/{shopId}/reviews`
   - `PUT /api/reviews/{reviewId}`
   - 请求体新增可选字段：`images`（逗号分隔图片 URL）
6. 商铺申请与审批
   - `POST /api/merchant/shops/apply`：用户提交商铺申请
   - `GET /api/merchant/shops/apply/mine`：查看我的申请
   - `GET /api/admin/shops/apply/pending`：管理员查看待审批申请
   - `POST /api/admin/shops/apply/{id}/approve`：管理员审批通过（自动创建 shops + 同步 GEO）
   - `POST /api/admin/shops/apply/{id}/reject`：管理员驳回并记录备注
7. AI 智能客服（SSE，公开）
   - `POST /api/ai/chat/stream`
   - 请求体 JSON：`message`（必填）、`history`（可选，多轮对话）
   - 响应：`text/event-stream`，事件 `chunk` 推送文本片段，`done` 表示结束；`app.ai.enabled=false` 时为本地兜底流式输出
8. AI 帮写点评（公开）
   - `POST /api/ai/generate-review`
   - 请求体：`shopName`、`shopType`；用于点评侧栏生成草稿文案

## 8. 商铺类型规范

系统内置商铺分类白名单：

- `美食`
- `酒店`
- `娱乐`
- `按摩`
- `电影院`
- `足疗`
- `丽人`
- `运动`

创建商铺与提交申请均会校验类型合法性，不在白名单中的类型将被拒绝。

---

## 9. MCP 商户检索（Cursor 开发辅助）

仓库内提供独立子项目 [`mcp-shop-search/`](mcp-shop-search/README.md)：基于 **Python + MCP stdio**，通过 HTTP 调用后端已公开的商户查询接口（`GET /api/shops`、`/api/shops/nearby`、`/api/shops/{id}`、`/api/shops/types`），**无需 JWT**。先在本地启动 Spring Boot，再在 Cursor 的 MCP 设置中配置 `python -m mcp_shop_search` 及环境变量 `LIFE_REVIEW_API_BASE`（默认 `http://localhost:8080`）。详细安装与工具列表见 **`mcp-shop-search/README.md`**。
