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
  emailVerified: boolean
}

export interface PasswordChangeRequest {
  currentPassword: string
  newPassword: string
}

export interface PasswordResetRequest {
  email: string
}

export interface PasswordResetRequestResponse {
  message: string
}

export interface PasswordResetConfirmRequest {
  token: string
  newPassword: string
}

export interface CsrfTokenResponse {
  token: string
  headerName: string
}

export interface EmailChangeRequest {
  newEmail: string
  currentPassword: string
}

export interface PendingEmailResponse {
  pendingEmail: string
}
