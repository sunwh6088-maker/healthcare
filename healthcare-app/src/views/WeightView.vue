<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { health } from '../utils/healthStore'

const router = useRouter()
const unit = ref('公斤')
const weight = ref(Math.round(health.weightKg))

const unitOptions = [
  { value: '公斤', label: '公斤' },
  { value: '磅', label: '磅' },
]

const KG_TO_LB = 2.20462

// 切换单位时转换当前数值
watch(unit, (next, prev) => {
  if (next === prev) return
  weight.value =
    next === '磅' ? Math.round(weight.value * KG_TO_LB) : Math.round(weight.value / KG_TO_LB)
})

function change(delta: number) {
  const min = unit.value === '公斤' ? 30 : 66
  const max = unit.value === '公斤' ? 120 : 265
  weight.value = Math.min(max, Math.max(min, weight.value + delta))
}

const scale = computed(() => (unit.value === '公斤' ? [40, 60, 80] : [88, 132, 176]))

function done() {
  health.weightKg =
    unit.value === '公斤' ? weight.value : Math.round((weight.value / KG_TO_LB) * 10) / 10
  router.push('/body-info')
}
</script>

<template>
  <div class="weight">
    <StatusBar />

    <BackButton class="back-top" @click="router.push('/gender')" />

    <div class="content">
      <SegmentedControl v-model="unit" :options="unitOptions" class="unit-seg" />

      <div class="dial card">
        <button class="minus" @click="change(-1)">−</button>
        <div class="dial-center">
          <span class="num big">{{ weight }}</span>
          <span class="unit">{{ unit }}</span>
        </div>
        <button class="plus" @click="change(1)">+</button>
        <div class="scale num">
          <span v-for="s in scale" :key="s">{{ s }}</span>
        </div>
      </div>

      <p class="hint">你的体重是多少？</p>

      <button class="btn-primary mt-24" @click="done">完成</button>
    </div>
  </div>
</template>

<style scoped>
.weight {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0 24px;
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-bottom: 40px;
}

.unit-seg {
  align-self: center;
  background: var(--white);
  box-shadow: var(--shadow-card);
}

.unit-seg :deep(.seg) {
  min-width: 64px;
  text-align: center;
}

.dial {
  margin-top: 32px;
  height: 280px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 40px;
  position: relative;
}

.minus,
.plus {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--primary-2);
  color: var(--primary);
  font-size: 22px;
  font-weight: 700;
  z-index: 2;
}

.minus:active,
.plus:active {
  transform: scale(0.9);
}

.dial-center {
  display: flex;
  align-items: baseline;
  gap: 6px;
  z-index: 2;
}

.big {
  font-size: 56px;
  font-weight: 700;
  color: var(--primary);
}

.unit {
  font-size: var(--fs-headline2);
  color: var(--primary);
  font-weight: 700;
  text-transform: uppercase;
}

.scale {
  position: absolute;
  bottom: 16px;
  left: 40px;
  right: 40px;
  display: flex;
  justify-content: space-between;
  font-size: var(--fs-caption);
  color: var(--neutral-2);
}

.hint {
  margin-top: 28px;
  text-align: center;
  font-size: var(--fs-body1);
  color: var(--neutral-2);
}
</style>
