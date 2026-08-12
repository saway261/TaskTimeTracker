import { httpClient } from './httpClient'
import type {
  WorkSession,
  WorkSessionCreateRequest,
  WorkSessionUpdateRequest,
} from '@/types/workSession'

export function fetchAllInTask(taskId: number) {
  return httpClient.get<WorkSession[]>(`/tasks/${taskId}/work-sessions`)
}

// 稼働中セッションは合計に含まれない（サーバのSUMがminutes IS NULLを除外する）。
export function fetchTotalMinutes(taskId: number) {
  return httpClient.get<number>(`/tasks/${taskId}/work-sessions/total-minutes`)
}

// レスポンスは200（他の登録系は201）。
export function create(taskId: number, req: WorkSessionCreateRequest) {
  return httpClient.post<WorkSession>(`/tasks/${taskId}/work-sessions`, req)
}

// タイマー停止（フェーズ6で使用）。ボディ不要。
export function end(workSessionId: number) {
  return httpClient.post<WorkSession>(`/work-sessions/${workSessionId}/end`)
}

// 完了済みタスクのセッションは400で拒否される。
export function update(workSessionId: number, req: WorkSessionUpdateRequest) {
  return httpClient.patch<WorkSession>(`/work-sessions/${workSessionId}`, req)
}

export function remove(workSessionId: number) {
  return httpClient.delete<void>(`/work-sessions/${workSessionId}`)
}
