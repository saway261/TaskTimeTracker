import { defineStore } from 'pinia'
import * as projectsApi from '@/api/projectsApi'
import * as memosApi from '@/api/memosApi'
import type {
  ProjectCreateRequest,
  ProjectResponse,
  ProjectUpdateFinishedRequest,
  ProjectUpdateRequest,
} from '@/types/project'
import type { MemoRequest, MemoResponse } from '@/types/memo'
import type { ApiError } from '@/types/apiError'
import { sortById } from '@/utils/sort'

export const useProjectStore = defineStore('project', {
  state: () => ({
    projects: [] as ProjectResponse[],
    currentProject: null as ProjectResponse | null,
    loading: false,
    error: null as ApiError | null,
  }),
  actions: {
    async fetchProjects(isFinished?: boolean) {
      this.loading = true
      this.error = null
      try {
        const res = await projectsApi.fetchAll(isFinished)
        this.projects = sortById(res.data)
      } catch (e) {
        this.error = e as ApiError
        throw e
      } finally {
        this.loading = false
      }
    },

    async fetchProject(id: number) {
      this.loading = true
      this.error = null
      try {
        const res = await projectsApi.fetchById(id)
        this.currentProject = res.data
      } catch (e) {
        this.error = e as ApiError
        throw e
      } finally {
        this.loading = false
      }
    },

    // 登録後オブジェクトをそのまま反映するため、再取得は不要。
    async createProject(req: ProjectCreateRequest) {
      const res = await projectsApi.create(req)
      this.projects = sortById([...this.projects, res.data])
      return res.data
    },

    // 更新後オブジェクトをそのまま反映するため、再取得は不要。
    async updateProject(id: number, req: ProjectUpdateRequest) {
      const res = await projectsApi.update(id, req)
      if (this.currentProject?.id === id) {
        this.currentProject = res.data
      }
      const idx = this.projects.findIndex((p) => p.id === id)
      if (idx !== -1) {
        this.projects[idx] = res.data
      }
      return res.data
    },

    // 更新後オブジェクトをそのまま反映するため、再取得は不要。
    async updateFinished(id: number, req: ProjectUpdateFinishedRequest) {
      const res = await projectsApi.updateFinished(id, req)
      if (this.currentProject?.id === id) {
        this.currentProject = res.data
      }
      const idx = this.projects.findIndex((p) => p.id === id)
      if (idx !== -1) {
        this.projects[idx] = res.data
      }
      return res.data
    },

    // メモCRUDはMemoResponseしか返らないため、currentProject.memos は自前で更新する。
    async createProjectMemo(projectId: number, req: MemoRequest) {
      const res = await memosApi.createMemoInProject(projectId, req)
      if (this.currentProject?.id === projectId) {
        this.currentProject.memos.push(res.data)
      }
      return res.data
    },

    syncMemoUpdated(memo: MemoResponse) {
      if (!this.currentProject) return
      const idx = this.currentProject.memos.findIndex((m) => m.id === memo.id)
      if (idx !== -1) {
        this.currentProject.memos[idx] = memo
      }
    },

    syncMemoRemoved(id: number) {
      if (!this.currentProject) return
      this.currentProject.memos = this.currentProject.memos.filter((m) => m.id !== id)
    },
  },
})
