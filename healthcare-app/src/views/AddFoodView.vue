<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { foods, recentlyEaten as recent, snackFoods as snacks } from '../mock/foods'
import { toast } from '../utils/toast'
import { health } from '../utils/healthStore'
import { api } from '../api'
import { todayISO } from '../utils/date'
import { MEALS, mealLabel } from '../utils/meals'

const route = useRoute()
const router = useRouter()

const meal = ref('breakfast')
const logDate = ref(todayISO())
const adding = ref(false)
const mealOptions = MEALS.map((item) => ({ value: item.key, label: item.label }))

// 从搜索页带过来的食物
const picked = computed(() => {
  const name = String(route.query.food || '')
  return name ? foods.find((f) => f.name === name) : undefined
})

async function addFood(name: string, cal: string, emoji: string, portion: string) {
  if (adding.value) return
  const calories = Number.parseInt(cal, 10) || 0
  adding.value = true
  try {
    const result = await api.createFoodLog({
      date: logDate.value,
      meal: meal.value,
      name,
      portion,
      calories,
      emoji,
    })
    health.foodKcal = result.todayTotalCalories
    toast(`已添加 ${name} 到${mealLabel(meal.value)}`)
  } catch (error) {
    toast(error instanceof Error ? error.message : '添加失败')
  } finally {
    adding.value = false
  }
}
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
        <p class="f-en">{{ picked.en }}</p>
      </div>
      <span class="f-cal">{{ picked.cal }}</span>
      <button class="picked-add" @click="addFood(picked.name, picked.cal, picked.emoji, picked.en)">
        添加
      </button>
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
    <div class="foods mt-12">
      <div
        v-for="f in recent"
        :key="f.name"
        class="food card"
        @click="addFood(f.name, f.cal, f.emoji, f.en)"
      >
        <span class="f-emoji">{{ f.emoji }}</span>
        <div class="f-info">
          <h4>{{ f.name }}</h4>
          <p class="f-en">{{ f.en }}</p>
        </div>
        <span class="f-cal">{{ f.cal }}</span>
        <span class="f-add">＋</span>
      </div>
    </div>

    <!-- 零食 -->
    <h3 class="section-title mt-20">零食</h3>
    <div class="foods mt-12">
      <div
        v-for="f in snacks"
        :key="f.name"
        class="food card"
        @click="addFood(f.name, f.cal, f.emoji, f.en)"
      >
        <span class="f-emoji">{{ f.emoji }}</span>
        <div class="f-info">
          <h4>{{ f.name }}</h4>
          <p class="f-en">{{ f.en }}</p>
        </div>
        <span class="f-cal">{{ f.cal }}</span>
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
