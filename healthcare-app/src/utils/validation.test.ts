import { describe, expect, it } from 'vitest'
import { isValidEmail, isValidPhone, maskPhone } from './validation'

describe('isValidPhone', () => {
  it('接受 7-15 位数字，允许空格', () => {
    expect(isValidPhone('13900001111')).toBe(true)
    expect(isValidPhone('139 0000 1111')).toBe(true)
  })

  it('拒绝过短或包含字母', () => {
    expect(isValidPhone('123')).toBe(false)
    expect(isValidPhone('138abc')).toBe(false)
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
  it('保留前四位与后三位', () => {
    expect(maskPhone('139 0000 1111')).toBe('1390 **** 111')
  })

  it('过短号码原样返回', () => {
    expect(maskPhone('123')).toBe('123')
  })
})
