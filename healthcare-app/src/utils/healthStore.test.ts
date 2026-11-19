import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'

const STORAGE_KEY = 'healthcare-app-state'

describe('healthStore', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.resetModules()
  })

  it('提供默认健康数据', async () => {
    const { health } = await import('./healthStore')
    expect(health.userName).toBe('健康用户')
    expect(health.steps).toBe(0)
    expect(health.stepsGoal).toBe(6000)
    expect(health.foodKcal).toBe(0)
    expect(health.alarms).toHaveLength(2)
    expect(health.notifyEnabled).toBe(false)
    expect(health.deviceConnected).toBe(false)
  })

  it('修改后写入 localStorage', async () => {
    const { health } = await import('./healthStore')
    health.foodKcal += 100
    await nextTick()
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
    expect(saved.foodKcal).toBe(100)
  })

  it('兼容旧版本没有 repeat 字段的闹钟数据', async () => {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        alarms: [{ id: 9, time: '08:00', label: '旧闹钟', on: true }],
      }),
    )
    const { health } = await import('./healthStore')
    expect(health.alarms[0].repeat).toEqual([])
  })

  it('按步数估算距离与卡路里', async () => {
    const { stepsToKm, stepsToKcal } = await import('./healthStore')
    expect(stepsToKm(4204)).toBe('2.9')
    expect(stepsToKcal(4204)).toBe(168)
  })
})
