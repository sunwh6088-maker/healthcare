<script setup lang="ts">
// 模块：睡眠页 —— 睡眠时长、闹钟管理和助眠声音。
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { toast } from '../utils/toast'
import { health, type Alarm } from '../utils/healthStore'
import { catalog, loadCatalog } from '../utils/catalog'
import { repeatText } from '../utils/alarm'

const router = useRouter()

const sleepText = computed(() => {
  const h = Math.floor(health.sleepMinutes / 60)
  const m = health.sleepMinutes % 60
  return `${h}小时${m}分`
})

function formatMinutes(total: number): string {
  const m = ((total % 1440) + 1440) % 1440
  const h = Math.floor(m / 60)
  const mm = m % 60
  return `${String(h).padStart(2, '0')}:${String(mm).padStart(2, '0')}`
}

const bedTime = computed(() => formatMinutes(health.sleepStartMinutes))
const wakeTime = computed(() => formatMinutes(health.sleepStartMinutes + health.sleepMinutes))

const clockNums = [
  { n: '12', x: 50, y: 6 },
  { n: '1', x: 71, y: 11 },
  { n: '2', x: 86, y: 25 },
  { n: '3', x: 92, y: 47 },
  { n: '4', x: 86, y: 69 },
  { n: '5', x: 71, y: 82 },
  { n: '6', x: 50, y: 87 },
  { n: '7', x: 29, y: 82 },
  { n: '8', x: 14, y: 69 },
  { n: '9', x: 8, y: 47 },
  { n: '10', x: 14, y: 25 },
  { n: '11', x: 29, y: 11 },
]

// 新增闹钟时依次取未被占用的时间，避免重复
const presetTimes = ['07:00', '06:30', '07:30', '06:00', '08:00']

// 星期选择（1 = 周一 … 0 = 周日）
const weekDays = [
  { value: 1, label: '一' },
  { value: 2, label: '二' },
  { value: 3, label: '三' },
  { value: 4, label: '四' },
  { value: 5, label: '五' },
  { value: 6, label: '六' },
  { value: 0, label: '日' },
]
// 编辑弹层
const sheetOpen = ref(false)
const isNew = ref(false)
const editId = ref<number | null>(null)
const editTime = ref('07:00')
const editLabel = ref('新闹钟')
const editRepeat = ref<number[]>([])
const confirmDelete = ref(false)
const soundPlaying = ref(false)
const sound = computed(() => catalog.sleepSounds[0])
let audioContext: AudioContext | null = null
let ambientSource: AudioBufferSourceNode | null = null

onMounted(() => {
  loadCatalog().catch(() => undefined)
})

onUnmounted(() => {
  stopAmbient()
})

function toggleRepeat(day: number) {
  if (editRepeat.value.includes(day)) {
    editRepeat.value = editRepeat.value.filter((d) => d !== day)
  } else {
    editRepeat.value = [...editRepeat.value, day]
  }
}

function sortAlarms() {
  health.alarms.sort((a, b) => a.time.localeCompare(b.time))
}

function openNewAlarm() {
  const used = new Set(health.alarms.map((a) => a.time))
  editId.value = null
  isNew.value = true
  editTime.value = presetTimes.find((t) => !used.has(t)) ?? '07:00'
  editLabel.value = '新闹钟'
  editRepeat.value = [1, 2, 3, 4, 5]
  confirmDelete.value = false
  sheetOpen.value = true
}

function openEdit(a: Alarm) {
  editId.value = a.id
  isNew.value = false
  editTime.value = a.time
  editLabel.value = a.label
  editRepeat.value = [...a.repeat]
  confirmDelete.value = false
  sheetOpen.value = true
}

function closeSheet() {
  sheetOpen.value = false
  confirmDelete.value = false
}

function saveAlarm() {
  if (!editTime.value) {
    toast('请选择闹钟时间')
    return
  }
  const label = editLabel.value.trim() || '闹钟'
  const repeat = [...editRepeat.value].sort((a, b) => a - b)
  if (isNew.value) {
    health.alarms.push({ id: Date.now(), time: editTime.value, label, repeat, on: true })
    toast(`已添加闹钟 ${editTime.value}`)
  } else {
    const alarm = health.alarms.find((a) => a.id === editId.value)
    if (alarm) {
      alarm.time = editTime.value
      alarm.label = label
      alarm.repeat = repeat
      toast(`闹钟已更新为 ${editTime.value}`)
    }
  }
  sortAlarms()
  closeSheet()
}

function requestDelete() {
  if (!confirmDelete.value) {
    confirmDelete.value = true
    toast('再次点击即可删除闹钟')
    return
  }
  deleteAlarm()
}

function deleteAlarm() {
  const index = health.alarms.findIndex((a) => a.id === editId.value)
  if (index >= 0) {
    const [removed] = health.alarms.splice(index, 1)
    toast(`已删除闹钟 ${removed.time}`)
  }
  closeSheet()
}

// 用 Web Audio 生成低频白噪音，无需额外音频文件
function startAmbient() {
  const AudioCtor =
    window.AudioContext ||
    (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
  if (!AudioCtor) {
    toast('当前浏览器不支持音频播放')
    return
  }
  audioContext = new AudioCtor()
  void audioContext.resume()
  const bufferSize = audioContext.sampleRate * 2
  const buffer = audioContext.createBuffer(1, bufferSize, audioContext.sampleRate)
  const data = buffer.getChannelData(0)
  for (let i = 0; i < bufferSize; i++) {
    data[i] = (Math.random() * 2 - 1) * 0.12
  }
  const source = audioContext.createBufferSource()
  source.buffer = buffer
  source.loop = true
  const filter = audioContext.createBiquadFilter()
  filter.type = 'lowpass'
  filter.frequency.value = 900
  source.connect(filter)
  filter.connect(audioContext.destination)
  source.start()
  ambientSource = source
}

function stopAmbient() {
  ambientSource?.stop()
  ambientSource = null
  void audioContext?.close().catch(() => undefined)
  audioContext = null
}

function toggleSound() {
  if (soundPlaying.value) {
    stopAmbient()
    soundPlaying.value = false
    toast('已暂停')
    return
  }
  startAmbient()
  if (!audioContext) return
  soundPlaying.value = true
  toast(`正在播放：${sound.value?.name ?? '助眠声音'}`)
}

function setAlarm(a: Alarm, on: boolean) {
  a.on = on
  toast(`闹钟 ${a.time} 已${on ? '开启' : '关闭'}`)
}
</script>

<template>
  <div class="sleep">
    <StatusBar />

    <NavBar title="睡眠" @back="goBack(router)">
      <template #right>
        <button class="add-top green-link" @click="openNewAlarm">添加</button>
      </template>
    </NavBar>

    <!-- 时钟插图 -->
    <div class="clock card mt-8">
      <div class="clock-face">
        <span
          v-for="c in clockNums"
          :key="c.n"
          class="num clock-num"
          :style="{ left: c.x + '%', top: c.y + '%' }"
          >{{ c.n }}</span
        >
        <div class="clock-hand h1"></div>
        <div class="clock-hand h2"></div>
      </div>
      <div class="sleep-time">
        <span class="num big">{{ sleepText }}</span>
        <span class="sub">总睡眠时长</span>
      </div>
      <div class="bed-wake">
        <div class="bw">
          <span class="bw-label">就寝时间</span>
          <span class="num bw-value">{{ bedTime }}</span>
        </div>
        <div class="bw">
          <span class="bw-label">醒来时间</span>
          <span class="num bw-value">{{ wakeTime }}</span>
        </div>
      </div>
    </div>

    <!-- 闹钟列表 -->
    <div class="alarms card mt-16">
      <div class="alarm-head">
        <span class="section-title">设置闹钟</span>
        <button class="green-link" @click="openNewAlarm">＋ 添加</button>
      </div>
      <div v-for="a in health.alarms" :key="a.id" class="alarm-row" @click="openEdit(a)">
        <span class="num alarm-time">{{ a.time }}</span>
        <div class="alarm-info">
          <span class="alarm-label">{{ a.label }}</span>
          <span v-if="repeatText(a.repeat) !== a.label" class="alarm-repeat">
            {{ repeatText(a.repeat) }}
          </span>
        </div>
        <span @click.stop>
          <AppSwitch
            :model-value="a.on"
            :label="`${a.time} 闹钟`"
            @update:modelValue="setAlarm(a, $event)"
          />
        </span>
      </div>
      <p v-if="health.alarms.length" class="alarm-tip">点按闹钟可编辑时间、重复或删除</p>
    </div>

    <!-- 助眠声音 -->
    <button
      v-if="sound"
      class="sound card mt-16"
      :aria-label="soundPlaying ? '暂停助眠声音' : '播放助眠声音'"
      @click="toggleSound"
    >
      <div class="sound-icon">{{ sound.emoji }}</div>
      <span class="sound-name">{{ sound.name }}</span>
      <span class="sound-play">{{ soundPlaying ? '⏸' : '▶' }}</span>
    </button>

    <!-- 编辑/新增闹钟弹层 -->
    <BottomSheet :open="sheetOpen" @close="closeSheet">
      <h3 class="sheet-title">{{ isNew ? '添加闹钟' : '编辑闹钟' }}</h3>

      <label class="sheet-field">
        <span class="sheet-key">时间</span>
        <input v-model="editTime" class="num sheet-input sheet-time" type="time" />
      </label>

      <label class="sheet-field">
        <span class="sheet-key">标签</span>
        <input v-model="editLabel" class="sheet-input" placeholder="例如：工作日" />
      </label>

      <div class="sheet-field">
        <span class="sheet-key">重复</span>
        <div class="repeat-row">
          <button
            v-for="d in weekDays"
            :key="d.value"
            type="button"
            class="repeat-chip"
            :class="{ active: editRepeat.includes(d.value) }"
            @click="toggleRepeat(d.value)"
          >
            {{ d.label }}
          </button>
        </div>
        <span class="repeat-summary">{{ repeatText(editRepeat) }}</span>
      </div>

      <div class="sheet-actions">
        <button v-if="!isNew" class="sheet-delete" @click="requestDelete">
          {{ confirmDelete ? '再次点击确认删除' : '删除闹钟' }}
        </button>
        <button class="btn-primary" @click="saveAlarm">保存</button>
      </div>
    </BottomSheet>
  </div>
</template>

<style scoped>
.sleep {
  padding: 0 24px 24px;
}

.add-top {
  font-weight: 600;
}

.clock {
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.clock-face {
  position: relative;
  width: 190px;
  height: 190px;
  border-radius: 50%;
  background: var(--primary);
}

.clock-num {
  position: absolute;
  transform: translate(-50%, -50%);
  font-size: var(--fs-caption);
  font-weight: 600;
  color: var(--neutral-2);
}

.clock-face .clock-num {
  color: var(--white);
  opacity: 0.85;
}

.h1 {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 4px;
  height: 56px;
  border-radius: 2px;
  background: var(--white);
  transform-origin: bottom center;
  transform: translate(-50%, -100%) rotate(30deg);
}

.h2 {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 4px;
  height: 74px;
  border-radius: 2px;
  background: var(--white);
  transform-origin: bottom center;
  transform: translate(-50%, -100%) rotate(-60deg);
}

.sleep-time {
  margin-top: 20px;
  text-align: center;
}

.sleep-time .big {
  font-size: var(--fs-title3);
  font-weight: 700;
}

.sleep-time .sub {
  display: block;
  margin-top: 4px;
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.bed-wake {
  margin-top: 20px;
  width: 100%;
  display: flex;
  gap: 12px;
}

.bw {
  flex: 1;
  background: var(--surface-soft, #f7f9f8);
  border-radius: var(--radius-md);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
}

.bw-label {
  font-size: var(--fs-body2);
  color: var(--neutral);
}
.bw-value {
  font-size: var(--fs-title3);
  font-weight: 700;
}

.alarms {
  overflow: hidden;
}

.alarm-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 8px;
}

.alarm-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--neutral-3);
  cursor: pointer;
  transition: background 0.15s ease;
}

.alarm-row:hover {
  background: #f7f9f8;
}

.alarm-row:last-of-type {
  border-bottom: none;
}

.alarm-time {
  font-size: var(--fs-headline3);
  font-weight: 700;
}
.alarm-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.alarm-label {
  font-size: var(--fs-body2);
  color: var(--neutral);
}
.alarm-repeat {
  font-size: var(--fs-caption);
  color: var(--neutral-2);
}

.alarm-tip {
  padding: 8px 20px 14px;
  font-size: var(--fs-caption);
  color: var(--neutral-2);
}

/* 编辑闹钟弹层 */
.sheet-time {
  font-size: var(--fs-title3);
  font-weight: 700;
  letter-spacing: 1px;
}

.repeat-row {
  display: flex;
  gap: 8px;
}

.repeat-chip {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f7f9f8;
  color: var(--neutral-2);
  font-size: var(--fs-body2);
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.repeat-chip.active {
  background: var(--primary);
  color: var(--white);
}

.repeat-summary {
  font-size: var(--fs-caption);
  color: var(--neutral-2);
}

.sound {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  text-align: left;
  color: var(--neutral);
}

.sound-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.sound-name {
  flex: 1;
  font-size: var(--fs-headline3);
  font-weight: 600;
}

.sound-play {
  color: var(--primary);
}
</style>
