<script setup lang="ts">
import { useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { health, stepsToKcal, stepsToKm } from '../utils/healthStore'

const router = useRouter()

const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
// 2021年1月4日-10日（周一至周日）
const days = [
  { d: 4, active: true },
  { d: 5, active: false },
  { d: 6, active: true },
  { d: 7, active: true },
  { d: 8, active: false },
  { d: 9, active: true },
  { d: 10, active: false },
]

const bars = [
  { h: 42, active: true },
  { h: 24, active: false },
  { h: 56, active: true },
  { h: 48, active: true },
  { h: 18, active: false },
  { h: 32, active: false },
  { h: 12, active: false },
]
</script>

<template>
  <div class="calendar-view">
    <StatusBar light />

    <NavBar title="运动日历" light @back="goBack(router, '/exercise')">
      <template #right>
        <button class="time-link" @click="router.push('/exercise-time')">运动时间</button>
      </template>
    </NavBar>

    <!-- 绿色日历卡片 -->
    <div class="cal-card">
      <p class="cal-range num">1月4日 - 1月10日</p>
      <div class="cal-big">
        <span class="num big">{{ health.activeMinutes }}/{{ health.activeGoalMinutes }}</span>
        <span class="avg">平均每日活跃时间</span>
      </div>

      <div class="cal-grid">
        <span v-for="w in weekDays" :key="w" class="num wd">{{ w }}</span>
        <button v-for="d in days" :key="d.d" class="day" :class="{ active: d.active }">
          {{ d.d }}
        </button>
      </div>

      <div class="cal-bars">
        <span
          v-for="(b, i) in bars"
          :key="i"
          class="bar"
          :class="{ on: b.active }"
          :style="{ height: b.h + 'px' }"
        ></span>
      </div>
    </div>

    <!-- 统计 -->
    <div class="stats">
      <div class="stat">
        <span class="s-label">距离</span>
        <span class="num s-value">{{ stepsToKm(health.steps) }} 公里</span>
      </div>
      <div class="stat">
        <span class="s-label">总步数</span>
        <span class="num s-value">{{ health.steps }}/18000</span>
      </div>
      <div class="stat">
        <span class="s-label">燃烧的卡路里</span>
        <span class="num s-value">{{ stepsToKcal(health.steps) }} 千卡</span>
      </div>
    </div>

    <button class="green-link edit" @click="router.push('/set-goal?type=active')">编辑目标</button>
  </div>
</template>

<style scoped>
.calendar-view {
  padding: 0 24px 24px;
}

.time-link {
  font-size: var(--fs-body2);
  color: var(--white);
  font-weight: 600;
}

.cal-card {
  margin-top: 12px;
  background: var(--primary);
  border-radius: var(--radius-lg);
  padding: 20px;
  color: var(--white);
}

.cal-range {
  text-align: center;
  font-size: var(--fs-headline3);
  font-weight: 600;
}

.cal-big {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.cal-big .big {
  font-size: var(--fs-title2);
  font-weight: 700;
  line-height: 1.2;
}

.avg {
  font-size: var(--fs-caption);
  color: var(--neutral-2);
}

.cal-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
}

.wd {
  text-align: center;
  font-size: var(--fs-caption);
  color: var(--white);
  opacity: 0.9;
}

.day {
  height: 32px;
  border-radius: 50%;
  color: var(--white);
  font-size: var(--fs-caption);
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.day.active {
  background: var(--white);
  color: var(--primary);
}

.cal-bars {
  margin-top: 16px;
  display: flex;
  gap: 8px;
  align-items: flex-end;
  height: 60px;
}

.bar {
  flex: 1;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.35);
}

.bar.on {
  background: var(--white);
}

.stats {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.stat {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.s-label {
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.s-value {
  font-size: var(--fs-headline3);
  font-weight: 700;
}

.edit {
  margin-top: 20px;
  text-align: right;
  display: block;
  font-weight: 600;
}
</style>
