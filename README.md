# 生活服务点评平台（Vue3 + Spring Boot 3）

## 1. 整体架构设计

本项目采用前后端分离架构：

- 前端：`frontend`（Vue3 + Element Plus + Axios + Vue Router）
- 后端：`backend`（Spring Boot 3 + Spring Web + JPA + MySQL）
- 存储：MySQL 8.0 + Redis（登录态 TTL + GEO 附近商户）
- 可扩展：图片上传支持本地/OSS 两种模式，AI 帮写可切换到大模型 API（如 ChatGLM）

核心模块关系：

1. 用户模块  
   前端登录页调用 `/api/auth/*` 完成注册、密码登录、验证码登录；后端使用 `BCryptPasswordEncoder` 加密密码，签发 JWT，并在 Redis 中维护会话 TTL。
2. 用户关系模块  
   前端关注页调用 `/api/users/{id}/follow`、`/api/users/me/following`、`/api/users/me/followers`。
3. 内容模块  
   前端博客页调用 `/api/blogs` 完成发布、编辑、删除；调用 `/api/blogs/{id}/like` 点赞或取消点赞。
4. 店铺点评模块  
   前端点评页调用 `/api/shops/{id}/reviews` 进行图文评价发布、编辑、删除；后端自动聚合店铺平均分并更新 `shops.avg_score`。  
   同时支持全站热门点评：`/api/reviews/hot`（按点赞数排行）和最新点评：`/api/reviews/latest`（按时间排序）。
5. 附近商户模块  
   前端调用 `/api/shops/nearby`，后端基于 Redis GEO 查询半径内店铺并按距离排序返回。
6. 初始化数据模块  
   管理员可调用 `/api/admin/seed/shops` 手动注入虚拟商铺数据（足疗、美食、娱乐、酒店、丽人等），并自动同步 GEO 索引。  
7. 商户服务申请模块  
   普通用户可提交新增商铺申请，管理员审批通过后商铺自动入库并可被普通用户查询。  
8. AI 帮写  
   前端点击“AI帮写”调用 `/api/ai/generate-review`，后端根据店铺类型生成场景化评价文案。

数据流向（简化）：

- `Vue 页面 -> Axios -> Spring Controller -> Service -> Repository -> MySQL`
- 登录时：`AuthService` 生成 token，`LoginInterceptor` 校验 `Authorization: Bearer <token>`
- 登录时：`AuthService` 生成 JWT 并写入 Redis（带过期时间），`LoginInterceptor` 同时校验 JWT 与 Redis 会话
- 点赞时：写入/删除 `like_records`，并同步更新 `blogs.like_count` 或 `reviews.like_count`，热门点评按 `reviews.like_count` 排序
- 评分时：新增/修改/删除评价后，聚合 `reviews.score` 并回写店铺平均分
- 地理查询时：店铺经纬度写入 Redis GEO（`geo:shops`），按距离升序查询附近商户
- 图片上传时：`/api/files/upload` 根据模式写入本地文件或返回 OSS URL，再写入 `blogs.images`

---

## 2. 数据库表结构

SQL 建表脚本见：`backend/src/main/resources/sql/schema.sql`  
包含以下核心表：

- `users`：用户信息（手机号/邮箱、加密密码、昵称）
- `follows`：关注关系（唯一键：`user_id + follow_user_id`）
- `blogs`：博客内容（标题、正文、图片、点赞数）
- `shops`：店铺信息（类型、地址、经纬度、平均分、评价数）
- `reviews`：店铺评价（文字、图片、星级、点赞数，强制关联 `shops` 与 `users`）
- `shop_apply_requests`：商铺申请记录（申请人、状态、审批人、审批备注）
- `like_records`：点赞记录（支持博客/评价多目标点赞）

---

## 3. 核心功能实现说明

### 后端核心代码位置

- 登录与注册：`backend/src/main/java/com/lifereview/controller/AuthController.java`
- 登录态校验：`backend/src/main/java/com/lifereview/config/LoginInterceptor.java`
- 用户关系：`backend/src/main/java/com/lifereview/controller/UserRelationController.java`
- 博客与点赞：`backend/src/main/java/com/lifereview/controller/BlogController.java`
- 店铺评价与评分：`backend/src/main/java/com/lifereview/controller/ShopReviewController.java`
- AI 帮写：`backend/src/main/java/com/lifereview/controller/AiController.java`
- 图片上传：`backend/src/main/java/com/lifereview/controller/FileController.java`

### 前端核心页面位置

- 登录页：`frontend/src/views/LoginView.vue`
- 博客页：`frontend/src/views/BlogView.vue`
- 店铺点评页：`frontend/src/views/ShopReviewView.vue`
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
9. 数据初始化：管理员手动触发虚拟商铺注入，支持幂等执行（重复调用不会重复插入）
10. 商铺申请审批：普通用户提交，管理员审核通过后上线

---

## 4. AI 帮写评价

当前后端已支持调用第三方大模型 API（OpenAI 兼容协议），默认配置为千问：

- 接口：`POST /api/ai/generate-review`
- 位置：`backend/src/main/java/com/lifereview/service/AiReviewService.java`
- 默认模型：`qwen-plus`
- 默认网关：`https://dashscope.aliyuncs.com/compatible-mode/v1`

可通过 `application.yml` 中 `app.ai.*` 配置切换模型或供应商。

---

## 5. 快速启动

### 后端

1. 创建 MySQL 数据库（与 `application.yml` 一致，默认示例：`life_service_review_platform`）
2. 启动 Redis（地址与 `application.yml` 一致）
3. 配置后端参数：`backend/src/main/resources/application.yml`
   - `spring.datasource.*`（MySQL 地址、账号、密码）
   - `spring.data.redis.*`（Redis 地址、端口、可选密码）
   - `app.jwt.secret`
   - `app.admin.user-ids`（可执行 `/api/admin/seed/*` 的管理员用户 ID，默认 `1`）
4. 配置 AI（千问）：
   - `app.ai.enabled: true`
   - `app.ai.base-url: https://dashscope.aliyuncs.com/compatible-mode/v1`
   - `app.ai.model: qwen-plus`
   - 设置环境变量 `AI_API_KEY`
     - Windows PowerShell:
       ```powershell
       setx AI_API_KEY "你的百炼API-KEY"
       ```
     - 设置后请新开一个终端再启动后端
5. 启动后端：

```bash
cd backend
mvn spring-boot:run
```

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

---

## 6. 已完成功能清单（对应需求）

- 用户模块：密码登录、验证码快捷登录、JWT + Redis 登录态过期校验、密码加密存储、关注/取消关注、关注列表、粉丝列表
- 内容模块：图文博客发布、编辑、删除、博客点赞/取消点赞、图片上传（本地/OSS 模式）
- 店铺点评模块：1-5 星评分、平均分展示、图文评价发布/编辑/删除、评价点赞/取消点赞、全站热门点评排行（按点赞数）、AI帮写评价
- 附近商户模块：店铺经纬度管理、Redis GEO 附近商户查询（按距离排序）

---

## 7. 新增接口说明（点评排行 + 附近商户）

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
6. 手动注入虚拟商铺（仅管理员）
   - `POST /api/admin/seed/shops`
   - 鉴权：需要登录且 `currentUserId` 在 `app.admin.user-ids` 内
7. 商铺申请与审批
   - `POST /api/merchant/shops/apply`：用户提交商铺申请
   - `GET /api/merchant/shops/apply/mine`：查看我的申请
   - `GET /api/admin/shops/apply/pending`：管理员查看待审批申请
   - `POST /api/admin/shops/apply/{id}/approve`：管理员审批通过（自动创建 shops + 同步 GEO）
   - `POST /api/admin/shops/apply/{id}/reject`：管理员驳回并记录备注

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
