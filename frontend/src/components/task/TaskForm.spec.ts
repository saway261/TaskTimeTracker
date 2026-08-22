// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { describe, expect, it, vi } from 'vitest'
import { useTagStore } from '@/stores/tagStore'
import type { TaskGroupResponse } from '@/types/taskGroup'
import TaskForm from './TaskForm.vue'

const taskGroups: TaskGroupResponse[] = [
  {
    id: 'tg2',
    projectId: 'p1',
    title: '開発',
    description: null,
    isFinished: false,
    memos: [],
  },
  {
    id: 'tg3',
    projectId: 'p1',
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
    tagStore.tags = [{ id: 'tag5', name: '設計', isArchived: false, assignedTaskCount: 3 }]
    tagStore.initialized = true
    vi.spyOn(tagStore, 'fetchTags').mockResolvedValue()
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
    expect(wrapper.get('.selected-task-tags-empty').text()).toBe('なし')

    await wrapper.get('input[type="text"]').setValue('新しいタスク')
    await wrapper.get('input[type="number"]').setValue('30')
    await wrapper.get('.tag-select input').trigger('focus')
    await wrapper.get('.suggestion').trigger('click')
    const selectedTag = wrapper.get('.selected-task-tags .tag-badge')
    expect(selectedTag.text()).toContain('設計')
    expect(selectedTag.get('button').attributes('aria-label')).toBe('設計を外す')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')?.[0]).toEqual([
      {
        title: '新しいタスク',
        description: null,
        estimatedMinutes: 30,
        tagIds: ['tag5'],
        taskGroupId: null,
      },
    ])

    await wrapper.get('select').setValue('tg2')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')?.[1]).toEqual([
      {
        title: '新しいタスク',
        description: null,
        estimatedMinutes: 30,
        tagIds: ['tag5'],
        taskGroupId: 'tg2',
      },
    ])
  })

  it('更新時はタグ選択を表示せずタグIDを送らない', () => {
    const wrapper = mount(TaskForm, {
      props: {
        task: {
          id: 'task10',
          projectId: 'p1',
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
          tags: [{ id: 'tag5', name: '設計' }],
        },
      },
    })

    expect(wrapper.find('.tag-select').exists()).toBe(false)
  })
})
