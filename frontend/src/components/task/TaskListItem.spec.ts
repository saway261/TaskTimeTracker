// @vitest-environment jsdom

import { createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import type { TaskResponse } from '@/types/task'
import TaskListItem from './TaskListItem.vue'

const task: TaskResponse = {
  id: 10,
  projectId: 1,
  taskGroupId: null,
  title: '実装する',
  description: null,
  estimatedMinutes: 60,
  createdAt: '2026-08-15T00:00:00',
  finishedAt: null,
  actualMinutesCached: null,
  gapMinutesCached: null,
  gapRateCached: null,
  memos: [],
  tags: [
    { id: 1, name: '調査' },
    { id: 2, name: '設計' },
    { id: 3, name: '実装' },
    { id: 4, name: '検証' },
    { id: 5, name: '改善' },
  ],
}

describe('TaskListItem', () => {
  it('タスク行を押すと詳細遷移ではなく操作モーダルを開く', async () => {
    const wrapper = mount(TaskListItem, {
      props: {
        task,
        to: `/projects/1/tasks/${task.id}`,
        projectId: 1,
        containerKey: 'project:1',
        taskGroups: [],
        canMoveUp: false,
        canMoveDown: false,
      },
      global: {
        plugins: [createPinia()],
        stubs: {
          TaskRowMenu: true,
          TaskQuickActionModal: {
            props: ['modelValue'],
            template: '<div v-if="modelValue" class="quick-action-modal-stub" />',
          },
        },
      },
    })

    expect(wrapper.get('.task-row').element.tagName).toBe('BUTTON')
    expect(wrapper.find('.quick-action-modal-stub').exists()).toBe(false)
    expect(wrapper.findAll('.tag-badge').map((badge) => badge.text())).toEqual([
      'タグ 調査',
      'タグ 設計',
      'タグ 実装',
    ])
    expect(wrapper.get('.remaining-badge').text()).toBe('他2件')

    await wrapper.get('.task-row').trigger('click')

    expect(wrapper.find('.quick-action-modal-stub').exists()).toBe(true)
  })
})
