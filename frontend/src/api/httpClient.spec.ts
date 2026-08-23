// @vitest-environment jsdom

import { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { createPinia, setActivePinia } from 'pinia'
import { afterAll, beforeEach, describe, expect, it } from 'vitest'
import { router } from '@/router'
import { useAuthStore } from '@/stores/authStore'
import { useAppSettingsStore } from '@/stores/appSettingsStore'
import { httpClient } from './httpClient'

const originalAdapter = httpClient.defaults.adapter

function response(config: InternalAxiosRequestConfig, data: unknown, status = 200): AxiosResponse {
  return {
    config,
    data,
    headers: {},
    status,
    statusText: status === 200 ? 'OK' : 'Error',
  } as AxiosResponse
}

function unauthorized(config: InternalAxiosRequestConfig) {
  const errorResponse = response(
    config,
    { status: 'UNAUTHORIZED', message: 'authentication required', errors: [] },
    401,
  )
  return new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined, errorResponse)
}

describe('httpClient authentication interceptors', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    const authStore = useAuthStore()
    authStore.initialized = true
    authStore.currentUser = {
      id: 1,
      email: 'user@example.com',
      passwordChangeRequired: false,
      emailVerified: true,
      onboardingCompleted: false,
    }
    useAppSettingsStore().loaded = true
    await router.push('/projects')
  })

  afterAll(() => {
    httpClient.defaults.adapter = originalAdapter
  })

  it('sends credentials and adds a fetched CSRF token to non-GET requests', async () => {
    const requestedUrls: string[] = []
    httpClient.defaults.adapter = async (config) => {
      requestedUrls.push(config.url ?? '')
      if (config.url === '/auth/csrf') {
        return response(config, { token: 'csrf-token', headerName: 'X-CSRF-TOKEN' })
      }
      return response(config, null, 204)
    }

    const result = await httpClient.post('/projects', {})

    expect(httpClient.defaults.withCredentials).toBe(true)
    expect(requestedUrls).toEqual(['/auth/csrf', '/projects'])
    expect(result.config.headers.get('X-CSRF-TOKEN')).toBe('csrf-token')
    expect(useAuthStore().csrfToken).toBe('csrf-token')
  })

  it('shares one CSRF fetch between concurrent non-GET requests', async () => {
    let csrfFetchCount = 0
    httpClient.defaults.adapter = async (config) => {
      if (config.url === '/auth/csrf') {
        csrfFetchCount += 1
        return response(config, { token: 'shared-token', headerName: 'X-CSRF-TOKEN' })
      }
      return response(config, null, 204)
    }

    const [first, second] = await Promise.all([
      httpClient.post('/projects', {}),
      httpClient.patch('/tasks/1', {}),
    ])

    expect(csrfFetchCount).toBe(1)
    expect(first.config.headers.get('X-CSRF-TOKEN')).toBe('shared-token')
    expect(second.config.headers.get('X-CSRF-TOKEN')).toBe('shared-token')
  })

  it('clears authentication and navigates to login after a protected API returns 401', async () => {
    const authStore = useAuthStore()
    authStore.currentUser = {
      id: 1,
      email: 'user@example.com',
      passwordChangeRequired: false,
      emailVerified: true,
      onboardingCompleted: false,
    }
    authStore.csrfToken = 'csrf-token'
    httpClient.defaults.adapter = async (config) => {
      throw unauthorized(config)
    }

    await expect(httpClient.get('/projects')).rejects.toMatchObject({ status: 401 })

    expect(authStore.currentUser).toBeNull()
    expect(authStore.csrfToken).toBeNull()
    await expect.poll(() => router.currentRoute.value.path).toBe('/login')
  })

  it('keeps the current public page when the session probe returns 401', async () => {
    const authStore = useAuthStore()
    authStore.currentUser = null
    authStore.csrfToken = null
    await router.push('/password-reset?token=password-reset-token')
    httpClient.defaults.adapter = async (config) => {
      throw unauthorized(config)
    }

    await expect(httpClient.get('/auth/me')).rejects.toMatchObject({ status: 401 })

    expect(authStore.currentUser).toBeNull()
    expect(router.currentRoute.value.fullPath).toBe('/password-reset?token=password-reset-token')
  })

  it('does not clear authentication or navigate after a login 401', async () => {
    const authStore = useAuthStore()
    authStore.currentUser = {
      id: 1,
      email: 'user@example.com',
      passwordChangeRequired: false,
      emailVerified: true,
      onboardingCompleted: false,
    }
    authStore.csrfToken = 'csrf-token'
    httpClient.defaults.adapter = async (config) => {
      throw unauthorized(config)
    }

    await expect(httpClient.post('/auth/login', {})).rejects.toMatchObject({ status: 401 })

    expect(authStore.currentUser?.id).toBe(1)
    expect(authStore.csrfToken).toBe('csrf-token')
    expect(router.currentRoute.value.path).toBe('/projects')
  })
})
