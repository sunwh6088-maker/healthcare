// 本地轻量 MQTT broker（仅用于开发联调）：node mqtt-broker.mjs [port]
import { createServer } from 'node:net'
import { Aedes } from 'aedes'

const port = Number(process.argv[2] || process.env.MQTT_PORT || 1883)
const aedes = await Aedes.createBroker()
const server = createServer(aedes.handle)

aedes.on('client', (client) => console.log('client connected:', client.id))
aedes.on('clientDisconnect', (client) => console.log('client disconnected:', client.id))
aedes.on('publish', (packet, client) => {
  if (client) console.log('publish:', packet.topic)
})

server.listen(port, '0.0.0.0', () => {
  console.log(`MQTT broker listening on ${port}`)
})
