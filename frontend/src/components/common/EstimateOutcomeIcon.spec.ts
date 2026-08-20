// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAppSettingsStore } from '@/stores/appSettingsStore'
import EstimateOutcomeIcon from './EstimateOutcomeIcon.vue'

describe('EstimateOutcomeIcon', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it.each([
    [-11, 'early', '見積より早く完了'],
    [11, 'late', '見積を超過'],
    [-10, 'on-time', 'おおむね見積どおり（誤差比±10%以内）'],
    [0, 'on-time', 'おおむね見積どおり（誤差比±10%以内）'],
    [10, 'on-time', 'おおむね見積どおり（誤差比±10%以内）'],
  ] as const)('誤差比%s%%に対応する表情を表示する', (gapRate, outcome, label) => {
    const wrapper = mount(EstimateOutcomeIcon, { props: { gapRate } })
    const icon = wrapper.get('[role="img"]')

    expect(icon.classes()).toContain(outcome)
    expect(icon.attributes('aria-label')).toBe(label)
    expect(icon.find('svg').exists()).toBe(true)
  })

  it('見積より早い場合は驚いた顔、見積どおりの場合は笑顔にする', () => {
    const early = mount(EstimateOutcomeIcon, { props: { gapRate: -20 } })
    const onTime = mount(EstimateOutcomeIcon, { props: { gapRate: 5 } })

    expect(early.find('.surprised-mouth').exists()).toBe(true)
    expect(early.find('.smile-mouth').exists()).toBe(false)
    expect(onTime.find('.smile-mouth').exists()).toBe(true)
  })

  it('誤差が不明な場合はアイコンを表示しない', () => {
    const wrapper = mount(EstimateOutcomeIcon, { props: { gapRate: null } })

    expect(wrapper.find('[role="img"]').exists()).toBe(false)
  })

  it('ストアのしきい値で判定と説明文を変更する', () => {
    useAppSettingsStore().onTimeThresholdPercent = 20

    const wrapper = mount(EstimateOutcomeIcon, { props: { gapRate: 15 } })
    const icon = wrapper.get('[role="img"]')

    expect(icon.classes()).toContain('on-time')
    expect(icon.attributes('aria-label')).toBe('おおむね見積どおり（誤差比±20%以内）')
  })
})
