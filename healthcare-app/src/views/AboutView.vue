<script setup lang="ts">
// 模块：关于我们 —— 版本、功能简介、技术栈和开源地址。
import { useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'

const router = useRouter()

const features = [
  '食物 / 饮水 / 睡眠 / 步数 / 运动记录',
  '健康数据跨设备同步',
  '运动记录新增、编辑和删除',
  '基础数据由数据库统一维护',
  'HttpOnly Cookie 登录与验证码限流',
]

const stack = ['Vue 3', 'TypeScript', 'Vite', 'Spring Boot', 'MySQL', 'Flyway']

function openRepo() {
  window.open('https://github.com/sunwh6088-maker/healthcare', '_blank')
}
</script>

<template>
  <div class="about">
    <StatusBar />
    <NavBar title="关于我们" @back="goBack(router, '/settings')" />

    <div class="hero card mt-16">
      <div class="logo">🌿</div>
      <h1>健康追踪</h1>
      <p class="version num">v1.0.0</p>
      <p class="desc">记录每一天的健康数据，让好习惯看得见。</p>
    </div>

    <h3 class="section-title mt-20">主要功能</h3>
    <div class="card mt-12 feature-card">
      <p v-for="item in features" :key="item" class="feature">
        <span class="dot">✓</span>{{ item }}
      </p>
    </div>

    <h3 class="section-title mt-20">技术栈</h3>
    <div class="stack mt-12">
      <span v-for="item in stack" :key="item" class="chip">{{ item }}</span>
    </div>

    <h3 class="section-title mt-20">隐私说明</h3>
    <div class="card mt-12 privacy">
      <p>
        健康数据属于敏感信息。账号和健康数据仅用于本应用的功能展示与同步，不会出售或提供给第三方。
      </p>
      <p>你可以在「账户与安全」中注销账号，注销后所有健康数据会被删除且无法恢复。</p>
    </div>

    <div class="links card mt-16">
      <button class="link-row" @click="openRepo">
        <span>项目源码</span>
        <span class="arrow">›</span>
      </button>
      <button class="link-row" @click="router.push('/help')">
        <span>帮助与反馈</span>
        <span class="arrow">›</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.about {
  padding: 0 24px 24px;
}

.hero {
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.logo {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  background: var(--primary-2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
}

.hero h1 {
  margin-top: 12px;
  font-size: var(--fs-title3);
  font-weight: 700;
}

.version {
  margin-top: 2px;
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.desc {
  margin-top: 10px;
  font-size: var(--fs-body2);
  line-height: 1.6;
  color: var(--neutral-2);
}

.feature-card {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.feature {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: var(--fs-body2);
  color: var(--neutral);
}

.dot {
  color: var(--primary);
  font-weight: 700;
}

.stack {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  padding: 7px 14px;
  border-radius: var(--radius-pill);
  background: var(--primary-2);
  color: var(--primary);
  font-size: var(--fs-body2);
  font-weight: 600;
}

.privacy {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.privacy p {
  font-size: var(--fs-body2);
  line-height: 1.7;
  color: var(--neutral-2);
}

.links {
  overflow: hidden;
}

.link-row {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--neutral-3);
  font-size: var(--fs-headline3);
  font-weight: 600;
  text-align: left;
}

.link-row:last-child {
  border-bottom: none;
}

.arrow {
  color: var(--neutral-2);
  font-size: 22px;
}
</style>
