// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { describe, expect, it, vi } from 'vitest'
import * as itemOrderApi from '@/api/itemOrderApi'
import { useTaskStore } from '@/stores/taskStore'
import type { TaskResponse } from '@/types/task'
import type { TaskGroupResponse } from '@/types/taskGroup'
import TaskGroupListItem from './TaskGroupListItem.vue'

vi.mock('@/api/itemOrderApi')

const taskGroup: TaskGroupResponse = {
  id: 10,
  projectId: 1,
  title: '完了タスクだけのグループ',
  description: null,
  isFinished: false,
  memos: [],
}

const finishedTask: TaskResponse = {
  id: 20,
  projectId: null,
  taskGroupId: 10,
  title: '完了したタスク',
  description: null,
  estimatedMinutes: 30,
  createdAt: '2026-08-21T09:00:00+09:00',
  finishedAt: '2026-08-21T10:00:00+09:00',
  actualMinutesCached: 30,
  gapMinutesCached: 0,
  gapRateCached: 0,
  memos: [],
  tags: [],
}

function mountItem(showCompletedTasks: boolean) {
  const pinia = createPinia()
  useTaskStore(pinia).tasks = [finishedTask]
  vi.mocked(itemOrderApi.fetchTaskGroupItemOrder).mockResolvedValue({ data: [] } as never)

  return mount(TaskGroupListItem, {
    props: {
      taskGroup,
      taskGroups: [taskGroup],
      showCompletedTasks,
      canMoveUp: false,
      canMoveDown: false,
    },
    global: {
      plugins: [pinia],
      stubs: {
        TaskListItem: {
          props: ['task'],
          template: '<div class="task-stub">{{ task.title }}</div>',
        },
        TaskGroupRowMenu: true,
        BaseModal: true,
      },
    },
  })
}

describe('TaskGroupListItem', () => {
  it('未完了グループの所属タスクがすべて完了済みで非表示の場合は空メッセージを表示する', async () => {
    const wrapper = mountItem(false)

    await wrapper.get('.row-header').trigger('click')
    await flushPromises()

    expect(wrapper.get('.empty').text()).toBe('タスクがまだありません。')
    expect(wrapper.find('.task-stub').exists()).toBe(false)
  })

  it('完了済みを表示する場合は完了タスクも展開表示する', async () => {
    const wrapper = mountItem(true)

    await wrapper.get('.row-header').trigger('click')
    await flushPromises()

    expect(wrapper.get('.task-stub').text()).toBe('完了したタスク')
    expect(wrapper.find('.empty').exists()).toBe(false)
  })
})
