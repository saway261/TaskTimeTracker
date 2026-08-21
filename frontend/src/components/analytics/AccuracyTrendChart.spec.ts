// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import type { AccuracyTrendPointResponse } from '@/types/analytics'
import AccuracyTrendChart from './AccuracyTrendChart.vue'

const points: AccuracyTrendPointResponse[] = [
  {
    sequence: 10,
    windowFrom: '2026-08-01T10:00:00',
    finishedAt: '2026-08-10T10:00:00',
    factorMedian: 1.1,
    variancePercent: 20,
  },
  {
    sequence: 11,
    windowFrom: '2026-08-02T10:00:00',
    finishedAt: '2026-08-11T10:00:00',
    factorMedian: 0.9,
    variancePercent: 15,
  },
]

describe('AccuracyTrendChart', () => {
  it('件数不足ならグラフを出さず残り件数を表示する', () => {
    const wrapper = mount(AccuracyTrendChart, {
      props: {
        points: [],
        availability: { available: false, requiredCount: 20, currentCount: 14 },
        thresholdPercent: 10,
      },
    })

    expect(wrapper.text()).toContain('あと6件でここに数値が表示されます')
    expect(wrapper.find('svg').exists()).toBe(false)
  })

  it('代表係数とばらつきを上下2段で描画して代替表へ関連付ける', () => {
    const wrapper = mount(AccuracyTrendChart, {
      props: {
        points,
        availability: { available: true, requiredCount: 20, currentCount: 21 },
        thresholdPercent: 10,
      },
    })

    const svgs = wrapper.findAll('svg')
    expect(svgs).toHaveLength(2)
    expect(wrapper.find('.on-time-zone').exists()).toBe(true)
    expect(wrapper.find('.reference-line').exists()).toBe(true)
    expect(wrapper.find('.factor-series').exists()).toBe(true)
    expect(wrapper.find('.variance-series').exists()).toBe(true)
    expect(wrapper.findAll('.factor-point')).toHaveLength(2)
    expect(wrapper.findAll('.variance-point')).toHaveLength(2)
    expect(svgs[0].attributes('aria-describedby')).toBe(wrapper.get('table').attributes('id'))
    expect(svgs[1].attributes('aria-describedby')).toBe(wrapper.get('table').attributes('id'))
  })

  it('点をフォーカスすると窓の期間と値を表示する', async () => {
    const wrapper = mount(AccuracyTrendChart, {
      props: {
        points,
        availability: { available: true, requiredCount: 20, currentCount: 21 },
        thresholdPercent: 10,
      },
    })

    await wrapper.findAll('.factor-point')[0].trigger('focus')

    expect(wrapper.get('.point-detail').text()).toContain('完了順 10')
    expect(wrapper.get('.point-detail').text()).toContain('代表係数 1.1倍')
    expect(wrapper.get('.point-detail').text()).toContain('2026年8月1日')
  })

  it('レスポンスのしきい値変更に合わせて代表係数帯を変更する', async () => {
    const wrapper = mount(AccuracyTrendChart, {
      props: {
        points,
        availability: { available: true, requiredCount: 20, currentCount: 21 },
        thresholdPercent: 10,
      },
    })
    const initialHeight = wrapper.get('.on-time-zone').attributes('height')

    await wrapper.setProps({ thresholdPercent: 20 })

    expect(wrapper.get('.on-time-zone').attributes('height')).not.toBe(initialHeight)
  })
})
