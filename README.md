# 健康追踪（Healthcare）

健康管理 App 的全栈项目：Vue 3 移动端高保真原型 + Spring Boot 最小后端。

## 目录

| 目录             | 说明                                                                     |
| ---------------- | ------------------------------------------------------------------------ |
| `healthcare-app` | Vue 3 + TypeScript + Vite 前端，27 个页面，覆盖设计稿全部画板            |
| `healthcare-api` | Spring Boot 3.5 + MySQL 8 后端，提供认证、账号、健康数据、食物和运动接口 |
| `ui-tools`       | 设计稿解析与 Puppeteer 冒烟检查脚本（生成物不入库）                      |

## 快速开始

环境要求：Node.js 22+、Java 21、Maven、MySQL 8。

```bash
# 1) 初始化数据库（首次，需 MySQL root 密码）
mysql -u root -p < healthcare-api/db/init.sql

# 2) 启动后端（默认 http://localhost:8080；被占用时可加 --server.port=8081）
cd healthcare-api
mvn spring-boot:run

# 3) 启动前端（http://localhost:5173）
cd ../healthcare-app
npm install
npm run dev
```

开发模式下验证码会通过 `POST /api/auth/send-code` 的 `devCode` 字段返回，前端会自动填充。Vite 的 `/api` 代理默认指向 `http://localhost:8081`，可用 `.env.local` 中的 `VITE_API_PROXY_TARGET` 覆盖。

## Docker 部署

```bash
cp .env.example .env      # 至少设置 MYSQL_ROOT_PASSWORD
docker compose up --build
```

默认访问 `http://localhost:8088`。生产环境请在 `.env` 中设置 `APP_DEV_MODE=false`、`AUTH_COOKIE_SECURE=true`，并配置 `SMS_WEBHOOK_URL` 与 `CORS_ALLOWED_ORIGINS`。

## 质量检查

```bash
# 前端：类型检查 + ESLint + Prettier / 单元测试 / 生产构建
cd healthcare-app
npm run check
npm test
npm run build

# 后端：单元与集成测试（H2 内存库）
cd ../healthcare-api
mvn test
```

CI 位于 `.github/workflows/`：`frontend-ci.yml` 和 `backend-ci.yml` 按改动路径分别触发。

## 说明

- `ui-tools/sketch-extract/`、`ui-tools/slices/` 和截图均为本地生成物，已 gitignore；可用 `ui-tools` 下的脚本从设计稿重新生成。
- 数据库结构由 Flyway 管理（`healthcare-api/src/main/resources/db/migration`），JPA 以 `validate` 模式启动。
- 食物、运动类型、课程、助眠声音、设备、饮水份量和餐次等基础数据全部存放在数据库，前端通过 `GET /api/catalog` 一次加载，不再使用本地 mock 文件。
- 登录 token 使用 HttpOnly Cookie，前端只在 localStorage 记录一个非敏感的已登录标记。
- 生产部署前需关闭 `APP_DEV_MODE`、配置 `SMS_WEBHOOK_URL`、收紧 `CORS_ALLOWED_ORIGINS`，启用 HTTPS 并设置 `AUTH_COOKIE_SECURE=true`。
- 项目当前处于“高保真原型 + 最小可用后端”阶段，账户注销、真实设备同步和通知推送尚未实现。
