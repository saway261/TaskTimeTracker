import type { TaskResponse } from '@/types/task'

/** finishedAt の有無からタスクの完了状態を導出する（TaskResponse に isFinished は無い）。 */
export function isFinished(task: TaskResponse): boolean {
  return task.finishedAt !== null
}

/** タスク集合の見積時間を合計する。未設定（null）のタスクは0分として扱う。 */
export function sumEstimatedMinutes(
  tasks: readonly Pick<TaskResponse, 'estimatedMinutes'>[],
): number {
  return tasks.reduce((total, task) => total + (task.estimatedMinutes ?? 0), 0)
}

export type TaskParent = { type: 'project'; id: number } | { type: 'taskGroup'; id: number }

/** projectId と taskGroupId は排他のため、どちらに属するかをまとめて返す。 */
export function parentOf(task: TaskResponse): TaskParent {
  return task.projectId !== null
    ? { type: 'project', id: task.projectId }
    : { type: 'taskGroup', id: task.taskGroupId as number }
}
