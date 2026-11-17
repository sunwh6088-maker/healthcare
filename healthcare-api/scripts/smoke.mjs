const base = 'http://localhost:8080/api'
const phone = '13900001111'

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

const login = await post('/auth/login', { phone, code })
const token = login.data.token
const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }
console.log('login:', { newUser: login.data.newUser, user: login.data.user })

const me = await fetch(base + '/me', { headers }).then(json)
console.log('me:', me.data)

const state = await fetch(base + '/state', { headers }).then(json)
console.log('state:', {
  steps: state.data.health.steps,
  waterMl: state.data.health.waterMl,
  alarms: state.data.alarms.length,
})

const health = { ...state.data.health, steps: 5123, waterMl: 1200 }
const alarms = [{ time: '07:30', label: 'MySQL 测试闹钟', repeat: [1, 3, 5], on: true }]
const saved = await fetch(base + '/state', {
  method: 'PUT',
  headers,
  body: JSON.stringify({ health, alarms }),
}).then(json)
console.log('saved:', {
  steps: saved.data.health.steps,
  waterMl: saved.data.health.waterMl,
  alarms: saved.data.alarms,
})

const patched = await fetch(base + '/me', {
  method: 'PATCH',
  headers,
  body: JSON.stringify({ email: 'mysql@example.com' }),
}).then(json)
console.log('patched email:', patched.data.email)

const unauthorized = await fetch(base + '/state')
console.log('unauthorized status:', unauthorized.status)
