<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { health } from '../utils/healthStore'

const router = useRouter()
const selected = ref<'female' | 'male' | null>(null)

function next() {
  if (selected.value) health.gender = selected.value
  router.push('/weight')
}
</script>

<template>
  <div class="gender">
    <StatusBar />

    <BackButton class="back-top" @click="router.push('/verify')" />

    <div class="content">
      <h1 class="title">选择您的性别</h1>

      <div class="cards">
        <button
          class="gender-card"
          :class="{ active: selected === 'female' }"
          @click="selected = 'female'"
        >
          <div class="g-illus" style="background: var(--primary-2)">
            <span>👩</span>
          </div>
          <span class="g-label">女</span>
        </button>
        <button
          class="gender-card"
          :class="{ active: selected === 'male' }"
          @click="selected = 'male'"
        >
          <div class="g-illus" style="background: var(--accent-2)">
            <span>👨</span>
          </div>
          <span class="g-label">男</span>
        </button>
      </div>

      <p class="hint">为了给您定制体验，我们需要知道您的性别</p>

      <button class="btn-primary mt-24" @click="next">继续</button>
      <button class="skip-link mt-16" @click="next">宁愿不选择</button>
    </div>
  </div>
</template>

<style scoped>
.gender {
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

.title {
  font-size: var(--fs-title2);
  line-height: var(--lh-title2);
  font-weight: 700;
  text-align: center;
}

.cards {
  margin-top: 40px;
  display: flex;
  gap: 16px;
}

.gender-card {
  flex: 1;
  background: var(--white);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  border: 2px solid transparent;
  padding-bottom: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  transition: all 0.15s ease;
}

.gender-card.active {
  border-color: var(--primary);
}

.g-illus {
  width: 100%;
  height: 120px;
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 56px;
}

.g-label {
  font-size: var(--fs-headline3);
  font-weight: 600;
}

.hint {
  margin-top: 28px;
  text-align: center;
  font-size: var(--fs-body1);
  color: var(--neutral-2);
}

.skip-link {
  text-align: center;
  font-size: var(--fs-headline3);
  color: var(--primary);
  font-weight: 600;
}
</style>
