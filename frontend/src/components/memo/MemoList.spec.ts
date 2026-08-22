// @vitest-environment jsdom

import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import MemoList from './MemoList.vue'

function mountMemoList(
  onCreate = vi.fn().mockResolvedValue({ id: 'memo2', comment: '追加したメモ' }),
  memos = [{ id: 'memo1', comment: '登録済みのメモ' }],
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

  it('2行目以降のメモを折りたたみ、追加ボタンは常に表示する', async () => {
    const memos = [
      { id: 'memo1', comment: '1件目' },
      { id: 'memo2', comment: '2件目' },
      { id: 'memo3', comment: '3件目' },
    ]
    const { wrapper } = mountMemoList(undefined, memos)
    const notes = wrapper.findAll<HTMLElement>('.memo-note')
    Object.defineProperty(notes[0].element, 'offsetTop', { configurable: true, value: 0 })
    Object.defineProperty(notes[1].element, 'offsetTop', { configurable: true, value: 0 })
    Object.defineProperty(notes[2].element, 'offsetTop', { configurable: true, value: 90 })

    window.dispatchEvent(new Event('resize'))
    await wrapper.vm.$nextTick()

    expect(wrapper.get('.memo-items').classes()).toContain('collapsed')
    expect(wrapper.get('.memo-expand').text()).toBe('さらに表示')
    expect(wrapper.find('.add-memo').exists()).toBe(true)

    await wrapper.get('.memo-expand').trigger('click')
    expect(wrapper.get('.memo-items').classes()).not.toContain('collapsed')
    expect(wrapper.get('.memo-expand').text()).toBe('閉じる')
  })

  it('追加ボタンからモーダルを開き、メモ登録後に閉じる', async () => {
    const { wrapper, onCreate } = mountMemoList()

    await wrapper.find('.add-memo').trigger('click')
    expect(wrapper.get('[role="dialog"]').attributes('aria-label')).toBe('メモを追加')

    await wrapper.get('textarea').setValue('追加したメモ')
    await wrapper.get('.memo-form').trigger('submit')
    await flushPromises()

    expect(onCreate).toHaveBeenCalledWith({ comment: '追加したメモ' })
    expect(wrapper.emitted('created')).toEqual([[{ id: 'memo2', comment: '追加したメモ' }]])
    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
  })
})
