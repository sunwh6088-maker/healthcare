<script setup lang="ts">
import { health } from '../utils/healthStore'
</script>

<template>
  <div class="devices">
    <StatusBar />

    <h1 class="page-title">设备</h1>
    <p class="page-sub">管理您的健康设备</p>

    <!-- 主设备卡片 -->
    <div class="main-card card mt-16">
      <div class="watch-icon">⌚</div>
      <div class="main-info">
        <h2 class="num">智能手表</h2>
        <p class="status">
          {{ health.deviceConnected ? '已连接 · 电量 86%' : '已断开' }}
        </p>
      </div>
      <span :class="health.deviceConnected ? 'dot-ok' : 'dot-off'"></span>
    </div>

    <!-- 其他设备 -->
    <div class="sub-grid mt-16">
      <div class="sub-card card">
        <span class="sub-icon">📱</span>
        <h3>智能手机</h3>
        <p>步数 · 睡眠</p>
      </div>
      <div class="sub-card card">
        <span class="sub-icon">⚖️</span>
        <h3>智能体脂秤</h3>
        <p>体重 · 体脂</p>
      </div>
    </div>

    <div class="sync card mt-16">
      <div class="row gap-12">
        <span class="sync-icon">🔄</span>
        <div>
          <h3>同步数据</h3>
          <p>上次同步：今天 09:41</p>
        </div>
      </div>
      <button class="sync-btn" @click="health.deviceConnected = !health.deviceConnected">
        {{ health.deviceConnected ? '断开' : '连接' }}
      </button>
    </div>

    <button class="btn-ghost-add mt-20" @click="health.deviceAdded = !health.deviceAdded">
      {{ health.deviceAdded ? '✓ 已添加' : '＋ 添加新设备' }}
    </button>

    <div class="tabbar-spacer"></div>
  </div>
</template>

<style scoped>
.devices {
  padding: 0 24px 24px;
}

.page-title {
  font-size: var(--fs-title3);
  font-weight: 700;
}

.main-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.watch-icon {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-md);
  background: var(--primary-2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
}

.main-info h2 {
  font-size: var(--fs-headline3);
  font-weight: 700;
}

.main-info .status {
  margin-top: 4px;
  font-size: var(--fs-body2);
  color: var(--primary);
}

.dot-ok,
.dot-off {
  margin-left: auto;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--primary);
}

.dot-off {
  background: var(--neutral-2);
}

.sub-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.sub-card {
  padding: 16px;
}

.sub-icon {
  font-size: 32px;
}

.sub-card h3 {
  margin-top: 8px;
  font-size: var(--fs-headline3);
  font-weight: 600;
}

.sub-card p {
  margin-top: 2px;
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.sync {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
}

.sync-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--accent-2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.sync h3 {
  font-size: var(--fs-headline3);
  font-weight: 600;
}
.sync p {
  margin-top: 2px;
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.sync-btn {
  padding: 8px 20px;
  border-radius: var(--radius-pill);
  background: var(--primary);
  color: var(--white);
  font-weight: 600;
  font-size: var(--fs-body2);
}

.btn-ghost-add {
  width: 100%;
  height: 48px;
  border-radius: var(--radius-pill);
  border: 1.5px dashed var(--primary);
  color: var(--primary);
  font-size: var(--fs-headline3);
  font-weight: 600;
  background: transparent;
}
</style>
