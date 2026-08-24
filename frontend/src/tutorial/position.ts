export interface Rect {
  top: number
  left: number
  right: number
  bottom: number
}

interface Size {
  width: number
  height: number
}

interface Point {
  top: number
  left: number
}

const GAP = 12
const MARGIN = 8

// アンカーの下→上→右→左の順で、画面内に完全に収まる配置を探す。
// どれも収まらない場合は下配置を画面内へクランプする(要件 §10.2「画面外へはみ出さない位置を自動選択」)。
export function resolveCardPosition(
  anchor: Rect,
  card: Size,
  viewport: Size,
  gap: number = GAP,
): Point {
  const candidates: Point[] = [
    { top: anchor.bottom + gap, left: anchor.left }, // 下
    { top: anchor.top - gap - card.height, left: anchor.left }, // 上
    { top: anchor.top, left: anchor.right + gap }, // 右
    { top: anchor.top, left: anchor.left - gap - card.width }, // 左
  ]

  const fitsInViewport = (p: Point) =>
    p.top >= MARGIN &&
    p.left >= MARGIN &&
    p.top + card.height <= viewport.height - MARGIN &&
    p.left + card.width <= viewport.width - MARGIN

  const fit = candidates.find(fitsInViewport)
  if (fit) return fit

  const fallback = candidates[0]
  return {
    top: clamp(fallback.top, MARGIN, viewport.height - card.height - MARGIN),
    left: clamp(fallback.left, MARGIN, viewport.width - card.width - MARGIN),
  }
}

function clamp(value: number, min: number, max: number): number {
  // ビューポートよりカードが大きい場合、maxがminを下回りうる。その場合はminを優先する。
  if (max < min) return min
  return Math.min(Math.max(value, min), max)
}
