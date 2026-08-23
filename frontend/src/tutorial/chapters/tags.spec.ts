// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useTutorialStore } from '@/stores/tutorialStore'
import TutorialHost from '@/components/tutorial/TutorialHost.vue'
import TutorialOverlay from '@/components/tutorial/TutorialOverlay.vue'
import { findChapter } from './index'
import { tagsChapter } from './tags'

const DEMONSTRATIVES = ['ここ', 'この', 'その', 'あの', 'それ', 'そこ', 'あそこ']

describe('tagsChapter (content)', () => {
  it('has the expected chapter metadata', () => {
    expect(tagsChapter.id).toBe('tags')
    expect(tagsChapter.title).toBe('タグ管理')
    expect(tagsChapter.replayable).toBe(true)
    expect(tagsChapter.entryRoute).toBe('/tags')
    expect(tagsChapter.summary.length).toBeGreaterThan(0)
  })

  it('defines all 8 steps from requirements §9.5, in order', () => {
    expect(tagsChapter.steps.map((s) => s.id)).toEqual([
      'purpose',
      'cross-project',
      'preset',
      'rename',
      'create',
      'limit',
      'assign',
      'archive',
    ])
  })

  it('presents preset before rename, so the presets read as a starting point, not a fixed set', () => {
    const presetIndex = tagsChapter.steps.findIndex((s) => s.id === 'preset')
    const renameIndex = tagsChapter.steps.findIndex((s) => s.id === 'rename')
    expect(presetIndex).toBeLessThan(renameIndex)
  })

  it('states the actual preset tag names and the 50-tag limit', () => {
    const preset = tagsChapter.steps.find((s) => s.id === 'preset')
    const limit = tagsChapter.steps.find((s) => s.id === 'limit')
    expect(preset?.body).toContain('調査・計画')
    expect(preset?.body).toContain('環境構築')
    expect(preset?.body).toContain('手作業')
    expect(limit?.body).toContain('50件')
  })

  it('never describes the unimplemented tag-based estimate reference feature', () => {
    for (const step of tagsChapter.steps) {
      expect(step.body).not.toContain('見積もり')
    }
  })

  it('is registered in the chapter list', () => {
    expect(findChapter('tags')).toBe(tagsChapter)
  })

  it('never uses anchor-dependent demonstratives outside of quoted UI labels', () => {
    for (const step of tagsChapter.steps) {
      const withoutQuotedLabels = step.body.replace(/「[^」]*」/g, '')
      for (const word of DEMONSTRATIVES) {
        expect(withoutQuotedLabels, `step "${step.id}": ${step.body}`).not.toContain(word)
      }
    }
  })

  it('keeps each step body within roughly 3〜4 lines (about 200 characters)', () => {
    for (const step of tagsChapter.steps) {
      expect(
        step.body.length,
        `step "${step.id}" is ${step.body.length} chars`,
      ).toBeLessThanOrEqual(200)
    }
  })

  it('gives every step a non-empty title distinct from its body', () => {
    for (const step of tagsChapter.steps) {
      expect(step.title.length, `step "${step.id}"`).toBeGreaterThan(0)
      expect(step.title).not.toBe(step.body)
    }
  })
})

describe('tagsChapter (replay integration)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    document.body.innerHTML = ''
  })

  it('reaches every step without terminating early, on an empty /tags', async () => {
    document.body.innerHTML = `
      <main class="tag-management-view">
        <header class="page-header"><h1>タグ管理</h1></header>
      </main>
    `
    const store = useTutorialStore()
    store.start('tags', 'replay')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    const seenStepIds = await walkAllSteps(wrapper)
    expect(seenStepIds).toEqual(tagsChapter.steps.map((s) => s.id))
    expect(store.activeChapterId).toBeNull()
  })

  it('spotlights real elements on the tag management screen and the task detail screen', async () => {
    document.body.innerHTML = `
      <main class="tag-management-view">
        <header class="page-header"><h1>タグ管理</h1></header>
        <section class="create-section"></section>
        <ul class="tag-list">
          <li class="tag-row"><div class="row-actions"></div></li>
        </ul>
        <p class="active-count"></p>
        <label class="archive-toggle"></label>
      </main>
      <div class="task-detail-view">
        <button class="tag-add-button"></button>
      </div>
    `
    // 要素ごとに異なるrectを与える。全要素が同じrectだと、誤って別の要素を指しても
    // テストが気づけない。
    markVisible(document.body.querySelector('.tag-management-view h1')!, { top: 10, left: 10 })
    markVisible(document.body.querySelector('.page-header')!, { top: 20, left: 20 })
    markVisible(document.body.querySelector('.tag-list')!, { top: 30, left: 30 })
    markVisible(document.body.querySelector('.row-actions')!, { top: 40, left: 40 })
    markVisible(document.body.querySelector('.create-section')!, { top: 50, left: 50 })
    markVisible(document.body.querySelector('.active-count')!, { top: 60, left: 60 })
    markVisible(document.body.querySelector('.tag-add-button')!, { top: 70, left: 70 })
    markVisible(document.body.querySelector('.archive-toggle')!, { top: 80, left: 80 })

    const store = useTutorialStore()
    store.start('tags', 'replay')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    const expectations: Record<string, string> = {
      purpose: '.tag-management-view h1',
      'cross-project': '.page-header',
      preset: '.tag-list',
      rename: '.row-actions',
      create: '.create-section',
      limit: '.active-count',
      assign: '.tag-add-button',
      archive: '.archive-toggle',
    }

    for (let guard = 0; guard < tagsChapter.steps.length; guard += 1) {
      const currentId = tagsChapter.steps[store.stepIndex]?.id
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
})

describe('tagsChapter (scoped replay from a help button)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    document.body.innerHTML = ''
  })

  // 実際に報告された設計上の判断: assignのアンカー(.tag-add-button)はタスク詳細画面にあり、
  // タグ管理画面の配下にない。タグ管理画面からのスコープ再生では対象外になるのが意図どおり。
  it('omits assign when replayed from the tag management screen (its anchor lives on the task detail screen)', async () => {
    document.body.innerHTML = `
      <main class="tag-management-view">
        <header class="page-header"><h1>タグ管理</h1></header>
        <section class="create-section"></section>
        <ul class="tag-list">
          <li class="tag-row"><div class="row-actions"></div></li>
        </ul>
        <p class="active-count"></p>
        <label class="archive-toggle"></label>
      </main>
    `
    for (const el of document.body.querySelectorAll<HTMLElement>('*')) markVisible(el)

    const store = useTutorialStore()
    store.start('tags', 'replay', '.tag-management-view')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    const seen = await walkAllSteps(wrapper)
    expect(seen).not.toContain('assign')
    expect(seen).toEqual([
      'purpose',
      'cross-project',
      'preset',
      'rename',
      'create',
      'limit',
      'archive',
    ])
  })

  it('plays assign only in the unscoped, whole-chapter replay', async () => {
    document.body.innerHTML = `
      <main class="tag-management-view"><h1>タグ管理</h1></main>
      <div class="task-detail-view"><button class="tag-add-button"></button></div>
    `
    for (const el of document.body.querySelectorAll<HTMLElement>('*')) markVisible(el)

    const store = useTutorialStore()
    store.start('tags', 'replay')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    expect(await walkAllSteps(wrapper)).toContain('assign')
  })

  it('falls back to the whole chapter when the scope matches no steps', async () => {
    document.body.innerHTML = `<div class="unrelated-screen"></div>`
    markVisible(document.body.querySelector('.unrelated-screen')!)

    const store = useTutorialStore()
    store.start('tags', 'replay', '.unrelated-screen')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    expect(await walkAllSteps(wrapper)).toContain('purpose')
  })
})

async function walkAllSteps(wrapper: ReturnType<typeof mount>): Promise<string[]> {
  const seen: string[] = []
  for (let guard = 0; guard <= tagsChapter.steps.length; guard += 1) {
    const title = wrapper.find('.step-title')
    if (!title.exists()) break
    const match = tagsChapter.steps.find((s) => s.title === title.text())
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
