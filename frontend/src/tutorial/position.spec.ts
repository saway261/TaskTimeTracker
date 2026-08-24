import { describe, expect, it } from 'vitest'
import { resolveCardPosition } from './position'

const viewport = { width: 1000, height: 800 }
const card = { width: 200, height: 100 }

describe('resolveCardPosition', () => {
  it('places the card below the anchor when it fits', () => {
    const anchor = { top: 100, left: 100, right: 200, bottom: 130 }

    const result = resolveCardPosition(anchor, card, viewport)

    expect(result).toEqual({ top: 142, left: 100 })
  })

  it('places the card above the anchor when below would overflow the viewport', () => {
    const anchor = { top: 720, left: 100, right: 200, bottom: 750 }

    const result = resolveCardPosition(anchor, card, viewport)

    expect(result).toEqual({ top: 608, left: 100 })
  })

  it('places the card to the right when above and below both overflow', () => {
    // 縦に長い(ほぼ画面いっぱいの)対象で上下どちらにも収まらず、右には収まるケース。
    const anchor = { top: 50, left: 0, right: 50, bottom: 750 }

    const result = resolveCardPosition(anchor, card, viewport)

    expect(result).toEqual({ top: 50, left: 62 })
  })

  it('clamps within the viewport when no placement fits', () => {
    const anchor = { top: 750, left: 950, right: 990, bottom: 790 }

    const result = resolveCardPosition(anchor, card, viewport)

    expect(result.top).toBeGreaterThanOrEqual(8)
    expect(result.top + card.height).toBeLessThanOrEqual(viewport.height - 8)
    expect(result.left).toBeGreaterThanOrEqual(8)
    expect(result.left + card.width).toBeLessThanOrEqual(viewport.width - 8)
  })
})
