import { describe, expect, it } from 'vitest'
import { formatPhone, isValidEmail, isValidPhone, maskPhone, normalizePhone } from './validation'

describe('isValidPhone', () => {
  it('接受 11 位国内手机号，允许空格与 +86 前缀', () => {
    expect(isValidPhone('13900001111')).toBe(true)
    expect(isValidPhone('139 0000 1111')).toBe(true)
    expect(isValidPhone('19912345678')).toBe(true)
    expect(isValidPhone('+86 138 0013 8000')).toBe(true)
    expect(isValidPhone('8613800138000')).toBe(true)
  })

  it('拒绝非国内手机号格式', () => {
    expect(isValidPhone('123')).toBe(false)
    expect(isValidPhone('138abc')).toBe(false)
    expect(isValidPhone('12345678901')).toBe(false) // 第二位非 3-9
    expect(isValidPhone('12000000000')).toBe(false)
    expect(isValidPhone('1380000111')).toBe(false) // 10 位
    expect(isValidPhone('138000011112')).toBe(false) // 12 位
    expect(isValidPhone('+1 234 567 8901')).toBe(false) // 美国号码
  })
})

describe('normalizePhone', () => {
  it('去除空格并剥离 +86 前缀', () => {
    expect(normalizePhone('+86 138 0013 8000')).toBe('13800138000')
    expect(normalizePhone('138-0013-8000')).toBe('13800138000')
  })
})

describe('formatPhone', () => {
  it('按 3-4-4 分组且最多保留 11 位', () => {
    expect(formatPhone('13800138000')).toBe('138 0013 8000')
    expect(formatPhone('1380013')).toBe('138 0013')
    expect(formatPhone('+86 13800138000')).toBe('138 0013 8000')
    expect(formatPhone('13800138000999')).toBe('138 0013 8000')
  })
})

describe('isValidEmail', () => {
  it('接受常规邮箱', () => {
    expect(isValidEmail('jessie@example.com')).toBe(true)
    expect(isValidEmail('a.b+c@mail.co')).toBe(true)
  })

  it('拒绝缺少 @ 或域名后缀的输入', () => {
    expect(isValidEmail('jessie.example.com')).toBe(false)
    expect(isValidEmail('jessie@example')).toBe(false)
    expect(isValidEmail('jessie @example.com')).toBe(false)
  })
})

describe('maskPhone', () => {
  it('国内号码保留前三位与后四位', () => {
    expect(maskPhone('139 0000 1111')).toBe('139 **** 1111')
  })

  it('过短号码原样返回', () => {
    expect(maskPhone('123')).toBe('123')
  })
})
