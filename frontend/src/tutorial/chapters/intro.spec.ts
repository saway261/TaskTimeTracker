// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '@/api/authApi'
import { useTutorialStore } from '@/stores/tutorialStore'
import TutorialHost from '@/components/tutorial/TutorialHost.vue'
import TutorialOverlay from '@/components/tutorial/TutorialOverlay.vue'
import { findChapter } from './index'
import { introChapter } from './intro'

vi.mock('@/api/authApi')

const DEMONSTRATIVES = ['ここ', 'この', 'その', 'あの', 'それ', 'そこ', 'あそこ']

describe('introChapter (content)', () => {
  it('has the expected chapter metadata', () => {
    expect(introChapter.id).toBe('intro')
    expect(introChapter.title).toBe('はじめに')
    expect(introChapter.entryRoute).toBe('/projects')
    expect(introChapter.summary.length).toBeGreaterThan(0)
  })

  // 初回ツアー専用の章であり、章選択モーダル(TutorialChapterModal.vue)には現れない
  // (要件 §9.1)。TutorialChapterModal.vue はこのフラグで一覧を絞り込む。
  it('is not replayable from the chapter selection modal', () => {
    expect(introChapter.replayable).toBe(false)
  })

  it('defines all 9 steps from requirements §9.1, in order', () => {
    expect(introChapter.steps.map((s) => s.id)).toEqual([
      'welcome',
      'loop',
      'f-estimate',
      'f-measure',
      'f-reflect',
      'f-analyze',
      'f-tags',
      'replay-entry',
      'handoff',
    ])
  })

  it('keeps the 4 loop steps (f-estimate〜f-analyze) in the order announced by loop', () => {
    const loopStepIds = ['f-estimate', 'f-measure', 'f-reflect', 'f-analyze']
    const indices = loopStepIds.map((id) => introChapter.steps.findIndex((s) => s.id === id))
    expect(indices).toEqual([...indices].sort((a, b) => a - b))
  })

  it('leaves welcome/loop/f-reflect without an anchor (concept-only, center card)', () => {
    const anchorless = introChapter.steps.filter((s) => s.targets === undefined)
    expect(anchorless.map((s) => s.id)).toEqual(['welcome', 'loop', 'f-reflect'])
  })

  it('scopes the handoff anchor to the project list screen, not any of the 12 help buttons', () => {
    const handoff = introChapter.steps.find((s) => s.id === 'handoff')
    expect(handoff?.targets).toEqual(['.project-list-view .help-button'])
  })

  it('is registered in the chapter list', () => {
    expect(findChapter('intro')).toBe(introChapter)
  })

  it('never uses anchor-dependent demonstratives outside of quoted UI labels', () => {
    for (const step of introChapter.steps) {
      const withoutQuotedLabels = step.body.replace(/「[^」]*」/g, '')
      for (const word of DEMONSTRATIVES) {
        expect(withoutQuotedLabels, `step "${step.id}": ${step.body}`).not.toContain(word)
      }
    }
  })

  it('keeps each step body within roughly 3〜4 lines (about 200 characters)', () => {
    for (const step of introChapter.steps) {
      expect(
        step.body.length,
        `step "${step.id}" is ${step.body.length} chars`,
      ).toBeLessThanOrEqual(200)
    }
  })

  it('gives every step a non-empty title distinct from its body', () => {
    for (const step of introChapter.steps) {
      expect(step.title.length, `step "${step.id}"`).toBeGreaterThan(0)
      expect(step.title).not.toBe(step.body)
    }
  })
})

describe('introChapter (replay integration)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    document.body.innerHTML = ''
    vi.mocked(authApi.completeOnboarding)
      .mockReset()
      .mockResolvedValue(undefined as never)
  })

  it('reaches every step without terminating early, with no data on /projects', async () => {
    document.body.innerHTML = `
      <div class="project-list-view">
        <h1>タスク管理</h1>
        <div class="header-actions"></div>
      </div>
    `
    // 初回ツアーは必ずスコープ指定なしで開始する。
    const store = useTutorialStore()
    store.start('intro', 'tour')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    const seenStepIds = await walkAllSteps(wrapper)
    expect(seenStepIds).toEqual(introChapter.steps.map((s) => s.id))
    expect(store.activeChapterId).toBeNull()
    // 初回ツアーの完走は完了記録APIを呼ぶ(要件 §6.3。TutorialHost.spec.tsで詳細検証済み)。
    expect(authApi.completeOnboarding).toHaveBeenCalledTimes(1)
  })

  it('spotlights the real elements for f-estimate/f-measure/f-analyze/f-tags/replay-entry/handoff', async () => {
    document.body.innerHTML = `
      <header class="app-header">
        <nav class="main-nav"></nav>
        <button class="mobile-nav-trigger"></button>
        <button class="timer-menu-trigger"></button>
        <button class="user-menu-trigger"></button>
      </header>
      <div class="project-list-view">
        <h1>
          タスク管理
          <button class="help-button">?</button>
        </h1>
        <div class="header-actions"></div>
      </div>
    `
    // 要素ごとに異なるrectを与える。全要素が同じrectだと、誤って別の要素を指しても
    // テストが気づけない。
    markVisible(document.body.querySelector('.header-actions')!, { top: 10, left: 10 })
    markVisible(document.body.querySelector('.timer-menu-trigger')!, { top: 20, left: 20 })
    markVisible(document.body.querySelector('.main-nav')!, { top: 30, left: 30 })
    markInvisible(document.body.querySelector('.mobile-nav-trigger')!)
    markVisible(document.body.querySelector('.user-menu-trigger')!, { top: 40, left: 40 })
    markVisible(document.body.querySelector('.project-list-view .help-button')!, {
      top: 50,
      left: 50,
    })

    const store = useTutorialStore()
    store.start('intro', 'tour')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    const expectations: Record<string, string> = {
      'f-estimate': '.header-actions',
      'f-measure': '.timer-menu-trigger',
      'f-analyze': '.main-nav',
      'f-tags': '.user-menu-trigger',
      'replay-entry': '.user-menu-trigger',
      handoff: '.project-list-view .help-button',
    }

    for (let guard = 0; guard < introChapter.steps.length; guard += 1) {
      const currentId = introChapter.steps[store.stepIndex]?.id
      const expectedSelector = currentId ? expectations[currentId] : undefined
      if (expectedSelector) {
        const overlay = wrapper.findComponent(TutorialOverlay)
        const expectedEl = document.body.querySelector(expectedSelector) as HTMLElement
        expect(
          overlay.props('rect'),
          `step "${currentId}" should spotlight a real element`,
        ).toEqual({
          top: expectedEl.getBoundingClientRect().top,
          left: expectedEl.getBoundingClientRect().left,
          right: expectedEl.getBoundingClientRect().right,
          bottom: expectedEl.getBoundingClientRect().bottom,
        })
      }
      const nextButton = wrapper
        .findAll('button')
        .find((b) => ['次へ', 'はじめる', '閉じる'].includes(b.text()))
      if (!nextButton) break
      await nextButton.trigger('click')
      await flushPromises()
    }
  })

  // 実際に起こりうる不具合の回帰テスト: ヘルプボタンは12箇所に存在する(実装計画 0-2-20)。
  // handoffのアンカーを画面で絞り込んでいないと、DOM順で最初に見つかった無関係な
  // ヘルプボタン(例: タスク詳細のもの)を誤って指してしまう。
  it('spotlights the project-list help button, not another screen’s help button earlier in the DOM', async () => {
    document.body.innerHTML = `
      <div class="task-detail-view"><button class="help-button">?</button></div>
      <div class="project-list-view">
        <h1><button class="help-button">?</button></h1>
      </div>
    `
    const decoyButton = document.querySelector('.task-detail-view .help-button') as HTMLElement
    const realButton = document.querySelector('.project-list-view .help-button') as HTMLElement
    markVisible(decoyButton, { top: 999, left: 999 })
    markVisible(realButton, { top: 1, left: 1 })

    const store = useTutorialStore()
    store.start('intro', 'tour')
    store.goTo(introChapter.steps.findIndex((s) => s.id === 'handoff'))
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    expect(wrapper.findComponent(TutorialOverlay).props('rect')).toEqual({
      top: 1,
      left: 1,
      right: realButton.getBoundingClientRect().right,
      bottom: realButton.getBoundingClientRect().bottom,
    })
  })
})

async function walkAllSteps(wrapper: ReturnType<typeof mount>): Promise<string[]> {
  const seen: string[] = []
  for (let guard = 0; guard <= introChapter.steps.length; guard += 1) {
    const title = wrapper.find('.step-title')
    if (!title.exists()) break
    const match = introChapter.steps.find((s) => s.title === title.text())
    if (match) seen.push(match.id)
    const nextButton = wrapper
      .findAll('button')
      .find((b) => ['次へ', 'はじめる', '閉じる'].includes(b.text()))
    if (!nextButton) break
    await nextButton.trigger('click')
    await flushPromises()
  }
  return seen
}

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
