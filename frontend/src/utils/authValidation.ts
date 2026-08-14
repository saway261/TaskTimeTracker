const MIN_PASSWORD_LENGTH = 12
const MAX_PASSWORD_BYTES = 72

export function validateNewPassword(password: string) {
  if (password.length < MIN_PASSWORD_LENGTH) {
    return `${MIN_PASSWORD_LENGTH}文字以上で入力してください`
  }
  if (new TextEncoder().encode(password).length > MAX_PASSWORD_BYTES) {
    return `UTF-8換算で${MAX_PASSWORD_BYTES}バイト以内で入力してください`
  }
  return undefined
}

export function validatePasswordConfirmation(password: string, confirmation: string) {
  return password === confirmation ? undefined : 'パスワードが一致しません'
}
