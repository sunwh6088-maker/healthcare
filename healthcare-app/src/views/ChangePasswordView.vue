<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import NumericKeypad from '../components/NumericKeypad.vue'
import { toast } from '../utils/toast'
import { passwordError } from '../utils/validation'
import { api } from '../api'
import { setSession } from '../utils/session'
import { syncFromServer } from '../utils/healthStore'

const route = useRoute()
const router = useRouter()

const resetMode = computed(() => route.name === 'reset-password')
const resetPhone = computed(() => String(route.query.phone || ''))
const resetCode = computed(() => String(route.query.code || ''))

const oldPwd = ref('')
const pwd1 = ref('')
const pwd2 = ref('')
const focus = ref(0)
const maxLen = 64
const submitting = ref(false)

const fields = computed(() => (resetMode.value ? [pwd1, pwd2] : [oldPwd, pwd1, pwd2]))

watch(resetMode, () => {
  oldPwd.value = ''
  pwd1.value = ''
  pwd2.value = ''
  focus.value = 0
})

function currentField() {
  return fields.value[focus.value - 1]
}

function onKey(key: string) {
  const field = currentField()
  if (field && field.value.length < maxLen) field.value += key
}

function onDelete() {
  const field = currentField()
  if (field) field.value = field.value.slice(0, -1)
}

function back() {
  goBack(router, resetMode.value ? '/forgot-password' : '/settings')
}

async function submit() {
  const pwdError = passwordError(pwd1.value)
  if (pwdError) {
    toast(pwdError)
    return
  }
  if (pwd1.value !== pwd2.value) {
    toast('两次输入的新密码不一致')
    return
  }

  submitting.value = true
  try {
    if (resetMode.value) {
      if (!resetPhone.value || !resetCode.value) {
        toast('重置信息不完整，请重新获取验证码')
        router.replace('/forgot-password')
        return
      }
      await api.resetPassword(resetPhone.value, resetCode.value, pwd1.value)
      setSession()
      await syncFromServer()
      toast('密码已重置，已自动登录')
      router.replace('/home')
    } else {
      await api.changePassword(oldPwd.value, pwd1.value)
      toast('密码已更改 ✓')
      setTimeout(() => router.push('/settings'), 400)
    }
  } catch (error) {
    toast(error instanceof Error ? error.message : '操作失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="cp">
    <StatusBar />

    <NavBar :title="resetMode ? '重置密码' : '更改密码'" @back="back" />

    <div class="cp-body">
      <!-- 重置密码时展示插画；正常改密有三个输入框，隐藏插画留出空间 -->
      <div v-if="resetMode" class="illus">
        <span class="illus-emoji">🔑</span>
      </div>

      <!-- 当前密码（首次设置密码可留空） -->
      <div v-if="!resetMode" class="field" :class="{ focus: focus === 1 }" @click="focus = 1">
        <span class="f-label">当前密码</span>
        <div class="f-input">
          <span class="num dots">{{ '●'.repeat(oldPwd.length) }}</span>
          <span v-if="!oldPwd" class="f-placeholder">输入当前密码（首次可留空）</span>
          <span v-if="focus === 1" class="caret"></span>
        </div>
      </div>

      <!-- 新密码 -->
      <div
        class="field"
        :class="{ focus: focus === (resetMode ? 1 : 2) }"
        @click="focus = resetMode ? 1 : 2"
      >
        <span class="f-label">新密码</span>
        <div class="f-input">
          <span class="num dots">{{ '●'.repeat(pwd1.length) }}</span>
          <span v-if="!pwd1" class="f-placeholder">输入新密码</span>
          <span v-if="focus === (resetMode ? 1 : 2)" class="caret"></span>
        </div>
      </div>

      <!-- 确认新密码 -->
      <div
        class="field"
        :class="{ focus: focus === (resetMode ? 2 : 3) }"
        @click="focus = resetMode ? 2 : 3"
      >
        <span class="f-label">确认新密码</span>
        <div class="f-input">
          <span class="num dots">{{ '●'.repeat(pwd2.length) }}</span>
          <span v-if="!pwd2" class="f-placeholder">再次输入新密码</span>
          <span v-if="focus === (resetMode ? 2 : 3)" class="caret"></span>
        </div>
      </div>

      <p v-if="!resetMode" class="tip">首次设置密码时，当前密码可以留空</p>
      <p v-else class="tip">正在为 {{ resetPhone }} 重置密码</p>

      <button
        class="btn-primary mt-20"
        :class="{ disabled: submitting }"
        :disabled="submitting"
        @click="submit"
      >
        {{ submitting ? '处理中...' : resetMode ? '重置密码' : '更改密码' }}
      </button>
    </div>

    <NumericKeypad @key="onKey" @delete="onDelete" />
  </div>
</template>

<style scoped>
.cp {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--white);
}

.cp-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 24px;
}

.illus {
  margin-top: 24px;
  width: 140px;
  height: 160px;
  border-radius: 50%;
  background: var(--primary-2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.illus-emoji {
  font-size: 56px;
}

/* 输入框：对应设计稿 New password @(24,300)/(24,392) 342x76 */
.field {
  margin-top: 20px;
  width: 100%;
  max-width: 342px;
  height: 76px;
  border-radius: var(--radius-md);
  background: var(--white);
  box-shadow: var(--shadow-card);
  padding: 10px 16px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  cursor: text;
  border: 1.5px solid transparent;
}

.field.focus {
  border-color: var(--primary);
}

.f-label {
  font-size: var(--fs-caption);
  color: var(--neutral-2);
}

.f-input {
  position: relative;
  margin-top: 4px;
  height: 24px;
  display: flex;
  align-items: center;
}

.dots {
  font-size: 16px;
  letter-spacing: 4px;
  color: var(--primary);
}

.f-placeholder {
  font-size: var(--fs-headline3);
  color: var(--neutral-2);
}

.caret {
  width: 1.5px;
  height: 22px;
  background: var(--primary);
  margin-left: 4px;
  animation: blink 1s step-end infinite;
}

.tip {
  margin-top: 12px;
  font-size: var(--fs-caption);
  color: var(--neutral-2);
  text-align: center;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}
</style>
