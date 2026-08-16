import { describe, expect, it } from 'vitest'
import { aggregateReflectionMetrics } from './reflectionMetrics'

describe('aggregateReflectionMetrics', () => {
  it('合計実績と合計誤差から、重み付けされた合計誤差比を算出する', () => {
    const metrics = aggregateReflectionMetrics([
      { actualMinutesCached: 60, gapMinutesCached: 0 },
      { actualMinutesCached: 90, gapMinutesCached: 30 },
    ])

    expect(metrics).toEqual({
      actualMinutes: 150,
      gapMinutes: 30,
      gapRate: 25,
      includedTaskCount: 2,
      excludedTaskCount: 0,
    })
  })

  it('確定値が欠けたタスクを集計から除外する', () => {
    const metrics = aggregateReflectionMetrics([
      { actualMinutesCached: 60, gapMinutesCached: 10 },
      { actualMinutesCached: null, gapMinutesCached: null },
    ])

    expect(metrics.includedTaskCount).toBe(1)
    expect(metrics.excludedTaskCount).toBe(1)
    expect(metrics.actualMinutes).toBe(60)
  })
})
