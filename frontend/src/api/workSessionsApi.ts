import { httpClient } from './httpClient'
import type { WorkSession } from '@/types/workSession'

export function fetchAllInTask(taskId: number) {
  return httpClient.get<WorkSession[]>(`/tasks/${taskId}/work-sessions`)
}

// 稼働中セッションは合計に含まれない（サーバのSUMがminutes IS NULLを除外する）。
export function fetchTotalMinutes(taskId: number) {
  return httpClient.get<number>(`/tasks/${taskId}/work-sessions/total-minutes`)
}
