<script setup lang="ts">
// 模块：iPhone 健康数据导入 —— 生成令牌并展示快捷指令配置步骤。
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { api } from '../api'
import { toast } from '../utils/toast'

const router = useRouter()
const hasToken = ref<boolean | null>(null)
const token = ref('')
const generating = ref(false)

const importUrl = computed(() => `${window.location.origin}/api/import/health`)

onMounted(async () => {
  try {
    hasToken.value = (await api.getImportTokenStatus()).hasToken
  } catch {
    // 加载失败时按未生成处理
  }
})

async function generateToken() {
  generating.value = true
  try {
    token.value = (await api.generateImportToken()).token
    hasToken.value = true
    toast('导入令牌已生成')
  } catch (error) {
    toast(error instanceof Error ? error.message : '生成令牌失败')
  } finally {
    generating.value = false
  }
}

async function copy(value: string, label: string) {
  try {
    await navigator.clipboard.writeText(value)
    toast(`${label}已复制`)
  } catch {
    toast('复制失败，请长按文字手动复制')
  }
}
</script>

<template>
  <div class="import-page">
    <StatusBar />
    <NavBar title="iPhone 健康数据导入" @back="goBack(router, '/settings')" />

    <div class="card mt-12 intro">
      <p>
        网页无法直接读取 iPhone 健康数据。用「快捷指令」从健康 App
        读取步数、睡眠和手机电量，再发送到下面的地址。
      </p>

      <div class="info-row">
        <span class="info-label">导入地址</span>
        <code class="info-value">{{ importUrl }}</code>
        <button class="copy-btn" @click="copy(importUrl, '地址')">复制</button>
      </div>

      <div class="info-row">
        <span class="info-label">导入令牌</span>
        <code v-if="token" class="info-value">{{ token }}</code>
        <span v-else class="info-muted">
          {{ hasToken ? '已生成，出于安全不再显示' : '尚未生成' }}
        </span>
        <button class="copy-btn" :disabled="generating" @click="generateToken">
          {{ generating ? '生成中…' : token ? '重新生成' : '生成令牌' }}
        </button>
      </div>

      <p v-if="token" class="warn">令牌只显示这一次，请立即复制到快捷指令；丢失可以重新生成。</p>
    </div>

    <h3 class="section-title mt-20">快捷指令步骤</h3>
    <div class="card mt-12 steps">
      <ol>
        <li>打开 iPhone「快捷指令」，新建一个快捷指令</li>
        <li>添加「查找健康样本」：类型选「步数」，开始日期「今天」，结束日期「现在」，限制关闭</li>
        <li>添加「计算统计信息」：对样本求和，取「数值」，得到今天的步数</li>
        <li>添加「获取电池电量」，得到手机电量</li>
        <li>添加「当前日期」，格式选自定义，输入 <code>yyyy-MM-dd</code></li>
        <li>添加「文本」，粘贴下面模板，把中文占位换成上一步的变量</li>
        <li>
          添加「获取 URL 内容」：URL 填上面的导入地址；方法选 POST；头部加
          <code>Content-Type: application/json</code> 和
          <code>X-Import-Token: 你的令牌</code>；请求体选刚建的文本
        </li>
        <li>运行一次，回到 App 刷新即可看到步数</li>
      </ol>

      <pre>
{
  "date": "日期变量",
  "steps": 步数变量,
  "battery": 电量变量
}</pre>

      <p class="tip">
        睡眠同理：类型选「睡眠分析」，统计时长（分钟）后加入
        <code>sleepMinutes</code>；体重可加 <code>weightKg</code>。
      </p>
    </div>

    <p class="foot-tip">
      当前演示阶段手机和电脑需在同一 Wi-Fi；后端部署到公网后，把地址换成域名即可。
    </p>
  </div>
</template>

<style scoped>
.import-page {
  padding: 0 24px 32px;
}

.intro {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.intro > p {
  font-size: var(--fs-body2);
  line-height: 1.7;
  color: var(--neutral-2);
}

.info-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--neutral-3);
}

.info-label {
  width: 100%;
  font-size: var(--fs-caption);
  color: var(--neutral-2);
}

.info-value {
  flex: 1;
  min-width: 0;
  word-break: break-all;
  font-size: var(--fs-caption);
  color: var(--primary);
}

.info-muted {
  flex: 1;
  font-size: var(--fs-caption);
  color: var(--neutral-2);
}

.copy-btn {
  padding: 6px 12px;
  border-radius: var(--radius-pill);
  background: var(--primary);
  color: var(--white);
  font-size: var(--fs-caption);
  font-weight: 600;
}

.warn {
  font-size: var(--fs-caption);
  line-height: 1.6;
  color: var(--accent);
}

.steps {
  padding: 16px 20px;
}

.steps ol {
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.steps li {
  font-size: var(--fs-body2);
  line-height: 1.7;
  color: var(--neutral-2);
}

.steps code {
  color: var(--primary);
  word-break: break-all;
}

.steps pre {
  margin-top: 12px;
  padding: 12px;
  border-radius: var(--radius-md);
  background: #f4f7f5;
  color: var(--neutral);
  font-size: var(--fs-caption);
  line-height: 1.6;
  overflow-x: auto;
}

.tip {
  margin-top: 10px;
  font-size: var(--fs-caption);
  line-height: 1.6;
  color: var(--neutral-2);
}

.foot-tip {
  margin-top: 16px;
  font-size: var(--fs-caption);
  line-height: 1.6;
  color: var(--neutral-2);
  text-align: center;
}
</style>
