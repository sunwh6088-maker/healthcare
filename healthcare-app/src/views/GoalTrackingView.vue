<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { health } from '../utils/healthStore'

const router = useRouter()
const tab = ref<'track' | 'history'>('track')

const progress = computed(() => {
  if (!health.activeGoalMinutes) return 0
  return Math.min(100, Math.round((health.activeMinutes / health.activeGoalMinutes) * 100))
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
    </template>

    <!-- 历史/记录：空态 -->
    <template v-else>
      <div class="empty card">
        <span class="empty-emoji">📋</span>
        <p>暂无历史记录</p>
      </div>
    </template>
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
