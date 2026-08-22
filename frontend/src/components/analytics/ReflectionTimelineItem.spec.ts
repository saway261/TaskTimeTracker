// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import type { ReflectionTimelineItemResponse } from '@/types/analytics'
import ReflectionTimelineItem from './ReflectionTimelineItem.vue'

const item: ReflectionTimelineItemResponse = {
  taskId: 'task9',
  taskTitle: '分析画面を実装する',
  projectId: 'p3',
  projectTitle: 'TaskTimeTracker',
  finishedAt: '2026-08-20T10:00:00',
  estimatedMinutes: 60,
  actualMinutes: 90,
  gapMinutes: 30,
  gapRate: 50,
  outcome: 'LATE',
  causeCategories: [
    { code: 'SCOPE', label: '想定外の作業' },
    { code: 'RESEARCH', label: '調査不足' },
  ],
  tags: [
    { id: 'tag1', name: '調査' },
    { id: 'tag2', name: '実装' },
  ],
  cause: 'API仕様の確認に時間がかかった。\n事前調査が足りなかった。',
  nextAction: '実装前にレスポンス例を確認する。',
}

describe('ReflectionTimelineItem', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('タスク導線、判定、複数カテゴリ、振り返り全文を表示する', () => {
    const wrapper = mount(ReflectionTimelineItem, {
      props: { item },
      global: {
        stubs: {
          RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
        },
      },
    })

    expect(wrapper.get('h3 a').attributes('href')).toBe('/projects/p3/tasks/task9')
    expect(wrapper.text()).toContain('超過')
    expect(wrapper.findAll('.category-badge').map((badge) => badge.text())).toEqual([
      '想定外の作業',
      '調査不足',
    ])
    expect(wrapper.findAll('.tag-badge').map((badge) => badge.text())).toEqual([
      'タグ 調査',
      'タグ 実装',
    ])
    expect(wrapper.get('.category-badge').classes()).toContain('category-badge')
    expect(wrapper.get('.tag-badge').classes()).toContain('tag-badge')
    expect(wrapper.text()).toContain(item.cause)
    expect(wrapper.text()).toContain(item.nextAction)
  })

  it('原因が未記入なら控えめな案内を表示する', () => {
    const wrapper = mount(ReflectionTimelineItem, {
      props: { item: { ...item, cause: null, nextAction: null, outcome: null, gapRate: null } },
      global: { stubs: ['RouterLink'] },
    })

    expect(wrapper.get('.not-recorded').text()).toBe('原因の記述はありません。')
    expect(wrapper.get('.outcome-badge.unknown').text()).toBe('-')
    expect(wrapper.text()).not.toContain('改善アクション')
  })
})
