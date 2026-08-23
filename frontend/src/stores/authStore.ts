import { defineStore } from 'pinia'
import * as authApi from '@/api/authApi'
import type {
  AuthenticatedUserResponse,
  LoginRequest,
  PasswordChangeRequest,
  RegisterRequest,
} from '@/types/auth'
import type { ApiError } from '@/types/apiError'

let initializationRequest: Promise<void> | null = null

export const useAuthStore = defineStore('auth', {
  state: () => ({
    currentUser: null as AuthenticatedUserResponse | null,
    csrfToken: null as string | null,
    initialized: false,
  }),
  getters: {
    isAuthenticated: (state) => state.currentUser !== null,
  },
  actions: {
    async fetchCsrfToken() {
      const { data } = await authApi.fetchCsrfToken()
      this.csrfToken = data.token
      return data
    },

    async register(req: RegisterRequest) {
      const { data } = await authApi.register(req)
      this.currentUser = data
      this.initialized = true
      return data
    },

    async login(req: LoginRequest) {
      const { data } = await authApi.login(req)
      this.currentUser = data
      this.initialized = true
      return data
    },

    async logout() {
      await authApi.logout()
      this.clear()
    },

    async fetchMe() {
      const { data } = await authApi.fetchMe()
      this.currentUser = data
      this.initialized = true
      return data
    },

    async completeOnboarding() {
      await authApi.completeOnboarding()
      if (this.currentUser) {
        this.currentUser.onboardingCompleted = true
      }
    },

    initialize() {
      if (this.initialized) return Promise.resolve()
      if (initializationRequest) return initializationRequest

      initializationRequest = this.fetchMe()
        .then(() => undefined)
        .catch((e) => {
          this.currentUser = null
          if ((e as ApiError).status !== 401) throw e
        })
        .finally(() => {
          this.initialized = true
          initializationRequest = null
        })

      return initializationRequest
    },

    async changePassword(req: PasswordChangeRequest) {
      await authApi.changePassword(req)
      this.clear()
    },

    clear() {
      this.currentUser = null
      this.csrfToken = null
    },
  },
})
