import { describe, expect, it } from 'vitest'
import { isReasonableSleep, minutesToTime, sleepDuration, timeToMinutes } from './sleep'

describe('timeToMinutes', () => {
  it('解析 HH:mm', () => {
    expect(timeToMinutes('23:00')).toBe(1380)
    expect(timeToMinutes('07:30')).toBe(450)
  })

  it('拒绝非法时间', () => {
    expect(timeToMinutes('24:00')).toBeNull()
    expect(timeToMinutes('7:5')).toBeNull()
    expect(timeToMinutes('abc')).toBeNull()
  })
})

describe('minutesToTime', () => {
  it('格式化为两位 HH:mm 并支持跨天', () => {
    expect(minutesToTime(1380)).toBe('23:00')
    expect(minutesToTime(450)).toBe('07:30')
    expect(minutesToTime(1500)).toBe('01:00')
    expect(minutesToTime(-60)).toBe('23:00')
  })
})

describe('sleepDuration', () => {
  it('处理跨午夜与同日睡眠', () => {
    expect(sleepDuration(23 * 60, 7 * 60)).toBe(8 * 60)
    expect(sleepDuration(1 * 60, 8 * 60 + 30)).toBe(7 * 60 + 30)
    expect(sleepDuration(23 * 60, 23 * 60)).toBe(0)
  })
})

describe('isReasonableSleep', () => {
  it('限制在 1-16 小时', () => {
    expect(isReasonableSleep(8 * 60)).toBe(true)
    expect(isReasonableSleep(60)).toBe(true)
    expect(isReasonableSleep(59)).toBe(false)
    expect(isReasonableSleep(17 * 60)).toBe(false)
  })
})
