import { httpClient } from './httpClient'
import type { MemoRequest, MemoResponse } from '@/types/memo'

export function createMemoInProject(projectId: string, req: MemoRequest) {
  return httpClient.post<MemoResponse>(`/projects/${projectId}/memo`, req)
}

export function createMemoInTaskGroup(taskGroupId: string, req: MemoRequest) {
  return httpClient.post<MemoResponse>(`/task-groups/${taskGroupId}/memo`, req)
}

export function createMemoInTask(taskId: string, req: MemoRequest) {
  return httpClient.post<MemoResponse>(`/tasks/${taskId}/memo`, req)
}

export function updateMemo(id: string, req: MemoRequest) {
  return httpClient.patch<MemoResponse>(`/memo/${id}`, req)
}

export function deleteMemo(id: string) {
  return httpClient.delete<void>(`/memo/${id}`)
}
