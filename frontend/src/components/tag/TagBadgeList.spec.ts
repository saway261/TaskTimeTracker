// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TagBadgeList from './TagBadgeList.vue'

const tags = [
  { id: 'tag1', name: '調査' },
  { id: 'tag2', name: '設計' },
  { id: 'tag3', name: '実装' },
  { id: 'tag4', name: '検証' },
  { id: 'tag5', name: '改善' },
]

describe('TagBadgeList', () => {
  it('一覧向けの既定表示では3件を表示し、残りをまとめる', () => {
    const wrapper = mount(TagBadgeList, { props: { tags } })

    expect(wrapper.findAll('.tag-badge')).toHaveLength(3)
    expect(wrapper.get('.remaining-badge').text()).toBe('他2件')
  })

  it('limitがnullなら詳細向けに全件を表示する', () => {
    const wrapper = mount(TagBadgeList, { props: { tags, limit: null } })

    expect(wrapper.findAll('.tag-badge')).toHaveLength(5)
    expect(wrapper.find('.remaining-badge').exists()).toBe(false)
  })

  it('removableなら各タグに削除ボタンを表示して対象IDを通知する', async () => {
    const wrapper = mount(TagBadgeList, {
      props: { tags: tags.slice(0, 2), removable: true },
    })

    const removeButtons = wrapper.findAll('.remove-button')
    expect(removeButtons).toHaveLength(2)
    expect(removeButtons[0].attributes('aria-label')).toBe('調査を外す')

    await removeButtons[0].trigger('click')

    expect(wrapper.emitted('remove')?.[0]).toEqual(['tag1'])
  })
})
