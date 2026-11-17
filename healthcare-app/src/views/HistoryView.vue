<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { health } from '../utils/healthStore'
import { api, type ExerciseLog } from '../api'
import { formatDateCN, todayISO } from '../utils/date'
import { toast } from '../utils/toast'

const router = useRouter()
const tab = ref('all')
const logs = ref<ExerciseLog[]>([])
const loading = ref(false)

const rangeOptions = [
  { value: 'all', label: '全部' },
  { value: 'week', label: '星期' },
  { value: 'month', label: '月份' },
]

const typeMeta: Record<string, { name: string; emoji: string; bg: string }> = {
  yoga: { name: '瑜伽', emoji: '🧘', bg: '#bacac2' },
  walk: { name: '快走', emoji: '🚶', bg: '#ffe2d3' },
  run: { name: '跑步', emoji: '🏃', bg: '#d2d0e8' },
  ride: { name: '骑车', emoji: '🚴', bg: '#fdedd0' },
}

function dateOffsetISO(days: number) {
  const date = new Date()
  date.setDate(date.getDate() - days)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDuration(minutes: number) {
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return `${String(hours).padStart(2, '0')}:${String(mins).padStart(2, '0')}:00`
}

const list = computed(() => {
  if (tab.value === 'week') {
    const start = dateOffsetISO(6)
    return logs.value.filter((log) => log.date >= start)
  }
  if (tab.value === 'month') {
    const month = todayISO().slice(0, 7)
    return logs.value.filter((log) => log.date.startsWith(month))
  }
  return logs.value
})

const displayList = computed(() =>
  list.value.map((log) => {
    const meta = typeMeta[log.type] ?? { name: '运动', emoji: '🏃', bg: '#bacac2' }
    return {
      ...log,
      ...meta,
      dateText: formatDateCN(log.date),
      durationText: formatDuration(log.minutes),
    }
  }),
)

const totalKcal = computed(() => list.value.reduce((sum, log) => sum + log.calories, 0))

async function loadLogs() {
  loading.value = true
  try {
    logs.value = await api.listExerciseLogs()
  } catch (error) {
    toast(error instanceof Error ? error.message : '加载运动记录失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadLogs)
</script>

<template>
  <div class="history">
    <StatusBar />

    <NavBar title="历史" @back="goBack(router, '/exercise')" />

    <!-- 筛选分段 -->
    <SegmentedControl v-model="tab" :options="rangeOptions" />

    <!-- 运动记录 -->
    <div
      v-for="r in displayList"
      :key="r.id"
      class="record card mt-16"
      :style="{ background: r.bg }"
    >
      <div class="r-top">
        <span class="r-name">{{ r.name }}</span>
        <span class="r-date">{{ r.dateText }}</span>
      </div>
      <div class="r-main">
        <span class="num r-time">{{ r.durationText }}</span>
        <span class="r-emoji">{{ r.emoji }}</span>
      </div>
      <div class="r-bottom">
        <span class="num r-dist">{{ r.calories }} 千卡</span>
        <span class="r-label">本次运动</span>
      </div>
    </div>

    <div v-if="!displayList.length" class="empty card mt-16">
      <span class="empty-emoji">🏃</span>
      <p>{{ loading ? '加载中…' : '还没有运动记录，去运动页记录一次吧' }}</p>
    </div>

    <!-- 步数与汇总 -->
    <div class="step-stat card mt-16">
      <span class="num big">{{ health.steps.toLocaleString() }}</span>
      <span class="unit">步</span>
    </div>

    <div class="summary card mt-16">
      <div class="sum-row">
        <span class="s-label">锻炼</span>
        <span class="num s-value">{{ displayList.length }} 节</span>
      </div>
      <div class="sum-row">
        <span class="s-label">锻炼消耗</span>
        <span class="num s-value">{{ totalKcal }} 大卡</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.history {
  padding: 0 24px 24px;
}

.record {
  padding: 20px;
  position: relative;
}

.r-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.r-name {
  font-size: var(--fs-headline2);
  font-weight: 600;
}

.r-date {
  font-size: var(--fs-body2);
  color: var(--neutral);
  opacity: 0.75;
}

.r-main {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.r-time {
  font-size: var(--fs-headline3);
  font-weight: 700;
}

.r-emoji {
  font-size: 56px;
}

.r-bottom {
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.r-dist {
  font-size: var(--fs-title3);
  font-weight: 700;
  line-height: 1;
}

.r-label {
  font-size: var(--fs-headline3);
  color: var(--neutral-2);
}

.empty {
  padding: 36px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.empty-emoji {
  font-size: 36px;
}

.empty p {
  font-size: var(--fs-body2);
  color: var(--neutral-2);
  text-align: center;
}

.step-stat {
  padding: 20px;
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.big {
  font-size: var(--fs-headline1);
  font-weight: 700;
}

.unit {
  font-size: var(--fs-caption);
  font-weight: 600;
  color: var(--neutral);
}

.summary {
  overflow: hidden;
}

.sum-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid var(--neutral-3);
}

.sum-row:last-child {
  border-bottom: none;
}

.s-label {
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}
.s-value {
  font-size: var(--fs-headline3);
  font-weight: 600;
}
</style>
