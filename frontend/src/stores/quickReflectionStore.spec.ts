import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { useQuickReflectionStore } from './quickReflectionStore'
import type { ReflectionTaskResponse } from '@/types/reflection'

const task: ReflectionTaskResponse = {
  id: 'task10',
  title: '実装する',
  finishedAt: '2026-08-15T02:00:00',
  actualMinutesCached: 25,
  gapMinutesCached: -35,
  gapRateCached: -58.3,
  reflection: null,
}

describe('quickReflectionStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('初期状態では対象を持たない', () => {
    const store = useQuickReflectionStore()

    expect(store.task).toBeNull()
    expect(store.isOpen).toBe(false)
  })

  it('open で対象を保持し、close で解除する', () => {
    const store = useQuickReflectionStore()

    store.open(task)
    expect(store.task).toEqual(task)
    expect(store.isOpen).toBe(true)

    store.close()
    expect(store.task).toBeNull()
    expect(store.isOpen).toBe(false)
  })

  it('続けて別のタスクを完了にすると対象が差し替わる', () => {
    const store = useQuickReflectionStore()
    const another = { ...task, id: 'task11', title: '設計する' }

    store.open(task)
    store.open(another)

    expect(store.task).toEqual(another)
  })
})
