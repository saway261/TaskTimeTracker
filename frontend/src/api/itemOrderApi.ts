import { httpClient } from './httpClient'
import type {
  ProjectItemOrderReplaceRequest,
  ProjectItemOrderResponse,
  TaskGroupItemOrderReplaceRequest,
  TaskGroupItemOrderResponse,
} from '@/types/itemOrder'

export function fetchProjectItemOrder(projectId: number) {
  return httpClient.get<ProjectItemOrderResponse[]>(`/projects/${projectId}/item-order`)
}

// PUTは全置換。対象コンテナの現在の項目を過不足なく含める必要がある（§7.4.2）。
export function replaceProjectItemOrder(projectId: number, req: ProjectItemOrderReplaceRequest) {
  return httpClient.put<ProjectItemOrderResponse[]>(`/projects/${projectId}/item-order`, req)
}

export function fetchTaskGroupItemOrder(taskGroupId: number) {
  return httpClient.get<TaskGroupItemOrderResponse[]>(`/task-groups/${taskGroupId}/item-order`)
}

export function replaceTaskGroupItemOrder(
  taskGroupId: number,
  req: TaskGroupItemOrderReplaceRequest,
) {
  return httpClient.put<TaskGroupItemOrderResponse[]>(`/task-groups/${taskGroupId}/item-order`, req)
}
