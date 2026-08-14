import { httpClient } from './httpClient'
import type {
  AuthenticatedUserResponse,
  CsrfTokenResponse,
  LoginRequest,
  PasswordChangeRequest,
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

export function changePassword(req: PasswordChangeRequest) {
  return httpClient.put<void>('/auth/password', req)
}
