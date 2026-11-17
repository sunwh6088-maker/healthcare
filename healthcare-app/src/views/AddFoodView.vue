<script setup lang="ts">
// 模块：添加食物 —— 选择餐次/日期并写入后端食物记录。
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { catalog, catalogStatus, loadCatalog, pickFoods } from '../utils/catalog'
import { toast } from '../utils/toast'
import { health } from '../utils/healthStore'
import { api, type CatalogFood } from '../api'
import { todayISO } from '../utils/date'
import { mealLabel } from '../utils/meals'

const route = useRoute()
const router = useRouter()

const meal = ref('breakfast')
const logDate = ref(todayISO())
const adding = ref(false)
const mealOptions = computed(() =>
  catalog.meals.map((item) => ({ value: item.key, label: item.label })),
)

// 从搜索页带过来的食物
const picked = computed(() => {
  const name = String(route.query.food || '')
  return name ? catalog.foods.find((f) => f.name === name) : undefined
})

// 优先展示用户最近吃过的，没有记录时回退到常吃主食
const recent = computed(() => {
  const pickedRecent = pickFoods(catalog.recentFoodNames, 6)
  return catalog.recentFoodNames.length
    ? pickedRecent
    : catalog.foods.filter((f) => f.category === 'main').slice(0, 6)
})

const snacks = computed(() => catalog.foods.filter((f) => f.category === 'snack'))

async function addFood(food: CatalogFood) {
  if (adding.value) return
  adding.value = true
  try {
    const result = await api.createFoodLog({
      date: logDate.value,
      meal: meal.value,
      name: food.name,
      portion: food.portion,
      calories: food.calories,
      emoji: food.emoji,
    })
    health.foodKcal = result.todayTotalCalories
    toast(`已添加 ${food.name} 到${mealLabel(meal.value)}`)
  } catch (error) {
    toast(error instanceof Error ? error.message : '添加失败')
  } finally {
    adding.value = false
  }
}

onMounted(async () => {
  try {
    await loadCatalog()
  } catch (error) {
    toast(error instanceof Error ? error.message : catalogStatus.error)
  }
})
</script>

<template>
  <div class="add-food">
    <StatusBar />

    <NavBar title="添加食物" @back="goBack(router, '/food')" />

    <!-- 搜索页选中的食物 -->
    <div v-if="picked" class="picked card">
      <span class="f-emoji">{{ picked.emoji }}</span>
      <div class="f-info">
        <h4>{{ picked.name }}</h4>
        <p class="f-en">{{ picked.portion }}</p>
      </div>
      <span class="f-cal">{{ picked.calories }} 卡路里</span>
      <button class="picked-add" @click="addFood(picked)">添加</button>
    </div>

    <!-- 餐次 -->
    <h3 class="section-title mt-20">餐次</h3>
    <SegmentedControl v-model="meal" :options="mealOptions" class="mt-12" />

    <!-- 日期选择 -->
    <div class="date-row card mt-16">
      <span class="d-label">日期</span>
      <input v-model="logDate" class="num d-input" type="date" />
    </div>

    <!-- 最近吃过 -->
    <h3 class="section-title mt-20">以前吃过</h3>
    <p v-if="!catalogStatus.loaded && catalogStatus.loading" class="empty">加载中...</p>
    <div v-else class="foods mt-12">
      <div v-for="f in recent" :key="f.id" class="food card" @click="addFood(f)">
        <span class="f-emoji">{{ f.emoji }}</span>
        <div class="f-info">
          <h4>{{ f.name }}</h4>
          <p class="f-en">{{ f.portion }}</p>
        </div>
        <span class="f-cal">{{ f.calories }} 卡路里</span>
        <span class="f-add">＋</span>
      </div>
    </div>

    <!-- 零食 -->
    <h3 class="section-title mt-20">零食</h3>
    <div v-if="catalogStatus.loaded" class="foods mt-12">
      <div v-for="f in snacks" :key="f.id" class="food card" @click="addFood(f)">
        <span class="f-emoji">{{ f.emoji }}</span>
        <div class="f-info">
          <h4>{{ f.name }}</h4>
          <p class="f-en">{{ f.portion }}</p>
        </div>
        <span class="f-cal">{{ f.calories }} 卡路里</span>
        <span class="f-add">＋</span>
      </div>
    </div>

    <button class="btn-primary mt-20" @click="router.push('/search-food')">🔍 搜索更多食物</button>
  </div>
</template>

<style scoped>
.add-food {
  padding: 0 24px 24px;
}

.picked {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 12px;
  background: var(--primary-2);
}

.picked .f-info h4 {
  font-size: var(--fs-headline3);
  font-weight: 700;
}

.picked .f-en {
  font-size: var(--fs-body2);
  color: var(--primary);
}

.picked .f-cal {
  margin-left: auto;
  font-size: var(--fs-body2);
  color: var(--primary);
}

.picked-add {
  height: 34px;
  padding: 0 16px;
  border-radius: var(--radius-pill);
  background: var(--primary);
  color: var(--white);
  font-size: var(--fs-body2);
  font-weight: 600;
  flex-shrink: 0;
}

.date-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
}

.d-label {
  font-size: var(--fs-headline3);
  font-weight: 600;
}

.d-input {
  font-size: var(--fs-body2);
  font-weight: 600;
  color: var(--primary);
  background: transparent;
  text-align: right;
}

.foods {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.empty {
  margin-top: 20px;
  text-align: center;
  color: var(--neutral-2);
  font-size: var(--fs-body2);
}

.food {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
}

.f-emoji {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-sm);
  background: var(--primary-2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.f-info h4 {
  font-size: var(--fs-headline3);
  font-weight: 600;
}

.f-en {
  font-size: var(--fs-body2);
  color: var(--primary);
}

.f-cal {
  margin-left: auto;
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.f-add {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--primary);
  color: var(--white);
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
