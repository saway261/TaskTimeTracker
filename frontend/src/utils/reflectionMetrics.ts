import type { ReflectionTaskResponse } from '@/types/reflection'

type ReflectionMetricTask = Pick<ReflectionTaskResponse, 'actualMinutesCached' | 'gapMinutesCached'>

export interface AggregateReflectionMetrics {
  actualMinutes: number | null
  gapMinutes: number | null
  gapRate: number | null
  includedTaskCount: number
  excludedTaskCount: number
}

/**
 * 完了タスクの確定値から集計実績・誤差・誤差比を求める。
 * 集計誤差比は個別比率の平均ではなく、合計誤差 / 合計見積 × 100 とする。
 */
export function aggregateReflectionMetrics(
  tasks: readonly ReflectionMetricTask[],
): AggregateReflectionMetrics {
  const aggregatableTasks = tasks.filter(
    (task) => task.actualMinutesCached !== null && task.gapMinutesCached !== null,
  )

  if (aggregatableTasks.length === 0) {
    return {
      actualMinutes: null,
      gapMinutes: null,
      gapRate: null,
      includedTaskCount: 0,
      excludedTaskCount: tasks.length,
    }
  }

  const actualMinutes = aggregatableTasks.reduce(
    (total, task) => total + (task.actualMinutesCached as number),
    0,
  )
  const gapMinutes = aggregatableTasks.reduce(
    (total, task) => total + (task.gapMinutesCached as number),
    0,
  )
  // タスク単位で「誤差 = 実績 - 見積」のため、見積は「実績 - 誤差」で復元できる。
  const estimatedMinutes = actualMinutes - gapMinutes

  return {
    actualMinutes,
    gapMinutes,
    gapRate: estimatedMinutes > 0 ? (gapMinutes / estimatedMinutes) * 100 : null,
    includedTaskCount: aggregatableTasks.length,
    excludedTaskCount: tasks.length - aggregatableTasks.length,
  }
}
