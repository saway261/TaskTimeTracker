/**
 * id昇順に整列した新しい配列を返す。
 *
 * バックエンドの一覧APIには work-sessions 以外 ORDER BY が無く取得順が不定なため、
 * フロント側で必ず整列する（docs/frontend-implementation-plan.md §0-1 #11）。
 */
export function sortById<T extends { id: number }>(items: T[]): T[] {
  return [...items].sort((a, b) => a.id - b.id)
}
