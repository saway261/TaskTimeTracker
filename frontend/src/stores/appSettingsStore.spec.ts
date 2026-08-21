// @vitest-environment jsdom

import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as appSettingsApi from '@/api/appSettingsApi'
import { DEFAULT_ON_TIME_THRESHOLD_PERCENT } from '@/utils/duration'
import { useAppSettingsStore } from './appSettingsStore'

vi.mock('@/api/appSettingsApi')

describe('appSettingsStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.resetAllMocks()
  })

  it('同時に呼ばれても設定を一度だけ取得して保持する', async () => {
    vi.mocked(appSettingsApi.fetchSettings).mockResolvedValue({
      data: { onTimeThresholdPercent: 20 },
    } as never)
    const store = useAppSettingsStore()

    await Promise.all([store.load(), store.load()])
    await store.load()

    expect(appSettingsApi.fetchSettings).toHaveBeenCalledTimes(1)
    expect(store.onTimeThresholdPercent).toBe(20)
    expect(store.loaded).toBe(true)
  })

  it('取得に失敗しても例外を伝播せず既定値で読み込み済みにする', async () => {
    vi.mocked(appSettingsApi.fetchSettings).mockRejectedValue({ status: 401 })
    const store = useAppSettingsStore()

    await expect(store.load()).resolves.toBeUndefined()

    expect(store.onTimeThresholdPercent).toBe(DEFAULT_ON_TIME_THRESHOLD_PERCENT)
    expect(store.loaded).toBe(true)
  })
})
