import type { MemoResponse } from './memo'

export interface TaskGroupResponse {
  id: number
  projectId: number
  title: string
  description: string | null
  isFinished: boolean
  memos: MemoResponse[]
}

export interface TaskGroupCreateRequest {
  title: string
  description: string | null
}

export interface TaskGroupUpdateRequest {
  title: string
  description: string | null
}

export interface TaskGroupUpdateFinishedRequest {
  isFinished: boolean
}
