// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '@/api/authApi'
import { useNotificationStore } from '@/stores/notificationStore'
import { useTutorialStore } from '@/stores/tutorialStore'
import type { TutorialChapter, TutorialStep } from '@/tutorial/types'
import TutorialHost from './TutorialHost.vue'
import TutorialOverlay from './TutorialOverlay.vue'
import TutorialCard from './TutorialCard.vue'

vi.mock('@/api/authApi')

const { findChapterMock } = vi.hoisted(() => ({ findChapterMock: vi.fn() }))
vi.mock('@/tutorial/chapters', () => ({ findChapter: findChapterMock }))

function markVisible(el: HTMLElement, rect: Partial<DOMRect> = {}) {
  vi.spyOn(el, 'getClientRects').mockReturnValue([{}] as unknown as DOMRectList)
  vi.spyOn(el, 'getBoundingClientRect').mockReturnValue({
    top: 0,
    left: 0,
    right: 100,
    bottom: 40,
    width: 100,
    height: 40,
    x: 0,
    y: 0,
    toJSON: () => '',
    ...rect,
  })
  el.scrollIntoView = vi.fn()
}

function markInvisible(el: HTMLElement) {
  vi.spyOn(el, 'getClientRects').mockReturnValue([] as unknown as DOMRectList)
}

function makeChapter(steps: TutorialStep[]): TutorialChapter {
  return {
    id: 'tasks',
    title: 'タスク管理',
    summary: '',
    entryRoute: '/projects',
    replayable: true,
    steps,
  }
}

describe('TutorialHost', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    findChapterMock.mockReset()
    vi.mocked(authApi.completeOnboarding)
      .mockReset()
      .mockResolvedValue(undefined as never)
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('shows the spotlight rect matching the resolved anchor', async () => {
    const target = document.createElement('button')
    target.className = 'real-target'
    document.body.appendChild(target)
    markVisible(target, { top: 10, left: 20, right: 120, bottom: 50 })

    findChapterMock.mockReturnValue(
      makeChapter([{ id: 's1', title: 'タイトル', body: '本文', targets: ['.real-target'] }]),
    )
    useTutorialStore().start('tasks', 'replay')

    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    const overlay = wrapper.findComponent(TutorialOverlay)
    expect(overlay.props('rect')).toEqual({ top: 10, left: 20, right: 120, bottom: 50 })
  })

  it('shows a centered card (no rect) when the step has no targets', async () => {
    findChapterMock.mockReturnValue(makeChapter([{ id: 's1', title: 'タイトル', body: '本文' }]))
    useTutorialStore().start('tasks', 'replay')

    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    const overlay = wrapper.findComponent(TutorialOverlay)
    expect(overlay.props('rect')).toBeNull()
    expect(wrapper.findComponent(TutorialCard).props('rect')).toBeNull()
  })

  it('closes the tutorial on Escape', async () => {
    findChapterMock.mockReturnValue(makeChapter([{ id: 's1', title: 'タイトル', body: '本文' }]))
    const store = useTutorialStore()
    store.start('tasks', 'replay')

    mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
    await flushPromises()

    expect(store.activeChapterId).toBeNull()
  })

  it('navigates forward and backward with ArrowRight/ArrowLeft', async () => {
    findChapterMock.mockReturnValue(
      makeChapter([
        { id: 's1', title: 'ステップ1', body: '本文1' },
        { id: 's2', title: 'ステップ2', body: '本文2' },
      ]),
    )
    const store = useTutorialStore()
    store.start('tasks', 'replay')

    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }))
    await flushPromises()
    expect(store.stepIndex).toBe(1)
    expect(wrapper.text()).toContain('ステップ2')

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft', bubbles: true }))
    await flushPromises()
    expect(store.stepIndex).toBe(0)
    expect(wrapper.text()).toContain('ステップ1')
  })

  it('finishes the tutorial when Next is pressed on the last step', async () => {
    findChapterMock.mockReturnValue(makeChapter([{ id: 's1', title: 'ステップ1', body: '本文1' }]))
    const store = useTutorialStore()
    store.start('tasks', 'tour')

    mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }))
    await flushPromises()

    expect(store.activeChapterId).toBeNull()
  })

  it('records onboarding completion and notifies the user when a tour finishes (要件 §6.3)', async () => {
    findChapterMock.mockReturnValue(makeChapter([{ id: 's1', title: 'ステップ1', body: '本文1' }]))
    useTutorialStore().start('tasks', 'tour')

    mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }))
    await flushPromises()

    expect(authApi.completeOnboarding).toHaveBeenCalledTimes(1)
    const notifications = useNotificationStore().notifications
    expect(notifications).toHaveLength(1)
    expect(notifications[0]).toMatchObject({
      kind: 'info',
      message: 'チュートリアルはユーザーメニューからいつでも見られます。',
    })
  })

  it('records onboarding completion when a tour is skipped via Escape (要件 §6.3)', async () => {
    findChapterMock.mockReturnValue(makeChapter([{ id: 's1', title: 'ステップ1', body: '本文1' }]))
    useTutorialStore().start('tasks', 'tour')

    mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
    await flushPromises()

    expect(authApi.completeOnboarding).toHaveBeenCalledTimes(1)
  })

  it('does not record onboarding completion when a chapter replay finishes (要件 §6.3)', async () => {
    findChapterMock.mockReturnValue(makeChapter([{ id: 's1', title: 'ステップ1', body: '本文1' }]))
    useTutorialStore().start('tasks', 'replay')

    mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }))
    await flushPromises()

    expect(authApi.completeOnboarding).not.toHaveBeenCalled()
    expect(useNotificationStore().notifications).toHaveLength(0)
  })

  it('closes the tutorial and shows no error notification when completion recording fails (要件 §6.4)', async () => {
    vi.mocked(authApi.completeOnboarding).mockRejectedValue(new Error('network error'))
    findChapterMock.mockReturnValue(makeChapter([{ id: 's1', title: 'ステップ1', body: '本文1' }]))
    const store = useTutorialStore()
    store.start('tasks', 'tour')

    mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }))
    await flushPromises()

    expect(store.activeChapterId).toBeNull()
    const notifications = useNotificationStore().notifications
    expect(notifications.some((n) => n.kind === 'error')).toBe(false)
    expect(notifications).toHaveLength(1)
    expect(notifications[0].kind).toBe('info')
  })

  it('moves focus into the card on mount and restores it to the opener on close', async () => {
    const opener = document.createElement('button')
    opener.textContent = 'ヘルプ'
    document.body.appendChild(opener)
    opener.focus()
    expect(document.activeElement).toBe(opener)

    findChapterMock.mockReturnValue(makeChapter([{ id: 's1', title: 'ステップ1', body: '本文1' }]))
    useTutorialStore().start('tasks', 'replay')

    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    expect(document.activeElement).toBe(wrapper.get('.tutorial-card').element)

    wrapper.unmount()

    expect(document.activeElement).toBe(opener)
  })

  it('skips onMissing:"skip" steps when navigating forward and backward', async () => {
    findChapterMock.mockReturnValue(
      makeChapter([
        { id: 's1', title: 'ステップ1', body: '本文1' },
        {
          id: 's2',
          title: 'デバイス限定ステップ',
          body: '存在しない機能',
          targets: ['.pc-only'],
          onMissing: 'skip',
        },
        { id: 's3', title: 'ステップ3', body: '本文3' },
      ]),
    )
    const store = useTutorialStore()
    store.start('tasks', 'replay')
    // .pc-only はDOMに存在しないため常に不可視 → resolveAnchorはnullを返す。

    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }))
    await flushPromises()
    expect(store.stepIndex).toBe(2)
    expect(wrapper.text()).toContain('ステップ3')

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft', bubbles: true }))
    await flushPromises()
    expect(store.stepIndex).toBe(0)
    expect(wrapper.text()).toContain('ステップ1')
  })

  it('re-resolves the anchor on resize', async () => {
    const pcNav = document.createElement('nav')
    pcNav.className = 'main-nav'
    const mobileNav = document.createElement('button')
    mobileNav.className = 'mobile-nav-trigger'
    document.body.append(pcNav, mobileNav)
    markVisible(pcNav, { top: 0, left: 0, right: 200, bottom: 40 })
    markInvisible(mobileNav)

    findChapterMock.mockReturnValue(
      makeChapter([
        {
          id: 's1',
          title: 'ナビ',
          body: '本文',
          targets: ['.main-nav', '.mobile-nav-trigger'],
        },
      ]),
    )
    useTutorialStore().start('tasks', 'replay')

    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()
    expect(wrapper.findComponent(TutorialOverlay).props('rect')).toEqual({
      top: 0,
      left: 0,
      right: 200,
      bottom: 40,
    })

    // 幅が変わってPC用ナビが隠れ、モバイル用ハンバーガーが可視になった状況を模す。
    markInvisible(pcNav)
    markVisible(mobileNav, { top: 5, left: 300, right: 340, bottom: 35 })
    window.dispatchEvent(new Event('resize'))
    await flushRaf()

    expect(wrapper.findComponent(TutorialOverlay).props('rect')).toEqual({
      top: 5,
      left: 300,
      right: 340,
      bottom: 35,
    })
  })

  it('does not modify the anchor element style or class', async () => {
    const target = document.createElement('button')
    target.className = 'real-target'
    document.body.appendChild(target)
    markVisible(target)
    const originalClass = target.className
    const originalStyle = target.getAttribute('style')

    findChapterMock.mockReturnValue(
      makeChapter([{ id: 's1', title: 'タイトル', body: '本文', targets: ['.real-target'] }]),
    )
    useTutorialStore().start('tasks', 'replay')

    mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    expect(target.className).toBe(originalClass)
    expect(target.getAttribute('style')).toBe(originalStyle)
  })
})

async function flushRaf() {
  await new Promise((resolve) => requestAnimationFrame(resolve))
  await flushPromises()
}
