// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import type { ScatterPointResponse } from '@/types/analytics'
import EstimateActualScatter from './EstimateActualScatter.vue'

const points: ScatterPointResponse[] = [
  {
    taskId: 1,
    taskTitle: '見積どおりのタスク',
    estimatedMinutes: 10,
    actualMinutes: 10,
    gapRate: 0,
    outcome: 'ON_TIME',
    tags: [],
  },
  {
    taskId: 2,
    taskTitle: '超過したタスク',
    estimatedMinutes: 60,
    actualMinutes: 120,
    gapRate: 100,
    outcome: 'LATE',
    tags: [
      { id: 1, name: '調査' },
      { id: 2, name: 'API' },
    ],
  },
  {
    taskId: 3,
    taskTitle: '短縮したタスク',
    estimatedMinutes: 100,
    actualMinutes: 50,
    gapRate: -50,
    outcome: 'EARLY',
    tags: [],
  },
]

describe('EstimateActualScatter', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('基準線・しきい値帯・傾向線と形状の異なる点を描画する', () => {
    const wrapper = mount(EstimateActualScatter, {
      props: { points, thresholdPercent: 10, factorMedian: 1.2, truncated: false },
    })

    const svg = wrapper.get('svg')
    expect(svg.attributes('role')).toBe('img')
    expect(svg.attributes('aria-describedby')).toBe(wrapper.get('table').attributes('id'))
    expect(wrapper.find('.on-time-zone').exists()).toBe(true)
    expect(wrapper.find('.reference-line').exists()).toBe(true)
    expect(wrapper.find('.trend-line').exists()).toBe(true)
    expect(wrapper.findAll('.scatter-point')).toHaveLength(3)
    expect(wrapper.find('defs path').exists()).toBe(true)
    expect(wrapper.find('defs circle').exists()).toBe(true)
    expect(wrapper.find('defs rect').exists()).toBe(true)
  })

  it('レスポンスのしきい値変更に合わせて網掛け帯を変更する', async () => {
    const wrapper = mount(EstimateActualScatter, {
      props: { points, thresholdPercent: 10, factorMedian: 1.2, truncated: false },
    })
    const initialPoints = wrapper.get('.on-time-zone').attributes('points')

    await wrapper.setProps({ thresholdPercent: 25 })

    expect(wrapper.get('.on-time-zone').attributes('points')).not.toBe(initialPoints)
  })

  it('対数軸へ切り替えても帯・基準線・傾向線を描画する', async () => {
    const wrapper = mount(EstimateActualScatter, {
      props: { points, thresholdPercent: 10, factorMedian: 1.2, truncated: false },
    })
    const linearPointX = wrapper.get('.scatter-point use').attributes('x')

    await wrapper.findAll('.scale-toggle button')[1].trigger('click')

    expect(wrapper.findAll('.scale-toggle button')[1].attributes('aria-pressed')).toBe('true')
    expect(wrapper.get('.on-time-zone').attributes('points')).not.toBe('')
    expect(wrapper.get('.scatter-point use').attributes('x')).not.toBe(linearPointX)
    expect(wrapper.find('.trend-line').exists()).toBe(true)
  })

  it('点をEnterで選択すると詳細モーダルを開く', async () => {
    const wrapper = mount(EstimateActualScatter, {
      attachTo: document.body,
      props: { points, thresholdPercent: 10, factorMedian: 1.2, truncated: false },
    })

    await wrapper.findAll('.scatter-point')[1].trigger('keydown', { key: 'Enter' })

    const dialog = document.body.querySelector('[role="dialog"]')
    expect(dialog).not.toBeNull()
    expect(dialog?.textContent).toContain('超過したタスク')
    expect(dialog?.textContent).toContain('2時間')
    expect(dialog?.textContent).toContain('タグ')
    expect(dialog?.textContent).toContain('調査')
    expect(dialog?.textContent).toContain('API')
    wrapper.unmount()
  })

  it('表示上限を超えた場合は最新500件のみであることを案内する', () => {
    const wrapper = mount(EstimateActualScatter, {
      props: { points, thresholdPercent: 10, factorMedian: 1.2, truncated: true },
    })

    expect(wrapper.get('.truncated-note').text()).toContain('最新500件のみ表示しています')
    expect(wrapper.get('.truncated-note').text()).toContain('期間を絞ってください')
  })
})
