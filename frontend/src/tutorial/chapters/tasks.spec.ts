// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useTutorialStore } from '@/stores/tutorialStore'
import TutorialHost from '@/components/tutorial/TutorialHost.vue'
import TutorialOverlay from '@/components/tutorial/TutorialOverlay.vue'
import { findChapter } from './index'
import { tasksChapter } from './tasks'

const DEMONSTRATIVES = ['ここ', 'この', 'その', 'あの', 'それ', 'そこ', 'あそこ']

describe('tasksChapter (content)', () => {
  it('has the expected chapter metadata', () => {
    expect(tasksChapter.id).toBe('tasks')
    expect(tasksChapter.title).toBe('タスク管理')
    expect(tasksChapter.replayable).toBe(true)
    expect(tasksChapter.entryRoute).toBe('/projects')
    expect(tasksChapter.summary.length).toBeGreaterThan(0)
  })

  it('defines all 14 steps from requirements §9.2, in order', () => {
    expect(tasksChapter.steps.map((s) => s.id)).toEqual([
      'structure',
      'create-project',
      'add-task',
      'estimate-first',
      'estimate-lock',
      'row-menu',
      'quick-actions',
      'reorder',
      'timer-start',
      'timer-manual',
      'active-timer',
      'memo',
      'tag',
      'finish',
    ])
  })

  it('marks only the reorder step as device-dependent (onMissing: skip)', () => {
    const skippable = tasksChapter.steps.filter((s) => s.onMissing === 'skip')
    expect(skippable.map((s) => s.id)).toEqual(['reorder'])
  })

  it('is registered in the chapter list', () => {
    expect(findChapter('tasks')).toBe(tasksChapter)
  })

  it('never uses anchor-dependent demonstratives outside of quoted UI labels', () => {
    for (const step of tasksChapter.steps) {
      // 「このメモを削除する」のような実際のUIラベルの引用は許可する。引用符の外側だけを検査する。
      const withoutQuotedLabels = step.body.replace(/「[^」]*」/g, '')
      for (const word of DEMONSTRATIVES) {
        expect(withoutQuotedLabels, `step "${step.id}": ${step.body}`).not.toContain(word)
      }
    }
  })

  it('keeps each step body within roughly 3〜4 lines (about 200 characters)', () => {
    for (const step of tasksChapter.steps) {
      expect(
        step.body.length,
        `step "${step.id}" is ${step.body.length} chars`,
      ).toBeLessThanOrEqual(200)
    }
  })

  it('gives every step a non-empty title distinct from its body', () => {
    for (const step of tasksChapter.steps) {
      expect(step.title.length, `step "${step.id}"`).toBeGreaterThan(0)
      expect(step.title).not.toBe(step.body)
    }
  })
})

describe('tasksChapter (replay integration)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    document.body.innerHTML = ''
  })

  it('reaches every step but the skipped one, without terminating early, on an empty /projects', async () => {
    const store = useTutorialStore()
    store.start('tasks', 'replay')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    const seenStepIds: string[] = []
    while (store.activeChapterId !== null && seenStepIds.length <= tasksChapter.steps.length) {
      seenStepIds.push(tasksChapter.steps[store.stepIndex].id)
      const nextButton = wrapper
        .findAll('button')
        .find((b) => ['次へ', 'はじめる', '閉じる'].includes(b.text()))
      if (!nextButton) break
      await nextButton.trigger('click')
      await flushPromises()
    }

    const expectedStepIds = tasksChapter.steps
      .filter((s) => s.onMissing !== 'skip')
      .map((s) => s.id)
    expect(seenStepIds).toEqual(expectedStepIds)
    expect(store.activeChapterId).toBeNull()
  })

  it('spotlights real elements on the task detail page for estimate/timer/memo/tag steps', async () => {
    document.body.innerHTML = `
      <div class="task-detail-view">
        <section class="estimation-section"></section>
        <section class="work-section"></section>
        <section class="memo-list"><button class="add-memo"></button></section>
        <button class="tag-add-button"></button>
      </div>
    `
    // 要素ごとに異なるrectを与える。全要素が同じrectだと、誤って別の要素を指しても
    // テストが気づけない。
    markVisible(document.body.querySelector('.estimation-section')!, { top: 10, left: 10 })
    markVisible(document.body.querySelector('.work-section')!, { top: 20, left: 20 })
    markVisible(document.body.querySelector('.add-memo')!, { top: 30, left: 30 })
    markVisible(document.body.querySelector('.tag-add-button')!, { top: 40, left: 40 })

    const store = useTutorialStore()
    store.start('tasks', 'replay')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    // 実物を指すべきステップID → 期待するアンカーのクラス名。
    // estimate-first はタスク作成モーダル専用になったためタスク詳細画面には現れない。
    const expectations: Record<string, string> = {
      'estimate-lock': '.estimation-section',
      'timer-start': '.work-section',
      'timer-manual': '.work-section',
      memo: '.add-memo',
      tag: '.tag-add-button',
    }

    for (let guard = 0; guard < tasksChapter.steps.length; guard += 1) {
      const currentId = tasksChapter.steps[store.stepIndex]?.id
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

  it('skips the reorder step when the drag handle exists but is hidden (mobile layout)', async () => {
    document.body.innerHTML = `<div class="task-row-wrapper"><span class="drag-handle"></span></div>`
    const handle = document.body.querySelector('.drag-handle') as HTMLElement
    // display:noneで隠れているモバイル相当。getClientRects()は空になる(要件 §8.2)。
    vi.spyOn(handle, 'getClientRects').mockReturnValue([] as unknown as DOMRectList)

    const store = useTutorialStore()
    store.start('tasks', 'replay')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    const seenStepIds: string[] = []
    for (let guard = 0; guard < tasksChapter.steps.length && store.activeChapterId; guard += 1) {
      seenStepIds.push(tasksChapter.steps[store.stepIndex].id)
      const nextButton = wrapper
        .findAll('button')
        .find((b) => ['次へ', 'はじめる', '閉じる'].includes(b.text()))
      if (!nextButton) break
      await nextButton.trigger('click')
      await flushPromises()
    }

    expect(seenStepIds).not.toContain('reorder')
  })
})

describe('tasksChapter (scoped replay from a help button)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    document.body.innerHTML = ''
  })

  // 実際に報告された不具合: タスク操作モーダルから再生しても
  // 「プロジェクトの下にタスクグループ…」という階層の説明から始まってしまう。
  // モーダルを開いても背後の一覧ページはDOMに残るため、可視判定だけでは絞り込めない。
  it('omits the project-list steps when replayed from the task quick action modal', async () => {
    document.body.innerHTML = `
      <header class="app-header"><button class="timer-menu-trigger"></button></header>
      <div class="project-list-view">
        <h1>タスク管理</h1>
        <div class="header-actions"></div>
        <div class="task-row-wrapper"><button class="task-row"></button><button class="menu-button"></button></div>
      </div>
      <div class="task-quick-actions">
        <div class="task-state"><label class="finished-checkbox"></label></div>
        <section class="metrics-section"></section>
        <section class="timer-section"></section>
        <section class="manual-record-section"></section>
        <section class="memo-list"><button class="add-memo"></button></section>
      </div>
    `
    for (const el of document.body.querySelectorAll<HTMLElement>('*')) markVisible(el)

    const store = useTutorialStore()
    store.start('tasks', 'replay', '.task-quick-actions')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    const seenStepIds = await walkAllSteps(wrapper, store)

    // 一覧ページ側の要素しか指さないステップは出ない。
    expect(seenStepIds).not.toContain('structure')
    expect(seenStepIds).not.toContain('create-project')
    expect(seenStepIds).not.toContain('quick-actions')
    expect(seenStepIds).not.toContain('reorder')
    // 見積時間はタスク作成時に入力するものなので、操作モーダルでは説明しない。
    expect(seenStepIds).not.toContain('estimate-first')
    // モーダル内に実物があるステップと、稼働中タイマーの説明が出る。
    expect(seenStepIds).toEqual([
      'estimate-lock',
      'timer-start',
      'timer-manual',
      'active-timer',
      'memo',
      'finish',
    ])
  })

  it('plays only the project-list steps when replayed from an empty project list', async () => {
    document.body.innerHTML = `
      <header class="app-header"><button class="timer-menu-trigger"></button></header>
      <div class="project-list-view">
        <h1>タスク管理</h1>
        <div class="header-actions"></div>
      </div>
    `
    for (const el of document.body.querySelectorAll<HTMLElement>('*')) markVisible(el)

    const store = useTutorialStore()
    store.start('tasks', 'replay', '.project-list-view')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    // 稼働中タイマーの説明は、作業中のタスクを扱う画面(タスク詳細・タスク操作モーダル)
    // だけに置く。一覧画面では出ない。
    expect(await walkAllSteps(wrapper, store)).toEqual(['structure', 'create-project'])
  })

  it('plays only the estimate and tag steps when replayed from the task creation modal', async () => {
    document.body.innerHTML = `
      <header class="app-header"><button class="timer-menu-trigger"></button></header>
      <div class="project-detail-view">
        <div class="task-row-wrapper"><button class="task-row"></button><button class="menu-button"></button></div>
        <section class="memo-list"><button class="add-memo"></button></section>
      </div>
      <form class="task-form">
        <div class="base-input estimate-field"></div>
        <div class="tag-select"></div>
      </form>
    `
    for (const el of document.body.querySelectorAll<HTMLElement>('*')) markVisible(el)

    const store = useTutorialStore()
    store.start('tasks', 'replay', '.task-form')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    // 見積時間の入力はタスク作成時にしか行わないため、作成モーダルでのみ説明する。
    expect(await walkAllSteps(wrapper, store)).toEqual(['estimate-first', 'tag'])
  })

  it('keeps the active timer step on the task detail screen', async () => {
    document.body.innerHTML = `
      <header class="app-header"><button class="timer-menu-trigger"></button></header>
      <div class="task-detail-view"><section class="work-section"></section></div>
    `
    for (const el of document.body.querySelectorAll<HTMLElement>('*')) markVisible(el)

    const store = useTutorialStore()
    store.start('tasks', 'replay', '.task-detail-view')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    expect(await walkAllSteps(wrapper, store)).toContain('active-timer')
  })

  it('still plays the whole chapter when no scope is given (chapter selection modal)', async () => {
    document.body.innerHTML = `<div class="project-list-view"><h1>タスク管理</h1></div>`
    markVisible(document.body.querySelector('h1')!)

    const store = useTutorialStore()
    store.start('tasks', 'replay')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    const seen = await walkAllSteps(wrapper, store)
    // 絞り込まないので、アンカーが見つからないステップも中央カードとして出る。
    expect(seen).toContain('structure')
    expect(seen).toContain('tag')
    expect(seen.length).toBeGreaterThan(10)
  })

  it('falls back to the whole chapter when the scope matches no steps', async () => {
    document.body.innerHTML = `<div class="unrelated-screen"></div>`
    markVisible(document.body.querySelector('.unrelated-screen')!)

    const store = useTutorialStore()
    store.start('tasks', 'replay', '.unrelated-screen')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    // 何も出ないよりは章全体を出す(ヘルプボタンが無反応にならないようにする)。
    expect(await walkAllSteps(wrapper, store)).toContain('structure')
  })
})

async function walkAllSteps(
  wrapper: ReturnType<typeof mount>,
  store: ReturnType<typeof useTutorialStore>,
): Promise<string[]> {
  const seen: string[] = []
  for (let guard = 0; guard <= tasksChapter.steps.length && store.activeChapterId; guard += 1) {
    const title = wrapper.find('.step-title')
    if (!title.exists()) break
    const match = tasksChapter.steps.find((s) => s.title === title.text())
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
