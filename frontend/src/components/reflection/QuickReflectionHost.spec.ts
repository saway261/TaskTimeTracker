// @vitest-environment jsdom

import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as reflectionsApi from '@/api/reflectionsApi'
import { useNotificationStore } from '@/stores/notificationStore'
import { useQuickReflectionStore } from '@/stores/quickReflectionStore'
import type { ReflectionTaskResponse } from '@/types/reflection'
import QuickReflectionHost from './QuickReflectionHost.vue'

vi.mock('@/api/reflectionsApi')

const unreflectedTask: ReflectionTaskResponse = {
  id: 'task10',
  title: '実装する',
  finishedAt: '2026-08-15T02:00:00',
  actualMinutesCached: 25,
  gapMinutesCached: -35,
  gapRateCached: -58.3,
  reflection: null,
}

const reflectedTask: ReflectionTaskResponse = {
  ...unreflectedTask,
  reflection: {
    id: 1,
    taskId: unreflectedTask.id,
    causeCategories: [],
    cause: '確認不足',
    nextAction: '手順を見直す',
    createdAt: '2026-08-15T03:00:00',
    updatedAt: '2026-08-15T03:00:00',
  },
}

function mountHost() {
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(QuickReflectionHost, {
    global: {
      plugins: [pinia],
      stubs: {
        Teleport: true,
        CauseCategorySelect: { template: '<div class="cause-category-select-stub" />' },
        EstimateOutcomeIcon: true,
      },
    },
  })
}

const payload = { causeCategoryCodes: ['ESTIMATE_MISS'], cause: '確認不足', nextAction: '見直す' }

describe('QuickReflectionHost', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(reflectionsApi.create).mockResolvedValue({ data: {} } as never)
    vi.mocked(reflectionsApi.update).mockResolvedValue({ data: {} } as never)
  })

  it('ストアに対象が入るまで何も表示しない', async () => {
    const wrapper = mountHost()
    await flushPromises()

    expect(wrapper.text()).toBe('')
  })

  it('ストアに対象が入ると、後回しにできる案内つきで振り返りモーダルを開く', async () => {
    const wrapper = mountHost()
    useQuickReflectionStore().open(unreflectedTask)
    await flushPromises()

    expect(wrapper.text()).toContain('振り返りを入力')
    expect(wrapper.text()).toContain('後で入力する場合は✖ボタンで閉じてください')
  })

  it('振り返り未入力なら登録APIを呼ぶ', async () => {
    const wrapper = mountHost()
    const store = useQuickReflectionStore()
    store.open(unreflectedTask)
    await flushPromises()

    wrapper.findComponent({ name: 'ReflectionModal' }).vm.$emit('submit', payload)
    await flushPromises()

    expect(reflectionsApi.create).toHaveBeenCalledWith(unreflectedTask.id, payload)
    expect(reflectionsApi.update).not.toHaveBeenCalled()
    expect(store.task).toBeNull()
  })

  it('振り返り入力済みなら更新APIを呼ぶ', async () => {
    const wrapper = mountHost()
    const store = useQuickReflectionStore()
    store.open(reflectedTask)
    await flushPromises()

    expect(wrapper.text()).toContain('振り返りの詳細・変更')

    wrapper.findComponent({ name: 'ReflectionModal' }).vm.$emit('submit', payload)
    await flushPromises()

    expect(reflectionsApi.update).toHaveBeenCalledWith(reflectedTask.id, payload)
    expect(reflectionsApi.create).not.toHaveBeenCalled()
    expect(store.task).toBeNull()
  })

  // 閉じるだけでは何も送信せず、画面遷移も起こさない。元居た画面がそのまま残ることが要件。
  it('閉じるとAPIを呼ばずに対象だけを解除する', async () => {
    const wrapper = mountHost()
    const store = useQuickReflectionStore()
    store.open(unreflectedTask)
    await flushPromises()

    wrapper.findComponent({ name: 'ReflectionModal' }).vm.$emit('update:modelValue', false)
    await flushPromises()

    expect(store.task).toBeNull()
    expect(reflectionsApi.create).not.toHaveBeenCalled()
    expect(reflectionsApi.update).not.toHaveBeenCalled()
  })

  it('登録に失敗したらモーダルを開いたままエラーを見せる', async () => {
    // ApiError は normalizeError が必ず fieldErrors / formErrors を埋めて返すため、
    // フィクスチャでも省略しない。
    vi.mocked(reflectionsApi.create).mockRejectedValue({
      status: 500,
      kind: 'server',
      message: '失敗しました',
      fieldErrors: {},
      formErrors: [],
    })
    const wrapper = mountHost()
    const store = useQuickReflectionStore()
    store.open(unreflectedTask)
    await flushPromises()

    wrapper.findComponent({ name: 'ReflectionModal' }).vm.$emit('submit', payload)
    await flushPromises()

    expect(store.task).not.toBeNull()
    expect(useNotificationStore().notifications).toHaveLength(0)
  })
})
