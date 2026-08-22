export type ItemType = 'TASK' | 'TASK_GROUP'

// Project直下：TaskとTaskGroupが1つの並び順を共有するため type を持つ。
export interface ProjectItemOrderResponse {
  type: ItemType
  id: string
  position: number
}

export interface ProjectItemOrderItemRequest {
  type: ItemType
  id: string
}

export interface ProjectItemOrderReplaceRequest {
  items: ProjectItemOrderItemRequest[]
}

// TaskGroup配下：Taskのみのため type を持たない。
export interface TaskGroupItemOrderResponse {
  id: string
  position: number
}

export interface TaskGroupItemOrderItemRequest {
  id: string
}

export interface TaskGroupItemOrderReplaceRequest {
  items: TaskGroupItemOrderItemRequest[]
}
