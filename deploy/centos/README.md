# CentOS 部署说明（前后端）

## 一、本地打包（Windows / macOS / Linux）

### 1. 后端 JAR

```bash
cd backend
mvn -DskipTests clean package
```

产物：`backend/target/life-review-backend-0.0.1-SNAPSHOT.jar`

### 2. 前端静态资源

**方案 A（推荐，与下方 Nginx 示例一致）** — 同域反代，API 走 `/api`：

```bash
cd frontend
copy .env.production.example .env.production
# 编辑 .env.production：保持 VITE_API_BASE= 为空
npm install
npm run build
```

**方案 B** — 浏览器直接访问 `http://服务器IP:8080`：

在 `.env.production` 中设置：

```env
VITE_API_BASE=http://你的服务器IP:8080
```

然后 `npm run build`。

产物：`frontend/dist/` 目录。

### 3. 上传到 CentOS

将以下文件/目录拷到服务器（例如 `/opt/life-review/`）：

| 本地路径 | 服务器建议路径 |
|----------|----------------|
| `backend/target/*.jar` | `/opt/life-review/app/life-review-backend.jar`（可重命名） |
| `frontend/dist/*` | `/opt/life-review/www/` |
| `deploy/centos/nginx-life-review.conf.example` | 参考配置 Nginx |
| `deploy/centos/life-review-backend.service` | 参考配置 systemd |

可用 `scp`、`rsync` 或 WinSCP。

---

## 二、服务器环境（CentOS）

- **JDK 17**：`sudo yum install -y java-17-openjdk` 或使用 SDK/手动安装
- **Nginx**：`sudo yum install -y nginx`（或 dnf）
- 已安装 **MySQL**、**Redis**（你已就绪）
- 若生产环境开启 **Kafka 秒杀**（`app.kafka.seckill.enabled=true`），需部署 Kafka 集群并保证与 `spring.kafka.bootstrap-servers` 网络可达，消费者组见 `spring.kafka.consumer.group-id`

### 1. 数据库

在 MySQL 中执行：

```bash
mysql -u... -p... < schema.sql
```

`schema.sql` 路径：`backend/src/main/resources/sql/schema.sql`

### 2. 后端配置

#### 方式 A：使用 JAR 内建的 Profile（推荐）

仓库已提供 **`application-prod.yml`**（MySQL/Redis/Kafka 服务名、秒杀 Kafka 开关等）。打包后 prod 配置在 classpath 中，只需 **激活 profile 并注入环境变量**：

```bash
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_PASSWORD='你的MySQL密码'
export SPRING_DATA_REDIS_PASSWORD='你的Redis密码'
export APP_JWT_SECRET='足够长的随机串'
# 可选：AI
export APP_AI_ENABLED=true
export APP_AI_API_KEY='你的DashScope等Key'

java -jar /opt/life-review/app/life-review-backend.jar
```

Kafka 地址默认 `kafka:9092`（与 Docker Compose 服务名一致）；若 broker 地址不同，设置 `KAFKA_BOOTSTRAP_SERVERS`。

#### 方式 B：外部配置文件覆盖

在服务器上放 **额外 YAML**（不把密码打进 Git），与 JAR 同目录，例如 `override-prod.yml`，启动参数：

```bash
java -jar /opt/life-review/app/life-review-backend.jar \
  --spring.profiles.active=prod \
  --spring.config.additional-location=file:/opt/life-review/app/override-prod.yml
```

#### Docker Compose 示例（后端容器）

与 MySQL、Redis、Kafka 同网段时，后端服务可配置为：

```yaml
services:
  java-app:
    image: your-registry/life-review-backend:latest
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD}
      SPRING_DATA_REDIS_PASSWORD: ${REDIS_PASSWORD}
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      APP_JWT_SECRET: ${APP_JWT_SECRET}
      # APP_AI_API_KEY: ${APP_AI_API_KEY}
    depends_on:
      - mysql
      - redis
      - kafka
    ports:
      - "8080:8080"
```

**注意**：容器内必须使用 **`SPRING_PROFILES_ACTIVE=prod`**，否则会使用默认的 **dev** profile（连接 `127.0.0.1` 的 MySQL，在容器内会失败）。Redis 主机名请使用 **`SPRING_DATA_REDIS_HOST=redis`**（若与 `application-prod.yml` 默认一致可省略）。

### 3. systemd（开机自启）

```bash
sudo cp life-review-backend.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now life-review-backend
sudo systemctl status life-review-backend
```

按需编辑 service 文件中的 `User`、`WorkingDirectory`、`ExecStart`。

### 4. Nginx（方案 A：同域）

```bash
sudo cp nginx-life-review.conf.example /etc/nginx/conf.d/life-review.conf
# 将 server_name、ssl_certificate 等改为你的域名与证书路径（HTTP 可先注释 SSL 段）
sudo nginx -t && sudo systemctl reload nginx
```

防火墙放行 80/443：

```bash
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

---

## 三、验证

- 后端：`curl -s http://127.0.0.1:8080/api/shops/types`（公开接口，按实际路径调整）
- 前端：浏览器访问 `http://服务器IP` 或域名

---

## 四、项目内一键打包脚本（Windows）

在仓库根目录执行：

```powershell
.\scripts\package-for-centos.ps1
```

会在 `dist-package/` 生成 `backend-jar/`、`frontend-dist/` 及说明文件，便于整体上传。
