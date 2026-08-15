// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
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
    const wrapper = mount(TaskForm, {
      props: {
        taskGroups,
        showTaskGroupSelector: true,
      },
    })

    const options = wrapper.findAll('select option')
    expect(options.map((option) => option.text())).toEqual([
      'プロジェクト直下（タスクグループに属さない）',
      '開発',
      'テスト',
    ])

    await wrapper.get('input[type="text"]').setValue('新しいタスク')
    await wrapper.get('input[type="number"]').setValue('30')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')?.[0]).toEqual([
      {
        title: '新しいタスク',
        description: null,
        estimatedMinutes: 30,
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
        taskGroupId: 2,
      },
    ])
  })
})
