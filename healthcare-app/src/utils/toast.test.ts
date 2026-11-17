import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { toast } from './toast'

describe('toast', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('优先挂载到手机壳内，并自动消失', () => {
    const phone = document.createElement('div')
    phone.className = 'phone'
    document.body.appendChild(phone)

    toast('已保存')

    const el = phone.querySelector('.toast')
    expect(el).not.toBeNull()
    expect(el?.getAttribute('role')).toBe('status')
    expect(el?.textContent).toBe('已保存')

    vi.advanceTimersByTime(1600)
    expect(phone.querySelector('.toast')).toBeNull()
  })

  it('没有手机壳时使用 fixed 兜底', () => {
    toast('fallback')
    const el = document.querySelector('.toast')
    expect(el?.classList.contains('toast-fixed')).toBe(true)
  })
})
