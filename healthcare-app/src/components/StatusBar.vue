<script setup lang="ts">
// 状态栏：时间 + 信号/电量图标。light 用于深色头部页面
withDefaults(defineProps<{ light?: boolean }>(), { light: false })
</script>

<template>
  <div class="statusbar" :class="{ light }">
    <span class="num">9:41</span>
    <div class="sb-icons">📶 🔋</div>
  </div>
</template>

<style scoped>
.statusbar.light {
  color: var(--white);
}
</style>
