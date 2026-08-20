import { httpClient } from './httpClient'
import type {
  AnalyticsCommonQuery,
  EstimationAccuracyResponse,
  ReflectionTimelineQuery,
  ReflectionTimelineResponse,
} from '@/types/analytics'

export function fetchEstimationAccuracy(params: AnalyticsCommonQuery) {
  return httpClient.get<EstimationAccuracyResponse>('/analytics/estimation-accuracy', { params })
}

export function fetchReflectionTimeline(params: ReflectionTimelineQuery) {
  return httpClient.get<ReflectionTimelineResponse>('/analytics/reflections', { params })
}
