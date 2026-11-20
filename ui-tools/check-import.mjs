// 端到端检查：登录 → 生成导入令牌 → 快捷指令上报 → 步骤页展示 → 设备重命名/解绑
import puppeteer from 'puppeteer-core'

const EDGE = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
const BASE = 'http://localhost:5173/#'
const PHONE = '136' + String(Date.now()).slice(-8)
const errors = []
const results = []

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const browser = await puppeteer.launch({
  executablePath: EDGE,
  headless: true,
  args: ['--no-sandbox', '--disable-gpu'],
})
const page = await browser.newPage()
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

function todayISO() {
  const date = new Date()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

try {
  // 1. 验证码登录（新用户完成引导）
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

  // 2. 生成导入令牌
  await go('/health-import')
  await page.waitForFunction(
    () => (document.body.innerText || '').includes('尚未生成'),
    { timeout: 8000 },
  )
  await clickText('生成令牌')
  await page.waitForFunction(
    () => document.querySelectorAll('.info-value').length >= 2,
    { timeout: 8000 },
  )
  const token = await page.evaluate(() => {
    const values = [...document.querySelectorAll('.info-value')]
    return values.length ? values[values.length - 1].textContent.trim() : ''
  })
  check('生成导入令牌', token.length >= 20, token.slice(0, 8) + '…')

  // 3. 模拟快捷指令上报（走 Vite 代理到后端）
  const importResponse = await fetch('http://localhost:5173/api/import/health', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Import-Token': token,
    },
    body: JSON.stringify({
      date: todayISO(),
      steps: 9876,
      sleepMinutes: 465,
      activeMinutes: 30,
      weightKg: 62.5,
      battery: 88,
    }),
  })
  const importBody = await importResponse.json()
  check(
    '快捷指令上报',
    importResponse.status === 200 && importBody.data?.steps === 9876,
    JSON.stringify(importBody.data),
  )

  // 4. 错误令牌被拒绝
  const badResponse = await fetch('http://localhost:5173/api/import/health', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Import-Token': 'bad-token' },
    body: JSON.stringify({ steps: 1 }),
  })
  check('错误令牌被拒绝', badResponse.status === 401, `HTTP ${badResponse.status}`)

  // 5. 步骤页展示导入的步数（日视图大数字）
  await go('/steps')
  await page.waitForFunction(
    () => (document.querySelector('.chart .big')?.textContent || '').includes('9,876'),
    { timeout: 10000 },
  )
  const litBars = await page.evaluate(() => document.querySelectorAll('.bar.on').length)
  check('步骤页展示导入步数', litBars >= 1, `点亮柱 ${litBars}`)

  // 6. 绑定设备并修改名称
  const deviceName = 'dev' + String(Date.now()).slice(-6)
  const bindResult = await page.evaluate(async (name) => {
    const res = await fetch('/api/iot/devices', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ productKey: 'pk-e2e', deviceName: name, name: '手表A' }),
    })
    return { status: res.status, body: await res.json() }
  }, deviceName)
  if (bindResult.status !== 200) {
    throw new Error('绑定设备失败: ' + JSON.stringify(bindResult.body))
  }
  await go('/devices')
  await page.waitForFunction(
    () => (document.body.innerText || '').includes('手表A'),
    { timeout: 8000 },
  )
  await clickText('修改名称')
  await page.waitForSelector('.sheet-input', { timeout: 5000 })
  await setInput('.sheet-input', '我的手表')
  await clickText('保存')
  await page.waitForFunction(
    () => (document.body.innerText || '').includes('我的手表'),
    { timeout: 8000 },
  )
  check('设备重命名', true)

  // 7. 解绑设备（底部弹层二次确认）
  await clickText('解绑')
  await page.waitForFunction(
    () => (document.body.innerText || '').includes('解除绑定'),
    { timeout: 5000 },
  )
  await clickText('确认解绑')
  await page.waitForFunction(
    () => !(document.body.innerText || '').includes('我的手表'),
    { timeout: 8000 },
  )
  check('设备解绑二次确认', true)

  await page.screenshot({ path: 'C:/Users/21693/Projects/ui-tools/import-check.png' })
} catch (error) {
  check('流程执行', false, error instanceof Error ? error.message : String(error))
}

const failed = results.filter((r) => !r.ok)
check('零控制台错误', errors.length === 0, errors.join(' | ').slice(0, 300))

await browser.close()
console.log(`\n${results.length - failed.length}/${results.length} 项通过`)
if (failed.length || errors.length) process.exit(1)
