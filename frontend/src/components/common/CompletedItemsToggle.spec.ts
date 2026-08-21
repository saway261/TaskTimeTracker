// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import CompletedItemsToggle from './CompletedItemsToggle.vue'

describe('CompletedItemsToggle', () => {
  it('押すと完了済み項目の表示を有効にする', async () => {
    const wrapper = mount(CompletedItemsToggle, { props: { modelValue: false } })

    expect(wrapper.get('button').attributes('aria-pressed')).toBe('false')
    expect(wrapper.get('.state-label').text()).toBe('非表示')
    await wrapper.get('button').trigger('click')

    expect(wrapper.emitted('update:modelValue')).toEqual([[true]])
  })

  it('表示中に押すと完了済み項目の表示を無効にする', async () => {
    const wrapper = mount(CompletedItemsToggle, { props: { modelValue: true } })

    expect(wrapper.get('button').attributes('aria-pressed')).toBe('true')
    expect(wrapper.get('button').classes()).toContain('active')
    expect(wrapper.get('.state-label').text()).toBe('表示中')
    await wrapper.get('button').trigger('click')

    expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
  })
})
