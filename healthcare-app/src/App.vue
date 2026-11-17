<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const screenRef = ref<HTMLElement | null>(null)

// 切换路由时把手机屏幕内的滚动位置归零（内部滚动容器，router scrollBehavior 管不到）
watch(
  () => route.fullPath,
  async () => {
    await nextTick()
    screenRef.value?.scrollTo({ top: 0 })
  },
)
</script>

<template>
  <div class="phone-wrap">
    <div class="phone">
      <div ref="screenRef" class="phone-screen">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>

      <AppTabBar v-if="route.meta.tab" />
    </div>
  </div>
</template>

<style scoped>
.page-fade-enter-active,
.page-fade-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
