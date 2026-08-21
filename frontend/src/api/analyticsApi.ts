import { httpClient } from './httpClient'
import type {
  AnalyticsCommonQuery,
  EstimationAccuracyResponse,
  GapCauseAggregateResponse,
  ReflectionTimelineQuery,
  ReflectionTimelineResponse,
} from '@/types/analytics'

export function fetchEstimationAccuracy(params: AnalyticsCommonQuery) {
  return httpClient.get<EstimationAccuracyResponse>('/analytics/estimation-accuracy', { params })
}

export function fetchReflectionTimeline(params: ReflectionTimelineQuery) {
  return httpClient.get<ReflectionTimelineResponse>('/analytics/reflections', { params })
}

export function fetchGapCauses(params: AnalyticsCommonQuery) {
  return httpClient.get<GapCauseAggregateResponse>('/analytics/gap-causes', { params })
}
