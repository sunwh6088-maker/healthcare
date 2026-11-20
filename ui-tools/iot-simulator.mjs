// 通过 MQTT 发布一条 Alink 物模型属性上报。
// 用法：node iot-simulator.mjs <productKey> <deviceName> [brokerUrl]
import mqtt from 'mqtt'

const productKey = process.argv[2]
const deviceName = process.argv[3]
const broker = process.argv[4] || process.env.MQTT_BROKER_URL || 'mqtt://127.0.0.1:1883'
// broker 开启认证时通过环境变量传入（例如 EMQX 的 healthcare 用户）
const auth = process.env.MQTT_USERNAME
  ? { username: process.env.MQTT_USERNAME, password: process.env.MQTT_PASSWORD || '' }
  : {}

if (!productKey || !deviceName) {
  console.error('用法：node iot-simulator.mjs <productKey> <deviceName> [brokerUrl]')
  process.exit(1)
}

const topic = `sys/${productKey}/${deviceName}/thing/event/property/post`
const payload = JSON.stringify({
  id: String(Date.now()),
  version: '1.0',
  method: 'thing.event.property.post',
  params: {
    steps: 5000,
    waterMl: 1200,
    sleepMinutes: 460,
    weightKg: 62.3,
    battery: 77,
  },
})

const client = mqtt.connect(broker, auth)
client.on('connect', () => {
  client.publish(topic, payload, { qos: 1 }, (error) => {
    if (error) {
      console.error('发布失败：', error.message)
      process.exitCode = 1
    } else {
      console.log(`已发布到 ${topic}`)
    }
    client.end()
  })
})
client.on('error', (error) => {
  console.error('MQTT 连接失败：', error.message)
  client.end()
  process.exit(1)
})
