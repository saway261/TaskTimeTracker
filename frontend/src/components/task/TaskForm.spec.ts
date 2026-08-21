// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import { useTagStore } from '@/stores/tagStore'
import type { TaskGroupResponse } from '@/types/taskGroup'
import TaskForm from './TaskForm.vue'

const taskGroups: TaskGroupResponse[] = [
  {
    id: 2,
    projectId: 1,
    title: '開発',
    description: null,
    isFinished: false,
    memos: [],
  },
  {
    id: 3,
    projectId: 1,
    title: 'テスト',
    description: null,
    isFinished: false,
    memos: [],
  },
]

describe('TaskForm', () => {
  it('プロジェクト直下または既存タスクグループを作成先として選べる', async () => {
    const pinia = createPinia()
    const tagStore = useTagStore(pinia)
    tagStore.tags = [{ id: 5, name: '設計', isArchived: false, assignedTaskCount: 3 }]
    tagStore.initialized = true
    const wrapper = mount(TaskForm, {
      props: {
        taskGroups,
        showTaskGroupSelector: true,
      },
      global: { plugins: [pinia] },
    })

    const options = wrapper.findAll('select option')
    expect(options.map((option) => option.text())).toEqual([
      'プロジェクト直下（タスクグループに属さない）',
      '開発',
      'テスト',
    ])

    await wrapper.get('input[type="text"]').setValue('新しいタスク')
    await wrapper.get('input[type="number"]').setValue('30')
    await wrapper.get('.tag-select input').trigger('focus')
    await wrapper.get('.suggestion').trigger('click')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')?.[0]).toEqual([
      {
        title: '新しいタスク',
        description: null,
        estimatedMinutes: 30,
        tagIds: [5],
        taskGroupId: null,
      },
    ])

    await wrapper.get('select').setValue('2')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')?.[1]).toEqual([
      {
        title: '新しいタスク',
        description: null,
        estimatedMinutes: 30,
        tagIds: [5],
        taskGroupId: 2,
      },
    ])
  })

  it('更新時はタグ選択を表示せずタグIDを送らない', () => {
    const wrapper = mount(TaskForm, {
      props: {
        task: {
          id: 10,
          projectId: 1,
          taskGroupId: null,
          title: '既存タスク',
          description: null,
          estimatedMinutes: 30,
          createdAt: '2026-08-22T00:00:00',
          finishedAt: null,
          actualMinutesCached: null,
          gapMinutesCached: null,
          gapRateCached: null,
          memos: [],
          tags: [{ id: 5, name: '設計' }],
        },
      },
    })

    expect(wrapper.find('.tag-select').exists()).toBe(false)
  })
})
