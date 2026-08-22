// @vitest-environment jsdom

import { flushPromises, shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as analyticsApi from '@/api/analyticsApi'
import * as projectsApi from '@/api/projectsApi'
import * as reflectionsApi from '@/api/reflectionsApi'
import * as tagsApi from '@/api/tagsApi'
import { useAnalyticsStore } from '@/stores/analyticsStore'
import { useNotificationStore } from '@/stores/notificationStore'
import type { ApiError } from '@/types/apiError'
import type {
  EstimationAccuracyResponse,
  GapCauseAggregateResponse,
  ReflectionTimelineResponse,
} from '@/types/analytics'
import AnalyticsFilterBar from '@/components/analytics/AnalyticsFilterBar.vue'
import AnalyticsView from './AnalyticsView.vue'

vi.mock('@/api/analyticsApi')
vi.mock('@/api/projectsApi')
vi.mock('@/api/reflectionsApi')
vi.mock('@/api/tagsApi')

const accuracy = {
  analyzedTaskCount: 0,
  excluded: { total: 0, missingGapRate: 0, missingActualMinutes: 0 },
  diagnosis: null,
} as EstimationAccuracyResponse
const timeline: ReflectionTimelineResponse = {
  items: [],
  page: 0,
  size: 20,
  totalCount: 0,
  hasNext: false,
}
const gapCauses = {
  analyzedTaskCount: 0,
  totalLinkCount: 0,
  groups: [],
} as GapCauseAggregateResponse
const archivedTagError: ApiError = {
  status: 400,
  kind: 'validation',
  message: 'アーカイブ済みのタグは分析の絞り込みに指定できません',
  fieldErrors: { tagId: 'アーカイブ済みのタグは分析の絞り込みに指定できません' },
  formErrors: [],
}

describe('AnalyticsView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('選択中のタグが400になったらすべてのタグへ戻して再取得し通知する', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const analyticsStore = useAnalyticsStore()
    analyticsStore.filter.tagId = 't5'
    vi.mocked(projectsApi.fetchAll).mockResolvedValue({ data: [] } as never)
    vi.mocked(reflectionsApi.fetchCauseCategories).mockResolvedValue({ data: [] } as never)
    vi.mocked(tagsApi.fetchAll).mockResolvedValue({
      data: [{ id: 't5', name: '調査', isArchived: true, assignedTaskCount: 4 }],
    } as never)
    vi.mocked(analyticsApi.fetchEstimationAccuracy)
      .mockRejectedValueOnce(archivedTagError)
      .mockResolvedValueOnce({ data: accuracy } as never)
    vi.mocked(analyticsApi.fetchReflectionTimeline)
      .mockRejectedValueOnce(archivedTagError)
      .mockResolvedValueOnce({ data: timeline } as never)
    vi.mocked(analyticsApi.fetchGapCauses)
      .mockRejectedValueOnce(archivedTagError)
      .mockResolvedValueOnce({ data: gapCauses } as never)

    const wrapper = shallowMount(AnalyticsView, { global: { plugins: [pinia] } })
    await flushPromises()

    expect(analyticsStore.filter.tagId).toBeNull()
    expect(wrapper.findComponent(AnalyticsFilterBar).props('tags')).toEqual([])
    expect(analyticsApi.fetchEstimationAccuracy).toHaveBeenNthCalledWith(1, {
      projectId: undefined,
      tagId: 't5',
      from: undefined,
    })
    expect(analyticsApi.fetchEstimationAccuracy).toHaveBeenNthCalledWith(2, {
      projectId: undefined,
      tagId: undefined,
      from: undefined,
    })
    expect(useNotificationStore().notifications.at(-1)?.message).toContain(
      'タグ「調査」はアーカイブ済み',
    )
  })
})
