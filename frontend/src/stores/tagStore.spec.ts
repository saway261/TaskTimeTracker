// @vitest-environment jsdom

import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as tagsApi from '@/api/tagsApi'
import { useAuthStore } from './authStore'
import { useTagStore } from './tagStore'

vi.mock('@/api/tagsApi')

describe('tagStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.resetAllMocks()
  })

  it('アーカイブ済みを含めて一度だけ取得し、選択候補はアクティブなタグに限定する', async () => {
    vi.mocked(tagsApi.fetchAll).mockResolvedValue({
      data: [
        { id: 'tag1', name: '調査', isArchived: false, assignedTaskCount: 4 },
        { id: 'tag2', name: '旧タグ', isArchived: true, assignedTaskCount: 1 },
      ],
    } as never)
    const store = useTagStore()

    await store.fetchTags()
    await store.fetchTags()

    expect(tagsApi.fetchAll).toHaveBeenCalledTimes(1)
    expect(tagsApi.fetchAll).toHaveBeenCalledWith(true)
    expect(store.activeTags.map((tag) => tag.name)).toEqual(['調査'])
  })

  it('作成結果を追加し、既存タグの再利用時は重複させない', async () => {
    const store = useTagStore()
    store.tags = [{ id: 'tag1', name: '調査', isArchived: false, assignedTaskCount: 4 }]
    vi.mocked(tagsApi.create)
      .mockResolvedValueOnce({
        data: { id: 'tag2', name: '設計', isArchived: false, assignedTaskCount: 0 },
      } as never)
      .mockResolvedValueOnce({
        data: { id: 'tag1', name: '調査', isArchived: false, assignedTaskCount: 4 },
      } as never)

    await store.createTag('設計')
    await store.createTag('調査')

    expect(tagsApi.create).toHaveBeenNthCalledWith(1, { name: '設計' })
    expect(store.tags.map((tag) => tag.id)).toEqual(['tag1', 'tag2'])
  })

  it('リネーム結果で該当タグを差し替えて並び順も更新する', async () => {
    const store = useTagStore()
    store.tags = [
      { id: 'tag1', name: '調査', isArchived: false, assignedTaskCount: 2 },
      { id: 'tag2', name: '設計', isArchived: false, assignedTaskCount: 2 },
    ]
    vi.mocked(tagsApi.update).mockResolvedValue({
      data: { id: 'tag1', name: 'アイデア', isArchived: false, assignedTaskCount: 2 },
    } as never)

    await store.renameTag('tag1', 'アイデア')

    expect(tagsApi.update).toHaveBeenCalledWith('tag1', { name: 'アイデア' })
    expect(store.tags.map((tag) => tag.name)).toEqual(['アイデア', '設計'])
  })

  it('アーカイブ結果で該当タグを差し替え、アクティブ候補から除外する', async () => {
    const store = useTagStore()
    store.tags = [{ id: 'tag1', name: '調査', isArchived: false, assignedTaskCount: 4 }]
    vi.mocked(tagsApi.updateArchived).mockResolvedValue({
      data: { id: 'tag1', name: '調査', isArchived: true, assignedTaskCount: 4 },
    } as never)

    await store.setArchived('tag1', true)

    expect(tagsApi.updateArchived).toHaveBeenCalledWith('tag1', { isArchived: true })
    expect(store.tags[0].isArchived).toBe(true)
    expect(store.activeTags).toEqual([])
  })

  it('ログインユーザーが変わったときは一覧を再取得する', async () => {
    vi.mocked(tagsApi.fetchAll)
      .mockResolvedValueOnce({
        data: [{ id: 'tag1', name: 'ユーザー1のタグ', isArchived: false, assignedTaskCount: 1 }],
      } as never)
      .mockResolvedValueOnce({
        data: [{ id: 'tag2', name: 'ユーザー2のタグ', isArchived: false, assignedTaskCount: 2 }],
      } as never)
    const authStore = useAuthStore()
    const store = useTagStore()
    authStore.currentUser = {
      id: 1,
      email: 'user1@example.com',
      passwordChangeRequired: false,
      emailVerified: true,
    }
    await store.fetchTags()

    authStore.currentUser = {
      id: 2,
      email: 'user2@example.com',
      passwordChangeRequired: false,
      emailVerified: true,
    }
    const secondFetch = store.fetchTags()

    expect(store.tags).toEqual([])
    await secondFetch

    expect(tagsApi.fetchAll).toHaveBeenCalledTimes(2)
    expect(store.tags.map((tag) => tag.name)).toEqual(['ユーザー2のタグ'])
  })
})
