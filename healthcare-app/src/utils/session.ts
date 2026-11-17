const TOKEN_KEY = 'token'
const PENDING_PHONE_KEY = 'healthcare-pending-phone'
const PENDING_CODE_KEY = 'healthcare-pending-code'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
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
