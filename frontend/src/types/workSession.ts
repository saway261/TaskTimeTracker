export type WorkSessionType = 'TIMER' | 'MANUAL'

// DTOではなくEntityがそのまま返る（docs/frontend-implementation-plan.md §5.1）。
// フェーズ4では total-minutes とセッション件数の取得にのみ使う。CRUD・タイマー対応はフェーズ5・6。
export interface WorkSession {
  id: number
  taskId: number
  minutes: number | null // 稼働中TIMERは null
  startedAt: string | null // MANUAL は null
  endedAt: string | null // 稼働中TIMERは null
  createdAt: string
  updatedAt: string
  type: WorkSessionType
}
