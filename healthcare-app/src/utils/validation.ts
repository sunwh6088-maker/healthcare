// 模块：表单校验 —— 手机号、邮箱校验与脱敏。

// 账号信息校验与脱敏
export function isValidPhone(value: string): boolean {
  return /^\d{7,15}$/.test(value.replace(/\s/g, ''))
}

export function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
}

export function maskPhone(value: string): string {
  const digits = value.replace(/\s/g, '')
  return digits.length >= 7 ? `${digits.slice(0, 4)} **** ${digits.slice(-3)}` : value
}
