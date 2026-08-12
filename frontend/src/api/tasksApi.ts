import { httpClient } from './httpClient'
import type {
  TaskCreateRequest,
  TaskResponse,
  TaskUpdateEstimatedMinutesRequest,
  TaskUpdateFinishedRequest,
  TaskUpdateParentRequest,
  TaskUpdatePropertyRequest,
} from '@/types/task'

// projectId 直下だけでなく、配下タスクグループのタスクも含めて返る。
// Project直下のみが欲しい場合は呼び出し側で projectId !== null に絞り込む。
export function fetchAllInProject(projectId: number, isFinished?: boolean) {
  return httpClient.get<TaskResponse[]>(`/projects/${projectId}/tasks`, {
    params: { isFinished },
  })
}

export function fetchAllInTaskGroup(taskGroupId: number, isFinished?: boolean) {
  return httpClient.get<TaskResponse[]>(`/task-groups/${taskGroupId}/tasks`, {
    params: { isFinished },
  })
}

export function fetchById(taskId: number) {
  return httpClient.get<TaskResponse>(`/tasks/${taskId}`)
}

export function createInProject(projectId: number, req: TaskCreateRequest) {
  return httpClient.post<TaskResponse>(`/projects/${projectId}/tasks`, req)
}

export function createInTaskGroup(taskGroupId: number, req: TaskCreateRequest) {
  return httpClient.post<TaskResponse>(`/task-groups/${taskGroupId}/tasks`, req)
}

export function updateProperty(taskId: number, req: TaskUpdatePropertyRequest) {
  return httpClient.patch<TaskResponse>(`/tasks/${taskId}`, req)
}

// 作業セッションが1件でもあると400（estimate minutes update not allowed）。
export function updateEstimatedMinutes(taskId: number, req: TaskUpdateEstimatedMinutesRequest) {
  return httpClient.patch<TaskResponse>(`/tasks/${taskId}/estimated-minutes`, req)
}

// 未終了セッションがあると400（task finish not allowed）。isFinished:falseで完了解除もできる。
export function updateFinished(taskId: number, req: TaskUpdateFinishedRequest) {
  return httpClient.patch<TaskResponse>(`/tasks/${taskId}/finished`, req)
}

export function updateParent(taskId: number, req: TaskUpdateParentRequest) {
  return httpClient.patch<TaskResponse>(`/tasks/${taskId}/parent`, req)
}

// B1修正済み。作業セッションを持つタスクも204で削除できる。
export function remove(taskId: number) {
  return httpClient.delete<void>(`/tasks/${taskId}`)
}
