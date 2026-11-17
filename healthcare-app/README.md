# 健康追踪 · 健康管理 APP 高保真原型

基于设计稿「02-36套-APP UI设计 / 01-医疗保健」还原的可交互移动端原型，使用 Vue 3 + TypeScript + Vite 实现。全站中文界面，覆盖设计稿全部 50 个画板，共 27 个页面。

## 技术栈

- **Vue 3.5**（`<script setup>` 单文件组件）
- **TypeScript**（`vue-tsc` 严格类型检查）
- **Vite 8**（开发服务器 / 构建）
- **vue-router 5**（Hash 路由，页面全部懒加载）
- 后端：**Spring Boot 3.5 + MySQL 8**（项目：`../healthcare-api`）
- 无状态管理库、无 UI 组件库；样式使用原生 CSS + 设计令牌

## 快速开始

先启动数据库和后端（详见 `../healthcare-api/README.md`）：

```bash
# 1) 初始化 MySQL（首次，详见 ../healthcare-api/README.md）
# 2) Spring Boot 后端（默认 8080；端口被占用时可加 --server.port=8081）
cd ../healthcare-api
mvn spring-boot:run
```

再启动前端：

```bash
npm install
npm run dev      # 开发预览，默认 http://localhost:5173（端口占用时自动换 5174/5175）
npm run build    # 类型检查 + 生产构建，输出到 dist/
npm run preview  # 预览生产构建
npm run lint     # ESLint 检查（vue + typescript）
npm run format   # Prettier 格式化整个项目
npm test         # Vitest 单元测试
npm run check    # 类型检查 + ESLint + Prettier 校验（CI 同款）
```

桌面浏览器下页面居中显示为 390×844 手机壳；宽度 ≤640px 时自动铺满全屏，方便真机体验。

## 目录结构

```
healthcare-app/
├─ index.html                  # 入口 HTML
├─ vite.config.ts              # Vite 配置
├─ tsconfig*.json              # TypeScript 工程配置
├─ eslint.config.js            # ESLint 扁平配置（Vue + TypeScript）
├─ .prettierrc.json            # Prettier 格式配置
├─ vitest.config.ts            # Vitest 配置（jsdom 环境）
├─ .mcp.json                   # 即时设计 jsdesign MCP 配置
├─ public/
│  └─ favicon.svg              # 站点图标
├─ src/
│  ├─ main.ts                  # 应用入口：全局组件注册、$toast、路由挂载
│  ├─ App.vue                  # 手机外框、页面过渡、Tab 栏条件渲染、滚动复位
│  ├─ style.css                # 设计令牌 + 全局工具类
│  ├─ api/index.ts             # 后端 API 封装（认证 / 账号 / 健康数据）
│  ├─ router/index.ts          # 28 条路由（懒加载）+ 404 兜底 + 登录守卫
│  ├─ components/
│  │  ├─ StatusBar.vue         # 状态栏（支持深色页面）
│  │  ├─ NavBar.vue            # 顶部导航（返回 / 标题 / 右侧插槽）
│  │  ├─ AppTabBar.vue         # 底部 Tab 栏（数据驱动）
│  │  ├─ BackButton.vue        # 圆形返回按钮
│  │  ├─ AppSwitch.vue         # 通用开关（v-model）
│  │  ├─ SegmentedControl.vue  # 分段选择器（v-model）
│  │  ├─ BottomSheet.vue       # 通用底部弹层（Teleport 到手机壳）
│  │  └─ NumericKeypad.vue     # 数字键盘
│  ├─ mock/
│  │  ├─ foods.ts              # 食物 mock 数据（发现 / 搜索 / 添加共用）
│  │  └─ exercise.ts           # 运动类型与历史记录 mock 数据
│  ├─ utils/
│  │  ├─ toast.ts              # 全局轻提示
│  │  ├─ healthStore.ts        # 全局健康数据（响应式 + localStorage 持久化）
│  │  ├─ alarm.ts              # 闹钟重复文案（含单测）
│  │  ├─ validation.ts         # 手机号 / 邮箱校验与脱敏（含单测）
│  │  ├─ session.ts            # 登录态标记 / 待登录手机号 / 开发验证码
│  │  └─ request.ts            # fetch 请求封装（自动携带 Cookie、401 处理）
│  └─ views/                   # 27 个页面
└─ dist/                       # 构建产物（已 gitignore）
```

## 页面与路由

### 启动与引导

| 路由          | 页面           | 说明                                       |
| ------------- | -------------- | ------------------------------------------ |
| `/`           | SplashView     | 启动页，1.6 秒后进入引导                   |
| `/onboarding` | OnboardingView | 4 页功能引导                               |
| `/login`      | LoginView      | 手机号输入                                 |
| `/verify`     | VerifyCodeView | 验证码 + 倒计时重发                        |
| `/gender`     | GenderView     | 性别选择                                   |
| `/weight`     | WeightView     | 体重转盘（公斤 / 磅）                      |
| `/body-info`  | BodyInfoView   | 体重 / 身高 / 年龄填写（引导与我的页共用） |

### 主 Tab（`meta.tab` 控制底部栏）

| 路由        | 页面         | 说明                                                           |
| ----------- | ------------ | -------------------------------------------------------------- |
| `/home`     | HomeView     | 步数进度 + 食物 / 睡眠 / 水 / 运动数据卡（数据来自全局 store） |
| `/discover` | DiscoverView | 食物发现与筛选                                                 |
| `/devices`  | DevicesView  | 设备管理与数据同步                                             |
| `/profile`  | ProfileView  | 个人资料（姓名 / 体重 / 身高 / 年龄来自 store）与功能入口      |

### 健康详情

| 路由                 | 页面                 | 说明                                                                                |
| -------------------- | -------------------- | ----------------------------------------------------------------------------------- |
| `/food`              | FoodView             | 热量圆环、三餐、卡路里图表                                                          |
| `/sleep`             | SleepView            | 睡眠时长、就寝 / 醒来、闹钟（可新增 / 编辑时间、标签、按星期重复 / 删除）、助眠声音 |
| `/water`             | WaterView            | 饮水记录与份量选择                                                                  |
| `/steps`             | StepsView            | 步数日 / 周 / 月视图，切换时图表、轴和统计同步变化                                  |
| `/exercise`          | ExerciseView         | 运动类型、课程、「记录 30 分钟运动」、目标跟踪入口                                  |
| `/exercise/calendar` | ExerciseCalendarView | 运动日历与活跃时间                                                                  |
| `/exercise-time`     | ExerciseTimeView     | 运动时间日 / 周视图、日周月切换                                                     |
| `/goal-tracking`     | GoalTrackingView     | 活动目标进度（由活跃时间实时计算）/ 历史记录                                        |
| `/set-goal`          | SetGoalView          | 步数或活动时间目标（`?type=steps\|active`）                                         |

### 食物流程

| 路由           | 页面           | 说明                                         |
| -------------- | -------------- | -------------------------------------------- |
| `/search-food` | SearchFoodView | 食物搜索与实时过滤，选中后带食物名进入添加页 |
| `/add-food`    | AddFoodView    | 选择餐次/日期添加食物，写入后端食物记录      |

### 记录与账户

| 路由                | 页面                | 说明                                                          |
| ------------------- | ------------------- | ------------------------------------------------------------- |
| `/history`          | HistoryView         | 读取后端运动记录，全部 / 星期 / 月份筛选                      |
| `/settings`         | SettingsView        | 设置、通知 / 隐私开关、退出登录                               |
| `/account-security` | AccountSecurityView | 账号信息（手机号 / 邮箱可修改）、安全设置、登录设备、注销入口 |
| `/forgot-password`  | ForgotPasswordView  | 忘记密码（手机号 → 验证码）                                   |
| `/change-password`  | ChangePasswordView  | 修改/首次设置密码（已接后端）                                 |
| `/reset-password`   | ChangePasswordView  | 忘记密码验证码通过后重置密码，并自动登录                      |

## 质量检查

- `npm run check`：`vue-tsc` 类型检查 + ESLint + Prettier 格式校验
- `npm test`：Vitest（6 个测试文件、23 个用例），覆盖：
  - `healthStore` 默认值、localStorage 持久化、旧数据迁移、步数估算；
  - 闹钟重复文案（仅一次 / 每天 / 工作日 / 周末 / 自定义排序）；
  - 手机号 / 邮箱校验与手机号脱敏；
  - 返回逻辑（有历史返回 / 无历史 fallback）；
  - Toast 挂载位置与自动消失；
  - 体重 / 身高 / 年龄范围校验。
- GitHub Actions（仓库根目录 `.github/workflows/frontend-ci.yml`）在 push / PR 时执行 `npm ci` → `check` → `test` → `build`。
- 路由切换会同步更新浏览器标签标题，如「首页 · 健康追踪」。

## 设计规范

- **画板尺寸**：390 × 844
- **主色**：`#2a6049`（深绿）；**强调色**：`#ec744a`（橙）
- **辅助色**：`#655eb0` 紫 / `#f8c060` 黄 / `#bacac2` 灰绿
- **中性色**：`#140c07` 主文字 / `#bfc0c0` 次要文字 / `#ececec` 分割线
- **字体**：SF Pro Display（数字英文）+ PingFang SC（中文）
- 设计令牌统一在 `src/style.css` 的 `:root` 中定义（颜色、字号、行高、圆角、阴影）

## 状态与数据

- 登录后从后端拉取账号与健康数据；未登录只能访问启动/引导/登录/验证码页。
- 登录 token 由后端写入 HttpOnly Cookie，前端不保存 token，只保留一个非敏感的已登录标记；请求自动携带 Cookie。生产环境请启用 HTTPS 并设置 `AUTH_COOKIE_SECURE=true`。
- 开发模式下后端 `send-code` 会返回 `devCode`，前端自动填充验证码并在页面上显示；生产环境应关闭 `APP_DEV_MODE` 并配置 `SMS_WEBHOOK_URL`，验证码不再返回给前端。
- 后端对验证码发送（60 秒冷却、每小时 10 次）和登录尝试（10 分钟 10 次）做了进程内限流；多实例部署时应改为 Redis 等共享存储。
- 通用 mock 数据集中在 `src/mock/`，发现页、搜索页、添加食物页共用同一份食物数据。
- `src/utils/healthStore.ts` 用一个响应式对象保存用户资料与健康数据，本地写 `localStorage` 缓存，并在变更后 600ms 防抖同步到后端：
  - 用户：姓名、性别、年龄、身高、体重、手机号、邮箱；
  - 健康：步数 / 目标、饮水 / 目标、食物热量、运动次数、活跃时间 / 目标、睡眠时长与就寝时间、闹钟；
  - 首页、我的、账户页共用同一姓名；引导页性别、体重、身高、年龄写入 store，我的页「身体数据」可点击编辑；
  - 添加食物会累加食物热量，首页 / 食物页 / 我的页同步更新；
  - 「记录 30 分钟运动」会累加活跃时间与运动次数，首页、目标跟踪、运动时间、运动日历同步更新；
  - 设定目标后，步骤页、运动时间页、运动日历页同步更新；
  - 睡眠页的就寝 / 醒来时间由「就寝时间 + 睡眠时长」推导，不再写死；
  - 闹钟的开关、新增、编辑时间 / 标签 / 重复星期、删除都会持久化；
  - 食物记录按「餐次 + 日期」写入 `food_logs`，今日总热量回写 `food_kcal`；
  - 运动记录写入 `exercise_logs`，历史页实时读取；
  - 账号支持昵称、手机号、邮箱修改，以及密码登录 / 修改 / 验证码重置；
  - 退出登录会同时清除服务端 token 与 HttpOnly Cookie；
  - 账户页修改的手机号与邮箱也会持久化；
  - 设置页的通知 / 隐私开关、设备连接 / 添加状态也会持久化。
- 距离与卡路里为演示估算：步数 × 0.7 米 / 步、步数 × 0.04 千卡 / 步。
- 测试或需要恢复初始数据时，清除浏览器 `localStorage` 中的 `healthcare-app-state` 即可。

## 部署

仓库根目录提供 `docker-compose.yml`（MySQL + Spring Boot + Nginx 托管构建产物）。复制 `.env.example` 为 `.env`，至少设置 `MYSQL_ROOT_PASSWORD`，然后：

```bash
docker compose up --build
```

默认访问 `http://localhost:8088`。生产环境请设置 `APP_DEV_MODE=false`、`AUTH_COOKIE_SECURE=true`，并把 `CORS_ALLOWED_ORIGINS` 改成实际域名。

## 说明

- 账号、健康数据、闹钟、食物记录、运动记录均已接后端；仅食物目录（发现 / 搜索）、运动课程、设备同步、助眠声音和步数仍为演示数据。
- 设计源文件为 `医疗保健APP.sketch`（比预览 PNG 更权威），解析结果位于 `ui-tools/sketch-extract`。
- 无目标页面的按钮统一使用全局 `$toast` 轻提示作为占位交互；Toast 挂在手机壳内，桌面端不会跑到窗口底部。
- 返回按钮使用 `goBack()`：有历史记录时返回上一页，直接打开内页时回退到对应页面（如设置 → 我的、账户安全 → 设置）。
- 底部弹层（闹钟 / 账户编辑）支持点击遮罩、按 Esc 关闭，打开时自动聚焦第一个输入框。
- 验证码默认留空；助眠声音可播放 / 暂停；纯图标按钮均补了 `aria-label`。
- 代码规范：ESLint（Vue + TypeScript 扁平配置）+ Prettier；提交前可运行 `npm run lint` 与 `npm run format:check`。
- 后端已实现验证码登录、密码登录 / 修改 / 重置、账号资料、健康数据与闹钟同步；「忘记密码 / 更改密码」已接后端，验证码重置成功后会直接登录。
- 尚未实现：账户注销（当前为占位提示）、真实设备同步与通知推送；生产部署前需关闭 `APP_DEV_MODE`、接入短信服务并收紧 CORS。
