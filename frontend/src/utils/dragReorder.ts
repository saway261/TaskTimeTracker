export interface KeyedStub {
  key: string
}

/**
 * ドラッグしていた項目（draggedStub）を、対象コンテナの現在の項目一覧（items）の中で
 * beforeKeyの直前（nullなら末尾）に挿入した新しい配列を返す。
 *
 * items に draggedStub と同じ key が既に含まれていても構わない（先に取り除いてから挿入するため）。
 * これにより同一コンテナ内の並べ替え・別コンテナからの挿入のどちらにも同じ関数を使える。
 */
export function insertStubAt<T extends KeyedStub>(
  items: T[],
  draggedStub: T,
  beforeKey: string | null,
): T[] {
  const withoutDragged = items.filter((i) => i.key !== draggedStub.key)
  if (beforeKey === null) {
    return [...withoutDragged, draggedStub]
  }
  const idx = withoutDragged.findIndex((i) => i.key === beforeKey)
  if (idx === -1) {
    return [...withoutDragged, draggedStub]
  }
  return [...withoutDragged.slice(0, idx), draggedStub, ...withoutDragged.slice(idx)]
}

/**
 * 一部の項目だけを表示している一覧で、表示上隣り合う2項目を全項目の配列内でも入れ替える。
 * 非表示項目を並べ替えリクエストから欠落させず、その位置も維持できる。
 */
export function swapVisibleItems<T>(
  allItems: readonly T[],
  visibleItems: readonly T[],
  visibleIndex: number,
  direction: -1 | 1,
): T[] {
  const otherVisibleIndex = visibleIndex + direction
  if (visibleIndex < 0 || otherVisibleIndex < 0 || otherVisibleIndex >= visibleItems.length) {
    return [...allItems]
  }

  const currentIndex = allItems.indexOf(visibleItems[visibleIndex])
  const otherIndex = allItems.indexOf(visibleItems[otherVisibleIndex])
  if (currentIndex === -1 || otherIndex === -1) return [...allItems]

  const result = [...allItems]
  ;[result[currentIndex], result[otherIndex]] = [result[otherIndex], result[currentIndex]]
  return result
}
