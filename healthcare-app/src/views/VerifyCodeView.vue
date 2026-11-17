<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import NumericKeypad from '../components/NumericKeypad.vue'
import { api } from '../api'
import {
  clearPendingPhone,
  getPendingDevCode,
  getPendingPhone,
  setPendingDevCode,
  setSession,
} from '../utils/session'
import { syncFromServer } from '../utils/healthStore'
import { toast } from '../utils/toast'

const router = useRouter()
const phone = ref('')
const codes = ref(['', '', '', '', '', ''])
const focusIndex = ref(0)
const seconds = ref(20)
const verifying = ref(false)
const devCode = ref('')
let timer: ReturnType<typeof setInterval> | null = null

function startCountdown() {
  seconds.value = 20
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    if (seconds.value > 0) seconds.value--
    else if (timer) clearInterval(timer)
  }, 1000)
}

onMounted(() => {
  phone.value = getPendingPhone()
  if (!phone.value) {
    toast('请先输入电话号码')
    router.replace('/login')
    return
  }
  const cachedCode = getPendingDevCode()
  if (/^\d{6}$/.test(cachedCode)) {
    devCode.value = cachedCode
    codes.value = cachedCode.split('')
    focusIndex.value = 6
  }
  startCountdown()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

function onKey(key: string) {
  if (focusIndex.value >= codes.value.length) return
  codes.value[focusIndex.value] = key
  focusIndex.value += 1
}

function onDelete() {
  if (focusIndex.value === 0) return
  focusIndex.value -= 1
  codes.value[focusIndex.value] = ''
}

async function resend() {
  try {
    const result = await api.sendCode(phone.value)
    devCode.value = result.devCode || ''
    setPendingDevCode(result.devCode)
    if (result.devCode) {
      codes.value = result.devCode.split('')
      focusIndex.value = 6
    }
    startCountdown()
    toast(result.devCode ? `验证码已发送：${result.devCode}` : '验证码已发送')
  } catch (error) {
    toast(error instanceof Error ? error.message : '发送验证码失败')
  }
}

async function confirm() {
  if (codes.value.some((code) => !code)) {
    toast('请输入完整验证码')
    return
  }
  verifying.value = true
  try {
    const result = await api.login(phone.value, codes.value.join(''))
    setSession()
    clearPendingPhone()
    await syncFromServer()
    toast('登录成功')
    router.replace(result.newUser ? '/gender' : '/home')
  } catch (error) {
    toast(error instanceof Error ? error.message : '登录失败')
    codes.value = ['', '', '', '', '', '']
    focusIndex.value = 0
  } finally {
    verifying.value = false
  }
}
</script>

<template>
  <div class="verify">
    <StatusBar />

    <div class="top-bar">
      <BackButton class="back-top" @click="router.push('/login')" />
    </div>

    <div class="content">
      <h1 class="title">输入验证码</h1>
      <p class="hint">请输入发送至 {{ phone }} 的代码</p>

      <div class="code-row">
        <div
          v-for="(code, index) in codes"
          :key="index"
          class="code-box num"
          :class="{ focus: index === focusIndex }"
          @click="focusIndex = index"
        >
          {{ code }}
        </div>
      </div>

      <p v-if="devCode" class="dev-code">开发验证码：{{ devCode }}</p>

      <div class="row gap-8 resend-row">
        <span class="green-link" @click="resend">重新发送验证码</span>
        <span class="num countdown">00:{{ String(seconds).padStart(2, '0') }}</span>
      </div>

      <button
        class="btn-primary mt-24"
        :class="{ disabled: verifying }"
        :disabled="verifying"
        @click="confirm"
      >
        {{ verifying ? '验证中...' : '验证' }}
      </button>
    </div>

    <NumericKeypad @key="onKey" @delete="onDelete" />
  </div>
</template>

<style scoped>
.verify {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.top-bar {
  padding: 0 24px;
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 24px 20px;
}

.title {
  font-size: var(--fs-title2);
  line-height: var(--lh-title2);
  font-weight: 700;
}

.hint {
  margin-top: 8px;
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.code-row {
  margin-top: 32px;
  display: flex;
  gap: 10px;
}

.code-box {
  flex: 1;
  height: 56px;
  border-radius: var(--radius-sm);
  border: 1.5px solid var(--primary-2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: var(--primary);
  cursor: pointer;
}

.code-box.focus {
  border-color: var(--primary);
}

.dev-code {
  margin-top: 12px;
  text-align: center;
  font-size: var(--fs-caption);
  color: var(--primary);
}

.resend-row {
  margin-top: 16px;
  justify-content: center;
}

.countdown {
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}
</style>
