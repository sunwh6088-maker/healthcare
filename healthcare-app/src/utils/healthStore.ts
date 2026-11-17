// 模块：健康数据仓库 —— 全局响应式状态、本地缓存和后端防抖同步。

import { nextTick, reactive, watch } from 'vue'
import { api, type StateDto, type User } from '../api'
import { hasSession } from './session'

// 轻量全局健康数据：首页、步骤、饮水、目标、睡眠等页面共用，并持久化到 localStorage
export interface Alarm {
  id: number
  time: string
  label: string
  /** 重复的星期：0 = 周日，1 = 周一 … 6 = 周六；空数组表示仅一次 */
  repeat: number[]
  on: boolean
}

export interface HealthState {
  userName: string
  gender: 'female' | 'male' | null
  age: number
  heightCm: number
  weightKg: number
  foodKcal: number
  exerciseCount: number
  steps: number
  stepsGoal: number
  waterMl: number
  waterGoalMl: number
  activeMinutes: number
  activeGoalMinutes: number
  sleepMinutes: number
  /** 就寝时间（从 0 点起的分钟数），默认 23:00 */
  sleepStartMinutes: number
  alarms: Alarm[]
  accountPhone: string
  accountEmail: string
  notifyEnabled: boolean
  privacyEnabled: boolean
  deviceConnected: boolean
  deviceAdded: boolean
  deviceBattery: number
  deviceLastSyncAt: string
}

const STORAGE_KEY = 'healthcare-app-state'

// ===== 默认值 =====
const defaults: HealthState = {
  userName: '杰西·比伯',
  gender: null,
  age: 22,
  heightCm: 160,
  weightKg: 60,
  foodKcal: 1284,
  exerciseCount: 13,
  steps: 4204,
  stepsGoal: 6000,
  waterMl: 900,
  waterGoalMl: 2000,
  activeMinutes: 0,
  activeGoalMinutes: 60,
  sleepMinutes: 500,
  sleepStartMinutes: 23 * 60,
  alarms: [
    { id: 1, time: '08:30', label: '工作日', repeat: [1, 2, 3, 4, 5], on: true },
    { id: 2, time: '09:00', label: '周末', repeat: [0, 6], on: false },
  ],
  accountPhone: '1234 5678 789',
  accountEmail: 'jessie@example.com',
  notifyEnabled: true,
  privacyEnabled: true,
  deviceConnected: true,
  deviceAdded: false,
  deviceBattery: 86,
  deviceLastSyncAt: new Date().toISOString(),
}

function normalizeAlarms(alarms: Alarm[]): Alarm[] {
  return alarms.map((a) => ({
    ...a,
    // 兼容旧版本没有 repeat 字段的本地数据
    repeat: Array.isArray(a.repeat) ? [...a.repeat] : [],
  }))
}

function loadState(): HealthState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const saved = JSON.parse(raw) as Partial<HealthState>
      return {
        ...defaults,
        ...saved,
        alarms: normalizeAlarms(saved.alarms ?? defaults.alarms),
        accountPhone: saved.accountPhone ?? defaults.accountPhone,
        accountEmail: saved.accountEmail ?? defaults.accountEmail,
      }
    }
  } catch {
    // 本地数据损坏时回退到默认值
  }
  return { ...defaults, alarms: normalizeAlarms(defaults.alarms) }
}

export const health = reactive<HealthState>(loadState())

let applyingRemote = false
let remoteTimer: ReturnType<typeof setTimeout> | null = null

// ===== 本地状态 → 后端 DTO =====
function toServerState(): StateDto {
  return {
    health: {
      steps: health.steps,
      stepsGoal: health.stepsGoal,
      waterMl: health.waterMl,
      waterGoalMl: health.waterGoalMl,
      activeMinutes: health.activeMinutes,
      activeGoalMinutes: health.activeGoalMinutes,
      sleepMinutes: health.sleepMinutes,
      sleepStartMinutes: health.sleepStartMinutes,
      foodKcal: health.foodKcal,
      exerciseCount: health.exerciseCount,
      notifyEnabled: health.notifyEnabled,
      privacyEnabled: health.privacyEnabled,
      deviceConnected: health.deviceConnected,
      deviceAdded: health.deviceAdded,
      deviceBattery: health.deviceBattery,
      deviceLastSyncAt: health.deviceLastSyncAt,
      gender: health.gender,
      age: health.age,
      heightCm: health.heightCm,
      weightKg: health.weightKg,
    },
    alarms: health.alarms.map((alarm) => ({
      time: alarm.time,
      label: alarm.label,
      repeat: alarm.repeat,
      on: alarm.on,
    })),
  }
}

function applyServerState(state: StateDto) {
  const h = state.health
  health.steps = h.steps
  health.stepsGoal = h.stepsGoal
  health.waterMl = h.waterMl
  health.waterGoalMl = h.waterGoalMl
  health.activeMinutes = h.activeMinutes
  health.activeGoalMinutes = h.activeGoalMinutes
  health.sleepMinutes = h.sleepMinutes
  health.sleepStartMinutes = h.sleepStartMinutes
  health.foodKcal = h.foodKcal
  health.exerciseCount = h.exerciseCount
  health.notifyEnabled = h.notifyEnabled
  health.privacyEnabled = h.privacyEnabled
  health.deviceConnected = h.deviceConnected
  health.deviceAdded = h.deviceAdded
  health.deviceBattery = h.deviceBattery
  health.deviceLastSyncAt = h.deviceLastSyncAt
  health.gender = h.gender === 'female' || h.gender === 'male' ? h.gender : null
  health.age = h.age
  health.heightCm = h.heightCm
  health.weightKg = h.weightKg
  health.alarms = state.alarms.map((alarm, index) => ({
    id: alarm.id ?? Date.now() + index,
    time: alarm.time,
    label: alarm.label,
    repeat: alarm.repeat ?? [],
    on: alarm.on,
  }))
}

// ===== 远端同步：变更后 600ms 防抖保存 =====
function scheduleRemoteSave() {
  if (!hasSession() || applyingRemote) return
  if (remoteTimer) clearTimeout(remoteTimer)
  remoteTimer = setTimeout(() => {
    remoteTimer = null
    api.saveState(toServerState()).catch((error) => {
      console.warn('健康数据同步失败', error)
    })
  }, 600)
}

watch(
  health,
  (state) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
    } catch {
      // 隐私模式等场景下写入失败时忽略，不影响页面使用
    }
    scheduleRemoteSave()
  },
  { deep: true },
)

/** 登录后从后端拉取账号与健康数据，覆盖本地缓存 */
export async function syncFromServer(): Promise<User | null> {
  if (!hasSession()) return null
  const [user, state] = await Promise.all([api.me(), api.getState()])
  applyingRemote = true
  health.userName = user.name
  health.accountPhone = user.phone
  health.accountEmail = user.email ?? ''
  applyServerState(state)
  await nextTick()
  applyingRemote = false
  return user
}

/** 退出登录时取消待发送的同步任务 */
export function stopRemoteSync() {
  if (remoteTimer) {
    clearTimeout(remoteTimer)
    remoteTimer = null
  }
}

// ===== 工具函数 =====
// 演示用估算：平均步幅约 0.7 米、每步约 0.04 千卡
export function stepsToKm(steps: number): string {
  return (steps * 0.0007).toFixed(1)
}

export function stepsToKcal(steps: number): number {
  return Math.round(steps * 0.04)
}
