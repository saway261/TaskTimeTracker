import { describe, expect, it } from 'vitest'
import { DEFAULT_ON_TIME_THRESHOLD_PERCENT, estimateOutcome } from './duration'

describe('estimateOutcome', () => {
  it.each([
    [null, 'unknown'],
    [undefined, 'unknown'],
    [-11, 'early'],
    [-10, 'on-time'],
    [0, 'on-time'],
    [10, 'on-time'],
    [11, 'late'],
  ] as const)('既定のしきい値で誤差比%s%%を%sに分類する', (gapRate, expected) => {
    expect(estimateOutcome(gapRate, DEFAULT_ON_TIME_THRESHOLD_PERCENT)).toBe(expected)
  })

  it('渡したしきい値に応じて判定境界を変更する', () => {
    expect(estimateOutcome(-20, 20)).toBe('on-time')
    expect(estimateOutcome(20, 20)).toBe('on-time')
    expect(estimateOutcome(-20.1, 20)).toBe('early')
    expect(estimateOutcome(20.1, 20)).toBe('late')
  })
})
