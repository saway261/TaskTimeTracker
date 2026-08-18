// @vitest-environment jsdom

import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '@/api/authApi'
import { useAuthStore } from '@/stores/authStore'
import { router } from './index'

const user = {
  id: 1,
  email: 'user@example.com',
  passwordChangeRequired: false,
  emailVerified: true,
}

describe('authentication navigation guard', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    setActivePinia(createPinia())
    useAuthStore().initialized = true
  })

  it('redirects unauthenticated users from protected routes to login', async () => {
    await router.push('/projects/10')

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/projects/10')
  })

  it('redirects authenticated users away from guest-only routes', async () => {
    useAuthStore().currentUser = user

    await router.push('/register')

    expect(router.currentRoute.value.name).toBe('project-list')
    expect(router.currentRoute.value.query.isFinished).toBe('false')
  })

  it('opens the unfinished project list from the application root', async () => {
    useAuthStore().currentUser = user

    await router.push('/')

    expect(router.currentRoute.value.name).toBe('project-list')
    expect(router.currentRoute.value.query.isFinished).toBe('false')
  })

  it('forces users who must change their password to the password change route', async () => {
    useAuthStore().currentUser = {
      ...user,
      passwordChangeRequired: true,
      emailVerified: false,
    }

    await router.push('/projects/10')

    expect(router.currentRoute.value.name).toBe('password-change')
  })

  it('forces unverified users to the email verification pending route', async () => {
    useAuthStore().currentUser = { ...user, emailVerified: false }

    await router.push('/projects/10')

    expect(router.currentRoute.value.name).toBe('email-verification-pending')
  })

  it('allows unverified users to open the email change route', async () => {
    useAuthStore().currentUser = { ...user, emailVerified: false }

    await router.push('/email-change')

    expect(router.currentRoute.value.name).toBe('email-change')
  })

  it('allows password-change-required users to open the email change route', async () => {
    useAuthStore().currentUser = {
      ...user,
      passwordChangeRequired: true,
      emailVerified: false,
    }

    await router.push('/email-change')

    expect(router.currentRoute.value.name).toBe('email-change')
  })

  it('allows unauthenticated users to open email confirmation routes', async () => {
    await router.push('/verify-email?token=verification-token')
    expect(router.currentRoute.value.name).toBe('verify-email')

    await router.push('/verify-email-change?token=email-change-token')
    expect(router.currentRoute.value.name).toBe('verify-email-change')
  })

  it('allows unauthenticated users to open password reset routes', async () => {
    await router.push('/password-reset-request')
    expect(router.currentRoute.value.name).toBe('password-reset-request')

    await router.push('/password-reset?token=password-reset-token')
    expect(router.currentRoute.value.name).toBe('password-reset')
  })

  it('keeps the password reset route after initial session detection returns 401', async () => {
    const authStore = useAuthStore()
    await router.push('/login')
    authStore.initialized = false
    vi.spyOn(authApi, 'fetchMe').mockRejectedValue({ status: 401 })

    await router.push('/password-reset?token=direct-access-token')

    expect(router.currentRoute.value.name).toBe('password-reset')
    expect(router.currentRoute.value.query.token).toBe('direct-access-token')
  })

  it('allows authenticated users to open the password change route', async () => {
    useAuthStore().currentUser = user

    await router.push('/password-change')

    expect(router.currentRoute.value.name).toBe('password-change')
  })
})
