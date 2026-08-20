// @vitest-environment jsdom

import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as taskGroupsApi from '@/api/taskGroupsApi'
import TaskGroupRowMenu from './TaskGroupRowMenu.vue'

vi.mock('@/api/taskGroupsApi')

function mountMenu(finished = false) {
  return mount(TaskGroupRowMenu, {
    props: {
      modelValue: true,
      taskGroupId: 2,
      detailTo: '/projects/1/task-groups/2',
      finished,
      canMoveUp: false,
      canMoveDown: false,
    },
    global: {
      plugins: [createPinia()],
      stubs: {
        Teleport: true,
        RouterLink: {
          props: ['to'],
          template: '<a :href="to"><slot /></a>',
        },
      },
    },
  })
}

describe('TaskGroupRowMenu', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('タスク追加を選ぶと追加イベントを通知してメニューを閉じる', async () => {
    const wrapper = mountMenu()

    const addButton = wrapper
      .findAll('.menu-list button')
      .find((button) => button.text().includes('タスク追加'))
    expect(addButton).toBeDefined()

    await addButton?.trigger('click')

    expect(wrapper.emitted('add-task')).toHaveLength(1)
    expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
  })

  it('未完了のタスクグループには完了にするボタンが表示され、押すと完了状態を更新してメニューを閉じること', async () => {
    vi.mocked(taskGroupsApi.updateFinished).mockResolvedValue({
      data: {
        id: 2,
        projectId: 1,
        title: 'タスクグループ',
        description: null,
        isFinished: true,
        memos: [],
      },
    } as never)
    const wrapper = mountMenu(false)

    const finishButton = wrapper
      .findAll('.menu-list button')
      .find((button) => button.text() === '完了にする')
    expect(finishButton).toBeDefined()

    await finishButton?.trigger('click')
    await flushPromises()

    expect(taskGroupsApi.updateFinished).toHaveBeenCalledWith(2, { isFinished: true })
    expect(wrapper.emitted('update:modelValue')).toContainEqual([false])
  })

  it('完了済みのタスクグループには完了にするボタンを表示しないこと', () => {
    const wrapper = mountMenu(true)

    const finishButton = wrapper
      .findAll('.menu-list button')
      .find((button) => button.text() === '完了にする')
    expect(finishButton).toBeUndefined()
  })
})
