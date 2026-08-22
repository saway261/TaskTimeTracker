// @vitest-environment jsdom

import { defineComponent, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as tagsApi from '@/api/tagsApi'
import { useTagStore } from '@/stores/tagStore'
import type { ApiError } from '@/types/apiError'
import BaseModal from '@/components/common/BaseModal.vue'
import TagSelect from './TagSelect.vue'

vi.mock('@/api/tagsApi')

function prepareStore() {
  const store = useTagStore()
  store.initialized = true
  store.loadedForUserId = null
  vi.spyOn(store, 'fetchTags').mockResolvedValue()
  return store
}

describe('TagSelect', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    vi.resetAllMocks()
    setActivePinia(createPinia())
    document.body.innerHTML = ''
  })

  it('サジェストを開いたときに最新の付与件数を取得して候補へ反映する', async () => {
    const store = useTagStore()
    store.tags = [{ id: 'tag1', name: '調査', isArchived: false, assignedTaskCount: 0 }]
    store.initialized = true
    store.loadedForUserId = null
    vi.mocked(tagsApi.fetchAll).mockResolvedValue({
      data: [{ id: 'tag1', name: '調査', isArchived: false, assignedTaskCount: 7 }],
    } as never)
    const wrapper = mount(TagSelect, { props: { modelValue: [] } })

    await wrapper.get('input').trigger('focus')
    await flushPromises()

    expect(tagsApi.fetchAll).toHaveBeenCalledWith(true)
    expect(wrapper.get('.suggestion .assigned-count').text()).toBe('7件')
  })

  it('NFKC正規化で候補を絞り、完全一致を先頭にして新規作成を出さない', async () => {
    const store = prepareStore()
    store.tags = [
      { id: 'tag1', name: 'API連携', isArchived: false, assignedTaskCount: 12 },
      { id: 'tag2', name: 'ＡＰＩ', isArchived: false, assignedTaskCount: 2 },
      { id: 'tag3', name: '旧API', isArchived: true, assignedTaskCount: 20 },
    ]
    const wrapper = mount(TagSelect, { props: { modelValue: [] } })

    await wrapper.get('input').setValue('API')

    expect(wrapper.findAll('.suggestion').map((item) => item.text())).toEqual([
      'ＡＰＩ2件',
      'API連携12件',
    ])
    expect(wrapper.find('.create-option').exists()).toBe(false)

    await wrapper.get('input').setValue('旧API')
    expect(wrapper.findAll('.suggestion')).toHaveLength(0)
    expect(wrapper.find('.create-option').exists()).toBe(false)
  })

  it('キーボードで候補を選べる', async () => {
    const store = prepareStore()
    store.tags = [
      { id: 'tag1', name: '設計', isArchived: false, assignedTaskCount: 8 },
      { id: 'tag2', name: '設定', isArchived: false, assignedTaskCount: 5 },
    ]
    const wrapper = mount(TagSelect, { props: { modelValue: [] } })
    const input = wrapper.get('input')

    await input.setValue('設')
    await input.trigger('keydown', { key: 'ArrowDown' })
    expect(input.attributes('aria-activedescendant')).toContain('option-0')
    await input.trigger('keydown', { key: 'Enter' })

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([[{ id: 'tag1', name: '設計' }]])
  })

  it('入力欄からタグを作成し、6件を超えても選択できる', async () => {
    const store = prepareStore()
    const createTag = vi.spyOn(store, 'createTag').mockResolvedValue({
      id: 'tag7',
      name: '新規',
      isArchived: false,
      assignedTaskCount: 0,
    })
    const selected = Array.from({ length: 6 }, (_, index) => ({
      id: `tag${index + 1}`,
      name: `タグ${index + 1}`,
    }))
    const wrapper = mount(TagSelect, { props: { modelValue: selected } })

    await wrapper.get('input').setValue('新規')
    await wrapper.get('.create-option').trigger('click')
    await flushPromises()

    expect(createTag).toHaveBeenCalledWith('新規')
    expect(wrapper.emitted('update:modelValue')?.[0]?.[0]).toHaveLength(7)
  })

  it('追加専用表示では選択済みタグを重複表示しない', () => {
    prepareStore()
    const wrapper = mount(TagSelect, {
      props: {
        modelValue: [{ id: 'tag1', name: '調査' }],
        showSelected: false,
      },
    })

    expect(wrapper.find('.selected-tags').exists()).toBe(false)
    expect(wrapper.find('input[role="combobox"]').exists()).toBe(true)
  })

  it('上限を解決した後に作成を再実行して選択する', async () => {
    const store = prepareStore()
    store.tags = [{ id: 'tag1', name: '未使用', isArchived: false, assignedTaskCount: 0 }]
    const limitError: ApiError = {
      status: 400,
      kind: 'validation',
      message: 'tag limit exceeded',
      fieldErrors: { tagLimit: 'タグは50件までです。' },
      formErrors: [],
    }
    vi.spyOn(store, 'createTag').mockRejectedValueOnce(limitError).mockResolvedValueOnce({
      id: 'tag2',
      name: '新規',
      isArchived: false,
      assignedTaskCount: 0,
    })
    const setArchived = vi.spyOn(store, 'setArchived').mockResolvedValue({
      id: 'tag1',
      name: '未使用',
      isArchived: true,
      assignedTaskCount: 0,
    })
    const wrapper = mount(TagSelect, { props: { modelValue: [] } })

    await wrapper.get('input').setValue('新規')
    await wrapper.get('.create-option').trigger('click')
    await flushPromises()
    expect(wrapper.findComponent({ name: 'TagLimitResolver' }).exists()).toBe(true)

    await wrapper.get('.candidate input').setValue()
    await wrapper.get('.tag-limit-resolver .primary').trigger('click')
    await flushPromises()

    expect(setArchived).toHaveBeenCalledWith('tag1', true)
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([[{ id: 'tag2', name: '新規' }]])
  })

  it('上限解決パネルでEscを押しても親モーダルを閉じず、入力を保持する', async () => {
    const store = prepareStore()
    store.tags = [{ id: 'tag1', name: '未使用', isArchived: false, assignedTaskCount: 0 }]
    const limitError: ApiError = {
      status: 400,
      kind: 'validation',
      message: 'tag limit exceeded',
      fieldErrors: { tagLimit: 'タグは50件までです。' },
      formErrors: [],
    }
    vi.spyOn(store, 'createTag').mockRejectedValue(limitError)
    const Host = defineComponent({
      components: { BaseModal, TagSelect },
      setup() {
        return { opened: ref(true), selected: ref([]) }
      },
      template:
        '<BaseModal v-model="opened" title="タスクを作成"><TagSelect v-model="selected" /></BaseModal>',
    })
    const wrapper = mount(Host, { attachTo: document.body })
    const select = wrapper.findComponent(TagSelect)
    const input = select.get('input')

    await input.setValue('保持する名前')
    await select.get('.create-option').trigger('click')
    await flushPromises()
    expect(select.find('.tag-limit-resolver').exists()).toBe(true)

    await input.trigger('keydown', { key: 'Escape' })
    await flushPromises()

    expect((wrapper.vm as unknown as { opened: boolean }).opened).toBe(true)
    expect(select.find('.tag-limit-resolver').exists()).toBe(false)
    expect((input.element as HTMLInputElement).value).toBe('保持する名前')
    wrapper.unmount()
  })
})
