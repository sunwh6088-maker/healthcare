<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { frequentFoods, recentFoods, customFoods } from '../mock/foods'

const router = useRouter()
const tab = ref('frequent')

const tabs = [
  { value: 'frequent', label: '频繁' },
  { value: 'recent', label: '最近' },
  { value: 'custom', label: '自定义' },
]

const list = computed(() => {
  if (tab.value === 'recent') return recentFoods
  if (tab.value === 'custom') return customFoods
  return frequentFoods
})
</script>

<template>
  <div class="discover">
    <StatusBar />

    <div class="head">
      <h1 class="page-title">发现</h1>
      <button class="add-btn" aria-label="搜索食物" @click="router.push('/search-food')">🔍</button>
    </div>

    <SegmentedControl v-model="tab" :options="tabs" class="mt-12" />

    <div class="food-grid mt-16">
      <button
        v-for="f in list"
        :key="f.name + tab"
        class="food-card"
        @click="router.push('/add-food')"
      >
        <span class="f-emoji">{{ f.emoji }}</span>
        <span class="f-name">{{ f.name }}</span>
        <span class="f-cal">{{ f.cal }}</span>
        <span class="f-add">＋</span>
      </button>
    </div>

    <div class="tabbar-spacer"></div>
  </div>
</template>

<style scoped>
.discover {
  padding: 0 24px 24px;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: var(--fs-title3);
  font-weight: 700;
}

.add-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--primary);
  color: var(--white);
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.food-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.food-card {
  position: relative;
  background: var(--white);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  padding: 14px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  text-align: left;
}

.f-emoji {
  font-size: 32px;
}
.f-name {
  font-size: var(--fs-headline3);
  font-weight: 600;
  color: var(--neutral);
}
.f-cal {
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.f-add {
  position: absolute;
  right: 10px;
  bottom: 10px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--primary);
  color: var(--white);
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
