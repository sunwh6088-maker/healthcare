<script setup lang="ts">
// 模块：运动时间 —— 按日/周/月展示真实运动时长。
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { toast } from '../utils/toast'
import { health, stepsToKcal, stepsToKm } from '../utils/healthStore'
import { api, type ExerciseLog } from '../api'

const router = useRouter()
const view = ref<'day' | 'week' | 'month'>('week')
const menuOpen = ref(false)
const alarmOn = ref(localStorage.getItem('healthcare-exercise-reminder') === '1')
const logs = ref<ExerciseLog[]>([])

const today = new Date()
const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const weekdayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

function startOfWeek(date: Date) {
  const result = new Date(date)
  const day = result.getDay()
  result.setDate(result.getDate() + (day === 0 ? -6 : 1 - day))
  result.setHours(0, 0, 0, 0)
  return result
}

function isoDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const weekStart = startOfWeek(today)
const weekDates = Array.from({ length: 7 }, (_, index) => {
  const date = new Date(weekStart)
  date.setDate(weekStart.getDate() + index)
  return date
})

const minutesByDate = computed(() => {
  const map = new Map<string, number>()
  for (const log of logs.value) {
    map.set(log.date, (map.get(log.date) ?? 0) + log.minutes)
  }
  return map
})

const todayMinutes = computed(() => minutesByDate.value.get(isoDate(today)) ?? 0)
const weekTotal = computed(() =>
  weekDates.reduce((sum, date) => sum + (minutesByDate.value.get(isoDate(date)) ?? 0), 0),
)
const monthTotal = computed(() => {
  const cutoff = new Date(today)
  cutoff.setDate(cutoff.getDate() - 27)
  cutoff.setHours(0, 0, 0, 0)
  return logs.value
    .filter((log) => new Date(`${log.date}T00:00:00`) >= cutoff)
    .reduce((sum, log) => sum + log.minutes, 0)
})

const weekData = computed(() =>
  weekDates.map((date, index) => ({
    label: weekDays[index],
    minutes: minutesByDate.value.get(isoDate(date)) ?? 0,
  })),
)

const monthData = computed(() => {
  const buckets = [0, 0, 0, 0]
  const thisWeek = startOfWeek(today)
  for (const log of logs.value) {
    const date = new Date(`${log.date}T00:00:00`)
    const weeksAgo = Math.round(
      (thisWeek.getTime() - startOfWeek(date).getTime()) / (7 * 24 * 60 * 60 * 1000),
    )
    if (weeksAgo >= 0 && weeksAgo < 4) {
      buckets[3 - weeksAgo] += log.minutes
    }
  }
  return buckets.map((minutes, index) => ({ label: `第${index + 1}周`, minutes }))
})

const lastDays = computed(() =>
  Array.from({ length: 7 }, (_, index) => {
    const date = new Date(today)
    date.setDate(today.getDate() - (6 - index))
    return {
      label: `${date.getMonth() + 1}/${date.getDate()}`,
      minutes: minutesByDate.value.get(isoDate(date)) ?? 0,
    }
  }),
)

const chartData = computed(() => (view.value === 'month' ? monthData.value : weekData.value))
const chartBars = computed(() => {
  const max = Math.max(30, ...chartData.value.map((item) => item.minutes))
  return chartData.value.map((item) => ({
    label: item.label,
    h: Math.max(6, Math.round((item.minutes / max) * 180)),
    hl: item.minutes > 0,
  }))
})

const dayBars = computed(() => {
  const max = Math.max(30, ...lastDays.value.map((item) => item.minutes))
  return lastDays.value.map((item) => ({
    label: item.label,
    h: Math.max(6, Math.round((item.minutes / max) * 180)),
    dim: item.minutes === 0,
  }))
})

const axis = computed(() => {
  const source = view.value === 'day' ? lastDays.value : chartData.value
  const max = Math.max(30, ...source.map((item) => item.minutes))
  return [1, 0.75, 0.5, 0.25].map((factor) => String(Math.round(max * factor)))
})

const bigNum = computed(() =>
  view.value === 'month' ? Math.round(monthTotal.value / 28) : Math.round(weekTotal.value / 7),
)
const dayMinutes = computed(() => todayMinutes.value)
const dayTitle = computed(
  () => `${today.getMonth() + 1}月${today.getDate()}日 ${weekdayNames[today.getDay()]}`,
)
const rangeText = computed(() => {
  if (view.value === 'month') return '最近 4 周'
  const start = weekDates[0]
  const end = weekDates[6]
  const endText =
    start.getMonth() === end.getMonth()
      ? `${end.getDate()}日`
      : `${end.getMonth() + 1}月${end.getDate()}日`
  return `${start.getMonth() + 1}月${start.getDate()}日 - ${endText}`
})

const isDark = computed(() => view.value !== 'day')

const views = [
  { key: 'day' as const, label: '日' },
  { key: 'week' as const, label: '周' },
  { key: 'month' as const, label: '月' },
]

function pickView(next: 'day' | 'week' | 'month') {
  view.value = next
  menuOpen.value = false
}

function toggleAlarm() {
  alarmOn.value = !alarmOn.value
  localStorage.setItem('healthcare-exercise-reminder', alarmOn.value ? '1' : '0')
  toast(alarmOn.value ? '已开启运动提醒 ⏰' : '已关闭运动提醒')
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
  <div class="et" :class="{ dark: isDark }">
    <StatusBar :light="isDark" />

    <NavBar :light="isDark" title="运动时间" @back="goBack(router, '/exercise/calendar')">
      <template #right>
        <button
          class="alarm"
          :class="{ on: alarmOn }"
          :aria-label="alarmOn ? '关闭运动提醒' : '开启运动提醒'"
          @click="toggleAlarm"
        >
          ⏰
        </button>
      </template>
    </NavBar>

    <!-- 深绿周视图（对照「运动时间 #1」画板） -->
    <template v-if="view !== 'day'">
      <div class="hero">
        <!-- 日期范围（点击打开日/周/月菜单） -->
        <button class="range-row" @click="menuOpen = true">
          <span class="arrow">‹</span>
          <span class="num range-text">{{ rangeText }}</span>
          <span class="arrow">›</span>
        </button>

        <div class="big-num num">{{ bigNum }}/{{ health.activeGoalMinutes }}</div>
        <div class="big-label">平均每日活跃时间</div>

        <!-- 柱状图 -->
        <div class="chart">
          <div class="plot">
            <div class="y-axis num">
              <span v-for="v in axis" :key="v">{{ v }}</span>
            </div>
            <div class="bars">
              <div v-for="(b, i) in chartBars" :key="i" class="bar-col">
                <span class="bar" :class="{ hl: b.hl }" :style="{ height: b.h + 'px' }"></span>
              </div>
            </div>
          </div>
          <div class="x-axis num">
            <span v-for="(b, i) in chartBars" :key="i">{{ b.label }}</span>
          </div>
        </div>
      </div>

      <!-- 统计（三行） -->
      <div class="stats">
        <div class="stat-row">
          <span class="s-label">距离</span>
          <span class="num s-value">{{ stepsToKm(health.steps) }} 公里</span>
        </div>
        <div class="stat-row">
          <span class="s-label">总步数</span>
          <span class="num s-value">{{ health.steps }}/18000</span>
        </div>
        <div class="stat-row">
          <span class="s-label">燃烧的卡路里</span>
          <span class="num s-value">{{ stepsToKcal(health.steps) }} 千卡</span>
        </div>
      </div>
    </template>

    <!-- 日视图（对照「运动时间」画板：灰绿图表卡） -->
    <template v-else>
      <div class="day-card card">
        <button class="day-title num" @click="menuOpen = true">{{ dayTitle }} ▾</button>

        <div class="day-head">
          <span class="num day-big">{{ dayMinutes }}/{{ health.activeGoalMinutes }}</span>
          <span class="day-mins">分钟</span>
        </div>

        <div class="chart">
          <div class="plot">
            <div class="y-axis num">
              <span v-for="v in axis" :key="v">{{ v }}</span>
            </div>
            <div class="bars">
              <div v-for="(b, i) in dayBars" :key="i" class="bar-col">
                <span
                  class="bar dbar"
                  :class="{ dim: b.dim }"
                  :style="{ height: b.h + 'px' }"
                ></span>
              </div>
            </div>
          </div>
          <div class="x-axis num">
            <span v-for="(b, i) in dayBars" :key="i" class="axis-cell">{{ b.label }}</span>
          </div>
        </div>
      </div>

      <div class="stats">
        <div class="stat-row">
          <span class="s-label">距离</span>
          <span class="num s-value">{{ stepsToKm(health.steps) }} 公里</span>
        </div>
        <div class="stat-row">
          <span class="s-label">燃烧的卡路里</span>
          <span class="num s-value">{{ stepsToKcal(health.steps) }} 千卡</span>
        </div>
      </div>
    </template>

    <!-- 编辑目标按钮 -->
    <button class="edit-btn" @click="router.push('/set-goal?type=active')">编辑目标</button>

    <!-- 日/周/月 弹出菜单（对照「弹出运动时间」画板） -->
    <Teleport to=".phone">
      <div v-if="menuOpen" class="mask" @click="menuOpen = false"></div>
      <div v-if="menuOpen" class="picker">
        <button
          v-for="v in views"
          :key="v.key"
          class="p-item"
          :class="{ active: view === v.key }"
          @click="pickView(v.key)"
        >
          {{ v.label }}
        </button>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.et {
  min-height: 100%;
  background: var(--white);
  padding-bottom: 24px;
}

/* ===== 深绿 hero 区（0-534） ===== */
.hero {
  background: var(--primary);
  padding: 8px 24px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.alarm {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dark .alarm {
  background: rgba(255, 255, 255, 0.18);
}
.alarm.on {
  background: var(--accent);
}

.range-row {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 14px;
  color: var(--white);
}

.arrow {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.85);
}

.range-text {
  font-size: var(--fs-headline3);
  font-weight: 600;
}

.big-num {
  margin-top: 20px;
  font-size: var(--fs-title2);
  font-weight: 700;
  color: var(--white);
  line-height: 1.2;
}

.big-label {
  margin-top: 6px;
  font-size: var(--fs-caption);
  font-weight: 600;
  color: var(--neutral-2);
}

/* 图表：Y 轴 + 柱 + X 轴 */
.chart {
  margin-top: 16px;
  width: 342px;
}

.plot {
  display: flex;
  gap: 10px;
}

.y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 200px;
  font-size: var(--fs-caption);
  color: var(--white);
  text-align: right;
  width: 20px;
}

.day-card .y-axis {
  color: var(--primary);
}

.bars {
  flex: 1;
  display: flex;
  gap: 8px;
  align-items: flex-end;
  height: 200px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.25);
}

.day-card .bars {
  border-bottom-color: rgba(42, 96, 73, 0.2);
}

.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.bar {
  width: 100%;
  max-width: 24px;
  border-radius: 4px;
  background: var(--white);
}

.bar.hl {
  background: #497462;
}

/* 日视图柱：深绿 + 浅灰绿 */
.bar.dbar {
  background: var(--primary);
}
.bar.dbar.dim {
  background: #d8e0dc;
}

.x-axis {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  width: 100%;
  font-size: var(--fs-caption);
  color: var(--white);
  padding-left: 30px;
}

.day-card .x-axis {
  color: var(--primary);
}

.x-axis span {
  width: 24px;
  text-align: center;
  flex-shrink: 0;
}

/* ===== 日视图卡片 ===== */
.day-card {
  margin: 16px 24px 0;
  background: var(--primary-2);
  padding: 20px;
}

.day-title {
  text-align: center;
  font-size: var(--fs-headline3);
  font-weight: 600;
  color: var(--primary);
}

.day-head {
  margin-top: 10px;
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 8px;
}

.day-big {
  font-size: var(--fs-title2);
  font-weight: 700;
  color: var(--primary);
  line-height: 1.2;
}

.day-mins {
  font-size: var(--fs-caption);
  font-weight: 600;
  color: var(--white);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.day-card .chart {
  width: 100%;
}

/* ===== 统计 ===== */
.stats {
  margin: 16px 24px 0;
  background: var(--white);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.stat-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid var(--neutral-3);
}

.stat-row:last-child {
  border-bottom: none;
}

.s-label {
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}
.s-value {
  font-size: var(--fs-headline3);
  font-weight: 700;
}

/* ===== 编辑目标按钮（白底深绿字） ===== */
.edit-btn {
  margin: 16px 24px 0;
  width: calc(100% - 48px);
  height: 48px;
  border-radius: var(--radius-pill);
  background: var(--white);
  color: var(--primary);
  font-size: var(--fs-headline3);
  font-weight: 600;
  box-shadow: var(--shadow-card);
  transition: transform 0.12s ease;
}

.edit-btn:active {
  transform: scale(0.97);
}

/* ===== 弹出菜单（对照设计稿 @(276,102) 90x136） ===== */
.mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  z-index: 50;
}

.picker {
  position: absolute;
  top: 102px;
  right: 24px;
  width: 90px;
  background: var(--white);
  border-radius: var(--radius-sm);
  box-shadow: 0 8px 24px rgba(20, 12, 7, 0.25);
  padding: 6px;
  z-index: 60;
  display: flex;
  flex-direction: column;
}

.p-item {
  height: 40px;
  border-radius: 8px;
  font-size: var(--fs-headline3);
  color: var(--neutral-2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.p-item.active {
  color: var(--neutral);
  font-weight: 600;
}
</style>
