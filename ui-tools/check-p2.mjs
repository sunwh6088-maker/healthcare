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
  await sleep(400)
}

console.log('=== 路由标题 ===')
for (const route of ['/home', '/sleep', '/account-security', '/goals']) {
  await go(route)
  console.log(route, '->', await page.title())
}

console.log('=== 闹钟重复（抽取工具函数后） ===')
await go('/sleep')
await page.evaluate(() => localStorage.removeItem('healthcare-app-state'))
await page.reload({ waitUntil: 'load' })
await sleep(450)
await page.evaluate(() => document.querySelector('.alarm-row')?.click())
await sleep(300)
console.log('重复摘要:', await page.evaluate(() => document.querySelector('.repeat-summary')?.textContent))

console.log('=== 账户校验（抽取工具函数后） ===')
await go('/account-security')
await page.evaluate(() => {
  const row = [...document.querySelectorAll('.row')].find((r) => r.textContent.includes('手机号'))
  row?.click()
})
await sleep(300)
await page.evaluate(() => {
  const i = document.querySelector('.sheet-input')
  i.value = '123'
  i.dispatchEvent(new Event('input', { bubbles: true }))
})
await page.evaluate(() => {
  const b = [...document.querySelectorAll('.sheet-actions button')].find((x) =>
    x.textContent.includes('保存'),
  )
  b?.click()
})
await sleep(300)
console.log('非法手机号提示:', await page.evaluate(() => document.querySelector('.toast')?.textContent))
await page.evaluate(() => {
  const i = document.querySelector('.sheet-input')
  i.value = '139 0000 1111'
  i.dispatchEvent(new Event('input', { bubbles: true }))
})
await page.evaluate(() => {
  const b = [...document.querySelectorAll('.sheet-actions button')].find((x) =>
    x.textContent.includes('保存'),
  )
  b?.click()
})
await sleep(400)
console.log('合法手机号显示:', await page.evaluate(() => document.querySelector('.r-value.num')?.textContent))

console.log('errors:', errors.length ? errors : 'none')
await browser.close()
