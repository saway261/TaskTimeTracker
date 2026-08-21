// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import type { AccuracySummaryResponse } from '@/types/analytics'
import AccuracySummaryTiles from './AccuracySummaryTiles.vue'

const summary: AccuracySummaryResponse = {
  availability: { available: true, requiredCount: 5, currentCount: 12 },
  outcomeBreakdown: { lateCount: 4, onTimeCount: 6, earlyCount: 2 },
  onTimeRate: 50,
  factorMedian: 1.2,
  factorP25: 0.9,
  factorP75: 1.4,
  variancePercent: 25,
  recentTrend: 'IMPROVED',
  recentVariancePercent: 18,
  previousVariancePercent: 30,
}

describe('AccuracySummaryTiles', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('4指標と内訳を表示し、代表係数を誤差率へ変換して判定する', () => {
    const wrapper = mount(AccuracySummaryTiles, { props: { summary } })

    expect(wrapper.findAll('.summary-tile')).toHaveLength(4)
    expect(wrapper.text()).toContain('50%')
    expect(wrapper.text()).toContain('超過 4件')
    expect(wrapper.text()).toContain('見積どおり 6件')
    expect(wrapper.text()).toContain('1.2倍')
    expect(wrapper.get('.factor-value [role="img"]').classes()).toContain('late')
    expect(wrapper.text()).toContain('±25%')
    expect(wrapper.text()).toContain('改善')
  })

  it('件数不足時は残り件数を各指標に表示しつつ判定内訳を残す', () => {
    const wrapper = mount(AccuracySummaryTiles, {
      props: {
        summary: {
          ...summary,
          availability: { available: false, requiredCount: 5, currentCount: 2 },
          onTimeRate: null,
          factorMedian: null,
          variancePercent: null,
          recentTrend: null,
        },
      },
    })

    expect(wrapper.findAll('.metric-unavailable')).toHaveLength(4)
    expect(wrapper.text()).toContain('あと3件でここに数値が表示されます')
    expect(wrapper.text()).toContain('超過 4件')
  })
})
