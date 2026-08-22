// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAppSettingsStore } from '@/stores/appSettingsStore'
import type { ReflectionTaskResponse } from '@/types/reflection'
import ReflectionTaskRow from './ReflectionTaskRow.vue'

const task: ReflectionTaskResponse = {
  id: 'task10',
  title: '画面実装',
  finishedAt: '2026-08-16T00:00:00',
  actualMinutesCached: 90,
  gapMinutesCached: 30,
  gapRateCached: 50,
  reflection: {
    id: 1,
    taskId: 'task10',
    causeCategories: [
      { code: 'TASK_BREAKDOWN', label: '作業の洗い出しが足りなかった' },
      { code: 'OTHER', label: 'その他' },
    ],
    cause: '確認不足',
    nextAction: null,
    createdAt: '2026-08-16T00:00:00',
    updatedAt: '2026-08-16T00:00:00',
  },
}

describe('ReflectionTaskRow', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('振り返りの原因カテゴリを複数バッジ表示する', () => {
    const wrapper = mount(ReflectionTaskRow, { props: { task } })

    const badges = wrapper.findAll('.cause-category-badge')
    expect(badges).toHaveLength(2)
    expect(badges.map((badge) => badge.text())).toEqual(['作業の洗い出しが足りなかった', 'その他'])
  })

  it('原因が未入力の場合は原因の行を表示しない', () => {
    const wrapper = mount(ReflectionTaskRow, {
      props: {
        task: {
          ...task,
          reflection: { ...task.reflection!, cause: null },
        },
      },
    })

    expect(wrapper.text()).not.toContain('原因：')
  })

  it('ストアのしきい値で行の判定表示を変更する', () => {
    useAppSettingsStore().onTimeThresholdPercent = 60

    const wrapper = mount(ReflectionTaskRow, { props: { task } })

    expect(wrapper.get('.outcome-meta').classes()).toContain('on-time')
  })
})
