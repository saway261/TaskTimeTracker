// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useTutorialStore } from '@/stores/tutorialStore'
import TutorialHost from '@/components/tutorial/TutorialHost.vue'
import TutorialOverlay from '@/components/tutorial/TutorialOverlay.vue'
import { findChapter } from './index'
import { reflectionsChapter } from './reflections'

const DEMONSTRATIVES = ['ここ', 'この', 'その', 'あの', 'それ', 'そこ', 'あそこ']

describe('reflectionsChapter (content)', () => {
  it('has the expected chapter metadata', () => {
    expect(reflectionsChapter.id).toBe('reflections')
    expect(reflectionsChapter.title).toBe('振り返り')
    expect(reflectionsChapter.replayable).toBe(true)
    expect(reflectionsChapter.entryRoute).toBe('/reflections')
    expect(reflectionsChapter.summary.length).toBeGreaterThan(0)
  })

  it('defines all 7 steps from requirements §9.3, in order', () => {
    expect(reflectionsChapter.steps.map((s) => s.id)).toEqual([
      'why',
      'project-select',
      'target',
      'gap',
      'cause-category',
      'next-action',
      'reopen',
    ])
  })

  it('leaves reopen without an anchor (concept-only, center card)', () => {
    const anchorless = reflectionsChapter.steps.filter((s) => s.targets === undefined)
    expect(anchorless.map((s) => s.id)).toEqual(['reopen'])
  })

  it('is registered in the chapter list', () => {
    expect(findChapter('reflections')).toBe(reflectionsChapter)
  })

  it('never uses anchor-dependent demonstratives outside of quoted UI labels', () => {
    for (const step of reflectionsChapter.steps) {
      const withoutQuotedLabels = step.body.replace(/「[^」]*」/g, '')
      for (const word of DEMONSTRATIVES) {
        expect(withoutQuotedLabels, `step "${step.id}": ${step.body}`).not.toContain(word)
      }
    }
  })

  it('keeps each step body within roughly 3〜4 lines (about 200 characters)', () => {
    for (const step of reflectionsChapter.steps) {
      expect(
        step.body.length,
        `step "${step.id}" is ${step.body.length} chars`,
      ).toBeLessThanOrEqual(200)
    }
  })

  it('gives every step a non-empty title distinct from its body', () => {
    for (const step of reflectionsChapter.steps) {
      expect(step.title.length, `step "${step.id}"`).toBeGreaterThan(0)
      expect(step.title).not.toBe(step.body)
    }
  })
})

describe('reflectionsChapter (replay integration)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    document.body.innerHTML = ''
  })

  it('reaches every step without terminating early, on an empty /reflections', async () => {
    const store = useTutorialStore()
    store.start('reflections', 'replay')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    const seenStepIds = await walkAllSteps(wrapper)
    expect(seenStepIds).toEqual(reflectionsChapter.steps.map((s) => s.id))
    expect(store.activeChapterId).toBeNull()
  })

  it('spotlights real elements on the reflection detail screen and modal', async () => {
    document.body.innerHTML = `
      <div class="reflection-view">
        <h1>振り返り</h1>
        <div class="project-cards"></div>
      </div>
      <div class="reflection-detail-view">
        <div class="reflection-task-row"><div class="row-meta"></div></div>
      </div>
      <div class="reflection-modal">
        <dl class="reference-info"></dl>
        <fieldset class="cause-category-select"></fieldset>
        <div class="next-action-field"></div>
      </div>
    `
    // 要素ごとに異なるrectを与える。全要素が同じrectだと、誤って別の要素を指しても
    // テストが気づけない。
    markVisible(document.body.querySelector('.reflection-view h1')!, { top: 10, left: 10 })
    markVisible(document.body.querySelector('.project-cards')!, { top: 20, left: 20 })
    markVisible(document.body.querySelector('.reflection-task-row')!, { top: 30, left: 30 })
    markVisible(document.body.querySelector('.row-meta')!, { top: 40, left: 40 })
    markVisible(document.body.querySelector('.cause-category-select')!, { top: 50, left: 50 })
    markVisible(document.body.querySelector('.next-action-field')!, { top: 60, left: 60 })

    const store = useTutorialStore()
    store.start('reflections', 'replay')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    // gapは詳細画面側(.row-meta)を優先して指す(targetsの先頭)。
    const expectations: Record<string, string> = {
      why: '.reflection-view h1',
      'project-select': '.project-cards',
      target: '.reflection-task-row',
      gap: '.row-meta',
      'cause-category': '.cause-category-select',
      'next-action': '.next-action-field',
    }

    for (let guard = 0; guard < reflectionsChapter.steps.length; guard += 1) {
      const currentId = reflectionsChapter.steps[store.stepIndex]?.id
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

describe('reflectionsChapter (scoped replay from a help button)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    document.body.innerHTML = ''
  })

  it('plays only the list steps when replayed from the reflection list', async () => {
    document.body.innerHTML = `
      <div class="reflection-view">
        <h1>振り返り</h1>
        <div class="project-cards"></div>
      </div>
    `
    for (const el of document.body.querySelectorAll<HTMLElement>('*')) markVisible(el)

    const store = useTutorialStore()
    store.start('reflections', 'replay', '.reflection-view')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    expect(await walkAllSteps(wrapper)).toEqual(['why', 'project-select'])
  })

  it('plays only the detail-screen steps when replayed from the reflection detail screen', async () => {
    document.body.innerHTML = `
      <div class="reflection-detail-view">
        <div class="reflection-task-row"><div class="row-meta"></div></div>
      </div>
    `
    for (const el of document.body.querySelectorAll<HTMLElement>('*')) markVisible(el)

    const store = useTutorialStore()
    store.start('reflections', 'replay', '.reflection-detail-view')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    expect(await walkAllSteps(wrapper)).toEqual(['target', 'gap'])
  })

  it('plays only the modal steps when replayed from the reflection modal', async () => {
    document.body.innerHTML = `
      <div class="reflection-modal">
        <dl class="reference-info"></dl>
        <fieldset class="cause-category-select"></fieldset>
        <div class="next-action-field"></div>
      </div>
    `
    for (const el of document.body.querySelectorAll<HTMLElement>('*')) markVisible(el)

    const store = useTutorialStore()
    store.start('reflections', 'replay', '.reflection-modal')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    expect(await walkAllSteps(wrapper)).toEqual(['gap', 'cause-category', 'next-action'])
  })

  it('includes reopen only in the unscoped, whole-chapter replay', async () => {
    document.body.innerHTML = `<div class="reflection-view"><h1>振り返り</h1></div>`
    markVisible(document.body.querySelector('h1')!)

    const store = useTutorialStore()
    store.start('reflections', 'replay')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    const seen = await walkAllSteps(wrapper)
    expect(seen).toContain('reopen')
    expect(seen).toEqual(reflectionsChapter.steps.map((s) => s.id))
  })

  it('falls back to the whole chapter when the scope matches no steps', async () => {
    document.body.innerHTML = `<div class="unrelated-screen"></div>`
    markVisible(document.body.querySelector('.unrelated-screen')!)

    const store = useTutorialStore()
    store.start('reflections', 'replay', '.unrelated-screen')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    expect(await walkAllSteps(wrapper)).toContain('why')
  })
})

async function walkAllSteps(wrapper: ReturnType<typeof mount>): Promise<string[]> {
  const seen: string[] = []
  for (let guard = 0; guard <= reflectionsChapter.steps.length; guard += 1) {
    const title = wrapper.find('.step-title')
    if (!title.exists()) break
    const match = reflectionsChapter.steps.find((s) => s.title === title.text())
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
