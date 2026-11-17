// 统一封装的请求工具（对应后端 SpringMVC 接口）
// 后端地址通过环境变量 VITE_API_BASE_URL 配置，默认 /api（配合 vite proxy）
const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

interface ApiResponse<T> {
  code: number
  msg?: string
  data: T
}

async function request<T = unknown>(path: string, options: RequestInit = {}): Promise<T> {
  // 自动携带 token
  const headers = new Headers(options.headers)
  const token = localStorage.getItem('token')
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers,
    signal: options.signal ?? AbortSignal.timeout(10000),
  })

  if (response.status === 401) {
    localStorage.removeItem('token')
    throw new Error('登录已过期，请重新登录')
  }

  if (!response.ok) {
    let message = `请求失败：${response.status}`
    try {
      const body = (await response.json()) as { msg?: string }
      if (body.msg) message = body.msg
    } catch {
      // 非 JSON 响应时使用默认提示
    }
    throw new Error(message)
  }

  // 统一处理后端返回格式 { code, msg, data }
  const payload: unknown = await response.json()
  if (payload && typeof payload === 'object' && 'code' in payload) {
    const res = payload as ApiResponse<T>
    if (res.code === 200 || res.code === 0) return res.data
    throw new Error(res.msg || '请求失败')
  }
  return payload as T
}

export default request
