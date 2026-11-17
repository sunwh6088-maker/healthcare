import { describe, expect, it } from 'vitest'
import { validateBodyInfo } from './body'

describe('validateBodyInfo', () => {
  it('接受正常范围', () => {
    expect(validateBodyInfo({ weightKg: 60, heightCm: 175, age: 28 })).toBeNull()
  })

  it('拒绝异常体重', () => {
    expect(validateBodyInfo({ weightKg: 10, heightCm: 175, age: 28 })).toBe(
      '体重需在 20–300 公斤之间',
    )
    expect(validateBodyInfo({ weightKg: 400, heightCm: 175, age: 28 })).toBe(
      '体重需在 20–300 公斤之间',
    )
  })

  it('拒绝异常身高', () => {
    expect(validateBodyInfo({ weightKg: 60, heightCm: 20, age: 28 })).toBe(
      '身高需在 50–250 厘米之间',
    )
  })

  it('拒绝异常年龄', () => {
    expect(validateBodyInfo({ weightKg: 60, heightCm: 175, age: 0 })).toBe('年龄需在 1–120 岁之间')
    expect(validateBodyInfo({ weightKg: 60, heightCm: 175, age: 130 })).toBe(
      '年龄需在 1–120 岁之间',
    )
  })
})
