<script setup lang="ts">
// 模块：账户与安全 —— 手机号/邮箱、密码、设备与注销入口。
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { toast } from '../utils/toast'
import { health, stopRemoteSync } from '../utils/healthStore'
import { clearSession } from '../utils/session'
import {
  formatPhone,
  isValidEmail,
  isValidPhone,
  maskPhone,
  normalizePhone,
} from '../utils/validation'
import { api, type LoginSession } from '../api'

const router = useRouter()

const maskedPhone = computed(() => maskPhone(health.accountPhone))
const hasPassword = ref<boolean | null>(null)
const deleteOpen = ref(false)
const deleteText = ref('')
const deleting = ref(false)

// 登录设备管理
const sessions = ref<LoginSession[]>([])
const confirmRevokeId = ref<number | null>(null)

onMounted(() => {
  void loadSecurity()
})

async function loadSecurity() {
  try {
    const user = await api.me()
    hasPassword.value = user.hasPassword
  } catch {
    // 资料加载失败时保留占位
  }
  try {
    sessions.value = await api.listSessions()
  } catch {
    // 设备列表加载失败时保持为空
  }
}

function formatSessionTime(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${month}月${day}日 ${hour}:${minute}`
}

async function revokeSession(session: LoginSession) {
  if (confirmRevokeId.value !== session.id) {
    confirmRevokeId.value = session.id
    toast('再次点击即可退出该设备')
    return
  }
  try {
    await api.revokeSession(session.id)
    sessions.value = sessions.value.filter((item) => item.id !== session.id)
    confirmRevokeId.value = null
    toast('已退出该设备')
  } catch (error) {
    toast(error instanceof Error ? error.message : '退出设备失败')
  }
}

async function revokeOthers() {
  try {
    await api.revokeOtherSessions()
    sessions.value = sessions.value.filter((item) => item.current)
    confirmRevokeId.value = null
    toast('已退出其他设备')
  } catch (error) {
    toast(error instanceof Error ? error.message : '操作失败')
  }
}

type EditField = 'name' | 'phone' | 'email'
const editField = ref<EditField | null>(null)
const editValue = ref('')
const saving = ref(false)

// 修改手机号分两步：输入新号码 → 验证新号码收到的短信验证码
const phoneStep = ref<'input' | 'verify'>('input')
const phoneCode = ref('')
const phoneDevCode = ref('')
const phoneSending = ref(false)
const cooldown = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

function startCooldown(seconds = 60) {
  clearCooldown()
  cooldown.value = seconds
  cooldownTimer = setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) clearCooldown()
  }, 1000)
}

function clearCooldown() {
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
    cooldownTimer = null
  }
  cooldown.value = 0
}

onUnmounted(clearCooldown)

const sheetTitle = computed(() =>
  editField.value === 'name' ? '修改昵称' : editField.value === 'phone' ? '修改手机号' : '修改邮箱',
)
const sheetKey = computed(() =>
  editField.value === 'name' ? '昵称' : editField.value === 'phone' ? '手机号' : '邮箱',
)
const sheetType = computed(() =>
  editField.value === 'name' ? 'text' : editField.value === 'phone' ? 'tel' : 'email',
)
const sheetPlaceholder = computed(() =>
  editField.value === 'name'
    ? '请输入昵称'
    : editField.value === 'phone'
      ? '请输入 11 位手机号'
      : '请输入邮箱',
)

function onEditInput() {
  if (editField.value === 'phone') {
    editValue.value = formatPhone(editValue.value)
  }
}

function openEdit(field: EditField) {
  editField.value = field
  editValue.value =
    field === 'name' ? health.userName : field === 'phone' ? '' : health.accountEmail
  phoneStep.value = 'input'
  phoneCode.value = ''
  phoneDevCode.value = ''
}

function closeEdit() {
  editField.value = null
  phoneStep.value = 'input'
  phoneCode.value = ''
  phoneDevCode.value = ''
  clearCooldown()
}

function backToPhoneInput() {
  phoneStep.value = 'input'
  phoneCode.value = ''
  phoneDevCode.value = ''
}

/** 第 1 步：校验新号码并请求验证码。 */
async function sendPhoneCode() {
  const value = normalizePhone(editValue.value)
  if (!isValidPhone(value)) {
    toast('请输入正确的 11 位中国大陆手机号')
    return
  }
  if (value === normalizePhone(health.accountPhone)) {
    toast('新手机号与当前手机号相同')
    return
  }
  phoneSending.value = true
  try {
    const result = await api.sendPhoneChangeCode(value)
    phoneStep.value = 'verify'
    phoneCode.value = result.devCode ?? ''
    phoneDevCode.value = result.devCode ?? ''
    toast('验证码已发送')
    startCooldown()
  } catch (error) {
    toast(error instanceof Error ? error.message : '验证码发送失败')
  } finally {
    phoneSending.value = false
  }
}

/** 第 2 步：提交新号码和验证码。 */
async function confirmPhoneChange() {
  const value = normalizePhone(editValue.value)
  if (!/^\d{6}$/.test(phoneCode.value.trim())) {
    toast('请输入 6 位验证码')
    return
  }
  saving.value = true
  try {
    const user = await api.updatePhone(value, phoneCode.value.trim())
    health.accountPhone = user.phone
    toast('手机号已更新')
    closeEdit()
  } catch (error) {
    toast(error instanceof Error ? error.message : '手机号更新失败')
  } finally {
    saving.value = false
  }
}

async function saveEdit() {
  if (editField.value !== 'name' && editField.value !== 'email') return
  const value = editValue.value.trim()
  if (editField.value === 'name') {
    if (!value) {
      toast('昵称不能为空')
      return
    }
    if (value.length > 64) {
      toast('昵称不能超过 64 个字符')
      return
    }
  }
  if (editField.value === 'email' && !isValidEmail(value)) {
    toast('请输入有效的邮箱地址')
    return
  }

  saving.value = true
  try {
    const user = await api.updateMe(editField.value === 'name' ? { name: value } : { email: value })
    health.userName = user.name
    health.accountEmail = user.email ?? ''
    toast(editField.value === 'name' ? '昵称已更新' : '邮箱已更新')
    closeEdit()
  } catch (error) {
    toast(error instanceof Error ? error.message : '更新失败')
  } finally {
    saving.value = false
  }
}

async function deleteAccount() {
  if (deleteText.value.trim() !== '注销') {
    toast('请输入「注销」确认')
    return
  }
  deleting.value = true
  try {
    await api.deleteMe()
    stopRemoteSync()
    clearSession()
    toast('账户已注销')
    router.replace('/login')
  } catch (error) {
    toast(error instanceof Error ? error.message : '注销失败')
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <div class="account">
    <StatusBar />

    <NavBar title="账户与安全" @back="goBack(router, '/settings')" />

    <!-- 账号概览 -->
    <div class="hero card">
      <div class="avatar">
        <img v-if="health.avatarUrl" :src="health.avatarUrl" alt="头像" />
        <span v-else>🧑</span>
      </div>
      <div class="hero-info">
        <h2>{{ health.userName }}</h2>
        <p class="num">{{ maskedPhone }}</p>
      </div>
      <span class="badge">已验证</span>
    </div>

    <h3 class="section-title mt-20">账号信息</h3>
    <div class="menu card mt-12">
      <button class="row" @click="openEdit('name')">
        <span class="r-label">昵称</span>
        <span class="r-value">{{ health.userName }}</span>
        <span class="r-arrow">›</span>
      </button>
      <button class="row" @click="openEdit('phone')">
        <span class="r-label">手机号</span>
        <span class="r-value num">{{ maskedPhone }}</span>
        <span class="r-arrow">›</span>
      </button>
      <button class="row" @click="openEdit('email')">
        <span class="r-label">邮箱</span>
        <span class="r-value">{{ health.accountEmail }}</span>
        <span class="r-arrow">›</span>
      </button>
    </div>

    <h3 class="section-title mt-20">安全设置</h3>
    <div class="menu card mt-12">
      <button class="row" @click="router.push('/change-password')">
        <span class="r-label">登录密码</span>
        <span class="r-value">
          {{ hasPassword === null ? '…' : hasPassword ? '已设置' : '去设置' }}
        </span>
        <span class="r-arrow">›</span>
      </button>
      <button class="row" @click="router.push('/forgot-password')">
        <span class="r-label">忘记密码</span>
        <span class="r-arrow">›</span>
      </button>
    </div>

    <!-- 登录设备 -->
    <h3 class="section-title mt-20">登录设备</h3>
    <div class="menu card mt-12">
      <div v-for="session in sessions" :key="session.id" class="session-row">
        <span class="d-dot" :class="{ muted: !session.current }"></span>
        <div class="d-info">
          <h4>
            {{ session.device }}
            <span v-if="session.current" class="current-badge">当前</span>
          </h4>
          <p>{{ session.ip || '未知 IP' }} · {{ formatSessionTime(session.createdAt) }} 登录</p>
        </div>
        <button v-if="!session.current" class="session-exit" @click="revokeSession(session)">
          {{ confirmRevokeId === session.id ? '确认退出' : '退出' }}
        </button>
      </div>
      <p v-if="!sessions.length" class="session-tip">暂无登录设备信息</p>
    </div>
    <button v-if="sessions.length > 1" class="revoke-others mt-12" @click="revokeOthers">
      退出其他设备
    </button>

    <button class="danger mt-24" @click="deleteOpen = true">注销账户</button>
    <p class="danger-tip">注销后所有健康数据将被清除，且无法恢复</p>

    <!-- 修改手机号 / 邮箱 -->
    <BottomSheet :open="editField !== null" @close="closeEdit">
      <h3 class="sheet-title">
        {{ editField === 'phone' && phoneStep === 'verify' ? '验证新手机号' : sheetTitle }}
      </h3>

      <!-- 修改手机号第 2 步：验证新号码 -->
      <template v-if="editField === 'phone' && phoneStep === 'verify'">
        <p class="code-tip">
          验证码已发送至 <span class="num">{{ formatPhone(editValue) }}</span>，5 分钟内有效。
        </p>
        <label class="sheet-field">
          <span class="sheet-key">验证码</span>
          <input
            v-model="phoneCode"
            class="sheet-input num"
            inputmode="numeric"
            maxlength="6"
            placeholder="请输入 6 位验证码"
          />
        </label>
        <p v-if="phoneDevCode" class="dev-tip">开发模式验证码：{{ phoneDevCode }}</p>
        <div class="sheet-actions">
          <button
            class="sheet-delete"
            :disabled="cooldown > 0 || phoneSending"
            @click="sendPhoneCode"
          >
            {{ cooldown > 0 ? `${cooldown}s 后重新发送` : '重新发送' }}
          </button>
          <button
            class="btn-primary"
            :class="{ disabled: saving }"
            :disabled="saving"
            @click="confirmPhoneChange"
          >
            {{ saving ? '提交中...' : '确认修改' }}
          </button>
        </div>
        <button class="back-edit" @click="backToPhoneInput">返回修改号码</button>
      </template>

      <!-- 第 1 步：昵称 / 邮箱直接保存，手机号先发验证码 -->
      <template v-else>
        <label class="sheet-field">
          <span class="sheet-key">{{ sheetKey }}</span>
          <input
            v-model="editValue"
            class="sheet-input"
            :type="sheetType"
            :maxlength="editField === 'phone' ? 20 : undefined"
            :placeholder="sheetPlaceholder"
            @input="onEditInput"
          />
        </label>

        <div class="sheet-actions">
          <button
            class="btn-primary"
            :class="{ disabled: saving || phoneSending }"
            :disabled="saving || phoneSending"
            @click="editField === 'phone' ? sendPhoneCode() : saveEdit()"
          >
            {{
              editField === 'phone'
                ? phoneSending
                  ? '发送中...'
                  : '发送验证码'
                : saving
                  ? '保存中...'
                  : '保存'
            }}
          </button>
        </div>
      </template>
    </BottomSheet>

    <!-- 注销账户确认 -->
    <BottomSheet :open="deleteOpen" @close="deleteOpen = false">
      <h3 class="sheet-title">注销账户</h3>
      <p class="delete-warning">
        注销后将删除全部健康数据、食物与运动记录，且无法恢复。请输入「注销」确认。
      </p>
      <label class="sheet-field">
        <span class="sheet-key">确认文字</span>
        <input v-model="deleteText" class="sheet-input" placeholder="请输入：注销" />
      </label>
      <div class="sheet-actions">
        <button
          class="sheet-delete"
          :disabled="deleting || deleteText.trim() !== '注销'"
          @click="deleteAccount"
        >
          {{ deleting ? '注销中...' : '确认注销' }}
        </button>
        <button class="btn-primary" @click="deleteOpen = false">取消</button>
      </div>
    </BottomSheet>
  </div>
</template>

<style scoped>
.account {
  padding: 0 24px 24px;
}

.hero {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--primary-2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
  overflow: hidden;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-info h2 {
  font-size: var(--fs-headline2);
  font-weight: 700;
}

.hero-info p {
  margin-top: 2px;
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.badge {
  margin-left: auto;
  padding: 4px 10px;
  border-radius: var(--radius-pill);
  background: var(--primary);
  color: var(--white);
  font-size: var(--fs-caption);
  font-weight: 600;
}

.menu {
  overflow: hidden;
}

.row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--neutral-3);
  text-align: left;
}

.row:last-child {
  border-bottom: none;
}

.r-label {
  font-size: var(--fs-headline3);
  font-weight: 600;
}

.r-value {
  margin-left: auto;
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.r-arrow {
  font-size: 22px;
  color: var(--neutral-2);
}

.session-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--neutral-3);
}

.session-row:last-of-type {
  border-bottom: none;
}

.d-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--primary);
  flex-shrink: 0;
}

.d-dot.muted {
  background: var(--neutral-3);
}

.current-badge {
  margin-left: 4px;
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  background: var(--primary-2);
  color: var(--primary);
  font-size: var(--fs-caption);
  font-weight: 600;
}

.session-exit {
  flex-shrink: 0;
  padding: 6px 12px;
  border-radius: var(--radius-pill);
  background: #f7f9f8;
  color: var(--accent);
  font-size: var(--fs-caption);
  font-weight: 600;
}

.session-tip {
  padding: 14px 20px;
  font-size: var(--fs-caption);
  color: var(--neutral-2);
}

.revoke-others {
  width: 100%;
  height: 44px;
  border-radius: var(--radius-pill);
  background: var(--white);
  color: var(--accent);
  font-size: var(--fs-body2);
  font-weight: 600;
  box-shadow: var(--shadow-card);
}

.d-info h4 {
  font-size: var(--fs-headline3);
  font-weight: 600;
}

.d-info p {
  margin-top: 2px;
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.delete-warning {
  font-size: var(--fs-body2);
  line-height: 1.6;
  color: var(--neutral-2);
  text-align: center;
}

.danger {
  width: 100%;
  height: 48px;
  border-radius: var(--radius-pill);
  background: var(--white);
  color: var(--accent);
  font-size: var(--fs-headline3);
  font-weight: 600;
  box-shadow: var(--shadow-card);
}

.danger-tip {
  margin-top: 10px;
  text-align: center;
  font-size: var(--fs-caption);
  color: var(--neutral-2);
}

.code-tip {
  font-size: var(--fs-body2);
  line-height: 1.6;
  color: var(--neutral-2);
  text-align: center;
}

.dev-tip {
  font-size: var(--fs-caption);
  color: var(--primary);
  text-align: center;
}

.back-edit {
  width: 100%;
  padding: 8px;
  font-size: var(--fs-body2);
  color: var(--neutral-2);
  text-align: center;
}
</style>
