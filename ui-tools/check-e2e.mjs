import puppeteer from 'puppeteer-core'

const EDGE = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
const BASE = 'http://localhost:5175/'
const PHONE = process.argv[2] || '13800003333'
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

const hash = () => page.evaluate(() => location.hash)
const waitHash = (target) =>
  page.waitForFunction((h) => location.hash === h, { timeout: 8000 }, target)
const clickText = async (text) => {
  await page.evaluate((t) => {
    const buttons = [...document.querySelectorAll('button')]
    const el =
      buttons.find((b) => b.textContent.trim() === t) ||
      buttons.find((b) => b.textContent.includes(t))
    if (!el) throw new Error('button not found: ' + t)
    el.click()
  }, text)
  await sleep(300)
}
const setInput = (selector, value) =>
  page.evaluate(
    ({ s, v }) => {
      const input = document.querySelector(s)
      if (!input) throw new Error('input not found: ' + s)
      input.value = v
      input.dispatchEvent(new Event('input', { bubbles: true }))
    },
    { s: selector, v: value },
  )
const apiGet = (path) =>
  page.evaluate(async (p) => {
    const res = await fetch(p, {
      headers: { Authorization: 'Bearer ' + localStorage.getItem('token') },
    })
    return (await res.json()).data
  }, path)
const press = async (digit, times = 1) => {
  for (let i = 0; i < times; i++) {
    await page.evaluate((d) => {
      const b = [...document.querySelectorAll('.kp-key')].find(
        (x) => x.textContent.trim() === d,
      )
      b?.click()
    }, digit)
  }
  await sleep(100)
}

console.log('=== 未登录访问首页 ===')
await page.goto(BASE + '#/home', { waitUntil: 'load' })
await sleep(800)
console.log('hash:', await hash(), '| token:', await page.evaluate(() => localStorage.getItem('token')))

console.log('=== 登录 ===')
await page.goto(BASE + '#/login', { waitUntil: 'load' })
await sleep(400)
await setInput('.phone-num', PHONE)
await clickText('继续')
await waitHash('#/verify')
await sleep(400)
const devCode = await page.evaluate(() => document.querySelector('.dev-code')?.textContent || '')
console.log('verify page dev code:', devCode, '| boxes:', await page.evaluate(() => [...document.querySelectorAll('.code-box')].map((e) => e.textContent.trim()).join('')))
await clickText('验证')
await waitHash('#/gender')
await sleep(500)
console.log('after login hash:', await hash(), '| token saved:', !!(await page.evaluate(() => localStorage.getItem('token'))))

console.log('=== 引导：性别 + 体重 ===')
await page.evaluate(() => document.querySelector('.gender-card')?.click())
await sleep(200)
await clickText('继续')
await waitHash('#/weight')
await clickText('完成')
await waitHash('#/body-info')
await sleep(400)
await page.evaluate(() => {
  const inputs = document.querySelectorAll('.f-input')
  inputs[1].value = '175'
  inputs[1].dispatchEvent(new Event('input', { bubbles: true }))
  inputs[2].value = '28'
  inputs[2].dispatchEvent(new Event('input', { bubbles: true }))
})
await clickText('保存')
await waitHash('#/home')
await sleep(900)
let state = await apiGet('/api/state')
console.log('server state:', {
  gender: state.health.gender,
  weightKg: state.health.weightKg,
  heightCm: state.health.heightCm,
  age: state.health.age,
  steps: state.health.steps,
  waterMl: state.health.waterMl,
  alarms: state.alarms.length,
})

console.log('=== 我的页编辑身体数据 ===')
await page.evaluate(() => { location.hash = '#/profile' })
await sleep(500)
await clickText('编辑')
await waitHash('#/body-info?from=profile')
await sleep(400)
await page.evaluate(() => {
  const inputs = document.querySelectorAll('.f-input')
  inputs[2].value = '30'
  inputs[2].dispatchEvent(new Event('input', { bubbles: true }))
})
await clickText('保存')
await waitHash('#/profile')
await sleep(900)
state = await apiGet('/api/state')
console.log('profile edit age:', state.health.age)

console.log('=== 食物记录落库 ===')
await page.evaluate(() => { location.hash = '#/search-food' })
await sleep(500)
await page.evaluate(() => document.querySelector('.food-row')?.click())
await sleep(500)
await page.evaluate(() => document.querySelector('.picked-add')?.click())
await sleep(1000)
state = await apiGet('/api/state')
const foodSummary = await apiGet('/api/food-logs')
console.log('foodKcal:', state.health.foodKcal, '| logs:', foodSummary.logs.length, '| total:', foodSummary.totalCalories)
await page.evaluate(() => { location.hash = '#/food' })
await sleep(600)
console.log('food page records:', await page.evaluate(() => document.querySelectorAll('.meal-row').length), '| total:', await page.evaluate(() => document.querySelector('.donut .total')?.textContent))

console.log('=== 饮水落库 ===')
await page.evaluate(() => { location.hash = '#/water' })
await sleep(500)
await clickText('保存')
await sleep(1000)
state = await apiGet('/api/state')
console.log('waterMl after save:', state.health.waterMl)

console.log('=== 闹钟落库 ===')
await page.evaluate(() => { location.hash = '#/sleep' })
await sleep(500)
await page.evaluate(() => document.querySelector('.add-top')?.click())
await sleep(400)
await page.evaluate(() => {
  const b = [...document.querySelectorAll('.sheet-actions button')].find((x) =>
    x.textContent.includes('保存'),
  )
  b?.click()
})
await sleep(1000)
state = await apiGet('/api/state')
console.log('alarms:', state.alarms)

console.log('=== 运动记录落库 ===')
await page.evaluate(() => { location.hash = '#/exercise' })
await sleep(500)
await page.evaluate(() => document.querySelector('.log-btn')?.click())
await sleep(1200)
state = await apiGet('/api/state')
const exerciseLogs = await apiGet('/api/exercise-logs')
console.log('activeMinutes:', state.health.activeMinutes, '| exerciseCount:', state.health.exerciseCount, '| logs:', exerciseLogs.length)
await page.evaluate(() => { location.hash = '#/history' })
await sleep(600)
console.log('history records:', await page.evaluate(() => document.querySelectorAll('.record').length))

console.log('=== 账户资料落库 ===')
await page.evaluate(() => { location.hash = '#/account-security' })
await sleep(500)
await page.evaluate(() => {
  const row = [...document.querySelectorAll('.row')].find((r) => r.textContent.includes('邮箱'))
  row?.click()
})
await sleep(400)
await setInput('.sheet-input', 'e2e@example.com')
await page.evaluate(() => {
  const b = [...document.querySelectorAll('.sheet-actions button')].find((x) =>
    x.textContent.includes('保存'),
  )
  b?.click()
})
await sleep(900)
const me = await apiGet('/api/me')
console.log('server me:', { name: me.name, email: me.email, phone: me.phone })
console.log('page email:', await page.evaluate(() => {
  const row = [...document.querySelectorAll('.row')].find((r) => r.textContent.includes('邮箱'))
  return row?.querySelector('.r-value')?.textContent
}))

console.log('=== 昵称修改 ===')
await page.evaluate(() => {
  const row = [...document.querySelectorAll('.row')].find((r) => r.textContent.includes('昵称'))
  row?.click()
})
await sleep(400)
await setInput('.sheet-input', '测试用户')
await page.evaluate(() => {
  const b = [...document.querySelectorAll('.sheet-actions button')].find((x) =>
    x.textContent.includes('保存'),
  )
  b?.click()
})
await sleep(900)
const meWithName = await apiGet('/api/me')
console.log('server name:', meWithName.name)

console.log('=== 修改密码 ===')
await page.evaluate(() => { location.hash = '#/change-password' })
await sleep(600)
await page.evaluate(() => document.querySelectorAll('.field')[1]?.click())
await press('1', 6)
await page.evaluate(() => document.querySelectorAll('.field')[2]?.click())
await press('1', 6)
await clickText('更改密码')
await sleep(1000)
console.log('after change password hash:', await hash())

console.log('=== 退出并用密码登录 ===')
await page.evaluate(() => { location.hash = '#/settings' })
await sleep(500)
await clickText('退出登录')
await sleep(700)
console.log('after logout hash:', await hash(), '| token:', await page.evaluate(() => localStorage.getItem('token')))
await page.evaluate(() => {
  const b = [...document.querySelectorAll('.segmented .seg')].find(
    (x) => x.textContent.trim() === '密码登录',
  )
  b?.click()
})
await sleep(300)
await setInput('.phone-num', PHONE)
await setInput('.password-input', '111111')
await clickText('登录')
await waitHash('#/home')
await sleep(900)
state = await apiGet('/api/state')
const finalMe = await apiGet('/api/me')
console.log('password login ok | waterMl:', state.health.waterMl, '| alarms:', state.alarms.length, '| name:', finalMe.name)

console.log('errors:', errors.length ? errors : 'none')
await browser.close()
