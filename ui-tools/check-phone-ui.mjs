import puppeteer from 'puppeteer-core'

const EDGE = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
const BASE = 'http://localhost:5173/#'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

const browser = await puppeteer.launch({
  executablePath: EDGE,
  headless: true,
  args: ['--no-sandbox', '--disable-gpu'],
})
const page = await browser.newPage()
await page.setViewport({ width: 500, height: 1000, deviceScaleFactor: 1 })

let sendCodeCalls = 0
page.on('request', (req) => {
  if (req.url().includes('/api/auth/send-code')) sendCodeCalls++
})

async function setPhone(value) {
  await page.evaluate((val) => {
    const input = document.querySelector('.phone-num')
    input.value = val
    input.dispatchEvent(new Event('input', { bubbles: true }))
  }, value)
}

async function readPhone() {
  return page.$eval('.phone-num', (el) => el.value)
}

await page.goto(BASE + '/login', { waitUntil: 'load' })
await page.evaluate(() => localStorage.clear())
await page.reload({ waitUntil: 'load' })
await sleep(400)

const country = await page.$eval('.country', (el) => el.textContent.trim())
const placeholder = await page.$eval('.phone-num', (el) => el.placeholder)
const clickContinue = () =>
  page.evaluate(() => {
    ;[...document.querySelectorAll('button')]
      .find((b) => b.textContent.trim() === '继续')
      ?.click()
  })

// 1. 非法号码（123 开头）应被拦截，不调用发送验证码接口
await setPhone('12345678901')
const formattedInvalid = await readPhone()
await clickContinue()
await sleep(300)
const toastText = await page.$eval('.toast', (el) => el.textContent).catch(() => null)
const fieldError = await page.$eval('.field-error', (el) => el.textContent).catch(() => null)
const urlAfterInvalid = page.url()
const callsAfterInvalid = sendCodeCalls

// 2. 合法号码自动格式化为 3-4-4
await setPhone('13800138000')
const formattedValid = await readPhone()

// 3. 粘贴带 +86 的完整号码
await setPhone('+86 13800138000')
const formattedPlus86 = await readPhone()

// 4. 第二位非 3-9（120 开头）仍应拦截
await setPhone('12000000000')
await clickContinue()
await sleep(300)
const callsAfterInvalid2 = sendCodeCalls

// 5. 忘记密码页：同样的号码格式校验
await page.goto(BASE + '/forgot-password', { waitUntil: 'load' })
await sleep(400)
await setPhone('12345678901')
const forgotInvalidValue = await readPhone()
const callsBeforeForgot = sendCodeCalls
await page.evaluate(() => {
  ;[...document.querySelectorAll('button')]
    .find((b) => b.textContent.trim() === '发送')
    ?.click()
})
await sleep(300)
const forgotToast = await page.$eval('.toast', (el) => el.textContent).catch(() => null)
const forgotStepText = await page.$eval('.btn-primary', (el) => el.textContent.trim())
const callsAfterForgotInvalid = sendCodeCalls
await setPhone('13800138000')
const forgotFormatted = await readPhone()

console.log(
  JSON.stringify(
    {
      country,
      placeholder,
      formattedInvalid,
      toastText,
      fieldError,
      urlAfterInvalid,
      callsAfterInvalid,
      formattedValid,
      formattedPlus86,
      callsAfterInvalid2,
      forgotInvalidValue,
      forgotToast,
      forgotStepText,
      callsBeforeForgot,
      callsAfterForgotInvalid,
      forgotFormatted,
    },
    null,
    2,
  ),
)

await browser.close()
