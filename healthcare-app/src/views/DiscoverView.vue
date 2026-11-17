<script setup lang="ts">
// 模块：发现页 —— 频繁/最近/自定义食物分类，数据来自数据库目录。
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { catalog, catalogStatus, loadCatalog, pickFoods } from '../utils/catalog'
import { toast } from '../utils/toast'

const router = useRouter()
const tab = ref('frequent')

const tabs = [
  { value: 'frequent', label: '频繁' },
  { value: 'recent', label: '最近' },
  { value: 'custom', label: '自定义' },
]

const list = computed(() => {
  if (tab.value === 'recent') {
    const recent = pickFoods(catalog.recentFoodNames, 24)
    return catalog.recentFoodNames.length ? recent : [...catalog.foods].slice(0, 24).reverse()
  }
  if (tab.value === 'custom') {
    const custom = catalog.foods.filter((food) => food.custom)
    return custom.length ? custom : catalog.foods.slice(16, 30)
  }
  return pickFoods(catalog.frequentFoodNames, 16)
})

onMounted(async () => {
  try {
    await loadCatalog()
  } catch (error) {
    toast(error instanceof Error ? error.message : catalogStatus.error)
  }
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

    <p v-if="!catalogStatus.loaded && catalogStatus.loading" class="empty">加载中...</p>
    <p v-else-if="!list.length" class="empty">暂无食物数据</p>
    <div v-else class="food-grid mt-16">
      <button
        v-for="f in list"
        :key="f.id + tab"
        class="food-card"
        @click="router.push({ path: '/add-food', query: { food: f.name } })"
      >
        <span class="f-emoji">{{ f.emoji }}</span>
        <span class="f-name">{{ f.name }}</span>
        <span class="f-cal">{{ f.calories }} 卡路里</span>
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

.empty {
  margin-top: 24px;
  text-align: center;
  color: var(--neutral-2);
  font-size: var(--fs-body2);
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
