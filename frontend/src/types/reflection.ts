export type CauseDirection = 'OVER' | 'UNDER' | 'BOTH'

export interface ReflectionCauseCategoryResponse {
  code: string
  label: string
  direction: CauseDirection
  nextActionHint: string | null
  requiresCause: boolean
}

export interface ReflectionCauseCategorySummary {
  code: string
  label: string
}

export interface ReflectionResponse {
  id: number
  taskId: number
  causeCategories: ReflectionCauseCategorySummary[] // 表示順。未設定の場合は空配列
  cause: string | null // 任意項目のため未入力の場合はnull
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
  causeCategoryCodes: string[] // 1件以上3件以下、重複不可
  cause: string | null // 任意。ただし選択した原因カテゴリによっては必須
  nextAction: string | null
}
