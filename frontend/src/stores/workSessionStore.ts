import { defineStore } from 'pinia'
import * as workSessionsApi from '@/api/workSessionsApi'
import type {
  WorkSession,
  WorkSessionCreateRequest,
  WorkSessionUpdateRequest,
} from '@/types/workSession'
import type { ApiError } from '@/types/apiError'
import { sortById } from '@/utils/sort'
import { toDatetimeLocalValue } from '@/utils/datetimeLocal'

export const useWorkSessionStore = defineStore('workSession', {
  state: () => ({
    workSessions: [] as WorkSession[],
    totalMinutes: null as number | null,
    // type===TIMER かつ endedAt===null のセッション。2件以上あれば不整合として multipleActiveSessions を立てる。
    activeSession: null as WorkSession | null,
    multipleActiveSessions: false,
    loading: false,
    error: null as ApiError | null,
  }),
  actions: {
    async fetchSessions(taskId: number) {
      this.loading = true
      this.error = null
      try {
        const res = await workSessionsApi.fetchAllInTask(taskId)
        this.workSessions = sortById(res.data)
        this.updateActiveSession()
      } catch (e) {
        this.error = e as ApiError
        throw e
      } finally {
        this.loading = false
      }
    },

    async fetchTotalMinutes(taskId: number) {
      const res = await workSessionsApi.fetchTotalMinutes(taskId)
      this.totalMinutes = res.data
    },

    // 登録後オブジェクトをそのまま反映するため、再取得は不要。
    async createManualSession(taskId: number, minutes: number) {
      const req: WorkSessionCreateRequest = { type: 'MANUAL', minutes }
      const res = await workSessionsApi.create(taskId, req)
      this.workSessions = sortById([...this.workSessions, res.data])
      return res.data
    },

    // startedAtはサーバのNOW()で上書きされる（バリデーション上必須なだけ）。
    // このエンドポイントの startedAt は §22 Q1 の対称化修正の対象外で、依然オフセット無し
    // （LocalDateTime互換）の文字列としてしか受け付けない（付録C #17）。オフセット付きを送ると
    // Jacksonのデシリアライズで汎用400になり、タイマーが開始できない。
    async startTimer(taskId: number) {
      const req: WorkSessionCreateRequest = {
        type: 'TIMER',
        startedAt: toDatetimeLocalValue(new Date().toISOString()),
      }
      const res = await workSessionsApi.create(taskId, req)
      this.workSessions = sortById([...this.workSessions, res.data])
      this.updateActiveSession()
      return res.data
    },

    async stopTimer(workSessionId: number) {
      const res = await workSessionsApi.end(workSessionId)
      const idx = this.workSessions.findIndex((s) => s.id === workSessionId)
      if (idx !== -1) {
        this.workSessions[idx] = res.data
      }
      this.updateActiveSession()
      return res.data
    },

    async updateSession(id: number, req: WorkSessionUpdateRequest) {
      const res = await workSessionsApi.update(id, req)
      const idx = this.workSessions.findIndex((s) => s.id === id)
      if (idx !== -1) {
        this.workSessions[idx] = res.data
      }
      this.updateActiveSession()
      return res.data
    },

    // 204で本文が無いため、ローカル配列から該当要素を除去する。
    async removeSession(id: number) {
      await workSessionsApi.remove(id)
      this.workSessions = this.workSessions.filter((s) => s.id !== id)
      this.updateActiveSession()
    },

    // 0件→停止中 / 1件→稼働中 / 2件以上→データ不整合（B6）。最新(id最大)を採用し画面に警告する。
    updateActiveSession() {
      const active = this.workSessions.filter((s) => s.type === 'TIMER' && s.endedAt === null)
      if (active.length === 0) {
        this.activeSession = null
        this.multipleActiveSessions = false
        return
      }
      this.activeSession = active.reduce((latest, s) => (s.id > latest.id ? s : latest))
      this.multipleActiveSessions = active.length > 1
    },
  },
})
