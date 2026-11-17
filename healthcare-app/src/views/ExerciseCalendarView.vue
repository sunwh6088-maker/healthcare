<script setup lang="ts">
// 模块：运动日历 —— 按本周真实记录展示活跃天数和柱状图。
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { health, stepsToKcal, stepsToKm } from '../utils/healthStore'
import { api, type ExerciseLog } from '../api'
import { toast } from '../utils/toast'

const router = useRouter()
const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const logs = ref<ExerciseLog[]>([])

// 本周（周一至周日）
const weekDates = (() => {
  const today = new Date()
  const day = today.getDay()
  const diff = day === 0 ? -6 : 1 - day
  const start = new Date(today)
  start.setDate(today.getDate() + diff)
  start.setHours(0, 0, 0, 0)
  return Array.from({ length: 7 }, (_, index) => {
    const date = new Date(start)
    date.setDate(start.getDate() + index)
    return date
  })
})()

function isoDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const minutesByDate = computed(() => {
  const map = new Map<string, number>()
  for (const log of logs.value) {
    map.set(log.date, (map.get(log.date) ?? 0) + log.minutes)
  }
  return map
})

const days = computed(() =>
  weekDates.map((date) => {
    const iso = isoDate(date)
    const minutes = minutesByDate.value.get(iso) ?? 0
    return { d: date.getDate(), iso, minutes, active: minutes > 0 }
  }),
)

const bars = computed(() =>
  days.value.map((day) => ({
    h: Math.min(60, Math.max(8, Math.round(day.minutes * 1.2))),
    active: day.active,
  })),
)

const weekMinutes = computed(() => days.value.reduce((sum, day) => sum + day.minutes, 0))
const avgDaily = computed(() => Math.round(weekMinutes.value / 7))
const rangeText = computed(() => {
  const start = weekDates[0]
  const end = weekDates[6]
  const startText = `${start.getMonth() + 1}月${start.getDate()}日`
  const endText =
    start.getMonth() === end.getMonth()
      ? `${end.getDate()}日`
      : `${end.getMonth() + 1}月${end.getDate()}日`
  return `${startText} - ${endText}`
})

function showDay(day: { iso: string; minutes: number }) {
  toast(`${day.iso}：${day.minutes} 分钟`)
}

async function loadLogs() {
  try {
    logs.value = await api.listExerciseLogs()
  } catch (error) {
    toast(error instanceof Error ? error.message : '加载运动记录失败')
  }
}

onMounted(loadLogs)
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
      <p class="cal-range num">{{ rangeText }}</p>
      <div class="cal-big">
        <span class="num big">{{ avgDaily }}/{{ health.activeGoalMinutes }}</span>
        <span class="avg">平均每日活跃时间</span>
      </div>

      <div class="cal-grid">
        <span v-for="w in weekDays" :key="w" class="num wd">{{ w }}</span>
        <button
          v-for="d in days"
          :key="d.iso"
          class="day"
          :class="{ active: d.active }"
          @click="showDay(d)"
        >
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
