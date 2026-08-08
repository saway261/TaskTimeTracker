import { httpClient } from './httpClient'
import type { MemoRequest, MemoResponse } from '@/types/memo'

export function createMemoInProject(projectId: number, req: MemoRequest) {
  return httpClient.post<MemoResponse>(`/projects/${projectId}/memo`, req)
}

export function createMemoInTaskGroup(taskGroupId: number, req: MemoRequest) {
  return httpClient.post<MemoResponse>(`/task-groups/${taskGroupId}/memo`, req)
}

export function updateMemo(id: number, req: MemoRequest) {
  return httpClient.patch<MemoResponse>(`/memo/${id}`, req)
}

export function deleteMemo(id: number) {
  return httpClient.delete<void>(`/memo/${id}`)
}
