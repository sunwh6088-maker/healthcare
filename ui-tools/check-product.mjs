// 产品级端到端检查：登录 → 改手机号（验证码）→ 头像 → 睡眠编辑 → 法律页 → 数据导出
import puppeteer from 'puppeteer-core'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'

const EDGE = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
const BASE = 'http://localhost:5173/#'
const PHONE = '135' + String(Date.now()).slice(-8)
const NEW_PHONE = '139' + String(Date.now()).slice(-8)
const AVATAR = 'C:/Users/21693/Projects/healthcare-app/public/icon-192.png'
const DOWNLOAD_DIR = fs.mkdtempSync(path.join(os.tmpdir(), 'healthcare-dl-'))
const errors = []
const results = []

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const browser = await puppeteer.launch({
  executablePath: EDGE,
  headless: true,
  args: ['--no-sandbox', '--disable-gpu'],
})
const page = await browser.newPage()
await browser
  .defaultBrowserContext()
  .overridePermissions('http://localhost:5173', ['notifications'])
await page.setViewport({ width: 500, height: 1000, deviceScaleFactor: 1 })
page.on('console', (m) => {
  if (m.type() === 'error') errors.push('console: ' + m.text())
})
page.on('pageerror', (e) => errors.push('pageerror: ' + e.message))

function check(name, ok, detail = '') {
  results.push({ name, ok, detail })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? ' — ' + detail : ''}`)
}

async function go(hash) {
  await page.goto(BASE + hash, { waitUntil: 'load' })
  await sleep(500)
}

async function clickText(text) {
  const clicked = await page.evaluate((t) => {
    const candidates = [...document.querySelectorAll('button, .menu-row, .row, .green-link, a')]
    const el =
      candidates.find((node) => (node.textContent || '').trim() === t) ||
      candidates.find((node) => (node.textContent || '').includes(t))
    if (!el) return false
    el.click()
    return true
  }, text)
  if (!clicked) throw new Error('元素未找到: ' + text)
  await sleep(400)
}

async function setInput(selector, value) {
  await page.$eval(
    selector,
    (el, v) => {
      el.value = v
      el.dispatchEvent(new Event('input', { bubbles: true }))
    },
    value,
  )
}

try {
  // 1. 登录（勾选协议 + 验证码）
  await go('/login')
  await page.$eval('input[type=checkbox]', (el) => el.click())
  await setInput('input[type=tel]', PHONE)
  await clickText('继续')
  await page.waitForFunction(() => location.hash.startsWith('#/verify'), { timeout: 8000 })
  await page.waitForFunction(
    () => /^\d{6}$/.test((document.querySelector('.dev-code')?.textContent || '').replace(/\D/g, '')),
    { timeout: 8000 },
  )
  await clickText('验证')
  await page.waitForFunction(() => /#\/(home|gender)/.test(location.hash), { timeout: 10000 })
  if (await page.evaluate(() => location.hash.startsWith('#/gender'))) {
    await clickText('继续')
    await page.waitForFunction(() => location.hash.startsWith('#/weight'), { timeout: 8000 })
    await clickText('完成')
    await page.waitForFunction(() => location.hash.startsWith('#/body-info'), { timeout: 8000 })
    await clickText('稍后设置')
  }
  await page.waitForFunction(() => location.hash.startsWith('#/home'), { timeout: 10000 })
  check('验证码登录（含新用户引导）', true)

  // 2. 修改手机号：发送验证码 → 验证 → 更新
  await go('/account-security')
  await clickText('手机号')
  await page.waitForSelector('input[type=tel]', { timeout: 5000 })
  await setInput('input[type=tel]', NEW_PHONE)
  await clickText('发送验证码')
  await page.waitForFunction(
    () => (document.body.innerText || '').includes('验证新手机号'),
    { timeout: 8000 },
  )
  await page.waitForFunction(
    () => {
      const input = document.querySelector('input[placeholder="请输入 6 位验证码"]')
      return input && /^\d{6}$/.test(input.value)
    },
    { timeout: 5000 },
  )
  // 无密码账号需要当前手机号验证码完成二次验证
  await clickText('发送当前手机号验证码')
  await page.waitForFunction(
    () => {
      const input = document.querySelector('input[placeholder="请输入当前手机号验证码"]')
      return input && /^\d{6}$/.test(input.value)
    },
    { timeout: 8000 },
  )
  await clickText('确认修改')
  await page.waitForFunction(
    (last4) => (document.body.innerText || '').includes('139 **** ' + last4),
    { timeout: 8000 },
    NEW_PHONE.slice(-4),
  )
  check('修改手机号短信二次验证', true, NEW_PHONE)

  // 3. 头像上传与展示
  await go('/profile')
  const fileInput = await page.$('input[type=file]')
  await fileInput.uploadFile(AVATAR)
  await page.waitForSelector('img[alt="头像"]', { timeout: 10000 })
  const avatarSrc = await page.$eval('img[alt="头像"]', (el) => el.getAttribute('src'))
  check('头像上传与展示', avatarSrc.includes('/api/me/avatar?v='), avatarSrc)

  // 3.5 绑定 IoT 设备并模拟上报
  await go('/devices')
  await clickText('绑定')
  await page.waitForFunction(
    () => (document.body.innerText || '').includes('绑定设备'),
    { timeout: 5000 },
  )
  const deviceInputs = await page.$$('.sheet-input')
  await deviceInputs[0].evaluate((el, value) => {
    el.value = value
    el.dispatchEvent(new Event('input', { bubbles: true }))
  }, `pk-e2e-${Date.now()}`)
  await deviceInputs[1].evaluate((el) => {
    el.value = 'watch-e2e'
    el.dispatchEvent(new Event('input', { bubbles: true }))
  })
  await deviceInputs[2].evaluate((el) => {
    el.value = '测试手表'
    el.dispatchEvent(new Event('input', { bubbles: true }))
  })
  await clickText('绑定设备')
  await page.waitForFunction(
    () => (document.body.innerText || '').includes('测试手表'),
    { timeout: 8000 },
  )
  await clickText('模拟上报')
  await page.waitForFunction(
    () => {
      const state = JSON.parse(localStorage.getItem('healthcare-app-state') || '{}')
      return typeof state.steps === 'number' && state.steps > 0
    },
    { timeout: 12000 },
  )
  check('IoT 设备绑定与模拟上报', true)

  // 4. 睡眠时间编辑
  await go('/sleep')
  await page.evaluate(() => document.querySelector('.bw')?.click())
  await page.waitForFunction(
    () => (document.body.innerText || '').includes('调整睡眠时间'),
    { timeout: 5000 },
  )
  const timeInputs = await page.$$('input[type=time]')
  await timeInputs[0].evaluate((el) => {
    el.value = '22:30'
    el.dispatchEvent(new Event('input', { bubbles: true }))
  })
  await timeInputs[1].evaluate((el) => {
    el.value = '06:30'
    el.dispatchEvent(new Event('input', { bubbles: true }))
  })
  await clickText('保存')
  await page.waitForFunction(
    () => {
      const text = document.body.innerText || ''
      return text.includes('22:30') && text.includes('8小时0分')
    },
    { timeout: 5000 },
  )
  check('睡眠就寝/醒来时间编辑', true)

  // 5. 用户协议与隐私政策
  await go('/terms')
  const termsText = await page.evaluate(() => document.body.innerText || '')
  check('用户协议可访问', termsText.includes('用户协议') && termsText.includes('服务说明'))
  await go('/privacy')
  const privacyText = await page.evaluate(() => document.body.innerText || '')
  check(
    '隐私政策可访问',
    privacyText.includes('隐私政策') && privacyText.includes('我们收集的信息'),
  )

  // 6. 数据导出（设置页触发下载）
  const cdp = await page.createCDPSession()
  await cdp.send('Page.setDownloadBehavior', { behavior: 'allow', downloadPath: DOWNLOAD_DIR })
  await go('/settings')
  const exportResponse = page.waitForResponse(
    (res) => res.url().includes('/api/me/export') && res.status() === 200,
    { timeout: 10000 },
  )
  await clickText('导出我的数据')
  await exportResponse
  let exportedFile = ''
  for (let i = 0; i < 20 && !exportedFile; i++) {
    await sleep(250)
    exportedFile = fs.readdirSync(DOWNLOAD_DIR).find((name) => name.endsWith('.json')) || ''
  }
  const exported = exportedFile
    ? JSON.parse(fs.readFileSync(path.join(DOWNLOAD_DIR, exportedFile), 'utf8'))
    : null
  check(
    '数据导出 JSON',
    !!exported && !!exported.account && Array.isArray(exported.foodLogs),
    exportedFile,
  )

  // 7. Web Push：开启通知、确认订阅、发送测试推送
  await page.evaluate(() => {
    document.querySelector('button[role="switch"][aria-label="消息通知"]')?.click()
  })
  await page.waitForFunction(
    () => localStorage.getItem('healthcare-push-enabled') === '1',
    { timeout: 20000 },
  )
  const pushEndpoint = await page.evaluate(async () => {
    const registration = await navigator.serviceWorker.ready
    const subscription = await registration.pushManager.getSubscription()
    return subscription ? subscription.endpoint : null
  })
  check('Web Push 订阅创建', !!pushEndpoint, pushEndpoint ? pushEndpoint.slice(0, 48) + '…' : '')

  const testPushResponse = page.waitForResponse(
    (res) => res.url().includes('/api/push/test') && res.status() === 200,
    { timeout: 20000 },
  )
  await clickText('发送测试通知')
  const pushBody = await (await testPushResponse).json()
  check(
    '测试推送请求',
    pushBody.code === 0 || pushBody.code === 200,
    `推送设备数 ${pushBody.data}`,
  )

  await page.screenshot({ path: 'C:/Users/21693/Projects/ui-tools/product-check.png' })
} catch (error) {
  check('流程执行', false, error instanceof Error ? error.message : String(error))
}

const failed = results.filter((r) => !r.ok)
check('零控制台错误', errors.length === 0, errors.join(' | ').slice(0, 300))

await browser.close()
console.log(`\n${results.length - failed.length}/${results.length} 项通过`)
if (failed.length || errors.length) process.exit(1)
