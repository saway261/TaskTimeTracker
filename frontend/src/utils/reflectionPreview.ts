export const PREVIEW_MAX_LENGTH = 50

/** 振り返り入力項目を一覧プレビュー用に先頭50文字へ切り詰める。保存データ自体は切り詰めない。 */
export function truncateForPreview(text: string | null): string {
  if (text === null) return ''
  if (text.length <= PREVIEW_MAX_LENGTH) return text
  return `${text.slice(0, PREVIEW_MAX_LENGTH)}…`
}
