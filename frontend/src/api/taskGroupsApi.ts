import { httpClient } from './httpClient'
import type {
  TaskGroupCreateRequest,
  TaskGroupResponse,
  TaskGroupUpdateRequest,
} from '@/types/taskGroup'

export function fetchAllInProject(projectId: number, isFinished?: boolean) {
  return httpClient.get<TaskGroupResponse[]>(`/projects/${projectId}/task-groups`, {
    params: { isFinished },
  })
}

export function fetchById(taskGroupId: number) {
  return httpClient.get<TaskGroupResponse>(`/task-groups/${taskGroupId}`)
}

export function create(projectId: number, req: TaskGroupCreateRequest) {
  return httpClient.post<TaskGroupResponse>(`/projects/${projectId}/task-groups`, req)
}

export function update(taskGroupId: number, req: TaskGroupUpdateRequest) {
  return httpClient.put<TaskGroupResponse>(`/task-groups/${taskGroupId}`, req)
}
