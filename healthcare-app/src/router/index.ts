// 模块：路由模块 —— 页面懒加载、登录守卫、页面标题和 404 兜底。

import { createRouter, createWebHashHistory } from 'vue-router'
import { hasSession } from '../utils/session'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    // 启动页
    { path: '/', name: 'splash', component: () => import('../views/SplashView.vue') },
    // 引导流程
    {
      path: '/onboarding',
      name: 'onboarding',
      component: () => import('../views/OnboardingView.vue'),
    },
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    { path: '/verify', name: 'verify', component: () => import('../views/VerifyCodeView.vue') },
    { path: '/gender', name: 'gender', component: () => import('../views/GenderView.vue') },
    { path: '/weight', name: 'weight', component: () => import('../views/WeightView.vue') },
    { path: '/body-info', name: 'body-info', component: () => import('../views/BodyInfoView.vue') },
    // 主界面（带 Tab 栏）
    {
      path: '/home',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
      meta: { tab: true },
    },
    {
      path: '/discover',
      name: 'discover',
      component: () => import('../views/DiscoverView.vue'),
      meta: { tab: true },
    },
    {
      path: '/devices',
      name: 'devices',
      component: () => import('../views/DevicesView.vue'),
      meta: { tab: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue'),
      meta: { tab: true },
    },
    // 健康详情
    { path: '/food', name: 'food', component: () => import('../views/FoodView.vue') },
    { path: '/sleep', name: 'sleep', component: () => import('../views/SleepView.vue') },
    { path: '/water', name: 'water', component: () => import('../views/WaterView.vue') },
    { path: '/exercise', name: 'exercise', component: () => import('../views/ExerciseView.vue') },
    {
      path: '/exercise/calendar',
      name: 'exercise-calendar',
      component: () => import('../views/ExerciseCalendarView.vue'),
    },
    { path: '/steps', name: 'steps', component: () => import('../views/StepsView.vue') },
    { path: '/history', name: 'history', component: () => import('../views/HistoryView.vue') },
    {
      path: '/search-food',
      name: 'search-food',
      component: () => import('../views/SearchFoodView.vue'),
    },
    { path: '/add-food', name: 'add-food', component: () => import('../views/AddFoodView.vue') },
    { path: '/settings', name: 'settings', component: () => import('../views/SettingsView.vue') },
    {
      path: '/account-security',
      name: 'account-security',
      component: () => import('../views/AccountSecurityView.vue'),
    },
    { path: '/help', name: 'help', component: () => import('../views/HelpView.vue') },
    { path: '/about', name: 'about', component: () => import('../views/AboutView.vue') },
    // 法律文本（未登录也可访问）
    {
      path: '/terms',
      name: 'terms',
      component: () => import('../views/LegalView.vue'),
      props: { doc: 'terms' },
    },
    {
      path: '/privacy',
      name: 'privacy',
      component: () => import('../views/LegalView.vue'),
      props: { doc: 'privacy' },
    },
    // 账户流程
    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: () => import('../views/ForgotPasswordView.vue'),
    },
    {
      path: '/change-password',
      name: 'change-password',
      component: () => import('../views/ChangePasswordView.vue'),
    },
    {
      path: '/reset-password',
      name: 'reset-password',
      component: () => import('../views/ChangePasswordView.vue'),
    },
    // 运动扩展
    {
      path: '/exercise-time',
      name: 'exercise-time',
      component: () => import('../views/ExerciseTimeView.vue'),
    },
    { path: '/set-goal', name: 'set-goal', component: () => import('../views/SetGoalView.vue') },
    {
      path: '/goal-tracking',
      name: 'goal-tracking',
      component: () => import('../views/GoalTrackingView.vue'),
    },
    // 404 兜底
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../views/NotFoundView.vue'),
    },
  ],
})

// 路由级页面标题
const pageTitles: Record<string, string> = {
  splash: '启动',
  onboarding: '欢迎',
  login: '登录',
  verify: '验证码',
  gender: '选择性别',
  weight: '体重',
  'body-info': '身体数据',
  home: '首页',
  discover: '发现',
  devices: '设备',
  profile: '我的',
  food: '食物',
  sleep: '睡眠',
  water: '饮水',
  steps: '步骤',
  exercise: '运动',
  'exercise-calendar': '运动日历',
  'exercise-time': '运动时间',
  'goal-tracking': '目标跟踪',
  'set-goal': '设定目标',
  'search-food': '搜索食物',
  'add-food': '添加食物',
  history: '历史',
  settings: '设置',
  'account-security': '账户与安全',
  help: '帮助与反馈',
  about: '关于我们',
  terms: '用户协议',
  privacy: '隐私政策',
  'not-found': '页面不存在',
  'forgot-password': '忘记密码',
  'change-password': '更改密码',
  'reset-password': '重置密码',
}

router.afterEach((to) => {
  const title = pageTitles[String(to.name)]
  document.title = title ? `${title} · 健康追踪` : '健康追踪'
})

// 未登录时只允许访问启动/引导/登录/验证码页
const publicRoutes = new Set([
  'splash',
  'onboarding',
  'login',
  'verify',
  'forgot-password',
  'reset-password',
  'terms',
  'privacy',
  'not-found',
])
router.beforeEach((to) => {
  if (!hasSession() && !publicRoutes.has(String(to.name))) {
    return { name: 'login' }
  }
  return true
})

export default router
