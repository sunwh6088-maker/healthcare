<script setup lang="ts">
// 模块：数字键盘 —— 登录验证码等场景复用。
// 数字键盘（对照设计稿 Light / Numeric）：1-9 / 0 / 退格
defineEmits<{ (e: 'key', k: string): void; (e: 'delete'): void }>()

const rows = [
  ['1', '2', '3'],
  ['4', '5', '6'],
  ['7', '8', '9'],
  ['', '0', '⌫'],
]
</script>

<template>
  <div class="keypad">
    <div v-for="(row, ri) in rows" :key="ri" class="kp-row">
      <button
        v-for="(k, ki) in row"
        :key="ki"
        class="kp-key num"
        :class="{ empty: k === '', del: k === '⌫' }"
        :disabled="k === ''"
        @click="k === '⌫' ? $emit('delete') : $emit('key', k)"
      >
        {{ k }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.keypad {
  background: #f2f3f5;
  padding: 8px 12px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: auto;
}

.kp-row {
  display: flex;
  gap: 8px;
}

.kp-key {
  flex: 1;
  height: 58px;
  border-radius: 8px;
  background: var(--white);
  box-shadow: 0 1px 2px rgba(20, 12, 7, 0.12);
  font-size: 22px;
  font-weight: 600;
  color: var(--neutral);
  transition: transform 0.08s ease;
}

.kp-key:active {
  transform: scale(0.96);
  background: var(--neutral-3);
}

.kp-key.empty {
  background: transparent;
  box-shadow: none;
  cursor: default;
}

.kp-key.del {
  font-size: 20px;
  color: var(--neutral);
}
</style>
