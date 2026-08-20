// @vitest-environment jsdom

import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as analyticsApi from '@/api/analyticsApi'
import type {
  EstimationAccuracyResponse,
  GapCauseAggregateResponse,
  ReflectionTimelineItemResponse,
  ReflectionTimelineResponse,
} from '@/types/analytics'
import { periodStart, useAnalyticsStore } from './analyticsStore'

vi.mock('@/api/analyticsApi')

const accuracy: EstimationAccuracyResponse = {
  onTimeThresholdPercent: 10,
  analyzedTaskCount: 12,
  excluded: { total: 2, missingGapRate: 1, missingActualMinutes: 1 },
  summary: {
    availability: { available: true, requiredCount: 5, currentCount: 12 },
    outcomeBreakdown: { lateCount: 4, onTimeCount: 6, earlyCount: 2 },
    onTimeRate: 50,
    factorMedian: 1.2,
    factorP25: 0.9,
    factorP75: 1.4,
    variancePercent: 25,
    recentTrend: 'IMPROVED',
    recentVariancePercent: 18,
    previousVariancePercent: 30,
  },
  diagnosis: null,
  scatter: [],
  scatterTruncated: false,
  sizeBuckets: [],
  trend: [],
  trendAvailability: { available: false, requiredCount: 20, currentCount: 12 },
}

const gapCauses: GapCauseAggregateResponse = {
  analyzedTaskCount: 12,
  totalLinkCount: 3,
  groups: [
    {
      direction: 'OVER',
      label: '超過側',
      totalCount: 3,
      sharePercent: 25,
      items: [
        {
          causeCategoryCode: 'SCOPE_CREEP',
          causeCategoryLabel: '想定外の作業',
          taskCount: 3,
          sharePercent: 25,
          gapRateMedian: 30,
        },
      ],
    },
  ],
}

const timelineItem = (taskId: number): ReflectionTimelineItemResponse => ({
  taskId,
  taskTitle: `タスク${taskId}`,
  projectId: 3,
  projectTitle: 'プロジェクト',
  finishedAt: '2026-08-20T10:00:00',
  estimatedMinutes: 60,
  actualMinutes: 75,
  gapMinutes: 15,
  gapRate: 25,
  outcome: 'LATE',
  causeCategories: [],
  cause: null,
  nextAction: null,
})

const timeline = (
  items: ReflectionTimelineItemResponse[],
  page = 0,
  hasNext = false,
): ReflectionTimelineResponse => ({
  items,
  page,
  size: 20,
  totalCount: items.length,
  hasNext,
})

describe('analyticsStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.resetAllMocks()
  })

  it('期間をローカル日時のfromへ変換する', () => {
    const now = new Date(2026, 7, 20, 12, 34, 56)

    expect(periodStart('ALL', now)).toBeUndefined()
    expect(periodStart('LAST_30_DAYS', now)).toBe('2026-07-21T12:34:56')
    expect(periodStart('LAST_90_DAYS', now)).toBe('2026-05-22T12:34:56')
    expect(periodStart('LAST_YEAR', now)).toBe('2025-08-20T12:34:56')
  })

  it('共通フィルターを使って精度・原因集計・タイムラインを同時に再取得する', async () => {
    vi.mocked(analyticsApi.fetchEstimationAccuracy).mockResolvedValue({ data: accuracy } as never)
    vi.mocked(analyticsApi.fetchReflectionTimeline).mockResolvedValue({
      data: timeline([timelineItem(1)]),
    } as never)
    vi.mocked(analyticsApi.fetchGapCauses).mockResolvedValue({ data: gapCauses } as never)
    const store = useAnalyticsStore()
    store.filter.projectId = 3

    await store.refresh()

    expect(analyticsApi.fetchEstimationAccuracy).toHaveBeenCalledWith({
      projectId: 3,
      from: undefined,
    })
    expect(analyticsApi.fetchReflectionTimeline).toHaveBeenCalledWith({
      projectId: 3,
      from: undefined,
      causeCategory: undefined,
      outcome: 'ALL',
      page: 0,
      size: 20,
    })
    expect(analyticsApi.fetchGapCauses).toHaveBeenCalledWith({
      projectId: 3,
      from: undefined,
    })
    expect(store.accuracy).toEqual(accuracy)
    expect(store.timeline?.items).toHaveLength(1)
    expect(store.gapCauses).toEqual(gapCauses)
    expect(store.refreshing).toBe(false)
  })

  it('さらに読み込むと次ページを既存項目へ追加する', async () => {
    vi.mocked(analyticsApi.fetchReflectionTimeline)
      .mockResolvedValueOnce({ data: timeline([timelineItem(1)], 0, true) } as never)
      .mockResolvedValueOnce({ data: timeline([timelineItem(2)], 1, false) } as never)
    const store = useAnalyticsStore()

    await store.fetchTimeline(true)
    await store.loadMoreTimeline()

    expect(analyticsApi.fetchReflectionTimeline).toHaveBeenLastCalledWith({
      projectId: undefined,
      from: undefined,
      causeCategory: undefined,
      outcome: 'ALL',
      page: 1,
      size: 20,
    })
    expect(store.timeline?.items.map((item) => item.taskId)).toEqual([1, 2])
    expect(store.timeline?.page).toBe(1)
  })

  it('原因カテゴリの変更ではタイムラインだけを先頭から再取得する', async () => {
    vi.mocked(analyticsApi.fetchReflectionTimeline).mockResolvedValue({
      data: timeline([]),
    } as never)
    const store = useAnalyticsStore()

    await store.setCauseCategory('SCOPE_CREEP')

    expect(analyticsApi.fetchEstimationAccuracy).not.toHaveBeenCalled()
    expect(analyticsApi.fetchGapCauses).not.toHaveBeenCalled()
    expect(analyticsApi.fetchReflectionTimeline).toHaveBeenCalledWith(
      expect.objectContaining({ causeCategory: 'SCOPE_CREEP', page: 0 }),
    )
  })
})
