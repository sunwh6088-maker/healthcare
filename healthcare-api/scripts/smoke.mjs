// 真实 MySQL 环境接口冒烟：登录后使用 HttpOnly Cookie 访问需要认证的接口。
// 用法：API_BASE=http://localhost:8081/api node scripts/smoke.mjs
const base = (process.env.API_BASE || 'http://localhost:8080/api').replace(/\/$/, '')
const suffix = String(Date.now()).slice(-8)
const phone = `137${suffix}`

const json = async (res) => {
  const body = await res.json()
  if (!res.ok) throw new Error(`${res.status} ${JSON.stringify(body)}`)
  return body
}
const post = (path, body) =>
  fetch(base + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }).then(json)

const send = await post('/auth/send-code', { phone })
const code = send.data.devCode
console.log('send-code devCode:', code)

const loginResponse = await fetch(base + '/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ phone, code }),
})
const login = await json(loginResponse)
const cookie = loginResponse.headers.getSetCookie()[0].split(';')[0]
const headers = { 'Content-Type': 'application/json', Cookie: cookie }
const authHeaders = { Cookie: cookie }
console.log('login:', { newUser: login.data.newUser, user: login.data.user })

const me = await fetch(base + '/me', { headers }).then(json)
console.log('me:', me.data)

// 头像上传、读取、删除
const png = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a])
const form = new FormData()
form.append('file', new Blob([png], { type: 'image/png' }), 'avatar.png')
const avatarUpload = await fetch(base + '/me/avatar', {
  method: 'POST',
  headers: authHeaders,
  body: form,
}).then(json)
console.log('avatar uploaded:', avatarUpload.data.avatarUrl)

const avatarImage = await fetch(base + '/me/avatar', { headers: authHeaders })
console.log('avatar image:', avatarImage.status, avatarImage.headers.get('content-type'))
await fetch(base + '/me/avatar', { method: 'DELETE', headers: authHeaders }).then(json)

// 修改手机号需要新号码验证码 + 当前身份凭证（无密码时为当前手机号验证码）
const newPhone = `138${suffix}`
const phoneCode = await fetch(base + '/me/phone/send-code', {
  method: 'POST',
  headers,
  body: JSON.stringify({ phone: newPhone }),
}).then(json)
const stepUp = await fetch(base + '/me/step-up/send-code', {
  method: 'POST',
  headers,
}).then(json)
const phoneUpdated = await fetch(base + '/me/phone', {
  method: 'PUT',
  headers,
  body: JSON.stringify({
    phone: newPhone,
    code: phoneCode.data.devCode,
    currentCode: stepUp.data.devCode,
  }),
}).then(json)
console.log('phone updated:', phoneUpdated.data.phone)

// Web Push 公钥与订阅
const pushKey = await fetch(base + '/push/public-key', { headers }).then(json)
const fakeEndpoint = `https://fcm.googleapis.com/fcm/send/smoke-${suffix}`
await fetch(base + '/push/subscribe', {
  method: 'POST',
  headers,
  body: JSON.stringify({
    endpoint: fakeEndpoint,
    keys: { p256dh: 'smoke-p256dh', auth: 'smoke-auth' },
  }),
}).then(json)
await fetch(base + '/push/unsubscribe', {
  method: 'POST',
  headers,
  body: JSON.stringify({ endpoint: fakeEndpoint }),
}).then(json)
console.log('push public key length:', pushKey.data.publicKey.length)

// 睡眠时间写入与读取
const state = await fetch(base + '/state', { headers }).then(json)
const health = { ...state.data.health, sleepStartMinutes: 22 * 60 + 30, sleepMinutes: 8 * 60 }
const saved = await fetch(base + '/state', {
  method: 'PUT',
  headers,
  body: JSON.stringify({ health, alarms: state.data.alarms }),
}).then(json)
console.log('sleep saved:', {
  start: saved.data.health.sleepStartMinutes,
  minutes: saved.data.health.sleepMinutes,
  alarms: saved.data.alarms.length,
})

// 数据导出
const exported = await fetch(base + '/me/export', { headers }).then(json)
console.log('export keys:', Object.keys(exported))

// 退出登录后旧 Cookie 失效
await fetch(base + '/auth/logout', { method: 'POST', headers }).then(json)
const afterLogout = await fetch(base + '/me', { headers })
console.log('after logout status:', afterLogout.status)
