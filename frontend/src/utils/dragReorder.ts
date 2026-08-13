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
