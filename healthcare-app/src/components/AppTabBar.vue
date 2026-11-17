<script setup lang="ts">
// 模块：底部 Tab 栏 —— 根据当前路由高亮。
// 底部 Tab 栏（由数据驱动）
const tabs = [
  { to: '/home', icon: '🏠', label: '首页' },
  { to: '/discover', icon: '🧭', label: '发现' },
  { to: '/devices', icon: '⌚', label: '设备' },
  { to: '/profile', icon: '👤', label: '我的' },
]
</script>

<template>
  <nav class="tabbar">
    <router-link v-for="t in tabs" :key="t.to" :to="t.to" class="tab" :aria-label="t.label">
      <span class="tab-icon">{{ t.icon }}</span>
      <span class="tab-label">{{ t.label }}</span>
    </router-link>
  </nav>
</template>
