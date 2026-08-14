import { describe, expect, it } from 'vitest'
import { validateNewPassword, validatePasswordConfirmation } from './authValidation'

describe('authentication validation', () => {
  it('requires at least 12 password characters', () => {
    expect(validateNewPassword('12345678901')).toBe('12文字以上で入力してください')
    expect(validateNewPassword('123456789012')).toBeUndefined()
  })

  it('rejects passwords longer than 72 UTF-8 bytes', () => {
    expect(validateNewPassword('あ'.repeat(24))).toBeUndefined()
    expect(validateNewPassword('あ'.repeat(25))).toBe('UTF-8換算で72バイト以内で入力してください')
  })

  it('compares password confirmation without trimming', () => {
    expect(validatePasswordConfirmation('password1234 ', 'password1234')).toBe(
      'パスワードが一致しません',
    )
    expect(validatePasswordConfirmation('password1234 ', 'password1234 ')).toBeUndefined()
  })
})
