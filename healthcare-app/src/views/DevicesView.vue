<script setup lang="ts">
// 模块：设备页 —— 绑定真实 IoT 设备、查看上报状态并模拟上报。
import { computed, onMounted, ref } from 'vue'
import { api, type IotDevice } from '../api'
import { syncFromServer } from '../utils/healthStore'
import { catalog, loadCatalog } from '../utils/catalog'
import { toast } from '../utils/toast'

const showSimulator = import.meta.env.DEV
const boundDevices = ref<IotDevice[]>([])
const loading = ref(true)
const bindOpen = ref(false)
const saving = ref(false)
const busyId = ref<number | null>(null)
const confirmUnbindId = ref<number | null>(null)
const form = ref({ productKey: '', deviceName: '', name: '' })

const primaryDevice = computed(
  () => catalog.devices.find((device) => device.primary) ?? catalog.devices[0],
)
const otherDevices = computed(() =>
  catalog.devices.filter((device) => device.id !== primaryDevice.value?.id),
)
const catalogDevices = computed(() =>
  primaryDevice.value ? [primaryDevice.value, ...otherDevices.value] : [],
)

function formatSyncTime(value: string | null) {
  if (!value) return '暂无同步记录'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '暂无同步记录'
  const now = new Date()
  const time = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  if (date.toDateString() === now.toDateString()) return `今天 ${time}`
  return `${date.getMonth() + 1}月${date.getDate()}日 ${time}`
}

async function loadDevices() {
  try {
    boundDevices.value = await api.listIotDevices()
  } catch (error) {
    toast(error instanceof Error ? error.message : '设备列表加载失败')
  } finally {
    loading.value = false
  }
}

function openBind() {
  form.value = { productKey: '', deviceName: '', name: '' }
  bindOpen.value = true
}

async function bindDevice() {
  const productKey = form.value.productKey.trim()
  const deviceName = form.value.deviceName.trim()
  if (!productKey || !deviceName) {
    toast('请填写 ProductKey 和 DeviceName')
    return
  }
  saving.value = true
  try {
    await api.bindIotDevice({
      productKey,
      deviceName,
      name: form.value.name.trim() || undefined,
    })
    await loadDevices()
    bindOpen.value = false
    toast('设备已绑定 ✓')
  } catch (error) {
    toast(error instanceof Error ? error.message : '设备绑定失败')
  } finally {
    saving.value = false
  }
}

async function simulate(device: IotDevice) {
  busyId.value = device.id
  try {
    await api.simulateIotDevice(device.id)
    await Promise.all([syncFromServer(), loadDevices()])
    toast('模拟上报完成，健康数据已更新 ✓')
  } catch (error) {
    toast(error instanceof Error ? error.message : '模拟上报失败')
  } finally {
    busyId.value = null
  }
}

async function unbind(device: IotDevice) {
  if (confirmUnbindId.value !== device.id) {
    confirmUnbindId.value = device.id
    toast('再次点击即可解绑设备')
    return
  }
  busyId.value = device.id
  try {
    await api.unbindIotDevice(device.id)
    confirmUnbindId.value = null
    await loadDevices()
    toast('设备已解绑')
  } catch (error) {
    toast(error instanceof Error ? error.message : '解绑失败')
  } finally {
    busyId.value = null
  }
}

onMounted(() => {
  loadCatalog().catch(() => undefined)
  void loadDevices()
})
</script>

<template>
  <div class="devices">
    <StatusBar />

    <h1 class="page-title">设备</h1>
    <p class="page-sub">绑定设备后，上报数据会自动同步到健康记录</p>

    <div class="between mt-16">
      <span class="section-title">已绑定设备</span>
      <button class="green-link" @click="openBind">＋ 绑定</button>
    </div>

    <div v-if="boundDevices.length" class="bound-list mt-12">
      <div v-for="device in boundDevices" :key="device.id" class="bound-card card">
        <span class="b-icon">⌚</span>
        <div class="b-info">
          <h3>{{ device.name }}</h3>
          <p class="b-key">{{ device.productKey }} / {{ device.deviceName }}</p>
          <p class="b-meta">
            {{ device.connected ? '在线' : '离线' }}
            <template v-if="device.battery">· 电量 {{ device.battery }}%</template>
            · {{ formatSyncTime(device.lastSeenAt) }}
          </p>
        </div>
        <div class="b-actions">
          <button
            v-if="showSimulator"
            class="b-sim"
            :disabled="busyId === device.id"
            @click="simulate(device)"
          >
            {{ busyId === device.id ? '上报中' : '模拟上报' }}
          </button>
          <button class="b-del" :disabled="busyId === device.id" @click="unbind(device)">
            {{ confirmUnbindId === device.id ? '确认解绑' : '解绑' }}
          </button>
        </div>
      </div>
    </div>

    <div v-else class="empty card mt-12">
      <span class="empty-emoji">⌚</span>
      <p>{{ loading ? '加载中…' : '还没有绑定设备' }}</p>
      <button v-if="!loading" class="bind-link" @click="openBind">绑定第一台设备</button>
    </div>

    <h3 class="section-title mt-20">支持的设备类型</h3>
    <div class="catalog card mt-12">
      <div v-for="device in catalogDevices" :key="device.id" class="catalog-row">
        <span class="c-icon">{{ device.icon }}</span>
        <div class="c-info">
          <strong>{{ device.name }}</strong>
          <small>{{ device.capabilities }}</small>
        </div>
      </div>
    </div>

    <p class="protocol-tip">
      数据协议：MQTT + Alink 物模型，主题
      <span class="num">sys/{productKey}/{deviceName}/thing/event/property/post</span>
    </p>

    <div class="tabbar-spacer"></div>

    <BottomSheet :open="bindOpen" @close="bindOpen = false">
      <h3 class="sheet-title">绑定设备</h3>
      <p class="add-tip">
        填写设备上报时使用的 ProductKey 和 DeviceName，绑定后该设备的数据会自动写入你的账号。
      </p>
      <label class="sheet-field">
        <span class="sheet-key">ProductKey</span>
        <input v-model="form.productKey" class="sheet-input" placeholder="例如 pk-demo" />
      </label>
      <label class="sheet-field">
        <span class="sheet-key">DeviceName</span>
        <input v-model="form.deviceName" class="sheet-input" placeholder="例如 watch-001" />
      </label>
      <label class="sheet-field">
        <span class="sheet-key">显示名称</span>
        <input v-model="form.name" class="sheet-input" placeholder="例如 我的手表（可选）" />
      </label>
      <div class="sheet-actions">
        <button
          class="btn-primary"
          :class="{ disabled: saving }"
          :disabled="saving"
          @click="bindDevice"
        >
          {{ saving ? '绑定中...' : '绑定设备' }}
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

.page-sub {
  margin-top: 4px;
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.bound-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bound-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
}

.b-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  background: var(--primary-2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.b-info {
  flex: 1;
  min-width: 0;
}

.b-info h3 {
  font-size: var(--fs-headline3);
  font-weight: 700;
}

.b-key {
  margin-top: 2px;
  font-size: var(--fs-caption);
  color: var(--neutral-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.b-meta {
  margin-top: 4px;
  font-size: var(--fs-caption);
  color: var(--primary);
}

.b-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}

.b-sim,
.b-del {
  padding: 6px 12px;
  border-radius: var(--radius-pill);
  font-size: var(--fs-caption);
  font-weight: 600;
}

.b-sim {
  background: var(--primary);
  color: var(--white);
}

.b-del {
  background: #f7f9f8;
  color: var(--accent);
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 24px;
}

.empty-emoji {
  font-size: 40px;
}

.empty p {
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.bind-link {
  margin-top: 4px;
  color: var(--primary);
  font-size: var(--fs-body2);
  font-weight: 600;
}

.catalog {
  overflow: hidden;
}

.catalog-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--neutral-3);
}

.catalog-row:last-child {
  border-bottom: none;
}

.c-icon {
  font-size: 26px;
}

.c-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.c-info strong {
  font-size: var(--fs-headline3);
  font-weight: 600;
}

.c-info small {
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.protocol-tip {
  margin-top: 16px;
  padding: 0 4px;
  font-size: var(--fs-caption);
  line-height: 1.6;
  color: var(--neutral-2);
}

.protocol-tip .num {
  word-break: break-all;
}

.add-tip {
  font-size: var(--fs-body2);
  line-height: 1.6;
  color: var(--neutral-2);
  text-align: center;
}
</style>
