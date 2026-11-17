import { describe, expect, it } from 'vitest'
import { repeatText } from './alarm'

describe('repeatText', () => {
  it('空数组表示仅一次', () => {
    expect(repeatText([])).toBe('仅一次')
  })

  it('七天表示每天', () => {
    expect(repeatText([0, 1, 2, 3, 4, 5, 6])).toBe('每天')
  })

  it('周一至周五表示工作日', () => {
    expect(repeatText([1, 2, 3, 4, 5])).toBe('工作日')
  })

  it('周六和周日表示周末', () => {
    expect(repeatText([6, 0])).toBe('周末')
  })

  it('自定义重复按周一优先排序', () => {
    expect(repeatText([6, 1, 3])).toBe('周一、周三、周六')
  })
})
