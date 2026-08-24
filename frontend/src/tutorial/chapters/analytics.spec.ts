// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useTutorialStore } from '@/stores/tutorialStore'
import TutorialHost from '@/components/tutorial/TutorialHost.vue'
import TutorialOverlay from '@/components/tutorial/TutorialOverlay.vue'
import { findChapter } from './index'
import { analyticsChapter } from './analytics'

const DEMONSTRATIVES = ['ここ', 'この', 'その', 'あの', 'それ', 'そこ', 'あそこ']

describe('analyticsChapter (content)', () => {
  it('has the expected chapter metadata', () => {
    expect(analyticsChapter.id).toBe('analytics')
    expect(analyticsChapter.title).toBe('分析')
    expect(analyticsChapter.replayable).toBe(true)
    expect(analyticsChapter.entryRoute).toBe('/analytics')
    expect(analyticsChapter.summary.length).toBeGreaterThan(0)
  })

  it('defines all 7 steps from requirements §9.4, in order', () => {
    expect(analyticsChapter.steps.map((s) => s.id)).toEqual([
      'prerequisite',
      'filter',
      'excluded',
      'summary',
      'bias-vs-variance',
      'diagnosis',
      'charts',
    ])
  })

  it('keeps the spotlight on .accuracy-summary for both summary and bias-vs-variance', () => {
    const summary = analyticsChapter.steps.find((s) => s.id === 'summary')
    const biasVsVariance = analyticsChapter.steps.find((s) => s.id === 'bias-vs-variance')
    expect(summary?.targets).toEqual(['.accuracy-summary'])
    expect(biasVsVariance?.targets).toEqual(['.accuracy-summary'])
  })

  it('is registered in the chapter list', () => {
    expect(findChapter('analytics')).toBe(analyticsChapter)
  })

  it('never uses anchor-dependent demonstratives outside of quoted UI labels', () => {
    for (const step of analyticsChapter.steps) {
      const withoutQuotedLabels = step.body.replace(/「[^」]*」/g, '')
      for (const word of DEMONSTRATIVES) {
        expect(withoutQuotedLabels, `step "${step.id}": ${step.body}`).not.toContain(word)
      }
    }
  })

  it('keeps each step body within roughly 3〜4 lines (about 200 characters)', () => {
    for (const step of analyticsChapter.steps) {
      expect(
        step.body.length,
        `step "${step.id}" is ${step.body.length} chars`,
      ).toBeLessThanOrEqual(200)
    }
  })

  it('gives every step a non-empty title distinct from its body', () => {
    for (const step of analyticsChapter.steps) {
      expect(step.title.length, `step "${step.id}"`).toBeGreaterThan(0)
      expect(step.title).not.toBe(step.body)
    }
  })
})

describe('analyticsChapter (replay integration)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    document.body.innerHTML = ''
  })

  it('reaches every step without terminating early, on an empty /analytics', async () => {
    document.body.innerHTML = `
      <main class="analytics-view">
        <h1>分析</h1>
        <div class="empty-state"></div>
      </main>
    `
    const store = useTutorialStore()
    store.start('analytics', 'replay')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    const seenStepIds = await walkAllSteps(wrapper)
    expect(seenStepIds).toEqual(analyticsChapter.steps.map((s) => s.id))
    expect(store.activeChapterId).toBeNull()
  })

  it('spotlights real elements for the filter/summary/diagnosis/charts steps', async () => {
    document.body.innerHTML = `
      <main class="analytics-view">
        <h1>分析</h1>
        <section class="analytics-filter-bar">
          <div class="counts"></div>
        </section>
        <section class="accuracy-summary"></section>
        <section class="diagnosis-card"></section>
        <div class="analytics-charts"><section class="chart-card"></section></div>
      </main>
    `
    for (const el of document.body.querySelectorAll<HTMLElement>('*')) markVisible(el)
    // 要素ごとに異なるrectを与える。全要素が同じrectだと、誤って別の要素を指しても
    // テストが気づけない。
    markVisible(document.body.querySelector('.analytics-filter-bar')!, { top: 10, left: 10 })
    markVisible(document.body.querySelector('.counts')!, { top: 20, left: 20 })
    markVisible(document.body.querySelector('.accuracy-summary')!, { top: 30, left: 30 })
    markVisible(document.body.querySelector('.diagnosis-card')!, { top: 40, left: 40 })
    markVisible(document.body.querySelector('.analytics-charts')!, { top: 50, left: 50 })

    const store = useTutorialStore()
    store.start('analytics', 'replay')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    const expectations: Record<string, string> = {
      filter: '.analytics-filter-bar',
      excluded: '.counts',
      summary: '.accuracy-summary',
      'bias-vs-variance': '.accuracy-summary',
      diagnosis: '.diagnosis-card',
      charts: '.analytics-charts',
    }

    for (let guard = 0; guard < analyticsChapter.steps.length; guard += 1) {
      const currentId = analyticsChapter.steps[store.stepIndex]?.id
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

describe('analyticsChapter (scoped replay from a help button)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    document.body.innerHTML = ''
  })

  it('plays the whole chapter when replayed from the analytics screen (all anchors share one scope)', async () => {
    document.body.innerHTML = `
      <main class="analytics-view">
        <h1>分析</h1>
        <section class="analytics-filter-bar"><div class="counts"></div></section>
        <section class="accuracy-summary"></section>
        <section class="diagnosis-card"></section>
        <div class="analytics-charts"><section class="chart-card"></section></div>
      </main>
    `
    for (const el of document.body.querySelectorAll<HTMLElement>('*')) markVisible(el)

    const store = useTutorialStore()
    store.start('analytics', 'replay', '.analytics-view')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    expect(await walkAllSteps(wrapper)).toEqual(analyticsChapter.steps.map((s) => s.id))
  })

  it('falls back to the whole chapter when the scope matches no steps', async () => {
    document.body.innerHTML = `<div class="unrelated-screen"></div>`
    markVisible(document.body.querySelector('.unrelated-screen')!)

    const store = useTutorialStore()
    store.start('analytics', 'replay', '.unrelated-screen')
    const wrapper = mount(TutorialHost, { attachTo: document.body })
    await flushPromises()

    expect(await walkAllSteps(wrapper)).toContain('prerequisite')
  })
})

async function walkAllSteps(wrapper: ReturnType<typeof mount>): Promise<string[]> {
  const seen: string[] = []
  for (let guard = 0; guard <= analyticsChapter.steps.length; guard += 1) {
    const title = wrapper.find('.step-title')
    if (!title.exists()) break
    const match = analyticsChapter.steps.find((s) => s.title === title.text())
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
