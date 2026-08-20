import type { ReflectionCauseCategorySummary } from './reflection'

export type AnalyticsPeriod = 'ALL' | 'LAST_30_DAYS' | 'LAST_90_DAYS' | 'LAST_YEAR'
export type AnalyticsOutcome = 'LATE' | 'ON_TIME' | 'EARLY'
export type ReflectionOutcomeFilter = AnalyticsOutcome | 'ALL'
export type RecentTrend = 'IMPROVED' | 'STABLE' | 'WORSENED'
export type DiagnosisCode = 'GOOD' | 'UNSTABLE' | 'BIASED_LATE' | 'BIASED_EARLY' | 'UNSTABLE_BIASED'
export type BiasDirection = 'LATE' | 'EARLY' | 'NONE'
export type GapCauseDirection = 'OVER' | 'UNDER' | 'BOTH'

export interface AnalyticsFilter {
  projectId: number | null
  period: AnalyticsPeriod
  causeCategory: string | null
  outcome: ReflectionOutcomeFilter
}

export interface MetricAvailabilityResponse {
  available: boolean
  requiredCount: number
  currentCount: number
}

export interface OutcomeBreakdownResponse {
  lateCount: number
  onTimeCount: number
  earlyCount: number
}

export interface AccuracySummaryResponse {
  availability: MetricAvailabilityResponse
  outcomeBreakdown: OutcomeBreakdownResponse
  onTimeRate: number | null
  factorMedian: number | null
  factorP25: number | null
  factorP75: number | null
  variancePercent: number | null
  recentTrend: RecentTrend | null
  recentVariancePercent: number | null
  previousVariancePercent: number | null
}

export interface ExcludedTaskCountResponse {
  total: number
  missingGapRate: number
  missingActualMinutes: number
}

export interface DiagnosisResponse {
  code: DiagnosisCode
  biasDirection: BiasDirection
  title: string
  message: string
}

export interface ScatterPointResponse {
  taskId: number
  taskTitle: string
  estimatedMinutes: number
  actualMinutes: number
  gapRate: number
  outcome: AnalyticsOutcome
}

export interface SizeBucketResponse {
  bucketCode: 'M15' | 'M30' | 'M60' | 'M120' | 'OVER120'
  label: string
  taskCount: number
  factorMedian: number | null
  onTimeRate: number | null
}

export interface AccuracyTrendPointResponse {
  sequence: number
  finishedAt: string
  windowFrom: string
  factorMedian: number
  variancePercent: number
}

export interface GapCauseItemResponse {
  causeCategoryCode: string | null
  causeCategoryLabel: string
  taskCount: number
  sharePercent: number
  gapRateMedian: number | null
}

export interface GapCauseGroupResponse {
  direction: GapCauseDirection
  label: string
  totalCount: number
  sharePercent: number
  items: GapCauseItemResponse[]
}

export interface GapCauseAggregateResponse {
  analyzedTaskCount: number
  totalLinkCount: number
  groups: GapCauseGroupResponse[]
}

export interface EstimationAccuracyResponse {
  onTimeThresholdPercent: number
  analyzedTaskCount: number
  excluded: ExcludedTaskCountResponse
  summary: AccuracySummaryResponse
  diagnosis: DiagnosisResponse | null
  scatter: ScatterPointResponse[]
  scatterTruncated: boolean
  sizeBuckets: SizeBucketResponse[]
  trend: AccuracyTrendPointResponse[]
  trendAvailability: MetricAvailabilityResponse
}

export interface ReflectionTimelineItemResponse {
  taskId: number
  taskTitle: string
  projectId: number
  projectTitle: string
  finishedAt: string
  estimatedMinutes: number
  actualMinutes: number | null
  gapMinutes: number | null
  gapRate: number | null
  outcome: AnalyticsOutcome | null
  causeCategories: ReflectionCauseCategorySummary[]
  cause: string | null
  nextAction: string | null
}

export interface ReflectionTimelineResponse {
  items: ReflectionTimelineItemResponse[]
  page: number
  size: number
  totalCount: number
  hasNext: boolean
}

export interface AnalyticsCommonQuery {
  projectId?: number
  from?: string
  to?: string
}

export interface ReflectionTimelineQuery extends AnalyticsCommonQuery {
  causeCategory?: string
  outcome: ReflectionOutcomeFilter
  page: number
  size: number
}
