<script setup lang="ts">
// 模块：目标跟踪 —— 活跃目标进度和可编辑的运动历史。
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { health, syncFromServer } from '../utils/healthStore'
import ExerciseLogSheet from '../components/ExerciseLogSheet.vue'
import { api, type ExerciseLog } from '../api'
import { catalog, loadCatalog } from '../utils/catalog'
import { formatDateCN } from '../utils/date'
import { toast } from '../utils/toast'

const router = useRouter()
const tab = ref<'track' | 'history'>('track')
const logs = ref<ExerciseLog[]>([])
const loading = ref(false)
const sheetOpen = ref(false)
const editingLog = ref<ExerciseLog | null>(null)

const progress = computed(() => {
  if (!health.activeGoalMinutes) return 0
  return Math.min(100, Math.round((health.activeMinutes / health.activeGoalMinutes) * 100))
})

function typeName(key: string) {
  return catalog.exerciseTypes.find((sport) => sport.key === key)?.name ?? '运动'
}

function typeEmoji(key: string) {
  return catalog.exerciseTypes.find((sport) => sport.key === key)?.emoji ?? '🏃'
}

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

function openEdit(log: ExerciseLog) {
  editingLog.value = log
  sheetOpen.value = true
}

async function refreshAfterChange() {
  await Promise.all([loadLogs(), syncFromServer().catch(() => undefined)])
}

onMounted(async () => {
  await Promise.all([loadCatalog().catch(() => undefined), loadLogs()])
})
</script>

<template>
  <div class="gt">
    <StatusBar />

    <NavBar title="目标跟踪" @back="goBack(router, '/exercise')" />

    <!-- 追踪 / 历史记录 切换（对照设计稿 Group 2 @(24,156) 342x50） -->
    <div class="tabs">
      <button class="tab-pill" :class="{ active: tab === 'track' }" @click="tab = 'track'">
        追踪
      </button>
      <button class="tab-text" :class="{ active: tab === 'history' }" @click="tab = 'history'">
        历史/记录
      </button>
    </div>

    <!-- 追踪：活动目标进度卡（对照设计稿 Group @(24,220) 342x120） -->
    <template v-if="tab === 'track'">
      <div class="yoga-card">
        <div class="yoga-info">
          <h3 class="yoga-name">活跃时间</h3>
          <span class="yoga-kcal"
            >{{ health.activeMinutes }}/{{ health.activeGoalMinutes }} 分钟</span
          >

          <div class="progress-track yoga-track">
            <div class="progress-fill" :style="{ width: progress + '%' }"></div>
          </div>
          <span class="num yoga-pct">{{ progress }}%</span>
        </div>
        <span class="yoga-emoji">⏱️</span>
      </div>

      <button class="green-link mt-12" @click="router.push('/set-goal?type=active')">
        编辑活跃目标
      </button>
    </template>

    <!-- 历史/记录：真实运动记录，可点按编辑 -->
    <template v-else>
      <div v-if="logs.length" class="log-list">
        <button v-for="log in logs" :key="log.id" class="log-row card" @click="openEdit(log)">
          <span class="log-emoji">{{ typeEmoji(log.type) }}</span>
          <div class="log-info">
            <h4>{{ typeName(log.type) }}</h4>
            <p>{{ formatDateCN(log.date) }} · {{ log.minutes }} 分钟</p>
          </div>
          <span class="log-kcal">{{ log.calories }} 千卡</span>
        </button>
      </div>

      <div v-else class="empty card">
        <span class="empty-emoji">📋</span>
        <p>{{ loading ? '加载中…' : '暂无历史记录' }}</p>
      </div>
    </template>

    <ExerciseLogSheet
      :open="sheetOpen"
      :log="editingLog"
      @close="sheetOpen = false"
      @saved="refreshAfterChange"
      @deleted="refreshAfterChange"
    />
  </div>
</template>

<style scoped>
.gt {
  padding: 0 24px 24px;
}

.tabs {
  margin-top: 8px;
  position: relative;
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 0 8px;
}

.tab-pill {
  padding: 8px 20px;
  border-radius: var(--radius-pill);
  font-size: var(--fs-headline3);
  color: var(--neutral-2);
  transition: all 0.15s ease;
}

.tab-pill.active {
  background: var(--primary);
  color: var(--white);
  font-weight: 600;
}

.tab-text {
  font-size: var(--fs-headline3);
  color: var(--neutral-2);
}

.tab-text.active {
  color: var(--neutral);
  font-weight: 600;
}

/* Yoga 进度卡：灰绿底（#bacac2） */
.yoga-card {
  margin-top: 16px;
  height: 120px;
  border-radius: var(--radius-lg);
  background: var(--primary-2);
  padding: 24px 32px;
  position: relative;
  display: flex;
  align-items: center;
}

.yoga-name {
  font-size: var(--fs-headline3);
  font-weight: 700;
  color: var(--neutral);
}

.yoga-kcal {
  display: block;
  margin-top: 2px;
  font-size: var(--fs-body2);
  color: var(--white);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.yoga-track {
  position: absolute;
  left: 32px;
  bottom: 24px;
  width: 123px;
}

.yoga-pct {
  position: absolute;
  left: 160px;
  bottom: 18px;
  font-size: var(--fs-caption);
  font-weight: 600;
  color: var(--primary);
}

.yoga-emoji {
  position: absolute;
  right: 24px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 56px;
}

/* 运动历史记录 */
.log-list {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.log-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  text-align: left;
}

.log-emoji {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--primary-2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.log-info h4 {
  font-size: var(--fs-headline3);
  font-weight: 600;
}

.log-info p {
  margin-top: 2px;
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.log-kcal {
  margin-left: auto;
  font-size: var(--fs-body2);
  color: var(--primary);
  font-weight: 600;
}

/* 空态 */
.empty {
  margin-top: 16px;
  padding: 48px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.empty-emoji {
  font-size: 40px;
}

.empty p {
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}
</style>
