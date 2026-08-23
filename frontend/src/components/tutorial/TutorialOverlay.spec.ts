// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TutorialOverlay from './TutorialOverlay.vue'

describe('TutorialOverlay', () => {
  it('darkens the whole screen without a cutout when rect is null', () => {
    const wrapper = mount(TutorialOverlay, { props: { rect: null } })

    const scrim = wrapper.get('.tutorial-scrim')
    expect(scrim.attributes('style')).toContain('clip-path: none')
  })

  it('cuts out the target rect (with padding) when rect is given', () => {
    const wrapper = mount(TutorialOverlay, {
      props: { rect: { top: 100, left: 50, right: 150, bottom: 130 } },
    })

    const scrim = wrapper.get('.tutorial-scrim')
    const style = scrim.attributes('style') ?? ''
    // パディング6px適用後の内周座標(§実装: pad=6)。
    expect(style).toContain('44px 94px')
    expect(style).toContain('156px 136px')
  })

  it('renders both a full-screen blocker and a scrim layer', () => {
    const wrapper = mount(TutorialOverlay, { props: { rect: null } })

    expect(wrapper.find('.tutorial-blocker').exists()).toBe(true)
    expect(wrapper.find('.tutorial-scrim').exists()).toBe(true)
  })
})
