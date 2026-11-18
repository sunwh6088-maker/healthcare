<script setup lang="ts">
// 模块：404 页面 —— 未匹配路由的兜底。
import { useRouter } from 'vue-router'
import { hasSession } from '../utils/session'

const router = useRouter()

function goHome() {
  router.replace(hasSession() ? '/home' : '/login')
}
</script>

<template>
  <div class="not-found">
    <StatusBar />
    <div class="content">
      <div class="code num">404</div>
      <h1>页面不存在</h1>
      <p>你访问的页面可能已被移动或删除。</p>
      <button class="btn-primary mt-24" @click="goHome">
        返回{{ hasSession() ? '首页' : '登录' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 32px 80px;
  text-align: center;
}

.code {
  font-size: 72px;
  font-weight: 800;
  color: var(--primary-2);
  letter-spacing: 4px;
}

h1 {
  margin-top: 8px;
  font-size: var(--fs-headline1);
  font-weight: 700;
}

p {
  margin-top: 10px;
  font-size: var(--fs-body2);
  line-height: 1.6;
  color: var(--neutral-2);
}

button {
  width: 100%;
}
</style>
