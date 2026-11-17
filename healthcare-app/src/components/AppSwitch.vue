<script setup lang="ts">
// 通用开关：支持 v-model，设置/闹钟等页面复用
withDefaults(defineProps<{ modelValue: boolean; label?: string }>(), { label: '' })
defineEmits<{ (e: 'update:modelValue', value: boolean): void }>()
</script>

<template>
  <button
    type="button"
    class="switch"
    :class="{ on: modelValue }"
    role="switch"
    :aria-checked="modelValue"
    :aria-label="label || undefined"
    @click="$emit('update:modelValue', !modelValue)"
  >
    <span class="knob"></span>
  </button>
</template>

<style scoped>
.switch {
  width: 51px;
  height: 31px;
  border-radius: 999px;
  background: var(--neutral-3);
  position: relative;
  flex-shrink: 0;
  transition: background 0.2s ease;
}

.switch.on {
  background: var(--primary);
}

.knob {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 25px;
  height: 25px;
  border-radius: 50%;
  background: var(--white);
  transition: left 0.2s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.switch.on .knob {
  left: 23px;
}
</style>
