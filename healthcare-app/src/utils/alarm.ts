// 闹钟重复星期的展示文案（0 = 周日，1 = 周一 … 6 = 周六）
const WEEK_ORDER = [1, 2, 3, 4, 5, 6, 0]
const DAY_NAMES = ['日', '一', '二', '三', '四', '五', '六']

export function repeatText(repeat: number[]): string {
  if (!repeat.length) return '仅一次'
  const set = new Set(repeat)
  if (repeat.length === 7) return '每天'
  if (repeat.length === 5 && [1, 2, 3, 4, 5].every((d) => set.has(d))) return '工作日'
  if (repeat.length === 2 && [0, 6].every((d) => set.has(d))) return '周末'
  return WEEK_ORDER.filter((d) => set.has(d))
    .map((d) => `周${DAY_NAMES[d]}`)
    .join('、')
}
