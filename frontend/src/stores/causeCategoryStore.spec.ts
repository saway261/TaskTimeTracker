// @vitest-environment jsdom

import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as reflectionsApi from '@/api/reflectionsApi'
import { useCauseCategoryStore } from './causeCategoryStore'

vi.mock('@/api/reflectionsApi')

const categories = [
  {
    code: 'TASK_BREAKDOWN',
    label: '作業の洗い出しが足りなかった',
    direction: 'OVER' as const,
    nextActionHint: '着手前に手順を書き出す',
    requiresCause: false,
  },
]

describe('causeCategoryStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.resetAllMocks()
  })

  it('同時に呼ばれても一覧を一度だけ取得し、以後はキャッシュを返す', async () => {
    vi.mocked(reflectionsApi.fetchCauseCategories).mockResolvedValue({ data: categories } as never)
    const store = useCauseCategoryStore()

    await Promise.all([store.fetchCategories(), store.fetchCategories()])
    await store.fetchCategories()

    expect(reflectionsApi.fetchCauseCategories).toHaveBeenCalledTimes(1)
    expect(store.categories).toEqual(categories)
    expect(store.initialized).toBe(true)
    expect(store.loading).toBe(false)
  })
})
