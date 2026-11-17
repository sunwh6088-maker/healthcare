<script setup lang="ts">
// 圆形返回按钮：light 用于深色/彩色头部页面
withDefaults(defineProps<{ light?: boolean }>(), { light: false })
defineEmits<{ (e: 'click'): void }>()
</script>

<template>
  <button
    type="button"
    class="back-btn"
    :class="{ light }"
    aria-label="返回"
    @click="$emit('click')"
  >
    ‹
  </button>
</template>

<style scoped>
.back-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--primary-2);
  font-size: 24px;
  color: var(--neutral);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.12s ease;
}

.back-btn:active {
  transform: scale(0.94);
}

.back-btn.light {
  background: var(--white);
}
</style>
