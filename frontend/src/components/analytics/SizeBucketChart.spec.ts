// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import type { SizeBucketResponse } from '@/types/analytics'
import SizeBucketChart from './SizeBucketChart.vue'

const buckets: SizeBucketResponse[] = [
  { bucketCode: 'M15', label: '〜15分', taskCount: 0, factorMedian: null, onTimeRate: null },
  { bucketCode: 'M30', label: '16〜30分', taskCount: 2, factorMedian: null, onTimeRate: null },
  { bucketCode: 'M60', label: '31〜60分', taskCount: 3, factorMedian: 1.2, onTimeRate: 33.3 },
  { bucketCode: 'M120', label: '61〜120分', taskCount: 4, factorMedian: 0.9, onTimeRate: 50 },
  { bucketCode: 'OVER120', label: '121分〜', taskCount: 5, factorMedian: 1.5, onTimeRate: 20 },
]

describe('SizeBucketChart', () => {
  it('全サイズ帯と基準、件数に応じたデータ状態を表示する', () => {
    const wrapper = mount(SizeBucketChart, { props: { buckets, thresholdPercent: 10 } })

    const svg = wrapper.get('svg')
    expect(svg.attributes('role')).toBe('img')
    expect(svg.attributes('aria-describedby')).toBe(wrapper.get('table').attributes('id'))
    expect(wrapper.findAll('.bucket-row')).toHaveLength(5)
    expect(wrapper.findAll('.factor-bar')).toHaveLength(3)
    expect(wrapper.text()).toContain('データなし')
    expect(wrapper.text()).toContain('データ不足（2件）')
    expect(wrapper.text()).toContain('1.2倍・オンタイム率 33.3%・3件')
    expect(wrapper.find('.reference-line').exists()).toBe(true)
  })

  it('レスポンスのしきい値変更に合わせて縦帯の幅を変える', async () => {
    const wrapper = mount(SizeBucketChart, { props: { buckets, thresholdPercent: 10 } })
    const initialWidth = wrapper.get('.on-time-zone').attributes('width')

    await wrapper.setProps({ thresholdPercent: 20 })

    expect(wrapper.get('.on-time-zone').attributes('width')).not.toBe(initialWidth)
  })
})
