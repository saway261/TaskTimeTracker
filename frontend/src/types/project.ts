import type { MemoResponse } from './memo'

export interface ProjectResponse {
  id: number
  title: string
  description: string | null
  isFinished: boolean
  memos: MemoResponse[]
}

export interface ProjectCreateRequest {
  title: string
  description: string | null
}

export interface ProjectUpdateRequest {
  title: string
  description: string | null
}

export interface ProjectUpdateFinishedRequest {
  isFinished: boolean
}
