<script setup lang="ts">
// 模块：帮助与反馈 —— 常见问题、反馈入口和联系方式。
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { goBack } from '../utils/navigation'
import { toast } from '../utils/toast'

const router = useRouter()
const feedback = ref('')

const faqs = [
  {
    q: '怎么记录食物和运动？',
    a: '在「发现」页选择食物并添加到对应餐次；在「运动」页点「记录运动」，可选择类型、日期、时长和热量。历史记录支持编辑和删除。',
  },
  {
    q: '我的数据存在哪里？',
    a: '账号、健康数据、食物和运动记录都保存在后端 MySQL 数据库中，登录后会自动同步，换设备也不会丢。',
  },
  {
    q: '怎么修改手机号、密码或昵称？',
    a: '进入「我的 → 设置 → 账户与安全」可以修改昵称、手机号和邮箱；修改密码在「设置 → 更改密码」。',
  },
  {
    q: '忘记密码怎么办？',
    a: '在登录页点「忘记密码」，输入手机号并验证验证码后即可重置密码，重置成功会自动登录。',
  },
  {
    q: '怎么注销账户？',
    a: '进入「设置 → 账户与安全 → 注销账户」，按提示输入「注销」确认。注销会删除全部健康数据，且无法恢复。',
  },
  {
    q: '设备、通知为什么还是演示状态？',
    a: '当前版本是 Web 原型，真实手环/体脂秤同步和系统推送需要接入 HealthKit、Google Fit 或蓝牙设备，后续版本会补齐。',
  },
]

function openFeedback() {
  window.open('https://github.com/sunwh6088-maker/healthcare/issues/new', '_blank')
}

function copyEmail() {
  const email = 'support@healthcare.app'
  navigator.clipboard
    ?.writeText(email)
    .then(() => toast(`反馈邮箱已复制：${email}`))
    .catch(() => toast(`反馈邮箱：${email}`))
}
</script>

<template>
  <div class="help">
    <StatusBar />
    <NavBar title="帮助与反馈" @back="goBack(router, '/profile')" />

    <h3 class="section-title mt-16">常见问题</h3>
    <div class="faq-list mt-12">
      <details v-for="item in faqs" :key="item.q" class="faq card">
        <summary>{{ item.q }}</summary>
        <p>{{ item.a }}</p>
      </details>
    </div>

    <h3 class="section-title mt-20">意见反馈</h3>
    <div class="feedback card mt-12">
      <textarea
        v-model="feedback"
        class="feedback-input"
        rows="4"
        placeholder="遇到问题或有建议，可以写在这里，然后到 GitHub 提交给我们"
      ></textarea>
      <button class="btn-primary mt-12" @click="openFeedback">在 GitHub 提交反馈</button>
      <button class="btn-ghost-add mt-12" @click="copyEmail">复制反馈邮箱</button>
    </div>

    <p class="help-tip">健康数据属于敏感信息，请勿在反馈中填写手机号、密码等隐私内容。</p>
  </div>
</template>

<style scoped>
.help {
  padding: 0 24px 24px;
}

.faq-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.faq {
  padding: 14px 16px;
}

.faq summary {
  cursor: pointer;
  font-size: var(--fs-headline3);
  font-weight: 600;
  list-style: none;
}

.faq summary::-webkit-details-marker {
  display: none;
}

.faq summary::after {
  content: '＋';
  float: right;
  color: var(--neutral-2);
}

.faq[open] summary::after {
  content: '－';
}

.faq p {
  margin-top: 10px;
  font-size: var(--fs-body2);
  line-height: 1.7;
  color: var(--neutral-2);
}

.feedback {
  padding: 16px;
}

.feedback-input {
  width: 100%;
  resize: vertical;
  border-radius: var(--radius-md);
  background: #f7f9f8;
  padding: 12px 14px;
  font-size: var(--fs-body2);
  line-height: 1.6;
  color: var(--neutral);
}

.help-tip {
  margin-top: 16px;
  font-size: var(--fs-caption);
  line-height: 1.6;
  color: var(--neutral-2);
  text-align: center;
}
</style>
