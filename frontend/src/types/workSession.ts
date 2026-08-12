export type WorkSessionType = 'TIMER' | 'MANUAL'

// DTOではなくEntityがそのまま返る（docs/frontend-implementation-plan.md §5.1）。
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

export interface WorkSessionCreateRequest {
  type: WorkSessionType
  minutes?: number // MANUAL 必須（未指定は400 / field="validManual"）
  startedAt?: string // TIMER 必須（未指定は400 / field="validTimer"）※サーバはDBのNOW()で上書きする
}

export interface WorkSessionUpdateRequest {
  type: WorkSessionType // 必須。typeを変更する導線は作らない。現在の値をそのまま送る
  minutes?: number // MANUAL 必須
  startedAt?: string // TIMER 必須（startedAt/endedAt はセットで必須）
  endedAt?: string // TIMER 必須
}
