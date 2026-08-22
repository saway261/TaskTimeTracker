import { describe, expect, it } from 'vitest'
import { normalizeError } from './apiError'

describe('normalizeError', () => {
  it('タスクグループの完了不可エラーを日本語の業務エラーに変換する', () => {
    const error = {
      response: {
        status: 400,
        data: {
          status: '400 BAD_REQUEST',
          message: 'task group finish not allowed',
          errors: [
            {
              field: 'taskGroup.id',
              message: '未完了のタスクがあるタスクグループは完了状態にできません',
            },
          ],
        },
      },
    }

    expect(normalizeError(error)).toMatchObject({
      status: 400,
      kind: 'businessRule',
      message: '未完了のタスクがあるため、タスクグループを完了にできません。',
    })
  })

  it('タグ上限エラーをフィールド識別を保ったまま日本語へ変換する', () => {
    const error = {
      response: {
        status: 400,
        data: {
          status: '400 BAD_REQUEST',
          message: 'tag limit exceeded',
          errors: [
            {
              field: 'tagLimit',
              message: '保有できるタグの上限（50件）に達しています',
            },
          ],
        },
      },
    }

    expect(normalizeError(error)).toMatchObject({
      status: 400,
      kind: 'validation',
      message: 'タグは50件までです。使っていないタグをアーカイブしてください。',
      fieldErrors: { tagLimit: '保有できるタグの上限（50件）に達しています' },
    })
  })
})
