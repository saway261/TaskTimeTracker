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

  it('ignores visible elements outside the given scope roots', () => {
    // モーダルを開いても背後のページはDOMに残り可視のままなので、
    // スコープ指定が無いと「今見ていない画面」の要素を拾ってしまう。
    const page = appendWithClass('page-root')
    const pageHeading = document.createElement('h1')
    page.appendChild(pageHeading)
    const modal = appendWithClass('modal-root')
    const modalSection = document.createElement('section')
    modalSection.className = 'metrics-section'
    modal.appendChild(modalSection)
    markVisible(pageHeading)
    markVisible(modalSection)

    expect(resolveAnchor(['.page-root h1'], [modal])).toBeNull()
    expect(resolveAnchor(['.metrics-section'], [modal])).toBe(modalSection)
  })

  it('prefers a later candidate that is inside the scope over an earlier one outside it', () => {
    const page = appendWithClass('page-root')
    const pageSection = document.createElement('section')
    pageSection.className = 'estimation-section'
    page.appendChild(pageSection)
    const modal = appendWithClass('modal-root')
    const modalSection = document.createElement('section')
    modalSection.className = 'metrics-section'
    modal.appendChild(modalSection)
    markVisible(pageSection)
    markVisible(modalSection)

    // スコープ無しなら第1候補(ページ側)、スコープありなら第2候補(モーダル側)。
    expect(resolveAnchor(['.estimation-section', '.metrics-section'])).toBe(pageSection)
    expect(resolveAnchor(['.estimation-section', '.metrics-section'], [modal])).toBe(modalSection)
  })

  it('accepts an element inside any of several scope roots', () => {
    const header = appendWithClass('app-header')
    const trigger = document.createElement('button')
    trigger.className = 'timer-menu-trigger'
    header.appendChild(trigger)
    const screen = appendWithClass('screen-root')
    markVisible(trigger)

    // ヘッダーは全画面共通なので、画面ルートと一緒にスコープへ含める運用。
    expect(resolveAnchor(['.timer-menu-trigger'], [screen])).toBeNull()
    expect(resolveAnchor(['.timer-menu-trigger'], [screen, header])).toBe(trigger)
  })
})
