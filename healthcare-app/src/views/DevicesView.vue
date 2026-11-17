<script setup lang="ts">
// 模块：设备页 —— 设备目录、连接状态、电量和同步时间。
import { computed, onMounted, ref } from 'vue'
import { health } from '../utils/healthStore'
import { catalog, loadCatalog } from '../utils/catalog'
import { toast } from '../utils/toast'

const addOpen = ref(false)

const primaryDevice = computed(
  () => catalog.devices.find((device) => device.primary) ?? catalog.devices[0],
)
const otherDevices = computed(() =>
  catalog.devices.filter((device) => device.id !== primaryDevice.value?.id),
)

function formatSyncTime(value: string) {
  if (!value) return '暂无同步记录'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '暂无同步记录'
  const now = new Date()
  const time = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  if (date.toDateString() === now.toDateString()) return `今天 ${time}`
  return `${date.getMonth() + 1}月${date.getDate()}日 ${time}`
}

function toggleConnection() {
  health.deviceConnected = !health.deviceConnected
  if (health.deviceConnected) {
    health.deviceLastSyncAt = new Date().toISOString()
  }
}

function addDevice(name: string) {
  health.deviceAdded = true
  health.deviceLastSyncAt = new Date().toISOString()
  addOpen.value = false
  toast(`已添加 ${name} ✓`)
}

onMounted(() => {
  loadCatalog().catch(() => undefined)
})
</script>

<template>
  <div class="devices">
    <StatusBar />

    <h1 class="page-title">设备</h1>
    <p class="page-sub">管理您的健康设备</p>

    <!-- 主设备卡片 -->
    <div v-if="primaryDevice" class="main-card card mt-16">
      <div class="watch-icon">{{ primaryDevice.icon }}</div>
      <div class="main-info">
        <h2 class="num">{{ primaryDevice.name }}</h2>
        <p class="status">
          {{ health.deviceConnected ? `已连接 · 电量 ${health.deviceBattery}%` : '已断开' }}
        </p>
      </div>
      <span :class="health.deviceConnected ? 'dot-ok' : 'dot-off'"></span>
    </div>

    <!-- 其他设备 -->
    <div class="sub-grid mt-16">
      <div v-for="device in otherDevices" :key="device.id" class="sub-card card">
        <span class="sub-icon">{{ device.icon }}</span>
        <h3>{{ device.name }}</h3>
        <p>{{ device.capabilities }}</p>
      </div>
    </div>

    <div class="sync card mt-16">
      <div class="row gap-12">
        <span class="sync-icon">🔄</span>
        <div>
          <h3>同步数据</h3>
          <p>上次同步：{{ formatSyncTime(health.deviceLastSyncAt) }}</p>
        </div>
      </div>
      <button class="sync-btn" @click="toggleConnection">
        {{ health.deviceConnected ? '断开' : '连接' }}
      </button>
    </div>

    <button class="btn-ghost-add mt-20" @click="addOpen = true">
      {{ health.deviceAdded ? '✓ 添加更多设备' : '＋ 添加新设备' }}
    </button>

    <div class="tabbar-spacer"></div>

    <BottomSheet :open="addOpen" @close="addOpen = false">
      <h3 class="sheet-title">添加设备</h3>
      <p class="add-tip">选择要连接的设备类型（当前为 Web 原型，真实同步需接入设备 SDK）。</p>
      <div class="device-options">
        <button
          v-for="device in catalog.devices"
          :key="device.id"
          class="device-option card"
          @click="addDevice(device.name)"
        >
          <span class="d-icon">{{ device.icon }}</span>
          <span class="d-info">
            <strong>{{ device.name }}</strong>
            <small>{{ device.capabilities }}</small>
          </span>
          <span class="d-add">＋</span>
        </button>
      </div>
    </BottomSheet>
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

.add-tip {
  font-size: var(--fs-body2);
  line-height: 1.6;
  color: var(--neutral-2);
  text-align: center;
}

.device-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.device-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  text-align: left;
}

.d-icon {
  font-size: 28px;
}

.d-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.d-info strong {
  font-size: var(--fs-headline3);
}

.d-info small {
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.d-add {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--primary);
  color: var(--white);
  display: flex;
  align-items: center;
  justify-content: center;
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
