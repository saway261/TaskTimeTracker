// @vitest-environment jsdom

import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '@/api/authApi'
import { useAuthStore } from './authStore'

vi.mock('@/api/authApi')

const user = {
  id: 1,
  email: 'user@example.com',
  passwordChangeRequired: false,
}

describe('authStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.resetAllMocks()
  })

  it('stores the CSRF token returned by the API', async () => {
    vi.mocked(authApi.fetchCsrfToken).mockResolvedValue({
      data: { token: 'csrf-token', headerName: 'X-CSRF-TOKEN' },
    } as never)

    const store = useAuthStore()
    const result = await store.fetchCsrfToken()

    expect(store.csrfToken).toBe('csrf-token')
    expect(result.headerName).toBe('X-CSRF-TOKEN')
  })

  it.each([
    ['register', authApi.register, { email: user.email, password: 'password1234' }],
    ['login', authApi.login, { email: user.email, password: 'password1234' }],
    ['fetchMe', authApi.fetchMe, undefined],
  ] as const)('sets the current user after %s succeeds', async (action, apiFunction, request) => {
    vi.mocked(apiFunction).mockResolvedValue({ data: user } as never)
    const store = useAuthStore()

    const result = action === 'fetchMe' ? await store.fetchMe() : await store[action](request)

    expect(store.currentUser).toEqual(user)
    expect(store.isAuthenticated).toBe(true)
    expect(result).toEqual(user)
  })

  it('clears authentication after logout succeeds', async () => {
    vi.mocked(authApi.logout).mockResolvedValue({} as never)
    const store = useAuthStore()
    store.currentUser = user
    store.csrfToken = 'csrf-token'

    await store.logout()

    expect(store.currentUser).toBeNull()
    expect(store.csrfToken).toBeNull()
    expect(store.isAuthenticated).toBe(false)
  })

  it('clears authentication after password change succeeds', async () => {
    vi.mocked(authApi.changePassword).mockResolvedValue({} as never)
    const store = useAuthStore()
    store.currentUser = user
    store.csrfToken = 'csrf-token'

    await store.changePassword({
      currentPassword: 'password1234',
      newPassword: 'new-password1234',
    })

    expect(store.currentUser).toBeNull()
    expect(store.csrfToken).toBeNull()
  })
})
