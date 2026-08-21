/** 分数を「1時間30分」のような表記へ変換する。 */
export function formatMinutes(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60

  if (hours === 0) {
    return `${rest}分`
  }
  if (rest === 0) {
    return `${hours}時間`
  }
  return `${hours}時間${rest}分`
}

/** 見積との差分を「+10分（見積超過）」のような表記へ変換する。 */
export function formatGap(gapMinutes: number): string {
  if (gapMinutes === 0) {
    return '見積どおり'
  }
  const sign = gapMinutes > 0 ? '+' : ''
  const label = gapMinutes > 0 ? '見積超過' : '見積より早い'
  return `${sign}${gapMinutes}分（${label}）`
}

/**
 * 誤差率を「+16.7%」のような表記へ変換する。
 * バックエンドの `gapRateCached` は既に百分率で入っているため、ここで100倍しない。
 */
export function formatGapRate(gapRatePercent: number): string {
  const rounded = gapRatePercent.toFixed(1)
  const sign = gapRatePercent > 0 ? '+' : ''
  return `${sign}${rounded}%`
}

export type EstimateOutcome = 'early' | 'on-time' | 'late' | 'unknown'

export const DEFAULT_ON_TIME_THRESHOLD_PERCENT = 10

/** 誤差比から、指定されたしきい値に基づいて見積に対する完了結果を分類する。 */
export function estimateOutcome(
  gapRatePercent: number | null | undefined,
  thresholdPercent: number,
): EstimateOutcome {
  if (gapRatePercent === null || gapRatePercent === undefined) return 'unknown'
  if (Math.abs(gapRatePercent) <= thresholdPercent) return 'on-time'
  return gapRatePercent < 0 ? 'early' : 'late'
}
