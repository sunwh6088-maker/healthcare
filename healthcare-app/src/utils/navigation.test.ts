import { describe, expect, it, vi } from 'vitest'
import type { Router } from 'vue-router'
import { goBack } from './navigation'

function createRouter() {
  return { back: vi.fn(), push: vi.fn() } as unknown as Router & {
    back: ReturnType<typeof vi.fn>
    push: ReturnType<typeof vi.fn>
  }
}

describe('goBack', () => {
  it('有历史记录时返回上一页', () => {
    window.history.replaceState({ back: '/home' }, '')
    const router = createRouter()
    goBack(router)
    expect(router.back).toHaveBeenCalledOnce()
    expect(router.push).not.toHaveBeenCalled()
  })

  it('没有历史记录时使用 fallback', () => {
    window.history.replaceState({ back: null }, '')
    const router = createRouter()
    goBack(router, '/settings')
    expect(router.push).toHaveBeenCalledWith('/settings')
  })
})
