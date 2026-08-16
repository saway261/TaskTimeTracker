// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import type { ReflectionTaskGroupResponse } from '@/types/reflection'
import ReflectionTaskGroupSection from './ReflectionTaskGroupSection.vue'

const taskGroup: ReflectionTaskGroupResponse = {
  id: 1,
  title: '実装',
  tasks: [
    {
      id: 10,
      title: '画面実装',
      finishedAt: '2026-08-16T00:00:00',
      actualMinutesCached: 90,
      gapMinutesCached: 30,
      gapRateCached: 50,
      reflection: null,
    },
  ],
}

describe('ReflectionTaskGroupSection', () => {
  it('展開状態のときだけ所属タスクの集計を表示する', async () => {
    const wrapper = mount(ReflectionTaskGroupSection, {
      props: { taskGroup, isOpen: false },
      global: {
        stubs: { ReflectionTaskRow: true },
      },
    })

    expect(wrapper.find('.aggregate-summary').exists()).toBe(false)

    await wrapper.setProps({ isOpen: true })

    const summary = wrapper.get('.aggregate-summary')
    expect(summary.text()).toContain('合計誤差')
    expect(summary.text()).toContain('合計誤差比')
    expect(summary.text()).not.toContain('合計実績')
    expect(summary.get('.estimate-outcome-icon').classes()).toContain('late')
  })
})
