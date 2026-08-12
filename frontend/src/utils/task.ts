import type { TaskResponse } from '@/types/task'

/** finishedAt の有無からタスクの完了状態を導出する（TaskResponse に isFinished は無い）。 */
export function isFinished(task: TaskResponse): boolean {
  return task.finishedAt !== null
}

export type TaskParent = { type: 'project'; id: number } | { type: 'taskGroup'; id: number }

/** projectId と taskGroupId は排他のため、どちらに属するかをまとめて返す。 */
export function parentOf(task: TaskResponse): TaskParent {
  return task.projectId !== null
    ? { type: 'project', id: task.projectId }
    : { type: 'taskGroup', id: task.taskGroupId as number }
}
