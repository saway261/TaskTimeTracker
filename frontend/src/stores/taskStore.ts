import { defineStore } from 'pinia'
import * as tasksApi from '@/api/tasksApi'
import * as memosApi from '@/api/memosApi'
import type {
  TaskCreateRequest,
  TaskResponse,
  TaskTagsUpdateRequest,
  TaskUpdateEstimatedMinutesRequest,
  TaskUpdateFinishedRequest,
  TaskUpdateParentRequest,
  TaskUpdatePropertyRequest,
} from '@/types/task'
import type { MemoRequest, MemoResponse } from '@/types/memo'
import type { ApiError } from '@/types/apiError'

export const useTaskStore = defineStore('task', {
  state: () => ({
    tasks: [] as TaskResponse[],
    currentTask: null as TaskResponse | null,
    loading: false,
    error: null as ApiError | null,
  }),
  actions: {
    async fetchTasksInProject(projectId: string, isFinished?: boolean) {
      this.loading = true
      this.error = null
      try {
        const res = await tasksApi.fetchAllInProject(projectId, isFinished)
        this.tasks = res.data
      } catch (e) {
        this.error = e as ApiError
        throw e
      } finally {
        this.loading = false
      }
    },

    async fetchTasksInTaskGroup(taskGroupId: string, isFinished?: boolean) {
      this.loading = true
      this.error = null
      try {
        const res = await tasksApi.fetchAllInTaskGroup(taskGroupId, isFinished)
        this.tasks = res.data
      } catch (e) {
        this.error = e as ApiError
        throw e
      } finally {
        this.loading = false
      }
    },

    async fetchTask(id: string) {
      this.loading = true
      this.error = null
      try {
        return await this.fetchTaskForInteraction(id)
      } catch (e) {
        this.error = e as ApiError
        throw e
      } finally {
        this.loading = false
      }
    },

    // 一覧上の操作モーダルや作業セッション更新後の再取得用。
    // 一覧全体のローディング状態を変更せず、開いているモーダルがアンマウントされるのを防ぐ。
    async fetchTaskForInteraction(id: string) {
      const res = await tasksApi.fetchById(id)
      this.currentTask = res.data
      const idx = this.tasks.findIndex((task) => task.id === id)
      if (idx !== -1) {
        this.tasks[idx] = res.data
      }
      return res.data
    },

    // 登録後オブジェクトをそのまま反映するため、再取得は不要。
    async createTaskInProject(projectId: string, req: TaskCreateRequest) {
      const res = await tasksApi.createInProject(projectId, req)
      this.tasks = [...this.tasks, res.data]
      return res.data
    },

    async createTaskInTaskGroup(taskGroupId: string, req: TaskCreateRequest) {
      const res = await tasksApi.createInTaskGroup(taskGroupId, req)
      this.tasks = [...this.tasks, res.data]
      return res.data
    },

    async updateTaskProperty(id: string, req: TaskUpdatePropertyRequest) {
      const res = await tasksApi.updateProperty(id, req)
      this.applyUpdated(res.data)
      return res.data
    },

    async updateEstimatedMinutes(id: string, req: TaskUpdateEstimatedMinutesRequest) {
      const res = await tasksApi.updateEstimatedMinutes(id, req)
      this.applyUpdated(res.data)
      return res.data
    },

    async updateFinished(id: string, req: TaskUpdateFinishedRequest) {
      const res = await tasksApi.updateFinished(id, req)
      this.applyUpdated(res.data)
      return res.data
    },

    async updateTaskParent(id: string, req: TaskUpdateParentRequest) {
      const res = await tasksApi.updateParent(id, req)
      this.applyUpdated(res.data)
      return res.data
    },

    async updateTaskTags(id: string, req: TaskTagsUpdateRequest) {
      const res = await tasksApi.updateTags(id, req)
      this.applyUpdated(res.data)
      return res.data
    },

    // B1修正済みのため、セッションを持つタスクも通常どおり削除できる前提。204のため本文は読まない。
    async removeTask(id: string) {
      await tasksApi.remove(id)
      this.tasks = this.tasks.filter((t) => t.id !== id)
      if (this.currentTask?.id === id) {
        this.currentTask = null
      }
    },

    applyUpdated(task: TaskResponse) {
      if (this.currentTask?.id === task.id) {
        this.currentTask = task
      }
      const idx = this.tasks.findIndex((t) => t.id === task.id)
      if (idx !== -1) {
        this.tasks[idx] = task
      }
    },

    // メモCRUDはMemoResponseしか返らないため、currentTask.memos は自前で更新する。
    async createTaskMemo(taskId: string, req: MemoRequest) {
      const res = await memosApi.createMemoInTask(taskId, req)
      if (this.currentTask?.id === taskId) {
        this.currentTask.memos.push(res.data)
      }
      return res.data
    },

    syncMemoUpdated(memo: MemoResponse) {
      if (!this.currentTask) return
      const idx = this.currentTask.memos.findIndex((m) => m.id === memo.id)
      if (idx !== -1) {
        this.currentTask.memos[idx] = memo
      }
    },

    syncMemoRemoved(id: string) {
      if (!this.currentTask) return
      this.currentTask.memos = this.currentTask.memos.filter((m) => m.id !== id)
    },
  },
})
