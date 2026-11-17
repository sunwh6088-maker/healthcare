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

app.use(router).mount('#app')
