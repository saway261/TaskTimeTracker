/**
 * item-order（並べ替えAPI）由来の順に整列した新しい配列を返す（§7.4.3）。
 * order に現れない項目（並び順レコードが未生成の既存データ）は末尾へ回し、
 * 末尾の中ではサーバから受け取った順（= 一覧APIのORDER BY）を保つ。
 * TaskGroup配下（idの名前空間がTaskのみ）で使う。Project直下の混在リストは
 * sortProjectItemsByOrder を使うこと（Task#3とTaskGroup#3のようなid衝突があるため）。
 */
export function sortByItemOrder<T extends { id: string }>(
  items: T[],
  order: { id: string; position: number }[],
): T[] {
  const positionById = new Map(order.map((o) => [o.id, o.position]))
  return stableSort(items, (a, b) => {
    const pa = positionById.get(a.id)
    const pb = positionById.get(b.id)
    if (pa !== undefined && pb !== undefined) return pa - pb
    if (pa !== undefined) return -1
    if (pb !== undefined) return 1
    return 0
  })
}

/**
 * Project直下（TaskとTaskGroupが同じ並び順を共有し、idの名前空間が別）用のitem-order整列。
 * Task#3 と TaskGroup#3 を取り違えないよう、type と id の組でキーを引く（§7.4.3）。
 */
export function sortProjectItemsByOrder<T extends { kind: 'TASK' | 'TASK_GROUP'; id: string }>(
  items: T[],
  order: { type: 'TASK' | 'TASK_GROUP'; id: string; position: number }[],
): T[] {
  const key = (type: string, id: string) => `${type}:${id}`
  const positionByKey = new Map(order.map((o) => [key(o.type, o.id), o.position]))
  return stableSort(items, (a, b) => {
    const pa = positionByKey.get(key(a.kind, a.id))
    const pb = positionByKey.get(key(b.kind, b.id))
    if (pa !== undefined && pb !== undefined) return pa - pb
    if (pa !== undefined) return -1
    if (pb !== undefined) return 1
    return 0
  })
}

/**
 * 比較関数が同値（0）を返した要素同士について、元の配列での前後関係を保つソート。
 *
 * 一覧APIは ORDER BY で順序が確定しているため、比較で決められない項目は
 * サーバから受け取った順をそのまま維持する。
 * Array.prototype.sort は仕様上は安定だが、「同値なら元の順を保つ」という意図を
 * 呼び出し側から読み取れるようにするために明示する。
 */
function stableSort<T>(items: T[], compare: (a: T, b: T) => number): T[] {
  return items
    .map((item, index) => ({ item, index }))
    .sort((a, b) => compare(a.item, b.item) || a.index - b.index)
    .map(({ item }) => item)
}
