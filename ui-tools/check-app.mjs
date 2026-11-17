import puppeteer from 'puppeteer-core'

const EDGE = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
const BASE = 'http://localhost:5174/#'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const errors = []

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

async function go(path) {
  await page.goto(BASE + path, { waitUntil: 'load' })
  await sleep(450)
}
const readState = () =>
  page.evaluate(() => JSON.parse(localStorage.getItem('healthcare-app-state') || '{}'))
const clickText = (text) =>
  page.evaluate((t) => {
    const el = [...document.querySelectorAll('button')].find(
      (b) => b.textContent.trim() === t,
    )
    if (!el) throw new Error('button not found: ' + t)
    el.click()
  }, text)

// ---------- 1. 全路由冒烟 ----------
const routes = [
  '/', '/onboarding', '/login', '/verify', '/gender', '/weight',
  '/home', '/discover', '/devices', '/profile',
  '/food', '/sleep', '/water', '/steps', '/exercise', '/exercise/calendar',
  '/exercise-time', '/goal-tracking', '/set-goal?type=steps', '/set-goal?type=active',
  '/search-food', '/add-food', '/history', '/settings', '/account-security',
  '/forgot-password', '/change-password', '/not-a-real-route',
]
console.log('=== 路由冒烟 ===')
for (const r of routes) {
  await go(r)
  const chars = await page.evaluate(
    () => (document.querySelector('#app')?.innerText || '').length,
  )
  console.log(`${r.padEnd(24)} ${chars > 0 ? 'OK' : 'EMPTY'} (${chars})`)
}

// ---------- 2. 饮水 → 首页联动 ----------
await page.evaluate(() => localStorage.removeItem('healthcare-app-state'))
await go('/water')
await clickText('保存')
await sleep(400)
const water = await readState()
await go('/home')
const homeWater = await page.evaluate(
  () => document.querySelector('.water-card')?.innerText.replace(/\s+/g, ' '),
)
console.log('\n=== 饮水联动 ===')
console.log('保存后 waterMl =', water.waterMl, '| 首页水卡 =', JSON.stringify(homeWater))

// ---------- 3. 闹钟开关 + 刷新持久化 ----------
await go('/sleep')
const alarmBefore = await page.evaluate(
  () => document.querySelector('.alarm-row button')?.getAttribute('aria-checked'),
)
await page.evaluate(() => document.querySelector('.alarm-row button')?.click())
await sleep(400)
const alarmAfter = await page.evaluate(
  () => document.querySelector('.alarm-row button')?.getAttribute('aria-checked'),
)
await page.reload({ waitUntil: 'load' })
await sleep(500)
const alarmReload = await page.evaluate(
  () => document.querySelector('.alarm-row button')?.getAttribute('aria-checked'),
)
console.log('\n=== 闹钟 ===')
console.log(`开关 ${alarmBefore} -> ${alarmAfter} | 刷新后 ${alarmReload}`)

// ---------- 4. 步数目标 → 步骤页联动 ----------
await go('/set-goal?type=steps')
for (let i = 0; i < 3; i++) await clickText('＋')
const stepDial = await page.evaluate(() => document.querySelector('.dial-value')?.textContent)
await clickText('保存')
await sleep(900)
const stepState = await readState()
await go('/steps')
const stepText = await page.evaluate(
  () => document.querySelector('.chart .big')?.textContent,
)
console.log('\n=== 步数目标 ===')
console.log(`转盘 ${stepDial} | 保存 stepsGoal=${stepState.stepsGoal} | 步骤页 ${stepText}`)

// ---------- 5. 活动目标 → 运动时间联动 ----------
await go('/set-goal?type=active')
for (let i = 0; i < 4; i++) await clickText('＋')
const activeDial = await page.evaluate(() => document.querySelector('.dial-value')?.textContent)
await clickText('保存')
await sleep(900)
const activeState = await readState()
await go('/exercise-time')
const activeText = await page.evaluate(
  () => document.querySelector('.big-num')?.textContent,
)
console.log('\n=== 活动目标 ===')
console.log(`转盘 ${activeDial} | 保存 activeGoalMinutes=${activeState.activeGoalMinutes} | 运动时间页 ${activeText}`)

// ---------- 6. 分段选择器 ----------
await go('/discover')
const segBefore = await page.evaluate(
  () => document.querySelector('.segmented .seg.active')?.textContent,
)
await page.evaluate(() => {
  const el = [...document.querySelectorAll('.segmented .seg')].find(
    (s) => s.textContent.trim() === '最近',
  )
  el?.click()
})
await sleep(250)
const segAfter = await page.evaluate(
  () => document.querySelector('.segmented .seg.active')?.textContent,
)
const firstFood = await page.evaluate(
  () => document.querySelector('.food-card .f-name')?.textContent,
)
console.log('\n=== 分段选择器 ===')
console.log(`激活项 ${segBefore} -> ${segAfter} | 最近列表首项 ${firstFood}`)

// ---------- 7. 404 兜底 ----------
await go('/not-a-real-route')
const hash = await page.evaluate(() => location.hash)
console.log('\n=== 404 ===')
console.log('重定向后 hash =', hash)

// ---------- 8. 切页滚动复位 ----------
await go('/sleep')
await page.evaluate(() => {
  const s = document.querySelector('.phone-screen')
  if (s) s.scrollTop = 600
})
const scrolled = await page.evaluate(
  () => document.querySelector('.phone-screen')?.scrollTop,
)
await page.evaluate(() => { location.hash = '#/home' })
await sleep(600)
const reset = await page.evaluate(
  () => document.querySelector('.phone-screen')?.scrollTop,
)
console.log('\n=== 滚动复位 ===')
console.log(`scrollTop ${scrolled} -> ${reset}`)

// ---------- 9. 截图（重置数据后） ----------
await page.evaluate(() => localStorage.removeItem('healthcare-app-state'))
const shots = [
  ['/home', 'check-home.png'],
  ['/water', 'check-water.png'],
  ['/sleep', 'check-sleep.png'],
  ['/set-goal?type=steps', 'check-set-goal.png'],
  ['/exercise-time', 'check-exercise-time.png'],
  ['/discover', 'check-discover.png'],
  ['/settings', 'check-settings.png'],
]
for (const [route, file] of shots) {
  await go(route)
  await page.screenshot({ path: 'C:/Users/21693/Projects/ui-tools/' + file })
}
console.log('\n=== 截图已保存到 ui-tools/check-*.png ===')

console.log('\n=== 运行时错误 ===')
console.log(errors.length ? errors : 'none')
await browser.close()
