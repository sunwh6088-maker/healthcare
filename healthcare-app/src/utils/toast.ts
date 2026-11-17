// 模块：全局轻提示 —— 在手机壳内显示短暂消息。

export function toast(msg: string) {
  document.querySelector('.toast')?.remove()
  const el = document.createElement('div')
  // 优先挂在手机壳内，桌面端不会跑到浏览器窗口底部
  const host = document.querySelector('.phone')
  el.className = host ? 'toast' : 'toast toast-fixed'
  el.setAttribute('role', 'status')
  el.textContent = msg
  ;(host ?? document.body).appendChild(el)
  setTimeout(() => el.remove(), 1600)
}
