import { describe, expect, it } from 'vitest'
import { sumEstimatedMinutes } from './task'

describe('sumEstimatedMinutes', () => {
  it('タスクの見積時間を合計し、未設定は0分として扱う', () => {
    expect(
      sumEstimatedMinutes([
        { estimatedMinutes: 30 },
        { estimatedMinutes: null },
        { estimatedMinutes: 90 },
      ]),
    ).toBe(120)
  })

  it('タスクがない場合は0分を返す', () => {
    expect(sumEstimatedMinutes([])).toBe(0)
  })
})
