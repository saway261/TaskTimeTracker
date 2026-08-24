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

  // 文字列の部分一致だけでは、内周の点の並び順(巻き方向)が間違っていても検知できない
  // (実際に一度、同じ回転方向で書いてしまい、穴が開かない不具合を作り込んだ)。
  // clip-pathは非零規則で塗りつぶすため、内周は外周と逆回りでなければ穴にならない。
  // jsdomは実際にラスタライズしないため、座標を靴紐公式(shoelace formula)で
  // 幾何学的に検証する。
  it('winds the inner cutout opposite to the outer rectangle, so the nonzero fill rule actually punches a hole', () => {
    const wrapper = mount(TutorialOverlay, {
      props: { rect: { top: 100, left: 50, right: 150, bottom: 130 } },
    })

    const style = wrapper.get('.tutorial-scrim').attributes('style') ?? ''
    const pointsStr = style.match(/polygon\(([^)]+)\)/)?.[1] ?? ''
    const points = pointsStr.split(',').map((pair) => {
      const [xStr, yStr] = pair.trim().split(/\s+/)
      const toNumber = (v: string) => (/vh|vw/.test(v) ? 10000 : Number.parseFloat(v))
      return [toNumber(xStr), toNumber(yStr)] as [number, number]
    })
    expect(points).toHaveLength(10)

    // 10点構成: 0-3が外周(4番目は0番目に戻る閉じ点)、5-8が内周(9番目は5番目に戻る閉じ点)。
    const outerRing = points.slice(0, 4)
    const innerRing = points.slice(5, 9)

    function shoelaceSign(ring: [number, number][]): number {
      let twiceArea = 0
      for (let i = 0; i < ring.length; i++) {
        const [x0, y0] = ring[i]
        const [x1, y1] = ring[(i + 1) % ring.length]
        twiceArea += x0 * y1 - x1 * y0
      }
      return Math.sign(twiceArea)
    }

    const outerSign = shoelaceSign(outerRing)
    const innerSign = shoelaceSign(innerRing)
    expect(outerSign).not.toBe(0)
    expect(innerSign).not.toBe(0)
    expect(innerSign).toBe(-outerSign)
  })
})
