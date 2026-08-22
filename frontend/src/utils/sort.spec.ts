import { describe, expect, it } from 'vitest'
import { sortByItemOrder, sortProjectItemsByOrder } from './sort'

describe('sortByItemOrder', () => {
  it('item-orderのposition昇順に整列する', () => {
    const items = [{ id: '10' }, { id: '20' }, { id: '30' }]
    const order = [
      { id: '30', position: 0 },
      { id: '10', position: 1 },
      { id: '20', position: 2 },
    ]

    expect(sortByItemOrder(items, order)).toEqual([{ id: '30' }, { id: '10' }, { id: '20' }])
  })

  it('orderに現れない項目は末尾へ回す', () => {
    const items = [{ id: '10' }, { id: '20' }, { id: '30' }]
    const order = [{ id: '30', position: 0 }]

    expect(sortByItemOrder(items, order)).toEqual([{ id: '30' }, { id: '10' }, { id: '20' }])
  })

  // 一覧APIのORDER BYで順序が確定しているため、決められない項目はサーバ順を維持する。
  // id昇順に並べ替えてはいけない（公開ID化後にidの大小が意味を持たなくなるため）。
  it('orderに現れない項目同士はサーバから受け取った順を保つ', () => {
    const items = [{ id: '30' }, { id: '10' }, { id: '20' }]

    expect(sortByItemOrder(items, [])).toEqual([{ id: '30' }, { id: '10' }, { id: '20' }])
  })

  it('元の配列を破壊しない', () => {
    const items = [{ id: '20' }, { id: '10' }]
    sortByItemOrder(items, [{ id: '10', position: 0 }])

    expect(items).toEqual([{ id: '20' }, { id: '10' }])
  })
})

describe('sortProjectItemsByOrder', () => {
  it('TaskとTaskGroupを1つのpositionで整列する', () => {
    const items = [
      { kind: 'TASK' as const, id: '1' },
      { kind: 'TASK_GROUP' as const, id: '2' },
    ]
    const order = [
      { type: 'TASK_GROUP' as const, id: '2', position: 0 },
      { type: 'TASK' as const, id: '1', position: 1 },
    ]

    expect(sortProjectItemsByOrder(items, order)).toEqual([
      { kind: 'TASK_GROUP', id: '2' },
      { kind: 'TASK', id: '1' },
    ])
  })

  // Task#3 と TaskGroup#3 はidが衝突するため、typeとidの組で引く必要がある。
  it('同じidのTaskとTaskGroupを取り違えない', () => {
    const items = [
      { kind: 'TASK' as const, id: '3' },
      { kind: 'TASK_GROUP' as const, id: '3' },
    ]
    const order = [
      { type: 'TASK_GROUP' as const, id: '3', position: 0 },
      { type: 'TASK' as const, id: '3', position: 1 },
    ]

    expect(sortProjectItemsByOrder(items, order)).toEqual([
      { kind: 'TASK_GROUP', id: '3' },
      { kind: 'TASK', id: '3' },
    ])
  })

  it('orderに現れない項目は末尾へ回し、その中ではサーバ順を保つ', () => {
    const items = [
      { kind: 'TASK' as const, id: '30' },
      { kind: 'TASK' as const, id: '10' },
      { kind: 'TASK_GROUP' as const, id: '20' },
    ]
    const order = [{ type: 'TASK_GROUP' as const, id: '20', position: 0 }]

    expect(sortProjectItemsByOrder(items, order)).toEqual([
      { kind: 'TASK_GROUP', id: '20' },
      { kind: 'TASK', id: '30' },
      { kind: 'TASK', id: '10' },
    ])
  })
})
