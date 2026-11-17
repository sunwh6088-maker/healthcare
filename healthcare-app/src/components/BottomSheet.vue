<script setup lang="ts">
// 通用底部弹层：Teleport 到手机壳，点击遮罩或按 Esc 关闭，打开时聚焦第一个输入框
import { nextTick, onUnmounted, ref, watch } from 'vue'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ (e: 'close'): void }>()

const sheetRef = ref<HTMLElement | null>(null)

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') emit('close')
}

watch(
  () => props.open,
  async (open) => {
    if (open) {
      document.addEventListener('keydown', onKeydown)
      await nextTick()
      const target =
        sheetRef.value?.querySelector<HTMLElement>('input, textarea, select') ?? sheetRef.value
      target?.focus()
    } else {
      document.removeEventListener('keydown', onKeydown)
    }
  },
)

onUnmounted(() => document.removeEventListener('keydown', onKeydown))
</script>

<template>
  <Teleport to=".phone">
    <div v-if="props.open" class="sheet-mask" @click.self="emit('close')">
      <div ref="sheetRef" class="sheet" role="dialog" aria-modal="true" tabindex="-1">
        <slot />
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.sheet-mask {
  position: absolute;
  inset: 0;
  background: rgba(20, 12, 7, 0.45);
  display: flex;
  align-items: flex-end;
  z-index: 100;
}

.sheet {
  width: 100%;
  max-height: 88%;
  overflow-y: auto;
  background: var(--white);
  border-radius: 24px 24px 0 0;
  padding: 20px 24px 28px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  animation: sheet-up 0.22s ease;
}

.sheet:focus {
  outline: none;
}

@keyframes sheet-up {
  from {
    transform: translateY(24px);
    opacity: 0.6;
  }
  to {
    transform: none;
    opacity: 1;
  }
}
</style>
