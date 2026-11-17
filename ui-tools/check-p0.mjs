import puppeteer from 'puppeteer-core'

const EDGE = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
const ROOT = 'http://localhost:5174/'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const browser = await puppeteer.launch({
  executablePath: EDGE,
  headless: true,
  args: ['--no-sandbox', '--disable-gpu'],
})
const ctx = await browser.createBrowserContext()
const page = await ctx.newPage()
await page.setViewport({ width: 390, height: 844 })
const errors = []
page.on('pageerror', (e) => errors.push(e.message))
page.on('console', (m) => {
  if (m.type() === 'error') errors.push(m.text())
})
let seq = 0
const go = async (route) => {
  await page.goto(`${ROOT}?t=${++seq}#${route}`, { waitUntil: 'load' })
  await sleep(450)
}
const state = () =>
  page.evaluate(() => JSON.parse(localStorage.getItem('healthcare-app-state') || '{}'))
const txt = (sel) => page.evaluate((s) => document.querySelector(s)?.textContent?.trim(), sel)
const clickSeg = async (label) => {
  await page.evaluate((l) => {
    const b = [...document.querySelectorAll('.segmented .seg')].find(
      (x) => x.textContent.trim() === l,
    )
    b?.click()
  }, label)
  await sleep(300)
}

await go('/home')
await page.evaluate(() => localStorage.removeItem('healthcare-app-state'))
await page.reload({ waitUntil: 'load' })
await sleep(500)

console.log('=== 用户名/首页 ===')
console.log(
  '首页:',
  await txt('.greeting .name'),
  '| 食物:',
  await txt('.stat-card .s-value'),
  '| 水:',
  await txt('.water-card .s-value'),
)
await go('/profile')
console.log(
  '我的:',
  await txt('.head .name'),
  '| 体重:',
  await txt('.stat:nth-child(1) .s-value'),
  '| 年龄:',
  await txt('.stat:nth-child(2) .s-value'),
  '| 身高:',
  await txt('.stat:nth-child(3) .s-value'),
  '| 食物:',
  await txt('.c-card .c-value'),
)
await go('/account-security')
console.log('账户:', await txt('.hero-info h2'))

console.log('=== 食物联动 ===')
await go('/search-food')
await page.evaluate(() => document.querySelector('.food-row')?.click())
await sleep(400)
await page.evaluate(() => document.querySelector('.picked-add')?.click())
await sleep(300)
console.log('添加后 foodKcal:', (await state()).foodKcal)
await go('/food')
console.log('食物页总计:', await txt('.donut .total'))
await go('/home')
console.log('首页食物卡:', await txt('.stat-card .s-value'))

console.log('=== 运动联动 ===')
await go('/exercise')
await page.evaluate(() => document.querySelector('.log-btn')?.click())
await sleep(400)
const stEx = await state()
console.log(
  'activeMinutes:',
  stEx.activeMinutes,
  '| exerciseCount:',
  stEx.exerciseCount,
  '| 瑜伽总时长:',
  await txt('.ov-value'),
)
await go('/home')
console.log('首页运动:', await txt('.stat-card:nth-child(4) .s-value'))
await go('/goal-tracking')
console.log('目标跟踪:', await txt('.yoga-kcal'), '| 进度:', await txt('.yoga-pct'))
await go('/exercise-time')
console.log('运动时间:', await txt('.big-num'))
await page.screenshot({ path: 'C:/Users/21693/Projects/ui-tools/check7-goal.png' })

console.log('=== 历史筛选 ===')
await go('/history')
const histAll = await page.evaluate(() => ({
  count: document.querySelectorAll('.record').length,
  summary: [...document.querySelectorAll('.s-value')].map((e) => e.textContent),
}))
await clickSeg('星期')
const histWeek = await page.evaluate(() => ({
  count: document.querySelectorAll('.record').length,
  summary: [...document.querySelectorAll('.s-value')].map((e) => e.textContent),
}))
await clickSeg('月份')
const histMonth = await page.evaluate(() => ({
  count: document.querySelectorAll('.record').length,
  summary: [...document.querySelectorAll('.s-value')].map((e) => e.textContent),
}))
console.log('全部:', JSON.stringify(histAll), '| 星期:', JSON.stringify(histWeek), '| 月份:', JSON.stringify(histMonth))
await page.screenshot({ path: 'C:/Users/21693/Projects/ui-tools/check7-history-week.png' })

console.log('=== 步骤筛选 ===')
await go('/steps')
const readSteps = () =>
  page.evaluate(() => ({
    big: document.querySelector('.chart .big')?.textContent,
    axis: [...document.querySelectorAll('.axis span')].map((e) => e.textContent),
    stats: [...document.querySelectorAll('.s-value')].map((e) => e.textContent),
    bars: document.querySelectorAll('.bars .bar').length,
  }))
const stepDay = await readSteps()
await clickSeg('周')
const stepWeek = await readSteps()
await clickSeg('月')
const stepMonth = await readSteps()
console.log('日:', JSON.stringify(stepDay))
console.log('周:', JSON.stringify(stepWeek))
console.log('月:', JSON.stringify(stepMonth))
await clickSeg('周')
await page.screenshot({ path: 'C:/Users/21693/Projects/ui-tools/check7-steps-week.png' })

console.log('=== 睡眠时间 ===')
await go('/sleep')
console.log(
  '就寝/醒来:',
  await page.evaluate(() => [...document.querySelectorAll('.bw-value')].map((e) => e.textContent)),
  '| 总时长:',
  await txt('.sleep-time .big'),
)

console.log('=== 体重单位/性别 ===')
const ctx2 = await browser.createBrowserContext()
const p2 = await ctx2.newPage()
await p2.setViewport({ width: 390, height: 844 })
await p2.goto(`${ROOT}?t=w1#/weight`, { waitUntil: 'load' })
await sleep(450)
await p2.evaluate(() => localStorage.removeItem('healthcare-app-state'))
await p2.reload({ waitUntil: 'load' })
await sleep(450)
const wKg = await p2.evaluate(() => document.querySelector('.dial-center')?.textContent)
await p2.evaluate(() => {
  const b = [...document.querySelectorAll('.segmented .seg')].find(
    (x) => x.textContent.trim() === '磅',
  )
  b?.click()
})
await sleep(300)
const wLb = await p2.evaluate(() => document.querySelector('.dial-center')?.textContent)
await p2.evaluate(() => document.querySelector('.plus')?.click())
await sleep(150)
const wLbPlus = await p2.evaluate(() => document.querySelector('.dial-center')?.textContent)
await p2.evaluate(() => {
  const b = [...document.querySelectorAll('button')].find((x) => x.textContent.includes('完成'))
  b?.click()
})
await sleep(500)
const wState = await p2.evaluate(() =>
  JSON.parse(localStorage.getItem('healthcare-app-state') || '{}'),
)
console.log('公斤:', wKg, '| 磅:', wLb, '| +1磅:', wLbPlus, '| 保存 weightKg:', wState.weightKg)
await p2.goto(`${ROOT}?t=w2#/gender`, { waitUntil: 'load' })
await sleep(450)
await p2.evaluate(() => document.querySelectorAll('.gender-card')[1]?.click())
await sleep(200)
await p2.evaluate(() => {
  const b = [...document.querySelectorAll('button')].find((x) => x.textContent.includes('继续'))
  b?.click()
})
await sleep(500)
const gState = await p2.evaluate(() =>
  JSON.parse(localStorage.getItem('healthcare-app-state') || '{}'),
)
console.log('选择性别后 gender:', gState.gender)
await ctx2.close()

console.log('errors:', errors.length ? errors : 'none')
await browser.close()
