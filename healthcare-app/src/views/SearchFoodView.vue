<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { foods as allFoods } from '../mock/foods'

const router = useRouter()
const keyword = ref('')

const list = computed(() => {
  if (!keyword.value.trim()) return allFoods
  return allFoods.filter(
    (f) =>
      f.name.includes(keyword.value) || f.en.toLowerCase().includes(keyword.value.toLowerCase()),
  )
})
</script>

<template>
  <div class="search">
    <StatusBar />

    <NavBar title="搜索食物" @back="goBack(router, '/discover')" />

    <div class="search-box card">
      <span class="s-icon">🔍</span>
      <input v-model="keyword" placeholder="搜索食物" />
    </div>

    <div class="list mt-16">
      <button
        v-for="f in list"
        :key="f.name"
        class="food-row card"
        @click="router.push({ path: '/add-food', query: { food: f.name } })"
      >
        <span class="f-emoji">{{ f.emoji }}</span>
        <div class="f-info">
          <h4>{{ f.name }}</h4>
          <p class="f-en">{{ f.en }}</p>
        </div>
        <span class="f-cal">{{ f.cal }}</span>
        <span class="f-add">＋</span>
      </button>
      <p v-if="!list.length" class="empty">未找到相关食物</p>
    </div>
  </div>
</template>

<style scoped>
.search {
  padding: 0 24px 24px;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  height: 48px;
  border-radius: var(--radius-pill);
}

.s-icon {
  font-size: 16px;
}

.search-box input {
  flex: 1;
  font-size: var(--fs-headline3);
  color: var(--neutral);
}

.search-box input::placeholder {
  color: var(--neutral-2);
}

.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.food-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  text-align: left;
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

.empty {
  text-align: center;
  color: var(--neutral-2);
  padding: 40px 0;
}
</style>
