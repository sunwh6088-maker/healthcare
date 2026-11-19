# 上线部署指南

本文档面向单机 Docker 部署。默认拓扑：

```text
浏览器 ──HTTPS──> Caddy / Nginx（宿主机）──> web:8088（Nginx + Vue 静态文件）
                                             └── /api/ ──> api:8080（Spring Boot）
                                                              └──> mysql:3306
```

## 1. 准备环境变量

```bash
cp .env.example .env
```

生产环境至少修改以下值：

| 变量 | 说明 |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码，必填且需强随机 |
| `DB_PASSWORD` | 应用数据库账号密码 |
| `APP_DEV_MODE` | 必须设为 `false`，否则验证码会返回给前端 |
| `SMS_WEBHOOK_URL` / `SMS_WEBHOOK_TOKEN` | 短信网关 Webhook，生产必填 |
| `VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY` | Web Push 固定密钥，生产必填 |
| `VAPID_SUBJECT` | VAPID 联系地址，如 `mailto:ops@example.com` |
| `CORS_ALLOWED_ORIGINS` | 对外站点地址，如 `https://health.example.com` |
| `AUTH_COOKIE_SECURE` | HTTPS 部署必须设为 `true` |
| `WEB_PORT` | Nginx 映射到宿主机的端口，默认 8088 |

### 生成 VAPID 密钥

```bash
npx web-push generate-vapid-keys
```

把输出的 Public Key / Private Key 填入 `.env`。如果不配置，后端会在启动时生成临时密钥并在日志中告警，
**重启后所有浏览器推送订阅都会失效**，因此生产环境必须固定密钥。

### 短信 Webhook 约定

后端会向 `SMS_WEBHOOK_URL` 发送：

```http
POST /your-webhook
Authorization: Bearer <SMS_WEBHOOK_TOKEN>
Content-Type: application/json

{"phone":"13800138000","code":"123456","scene":"login"}
```

`scene` 取值：`login`（登录/重置密码）、`change-phone`（修改手机号）、`step-up`（敏感操作二次验证）。
网关返回任意 2xx 即视为发送成功；失败时接口返回 503。

## 2. 启动

```bash
docker compose up -d --build
docker compose ps
```

首次启动时 Flyway 会自动建表并写入基础数据。检查：

```bash
curl http://127.0.0.1:8088/           # 前端
docker compose exec api wget -qO- http://127.0.0.1:8080/actuator/health
```

## 3. HTTPS

推荐用 Caddy 自动申请证书，仓库提供了 `deploy/Caddyfile` 示例：

```bash
cp deploy/Caddyfile /etc/caddy/Caddyfile
# 修改域名后
caddy validate --config /etc/caddy/Caddyfile
systemctl reload caddy
```

确认 `.env` 中 `AUTH_COOKIE_SECURE=true`、`CORS_ALLOWED_ORIGINS=https://你的域名` 后重启 api。

## 4. 数据持久化与备份

- `mysql-data`：MySQL 数据目录
- `uploads-data`：用户头像

备份：

```bash
DB_PASSWORD=xxx ./healthcare-api/scripts/backup.sh
```

建议 crontab：

```cron
0 3 * * * cd /opt/healthcare && DB_PASSWORD=xxx ./healthcare-api/scripts/backup.sh >> /var/log/healthcare-backup.log 2>&1
```

恢复：

```bash
DB_PASSWORD=xxx ./healthcare-api/scripts/restore.sh backups/healthcare-YYYYMMDD-HHMMSS.sql.gz
```

注意：不要执行 `docker compose down -v`，会删除数据卷。

## 5. 升级与回滚

```bash
git pull
docker compose up -d --build
```

Flyway 会在启动时自动应用新迁移。升级前先备份数据库；回滚时切回旧版本代码重新构建，
如果新版本包含破坏性迁移，需要用备份恢复数据库。

## 6. 设备接入（MQTT）

后端提供通用 MQTT 接入层，设备和后端使用标准 MQTT 协议，数据格式兼容阿里云 Alink 物模型。

### 主题与数据格式

设备连接 broker 后，向以下主题发布属性上报：

```text
sys/{productKey}/{deviceName}/thing/event/property/post
```

Payload 示例：

```json
{
  "id": "1",
  "version": "1.0",
  "method": "thing.event.property.post",
  "params": {
    "steps": 4321,
    "waterMl": 1500,
    "sleepMinutes": 450,
    "weightKg": 61.5,
    "battery": 88
  }
}
```

支持的字段别名：`steps/stepCount`、`waterMl/water/drinkMl`、`sleepMinutes/sleep`、
`weightKg/weight`、`battery/batteryLevel`；未知字段会保留在原始事件表中。

### 本地联调（无真实设备）

1. 启动 MQTT broker（二选一）：

   ```bash
   # Docker 引擎可用时
   docker compose --profile iot up -d mqtt

   # 或者使用仓库内的轻量 Node broker
   cd ui-tools && npm install && node mqtt-broker.mjs 1883
   ```

2. 在 `.env` 中开启后端接入并重启 api：

   ```dotenv
   MQTT_ENABLED=true
   MQTT_BROKER_URL=tcp://mqtt:1883
   ```

3. 在 App「设备」页绑定设备，ProductKey / DeviceName 与上报主题一致。
4. 没有 MQTT 发布端时，开发环境可直接点设备卡片上的「模拟上报」，走同一套解析入库逻辑。
5. EMQX 控制台：`http://localhost:18083`（默认 admin / public，可通过 `EMQX_DASHBOARD_PASSWORD` 修改）。
6. 也可以用 MQTT 客户端模拟真实设备（先在 App 里绑定相同的 ProductKey / DeviceName）：

   ```bash
   cd ui-tools
   node iot-simulator.mjs pk-demo watch-001 mqtt://127.0.0.1:1883
   ```

### 接入真实设备 / 阿里云 MQTT 版

- 把 `MQTT_BROKER_URL` 改成实际 broker 地址（`tcp://` 或 `ssl://`），并配置 `MQTT_USERNAME` / `MQTT_PASSWORD`。
- 设备侧使用相同的主题和 Alink JSON 发布即可。
- 生产环境建议为每台设备配置独立账号，并在 broker 侧限制只能发布自己的主题。

> 注意：阿里云物联网平台（IoT 平台）自 2025 年 2 月 1 日起停止新购，存量实例仍可使用；
> 新项目可选用阿里云消息队列 MQTT 版、EMQX Cloud 或自建 EMQX。

## 7. 推送排查

1. 必须使用 HTTPS（`localhost` 例外）；浏览器地址栏需显示安全锁。
2. 「设置 → 消息通知」开启后，浏览器的通知权限必须是「允许」。
3. DevTools → Application → Service Workers，确认 `/sw.js` 已注册。
4. 设置页点击「发送测试通知」，后端日志应出现推送请求；没有订阅时提示「暂无可用订阅」。
5. 生产环境重启后推送失效，通常是 VAPID 密钥未固定（见第 1 节）。
6. iOS 需要 16.4+，并且必须「添加到主屏幕」后从主屏幕打开才能接收 Web Push。

## 8. 当前单实例限制

- 验证码、限流计数、闹钟推送去重都在进程内存中，**只能单实例部署**。
- 需要水平扩容时：验证码/限流迁移到 Redis；闹钟调度加分布式锁（如 ShedLock）；
  并把会话粘滞或改为无状态 JWT（当前为数据库 token，天然支持多实例，但验证码/限流仍需共享存储）。

## 9. 上线检查清单

- [ ] `APP_DEV_MODE=false`
- [ ] `MYSQL_ROOT_PASSWORD`、`DB_PASSWORD` 已改为强随机值
- [ ] `SMS_WEBHOOK_URL` 可用，验证码能真实收到
- [ ] `VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY` 已固定
- [ ] HTTPS 生效，`AUTH_COOKIE_SECURE=true`
- [ ] `CORS_ALLOWED_ORIGINS` 为真实域名
- [ ] 数据库和头像卷已挂载，备份任务已配置
- [ ] `/actuator/health` 返回 UP，容器健康检查通过
- [ ] 用真实手机号走通：登录 → 改手机号 → 开启通知 → 收到测试推送 → 注销账号
- [ ] MQTT 设备能连接 broker，绑定后上报可更新 App 健康数据
