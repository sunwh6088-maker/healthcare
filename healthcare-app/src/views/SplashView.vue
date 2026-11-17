<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { clearToken, getToken } from '../utils/session'
import { syncFromServer } from '../utils/healthStore'

const router = useRouter()

onMounted(async () => {
  let target = '/onboarding'
  if (getToken()) {
    try {
      await syncFromServer()
      target = '/home'
    } catch {
      clearToken()
    }
  }
  setTimeout(() => router.replace(target), 1200)
})
</script>

<template>
  <div class="splash">
    <div class="logo">
      <span>🌿</span>
    </div>
    <h1 class="app-name num">健康追踪</h1>
    <p class="tag">记录健康 · 遇见更好的自己</p>
  </div>
</template>

<style scoped>
.splash {
  height: 100%;
  background: var(--primary);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--white);
}

.logo {
  width: 108px;
  height: 108px;
  background: var(--white);
  border-radius: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 56px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.25);
}

.app-name {
  font-size: 32px;
  font-weight: 700;
  letter-spacing: 1px;
}

.tag {
  font-size: 14px;
  opacity: 0.8;
}
</style>
