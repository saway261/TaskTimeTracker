// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TaskGroupRowMenu from './TaskGroupRowMenu.vue'

describe('TaskGroupRowMenu', () => {
  it('タスク追加を選ぶと追加イベントを通知してメニューを閉じる', async () => {
    const wrapper = mount(TaskGroupRowMenu, {
      props: {
        modelValue: true,
        detailTo: '/projects/1/task-groups/2',
        canMoveUp: false,
        canMoveDown: false,
      },
      global: {
        stubs: {
          Teleport: true,
          RouterLink: {
            props: ['to'],
            template: '<a :href="to"><slot /></a>',
          },
        },
      },
    })

    const addButton = wrapper
      .findAll('.menu-list button')
      .find((button) => button.text().includes('タスク追加'))
    expect(addButton).toBeDefined()

    await addButton?.trigger('click')

    expect(wrapper.emitted('add-task')).toHaveLength(1)
    expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
  })
})
