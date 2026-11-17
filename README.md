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

# 2) 启动后端（http://localhost:8080）
cd healthcare-api
mvn spring-boot:run

# 3) 启动前端（http://localhost:5173）
cd ../healthcare-app
npm install
npm run dev
```

开发模式下验证码会通过 `POST /api/auth/send-code` 的 `devCode` 字段返回，前端会自动填充。

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
- 生产部署前需关闭 `APP_DEV_MODE`、接入短信服务、收紧 CORS，并将登录 token 改为 HttpOnly Cookie。
- 项目当前处于“高保真原型 + 最小可用后端”阶段，账户注销、真实设备同步和通知推送尚未实现。
