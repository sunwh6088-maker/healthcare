<script setup lang="ts">
// 模块：步数页 —— 日/周/月视图，展示快捷指令导入的每日步数。
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { health, stepsToKcal, stepsToKm, syncFromServer } from '../utils/healthStore'
import { api, type HealthMetric } from '../api'
import { todayISO } from '../utils/date'
import { toast } from '../utils/toast'

const router = useRouter()
const tab = ref('day')
const metrics = ref<HealthMetric[]>([])

const rangeOptions = [
  { value: 'day', label: '日' },
  { value: 'week', label: '周' },
  { value: 'month', label: '月' },
]

const byDate = computed(() => new Map(metrics.value.map((metric) => [metric.date, metric])))

function toISO(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function shortDate(date: Date) {
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

function daysAgo(days: number) {
  const date = new Date()
  date.setDate(date.getDate() - days)
  date.setHours(0, 0, 0, 0)
  return date
}

/** 某天的步数；今天还没有导入时用当前健康数据兜底。 */
function stepsOn(iso: string) {
  const imported = byDate.value.get(iso)?.steps ?? 0
  return iso === todayISO() ? Math.max(imported, health.steps) : imported
}

function startOfWeek(date: Date) {
  const copy = new Date(date)
  copy.setHours(0, 0, 0, 0)
  copy.setDate(copy.getDate() - ((copy.getDay() + 6) % 7))
  return copy
}

function weekSum(start: Date) {
  let sum = 0
  const today = new Date()
  today.setHours(23, 59, 59, 999)
  for (let offset = 0; offset < 7; offset += 1) {
    const date = new Date(start)
    date.setDate(date.getDate() + offset)
    if (date > today) break
    sum += stepsOn(toISO(date))
  }
  return sum
}

function monthSum(year: number, month: number) {
  const today = new Date()
  const days = new Date(year, month + 1, 0).getDate()
  let sum = 0
  for (let day = 1; day <= days; day += 1) {
    const date = new Date(year, month, day)
    if (date > today) break
    sum += stepsOn(toISO(date))
  }
  return sum
}

function toBars(values: number[]) {
  const max = Math.max(...values, 1)
  return values.map((value) => ({
    h: value > 0 ? Math.max(8, Math.round((value / max) * 120)) : 6,
    on: value > 0,
  }))
}

const daySeries = computed(() => {
  const today = new Date()
  return Array.from({ length: 9 }, (_, index) => {
    const date = new Date(today)
    date.setDate(today.getDate() - (8 - index))
    date.setHours(0, 0, 0, 0)
    return {
      label: `${date.getMonth() + 1}/${date.getDate()}`,
      value: stepsOn(toISO(date)),
      date,
    }
  })
})

const weekSeries = computed(() => {
  const thisMonday = startOfWeek(new Date())
  return Array.from({ length: 7 }, (_, index) => {
    const start = new Date(thisMonday)
    start.setDate(start.getDate() - (6 - index) * 7)
    return {
      label: `${start.getMonth() + 1}/${start.getDate()}`,
      value: weekSum(start),
      date: start,
    }
  })
})

const monthSeries = computed(() => {
  const today = new Date()
  return Array.from({ length: 4 }, (_, index) => {
    const date = new Date(today.getFullYear(), today.getMonth() - (3 - index), 1)
    return {
      label: `${date.getMonth() + 1}月`,
      value: monthSum(date.getFullYear(), date.getMonth()),
      date,
    }
  })
})

const chart = computed(() => {
  if (tab.value === 'week') {
    const series = weekSeries.value
    const total = series.reduce((sum, item) => sum + item.value, 0)
    const goal = health.stepsGoal * 7
    return {
      range: `${shortDate(series[0].date)} - ${shortDate(new Date())}`,
      big: `${total.toLocaleString()}/${goal.toLocaleString()}`,
      label: '每周步数',
      bars: toBars(series.map((item) => item.value)),
      axis: series.map((item) => item.label),
      totalSteps: total,
      totalGoal: goal,
    }
  }
  if (tab.value === 'month') {
    const total = monthSeries.value.reduce((sum, item) => sum + item.value, 0)
    const today = new Date()
    const first = monthSeries.value[0].date
    const daysInMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0).getDate()
    const goal = health.stepsGoal * daysInMonth
    const sameYear = first.getFullYear() === today.getFullYear()
    const range = sameYear
      ? `${first.getFullYear()} 年 ${first.getMonth() + 1} 月 - ${today.getMonth() + 1} 月`
      : `${first.getFullYear()} 年 ${first.getMonth() + 1} 月 - ${today.getFullYear()} 年 ${today.getMonth() + 1} 月`
    return {
      range,
      big: `${total.toLocaleString()}/${goal.toLocaleString()}`,
      label: '每月步数',
      bars: toBars(monthSeries.value.map((item) => item.value)),
      axis: monthSeries.value.map((item) => item.label),
      totalSteps: total,
      totalGoal: goal,
    }
  }
  const series = daySeries.value
  const total = series.reduce((sum, item) => sum + item.value, 0)
  const goal = health.stepsGoal * series.length
  return {
    range: `${shortDate(series[0].date)} - ${shortDate(series[series.length - 1].date)}`,
    big: `${stepsOn(todayISO()).toLocaleString()}/${health.stepsGoal.toLocaleString()}`,
    label: '每日步数',
    bars: toBars(series.map((item) => item.value)),
    axis: series.map((item) => item.label),
    totalSteps: total,
    totalGoal: goal,
  }
})

const hasData = computed(() => metrics.value.some((metric) => metric.steps > 0))

onMounted(async () => {
  // 拉取服务端最新状态，避免导入或设备上报后本地旧值覆盖后端
  await syncFromServer().catch(() => undefined)
  try {
    const from = toISO(daysAgo(149))
    metrics.value = await api.listHealthMetrics(from, todayISO())
  } catch (error) {
    toast(error instanceof Error ? error.message : '加载步数历史失败')
  }
})
</script>

<template>
  <div class="steps">
    <StatusBar />

    <NavBar title="步骤" @back="goBack(router)">
      <template #right>
        <button class="green-link" @click="router.push('/set-goal?type=steps')">编辑目标</button>
      </template>
    </NavBar>

    <!-- 日/周/月 分段 -->
    <SegmentedControl v-model="tab" :options="rangeOptions" />

    <!-- 图表卡片（灰绿底） -->
    <div class="chart card mt-16">
      <p class="range num">{{ chart.range }}</p>
      <div class="chart-head">
        <span class="num big">{{ chart.big }}</span>
        <span class="daily">{{ chart.label }}</span>
      </div>
      <div class="bars">
        <span
          v-for="(b, i) in chart.bars"
          :key="i"
          class="bar"
          :class="{ on: b.on }"
          :style="{ height: b.h + 'px' }"
        ></span>
      </div>
      <div class="axis num">
        <span v-for="a in chart.axis" :key="a">{{ a }}</span>
      </div>
    </div>

    <p v-if="!hasData" class="empty-tip">
      还没有导入步数。在「设置 → iPhone 健康数据导入」生成令牌后，用快捷指令每天上报。
    </p>

    <!-- 统计 -->
    <div class="stats card mt-16">
      <div class="stat-row">
        <span class="s-label">距离</span>
        <span class="num s-value">{{ stepsToKm(chart.totalSteps) }} 公里</span>
      </div>
      <div class="stat-row">
        <span class="s-label">燃烧的卡路里</span>
        <span class="num s-value">{{ stepsToKcal(chart.totalSteps) }} 千卡</span>
      </div>
      <div class="stat-row">
        <span class="s-label">区间总步数</span>
        <span class="num s-value">
          {{ chart.totalSteps.toLocaleString() }}/{{ chart.totalGoal.toLocaleString() }}
        </span>
      </div>
    </div>

    <button class="btn-primary mt-20" @click="router.push('/exercise/calendar')">
      查看运动日历
    </button>
  </div>
</template>

<style scoped>
.steps {
  padding: 0 24px 24px;
}

.chart {
  background: var(--primary-2);
  padding: 20px;
}

.range {
  text-align: center;
  font-size: var(--fs-headline3);
  font-weight: 600;
  color: var(--primary);
}

.chart-head {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.big {
  font-size: var(--fs-title2);
  font-weight: 700;
  color: var(--primary);
  line-height: 1.2;
}

.daily {
  font-size: var(--fs-caption);
  font-weight: 600;
  color: var(--white);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.bars {
  margin-top: 16px;
  display: flex;
  gap: 10px;
  align-items: flex-end;
  height: 120px;
}

.bar {
  flex: 1;
  border-radius: 4px;
  background: var(--white);
}

.bar.on {
  background: var(--primary);
}

.axis {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  font-size: var(--fs-caption);
  color: var(--primary);
}

.empty-tip {
  margin-top: 12px;
  font-size: var(--fs-caption);
  line-height: 1.6;
  color: var(--neutral-2);
  text-align: center;
}

.stats {
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
</style>
