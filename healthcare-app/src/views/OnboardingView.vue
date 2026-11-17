<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const idx = ref(0)

const pages = [
  {
    emoji: '🏃',
    bg: '#fdedd0',
    title: '运动起来',
    desc: '记住您要评估的活动，快速检查健康状况并且很容易。',
  },
  {
    emoji: '🥗',
    bg: '#ffe2d3',
    title: '吃得更好',
    desc: '帮助您密切和理想地计划您的饮食，这样您就拥有一个健康的身体，并仍然拥有身体所需的所有营养。',
  },
  {
    emoji: '⚖️',
    bg: '#d2d0e8',
    title: '管理体重',
    desc: '严格控制体重，给你想要的理想身材。',
  },
  {
    emoji: '😴',
    bg: '#bacac2',
    title: '睡得更好',
    desc: '控制你的睡眠，确保你睡个好觉，这样你就可以在第二天有一个好的学习和工作。',
  },
]

const page = computed(() => pages[idx.value])

function next() {
  if (idx.value < pages.length - 1) idx.value++
  else router.push('/login')
}
</script>

<template>
  <div class="onboarding">
    <StatusBar />

    <div class="top-row">
      <button class="skip orange-link" @click="router.push('/login')">跳过</button>
    </div>

    <transition name="illus" mode="out-in">
      <div :key="idx" class="illus-wrap">
        <div class="illus-bg" :style="{ background: page.bg }"></div>
        <div class="illus-emoji">{{ page.emoji }}</div>
      </div>
    </transition>

    <transition name="text" mode="out-in">
      <div :key="idx" class="text-wrap">
        <h2 class="title">{{ page.title }}</h2>
        <p class="desc">{{ page.desc }}</p>
      </div>
    </transition>

    <div class="bottom">
      <div class="dots">
        <span v-for="(_p, i) in pages" :key="i" class="dot" :class="{ active: i === idx }"></span>
      </div>
      <button class="next-btn" @click="next">
        <span class="next-inner">➜</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.onboarding {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0 24px;
}

.top-row {
  display: flex;
  justify-content: flex-end;
  padding: 8px 0;
}

.skip {
  font-size: var(--fs-headline3);
  font-weight: 600;
}

.illus-wrap {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 280px;
}

.illus-bg {
  position: absolute;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  opacity: 0.9;
}

.illus-emoji {
  position: relative;
  font-size: 96px;
  animation: float 2.6s ease-in-out infinite;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-12px);
  }
}

.text-wrap {
  padding: 8px 8px 0;
  text-align: center;
}

.title {
  font-size: var(--fs-title3);
  line-height: var(--lh-title3);
  font-weight: 700;
}

.desc {
  margin-top: 12px;
  font-size: var(--fs-body1);
  line-height: var(--lh-body1);
  color: var(--neutral-2);
}

.bottom {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 32px 0 40px;
}

.dots {
  display: flex;
  gap: 10px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--neutral-3);
  transition: all 0.2s ease;
}

.dot.active {
  width: 28px;
  border-radius: 6px;
  background: var(--primary);
}

.next-btn {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--white);
  box-shadow: 0 8px 24px rgba(20, 12, 7, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.15s ease;
}

.next-btn:active {
  transform: scale(0.92);
}

.next-inner {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: var(--primary);
  color: var(--white);
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.illus-enter-active,
.illus-leave-active {
  transition: all 0.25s ease;
}
.illus-enter-from {
  opacity: 0;
  transform: scale(0.9);
}
.illus-leave-to {
  opacity: 0;
  transform: scale(1.05);
}
.text-enter-active,
.text-leave-active {
  transition: all 0.2s ease;
}
.text-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.text-leave-to {
  opacity: 0;
}
</style>
