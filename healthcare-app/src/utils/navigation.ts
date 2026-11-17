import type { Router } from 'vue-router'

/**
 * 返回上一页；如果是直接打开的内页（没有历史记录），回退到 fallback。
 */
export function goBack(router: Router, fallback = '/home') {
  const state = window.history.state as { back?: string | null } | null
  if (state?.back) router.back()
  else router.push(fallback)
}
