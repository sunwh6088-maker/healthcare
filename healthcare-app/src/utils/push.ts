// 模块：Web Push —— Service Worker 注册、订阅管理与测试通知。

import { api } from '../api'

const ENABLED_KEY = 'healthcare-push-enabled'

function sameBytes(left: Uint8Array, right: Uint8Array): boolean {
  if (left.length !== right.length) return false
  return left.every((value, index) => value === right[index])
}

function urlBase64ToUint8Array(base64String: string): Uint8Array<ArrayBuffer> {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4)
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
  const raw = atob(base64)
  const buffer = new ArrayBuffer(raw.length)
  const bytes = new Uint8Array(buffer)
  for (let i = 0; i < raw.length; i++) bytes[i] = raw.charCodeAt(i)
  return bytes
}

export function pushSupported(): boolean {
  return 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window
}

/** 开启推送：申请权限、注册订阅并同步到后端。 */
export async function enablePush(): Promise<void> {
  if (!pushSupported()) {
    throw new Error('当前浏览器不支持消息推送')
  }
  if (Notification.permission !== 'granted') {
    const permission = await Notification.requestPermission()
    if (permission !== 'granted') {
      throw new Error('浏览器已拒绝通知权限')
    }
  }
  const registration = await navigator.serviceWorker.ready
  const { publicKey } = await api.getPushPublicKey()
  if (!publicKey) {
    throw new Error('服务端未配置推送密钥')
  }
  const keyBytes = urlBase64ToUint8Array(publicKey)
  let subscription = await registration.pushManager.getSubscription()
  // 服务端换了 VAPID 密钥（例如开发环境重启）时，旧订阅不再有效
  if (subscription) {
    const existingKey = subscription.options?.applicationServerKey
    if (existingKey && !sameBytes(new Uint8Array(existingKey), keyBytes)) {
      await subscription.unsubscribe().catch(() => undefined)
      subscription = null
    }
  }
  if (!subscription) {
    subscription = await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: keyBytes,
    })
  }
  const json = subscription.toJSON()
  if (!json.endpoint || !json.keys?.p256dh || !json.keys?.auth) {
    throw new Error('推送订阅信息不完整')
  }
  await api.subscribePush({
    endpoint: json.endpoint,
    keys: { p256dh: json.keys.p256dh, auth: json.keys.auth },
  })
  localStorage.setItem(ENABLED_KEY, '1')
}

/** 关闭推送：通知后端删除订阅并取消浏览器订阅。 */
export async function disablePush(): Promise<void> {
  localStorage.removeItem(ENABLED_KEY)
  if (!pushSupported()) return
  const registration = await navigator.serviceWorker.ready
  const subscription = await registration.pushManager.getSubscription()
  if (!subscription) return
  const endpoint = subscription.endpoint
  await api.unsubscribePush(endpoint).catch(() => undefined)
  await subscription.unsubscribe().catch(() => undefined)
}

/** 页面加载时如果之前开启过推送，重新绑定到当前登录账号。 */
export async function resubscribeIfEnabled(): Promise<void> {
  if (localStorage.getItem(ENABLED_KEY) !== '1') return
  if (!pushSupported() || Notification.permission !== 'granted') return
  await enablePush()
}

export function sendTestPush(): Promise<number> {
  return api.sendTestPush()
}
