import puppeteer from 'puppeteer-core'

const EDGE = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
const BASE = 'http://localhost:5175/'
const PHONE = process.argv[2] || '13800014444'
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
  page.waitForFunction((h) => location.hash.startsWith(h), { timeout: 8000 }, target)
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
const press = async (digit, times) => {
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

await page.goto(BASE + '#/login', { waitUntil: 'load' })
await sleep(500)
await clickText('忘记密码？')
await waitHash('#/forgot-password')
await sleep(400)
await setInput('.phone-num', PHONE)
await clickText('发送')
await sleep(700)
console.log('dev code:', await page.evaluate(() => document.querySelector('.dev-code')?.textContent))
await clickText('继续')
await waitHash('#/reset-password')
await sleep(500)
console.log('reset page hash:', await hash())
await page.evaluate(() => document.querySelectorAll('.field')[0]?.click())
await press('2', 6)
await page.evaluate(() => document.querySelectorAll('.field')[1]?.click())
await press('2', 6)
await clickText('重置密码')
await waitHash('#/home')
await sleep(800)
console.log('after reset hash:', await hash(), '| token:', !!(await page.evaluate(() => localStorage.getItem('token'))))

const api = await page.evaluate(async () => {
  const res = await fetch('/api/state', {
    headers: { Authorization: 'Bearer ' + localStorage.getItem('token') },
  })
  return (await res.json()).data.health
})
console.log('state after reset:', { waterMl: api.waterMl, age: api.age })

console.log('errors:', errors.length ? errors : 'none')
await browser.close()
