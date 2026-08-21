import type { AxiosError } from 'axios'
import type { ErrorResponse, FieldError } from './errorResponse'
import { translateMessage } from '@/utils/validationMessages'

export type ApiErrorKind =
  'validation' | 'businessRule' | 'notFound' | 'methodNotAllowed' | 'server' | 'network' | 'unknown'

export interface ApiError {
  status: number
  kind: ApiErrorKind
  message: string
  fieldErrors: Record<string, string>
  formErrors: string[]
}

// クラスレベル検証の field 名。入力欄名と対応しないため formErrors 側へ回す。
// docs/frontend-implementation-plan.md §0-3 実測分。
const NON_FIELD_KEYS = new Set([
  'taskUpdateParentRequest',
  'validManual',
  'validTimer',
  'validTimeOrder', // フェーズ3.5で新規確認: endedAt < startedAt の更新時に返る
  'items', // フェーズ7 §7.4.6: 並べ替えリクエストの項目が現在の並び順と不一致
])

// 業務ルール違反として画面上部の警告に出し分けるメッセージ。
// docs/frontend-implementation-plan.md §0-3 実測分。
const BUSINESS_RULE_MESSAGES = new Set([
  'estimate minutes update not allowed',
  'task finish not allowed',
  'task group finish not allowed',
  'work session operation not allowed',
  'work session end not allowed',
  'invalid item order', // フェーズ7 §7.4.6: 送った項目集合がサーバの現在の項目と不一致
])

function buildFieldErrors(errors: FieldError[]): {
  fieldErrors: Record<string, string>
  formErrors: string[]
} {
  const fieldErrors: Record<string, string> = {}
  const formErrors: string[] = []

  for (const { field, message } of errors) {
    const translated = translateMessage(message)

    if (NON_FIELD_KEYS.has(field)) {
      formErrors.push(translated)
      continue
    }

    fieldErrors[field] = translated

    // "getById.id" のようなパスパラメータのネストは最終セグメントでも引けるようにする
    if (field.includes('.')) {
      const lastSegment = field.split('.').pop()
      if (lastSegment) {
        fieldErrors[lastSegment] = translated
      }
    }
  }

  return { fieldErrors, formErrors }
}

function resolveKind(status: number, message: string): ApiErrorKind {
  if (status === 404) return 'notFound'
  if (status === 405) return 'methodNotAllowed'
  if (status === 400) {
    return BUSINESS_RULE_MESSAGES.has(message) ? 'businessRule' : 'validation'
  }
  if (status >= 500) return 'server'
  return 'unknown'
}

/**
 * axiosのエラーを画面表示用の ApiError へ正規化する。
 *
 * 実測（docs/api-samples/）で確認済みの前提:
 * - 不正なJSON送信時（400）と存在しないメソッド（405）は本文もContent-Typeも空。
 * - `status` フィールドは "400 BAD_REQUEST" という文字列であり、数値ではない。
 *   そのため HTTPステータスコード（response.status）の方を正とする。
 */
export function normalizeError(error: unknown): ApiError {
  const axiosError = error as AxiosError<ErrorResponse | string | ''>

  if (!axiosError.response) {
    return {
      status: 0,
      kind: 'network',
      message: 'サーバーに接続できませんでした。通信環境を確認してください。',
      fieldErrors: {},
      formErrors: [],
    }
  }

  const { status, data } = axiosError.response

  const isErrorResponse =
    typeof data === 'object' && data !== null && 'message' in data && 'errors' in data

  if (!isErrorResponse) {
    // 本文が空のエラー（不正なJSON送信時の400、存在しないメソッドの405 など）
    return {
      status,
      kind: resolveKind(status, ''),
      message:
        status === 405 ? 'この操作はサポートされていません。' : '送信内容を処理できませんでした。',
      fieldErrors: {},
      formErrors: [],
    }
  }

  const { fieldErrors, formErrors } = buildFieldErrors(data.errors ?? [])

  return {
    status,
    kind: resolveKind(status, data.message),
    message: translateMessage(data.message),
    fieldErrors,
    formErrors,
  }
}
