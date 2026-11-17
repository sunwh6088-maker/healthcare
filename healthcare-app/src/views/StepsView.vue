<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { health, stepsToKcal, stepsToKm } from '../utils/healthStore'

const router = useRouter()
const tab = ref('day')

const rangeOptions = [
  { value: 'day', label: '日' },
  { value: 'week', label: '周' },
  { value: 'month', label: '月' },
]

const dayBars = [
  { h: 68, on: true },
  { h: 44, on: true },
  { h: 60, on: true },
  { h: 52, on: true },
  { h: 30, on: false },
  { h: 46, on: false },
  { h: 22, on: false },
  { h: 34, on: false },
  { h: 28, on: false },
]
const weekBars = [
  { h: 70, on: true },
  { h: 44, on: true },
  { h: 62, on: true },
  { h: 58, on: true },
  { h: 80, on: true },
  { h: 42, on: false },
  { h: 30, on: false },
]
const monthBars = [
  { h: 60, on: true },
  { h: 72, on: true },
  { h: 55, on: false },
  { h: 85, on: true },
]

const chart = computed(() => {
  if (tab.value === 'week') {
    const total = health.steps * 7
    return {
      range: '1月4日 - 1月10日',
      big: `${total}/42000`,
      label: '每周步数',
      bars: weekBars,
      axis: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      steps: total,
    }
  }
  if (tab.value === 'month') {
    const total = health.steps * 28
    return {
      range: '2021 年 1 月',
      big: `${total}/180000`,
      label: '每月步数',
      bars: monthBars,
      axis: ['第1周', '第2周', '第3周', '第4周'],
      steps: total,
    }
  }
  return {
    range: '1月4日 - 1月10日',
    big: `${health.steps}/${health.stepsGoal}`,
    label: '每日步数',
    bars: dayBars,
    axis: ['20', '21', '22', '23', '3/24'],
    steps: health.steps,
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

    <!-- 统计 -->
    <div class="stats card mt-16">
      <div class="stat-row">
        <span class="s-label">距离</span>
        <span class="num s-value">{{ stepsToKm(chart.steps) }} 公里</span>
      </div>
      <div class="stat-row">
        <span class="s-label">燃烧的卡路里</span>
        <span class="num s-value">{{ stepsToKcal(chart.steps) }} 千卡</span>
      </div>
      <div class="stat-row">
        <span class="s-label">每周总步数</span>
        <span class="num s-value">{{ health.steps * 7 }}/42000</span>
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
