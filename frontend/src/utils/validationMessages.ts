/**
 * バックエンドのバリデーション・業務ルールメッセージ（英語）を日本語へ変換する。
 * docs/frontend-implementation-plan.md §2.6 / 付録B。完全一致で引き、当たらなければ
 * サーバのメッセージ（独自制約は元々日本語）をそのまま返す。
 */
const EXACT_MATCHES: Record<string, string> = {
  // Bean Validation 標準メッセージ
  'must not be blank': '入力してください',
  'must not be null': '入力してください',
  'must be greater than 0': '1以上の数値を入力してください',

  // トップレベル message（§0-3）
  'payload validation error': '入力内容を確認してください。',
  'parameter validation error': '入力内容を確認してください。',
  'type mismatch error': '入力内容を確認してください。',
  'target not found': '対象が見つかりませんでした。',
  'estimate minutes update not allowed': '作業記録があるため、見積時間を変更できません。',
  'task finish not allowed': '未終了の作業セッションがあるため、完了にできません。',
  'work session operation not allowed': '完了済みのタスクでは作業セッションを操作できません。',
  'work session end not allowed': 'このセッションは終了できません。',
}

// "size must be between 0 and 20" → 上限値だけ抜き出して組み立てる
const SIZE_PATTERN = /^size must be between \d+ and (\d+)$/

export function translateMessage(message: string): string {
  const exact = EXACT_MATCHES[message]
  if (exact) {
    return exact
  }

  const sizeMatch = message.match(SIZE_PATTERN)
  if (sizeMatch) {
    return `${sizeMatch[1]}文字以内で入力してください`
  }

  return message
}
