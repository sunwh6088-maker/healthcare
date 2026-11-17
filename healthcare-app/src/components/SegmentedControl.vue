<script setup lang="ts">
// 分段胶囊选择器：支持 v-model，复用全局 .segmented / .seg 样式
defineProps<{
  modelValue: string
  options: { value: string; label: string }[]
}>()
defineEmits<{ (e: 'update:modelValue', value: string): void }>()
</script>

<template>
  <div class="segmented">
    <button
      v-for="o in options"
      :key="o.value"
      type="button"
      class="seg"
      :class="{ active: modelValue === o.value }"
      @click="$emit('update:modelValue', o.value)"
    >
      {{ o.label }}
    </button>
  </div>
</template>
