// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it } from 'vitest'
import TutorialCard from './TutorialCard.vue'

function baseProps() {
  return {
    chapterTitle: 'タスク管理',
    stepTitle: 'ステップの見出し',
    body: '本文です。',
    stepIndex: 1,
    stepCount: 3,
    mode: 'replay' as const,
    rect: null,
  }
}

describe('TutorialCard', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('renders the chapter title, progress, step title and body', () => {
    const wrapper = mount(TutorialCard, { props: baseProps() })

    expect(wrapper.text()).toContain('タスク管理')
    expect(wrapper.text()).toContain('2 / 3')
    expect(wrapper.text()).toContain('ステップの見出し')
    expect(wrapper.text()).toContain('本文です。')
  })

  it('shows "次へ" when not on the last step', () => {
    const wrapper = mount(TutorialCard, { props: baseProps() })

    expect(wrapper.text()).toContain('次へ')
  })

  it('shows "はじめる" on the last step in tour mode', () => {
    const wrapper = mount(TutorialCard, {
      props: { ...baseProps(), stepIndex: 2, mode: 'tour' },
    })

    expect(wrapper.text()).toContain('はじめる')
  })

  it('shows "閉じる" on the last step in replay mode', () => {
    const wrapper = mount(TutorialCard, {
      props: { ...baseProps(), stepIndex: 2, mode: 'replay' },
    })

    expect(wrapper.text()).toContain('閉じる')
  })

  it('emits next, back and skip on the respective button clicks', async () => {
    const wrapper = mount(TutorialCard, { props: baseProps() })

    await wrapper.get('.skip-button').trigger('click')
    await wrapper
      .findAll('button')
      .find((b) => b.text() === '戻る')
      ?.trigger('click')
    await wrapper
      .findAll('button')
      .find((b) => b.text() === '次へ')
      ?.trigger('click')

    expect(wrapper.emitted('skip')).toHaveLength(1)
    expect(wrapper.emitted('back')).toHaveLength(1)
    expect(wrapper.emitted('next')).toHaveLength(1)
  })

  it('centers itself when there is no anchor rect', () => {
    const wrapper = mount(TutorialCard, { props: baseProps() })

    expect(wrapper.get('.tutorial-card').classes()).toContain('centered')
  })

  it('does not center itself when an anchor rect is given', () => {
    const wrapper = mount(TutorialCard, {
      props: { ...baseProps(), rect: { top: 10, left: 10, right: 100, bottom: 40 } },
    })

    expect(wrapper.get('.tutorial-card').classes()).not.toContain('centered')
  })

  it('traps Tab focus within the card, wrapping from the last button to the first', () => {
    const wrapper = mount(TutorialCard, { props: baseProps(), attachTo: document.body })
    const buttons = wrapper.findAll('button')
    const first = buttons[0]!.element as HTMLElement
    const last = buttons[buttons.length - 1]!.element as HTMLElement

    last.focus()
    expect(document.activeElement).toBe(last)
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', bubbles: true }))

    expect(document.activeElement).toBe(first)
  })

  it('traps Shift+Tab focus, wrapping from the first button to the last', () => {
    const wrapper = mount(TutorialCard, { props: baseProps(), attachTo: document.body })
    const buttons = wrapper.findAll('button')
    const first = buttons[0]!.element as HTMLElement
    const last = buttons[buttons.length - 1]!.element as HTMLElement

    first.focus()
    expect(document.activeElement).toBe(first)
    window.dispatchEvent(
      new KeyboardEvent('keydown', { key: 'Tab', shiftKey: true, bubbles: true }),
    )

    expect(document.activeElement).toBe(last)
  })

  it('exposes a focus() method that focuses the card root', () => {
    const wrapper = mount(TutorialCard, { props: baseProps(), attachTo: document.body })

    ;(wrapper.vm as unknown as { focus: () => void }).focus()

    expect(document.activeElement).toBe(wrapper.get('.tutorial-card').element)
  })
})
