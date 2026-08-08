/**
 * ルートパラメータ文字列を正の整数へ変換する。
 * "0" / 負値 / 小数 / 数値でない文字列は不正として null を返す。
 */
export function toPositiveInt(value: string | string[] | undefined): number | null {
  if (typeof value !== 'string' || value === '') {
    return null
  }
  if (!/^\d+$/.test(value)) {
    return null
  }
  const parsed = Number(value)
  return parsed > 0 ? parsed : null
}
