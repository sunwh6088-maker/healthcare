# 健康追踪（Healthcare）

健康管理 App 的全栈项目：Vue 3 移动端高保真原型 + Spring Boot 最小后端。

## 目录

| 目录             | 说明                                                                           |
| ---------------- | ------------------------------------------------------------------------------ |
| `healthcare-app` | Vue 3 + TypeScript + Vite 前端，30+ 页面，覆盖设计稿全部画板，支持 PWA 安装    |
| `healthcare-api` | Spring Boot 3.5 + MySQL 8 后端，提供认证、账号、头像、健康数据、食物和运动接口 |
| `ui-tools`       | 设计稿解析、图标生成与 Puppeteer 冒烟检查脚本（生成物不入库）                  |

## 快速开始

环境要求：Node.js 22+、Java 25 (LTS)、Maven、MySQL 8。

```bash
# 1) 初始化数据库（首次，需 MySQL root 密码）
mysql -u root -p < healthcare-api/db/init.sql

# 2) 启动后端（前端代理默认指向 8081；8080 空闲时也可改回 8080）
cd healthcare-api
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081

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

默认访问 `http://localhost:8088`。生产环境请在 `.env` 中设置 `APP_DEV_MODE=false`、`AUTH_COOKIE_SECURE=true`，并配置 `SMS_WEBHOOK_URL`、`VAPID_*` 与 `CORS_ALLOWED_ORIGINS`。完整的 HTTPS、备份、推送排查与上线清单见 [DEPLOY.md](DEPLOY.md)。

## 上线检查清单

在生产环境部署前，至少确认以下事项：

1. **关闭开发模式**：`.env` 中设置 `APP_DEV_MODE=false`，并配置真实短信网关
   `SMS_WEBHOOK_URL` / `SMS_WEBHOOK_TOKEN`（Webhook 接收 `{"phone","code","scene"}`）。
2. **修改默认密码**：设置强随机 `MYSQL_ROOT_PASSWORD`、`DB_PASSWORD`。
3. **启用 HTTPS**：在宿主机或反向代理层配置证书，并设置
   `AUTH_COOKIE_SECURE=true`。应用容器只监听内网 8080，`X-Forwarded-*` 由 Nginx 透传。
4. **收紧跨域**：`CORS_ALLOWED_ORIGINS` 填写真实站点地址（逗号分隔）。
   同源部署会自动放行，跨域接口调用必须显式列出。
5. **数据持久化**：`mysql-data` 与 `uploads-data` 两个 Docker 卷分别保存数据库和头像；
   不要执行 `docker compose down -v`，否则会删除卷。
6. **备份**：`healthcare-api/scripts/backup.sh` 生成 gzip 备份并保留 14 天，
   建议加入 crontab；`restore.sh` 可用于恢复。
7. **健康检查**：容器探活使用 `GET /actuator/health`（仅健康信息，不暴露细节）；
   Nginx 层未对外开放该路径。
8. **单实例限制**：验证码和限流目前保存在进程内，多实例部署需要引入 Redis 等共享存储，
   并保持会话粘滞。

推荐的 HTTPS 反向代理（Caddy，自动申请证书）：

```caddyfile
health.example.com {
    encode gzip
    reverse_proxy 127.0.0.1:8088
}
```

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
- 账号安全：登录 token 只存在于 HttpOnly Cookie；密码 8-64 位且含字母和数字；修改手机号需新号码短信验证；
  写接口校验 Origin/Referer 防 CSRF；登录、验证码和改绑手机号均有频率限制。
- 敏感操作二次验证：修改手机号、注销账号、首次设置密码时，有密码需输入当前密码，无密码需当前手机号验证码。
- 登录设备：记录每次登录的设备与 IP，可在「账户与安全」查看并单独退出某台设备或一键退出其他设备；
  修改密码保留当前会话、重置密码撤销全部会话。
- Web Push：设置页开启通知后，闹钟到点会通过 Service Worker 推送提醒；生产环境需 HTTPS 与固定 `VAPID_*` 密钥。
- IoT 设备接入：通用 MQTT + Alink 物模型，设备上报 `sys/{productKey}/{deviceName}/thing/event/property/post`，
  解析步数、饮水、睡眠、体重、电量并写入健康数据；设备页支持绑定、解绑，开发环境可模拟上报。
  可连接 EMQX / Mosquitto / 阿里云 MQTT 版，配置见 [DEPLOY.md](DEPLOY.md#6-设备接入mqtt)。
- 合规能力：登录页需同意《用户协议》《隐私政策》；设置页可导出全部个人数据（JSON）；
  账号注销会级联删除健康数据、记录和头像文件。
- 头像保存在 `APP_UPLOAD_DIR`（Docker 中为 `/app/data/uploads`），仅接受 JPG/PNG/WebP，最大 2MB。
- 生产部署前需关闭 `APP_DEV_MODE`、配置 `SMS_WEBHOOK_URL`、收紧 `CORS_ALLOWED_ORIGINS`，启用 HTTPS 并设置 `AUTH_COOKIE_SECURE=true`。
- 项目当前处于“可上线 Web 应用”阶段；真实设备同步尚未实现（需要厂商 SDK / 健康平台接入）。
  验证码、限流与闹钟推送去重保存在进程内，多实例部署需引入 Redis 和分布式锁。
