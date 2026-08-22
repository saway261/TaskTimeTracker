// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useTagStore } from '@/stores/tagStore'
import TagLimitResolver from './TagLimitResolver.vue'

describe('TagLimitResolver', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('候補を付与数の少ない順に5件提示し、アーカイブ後に元の操作を再実行する', async () => {
    const store = useTagStore()
    store.tags = [
      { id: 'tag1', name: '5件', isArchived: false, assignedTaskCount: 5 },
      { id: 'tag2', name: '0件', isArchived: false, assignedTaskCount: 0 },
      { id: 'tag3', name: '3件', isArchived: false, assignedTaskCount: 3 },
      { id: 'tag4', name: '1件', isArchived: false, assignedTaskCount: 1 },
      { id: 'tag5', name: '2件', isArchived: false, assignedTaskCount: 2 },
      { id: 'tag6', name: '4件', isArchived: false, assignedTaskCount: 4 },
    ]
    const setArchived = vi.spyOn(store, 'setArchived').mockResolvedValue({
      id: 'tag2',
      name: '0件',
      isArchived: true,
      assignedTaskCount: 0,
    })
    const retry = vi.fn().mockResolvedValue(undefined)
    const wrapper = mount(TagLimitResolver, {
      props: { actionLabel: '作成', retry },
    })

    const candidates = wrapper.findAll('.candidate')
    expect(candidates).toHaveLength(5)
    expect(candidates.map((candidate) => candidate.get('input').attributes('value'))).toEqual([
      'tag2',
      'tag4',
      'tag5',
      'tag3',
      'tag6',
    ])
    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)

    await candidates[0].get('input').setValue()
    await wrapper.get('.primary').trigger('click')
    await flushPromises()

    expect(setArchived).toHaveBeenCalledWith('tag2', true)
    expect(retry).toHaveBeenCalledOnce()
    expect(setArchived.mock.invocationCallOrder[0]).toBeLessThan(retry.mock.invocationCallOrder[0])
    expect(wrapper.emitted('resolved')).toHaveLength(1)
  })
})
