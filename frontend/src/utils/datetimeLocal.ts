function pad(n: number): string {
  return n.toString().padStart(2, '0')
}

/**
 * ISO日時文字列（オフセット付き）を <input type="datetime-local"> 用の
 * ローカル時刻表記（"YYYY-MM-DDTHH:mm:ss"）へ変換する。
 */
export function toDatetimeLocalValue(iso: string): string {
  const date = new Date(iso)
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  )
}

/**
 * <input type="datetime-local"> の値（オフセット無し・ブラウザのローカル時刻）を、
 * そのローカル時刻のUTCオフセットを付与したISO文字列に変換してAPIへ送る。
 * バックエンドが startedAt/endedAt を OffsetDateTime として対称に受け付けるようになったため
 * （docs/frontend-design.md §22 回答）、オフセットを付けて送る。
 */
export function fromDatetimeLocalValue(value: string): string {
  const date = new Date(value)
  const offsetMinutes = -date.getTimezoneOffset()
  const sign = offsetMinutes >= 0 ? '+' : '-'
  const abs = Math.abs(offsetMinutes)
  const offset = `${sign}${pad(Math.floor(abs / 60))}:${pad(abs % 60)}`
  return `${toDatetimeLocalValue(value)}${offset}`
}
