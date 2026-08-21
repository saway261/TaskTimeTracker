import { describe, expect, it } from 'vitest'
import { swapVisibleItems } from './dragReorder'

describe('swapVisibleItems', () => {
  it('非表示項目を残したまま表示上隣り合う項目を入れ替える', () => {
    const first = { id: 1 }
    const hidden = { id: 2 }
    const last = { id: 3 }

    expect(swapVisibleItems([first, hidden, last], [first, last], 1, -1)).toEqual([
      last,
      hidden,
      first,
    ])
  })

  it('表示一覧の端より外へ移動しようとした場合は順序を変えない', () => {
    const items = [{ id: 1 }, { id: 2 }]

    expect(swapVisibleItems(items, items, 0, -1)).toEqual(items)
    expect(swapVisibleItems(items, items, 1, 1)).toEqual(items)
  })
})
