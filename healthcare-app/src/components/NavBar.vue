<script setup lang="ts">
// 顶部导航：返回按钮 + 标题 + 右侧操作区
withDefaults(defineProps<{ title: string; light?: boolean }>(), { light: false })
defineEmits<{ back: [] }>()
</script>

<template>
  <div class="navbar">
    <BackButton :light="light" @click="$emit('back')" />
    <span class="nav-title" :class="{ 'light-title': light }">{{ title }}</span>
    <div class="nav-right">
      <slot name="right">
        <span style="display: inline-block; width: 44px"></span>
      </slot>
    </div>
  </div>
</template>

<style scoped>
.light-title {
  color: var(--white);
}

.nav-right {
  min-width: 44px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}
</style>
