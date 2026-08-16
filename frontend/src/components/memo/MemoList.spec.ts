// @vitest-environment jsdom

import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import MemoList from './MemoList.vue'

function mountMemoList(
  onCreate = vi.fn().mockResolvedValue({ id: 2, comment: '追加したメモ' }),
  memos = [{ id: 1, comment: '登録済みのメモ' }],
) {
  return {
    onCreate,
    wrapper: mount(MemoList, {
      props: { memos, onCreate },
      global: {
        plugins: [createPinia()],
        stubs: { Teleport: true },
      },
    }),
  }
}

describe('MemoList', () => {
  it('登録済みメモの右隣にメモ追加ボタンを表示する', () => {
    const { wrapper } = mountMemoList()
    const buttons = wrapper.find('.memo-notes').findAll('button')

    expect(buttons).toHaveLength(2)
    expect(buttons[0].classes()).toContain('memo-note')
    expect(buttons[1].classes()).toContain('add-memo')
    expect(buttons[1].attributes('aria-label')).toBe('メモを追加')
  })

  it('メモが未登録でもメモ追加ボタンを表示する', () => {
    const { wrapper } = mountMemoList(undefined, [])

    expect(wrapper.findAll('.memo-note')).toHaveLength(0)
    expect(wrapper.get('.add-memo').classes()).toContain('empty')
    expect(wrapper.get('.add-memo').text()).toContain('メモを追加')
  })

  it('追加ボタンからモーダルを開き、メモ登録後に閉じる', async () => {
    const { wrapper, onCreate } = mountMemoList()

    await wrapper.find('.add-memo').trigger('click')
    expect(wrapper.get('[role="dialog"]').attributes('aria-label')).toBe('メモを追加')

    await wrapper.get('textarea').setValue('追加したメモ')
    await wrapper.get('.memo-form').trigger('submit')
    await flushPromises()

    expect(onCreate).toHaveBeenCalledWith({ comment: '追加したメモ' })
    expect(wrapper.emitted('created')).toEqual([[{ id: 2, comment: '追加したメモ' }]])
    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
  })
})
