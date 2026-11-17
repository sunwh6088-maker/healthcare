<script setup lang="ts">
// 模块：运动记录弹层 —— 新增、编辑和删除运动记录。
import { computed, nextTick, ref, watch } from 'vue'
import BottomSheet from './BottomSheet.vue'
import SegmentedControl from './SegmentedControl.vue'
import { api, type ExerciseLog } from '../api'
import { catalog, loadCatalog } from '../utils/catalog'
import { todayISO } from '../utils/date'
import { toast } from '../utils/toast'

const props = defineProps<{
  open: boolean
  log?: ExerciseLog | null
  defaultType?: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'saved', log: ExerciseLog): void
  (e: 'deleted', id: number): void
}>()

// ===== 表单状态 =====
const type = ref('')
const date = ref(todayISO())
const minutes = ref(30)
const calories = ref(0)
const saving = ref(false)
const confirmDelete = ref(false)
const initializing = ref(false)

const options = computed(() =>
  catalog.exerciseTypes.map((sport) => ({ value: sport.key, label: sport.name })),
)
const activeType = computed(() => catalog.exerciseTypes.find((sport) => sport.key === type.value))

function recalcCalories() {
  const sport = activeType.value
  if (!sport) return
  calories.value = Math.max(0, Math.round((sport.kcal * Number(minutes.value)) / 30))
}

// ===== 打开弹层时初始化：新增用默认类型，编辑用原记录 =====
watch(
  () => props.open,
  async (open) => {
    if (!open) {
      confirmDelete.value = false
      return
    }
    initializing.value = true
    loadCatalog().catch(() => undefined)

    if (props.log) {
      type.value = props.log.type
      date.value = props.log.date
      minutes.value = props.log.minutes
      calories.value = props.log.calories
    } else {
      type.value = props.defaultType || catalog.exerciseTypes[0]?.key || 'yoga'
      date.value = todayISO()
      minutes.value = 30
      recalcCalories()
    }
    confirmDelete.value = false
    await nextTick()
    initializing.value = false
  },
  { immediate: true },
)

// 目录异步加载完成后，修正当前类型
watch(
  () => catalog.exerciseTypes.length,
  () => {
    if (!props.open || !catalog.exerciseTypes.length) return
    if (!catalog.exerciseTypes.some((sport) => sport.key === type.value)) {
      type.value =
        props.defaultType && catalog.exerciseTypes.some((sport) => sport.key === props.defaultType)
          ? props.defaultType
          : catalog.exerciseTypes[0].key
      if (!props.log) recalcCalories()
    }
  },
)

watch([type, minutes], () => {
  if (!initializing.value && props.open) recalcCalories()
})

// ===== 保存与删除 =====
async function save() {
  if (!type.value) {
    toast('请选择运动类型')
    return
  }
  if (Number(minutes.value) < 1) {
    toast('运动时长至少 1 分钟')
    return
  }
  if (saving.value) return
  saving.value = true
  try {
    const payload = {
      date: date.value,
      type: type.value,
      minutes: Number(minutes.value),
      calories: Math.max(0, Number(calories.value) || 0),
    }
    const saved = props.log
      ? await api.updateExerciseLog(props.log.id, payload)
      : await api.createExerciseLog(payload)
    toast(props.log ? '运动记录已更新' : '运动已记录')
    emit('saved', saved)
    emit('close')
  } catch (error) {
    toast(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove() {
  if (!props.log) return
  if (!confirmDelete.value) {
    confirmDelete.value = true
    return
  }
  try {
    await api.deleteExerciseLog(props.log.id)
    toast('运动记录已删除')
    emit('deleted', props.log.id)
    emit('close')
  } catch (error) {
    toast(error instanceof Error ? error.message : '删除失败')
  }
}
</script>

<template>
  <BottomSheet :open="props.open" @close="emit('close')">
    <h3 class="sheet-title">{{ props.log ? '编辑运动' : '记录运动' }}</h3>

    <div class="sheet-field">
      <span class="sheet-key">运动类型</span>
      <SegmentedControl v-model="type" :options="options" />
    </div>

    <label class="sheet-field">
      <span class="sheet-key">日期</span>
      <input v-model="date" class="num sheet-input" type="date" />
    </label>

    <div class="row-fields">
      <label class="sheet-field">
        <span class="sheet-key">时长（分钟）</span>
        <input v-model.number="minutes" class="num sheet-input" type="number" min="1" max="600" />
      </label>
      <label class="sheet-field">
        <span class="sheet-key">消耗（千卡）</span>
        <input v-model.number="calories" class="num sheet-input" type="number" min="0" max="5000" />
      </label>
    </div>

    <div class="preset-row">
      <button
        v-for="preset in [15, 30, 45, 60]"
        :key="preset"
        class="preset"
        :class="{ active: Number(minutes) === preset }"
        @click="minutes = preset"
      >
        {{ preset }} 分钟
      </button>
    </div>

    <div class="sheet-actions">
      <button class="btn-primary" :class="{ disabled: saving }" :disabled="saving" @click="save">
        {{ saving ? '保存中...' : '保存' }}
      </button>
      <button v-if="props.log" class="sheet-delete" @click="remove">
        {{ confirmDelete ? '再次点击确认删除' : '删除这条记录' }}
      </button>
    </div>
  </BottomSheet>
</template>

<style scoped>
.row-fields {
  display: flex;
  gap: 10px;
}

.row-fields .sheet-field {
  flex: 1;
}

.preset-row {
  display: flex;
  gap: 8px;
}

.preset {
  flex: 1;
  height: 36px;
  border-radius: var(--radius-pill);
  background: #f7f9f8;
  color: var(--neutral-2);
  font-size: var(--fs-body2);
}

.preset.active {
  background: var(--primary-2);
  color: var(--primary);
  font-weight: 600;
}
</style>
