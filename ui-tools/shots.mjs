// 打开应用并截取关键页面，用于人工查看当前完成度。
import puppeteer from 'puppeteer-core'

const EDGE = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
const BASE = 'http://localhost:5173/#'
const OUT = 'C:/Users/21693/Projects/ui-tools'
const phone = '136' + String(Date.now()).slice(-8)
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
  if (m.type() === 'error') errors.push(m.text())
})
page.on('pageerror', (e) => errors.push(e.message))

async function go(hash) {
  await page.goto(BASE + hash, { waitUntil: 'load' })
  await sleep(600)
}
async function clickText(text) {
  await page.evaluate((t) => {
    const nodes = [...document.querySelectorAll('button, .menu-row, .row, .green-link, a')]
    const el =
      nodes.find((n) => (n.textContent || '').trim() === t) ||
      nodes.find((n) => (n.textContent || '').includes(t))
    el?.click()
  }, text)
  await sleep(450)
}
async function shot(name) {
  await page.screenshot({ path: `${OUT}/${name}.png` })
  console.log('saved', name)
}

// 登录 + 新用户引导
await go('/login')
await page.$eval('input[type=checkbox]', (el) => el.click())
await page.$eval('input[type=tel]', (el, v) => {
  el.value = v
  el.dispatchEvent(new Event('input', { bubbles: true }))
}, phone)
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
await sleep(800)
await shot('shot-home')

// 睡眠页并调整时间
await go('/sleep')
await page.evaluate(() => document.querySelector('.bw')?.click())
await page.waitForFunction(
  () => (document.body.innerText || '').includes('调整睡眠时间'),
  { timeout: 5000 },
)
const times = await page.$$('input[type=time]')
await times[0].evaluate((el) => {
  el.value = '22:30'
  el.dispatchEvent(new Event('input', { bubbles: true }))
})
await times[1].evaluate((el) => {
  el.value = '06:30'
  el.dispatchEvent(new Event('input', { bubbles: true }))
})
await clickText('保存')
await sleep(500)
await shot('shot-sleep')

// 账户与安全：登录设备列表
await go('/account-security')
await page.waitForFunction(
  () => (document.body.innerText || '').includes('登录设备'),
  { timeout: 8000 },
)
await sleep(800)
await shot('shot-security')

// 我的页
await go('/profile')
await sleep(500)
await shot('shot-profile')

// 设置页
await go('/settings')
await sleep(500)
await shot('shot-settings')

console.log('console errors:', errors.length ? errors : 'none')
await browser.close()
