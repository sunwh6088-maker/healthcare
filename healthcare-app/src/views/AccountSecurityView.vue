<script setup lang="ts">
// 模块：账户与安全 —— 手机号/邮箱、密码、设备与注销入口。
import { computed, onMounted, ref } from 'vue'
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
import { api } from '../api'

const router = useRouter()

const maskedPhone = computed(() => maskPhone(health.accountPhone))
const hasPassword = ref<boolean | null>(null)
const deleteOpen = ref(false)
const deleteText = ref('')
const deleting = ref(false)

const deviceLabel = computed(() => {
  const ua = navigator.userAgent
  const browser = /Edg\//.test(ua)
    ? 'Edge'
    : /Chrome\//.test(ua)
      ? 'Chrome'
      : /Firefox\//.test(ua)
        ? 'Firefox'
        : /Safari\//.test(ua)
          ? 'Safari'
          : '浏览器'
  const system = /Windows NT 10/.test(ua)
    ? 'Windows'
    : /Mac OS X/.test(ua)
      ? 'macOS'
      : /Android/.test(ua)
        ? 'Android'
        : /iPhone|iPad/.test(ua)
          ? 'iOS'
          : '本机'
  return `${system} · ${browser}`
})

onMounted(async () => {
  try {
    const user = await api.me()
    hasPassword.value = user.hasPassword
  } catch {
    // 资料加载失败时保留占位
  }
})

type EditField = 'name' | 'phone' | 'email'
const editField = ref<EditField | null>(null)
const editValue = ref('')
const saving = ref(false)

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
    field === 'name'
      ? health.userName
      : field === 'phone'
        ? formatPhone(health.accountPhone)
        : health.accountEmail
}

function closeEdit() {
  editField.value = null
}

async function saveEdit() {
  if (!editField.value) return
  const value =
    editField.value === 'phone' ? normalizePhone(editValue.value) : editValue.value.trim()
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
  if (editField.value === 'phone' && !isValidPhone(value)) {
    toast('请输入正确的 11 位中国大陆手机号')
    return
  }
  if (editField.value === 'email' && !isValidEmail(value)) {
    toast('请输入有效的邮箱地址')
    return
  }

  saving.value = true
  try {
    const payload =
      editField.value === 'name'
        ? { name: value }
        : editField.value === 'phone'
          ? { phone: value }
          : { email: value }
    const user = await api.updateMe(payload)
    health.userName = user.name
    health.accountPhone = user.phone
    health.accountEmail = user.email
    toast(
      editField.value === 'name'
        ? '昵称已更新'
        : editField.value === 'phone'
          ? '手机号已更新'
          : '邮箱已更新',
    )
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
      <div class="avatar">🧑</div>
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

    <!-- 当前登录设备 -->
    <div class="device card mt-16">
      <span class="d-dot"></span>
      <div class="d-info">
        <h4>{{ deviceLabel }}</h4>
        <p>当前设备 · 刚刚活跃</p>
      </div>
    </div>

    <button class="danger mt-24" @click="deleteOpen = true">注销账户</button>
    <p class="danger-tip">注销后所有健康数据将被清除，且无法恢复</p>

    <!-- 修改手机号 / 邮箱 -->
    <BottomSheet :open="editField !== null" @close="closeEdit">
      <h3 class="sheet-title">{{ sheetTitle }}</h3>

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
          :class="{ disabled: saving }"
          :disabled="saving"
          @click="saveEdit"
        >
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
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

.device {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
}

.d-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--primary);
  flex-shrink: 0;
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
</style>
