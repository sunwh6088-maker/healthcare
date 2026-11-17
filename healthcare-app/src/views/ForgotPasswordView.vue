<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import NumericKeypad from '../components/NumericKeypad.vue'
import { api } from '../api'
import { toast } from '../utils/toast'

const router = useRouter()

// 步骤 1 = 输入电话号码，步骤 2 = 输入验证码
const step = ref(1)
const phone = ref('')
const codes = ref(['', '', '', '', '', ''])
const focusIndex = ref(0)
const seconds = ref(20)
const devCode = ref('')
const sending = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

function startCountdown() {
  seconds.value = 20
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    if (seconds.value > 0) seconds.value--
    else if (timer) clearInterval(timer)
  }, 1000)
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

function onKey(k: string) {
  if (step.value === 1) {
    if (phone.value.replace(/\s/g, '').length < 11) {
      phone.value = (phone.value + k).replace(/(\d{4})(?=\d)/g, '$1 ')
    }
  } else {
    if (focusIndex.value < codes.value.length) {
      codes.value[focusIndex.value] = k
      focusIndex.value++
    }
  }
}

function onDelete() {
  if (step.value === 1) {
    phone.value = phone.value.replace(/[\s\d]$/, '').trim()
  } else {
    focusIndex.value = Math.max(0, focusIndex.value - 1)
    codes.value[focusIndex.value] = ''
  }
}

async function requestCode() {
  const value = phone.value.trim()
  if (!value) {
    toast('请先输入电话号码')
    return
  }
  try {
    const result = await api.sendCode(value)
    devCode.value = result.devCode || ''
    if (result.devCode) {
      codes.value = result.devCode.split('')
      focusIndex.value = 6
    } else {
      codes.value = ['', '', '', '', '', '']
      focusIndex.value = 0
    }
    startCountdown()
    toast(result.devCode ? `验证码已发送：${result.devCode}` : '验证码已发送')
  } catch (error) {
    toast(error instanceof Error ? error.message : '发送验证码失败')
    throw error
  }
}

async function sendCode() {
  if (!phone.value.trim()) {
    toast('请先输入电话号码')
    return
  }
  sending.value = true
  try {
    await requestCode()
    step.value = 2
  } catch {
    // requestCode 已经提示错误
  } finally {
    sending.value = false
  }
}

async function resend() {
  try {
    await requestCode()
  } catch {
    // requestCode 已经提示错误
  }
}

function continueFlow() {
  if (codes.value.some((c) => !c)) {
    toast('请输入完整验证码')
    return
  }
  const normalizedPhone = phone.value.replace(/\s/g, '')
  router.push({
    path: '/reset-password',
    query: { phone: normalizedPhone, code: codes.value.join('') },
  })
}

const btnText = computed(() => {
  if (sending.value) return '发送中...'
  return step.value === 1 ? '发送' : '继续'
})
const btnAction = () => (step.value === 1 ? sendCode() : continueFlow())
</script>

<template>
  <div class="fp">
    <StatusBar />

    <NavBar title="忘记密码" @back="router.push('/login')" />

    <div class="fp-body">
      <!-- 插画 -->
      <div class="illus">
        <span class="illus-emoji">🔒</span>
      </div>

      <!-- 步骤 1：输入电话号码 -->
      <template v-if="step === 1">
        <p class="hint">输入您的电话号码</p>

        <div class="phone-input">
          <div class="country num">+1</div>
          <input v-model="phone" class="num phone-num" type="tel" placeholder="1234 5678 789" />
        </div>

        <p class="note">我们向您发送了一个验证码以验证您的电话号码</p>
      </template>

      <!-- 步骤 2：输入验证码 -->
      <template v-else>
        <p class="hint">请输入发送至的代码</p>

        <div class="code-row">
          <div
            v-for="(c, i) in codes"
            :key="i"
            class="code-box num"
            :class="{ focus: i === focusIndex }"
            @click="focusIndex = i"
          >
            {{ c }}
          </div>
        </div>

        <p v-if="devCode" class="dev-code">开发验证码：{{ devCode }}</p>

        <div class="row gap-8 resend-row">
          <span class="green-link" @click="resend">重新发送验证码</span>
          <span class="num countdown">00:{{ String(seconds).padStart(2, '0') }}</span>
        </div>
      </template>

      <button
        class="btn-primary mt-20"
        :class="{ disabled: sending }"
        :disabled="sending"
        @click="btnAction"
      >
        {{ btnText }}
      </button>
    </div>

    <NumericKeypad @key="onKey" @delete="onDelete" />
  </div>
</template>

<style scoped>
.fp {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--white);
}

.fp-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 24px;
}

/* 插画：对应设计稿 Illus / 7 @(125,124) 140x160 */
.illus {
  margin-top: 24px;
  width: 140px;
  height: 160px;
  border-radius: 50%;
  background: var(--accent-2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.illus-emoji {
  font-size: 56px;
}

.hint {
  margin-top: 24px;
  font-size: var(--fs-body2);
  color: var(--neutral-2);
  text-align: center;
}

.phone-input {
  margin-top: 12px;
  width: 310px;
  display: flex;
  align-items: center;
  padding: 8px;
  border-radius: var(--radius-md);
  background: var(--white);
  box-shadow: var(--shadow-card);
}

.country {
  width: 106px;
  height: 44px;
  border-radius: var(--radius-sm);
  background: var(--primary-2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
  flex-shrink: 0;
}

.phone-num {
  flex: 1;
  padding: 0 16px;
  font-size: var(--fs-headline3);
  font-weight: 600;
  color: var(--neutral);
}

.note {
  margin-top: 16px;
  font-size: var(--fs-body2);
  color: var(--neutral);
  text-align: center;
  max-width: 310px;
}

.code-row {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

.code-box {
  width: 40px;
  height: 56px;
  border-radius: var(--radius-sm);
  border: 1.5px solid var(--primary-2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
  color: var(--primary);
  cursor: pointer;
}

.code-box.focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px rgba(42, 96, 73, 0.15);
}

.resend-row {
  margin-top: 16px;
  justify-content: center;
}

.dev-code {
  margin-top: 12px;
  text-align: center;
  font-size: var(--fs-caption);
  color: var(--primary);
}

.countdown {
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}
</style>
