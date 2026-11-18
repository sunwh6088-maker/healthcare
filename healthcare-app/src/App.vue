<script setup lang="ts">
// 模块：应用外壳 —— 手机外框、页面过渡、底部 Tab 栏和滚动复位。
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const screenRef = ref<HTMLElement | null>(null)

// 离线提示：网络恢复后 healthStore 会继续防抖同步
const online = ref(navigator.onLine)

function updateOnline() {
  online.value = navigator.onLine
}

onMounted(() => {
  window.addEventListener('online', updateOnline)
  window.addEventListener('offline', updateOnline)
})

onUnmounted(() => {
  window.removeEventListener('online', updateOnline)
  window.removeEventListener('offline', updateOnline)
})

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
      <div v-if="!online" class="offline-tip">网络已断开，恢复后将自动同步</div>
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
.offline-tip {
  position: absolute;
  top: 44px;
  left: 0;
  right: 0;
  z-index: 20;
  padding: 6px 12px;
  background: var(--accent);
  color: var(--white);
  font-size: var(--fs-caption);
  text-align: center;
}

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
