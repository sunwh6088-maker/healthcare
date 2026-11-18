<script setup lang="ts">
// 模块：我的页 —— 个人资料和功能入口。
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { health } from '../utils/healthStore'
import { api } from '../api'
import { toast } from '../utils/toast'

const router = useRouter()
const sleepHours = computed(() => Math.floor(health.sleepMinutes / 60))

// 头像上传：点击头像选择本地图片，前端先做类型/大小校验
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)

function pickAvatar() {
  fileInput.value?.click()
}

async function onAvatarChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    toast('仅支持 JPG、PNG 或 WebP 图片')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    toast('头像不能超过 2MB')
    return
  }
  uploading.value = true
  try {
    const user = await api.uploadAvatar(file)
    health.avatarUrl = user.avatarUrl
    toast('头像已更新 ✓')
  } catch (error) {
    toast(error instanceof Error ? error.message : '头像上传失败')
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="profile">
    <StatusBar />

    <!-- 头像与昵称 -->
    <div class="head">
      <button
        type="button"
        class="avatar"
        :aria-label="uploading ? '正在上传头像' : '更换头像'"
        @click="pickAvatar"
      >
        <img v-if="health.avatarUrl" :src="health.avatarUrl" alt="头像" />
        <span v-else>🧑</span>
        <span class="avatar-edit">{{ uploading ? '…' : '✎' }}</span>
      </button>
      <input
        ref="fileInput"
        class="file-input"
        type="file"
        accept="image/jpeg,image/png,image/webp"
        @change="onAvatarChange"
      />
      <h1 class="name">{{ health.userName }}</h1>
      <p class="bio">保持健康，保持活力，遇见更好的自己。</p>
    </div>

    <!-- 基础数据 -->
    <div class="between mt-20">
      <span class="section-title">身体数据</span>
      <button class="green-link" @click="router.push('/body-info?from=profile')">编辑</button>
    </div>
    <div class="stats">
      <button type="button" class="stat card" @click="router.push('/body-info?from=profile')">
        <span class="s-label">体重</span>
        <span class="num s-value">{{ health.weightKg }}<span class="s-unit">公斤</span></span>
      </button>
      <button type="button" class="stat card" @click="router.push('/body-info?from=profile')">
        <span class="s-label">年龄</span>
        <span class="num s-value">{{ health.age }}<span class="s-unit">岁</span></span>
      </button>
      <button type="button" class="stat card" @click="router.push('/body-info?from=profile')">
        <span class="s-label">身高</span>
        <span class="num s-value">{{ health.heightCm }}<span class="s-unit">厘米</span></span>
      </button>
    </div>

    <!-- 彩色数据卡 -->
    <div class="color-cards">
      <button class="c-card" style="background: var(--primary)" @click="router.push('/food')">
        <span class="c-title">食物</span>
        <span class="num c-value">{{ health.foodKcal.toLocaleString() }}</span>
        <span class="c-unit">千卡</span>
      </button>
      <button class="c-card" style="background: var(--accent)" @click="router.push('/sleep')">
        <span class="c-title">睡觉</span>
        <span class="num c-value">{{ sleepHours }}</span>
        <span class="c-unit">小时</span>
      </button>
    </div>

    <!-- 菜单 -->
    <div class="menu card mt-16">
      <button class="menu-row" @click="router.push('/devices')">
        <span class="m-icon">⌚</span>
        <span class="m-label">我的设备</span>
        <span class="m-arrow">›</span>
      </button>
      <button class="menu-row" @click="router.push('/history')">
        <span class="m-icon">📊</span>
        <span class="m-label">历史记录</span>
        <span class="m-arrow">›</span>
      </button>
      <button class="menu-row" @click="router.push('/settings')">
        <span class="m-icon">⚙️</span>
        <span class="m-label">设置</span>
        <span class="m-arrow">›</span>
      </button>
      <button class="menu-row" @click="router.push('/help')">
        <span class="m-icon">❓</span>
        <span class="m-label">帮助与反馈</span>
        <span class="m-arrow">›</span>
      </button>
    </div>

    <div class="tabbar-spacer"></div>
  </div>
</template>

<style scoped>
.profile {
  padding: 0 24px 24px;
}

.head {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.avatar {
  position: relative;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: var(--primary-2);
  border: 4px solid var(--white);
  box-shadow: var(--shadow-card);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  padding: 0;
  overflow: hidden;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-edit {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--primary);
  color: var(--white);
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--white);
}

.file-input {
  display: none;
}

.name {
  font-size: var(--fs-headline1);
  font-weight: 700;
}

.bio {
  font-size: var(--fs-body2);
  color: var(--neutral-2);
  text-align: center;
  max-width: 260px;
}

.stats {
  margin-top: 12px;
  display: flex;
  gap: 12px;
}

.stat {
  flex: 1;
  padding: 16px 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--neutral);
  transition: transform 0.12s ease;
}

.stat:active {
  transform: scale(0.97);
}

.s-label {
  font-size: var(--fs-body2);
  color: var(--primary);
}

.s-value {
  font-size: var(--fs-headline1);
  font-weight: 700;
}

.s-unit {
  font-size: var(--fs-body2);
  color: var(--neutral-2);
  margin-left: 2px;
}

.color-cards {
  margin-top: 16px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.c-card {
  border-radius: var(--radius-lg);
  padding: 16px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  color: var(--white);
  text-align: left;
}

.c-title {
  font-size: var(--fs-headline3);
}
.c-value {
  font-size: var(--fs-title3);
  font-weight: 700;
  line-height: 1;
}
.c-unit {
  font-size: var(--fs-body2);
  opacity: 0.9;
}

.menu {
  overflow: hidden;
}

.menu-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--neutral-3);
  text-align: left;
}

.menu-row:last-child {
  border-bottom: none;
}

.m-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--primary-2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.m-label {
  flex: 1;
  font-size: var(--fs-headline3);
  font-weight: 600;
}

.m-arrow {
  font-size: 22px;
  color: var(--neutral-2);
}
</style>
