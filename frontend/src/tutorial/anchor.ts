// 可視な候補を先頭から探す。offsetParentは position: fixed の要素で null になるため使わない
// (ヘッダーやモーダル内の要素を誤って不可視と判定してしまう)。
//
// scopeRoots を渡すと、いずれかのroot配下にある要素だけを対象にする。モーダルを開いても
// 背後のページはDOMに残り可視のままなので、「可視かどうか」だけでは今見ている画面の要素を
// 絞り込めないため(実装計画 §0-2-19)。
export function resolveAnchor(targets?: string[], scopeRoots?: Element[]): HTMLElement | null {
  if (!targets) return null
  const hasScope = scopeRoots !== undefined && scopeRoots.length > 0
  for (const selector of targets) {
    for (const el of document.querySelectorAll<HTMLElement>(selector)) {
      if (el.getClientRects().length === 0) continue
      if (hasScope && !scopeRoots.some((root) => root.contains(el))) continue
      return el
    }
  }
  return null
}
