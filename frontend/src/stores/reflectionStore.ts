import { defineStore } from 'pinia'
import * as reflectionsApi from '@/api/reflectionsApi'
import type {
  ProjectReflectionOverviewResponse,
  ReflectionRequest,
  ReflectionResponse,
} from '@/types/reflection'
import type { ApiError } from '@/types/apiError'

export const useReflectionStore = defineStore('reflection', {
  state: () => ({
    overview: null as ProjectReflectionOverviewResponse | null,
    selectedProjectId: null as string | null,
    loading: false,
    error: null as ApiError | null,
  }),
  actions: {
    async fetchOverview(projectId: string) {
      this.selectedProjectId = projectId
      this.loading = true
      this.error = null
      try {
        const res = await reflectionsApi.fetchOverview(projectId)
        this.overview = res.data
      } catch (e) {
        this.error = e as ApiError
        throw e
      } finally {
        this.loading = false
      }
    },

    // プロジェクト未選択に戻すとき、または切り替え直後に古い一覧を消すために使う。
    clear() {
      this.selectedProjectId = null
      this.overview = null
      this.error = null
    },

    // 登録後オブジェクトをそのまま対象タスク行へ反映するため、一覧の再取得は不要。
    async createReflection(taskId: string, req: ReflectionRequest) {
      const res = await reflectionsApi.create(taskId, req)
      this.applyReflection(taskId, res.data)
      return res.data
    },

    async updateReflection(taskId: string, req: ReflectionRequest) {
      const res = await reflectionsApi.update(taskId, req)
      this.applyReflection(taskId, res.data)
      return res.data
    },

    applyReflection(taskId: string, reflection: ReflectionResponse) {
      if (!this.overview) return
      const task =
        this.overview.tasks.find((t) => t.id === taskId) ??
        this.overview.taskGroups.flatMap((g) => g.tasks).find((t) => t.id === taskId)
      if (task) {
        task.reflection = reflection
      }
    },
  },
})
