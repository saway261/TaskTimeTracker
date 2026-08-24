// @vitest-environment jsdom

import { createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TaskRowMenu from './TaskRowMenu.vue'

describe('TaskRowMenu', () => {
  it('タスク詳細へのリンクを表示し、選ぶとメニューを閉じる', async () => {
    const wrapper = mount(TaskRowMenu, {
      props: {
        modelValue: true,
        taskId: 'task10',
        detailTo: '/projects/p1/tasks/task10',
        projectId: 'p1',
        containerKey: 'project:p1',
        taskGroups: [],
        finished: false,
        canMoveUp: false,
        canMoveDown: false,
      },
      global: {
        plugins: [createPinia()],
        stubs: {
          Teleport: true,
          RouterLink: {
            props: ['to'],
            emits: ['click'],
            template: '<a :href="to" @click.prevent="$emit(\'click\')"><slot /></a>',
          },
        },
      },
    })

    const detailLink = wrapper.get('.detail-link')
    expect(detailLink.attributes('href')).toBe('/projects/p1/tasks/task10')
    expect(detailLink.text()).toContain('タスクの詳細・編集・メモへ')

    await detailLink.trigger('click')

    expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
  })
})
