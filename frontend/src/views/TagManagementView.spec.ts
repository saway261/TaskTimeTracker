// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia, type Pinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as tagsApi from '@/api/tagsApi'
import type { ApiError } from '@/types/apiError'
import TagManagementView from './TagManagementView.vue'

vi.mock('@/api/tagsApi')

const limitError: ApiError = {
  status: 400,
  kind: 'validation',
  message: 'タグは50件までです。使っていないタグをアーカイブしてください。',
  fieldErrors: { tagLimit: '保有できるタグの上限（50件）に達しています' },
  formErrors: [],
}

describe('TagManagementView', () => {
  let pinia: Pinia

  beforeEach(() => {
    vi.resetAllMocks()
    pinia = createPinia()
    setActivePinia(pinia)
  })

  it('付与数順の一覧・アクティブ件数を表示し、アーカイブ済みは既定で隠す', async () => {
    vi.mocked(tagsApi.fetchAll).mockResolvedValue({
      data: [
        { id: 2, name: '未使用', isArchived: false, assignedTaskCount: 0 },
        { id: 1, name: '調査', isArchived: false, assignedTaskCount: 8 },
        { id: 3, name: '旧タグ', isArchived: true, assignedTaskCount: 3 },
      ],
    } as never)
    const wrapper = mount(TagManagementView, { global: { plugins: [pinia] } })
    await flushPromises()

    expect(tagsApi.fetchAll).toHaveBeenCalledWith(true)
    expect(wrapper.get('.active-count').text()).toContain('2 / 50 件')
    expect(wrapper.findAll('.tag-name').map((name) => name.text())).toEqual(['調査', '未使用'])
    expect(wrapper.text()).not.toContain('旧タグ')
    expect(wrapper.findAll('button').some((button) => button.text().includes('削除'))).toBe(false)

    await wrapper.get('.archive-toggle input').setValue(true)

    expect(wrapper.findAll('.tag-name').map((name) => name.text())).toEqual([
      '調査',
      '旧タグ',
      '未使用',
    ])
  })

  it('一覧からリネームとアーカイブができる', async () => {
    vi.mocked(tagsApi.fetchAll).mockResolvedValue({
      data: [{ id: 1, name: '調査', isArchived: false, assignedTaskCount: 4 }],
    } as never)
    vi.mocked(tagsApi.update).mockResolvedValue({
      data: { id: 1, name: 'リサーチ', isArchived: false, assignedTaskCount: 4 },
    } as never)
    vi.mocked(tagsApi.updateArchived).mockResolvedValue({
      data: { id: 1, name: 'リサーチ', isArchived: true, assignedTaskCount: 4 },
    } as never)
    const wrapper = mount(TagManagementView, { global: { plugins: [pinia] } })
    await flushPromises()

    const renameButton = wrapper
      .findAll('.tag-row button')
      .find((button) => button.text() === '名前を変更')!
    await renameButton.trigger('click')
    await wrapper.get('.rename-form input').setValue('リサーチ')
    await wrapper.get('.rename-form').trigger('submit')
    await flushPromises()

    expect(tagsApi.update).toHaveBeenCalledWith(1, { name: 'リサーチ' })
    expect(wrapper.text()).toContain('リサーチ')

    const archiveButton = wrapper
      .findAll('.tag-row button')
      .find((button) => button.text() === 'アーカイブ')!
    await archiveButton.trigger('click')
    await flushPromises()

    expect(tagsApi.updateArchived).toHaveBeenCalledWith(1, { isArchived: true })
    expect(wrapper.find('.tag-row').exists()).toBe(false)
  })

  it('新規作成が上限で拒否されたら、候補のアーカイブ後に作成を再実行する', async () => {
    vi.mocked(tagsApi.fetchAll).mockResolvedValue({
      data: [{ id: 1, name: '未使用', isArchived: false, assignedTaskCount: 0 }],
    } as never)
    vi.mocked(tagsApi.create)
      .mockRejectedValueOnce(limitError)
      .mockResolvedValueOnce({
        data: { id: 2, name: '新規タグ', isArchived: false, assignedTaskCount: 0 },
      } as never)
    vi.mocked(tagsApi.updateArchived).mockResolvedValue({
      data: { id: 1, name: '未使用', isArchived: true, assignedTaskCount: 0 },
    } as never)
    const wrapper = mount(TagManagementView, { global: { plugins: [pinia] } })
    await flushPromises()

    await wrapper.get('.create-form input').setValue('新規タグ')
    await wrapper.get('.create-form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('タグの上限に達しています')
    await wrapper.get('.tag-limit-resolver input[type="radio"]').setValue()
    const resolveButton = wrapper
      .findAll('.tag-limit-resolver button')
      .find((button) => button.text() === 'アーカイブして作成')
    expect(resolveButton).toBeDefined()
    await resolveButton!.trigger('click')
    await flushPromises()

    expect(tagsApi.updateArchived).toHaveBeenCalledWith(1, { isArchived: true })
    expect(tagsApi.create).toHaveBeenCalledTimes(2)
    expect(wrapper.find('.tag-limit-resolver').exists()).toBe(false)
    expect(wrapper.text()).toContain('新規タグ')
  })

  it('アーカイブ解除が上限で拒否されたら、同じ解決パネルから解除を再実行する', async () => {
    vi.mocked(tagsApi.fetchAll).mockResolvedValue({
      data: [
        { id: 1, name: '未使用', isArchived: false, assignedTaskCount: 0 },
        { id: 2, name: '復帰対象', isArchived: true, assignedTaskCount: 3 },
      ],
    } as never)
    vi.mocked(tagsApi.updateArchived)
      .mockRejectedValueOnce(limitError)
      .mockResolvedValueOnce({
        data: { id: 1, name: '未使用', isArchived: true, assignedTaskCount: 0 },
      } as never)
      .mockResolvedValueOnce({
        data: { id: 2, name: '復帰対象', isArchived: false, assignedTaskCount: 3 },
      } as never)
    const wrapper = mount(TagManagementView, { global: { plugins: [pinia] } })
    await flushPromises()
    await wrapper.get('.archive-toggle input').setValue(true)

    const targetRow = wrapper.findAll('.tag-row').find((row) => row.text().includes('復帰対象'))!
    const restoreButton = targetRow
      .findAll('button')
      .find((button) => button.text() === 'アーカイブを解除')!
    await restoreButton.trigger('click')
    await flushPromises()

    expect(targetRow.text()).toContain('タグの上限に達しています')
    await targetRow.get('.tag-limit-resolver input[type="radio"]').setValue()
    const resolveButton = targetRow
      .findAll('.tag-limit-resolver button')
      .find((button) => button.text() === 'アーカイブして解除')!
    await resolveButton.trigger('click')
    await flushPromises()

    expect(tagsApi.updateArchived).toHaveBeenNthCalledWith(1, 2, { isArchived: false })
    expect(tagsApi.updateArchived).toHaveBeenNthCalledWith(2, 1, { isArchived: true })
    expect(tagsApi.updateArchived).toHaveBeenNthCalledWith(3, 2, { isArchived: false })
    expect(wrapper.find('.tag-limit-resolver').exists()).toBe(false)
  })
})
