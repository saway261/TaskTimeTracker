import type { MemoResponse } from './memo'
import type { TagSummary } from './tag'

export interface TaskResponse {
  id: number
  projectId: number | null
  taskGroupId: number | null
  title: string
  description: string | null
  estimatedMinutes: number | null
  createdAt: string
  finishedAt: string | null // ← 完了判定はこれ。isFinished は存在しない
  actualMinutesCached: number | null // 未完了時は null
  gapMinutesCached: number | null // 未完了時は null
  gapRateCached: number | null // 未完了時は null。単位は「％」（100倍しない）
  memos: MemoResponse[]
  tags: TagSummary[]
}

export interface TaskCreateRequest {
  title: string
  description: string | null
  estimatedMinutes: number
  tagIds: number[]
}

export interface TaskUpdatePropertyRequest {
  title: string
  description: string | null
}

export interface TaskUpdateEstimatedMinutesRequest {
  estimatedMinutes: number
}

export interface TaskUpdateFinishedRequest {
  isFinished: boolean
}

export interface TaskUpdateParentRequest {
  projectId: number | null
  taskGroupId: number | null
}

export interface TaskTagsUpdateRequest {
  tagIds: number[]
}
