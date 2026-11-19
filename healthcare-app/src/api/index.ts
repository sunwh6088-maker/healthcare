// 模块：API 层 —— 封装后端接口和统一类型定义。

import request from '../utils/request'

export interface User {
  id: number
  name: string
  phone: string
  email: string
  hasPassword: boolean
  avatarUrl: string | null
}

export interface HealthDto {
  steps: number
  stepsGoal: number
  waterMl: number
  waterGoalMl: number
  activeMinutes: number
  activeGoalMinutes: number
  sleepMinutes: number
  sleepStartMinutes: number
  foodKcal: number
  exerciseCount: number
  notifyEnabled: boolean
  privacyEnabled: boolean
  deviceConnected: boolean
  deviceAdded: boolean
  deviceBattery: number
  deviceLastSyncAt: string
  gender: 'female' | 'male' | null
  age: number
  heightCm: number
  weightKg: number
}

export interface AlarmDto {
  id?: number
  time: string
  label: string
  repeat: number[]
  on: boolean
}

export interface StateDto {
  health: HealthDto
  alarms: AlarmDto[]
}

export interface LoginResponse {
  // token 由后端写入 HttpOnly Cookie，前端不接触
  user: User
  newUser: boolean
}

export interface PhoneCodeResponse {
  message: string
  devCode?: string
}

export interface LoginSession {
  id: number
  device: string
  ip: string | null
  createdAt: string
  expiresAt: string
  current: boolean
}

export interface PushKeys {
  p256dh: string
  auth: string
}

export interface PushSubscriptionPayload {
  endpoint: string
  keys: PushKeys
}

export interface IotDevice {
  id: number
  productKey: string
  deviceName: string
  name: string
  connected: boolean
  battery: number
  lastSeenAt: string | null
}

export interface SendCodeResponse {
  message: string
  devCode?: string
}

export interface FoodLog {
  id: number
  date: string
  meal: string
  name: string
  portion: string
  calories: number
  emoji: string
}

export interface FoodLogSummary {
  date: string
  logs: FoodLog[]
  totalCalories: number
}

export interface CreateFoodLogResponse {
  log: FoodLog
  todayTotalCalories: number
}

export interface ExerciseLog {
  id: number
  date: string
  type: string
  minutes: number
  calories: number
}

export interface CatalogFood {
  id: number
  name: string
  portion: string
  calories: number
  emoji: string
  category: 'main' | 'snack'
  custom: boolean
}

export interface ExerciseType {
  key: string
  name: string
  emoji: string
  bg: string
  bar: string
  kcal: number
}

export interface ExerciseLesson {
  id: number
  typeKey: string
  title: string
  level: string
  minutes: number
  calories: number
  emoji: string
}

export interface SleepSound {
  id: number
  name: string
  emoji: string
  audioUrl: string | null
}

export interface DeviceItem {
  id: number
  name: string
  icon: string
  capabilities: string
  primary: boolean
}

export interface WaterPortion {
  id: number
  name: string
  sizeLabel: string
  emoji: string
  ml: number
}

export interface MealType {
  key: string
  label: string
  emoji: string
}

export interface CatalogResponse {
  foods: CatalogFood[]
  exerciseTypes: ExerciseType[]
  exerciseLessons: ExerciseLesson[]
  sleepSounds: SleepSound[]
  devices: DeviceItem[]
  waterPortions: WaterPortion[]
  meals: MealType[]
  recentFoodNames: string[]
  frequentFoodNames: string[]
}

export const api = {
  // 认证：验证码 / 密码登录 / 重置密码 / 退出登录
  sendCode(phone: string) {
    return request<SendCodeResponse>('/auth/send-code', {
      method: 'POST',
      body: JSON.stringify({ phone }),
    })
  },

  login(phone: string, code: string) {
    return request<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ phone, code }),
    })
  },

  loginPassword(phone: string, password: string) {
    return request<LoginResponse>('/auth/login-password', {
      method: 'POST',
      body: JSON.stringify({ phone, password }),
    })
  },

  resetPassword(phone: string, code: string, newPassword: string) {
    return request<LoginResponse>('/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify({ phone, code, newPassword }),
    })
  },

  changePassword(oldPassword: string, newPassword: string, currentCode?: string) {
    return request<void>('/me/password', {
      method: 'PUT',
      body: JSON.stringify({
        oldPassword: oldPassword || null,
        currentCode: currentCode || null,
        newPassword,
      }),
    })
  },

  logout() {
    return request<void>('/auth/logout', { method: 'POST' })
  },

  // 账号资料
  me() {
    return request<User>('/me')
  },

  updateMe(data: { name?: string; email?: string }) {
    return request<User>('/me', {
      method: 'PATCH',
      body: JSON.stringify(data),
    })
  },

  // 修改手机号：必须通过新号码的短信验证码
  sendPhoneChangeCode(phone: string) {
    return request<PhoneCodeResponse>('/me/phone/send-code', {
      method: 'POST',
      body: JSON.stringify({ phone }),
    })
  },

  updatePhone(phone: string, code: string, currentPassword?: string, currentCode?: string) {
    return request<User>('/me/phone', {
      method: 'PUT',
      body: JSON.stringify({
        phone,
        code,
        currentPassword: currentPassword || null,
        currentCode: currentCode || null,
      }),
    })
  },

  // 敏感操作前向当前手机号发送验证码
  sendStepUpCode() {
    return request<PhoneCodeResponse>('/me/step-up/send-code', { method: 'POST' })
  },

  uploadAvatar(file: File) {
    const form = new FormData()
    form.append('file', file)
    return request<User>('/me/avatar', { method: 'POST', body: form })
  },

  deleteAvatar() {
    return request<User>('/me/avatar', { method: 'DELETE' })
  },

  deleteMe(payload: { password?: string; code?: string } = {}) {
    return request<void>('/me', {
      method: 'DELETE',
      body: JSON.stringify(payload),
    })
  },

  // 登录设备
  listSessions() {
    return request<LoginSession[]>('/me/sessions')
  },

  revokeSession(id: number) {
    return request<void>(`/me/sessions/${id}`, { method: 'DELETE' })
  },

  revokeOtherSessions() {
    return request<void>('/me/sessions', { method: 'DELETE' })
  },

  // Web Push
  getPushPublicKey() {
    return request<{ publicKey: string }>('/push/public-key')
  },

  subscribePush(payload: PushSubscriptionPayload) {
    return request<void>('/push/subscribe', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  unsubscribePush(endpoint: string) {
    return request<void>('/push/unsubscribe', {
      method: 'POST',
      body: JSON.stringify({ endpoint }),
    })
  },

  sendTestPush() {
    return request<number>('/push/test', { method: 'POST' })
  },

  // IoT 设备
  listIotDevices() {
    return request<IotDevice[]>('/iot/devices')
  },

  bindIotDevice(payload: { productKey: string; deviceName: string; name?: string }) {
    return request<IotDevice>('/iot/devices', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  unbindIotDevice(id: number) {
    return request<void>(`/iot/devices/${id}`, { method: 'DELETE' })
  },

  simulateIotDevice(id: number) {
    return request<IotDevice>(`/iot/devices/${id}/simulate`, { method: 'POST' })
  },

  /** 导出账号、健康状态、食物与运动记录（JSON）。 */
  exportMyData() {
    return request<Record<string, unknown>>('/me/export')
  },

  // 基础数据 + 健康状态
  getCatalog() {
    return request<CatalogResponse>('/catalog')
  },

  getState() {
    return request<StateDto>('/state')
  },

  saveState(state: StateDto) {
    return request<StateDto>('/state', {
      method: 'PUT',
      body: JSON.stringify(state),
    })
  },

  // 食物记录
  listFoodLogs(date: string) {
    return request<FoodLogSummary>(`/food-logs?date=${encodeURIComponent(date)}`)
  },

  createFoodLog(payload: {
    date: string
    meal: string
    name: string
    portion: string
    calories: number
    emoji: string
  }) {
    return request<CreateFoodLogResponse>('/food-logs', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  deleteFoodLog(id: number) {
    return request<void>(`/food-logs/${id}`, { method: 'DELETE' })
  },

  // 运动记录
  listExerciseLogs() {
    return request<ExerciseLog[]>('/exercise-logs')
  },

  createExerciseLog(payload: { date: string; type: string; minutes: number; calories: number }) {
    return request<ExerciseLog>('/exercise-logs', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  updateExerciseLog(
    id: number,
    payload: { date: string; type: string; minutes: number; calories: number },
  ) {
    return request<ExerciseLog>(`/exercise-logs/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  deleteExerciseLog(id: number) {
    return request<void>(`/exercise-logs/${id}`, { method: 'DELETE' })
  },
}
