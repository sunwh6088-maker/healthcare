<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { health } from '../utils/healthStore'
import { catalog, loadCatalog } from '../utils/catalog'
import { toast } from '../utils/toast'

const router = useRouter()
const portions = computed(() => catalog.waterPortions)
const selected = ref(0)
const saved = ref(false)

onMounted(() => {
  loadCatalog().catch(() => undefined)
})

const waterPct = computed(() =>
  Math.min(100, Math.round((health.waterMl / health.waterGoalMl) * 100)),
)
const filledBars = computed(() => Math.round((health.waterMl / health.waterGoalMl) * 8))

function save() {
  const portion = portions.value[selected.value]
  if (!portion) return
  const ml = portion.ml
  health.waterMl = Math.min(health.waterGoalMl, health.waterMl + ml)
  toast(`已记录 ${ml} 毫升`)
  saved.value = true
  setTimeout(() => (saved.value = false), 1500)
}
</script>

<template>
  <div class="water">
    <!-- 绿色头部 -->
    <div class="head-green">
      <StatusBar light />
      <div class="head-row">
        <span class="date-pill">📅 日期</span>
        <button class="save-btn" @click="save">{{ saved ? '已保存 ✓' : '保存' }}</button>
      </div>
      <div class="glass-area">
        <div class="pct-wrap">
          <span class="num pct">{{ waterPct }}%</span>
          <span class="pct-label">今日饮水</span>
        </div>
        <div class="glass">💧</div>
        <span class="num amount">{{ portions[selected]?.ml ?? 0 }} 毫升</span>
      </div>
    </div>

    <!-- 白色主体 -->
    <div class="body">
      <h2 class="section-title">选择饮水量</h2>
      <div class="portions">
        <button
          v-for="(p, i) in portions"
          :key="p.id"
          class="portion card"
          :class="{ active: selected === i }"
          @click="selected = i"
        >
          <span class="p-emoji">{{ p.emoji }}</span>
          <span class="p-name">{{ p.name }}</span>
          <span class="num p-size">{{ p.sizeLabel }}</span>
        </button>
      </div>

      <div class="today card mt-16">
        <span class="t-label">今日已记录</span>
        <span class="num t-value">{{ health.waterMl }} 毫升</span>
        <div class="t-strip">
          <span v-for="i in 8" :key="i" class="t-bar" :class="{ on: i <= filledBars }"></span>
        </div>
      </div>

      <button class="btn-primary mt-20" @click="router.push('/home')">返回首页</button>
    </div>
  </div>
</template>

<style scoped>
.water {
  min-height: 100%;
}

.head-green {
  background: var(--primary);
  padding-bottom: 24px;
  border-radius: 0 0 32px 32px;
}

.head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 24px 0;
}

.date-pill {
  background: var(--white);
  color: var(--primary);
  font-size: var(--fs-caption);
  font-weight: 600;
  padding: 6px 14px;
  border-radius: var(--radius-pill);
}

.save-btn {
  background: var(--white);
  color: var(--accent);
  font-size: var(--fs-body2);
  font-weight: 600;
  padding: 8px 18px;
  border-radius: var(--radius-pill);
}

.glass-area {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.pct {
  font-size: 40px;
  font-weight: 700;
  color: var(--neutral);
  background: var(--white);
  border-radius: 50%;
  width: 110px;
  height: 110px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pct-label {
  margin-top: 8px;
  font-size: var(--fs-body2);
  color: var(--white);
}

.glass {
  font-size: 56px;
}

.amount {
  font-size: var(--fs-headline3);
  font-weight: 600;
  color: var(--white);
}

.body {
  padding: 24px;
}

.portions {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}

.portion {
  flex: 1;
  padding: 16px 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  border: 2px solid transparent;
  transition: all 0.15s ease;
}

.portion.active {
  border-color: var(--primary);
  background: var(--accent-2);
}

.p-emoji {
  font-size: 32px;
}
.p-name {
  font-size: var(--fs-body2);
  font-weight: 600;
}
.p-size {
  font-size: var(--fs-caption);
  color: var(--neutral-2);
}

.today {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.t-label {
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}
.t-value {
  font-size: var(--fs-title3);
  font-weight: 700;
}

.t-strip {
  display: flex;
  gap: 6px;
  align-items: flex-end;
  height: 40px;
}

.t-bar {
  flex: 1;
  height: 16px;
  border-radius: 4px;
  background: var(--neutral-3);
}

.t-bar.on {
  background: var(--accent);
}
</style>
