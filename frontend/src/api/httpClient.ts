import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { router } from '@/router'
import { useAuthStore } from '@/stores/authStore'
import { normalizeError } from '@/types/apiError'
import type { CsrfTokenResponse } from '@/types/auth'

export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  withCredentials: true,
})

let csrfTokenRequest: Promise<string> | null = null

function isGetRequest(config: InternalAxiosRequestConfig) {
  return (config.method ?? 'get').toLowerCase() === 'get'
}

async function getCsrfToken() {
  const authStore = useAuthStore()
  if (authStore.csrfToken) return authStore.csrfToken

  csrfTokenRequest ??= httpClient
    .get<CsrfTokenResponse>('/auth/csrf')
    .then(({ data }) => {
      authStore.csrfToken = data.token
      return data.token
    })
    .finally(() => {
      csrfTokenRequest = null
    })

  return csrfTokenRequest
}

httpClient.interceptors.request.use(async (config) => {
  if (isGetRequest(config)) return config

  const csrfToken = await getCsrfToken()
  config.headers.set('X-CSRF-TOKEN', csrfToken)
  return config
})

function isLoginRequest(error: AxiosError) {
  const url = error.config?.url
  if (!url) return false

  try {
    const baseUrl = error.config?.baseURL ?? window.location.origin
    return new URL(url, new URL(baseUrl, window.location.origin)).pathname.endsWith('/auth/login')
  } catch {
    return url.split(/[?#]/, 1)[0].endsWith('/auth/login')
  }
}

httpClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    if (error.response?.status === 401 && !isLoginRequest(error)) {
      useAuthStore().clear()
      if (router.currentRoute.value.path !== '/login') {
        void router.push('/login')
      }
    }

    return Promise.reject(normalizeError(error))
  },
)
