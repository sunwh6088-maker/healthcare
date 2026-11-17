<script setup lang="ts">
// 模块：登录页 —— 验证码登录和密码登录。
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { setPendingPhone, setPendingDevCode, setSession } from '../utils/session'
import { syncFromServer } from '../utils/healthStore'
import { toast } from '../utils/toast'
import { formatPhone, isValidPhone, normalizePhone } from '../utils/validation'

const router = useRouter()
const phone = ref('')
const phoneError = ref('')
const password = ref('')
const mode = ref('code')
const sending = ref(false)
const loggingIn = ref(false)

const loginModes = [
  { value: 'code', label: '验证码登录' },
  { value: 'password', label: '密码登录' },
]

function onPhoneInput(event: Event) {
  const input = event.target as HTMLInputElement
  phone.value = formatPhone(input.value)
  input.value = phone.value
  phoneError.value = ''
}

/** 校验并返回纯号码；不符合国内格式时提示并返回 null。 */
function ensurePhone(): string | null {
  const normalized = normalizePhone(phone.value)
  if (!isValidPhone(normalized)) {
    phoneError.value = '请输入正确的 11 位中国大陆手机号'
    toast(phoneError.value)
    return null
  }
  return normalized
}

async function continueFlow() {
  const value = ensurePhone()
  if (!value) return
  sending.value = true
  try {
    const result = await api.sendCode(value)
    setPendingPhone(value)
    setPendingDevCode(result.devCode)
    toast(result.devCode ? `验证码已发送：${result.devCode}` : '验证码已发送')
    router.push({ path: '/verify', query: { phone: value } })
  } catch (error) {
    toast(error instanceof Error ? error.message : '发送验证码失败')
  } finally {
    sending.value = false
  }
}

async function passwordLogin() {
  const value = ensurePhone()
  if (!value) return
  if (!password.value) {
    toast('请输入密码')
    return
  }
  loggingIn.value = true
  try {
    await api.loginPassword(value, password.value)
    setSession()
    await syncFromServer()
    toast('登录成功')
    router.replace('/home')
  } catch (error) {
    toast(error instanceof Error ? error.message : '登录失败')
  } finally {
    loggingIn.value = false
  }
}
</script>

<template>
  <div class="login">
    <StatusBar />

    <BackButton class="back-top" @click="router.push('/onboarding')" />

    <div class="content">
      <h1 class="title">{{ mode === 'code' ? '验证您的手机号' : '密码登录' }}</h1>
      <p class="hint">
        {{ mode === 'code' ? '请输入中国大陆手机号，获取验证码登录' : '请输入手机号和密码' }}
      </p>

      <SegmentedControl v-model="mode" :options="loginModes" class="login-mode mt-16" />

      <div class="phone-input card">
        <div class="country">+86</div>
        <input
          :value="phone"
          class="num phone-num"
          type="tel"
          inputmode="numeric"
          autocomplete="tel"
          maxlength="20"
          placeholder="138 0013 8000"
          @input="onPhoneInput"
        />
      </div>
      <p v-if="phoneError" class="field-error">{{ phoneError }}</p>

      <input
        v-if="mode === 'password'"
        v-model="password"
        class="password-input card"
        type="password"
        placeholder="请输入密码"
        autocomplete="current-password"
      />

      <button
        v-if="mode === 'code'"
        class="btn-primary mt-24"
        :class="{ disabled: sending }"
        :disabled="sending"
        @click="continueFlow"
      >
        {{ sending ? '发送中...' : '继续' }}
      </button>
      <button
        v-else
        class="btn-primary mt-24"
        :class="{ disabled: loggingIn }"
        :disabled="loggingIn"
        @click="passwordLogin"
      >
        {{ loggingIn ? '登录中...' : '登录' }}
      </button>

      <button class="forgot orange-link" @click="router.push('/forgot-password')">
        忘记密码？
      </button>
    </div>
  </div>
</template>

<style scoped>
.login {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0 24px;
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-bottom: 60px;
}

.title {
  font-size: var(--fs-title2);
  line-height: var(--lh-title2);
  font-weight: 700;
}

.hint {
  margin-top: 8px;
  font-size: var(--fs-body2);
  line-height: var(--lh-body2);
  color: var(--neutral-2);
}

.phone-input {
  margin-top: 32px;
  display: flex;
  align-items: center;
  padding: 8px;
  border-radius: var(--radius-md);
}

.country {
  width: 90px;
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

.field-error {
  margin-top: 8px;
  padding-left: 8px;
  font-size: var(--fs-caption);
  color: var(--accent);
}

.login-mode {
  background: var(--white);
}

.password-input {
  margin-top: 12px;
  width: 100%;
  height: 52px;
  padding: 0 16px;
  font-size: var(--fs-body1);
  border-radius: var(--radius-md);
  color: var(--neutral);
}

.forgot {
  margin-top: 20px;
  text-align: center;
  font-size: var(--fs-body2);
}
</style>
