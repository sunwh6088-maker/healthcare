<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { health } from '../utils/healthStore'

const router = useRouter()

const stepsPct = computed(() => Math.round((health.steps / health.stepsGoal) * 100))
const sleepHours = computed(() => Math.floor(health.sleepMinutes / 60))
const waterBars = computed(() =>
  Array.from({ length: 8 }, (_, i) => i < Math.round((health.waterMl / health.waterGoalMl) * 8)),
)
</script>

<template>
  <div class="home">
    <StatusBar />

    <!-- 问候 + 步数卡片 -->
    <div class="steps-card" @click="router.push('/steps')">
      <div class="greeting">
        <p class="hello">你好，</p>
        <h1 class="name">{{ health.userName }}</h1>
      </div>
      <div class="sneaker">👟</div>
      <div class="steps-info">
        <span class="num big">{{ health.steps.toLocaleString() }}</span>
        <span class="goal">/{{ health.stepsGoal }} 步</span>
      </div>
      <div class="progress-track step-track">
        <div class="progress-fill" :style="{ width: stepsPct + '%' }"></div>
      </div>
      <span class="pct num">{{ stepsPct }}%</span>
    </div>

    <!-- 2x2 功能网格 -->
    <div class="grid">
      <button class="stat-card" @click="router.push('/food')">
        <span class="icon-dot">🍎</span>
        <span class="s-title">食物</span>
        <span class="num s-value">{{ health.foodKcal.toLocaleString() }}</span>
        <span class="s-unit">千卡</span>
      </button>

      <button class="stat-card" @click="router.push('/sleep')">
        <span class="icon-dot">😴</span>
        <span class="s-title">睡眠</span>
        <span class="num s-value">{{ sleepHours }}</span>
        <span class="s-unit">小时</span>
      </button>

      <button class="stat-card water-card" @click="router.push('/water')">
        <span class="icon-dot">💧</span>
        <span class="s-title">水</span>
        <span class="num s-value">{{ (health.waterMl / 1000).toFixed(1) }}</span>
        <span class="s-unit">升</span>
        <div class="mini-chart">
          <span v-for="(b, i) in waterBars" :key="i" class="bar" :class="{ on: b }"></span>
        </div>
        <span class="add-link orange-link">添加</span>
      </button>

      <button class="stat-card" @click="router.push('/exercise')">
        <span class="icon-dot">🏋️</span>
        <span class="s-title">运动</span>
        <span class="num s-value">{{ health.exerciseCount }}</span>
        <span class="s-unit">组</span>
      </button>
    </div>

    <div class="tabbar-spacer"></div>
  </div>
</template>

<style scoped>
.home {
  padding: 0 24px 24px;
}

/* 步数卡片（灰绿圆角大卡） */
.steps-card {
  margin-top: 16px;
  position: relative;
  background: var(--primary-2);
  border-radius: var(--radius-lg);
  padding: 20px;
  height: 137px;
  cursor: pointer;
}

.greeting .hello {
  font-size: var(--fs-headline3);
  color: var(--neutral-2);
}

.greeting .name {
  margin-top: 2px;
  font-size: var(--fs-title3);
  font-weight: 600;
  color: var(--neutral);
}

.sneaker {
  position: absolute;
  top: 8px;
  right: 12px;
  font-size: 56px;
  transform: rotate(-12deg);
}

.steps-info {
  position: absolute;
  bottom: 26px;
  left: 20px;
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.steps-info .big {
  font-size: 32px;
  font-weight: 700;
  color: var(--primary);
}

.steps-info .goal {
  font-size: var(--fs-body2);
  font-weight: 600;
  color: var(--white);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.step-track {
  position: absolute;
  bottom: 14px;
  left: 20px;
  width: 200px;
}

.pct {
  position: absolute;
  bottom: 10px;
  right: 16px;
  font-size: var(--fs-caption);
  font-weight: 600;
  color: var(--primary);
}

/* 2x2 网格 */
.grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.stat-card {
  position: relative;
  background: var(--white);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  height: 156px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 16px;
  text-align: left;
  transition: transform 0.12s ease;
}

.stat-card:active {
  transform: scale(0.96);
}

.icon-dot {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.s-title {
  font-size: var(--fs-headline3);
  font-weight: 600;
  color: var(--neutral);
}

.s-value {
  margin-top: 12px;
  font-size: var(--fs-title3);
  font-weight: 700;
  line-height: 1;
}

.s-unit {
  margin-top: 6px;
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

/* 水卡片（较高，带柱状图与添加按钮） */
.water-card {
  height: 220px;
}

.mini-chart {
  position: absolute;
  top: 60px;
  right: 16px;
  display: flex;
  gap: 6px;
  align-items: flex-end;
  height: 57px;
}

.bar {
  width: 6px;
  height: 24px;
  border-radius: 3px;
  background: var(--neutral-3);
}

.bar.on {
  background: var(--accent);
}

.add-link {
  position: absolute;
  bottom: 14px;
  left: 16px;
  font-weight: 600;
  cursor: pointer;
}
</style>
