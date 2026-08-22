import { defineStore } from 'pinia'
import * as taskGroupsApi from '@/api/taskGroupsApi'
import * as memosApi from '@/api/memosApi'
import type {
  TaskGroupCreateRequest,
  TaskGroupResponse,
  TaskGroupUpdateFinishedRequest,
  TaskGroupUpdateRequest,
} from '@/types/taskGroup'
import type { MemoRequest, MemoResponse } from '@/types/memo'
import type { ApiError } from '@/types/apiError'

export const useTaskGroupStore = defineStore('taskGroup', {
  state: () => ({
    taskGroups: [] as TaskGroupResponse[],
    currentTaskGroup: null as TaskGroupResponse | null,
    loading: false,
    error: null as ApiError | null,
  }),
  actions: {
    async fetchTaskGroups(projectId: string, isFinished?: boolean) {
      this.loading = true
      this.error = null
      try {
        const res = await taskGroupsApi.fetchAllInProject(projectId, isFinished)
        this.taskGroups = res.data
      } catch (e) {
        this.error = e as ApiError
        throw e
      } finally {
        this.loading = false
      }
    },

    async fetchTaskGroup(id: string) {
      this.loading = true
      this.error = null
      try {
        const res = await taskGroupsApi.fetchById(id)
        this.currentTaskGroup = res.data
      } catch (e) {
        this.error = e as ApiError
        throw e
      } finally {
        this.loading = false
      }
    },

    // 登録後オブジェクトをそのまま反映するため、再取得は不要。
    async createTaskGroup(projectId: string, req: TaskGroupCreateRequest) {
      const res = await taskGroupsApi.create(projectId, req)
      this.taskGroups = [...this.taskGroups, res.data]
      return res.data
    },

    // 更新後オブジェクトをそのまま反映するため、再取得は不要。
    async updateTaskGroup(id: string, req: TaskGroupUpdateRequest) {
      const res = await taskGroupsApi.update(id, req)
      if (this.currentTaskGroup?.id === id) {
        this.currentTaskGroup = res.data
      }
      const idx = this.taskGroups.findIndex((tg) => tg.id === id)
      if (idx !== -1) {
        this.taskGroups[idx] = res.data
      }
      return res.data
    },

    // 更新後オブジェクトをそのまま反映するため、再取得は不要。
    async updateFinished(id: string, req: TaskGroupUpdateFinishedRequest) {
      const res = await taskGroupsApi.updateFinished(id, req)
      if (this.currentTaskGroup?.id === id) {
        this.currentTaskGroup = res.data
      }
      const idx = this.taskGroups.findIndex((tg) => tg.id === id)
      if (idx !== -1) {
        this.taskGroups[idx] = res.data
      }
      return res.data
    },

    // メモCRUDはMemoResponseしか返らないため、currentTaskGroup.memos は自前で更新する。
    async createTaskGroupMemo(taskGroupId: string, req: MemoRequest) {
      const res = await memosApi.createMemoInTaskGroup(taskGroupId, req)
      if (this.currentTaskGroup?.id === taskGroupId) {
        this.currentTaskGroup.memos.push(res.data)
      }
      return res.data
    },

    syncMemoUpdated(memo: MemoResponse) {
      if (!this.currentTaskGroup) return
      const idx = this.currentTaskGroup.memos.findIndex((m) => m.id === memo.id)
      if (idx !== -1) {
        this.currentTaskGroup.memos[idx] = memo
      }
    },

    syncMemoRemoved(id: string) {
      if (!this.currentTaskGroup) return
      this.currentTaskGroup.memos = this.currentTaskGroup.memos.filter((m) => m.id !== id)
    },
  },
})
