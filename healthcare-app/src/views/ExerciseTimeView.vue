<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { toast } from '../utils/toast'
import { health, stepsToKcal, stepsToKm } from '../utils/healthStore'

const router = useRouter()

// 视图：周（深绿图表，默认）/ 日（灰绿图表卡）/ 月（同周数据）
const view = ref<'day' | 'week' | 'month'>('week')
const menuOpen = ref(false)
const alarmOn = ref(false)

// 周视图柱状图（对照设计稿「运动时间 #1」：周一至周日柱高）
const weekBars = [
  { label: '周一', h: 165 },
  { label: '周二', h: 84 },
  { label: '周三', h: 131 },
  { label: '周四', h: 110 },
  { label: '周五', h: 153, hl: true },
  { label: '周六', h: 125, hl: true },
  { label: '周日', h: 46, hl: true },
]

// 日视图柱状图（对照设计稿「运动时间」：20-24/03，深绿柱 + 浅灰绿柱）
const dayBars = [
  { label: '20', h: 194 },
  { label: '21', h: 99 },
  { label: '22', h: 154 },
  { label: '23', h: 129 },
  { label: '3/24', h: 180 },
  { label: '', h: 147, dim: true },
  { label: '', h: 54, dim: true },
  { label: '', h: 99, dim: true },
  { label: '', h: 125, dim: true },
]

const axis = ['70', '60', '50', '40']
const isDark = computed(() => view.value !== 'day')

const views = [
  { key: 'day' as const, label: '日' },
  { key: 'week' as const, label: '周' },
  { key: 'month' as const, label: '月' },
]

function pickView(v: 'day' | 'week' | 'month') {
  view.value = v
  menuOpen.value = false
}

function toggleAlarm() {
  alarmOn.value = !alarmOn.value
  toast(alarmOn.value ? '已设置运动提醒 ⏰' : '已关闭运动提醒')
}
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
          <span class="num range-text">1月4日 - 1月10日</span>
          <span class="arrow">›</span>
        </button>

        <div class="big-num num">{{ health.activeMinutes }}/{{ health.activeGoalMinutes }}</div>
        <div class="big-label">平均每日活跃时间</div>

        <!-- 柱状图 -->
        <div class="chart">
          <div class="plot">
            <div class="y-axis num">
              <span v-for="v in axis" :key="v">{{ v }}</span>
            </div>
            <div class="bars">
              <div v-for="(b, i) in weekBars" :key="i" class="bar-col">
                <span class="bar" :class="{ hl: b.hl }" :style="{ height: b.h + 'px' }"></span>
              </div>
            </div>
          </div>
          <div class="x-axis num">
            <span v-for="(b, i) in weekBars" :key="i">{{ b.label }}</span>
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
        <button class="day-title num" @click="menuOpen = true">1月7日 星期四 ▾</button>

        <div class="day-head">
          <span class="num day-big">{{ health.activeMinutes }}/{{ health.activeGoalMinutes }}</span>
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
