// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import type { ReflectionTimelineResponse } from '@/types/analytics'
import ReflectionTimeline from './ReflectionTimeline.vue'

const timeline: ReflectionTimelineResponse = {
  items: [
    {
      taskId: 'task1',
      taskTitle: 'タスク',
      projectId: 'p2',
      projectTitle: 'プロジェクト',
      finishedAt: '2026-08-20T10:00:00',
      estimatedMinutes: 30,
      actualMinutes: 30,
      gapMinutes: 0,
      gapRate: 0,
      outcome: 'ON_TIME',
      causeCategories: [],
      tags: [],
      cause: null,
      nextAction: null,
    },
  ],
  page: 0,
  size: 20,
  totalCount: 21,
  hasNext: true,
}

describe('ReflectionTimeline', () => {
  it('原因・判定フィルターと追加読み込みを通知し、編集導線を表示する', async () => {
    const wrapper = mount(ReflectionTimeline, {
      props: {
        timeline,
        categories: [
          {
            code: 'SCOPE',
            label: '想定外の作業',
            direction: 'OVER',
            nextActionHint: null,
            requiresCause: false,
          },
        ],
        causeCategory: null,
        outcome: 'ALL',
        loading: false,
      },
      global: {
        stubs: {
          RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
          ReflectionTimelineItem: { template: '<article class="timeline-item-stub" />' },
        },
      },
    })
    const selects = wrapper.findAll('select')

    expect(wrapper.get('.edit-link').attributes('href')).toBe('/reflections')
    expect(wrapper.text()).toContain('21件')

    await selects[0].setValue('SCOPE')
    await selects[1].setValue('LATE')
    await wrapper.get('.load-more button').trigger('click')

    expect(wrapper.emitted('causeCategoryChange')?.[0]).toEqual(['SCOPE'])
    expect(wrapper.emitted('outcomeChange')?.[0]).toEqual(['LATE'])
    expect(wrapper.emitted('loadMore')).toHaveLength(1)
  })

  it('該当項目がなければ空状態を表示する', () => {
    const wrapper = mount(ReflectionTimeline, {
      props: {
        timeline: { ...timeline, items: [], totalCount: 0, hasNext: false },
        categories: [],
        causeCategory: null,
        outcome: 'ALL',
        loading: false,
      },
      global: { stubs: ['RouterLink'] },
    })

    expect(wrapper.get('.empty').text()).toBe('条件に一致する振り返りはありません。')
  })
})
