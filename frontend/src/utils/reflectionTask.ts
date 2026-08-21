import type { TaskResponse } from '@/types/task'
import type { ReflectionTaskResponse } from '@/types/reflection'

/** 完了直後のクイック振り返り用に、TaskResponse を ReflectionTaskResponse へ変換する。常に未入力（reflection: null）扱い。 */
export function toReflectionTask(task: TaskResponse): ReflectionTaskResponse {
  return {
    id: task.id,
    title: task.title,
    finishedAt: task.finishedAt ?? '',
    actualMinutesCached: task.actualMinutesCached,
    gapMinutesCached: task.gapMinutesCached,
    gapRateCached: task.gapRateCached,
    reflection: null,
  }
}
