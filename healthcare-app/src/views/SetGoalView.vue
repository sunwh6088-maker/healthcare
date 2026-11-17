<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { toast } from '../utils/toast'
import { health } from '../utils/healthStore'

const route = useRoute()
const router = useRouter()

// type=steps → 设定目标目标跟踪（6000 步）；type=active → 设置目标活动时间（60 分钟）
const type = computed(() => (route.query.type === 'active' ? 'active' : 'steps'))

const config = computed(() =>
  type.value === 'steps'
    ? {
        min: 5000,
        max: 7000,
        step: 100,
        value: health.stepsGoal,
        unit: '步',
        desc: '设定每日步数目标来帮助您保持活跃和健康',
        emoji: '🏃',
      }
    : {
        min: 30,
        max: 90,
        step: 5,
        value: health.activeGoalMinutes,
        unit: '分钟',
        desc: '跟踪每日分钟数和整体进度。',
        emoji: '⏱️',
      },
)

const value = ref(config.value.value)
const dragging = ref(false)

// 从步数目标切换到活动时间目标时，重置转盘初始值
watch(type, () => {
  value.value = config.value.value
})

// 指针角度：-135°（最小）→ +135°（最大）
const pointerAngle = computed(() => {
  const { min, max } = config.value
  return -135 + ((value.value - min) / (max - min)) * 270
})

function valueFromAngle(angle: number) {
  const { min, max, step } = config.value
  let a = angle
  if (a < -135) a = -135
  if (a > 135) a = 135
  const raw = min + ((a + 135) / 270) * (max - min)
  return Math.round(raw / step) * step
}

function onPointerDown(e: PointerEvent) {
  dragging.value = true
  updateFromPointer(e)
  ;(e.target as Element).setPointerCapture?.(e.pointerId)
}

function onPointerMove(e: PointerEvent) {
  if (dragging.value) updateFromPointer(e)
}

function onPointerUp() {
  dragging.value = false
}

function updateFromPointer(e: PointerEvent) {
  const svg = e.currentTarget as SVGSVGElement
  const rect = svg.getBoundingClientRect()
  const cx = rect.left + rect.width / 2
  const cy = rect.top + rect.height / 2
  const angle = (Math.atan2(e.clientY - cy, e.clientX - cx) * 180) / Math.PI
  value.value = valueFromAngle(angle)
}

function adjust(delta: number) {
  const { min, max, step } = config.value
  const next = value.value + delta * step
  value.value = Math.min(max, Math.max(min, next))
}

function save() {
  if (type.value === 'steps') health.stepsGoal = value.value
  else health.activeGoalMinutes = value.value
  toast(`目标已保存：${value.value} ${config.value.unit} ✓`)
  setTimeout(() => goBack(router, '/steps'), 600)
}

// 刻度：-135°、-67.5°、0°、67.5°、135° 五个刻度 + 两端文字
const ticks = computed(() => {
  const { min, max } = config.value
  return [-135, -67.5, 0, 67.5, 135].map((a) => {
    const val = Math.round(min + ((a + 135) / 270) * (max - min))
    return { angle: a, label: String(val) }
  })
})

function polar(angle: number, r: number) {
  const rad = (angle * Math.PI) / 180
  return { x: 171 + r * Math.cos(rad), y: 171 + r * Math.sin(rad) }
}
</script>

<template>
  <div class="sg">
    <!-- 深绿头部（0-132） -->
    <div class="sg-head">
      <StatusBar light />
      <NavBar light title="设定目标" @back="goBack(router, '/steps')" />
    </div>

    <!-- 插画（对照设计稿 Group 7 @(95,194) 200x153） -->
    <div class="illus">
      <span class="illus-emoji">{{ config.emoji }}</span>
    </div>

    <!-- 说明文字 -->
    <p class="desc">{{ config.desc }}</p>

    <!-- 转盘（对照设计稿 Oval @(24,343) 342x342） -->
    <div class="dial-wrap">
      <svg
        class="dial"
        viewBox="0 0 342 342"
        @pointerdown="onPointerDown"
        @pointermove="onPointerMove"
        @pointerup="onPointerUp"
        @pointercancel="onPointerUp"
      >
        <!-- 同心灰环 -->
        <circle cx="171" cy="171" r="166" fill="none" stroke="#d7d7d7" stroke-width="10" />
        <circle cx="171" cy="171" r="152" fill="none" stroke="#e3e3e3" stroke-width="2" />

        <!-- 刻度与文字 -->
        <template v-for="t in ticks" :key="t.angle">
          <line
            :x1="polar(t.angle, 138).x"
            :y1="polar(t.angle, 138).y"
            :x2="polar(t.angle, 148).x"
            :y2="polar(t.angle, 148).y"
            stroke="#2a6049"
            stroke-width="2"
            stroke-linecap="round"
          />
          <text
            class="num"
            :x="polar(t.angle, 122).x"
            :y="polar(t.angle, 122).y + 4"
            text-anchor="middle"
            font-size="12"
            fill="#bfc0c0"
          >
            {{ t.label }}
          </text>
        </template>

        <!-- 指针（旋转）：0° 指向右侧，与刻度角度一致 -->
        <g :transform="`rotate(${pointerAngle} 171 171)`">
          <line
            x1="171"
            y1="171"
            x2="256"
            y2="171"
            stroke="#2a6049"
            stroke-width="6"
            stroke-linecap="round"
          />
          <circle cx="258" cy="171" r="9" fill="#2a6049" />
          <circle cx="258" cy="171" r="3" fill="#ffffff" />
        </g>
      </svg>

      <!-- 中心数值 -->
      <div class="dial-center">
        <span class="num dial-value">{{ value }}</span>
        <span class="dial-unit">{{ config.unit }}</span>
      </div>

      <!-- 加减按钮 -->
      <button class="adj minus" @click="adjust(-1)">−</button>
      <button class="adj plus" @click="adjust(1)">＋</button>
    </div>

    <button class="btn-primary save-btn" @click="save">保存</button>
  </div>
</template>

<style scoped>
.sg {
  min-height: 100%;
  background: var(--white);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-bottom: 28px;
}

.sg-head {
  width: 100%;
  background: var(--primary);
  padding-bottom: 12px;
}

.illus {
  margin-top: 24px;
  width: 200px;
  height: 153px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.illus-emoji {
  font-size: 88px;
  filter: drop-shadow(0 6px 12px rgba(20, 12, 7, 0.15));
}

.desc {
  margin-top: 8px;
  max-width: 303px;
  text-align: center;
  font-size: var(--fs-body2);
  line-height: var(--lh-body2);
  color: var(--neutral-2);
}

.dial-wrap {
  position: relative;
  margin-top: 16px;
  width: 342px;
  height: 342px;
}

.dial {
  width: 100%;
  height: 100%;
  touch-action: none;
  cursor: grab;
}

.dial:active {
  cursor: grabbing;
}

.dial-center {
  position: absolute;
  left: 50%;
  top: 42%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  pointer-events: none;
}

.dial-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--primary);
  line-height: 1.1;
}

.dial-unit {
  font-size: var(--fs-body2);
  color: var(--neutral-2);
}

.adj {
  position: absolute;
  bottom: 30px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--primary-2);
  color: var(--primary);
  font-size: 22px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.1s ease;
}

.adj:active {
  transform: scale(0.92);
}

.adj.minus {
  left: 44px;
}
.adj.plus {
  right: 44px;
}

.save-btn {
  margin-top: 20px;
  width: calc(100% - 48px);
  max-width: 342px;
}
</style>
