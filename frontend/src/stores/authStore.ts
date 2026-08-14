import { defineStore } from 'pinia'
import * as authApi from '@/api/authApi'
import type {
  AuthenticatedUserResponse,
  LoginRequest,
  PasswordChangeRequest,
  RegisterRequest,
} from '@/types/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    currentUser: null as AuthenticatedUserResponse | null,
    csrfToken: null as string | null,
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
      return data
    },

    async login(req: LoginRequest) {
      const { data } = await authApi.login(req)
      this.currentUser = data
      return data
    },

    async logout() {
      await authApi.logout()
      this.clear()
    },

    async fetchMe() {
      const { data } = await authApi.fetchMe()
      this.currentUser = data
      return data
    },

    async changePassword(req: PasswordChangeRequest) {
      await authApi.changePassword(req)
      this.clear()
    },

    clear() {
      this.$reset()
    },
  },
})
