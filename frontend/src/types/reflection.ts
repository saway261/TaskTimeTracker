export interface ReflectionResponse {
  id: number
  taskId: number
  cause: string
  nextAction: string | null
  createdAt: string
  updatedAt: string
}

export interface ReflectionTaskResponse {
  id: number
  title: string
  finishedAt: string
  actualMinutesCached: number | null // 過去データ不整合時など、確定していない場合はnull
  gapMinutesCached: number | null
  gapRateCached: number | null // 単位は「％」（100倍しない）。TaskResponse.gapRateCachedと同じ規約
  reflection: ReflectionResponse | null // 未入力の場合はnull
}

export interface ReflectionTaskGroupResponse {
  id: number
  title: string
  tasks: ReflectionTaskResponse[]
}

export interface ProjectReflectionOverviewResponse {
  projectId: number
  projectTitle: string
  tasks: ReflectionTaskResponse[] // プロジェクト直下の完了タスク
  taskGroups: ReflectionTaskGroupResponse[] // 完了タスクを1件以上含むタスクグループのみ
}

export interface ReflectionRequest {
  cause: string
  nextAction: string | null
}
