// @vitest-environment jsdom

import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { createPinia, setActivePinia } from 'pinia'
import { afterAll, beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@/stores/authStore'
import * as authApi from './authApi'
import { httpClient } from './httpClient'

const originalAdapter = httpClient.defaults.adapter

function response(config: InternalAxiosRequestConfig, data: unknown): AxiosResponse {
  return {
    config,
    data,
    headers: {},
    status: 200,
    statusText: 'OK',
  } as AxiosResponse
}

describe('authApi', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    useAuthStore().csrfToken = 'csrf-token'
  })

  afterAll(() => {
    httpClient.defaults.adapter = originalAdapter
  })

  it('uses the authentication API endpoints and HTTP methods', async () => {
    const requests: Array<{ method: string; url: string }> = []
    httpClient.defaults.adapter = async (config) => {
      requests.push({ method: config.method ?? '', url: config.url ?? '' })
      return response(config, {})
    }

    await authApi.fetchCsrfToken()
    await authApi.register({ email: 'user@example.com', password: 'password1234' })
    await authApi.login({ email: 'user@example.com', password: 'password1234' })
    await authApi.logout()
    await authApi.fetchMe()
    await authApi.changePassword({
      currentPassword: 'password1234',
      newPassword: 'new-password1234',
    })

    expect(requests).toEqual([
      { method: 'get', url: '/auth/csrf' },
      { method: 'post', url: '/auth/register' },
      { method: 'post', url: '/auth/login' },
      { method: 'post', url: '/auth/logout' },
      { method: 'get', url: '/auth/me' },
      { method: 'put', url: '/auth/password' },
    ])
  })
})
