// 验证真实 MQTT 链路：登录 → 绑定设备 → MQTT 发布 Alink 上报 → 健康数据更新
import mqtt from 'mqtt'

const API = process.env.API_BASE || 'http://localhost:8081/api'
const BROKER = process.env.MQTT_BROKER_URL || 'mqtt://127.0.0.1:1883'
const suffix = String(Date.now()).slice(-8)
const phone = `134${suffix}`
const productKey = `pk-mqtt-${suffix}`
const deviceName = 'watch-mqtt'

const post = (path, body, headers = {}) =>
  fetch(API + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(body),
  })

const send = await post('/auth/send-code', { phone }).then((r) => r.json())
const login = await post('/auth/login', { phone, code: send.data.devCode })
const cookie = login.headers.getSetCookie()[0].split(';')[0]
const headers = { 'Content-Type': 'application/json', Cookie: cookie }

const bound = await post(
  '/iot/devices',
  { productKey, deviceName, name: 'MQTT 测试手表' },
  headers,
).then((r) => r.json())
if (bound.code !== 0) throw new Error('绑定失败: ' + JSON.stringify(bound))
console.log('设备已绑定:', bound.data.id)

const topic = `sys/${productKey}/${deviceName}/thing/event/property/post`
const payload = JSON.stringify({
  id: String(Date.now()),
  version: '1.0',
  method: 'thing.event.property.post',
  params: { steps: 5000, waterMl: 1200, sleepMinutes: 460, weightKg: 62.3, battery: 77 },
})

await new Promise((resolve, reject) => {
  const client = mqtt.connect(BROKER)
  client.on('connect', () => {
    client.publish(topic, payload, { qos: 1 }, (error) => {
      client.end()
      error ? reject(error) : resolve()
    })
  })
  client.on('error', reject)
})
console.log('MQTT 已发布:', topic)

let health = null
for (let i = 0; i < 20; i++) {
  await new Promise((resolve) => setTimeout(resolve, 500))
  const state = await fetch(API + '/state', { headers }).then((r) => r.json())
  health = state.data.health
  if (health.steps === 5000) break
}
console.log('健康数据:', {
  steps: health.steps,
  waterMl: health.waterMl,
  sleepMinutes: health.sleepMinutes,
  deviceConnected: health.deviceConnected,
})
if (health.steps !== 5000 || health.waterMl !== 1200) {
  process.exitCode = 1
}
