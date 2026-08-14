export interface RegisterRequest {
  email: string
  password: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface AuthenticatedUserResponse {
  id: number
  email: string
  passwordChangeRequired: boolean
}

export interface PasswordChangeRequest {
  currentPassword: string
  newPassword: string
}

export interface CsrfTokenResponse {
  token: string
  headerName: string
}
