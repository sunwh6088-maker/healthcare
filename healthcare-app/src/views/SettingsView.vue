<script setup lang="ts">
// 模块：设置页 —— 通知/隐私开关和退出登录。
import { useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { health, stopRemoteSync } from '../utils/healthStore'
import { clearSession } from '../utils/session'
import { toast } from '../utils/toast'
import { api } from '../api'

const router = useRouter()

async function logout() {
  try {
    await api.logout()
  } catch {
    // 网络异常时也允许本地退出
  }
  stopRemoteSync()
  clearSession()
  toast('已退出登录')
  router.push('/login')
}
</script>

<template>
  <div class="settings">
    <StatusBar />

    <NavBar title="设置" @back="goBack(router, '/profile')" />

    <div class="menu card mt-8">
      <div class="menu-row" @click="router.push('/account-security')">
        <span class="m-label">账户与安全</span>
        <span class="m-arrow">›</span>
      </div>
      <div class="menu-row" @click="router.push('/change-password')">
        <span class="m-label">更改密码</span>
        <span class="m-arrow">›</span>
      </div>
      <div class="menu-row">
        <span class="m-label">消息通知</span>
        <AppSwitch v-model="health.notifyEnabled" label="消息通知" />
      </div>
      <div class="menu-row">
        <span class="m-label">隐私设置</span>
        <AppSwitch v-model="health.privacyEnabled" label="隐私设置" />
      </div>
    </div>

    <div class="menu card mt-16">
      <div class="menu-row" @click="$toast('缓存已清除 ✓')">
        <span class="m-label">清除缓存</span>
        <span class="m-value num">128 兆</span>
      </div>
      <div class="menu-row" @click="$toast('已是最新版本 v1.0.0')">
        <span class="m-label">检查更新</span>
        <span class="m-value">v1.0.0</span>
      </div>
      <div class="menu-row" @click="$toast('健康追踪 v1.0.0')">
        <span class="m-label">关于我们</span>
        <span class="m-arrow">›</span>
      </div>
    </div>

    <button class="logout mt-24" @click="logout">退出登录</button>
  </div>
</template>

<style scoped>
.settings {
  padding: 0 24px 24px;
}

.menu {
  overflow: hidden;
}

.menu-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--neutral-3);
}

.menu-row:last-child {
  border-bottom: none;
}

.m-label {
  font-size: var(--fs-headline3);
  font-weight: 600;
}

.m-arrow {
  font-size: 22px;
  color: var(--neutral-2);
}

.m-value {
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.logout {
  width: 100%;
  height: 48px;
  border-radius: var(--radius-pill);
  background: var(--white);
  color: var(--accent);
  font-size: var(--fs-headline3);
  font-weight: 600;
  box-shadow: var(--shadow-card);
}
</style>
