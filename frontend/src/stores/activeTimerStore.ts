import { defineStore } from 'pinia'
import * as workSessionsApi from '@/api/workSessionsApi'
import type { ActiveTimer } from '@/types/workSession'
import type { ApiError } from '@/types/apiError'

let requestVersion = 0

export const useActiveTimerStore = defineStore('activeTimer', {
  state: () => ({
    activeTimers: [] as ActiveTimer[],
    hasActiveTimers: false,
    loading: false,
    error: null as ApiError | null,
  }),
  actions: {
    async fetchActiveTimers() {
      const currentRequest = ++requestVersion
      this.loading = true
      this.error = null
      try {
        const { data } = await workSessionsApi.fetchActiveTimers()
        if (currentRequest === requestVersion) {
          this.activeTimers = data
          this.hasActiveTimers = data.length > 0
        }
        return data
      } catch (e) {
        if (currentRequest === requestVersion) this.error = e as ApiError
        throw e
      } finally {
        if (currentRequest === requestVersion) this.loading = false
      }
    },

    clear() {
      requestVersion += 1
      this.activeTimers = []
      this.hasActiveTimers = false
      this.error = null
      this.loading = false
    },

    markTimerStarted() {
      this.hasActiveTimers = true
    },

    markTimerStopped(sessionId: string) {
      const contained = this.activeTimers.some((timer) => timer.sessionId === sessionId)
      this.activeTimers = this.activeTimers.filter((timer) => timer.sessionId !== sessionId)
      if (contained) this.hasActiveTimers = this.activeTimers.length > 0
    },
  },
})
