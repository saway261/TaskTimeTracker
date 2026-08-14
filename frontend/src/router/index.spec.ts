// @vitest-environment jsdom

import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@/stores/authStore'
import { router } from './index'

const user = {
  id: 1,
  email: 'user@example.com',
  passwordChangeRequired: false,
}

describe('authentication navigation guard', () => {
  beforeEach(() => {
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
  })

  it('forces users who must change their password to the password change route', async () => {
    useAuthStore().currentUser = { ...user, passwordChangeRequired: true }

    await router.push('/projects/10')

    expect(router.currentRoute.value.name).toBe('password-change')
  })

  it('allows authenticated users to open the password change route', async () => {
    useAuthStore().currentUser = user

    await router.push('/password-change')

    expect(router.currentRoute.value.name).toBe('password-change')
  })
})
