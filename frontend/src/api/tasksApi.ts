import { httpClient } from './httpClient'
import type {
  TaskCreateRequest,
  TaskResponse,
  TaskTagsUpdateRequest,
  TaskUpdateEstimatedMinutesRequest,
  TaskUpdateFinishedRequest,
  TaskUpdateParentRequest,
  TaskUpdatePropertyRequest,
} from '@/types/task'

// projectId 直下だけでなく、配下タスクグループのタスクも含めて返る。
// Project直下のみが欲しい場合は呼び出し側で projectId !== null に絞り込む。
export function fetchAllInProject(projectId: string, isFinished?: boolean) {
  return httpClient.get<TaskResponse[]>(`/projects/${projectId}/tasks`, {
    params: { isFinished },
  })
}

export function fetchAllInTaskGroup(taskGroupId: string, isFinished?: boolean) {
  return httpClient.get<TaskResponse[]>(`/task-groups/${taskGroupId}/tasks`, {
    params: { isFinished },
  })
}

export function fetchById(taskId: string) {
  return httpClient.get<TaskResponse>(`/tasks/${taskId}`)
}

export function createInProject(projectId: string, req: TaskCreateRequest) {
  return httpClient.post<TaskResponse>(`/projects/${projectId}/tasks`, req)
}

export function createInTaskGroup(taskGroupId: string, req: TaskCreateRequest) {
  return httpClient.post<TaskResponse>(`/task-groups/${taskGroupId}/tasks`, req)
}

export function updateProperty(taskId: string, req: TaskUpdatePropertyRequest) {
  return httpClient.patch<TaskResponse>(`/tasks/${taskId}`, req)
}

// 作業セッションが1件でもあると400（estimate minutes update not allowed）。
export function updateEstimatedMinutes(taskId: string, req: TaskUpdateEstimatedMinutesRequest) {
  return httpClient.patch<TaskResponse>(`/tasks/${taskId}/estimated-minutes`, req)
}

// 未終了セッションがあると400（task finish not allowed）。isFinished:falseで完了解除もできる。
export function updateFinished(taskId: string, req: TaskUpdateFinishedRequest) {
  return httpClient.patch<TaskResponse>(`/tasks/${taskId}/finished`, req)
}

export function updateParent(taskId: string, req: TaskUpdateParentRequest) {
  return httpClient.patch<TaskResponse>(`/tasks/${taskId}/parent`, req)
}

export function updateTags(taskId: string, req: TaskTagsUpdateRequest) {
  return httpClient.put<TaskResponse>(`/tasks/${taskId}/tags`, req)
}

// B1修正済み。作業セッションを持つタスクも204で削除できる。
export function remove(taskId: string) {
  return httpClient.delete<void>(`/tasks/${taskId}`)
}
