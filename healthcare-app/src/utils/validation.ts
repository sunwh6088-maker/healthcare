// 模块：表单校验 —— 手机号、邮箱校验与脱敏。

// 中国大陆手机号：11 位，1 开头，第二位 3-9
const MAINLAND_PHONE_RE = /^1[3-9]\d{9}$/

/** 去掉空格、连字符、括号及开头的 +86/86，返回纯号码。 */
export function normalizePhone(value: string): string {
  const compact = value.replace(/[\s\-()]/g, '')
  // 允许粘贴 +86 / 86 前缀的完整号码
  if (/^\+?86(?=1[3-9]\d{9}$)/.test(compact)) {
    return compact.replace(/^\+?86/, '')
  }
  return compact
}

// 账号信息校验与脱敏
export function isValidPhone(value: string): boolean {
  return MAINLAND_PHONE_RE.test(normalizePhone(value))
}

/** 按国内习惯格成 3-4-4（138 0013 8000），最多保留 11 位数字。 */
export function formatPhone(value: string): string {
  const digits = value.replace(/\D/g, '')
  const local = digits.length === 13 && digits.startsWith('86') ? digits.slice(2) : digits
  const d = local.slice(0, 11)
  return [d.slice(0, 3), d.slice(3, 7), d.slice(7, 11)].filter(Boolean).join(' ')
}

export function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
}

/**
 * 密码策略：8-64 位、不含空白、同时包含字母和数字（与后端一致）。
 * 返回具体错误信息，合法时返回 null。
 */
export function passwordError(value: string): string | null {
  if (/\s/.test(value)) return '密码不能包含空格'
  if (value.length < 8 || value.length > 64) return '密码需为 8-64 位'
  if (!/[A-Za-z]/.test(value)) return '密码需包含字母'
  if (!/\d/.test(value)) return '密码需包含数字'
  return null
}

export function isValidPassword(value: string): boolean {
  return passwordError(value) === null
}

export function maskPhone(value: string): string {
  const digits = normalizePhone(value)
  if (MAINLAND_PHONE_RE.test(digits)) {
    return `${digits.slice(0, 3)} **** ${digits.slice(-4)}`
  }
  return digits.length >= 7 ? `${digits.slice(0, 4)} **** ${digits.slice(-3)}` : value
}
