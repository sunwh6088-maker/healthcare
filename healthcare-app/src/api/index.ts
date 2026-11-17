import request from '../utils/request'

export interface User {
  id: number
  name: string
  phone: string
  email: string
  hasPassword: boolean
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
  token: string
  user: User
  newUser: boolean
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

  changePassword(oldPassword: string, newPassword: string) {
    return request<void>('/me/password', {
      method: 'PUT',
      body: JSON.stringify({ oldPassword: oldPassword || null, newPassword }),
    })
  },

  logout() {
    return request<void>('/auth/logout', { method: 'POST' })
  },

  me() {
    return request<User>('/me')
  },

  updateMe(data: { name?: string; phone?: string; email?: string }) {
    return request<User>('/me', {
      method: 'PATCH',
      body: JSON.stringify(data),
    })
  },

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

  listExerciseLogs() {
    return request<ExerciseLog[]>('/exercise-logs')
  },

  createExerciseLog(payload: { date: string; type: string; minutes: number; calories: number }) {
    return request<ExerciseLog>('/exercise-logs', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
}
