// @vitest-environment jsdom

import { afterEach, describe, expect, it, vi } from 'vitest'
import { resolveAnchor } from './anchor'

// jsdomはレイアウトを計算しないため、getClientRects()は既定で常に空を返す。
// 可視/不可視を意図的に作るため、テストごとにスタブする。
function markVisible(el: HTMLElement) {
  vi.spyOn(el, 'getClientRects').mockReturnValue([{}] as unknown as DOMRectList)
}

function markInvisible(el: HTMLElement) {
  vi.spyOn(el, 'getClientRects').mockReturnValue([] as unknown as DOMRectList)
}

function appendWithClass(className: string): HTMLElement {
  const el = document.createElement('div')
  el.className = className
  document.body.appendChild(el)
  return el
}

describe('resolveAnchor', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('returns the second candidate selector when the first is invisible', () => {
    const first = appendWithClass('candidate-a')
    const second = appendWithClass('candidate-b')
    markInvisible(first)
    markVisible(second)

    expect(resolveAnchor(['.candidate-a', '.candidate-b'])).toBe(second)
  })

  it('returns the second matching element for the same selector when the first is invisible', () => {
    const first = appendWithClass('task-row')
    const second = appendWithClass('task-row')
    markInvisible(first)
    markVisible(second)

    expect(resolveAnchor(['.task-row'])).toBe(second)
  })

  it('returns null when every candidate is invisible', () => {
    const first = appendWithClass('candidate-a')
    const second = appendWithClass('candidate-b')
    markInvisible(first)
    markInvisible(second)

    expect(resolveAnchor(['.candidate-a', '.candidate-b'])).toBeNull()
  })

  it('returns null when targets is undefined', () => {
    expect(resolveAnchor(undefined)).toBeNull()
  })
})
