// 可視な候補を先頭から探す。offsetParentは position: fixed の要素で null になるため使わない
// (ヘッダーやモーダル内の要素を誤って不可視と判定してしまう)。
export function resolveAnchor(targets?: string[]): HTMLElement | null {
  if (!targets) return null
  for (const selector of targets) {
    for (const el of document.querySelectorAll<HTMLElement>(selector)) {
      if (el.getClientRects().length > 0) return el
    }
  }
  return null
}
