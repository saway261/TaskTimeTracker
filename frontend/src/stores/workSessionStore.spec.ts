// @vitest-environment jsdom

import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as workSessionsApi from '@/api/workSessionsApi'
import { useActiveTimerStore } from '@/stores/activeTimerStore'
import { useWorkSessionStore } from '@/stores/workSessionStore'
import type { WorkSession } from '@/types/workSession'

vi.mock('@/api/workSessionsApi')

const startedSession: WorkSession = {
  id: 10,
  taskId: 20,
  minutes: null,
  startedAt: '2026-08-18T10:00:00+09:00',
  endedAt: null,
  createdAt: '2026-08-18T10:00:00+09:00',
  updatedAt: '2026-08-18T10:00:00+09:00',
  type: 'TIMER',
}

describe('workSessionStore timer synchronization', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    setActivePinia(createPinia())
  })

  it('タイマー開始成功直後に一覧再取得を待たず稼働中表示へ切り替える', async () => {
    vi.mocked(workSessionsApi.create).mockResolvedValue({ data: startedSession } as never)
    vi.mocked(workSessionsApi.fetchActiveTimers).mockReturnValue(new Promise(() => {}))

    await useWorkSessionStore().startTimer(startedSession.taskId)

    expect(useActiveTimerStore().hasActiveTimers).toBe(true)
    expect(workSessionsApi.fetchActiveTimers).toHaveBeenCalledOnce()
  })
})
