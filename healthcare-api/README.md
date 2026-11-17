# 健康追踪后端 · healthcare-api

Spring Boot 3.5 + MySQL 8 的最小后端，提供账号、健康数据和闹钟接口。

## 技术栈

- Java 21
- Spring Boot 3.5.16（Web / Data JPA / Validation）
- MySQL 8.4
- Maven

## MySQL（本机 3306）

项目使用本机已有的 MySQL 8.4（`127.0.0.1:3306`），只创建 `healthcare` 库和专用账号，**不会修改已有数据库**。

首次初始化（已完成一次，无需重复；需要 root 密码）：

```bash
mysql -u root -p < db/init.sql
```

会创建：

- 数据库 `healthcare`（utf8mb4）
- 专用账号 `healthcare@localhost` / `healthcare@127.0.0.1`，密码 `healthcare_dev`
- 表结构由 Flyway 迁移创建（`src/main/resources/db/migration`），JPA 只做 `ddl-auto=validate` 校验：`users`、`health_states`、`alarms`、`auth_tokens`、`food_logs`、`exercise_logs`
- 基础数据表：`foods`、`exercise_types`、`exercise_lessons`、`sleep_sounds`、`devices`、`water_portions`、`meal_types`（V2 迁移）
- 已有数据库首次启动时会自动 baseline，不会重复建表；后续结构变更请新增 `V2__xxx.sql`

> 之前在没有 root 密码时曾用 `3307` 隔离实例联调，现已切换回 3306；`.mysql-data/` 目录可删除，仅作为历史备份。

> 如果你想改用本机已有的 3306 MySQL，先执行 `db/init.sql`，然后启动后端时设置
> `DB_URL=jdbc:mysql://127.0.0.1:3306/healthcare?...`（或修改 `application.yml` 默认值）。

## 启动

```bash
mvn spring-boot:run
# 或
mvn -DskipTests package
java -jar target/healthcare-api-0.0.1-SNAPSHOT.jar
```

默认监听 `http://localhost:8080`。

可用环境变量覆盖配置：

| 变量                            | 默认值                                                              | 说明                                             |
| ------------------------------- | ------------------------------------------------------------------- | ------------------------------------------------ |
| `DB_URL`                        | `jdbc:mysql://127.0.0.1:3306/healthcare?...`                        | 数据库连接                                       |
| `DB_USER`                       | `healthcare`                                                        | 数据库账号                                       |
| `DB_PASSWORD`                   | `healthcare_dev`                                                    | 数据库密码                                       |
| `APP_DEV_MODE`                  | `true`                                                              | 为 true 时验证码接口返回 `devCode` 并打印日志    |
| `APP_TOKEN_TTL_DAYS`            | `7`                                                                 | 登录 token 有效期                                |
| `CORS_ALLOWED_ORIGINS`          | `http://localhost:5173,http://localhost:5174,http://localhost:5175` | 允许的前端源，逗号分隔                           |
| `AUTH_COOKIE_SECURE`            | `false`                                                             | HTTPS 部署时必须设为 `true`                      |
| `AUTH_COOKIE_SAME_SITE`         | `Lax`                                                               | 前后端不同站时设为 `None`（需同时启用 Secure）   |
| `SMS_WEBHOOK_URL`               | 空                                                                  | 生产短信 Webhook 地址，POST `{phone,code,scene}` |
| `SMS_WEBHOOK_TOKEN`             | 空                                                                  | Webhook 的 Bearer token（可选）                  |
| `SEND_CODE_COOLDOWN_SECONDS`    | `60`                                                                | 同一手机号验证码发送冷却                         |
| `SEND_CODE_MAX_PER_HOUR`        | `10`                                                                | 同一手机号每小时验证码上限                       |
| `LOGIN_MAX_ATTEMPTS_PER_10_MIN` | `10`                                                                | 同一手机号 10 分钟登录尝试上限                   |

## 接口

统一返回格式：`{ "code": 0, "msg": "ok", "data": ... }`；错误时 `code` 为 400/401/404/500，`msg` 为原因。

### 认证（无需 token）

| 方法 | 路径                       | 请求                                                 |
| ---- | -------------------------- | ---------------------------------------------------- |
| POST | `/api/auth/send-code`      | `{ "phone": "13900001111" }`                         |
| POST | `/api/auth/login`          | `{ "phone": "13900001111", "code": "123456" }`       |
| POST | `/api/auth/login-password` | `{ "phone": "13900001111", "password": "pass1234" }` |
| POST | `/api/auth/reset-password` | `{ "phone": "…", "code": "…", "newPassword": "…" }`  |
| POST | `/api/auth/logout`         | 请求头带 Bearer token，服务端删除该 token            |

`send-code` 在开发模式下返回 `data.devCode`，同时写入后端日志；验证码 5 分钟有效，登录成功后立即失效。
`login-password` 用于已设置密码的账号；`reset-password` 验证码通过后设置新密码，并直接返回登录 token。

`login` 返回：

```json
{
  "token": "…",
  "user": {
    "id": 1,
    "name": "杰西·比伯",
    "phone": "13900001111",
    "email": "jessie@example.com"
  },
  "newUser": true
}
```

### 账号（需要 `Authorization: Bearer <token>`）

| 方法  | 路径               | 说明                                                                     |
| ----- | ------------------ | ------------------------------------------------------------------------ |
| GET   | `/api/me`          | 当前账号资料（含 `hasPassword`）                                         |
| PATCH | `/api/me`          | 修改 `name` / `email` / `phone`，字段可选                                |
| PUT   | `/api/me/password` | `{ "oldPassword": "…", "newPassword": "…" }`；首次设置可省略 oldPassword |

### 基础数据（需要 token）

| 方法 | 路径           | 说明                                                                                          |
| ---- | -------------- | --------------------------------------------------------------------------------------------- |
| GET  | `/api/catalog` | 一次性返回食物、运动类型、课程、助眠声音、设备、饮水份量、餐次，以及当前用户最近/频繁食物名称 |

前端启动后调用一次并缓存；新增食物或修改目录只需改数据库，不用重新发版。

### 健康数据与闹钟（需要 token）

| 方法 | 路径         | 说明                        |
| ---- | ------------ | --------------------------- |
| GET  | `/api/state` | 获取健康数据 + 闹钟列表     |
| PUT  | `/api/state` | 整体保存健康数据 + 闹钟列表 |

`GET /api/state` 返回示例：

```json
{
  "health": {
    "steps": 4204,
    "stepsGoal": 6000,
    "waterMl": 900,
    "waterGoalMl": 2000,
    "activeMinutes": 0,
    "activeGoalMinutes": 60,
    "sleepMinutes": 500,
    "sleepStartMinutes": 1380,
    "foodKcal": 0,
    "exerciseCount": 0,
    "notifyEnabled": true,
    "privacyEnabled": true,
    "deviceConnected": true,
    "deviceAdded": false,
    "gender": null,
    "age": 22,
    "heightCm": 160,
    "weightKg": 60.0
  },
  "alarms": [
    {
      "id": 1,
      "time": "08:30",
      "label": "工作日",
      "repeat": [1, 2, 3, 4, 5],
      "on": true
    }
  ]
}
```

`PUT /api/state` 使用相同结构；后端会整体替换该用户的闹钟列表。

### 食物记录（需要 token）

| 方法   | 路径                             | 说明                                                                                                              |
| ------ | -------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| GET    | `/api/food-logs?date=yyyy-MM-dd` | 某天的食物记录与总热量，date 缺省为今天                                                                           |
| POST   | `/api/food-logs`                 | `{ "date": "2026-11-13", "meal": "breakfast", "name": "萝卜", "portion": "1 根", "calories": 29, "emoji": "🥕" }` |
| DELETE | `/api/food-logs/{id}`            | 删除一条记录并重算当天总热量                                                                                      |

餐次取值：`breakfast` / `lunch` / `dinner` / `snack`。新增/删除后会同步更新 `health_states.food_kcal`（当天总热量）。

### 运动记录（需要 token）

| 方法   | 路径                      | 说明                                                                       |
| ------ | ------------------------- | -------------------------------------------------------------------------- |
| GET    | `/api/exercise-logs`      | 当前用户的运动记录，按日期倒序                                             |
| POST   | `/api/exercise-logs`      | `{ "date": "2026-11-13", "type": "yoga", "minutes": 30, "calories": 320 }` |
| DELETE | `/api/exercise-logs/{id}` | 删除一条运动记录                                                           |

运动类型：`yoga` / `walk` / `run` / `ride`。

## 安全说明

- 登录 token 为 32 字节随机值，数据库只保存 SHA-256 哈希，默认 7 天过期；退出登录会删除对应 token。
- 登录/重置密码时同时下发 HttpOnly Cookie，浏览器端不再把 token 写入 localStorage；接口仍返回 token 以兼容 CLI 和测试。
- 密码使用 BCrypt 哈希，支持验证码登录、密码登录、修改密码和验证码重置密码。
- 验证码为 6 位随机值，5 分钟有效，连续输错 5 次即失效；开发模式下接口返回 `devCode` 便于联调。
- 验证码发送和登录尝试都有限流；生产环境关闭 `APP_DEV_MODE` 后，验证码通过 `SMS_WEBHOOK_URL` 交给短信网关发送。
- CORS 使用 `CORS_ALLOWED_ORIGINS` 白名单，并允许携带 Cookie；不要把生产环境配成 `*`。
- 修改手机号暂未要求二次验证；生产环境建议增加原/新手机号验证。

## 测试

```bash
mvn test
```

`AuthFlowTest` 使用 H2 内存库覆盖：发送验证码 → 登录 → 获取账号 → 获取/保存健康数据与闹钟 → 修改邮箱 → 未登录 401 → 设置密码 → 密码登录 → 退出登录 token 失效 → 验证码重置密码 → 食物记录 → 运动记录。

## 前端联调

前端项目位于 `C:\Users\21693\Projects\healthcare-app`，开发时在 `vite.config.ts` 中把 `/api` 代理到后端（默认 `http://localhost:8081`，可用 `VITE_API_PROXY_TARGET` 覆盖）。登录 Cookie 由浏览器自动保存和携带，前端只维护一个非敏感的已登录标记。

## 数据库迁移

- 迁移脚本位于 `src/main/resources/db/migration`，启动时自动执行。
- 新增/修改表结构时添加 `V3__xxx.sql`、`V4__xxx.sql`，不要直接改已有脚本。
- 已有数据库首次接入 Flyway 时会自动 baseline 到版本 0，再执行 V1；`CREATE TABLE IF NOT EXISTS` 保证不会破坏现有表。

## 部署

仓库根目录提供 `docker-compose.yml`（MySQL + API + Nginx 前端）。复制 `.env.example` 为 `.env`，设置 `MYSQL_ROOT_PASSWORD` 后执行：

```bash
docker compose up --build
```

默认通过 `http://localhost:8088` 访问前端，`/api` 由 Nginx 转发到后端。生产环境必须设置 `APP_DEV_MODE=false`、`AUTH_COOKIE_SECURE=true`，并配置 `SMS_WEBHOOK_URL` 与 `CORS_ALLOWED_ORIGINS`。
