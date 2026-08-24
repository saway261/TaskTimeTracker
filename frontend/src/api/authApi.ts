import { httpClient } from './httpClient'
import type {
  AuthenticatedUserResponse,
  CsrfTokenResponse,
  EmailChangeRequest,
  LoginRequest,
  PasswordChangeRequest,
  PasswordResetConfirmRequest,
  PasswordResetRequest,
  PasswordResetRequestResponse,
  PendingEmailResponse,
  RegisterRequest,
} from '@/types/auth'

export function fetchCsrfToken() {
  return httpClient.get<CsrfTokenResponse>('/auth/csrf')
}

export function register(req: RegisterRequest) {
  return httpClient.post<AuthenticatedUserResponse>('/auth/register', req)
}

export function login(req: LoginRequest) {
  return httpClient.post<AuthenticatedUserResponse>('/auth/login', req)
}

export function logout() {
  return httpClient.post<void>('/auth/logout')
}

export function fetchMe() {
  return httpClient.get<AuthenticatedUserResponse>('/auth/me')
}

export function completeOnboarding() {
  return httpClient.post<void>('/auth/onboarding/complete')
}

export function changePassword(req: PasswordChangeRequest) {
  return httpClient.put<void>('/auth/password', req)
}

export function requestPasswordReset(req: PasswordResetRequest) {
  return httpClient.post<PasswordResetRequestResponse>('/auth/password-reset-requests', req)
}

export function resetPassword(req: PasswordResetConfirmRequest) {
  return httpClient.post<void>('/auth/password-resets', req)
}

export function verifyEmail(token: string) {
  return httpClient.post<void>('/auth/email-verifications', { token })
}

export function resendVerificationEmail() {
  return httpClient.post<void>('/auth/email-verifications/resend')
}

export function requestEmailChange(req: EmailChangeRequest) {
  return httpClient.put<PendingEmailResponse>('/auth/email', req)
}

export function confirmEmailChange(token: string) {
  return httpClient.post<void>('/auth/email-changes', { token })
}
