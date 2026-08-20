import { defineStore } from 'pinia'
import * as analyticsApi from '@/api/analyticsApi'
import type {
  AnalyticsCommonQuery,
  AnalyticsFilter,
  AnalyticsPeriod,
  EstimationAccuracyResponse,
  GapCauseAggregateResponse,
  ReflectionTimelineQuery,
  ReflectionTimelineResponse,
} from '@/types/analytics'
import type { ApiError } from '@/types/apiError'

const TIMELINE_PAGE_SIZE = 20

function toLocalDateTime(date: Date): string {
  const pad = (value: number) => value.toString().padStart(2, '0')
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  )
}

export function periodStart(period: AnalyticsPeriod, now = new Date()): string | undefined {
  if (period === 'ALL') return undefined

  const start = new Date(now)
  if (period === 'LAST_30_DAYS') start.setDate(start.getDate() - 30)
  if (period === 'LAST_90_DAYS') start.setDate(start.getDate() - 90)
  if (period === 'LAST_YEAR') start.setFullYear(start.getFullYear() - 1)
  return toLocalDateTime(start)
}

function commonQuery(filter: AnalyticsFilter): AnalyticsCommonQuery {
  return {
    projectId: filter.projectId ?? undefined,
    from: periodStart(filter.period),
  }
}

let accuracyRequestId = 0
let timelineRequestId = 0
let gapCausesRequestId = 0
let refreshRequestId = 0

export const useAnalyticsStore = defineStore('analytics', {
  state: () => ({
    accuracy: null as EstimationAccuracyResponse | null,
    timeline: null as ReflectionTimelineResponse | null,
    gapCauses: null as GapCauseAggregateResponse | null,
    filter: {
      projectId: null,
      period: 'ALL',
      causeCategory: null,
      outcome: 'ALL',
    } as AnalyticsFilter,
    loadingAccuracy: false,
    loadingTimeline: false,
    loadingGapCauses: false,
    refreshing: false,
    error: null as ApiError | null,
  }),
  actions: {
    async refresh() {
      const requestId = ++refreshRequestId
      this.error = null
      this.accuracy = null
      this.timeline = null
      this.gapCauses = null
      this.refreshing = true
      await Promise.allSettled([
        this.fetchAccuracy(),
        this.fetchTimeline(true),
        this.fetchGapCauses(),
      ])
      if (requestId === refreshRequestId) this.refreshing = false
    },

    async fetchAccuracy() {
      const requestId = ++accuracyRequestId
      this.loadingAccuracy = true
      try {
        const { data } = await analyticsApi.fetchEstimationAccuracy(commonQuery(this.filter))
        if (requestId === accuracyRequestId) this.accuracy = data
      } catch (e) {
        if (requestId === accuracyRequestId) this.error = e as ApiError
        throw e
      } finally {
        if (requestId === accuracyRequestId) this.loadingAccuracy = false
      }
    },

    async fetchTimeline(reset = false) {
      const requestId = ++timelineRequestId
      const page = reset || this.timeline === null ? 0 : this.timeline.page + 1
      const query: ReflectionTimelineQuery = {
        ...commonQuery(this.filter),
        causeCategory: this.filter.causeCategory ?? undefined,
        outcome: this.filter.outcome,
        page,
        size: TIMELINE_PAGE_SIZE,
      }

      this.loadingTimeline = true
      if (reset) this.timeline = null
      try {
        const { data } = await analyticsApi.fetchReflectionTimeline(query)
        if (requestId !== timelineRequestId) return

        const previousItems = reset || this.timeline === null ? [] : this.timeline.items
        this.timeline = { ...data, items: [...previousItems, ...data.items] }
      } catch (e) {
        if (requestId === timelineRequestId) this.error = e as ApiError
        throw e
      } finally {
        if (requestId === timelineRequestId) this.loadingTimeline = false
      }
    },

    async fetchGapCauses() {
      const requestId = ++gapCausesRequestId
      this.loadingGapCauses = true
      try {
        const { data } = await analyticsApi.fetchGapCauses(commonQuery(this.filter))
        if (requestId === gapCausesRequestId) this.gapCauses = data
      } catch (e) {
        if (requestId === gapCausesRequestId) this.error = e as ApiError
        throw e
      } finally {
        if (requestId === gapCausesRequestId) this.loadingGapCauses = false
      }
    },

    setProject(projectId: number | null) {
      this.filter.projectId = projectId
      return this.refresh()
    },

    setPeriod(period: AnalyticsPeriod) {
      this.filter.period = period
      return this.refresh()
    },

    setCauseCategory(causeCategory: string | null) {
      this.filter.causeCategory = causeCategory
      this.error = null
      return this.fetchTimeline(true).catch(() => {})
    },

    setOutcome(outcome: AnalyticsFilter['outcome']) {
      this.filter.outcome = outcome
      this.error = null
      return this.fetchTimeline(true).catch(() => {})
    },

    loadMoreTimeline() {
      if (this.loadingTimeline || !this.timeline?.hasNext) return Promise.resolve()
      return this.fetchTimeline(false).catch(() => {})
    },
  },
})
