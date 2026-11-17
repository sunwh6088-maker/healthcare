<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { health } from '../utils/healthStore'
import { api, type FoodLog } from '../api'
import { todayISO } from '../utils/date'
import { mealEmoji, mealLabel } from '../utils/meals'
import { toast } from '../utils/toast'

const router = useRouter()
const logs = ref<FoodLog[]>([])
const totalCalories = ref(0)
const loading = ref(false)

async function loadLogs() {
  loading.value = true
  try {
    const summary = await api.listFoodLogs(todayISO())
    logs.value = summary.logs
    totalCalories.value = summary.totalCalories
    health.foodKcal = summary.totalCalories
  } catch (error) {
    toast(error instanceof Error ? error.message : '加载食物记录失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadLogs)

async function removeLog(id: number) {
  try {
    await api.deleteFoodLog(id)
    await loadLogs()
    toast('记录已删除')
  } catch (error) {
    toast(error instanceof Error ? error.message : '删除失败')
  }
}

const macros = [
  { name: '碳水', amount: '100g', pct: 32, color: '#2a6049' },
  { name: '脂肪', amount: '100g', pct: 28, color: '#ec744a' },
  { name: '蛋白质', amount: '90g', pct: 40, color: '#655eb0' },
]

// 卡路里输入输出柱状图
const calBars = [
  { h: 56, color: '#2a6049' },
  { h: 32, color: '#ececec' },
  { h: 32, color: '#ececec' },
  { h: 32, color: '#ececec' },
  { h: 45, color: '#ec744a' },
  { h: 38, color: '#ec744a' },
  { h: 29, color: '#ec744a' },
  { h: 53, color: '#ececec' },
]
</script>

<template>
  <div class="food">
    <StatusBar />

    <NavBar title="食物" @back="goBack(router)" />

    <!-- 营养素环形图 -->
    <div class="donut-wrap card mt-8">
      <div
        class="donut"
        :style="{
          background: 'conic-gradient(#2a6049 0 32%, #ec744a 32% 60%, #655eb0 60% 100%)',
        }"
      >
        <div class="donut-hole">
          <span class="num total">{{ totalCalories.toLocaleString() }}</span>
          <span class="unit">千卡</span>
        </div>
      </div>
      <div class="legend">
        <div v-for="m in macros" :key="m.name" class="legend-row">
          <span class="l-dot" :style="{ background: m.color }"></span>
          <span class="l-name">{{ m.name }}</span>
          <span class="num l-amount">{{ m.amount }}</span>
          <span class="num l-pct">{{ m.pct }}%</span>
        </div>
      </div>
    </div>

    <!-- 今日记录 -->
    <div class="between mt-20">
      <span class="section-title">今日记录</span>
      <span class="section-sub">{{ logs.length }} 条</span>
    </div>
    <div v-if="logs.length" class="meals card mt-12">
      <div v-for="log in logs" :key="log.id" class="meal-row">
        <span class="meal-emoji">{{ log.emoji || mealEmoji(log.meal) }}</span>
        <div class="meal-info">
          <span class="meal-name">{{ log.name }}</span>
          <span class="meal-sub">
            {{ mealLabel(log.meal) }}<template v-if="log.portion"> · {{ log.portion }}</template>
          </span>
        </div>
        <span class="meal-cal">{{ log.calories }} 卡路里</span>
        <button class="meal-del" aria-label="删除记录" @click="removeLog(log.id)">×</button>
      </div>
    </div>
    <div v-else class="empty card mt-12">
      <span class="empty-emoji">🍽️</span>
      <p>{{ loading ? '加载中…' : '今天还没有食物记录' }}</p>
    </div>

    <!-- 卡路里输入输出 -->
    <div class="cal-card card mt-16">
      <h3 class="cal-title">卡路里输入与输出</h3>
      <div class="cal-chart">
        <div class="cal-axis num"><span>4千</span><span>2千</span><span>0</span></div>
        <div class="cal-bars">
          <span
            v-for="(b, i) in calBars"
            :key="i"
            class="cal-bar"
            :style="{ height: b.h + 'px', background: b.color }"
          ></span>
        </div>
      </div>
      <div class="cal-legend">
        <span class="cl"><i style="background: #2a6049"></i>达标</span>
        <span class="cl"><i style="background: #ec744a"></i>不足</span>
        <span class="cl"><i style="background: #655eb0"></i>超出</span>
        <span class="cl"><i style="background: #ec744a"></i>消耗卡路里</span>
      </div>
    </div>

    <div class="btns mt-16">
      <button class="btn-primary" @click="router.push('/search-food')">🔍 搜索食物</button>
      <button class="btn-add mt-12" @click="router.push('/add-food')">＋ 添加食物</button>
    </div>
  </div>
</template>

<style scoped>
.food {
  padding: 0 24px 24px;
}

.donut-wrap {
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.donut {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.donut-hole {
  width: 92px;
  height: 92px;
  border-radius: 50%;
  background: var(--white);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.total {
  font-size: var(--fs-title3);
  font-weight: 700;
}
.unit {
  font-size: var(--fs-caption);
  color: var(--neutral-2);
}

.legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.legend-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.l-dot {
  width: 16px;
  height: 16px;
  border-radius: 4px;
}
.l-name {
  font-size: var(--fs-headline3);
  color: var(--neutral);
  width: 56px;
}
.l-amount {
  font-size: var(--fs-headline3);
  font-weight: 600;
}
.l-pct {
  margin-left: auto;
  font-size: var(--fs-headline3);
  font-weight: 700;
}

.meals {
  overflow: hidden;
}

.meal-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--neutral-3);
}

.meal-row:last-child {
  border-bottom: none;
}

.meal-emoji {
  font-size: 24px;
}

.meal-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.meal-name {
  font-size: var(--fs-headline3);
  font-weight: 600;
}

.meal-sub {
  font-size: var(--fs-caption);
  color: var(--neutral-2);
}

.meal-cal {
  margin-left: auto;
  font-size: var(--fs-body2);
  color: var(--neutral-2);
  white-space: nowrap;
}

.meal-del {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--accent-2);
  color: var(--accent);
  font-size: 18px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.empty {
  padding: 32px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.empty-emoji {
  font-size: 32px;
}

.empty p {
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.cal-card {
  padding: 20px;
}

.cal-title {
  font-size: var(--fs-headline3);
  font-weight: 600;
}

.cal-chart {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  height: 160px;
}

.cal-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  font-size: var(--fs-caption);
  color: var(--neutral);
  padding-bottom: 4px;
}

.cal-bars {
  flex: 1;
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.cal-bar {
  flex: 1;
  border-radius: 4px;
}

.cal-legend {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.cl {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--fs-caption);
  font-weight: 600;
}

.cl i {
  width: 12px;
  height: 12px;
  border-radius: 4px;
  display: inline-block;
}

.btn-add {
  width: 100%;
  height: 48px;
  border-radius: var(--radius-pill);
  border: 1.5px solid var(--primary);
  color: var(--primary);
  font-size: var(--fs-headline3);
  font-weight: 600;
  background: transparent;
}
</style>
