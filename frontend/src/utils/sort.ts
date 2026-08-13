/**
 * id昇順に整列した新しい配列を返す。
 *
 * バックエンドの一覧APIには work-sessions 以外 ORDER BY が無く取得順が不定なため、
 * フロント側で必ず整列する（docs/frontend-implementation-plan.md §0-1 #11）。
 */
export function sortById<T extends { id: number }>(items: T[]): T[] {
  return [...items].sort((a, b) => a.id - b.id)
}

/**
 * item-order（並べ替えAPI）由来の順に整列した新しい配列を返す（§7.4.3）。
 * order に現れない項目（並び順レコードが未生成の既存データ）は末尾へ回し、末尾の中では id 昇順にする。
 * TaskGroup配下（idの名前空間がTaskのみ）で使う。Project直下の混在リストは
 * sortProjectItemsByOrder を使うこと（Task#3とTaskGroup#3のようなid衝突があるため）。
 */
export function sortByItemOrder<T extends { id: number }>(
  items: T[],
  order: { id: number; position: number }[],
): T[] {
  const positionById = new Map(order.map((o) => [o.id, o.position]))
  return [...items].sort((a, b) => {
    const pa = positionById.get(a.id)
    const pb = positionById.get(b.id)
    if (pa !== undefined && pb !== undefined) return pa - pb
    if (pa !== undefined) return -1
    if (pb !== undefined) return 1
    return a.id - b.id
  })
}

/**
 * Project直下（TaskとTaskGroupが同じ並び順を共有し、idの名前空間が別）用のitem-order整列。
 * Task#3 と TaskGroup#3 を取り違えないよう、type と id の組でキーを引く（§7.4.3）。
 */
export function sortProjectItemsByOrder<T extends { kind: 'TASK' | 'TASK_GROUP'; id: number }>(
  items: T[],
  order: { type: 'TASK' | 'TASK_GROUP'; id: number; position: number }[],
): T[] {
  const key = (type: string, id: number) => `${type}:${id}`
  const positionByKey = new Map(order.map((o) => [key(o.type, o.id), o.position]))
  return [...items].sort((a, b) => {
    const pa = positionByKey.get(key(a.kind, a.id))
    const pb = positionByKey.get(key(b.kind, b.id))
    if (pa !== undefined && pb !== undefined) return pa - pb
    if (pa !== undefined) return -1
    if (pb !== undefined) return 1
    return a.id - b.id
  })
}
