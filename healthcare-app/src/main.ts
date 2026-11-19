// 模块：应用入口 —— 注册全局组件/提示并挂载路由。

import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import StatusBar from './components/StatusBar.vue'
import NavBar from './components/NavBar.vue'
import AppTabBar from './components/AppTabBar.vue'
import BackButton from './components/BackButton.vue'
import AppSwitch from './components/AppSwitch.vue'
import SegmentedControl from './components/SegmentedControl.vue'
import BottomSheet from './components/BottomSheet.vue'
import { toast } from './utils/toast'

declare module 'vue' {
  interface ComponentCustomProperties {
    $toast: (msg: string) => void
  }
}

const app = createApp(App)

// 全局注册公共组件（无需逐文件 import）
app.component('StatusBar', StatusBar)
app.component('NavBar', NavBar)
app.component('AppTabBar', AppTabBar)
app.component('BackButton', BackButton)
app.component('AppSwitch', AppSwitch)
app.component('SegmentedControl', SegmentedControl)
app.component('BottomSheet', BottomSheet)

// 全局轻提示：用于没有独立页面的占位交互
app.config.globalProperties.$toast = toast

// 注册 Service Worker，用于接收 Web Push 通知（不缓存页面，避免版本残留）
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => undefined)
  })
}

app.use(router).mount('#app')
