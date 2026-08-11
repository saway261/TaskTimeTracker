import { defineStore } from 'pinia'
import * as tasksApi from '@/api/tasksApi'
import * as memosApi from '@/api/memosApi'
import * as workSessionsApi from '@/api/workSessionsApi'
import type {
  TaskCreateRequest,
  TaskResponse,
  TaskUpdateEstimatedMinutesRequest,
  TaskUpdateFinishedRequest,
  TaskUpdateParentRequest,
  TaskUpdatePropertyRequest,
} from '@/types/task'
import type { MemoRequest, MemoResponse } from '@/types/memo'
import type { ApiError } from '@/types/apiError'
import { sortById } from '@/utils/sort'
import { isFinished as isTaskFinished } from '@/utils/task'

export const useTaskStore = defineStore('task', {
  state: () => ({
    tasks: [] as TaskResponse[],
    currentTask: null as TaskResponse | null,
    // 未完了タスクの実績集計用（§4.5）。完了済みタスクは actualMinutesCached 等を使うため null のまま。
    currentTaskActualMinutes: null as number | null,
    // 見積編集の可否判定用（セッションが1件でもあると不可）。
    currentTaskSessionCount: null as number | null,
    loading: false,
    error: null as ApiError | null,
  }),
  actions: {
    async fetchTasksInProject(projectId: number, isFinished?: boolean) {
      this.loading = true
      this.error = null
      try {
        const res = await tasksApi.fetchAllInProject(projectId, isFinished)
        this.tasks = sortById(res.data)
      } catch (e) {
        this.error = e as ApiError
        throw e
      } finally {
        this.loading = false
      }
    },

    async fetchTasksInTaskGroup(taskGroupId: number, isFinished?: boolean) {
      this.loading = true
      this.error = null
      try {
        const res = await tasksApi.fetchAllInTaskGroup(taskGroupId, isFinished)
        this.tasks = sortById(res.data)
      } catch (e) {
        this.error = e as ApiError
        throw e
      } finally {
        this.loading = false
      }
    },

    async fetchTask(id: number) {
      this.loading = true
      this.error = null
      this.currentTaskActualMinutes = null
      this.currentTaskSessionCount = null
      try {
        const res = await tasksApi.fetchById(id)
        this.currentTask = res.data
        if (!isTaskFinished(res.data)) {
          await this.loadEstimationContext(id)
        }
      } catch (e) {
        this.error = e as ApiError
        throw e
      } finally {
        this.loading = false
      }
    },

    // 未完了タスクの「実績」（total-minutes）と、見積編集可否の判定に使うセッション件数を取得する。
    async loadEstimationContext(taskId: number) {
      const [totalRes, sessionsRes] = await Promise.all([
        workSessionsApi.fetchTotalMinutes(taskId),
        workSessionsApi.fetchAllInTask(taskId),
      ])
      this.currentTaskActualMinutes = totalRes.data
      this.currentTaskSessionCount = sessionsRes.data.length
    },

    // 登録後オブジェクトをそのまま反映するため、再取得は不要。
    async createTaskInProject(projectId: number, req: TaskCreateRequest) {
      const res = await tasksApi.createInProject(projectId, req)
      this.tasks = sortById([...this.tasks, res.data])
      return res.data
    },

    async createTaskInTaskGroup(taskGroupId: number, req: TaskCreateRequest) {
      const res = await tasksApi.createInTaskGroup(taskGroupId, req)
      this.tasks = sortById([...this.tasks, res.data])
      return res.data
    },

    async updateTaskProperty(id: number, req: TaskUpdatePropertyRequest) {
      const res = await tasksApi.updateProperty(id, req)
      this.applyUpdated(res.data)
      return res.data
    },

    async updateEstimatedMinutes(id: number, req: TaskUpdateEstimatedMinutesRequest) {
      const res = await tasksApi.updateEstimatedMinutes(id, req)
      this.applyUpdated(res.data)
      return res.data
    },

    async updateFinished(id: number, req: TaskUpdateFinishedRequest) {
      const res = await tasksApi.updateFinished(id, req)
      this.applyUpdated(res.data)
      if (this.currentTask?.id === id) {
        if (isTaskFinished(res.data)) {
          this.currentTaskActualMinutes = null
          this.currentTaskSessionCount = null
        } else {
          await this.loadEstimationContext(id)
        }
      }
      return res.data
    },

    async updateTaskParent(id: number, req: TaskUpdateParentRequest) {
      const res = await tasksApi.updateParent(id, req)
      this.applyUpdated(res.data)
      return res.data
    },

    // B1修正済みのため、セッションを持つタスクも通常どおり削除できる前提。204のため本文は読まない。
    async removeTask(id: number) {
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
    async createTaskMemo(taskId: number, req: MemoRequest) {
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

    syncMemoRemoved(id: number) {
      if (!this.currentTask) return
      this.currentTask.memos = this.currentTask.memos.filter((m) => m.id !== id)
    },
  },
})
