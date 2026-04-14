# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository shape

This is a full-stack life-service review platform with a Vue 3 frontend in `frontend/` and a Spring Boot 3 backend in `backend/`.

- Frontend: Vue 3 + Vue Router + Element Plus + Axios (`frontend/src/main.js`, `frontend/src/router/index.js`)
- Backend: Spring Boot 3.3 + MyBatis-Plus + Spring Security + Redis + optional Kafka + LangChain4j (`backend/pom.xml`)
- Data stores: MySQL + Redis
- Optional AI/RAG: OpenAI-compatible model access through LangChain4j, with FAQ retrieval from `backend/src/main/resources/rag/faq.md`
- Deployment helpers: `deploy/centos/` and `scripts/package-for-centos.ps1`
- Extra dev tool: `mcp-shop-search/` is a separate Python MCP server for read-only shop search against the backend API

## Common commands

### Frontend (`frontend/`)

```bash
cd frontend
npm install
npm run dev
```

Build production assets:

```bash
cd frontend
npm run build
```

Preview built assets:

```bash
cd frontend
npm run preview
```

Notes:
- Vite dev server runs on port `5173` (`frontend/vite.config.js`).
- Production API base is controlled by `frontend/.env.production`; the recommended Nginx setup leaves `VITE_API_BASE=` empty for same-origin `/api` proxying.
- There is currently no lint or frontend test script in `frontend/package.json`.

### Backend (`backend/`)

Run in local dev profile:

```bash
cd backend
mvn spring-boot:run
```

Run with an explicit profile:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

Run tests:

```bash
cd backend
mvn test
```

Run a single test class:

```bash
cd backend
mvn -Dtest=ClassName test
```

Run a single test method:

```bash
cd backend
mvn -Dtest=ClassName#methodName test
```

Build the backend jar:

```bash
cd backend
mvn -DskipTests clean package
```

Run the packaged jar in prod mode:

```bash
cd backend
java -jar target/life-review-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Notes:
- Java version is `17` (`backend/pom.xml`).
- There is no committed `backend/src/test` tree right now, but Maven test commands are standard when tests are added.

### Database / local environment

Import schema and seed data:

```bash
mysql -u<user> -p<password> life_service_review_platform < backend/src/main/resources/sql/schema.sql
```

Key runtime dependencies:
- MySQL is required.
- Redis is required for login session TTL, captcha, GEO queries, cache, and seckill support.
- Kafka is optional in dev and enabled in prod for async seckill flow.

### Packaging / deployment helpers

Windows packaging helper from repo root:

```powershell
.\scripts\package-for-centos.ps1
```

CentOS deployment reference:
- `deploy/centos/README.md`
- `deploy/centos/nginx-life-review.conf.example`
- `deploy/centos/life-review-backend.service`

## Configuration model

Backend configuration is split across:
- `backend/src/main/resources/application.yml` — shared defaults
- `backend/src/main/resources/application-dev.yml` — local Windows/dev setup
- `backend/src/main/resources/application-prod.yml` — Docker/CentOS/prod setup

Important behavior:
- Default Spring profile is `dev`.
- Dev expects MySQL on `127.0.0.1` and Redis on `192.168.88.101` unless overridden.
- Prod expects service names like `mysql`, `redis`, and `kafka`.
- If running in Docker/production, explicitly set `SPRING_PROFILES_ACTIVE=prod`; otherwise the app will fall back to dev addresses.

Frequently used environment variables:
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PASSWORD`
- `KAFKA_BOOTSTRAP_SERVERS`
- `APP_JWT_SECRET`
- `APP_AI_ENABLED`
- `APP_AI_API_KEY`
- `APP_AI_BASE_URL`
- `APP_AI_MODEL`
- `APP_RAG_ENABLED`

## High-level architecture

### Frontend application structure

The frontend is a single Vue SPA mounted from `frontend/src/main.js`.

Routing is centralized in `frontend/src/router/index.js`:
- Public routes include `/login`, `/shops`, `/shops/list`, shop detail routes, and review detail routes.
- All other routes are guarded in the router by checking `localStorage.getItem("token")` and redirecting to `/login`.
- Admin screens are nested under `/admin` with a shared `AdminLayout`.

The major UI areas are:
- user auth
- blogs / social content
- shops and reviews
- follow relationships
- user profile
- admin dashboard + shop audit/manage
- merchant product management

### Backend request flow

The backend follows a conventional layered flow:

`Controller -> Service -> Repository(MyBatis-Plus) -> MySQL`

Main entry point:
- `backend/src/main/java/com/lifereview/LifeReviewApplication.java`

Package responsibilities:
- `controller` — REST endpoints
- `service` / `service/impl` — business logic
- `repository` — MyBatis-Plus data access
- `entity` — persistence models
- `dto` — request/response payloads
- `config` — MVC, security, Redis, MyBatis, LangChain4j wiring
- `storage` — local vs OSS file storage abstraction
- `kafka` — async seckill consumer
- `util` — JWT utilities and similar helpers

### Authentication and authorization

Auth is intentionally split across Spring Security and a custom interceptor:
- `SecurityConfig` disables CSRF and permits all HTTP requests.
- Actual business auth is enforced by `LoginInterceptor` registered in `WebMvcConfig` for `/api/**`.
- Tokens are JWTs created by `JwtTokenProvider` and also backed by Redis session state.
- Session TTL supports sliding renewal through app config.

Implication: when changing access control, inspect both Spring Security config and interceptor logic rather than assuming filter-chain auth rules are doing the enforcement.

### Core business domains

The backend is organized around several domain slices exposed as controllers/services:
- auth and captcha
- user profile and follow graph
- blogs and likes
- shops, shop categories, nearby search, and reviews
- merchant shop application workflow
- admin dashboard / shop audit / shop seeding
- shop products, product orders, and seckill purchasing
- AI review generation and AI customer-service chat
- file upload

Important domain behaviors:
- Likes are tracked in `like_records` and denormalized back onto blog/review counters.
- Review create/update/delete recomputes shop aggregate score and review count.
- Nearby shop queries depend on Redis GEO, not just SQL.
- Shop categories are cached in Redis.
- Merchant onboarding has a user submission path and a separate admin approval path.

### Products and seckill flow

`ShopProductServiceImpl` is one of the densest services in the backend. It owns:
- product CRUD and management filtering
- product detail caching
- order creation / payment / cancellation
- seckill stock preload into Redis at startup
- Redis Lua stock deduction / rollback
- Redisson locking
- optional Kafka-based async seckill execution in prod
- DB fallback path if Redis scripting is unavailable

This means changes to products, order state, or inventory frequently span Redis, DB, and optionally Kafka.

### AI and RAG flow

AI features are centered around:
- `AiController`
- `AiReviewServiceImpl`
- `AiChatServiceImpl`
- `FaqRetrieverService`
- `AiLangChainConfig`

There are two distinct AI paths:
- review generation: regular request/response text generation
- customer service chat: SSE streaming via `SseEmitter`

Important behavior:
- AI chat streams `chunk` and `done` events over `text/event-stream`.
- RAG is optional and loads FAQ content from `backend/src/main/resources/rag/faq.md`.
- If AI is disabled or model calls fail, the backend returns local fallback copy; this fallback behavior is part of the intended product behavior, not just a temporary dev stub.

### File storage and uploads

Uploads go through `FileController` and the `storage` package.

Storage is designed as a mode switch:
- `app.upload.mode=local` serves files from `app.upload.local-path`
- OSS mode returns remote URLs instead

`WebMvcConfig` also maps the local upload directory onto the public URL prefix, so upload-related changes can involve both controller/storage logic and MVC static resource mapping.

## Data model anchors

The schema lives in `backend/src/main/resources/sql/schema.sql`.

Key tables include:
- `users`, `admin_accounts`
- `follows`
- `blogs`
- `shops`, `shop_categories`, `shop_apply_requests`
- `reviews`, `review_comments`
- `shop_products`, `product_orders`
- `like_records`

When investigating a feature, start from the controller and service, then confirm the schema here before changing queries or assumptions.

## Repo-specific guidance for future edits

- Prefer reading `README.md` before changing environment-dependent behavior; it contains the real dev/prod assumptions used in this repo.
- Do not assume Kafka is active locally; dev defaults to synchronous seckill handling.
- Do not assume Spring Security annotations or filter-chain rules enforce auth here; `LoginInterceptor` is the real gate for `/api/**`.
- For frontend auth bugs, inspect both router guards and backend token/session validation.
- For shop discovery or nearby-search bugs, check Redis GEO and cache behavior, not only SQL/repository code.
- For AI changes, keep the fallback path intact unless the task explicitly removes it.
