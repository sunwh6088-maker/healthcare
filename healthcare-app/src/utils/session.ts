const SESSION_KEY = 'healthcare-session'
const PENDING_PHONE_KEY = 'healthcare-pending-phone'
const PENDING_CODE_KEY = 'healthcare-pending-code'

// 清理旧版本遗留在 localStorage 里的 token（现在由 HttpOnly Cookie 维护）
try {
  localStorage.removeItem('token')
} catch {
  // 隐私模式等场景下忽略
}

// 登录 token 由后端写入 HttpOnly Cookie，前端不保存。
// 这里只记录一个非敏感的“已登录”标记，供路由守卫和启动页判断。
export function hasSession() {
  return localStorage.getItem(SESSION_KEY) === '1'
}

export function setSession() {
  localStorage.setItem(SESSION_KEY, '1')
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY)
}

export function setPendingPhone(phone: string) {
  sessionStorage.setItem(PENDING_PHONE_KEY, phone)
}

export function getPendingPhone() {
  return sessionStorage.getItem(PENDING_PHONE_KEY) || ''
}

export function clearPendingPhone() {
  sessionStorage.removeItem(PENDING_PHONE_KEY)
}

export function setPendingDevCode(code?: string) {
  if (code) sessionStorage.setItem(PENDING_CODE_KEY, code)
  else sessionStorage.removeItem(PENDING_CODE_KEY)
}

export function getPendingDevCode() {
  return sessionStorage.getItem(PENDING_CODE_KEY) || ''
}
