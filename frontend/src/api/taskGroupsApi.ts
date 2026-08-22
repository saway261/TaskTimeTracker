import { httpClient } from './httpClient'
import type {
  TaskGroupCreateRequest,
  TaskGroupResponse,
  TaskGroupUpdateFinishedRequest,
  TaskGroupUpdateRequest,
} from '@/types/taskGroup'

export function fetchAllInProject(projectId: string, isFinished?: boolean) {
  return httpClient.get<TaskGroupResponse[]>(`/projects/${projectId}/task-groups`, {
    params: { isFinished },
  })
}

export function fetchById(taskGroupId: string) {
  return httpClient.get<TaskGroupResponse>(`/task-groups/${taskGroupId}`)
}

export function create(projectId: string, req: TaskGroupCreateRequest) {
  return httpClient.post<TaskGroupResponse>(`/projects/${projectId}/task-groups`, req)
}

export function update(taskGroupId: string, req: TaskGroupUpdateRequest) {
  return httpClient.put<TaskGroupResponse>(`/task-groups/${taskGroupId}`, req)
}

export function updateFinished(taskGroupId: string, req: TaskGroupUpdateFinishedRequest) {
  return httpClient.patch<TaskGroupResponse>(`/task-groups/${taskGroupId}/finished`, req)
}
