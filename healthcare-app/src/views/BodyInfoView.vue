<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { toast } from '../utils/toast'
import { health } from '../utils/healthStore'
import { validateBodyInfo } from '../utils/body'

const route = useRoute()
const router = useRouter()
const fromProfile = computed(() => route.query.from === 'profile')

const weight = ref(health.weightKg)
const height = ref(health.heightCm)
const age = ref(health.age)

const bmi = computed(() => {
  const meters = Number(height.value) / 100
  const value = Number(weight.value) / (meters * meters)
  return meters > 0 && Number.isFinite(value) && value > 0 ? value.toFixed(1) : null
})

function back() {
  goBack(router, fromProfile.value ? '/profile' : '/weight')
}

function save() {
  const info = {
    weightKg: Number(weight.value),
    heightCm: Math.round(Number(height.value)),
    age: Math.round(Number(age.value)),
  }
  const error = validateBodyInfo(info)
  if (error) {
    toast(error)
    return
  }
  health.weightKg = Math.round(info.weightKg * 10) / 10
  health.heightCm = info.heightCm
  health.age = info.age
  toast('身体数据已保存')
  if (fromProfile.value) router.replace('/profile')
  else router.push('/home')
}

function skip() {
  router.push('/home')
}
</script>

<template>
  <div class="body-info">
    <StatusBar />

    <NavBar :title="fromProfile ? '编辑身体数据' : '完善身体数据'" @back="back" />

    <div class="content">
      <p class="intro">这些数据用于计算 BMI 和健康目标，之后也可以在我的页面修改。</p>

      <div class="fields">
        <label class="field card">
          <span class="f-label">体重</span>
          <input
            v-model.number="weight"
            class="num f-input"
            type="number"
            inputmode="decimal"
            min="20"
            max="300"
            step="0.1"
          />
          <span class="f-unit">公斤</span>
        </label>

        <label class="field card">
          <span class="f-label">身高</span>
          <input
            v-model.number="height"
            class="num f-input"
            type="number"
            inputmode="numeric"
            min="50"
            max="250"
            step="1"
          />
          <span class="f-unit">厘米</span>
        </label>

        <label class="field card">
          <span class="f-label">年龄</span>
          <input
            v-model.number="age"
            class="num f-input"
            type="number"
            inputmode="numeric"
            min="1"
            max="120"
            step="1"
          />
          <span class="f-unit">岁</span>
        </label>
      </div>

      <p v-if="bmi" class="bmi">
        当前 BMI <span class="num">{{ bmi }}</span>
      </p>

      <button class="btn-primary mt-24" @click="save">保存</button>
      <button v-if="!fromProfile" class="skip mt-16" @click="skip">稍后设置</button>
    </div>
  </div>
</template>

<style scoped>
.body-info {
  min-height: 100%;
  background: var(--white);
}

.content {
  padding: 0 24px 24px;
}

.intro {
  margin-top: 8px;
  font-size: var(--fs-body2);
  line-height: var(--lh-body2);
  color: var(--neutral-2);
}

.fields {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field {
  display: flex;
  align-items: center;
  padding: 16px 20px;
}

.f-label {
  width: 64px;
  font-size: var(--fs-headline3);
  font-weight: 600;
}

.f-input {
  flex: 1;
  min-width: 0;
  text-align: right;
  font-size: var(--fs-title3);
  font-weight: 700;
  color: var(--primary);
  background: transparent;
}

.f-unit {
  width: 48px;
  text-align: right;
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.bmi {
  margin-top: 16px;
  text-align: center;
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.bmi .num {
  color: var(--primary);
  font-weight: 700;
}

.skip {
  width: 100%;
  text-align: center;
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}
</style>
