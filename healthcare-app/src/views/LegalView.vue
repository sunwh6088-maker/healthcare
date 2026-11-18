<script setup lang="ts">
// 模块：法律文本 —— 用户协议与隐私政策共用同一版式。
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'

const props = defineProps<{ doc: 'terms' | 'privacy' }>()
const router = useRouter()

interface Section {
  heading: string
  paragraphs: string[]
}

const termsSections: Section[] = [
  {
    heading: '1. 服务说明',
    paragraphs: [
      '健康追踪为你提供健康数据记录、展示与同步服务，包括步数、饮水、睡眠、体重、食物和运动记录等功能。',
      '本服务不构成医疗诊断或治疗建议。如有健康问题，请及时咨询专业医生。',
    ],
  },
  {
    heading: '2. 账号注册与安全',
    paragraphs: [
      '你使用中国大陆手机号注册并登录本服务。请妥善保管手机、验证码和登录密码，不要向他人泄露。',
      '如发现账号异常，请尽快通过「账户与安全」修改密码，或联系我们处理。',
    ],
  },
  {
    heading: '3. 用户行为规范',
    paragraphs: [
      '你不得利用本服务从事违法违规活动，不得干扰或破坏服务正常运行，不得上传含有违法、侵权内容的信息。',
      '你记录的健康数据应真实、合法，因数据不准确造成的后果由你自行承担。',
    ],
  },
  {
    heading: '4. 服务变更与终止',
    paragraphs: [
      '我们可能根据产品发展调整服务内容，并尽可能提前公告。',
      '你可以随时在「账户与安全」中注销账号。注销后，账号及相关健康数据将被删除且无法恢复。',
    ],
  },
  {
    heading: '5. 免责声明',
    paragraphs: [
      '因网络故障、设备问题、不可抗力或第三方服务异常导致的服务中断或数据延迟，我们将在合理范围内协助处理，但不承担由此产生的间接损失。',
      '法律另有规定的，从其规定。',
    ],
  },
  {
    heading: '6. 协议更新与联系',
    paragraphs: [
      '本协议更新后会在应用内公布，继续使用服务即视为同意更新后的协议。',
      '如有疑问，请联系：support@healthcare.example。',
    ],
  },
]

const privacySections: Section[] = [
  {
    heading: '1. 我们收集的信息',
    paragraphs: [
      '账号信息：手机号；你还可以选择填写昵称、邮箱和头像。',
      '健康数据：步数、饮水量、睡眠时间、体重、身高、年龄、性别，以及食物和运动记录。',
      '设备与日志信息：浏览器类型、IP 地址等，仅用于安全防护和故障排查。',
    ],
  },
  {
    heading: '2. 信息的使用目的',
    paragraphs: [
      '用于提供登录、数据记录、跨设备同步和个性化展示等核心功能。',
      '用于验证码发送、异常登录识别、限流等安全保护。',
      '用于改进产品体验。我们不会将你的健康数据用于广告推送。',
    ],
  },
  {
    heading: '3. 存储与保护',
    paragraphs: [
      '数据存储于中国境内的服务器，传输过程使用加密连接。',
      '登录凭证存放在 HttpOnly Cookie 中，服务端仅保存令牌哈希；密码使用 BCrypt 哈希存储。',
      '我们采取访问控制、限流和审计等措施保护数据安全。',
    ],
  },
  {
    heading: '4. 共享与披露',
    paragraphs: [
      '我们不会出售你的个人信息。',
      '仅在以下情形共享必要信息：短信服务商发送验证码；法律法规要求或有权机关依法调取。',
    ],
  },
  {
    heading: '5. 你的权利',
    paragraphs: [
      '你可以在「账户与安全」中查询、更正手机号、昵称和邮箱。',
      '你可以在「设置」中导出全部个人数据（JSON 格式）。',
      '你可以随时注销账号。注销后，账号、健康数据、食物和运动记录将被删除且无法恢复。',
    ],
  },
  {
    heading: '6. Cookie 与未成年人保护',
    paragraphs: [
      '我们仅使用必要的 Cookie 维持登录状态，不使用第三方广告 Cookie。',
      '未满 14 周岁的用户应在监护人陪同下阅读本政策并使用本服务。',
    ],
  },
  {
    heading: '7. 政策更新与联系',
    paragraphs: [
      '本政策更新后会在应用内公布。如对个人信息处理有疑问，请联系：support@healthcare.example。',
    ],
  },
]

const isTerms = computed(() => props.doc === 'terms')
const title = computed(() => (isTerms.value ? '用户协议' : '隐私政策'))
const sections = computed(() => (isTerms.value ? termsSections : privacySections))
</script>

<template>
  <div class="legal">
    <StatusBar />
    <NavBar :title="title" @back="goBack(router)" />

    <div class="card mt-12 intro">
      <p class="updated">更新日期：2026 年 11 月 18 日</p>
      <p>
        {{
          isTerms
            ? '欢迎使用健康追踪。请在使用前仔细阅读本协议，特别是与责任限制相关的条款。'
            : '我们重视你的个人信息保护。本政策说明我们如何收集、使用、存储和保护你的信息。'
        }}
      </p>
    </div>

    <div v-for="section in sections" :key="section.heading" class="card mt-12 section">
      <h3>{{ section.heading }}</h3>
      <p v-for="(paragraph, index) in section.paragraphs" :key="index">{{ paragraph }}</p>
    </div>

    <p class="footer-tip">健康追踪 · 让好习惯看得见</p>
  </div>
</template>

<style scoped>
.legal {
  padding: 0 24px 32px;
}

.intro {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.intro p {
  font-size: var(--fs-body2);
  line-height: 1.7;
  color: var(--neutral-2);
}

.updated {
  color: var(--neutral-2);
  font-size: var(--fs-caption);
}

.section {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section h3 {
  font-size: var(--fs-headline3);
  font-weight: 700;
}

.section p {
  font-size: var(--fs-body2);
  line-height: 1.75;
  color: var(--neutral-2);
}

.footer-tip {
  margin-top: 20px;
  text-align: center;
  font-size: var(--fs-caption);
  color: var(--neutral-2);
}
</style>
