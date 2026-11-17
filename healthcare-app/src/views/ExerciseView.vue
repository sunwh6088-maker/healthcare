<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import ExerciseLogSheet from '../components/ExerciseLogSheet.vue'
import { catalog, catalogStatus, loadCatalog } from '../utils/catalog'
import { health, syncFromServer } from '../utils/healthStore'
import { toast } from '../utils/toast'
import { api, type ExerciseLog } from '../api'
import { todayISO } from '../utils/date'

const router = useRouter()
const tab = ref('yoga')
const sheetOpen = ref(false)
const editingLog = ref<ExerciseLog | null>(null)
const todayLogs = ref<ExerciseLog[]>([])
const loadingLogs = ref(false)

const sportOptions = computed(() =>
  catalog.exerciseTypes.map((sport) => ({ value: sport.key, label: sport.name })),
)

const active = computed(
  () => catalog.exerciseTypes.find((sport) => sport.key === tab.value) ?? catalog.exerciseTypes[0],
)

const lessons = computed(() => catalog.exerciseLessons)

const activeTotal = computed(() => {
  const h = Math.floor(health.activeMinutes / 60)
  const m = health.activeMinutes % 60
  return `${String(h).padStart(2, '0')}.${String(m).padStart(2, '0')}.00`
})

function typeName(key: string) {
  return catalog.exerciseTypes.find((sport) => sport.key === key)?.name ?? '运动'
}

function typeEmoji(key: string) {
  return catalog.exerciseTypes.find((sport) => sport.key === key)?.emoji ?? '🏃'
}

async function loadTodayLogs() {
  loadingLogs.value = true
  try {
    const logs = await api.listExerciseLogs()
    todayLogs.value = logs.filter((log) => log.date === todayISO())
  } catch (error) {
    toast(error instanceof Error ? error.message : '加载运动记录失败')
  } finally {
    loadingLogs.value = false
  }
}

function openCreate() {
  editingLog.value = null
  sheetOpen.value = true
}

function openEdit(log: ExerciseLog) {
  editingLog.value = log
  sheetOpen.value = true
}

async function refreshAfterChange() {
  await Promise.all([loadTodayLogs(), syncFromServer().catch(() => undefined)])
}

onMounted(async () => {
  try {
    await loadCatalog()
  } catch (error) {
    toast(error instanceof Error ? error.message : catalogStatus.error)
  }
  await loadTodayLogs()
})
</script>

<template>
  <div class="exercise">
    <StatusBar />

    <NavBar title="运动" @back="goBack(router)">
      <template #right>
        <button class="orange-link" @click="router.push('/history')">历史</button>
      </template>
    </NavBar>

    <!-- 运动类型分段 -->
    <SegmentedControl v-model="tab" :options="sportOptions" />

    <!-- 总览卡片 -->
    <div v-if="active" class="overview card mt-16">
      <div class="ov-left">
        <span class="num ov-value">{{ active.key === 'yoga' ? activeTotal : '0.00' }}</span>
        <span class="ov-unit">{{ active.key === 'yoga' ? '分钟' : '公里' }}</span>
        <span class="ov-label">{{ active.key === 'yoga' ? '瑜伽总时长' : '总长跑' }}</span>
      </div>
      <span class="ov-emoji">{{ active.emoji }}</span>
    </div>

    <!-- 运动课程卡片 -->
    <div v-if="active" class="course card mt-16" :style="{ background: active.bg }">
      <div class="course-info">
        <h3 class="course-name">{{ active.name }} #1</h3>
        <span class="course-kcal">{{ active.kcal }} 千卡</span>
        <div class="progress-track course-track">
          <div class="progress-fill" :style="{ width: '80%', background: active.bar }"></div>
        </div>
        <span class="course-pct num" :style="{ color: active.bar }">80%</span>
      </div>
      <span class="course-emoji">{{ active.emoji }}</span>
    </div>

    <!-- 课程列表 -->
    <div class="between mt-20">
      <span class="section-title">课程</span>
      <button class="green-link" @click="$toast('课程列表即将上线')">所有课程</button>
    </div>

    <div class="lessons mt-12">
      <div
        v-for="lesson in lessons"
        :key="lesson.id"
        class="lesson card"
        @click="router.push('/exercise/calendar')"
      >
        <span class="l-emoji">{{ lesson.emoji }}</span>
        <div class="l-info">
          <h4>{{ lesson.title }}</h4>
          <p>{{ lesson.level }} · {{ lesson.minutes }} 分钟 · {{ lesson.calories }} 千卡</p>
        </div>
        <span class="l-go">›</span>
      </div>
    </div>

    <div class="between mt-20">
      <span class="section-title">今天的运动</span>
      <button class="green-link" @click="router.push('/history')">全部记录</button>
    </div>

    <p v-if="!loadingLogs && !todayLogs.length" class="today-empty">
      今天还没有运动记录，点下方按钮记录一次吧
    </p>
    <div v-else class="lessons today-lessons mt-12">
      <button
        v-for="log in todayLogs"
        :key="log.id"
        class="lesson today-log card"
        @click="openEdit(log)"
      >
        <span class="l-emoji">{{ typeEmoji(log.type) }}</span>
        <div class="l-info">
          <h4>{{ typeName(log.type) }}</h4>
          <p>{{ log.minutes }} 分钟 · {{ log.calories }} 千卡</p>
        </div>
        <span class="l-go">编辑 ›</span>
      </button>
    </div>

    <div class="goal-entry card mt-16" @click="router.push('/goal-tracking')">
      <span class="g-emoji">🎯</span>
      <div class="g-info">
        <h4>目标跟踪</h4>
        <p>追踪运动进度与历史记录</p>
      </div>
      <span class="g-go">›</span>
    </div>
    <button class="green-link mt-8" @click="router.push('/set-goal?type=active')">
      编辑活跃目标
    </button>

    <button class="log-btn mt-16" @click="openCreate">＋ 记录运动</button>

    <button class="btn-primary mt-16" @click="router.push('/exercise/calendar')">
      查看运动日历
    </button>

    <ExerciseLogSheet
      :open="sheetOpen"
      :log="editingLog"
      :default-type="active?.key"
      @close="sheetOpen = false"
      @saved="refreshAfterChange"
      @deleted="refreshAfterChange"
    />
  </div>
</template>

<style scoped>
.exercise {
  padding: 0 24px 24px;
}

.overview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
}

.ov-left {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.ov-value {
  font-size: var(--fs-title3);
  font-weight: 700;
}

.ov-unit {
  font-size: var(--fs-headline3);
  font-weight: 600;
  margin-top: 2px;
}

.ov-label {
  margin-top: 6px;
  font-size: var(--fs-body2);
  color: var(--primary);
  font-weight: 600;
}

.ov-emoji {
  font-size: 56px;
}

.course {
  position: relative;
  padding: 20px;
  min-height: 120px;
  display: flex;
  align-items: center;
}

.course-name {
  font-size: var(--fs-headline3);
  font-weight: 700;
}

.course-kcal {
  display: block;
  margin-top: 4px;
  font-size: var(--fs-body2);
  color: var(--white);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.course-track {
  position: absolute;
  left: 20px;
  bottom: 24px;
  width: 123px;
}

.course-pct {
  position: absolute;
  left: 148px;
  bottom: 18px;
  font-size: var(--fs-caption);
  font-weight: 700;
}

.course-emoji {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 64px;
}

.log-btn {
  width: 100%;
  height: 48px;
  border-radius: var(--radius-pill);
  background: var(--white);
  color: var(--primary);
  font-size: var(--fs-headline3);
  font-weight: 600;
  box-shadow: var(--shadow-card);
}

.lessons {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.lesson {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  width: 100%;
  text-align: left;
}

.today-empty {
  margin-top: 12px;
  padding: 18px;
  text-align: center;
  color: var(--neutral-2);
  font-size: var(--fs-body2);
  background: var(--white);
  border-radius: var(--radius-md);
}

.l-emoji {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--primary-2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.l-info h4 {
  font-size: var(--fs-headline3);
  font-weight: 700;
}

.l-info p {
  margin-top: 2px;
  font-size: var(--fs-caption);
  color: var(--neutral-2);
}

.l-go {
  margin-left: auto;
  font-size: 22px;
  color: var(--neutral-2);
}

.goal-entry {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
}

.g-emoji {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--accent-2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.g-info h4 {
  font-size: var(--fs-headline3);
  font-weight: 700;
}

.g-info p {
  margin-top: 2px;
  font-size: var(--fs-caption);
  color: var(--neutral-2);
}

.g-go {
  margin-left: auto;
  font-size: 22px;
  color: var(--neutral-2);
}
</style>
