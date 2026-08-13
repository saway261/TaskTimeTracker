import { defineStore } from 'pinia'
import * as itemOrderApi from '@/api/itemOrderApi'
import { useTaskStore } from '@/stores/taskStore'
import type {
  ItemType,
  ProjectItemOrderItemRequest,
  ProjectItemOrderResponse,
  TaskGroupItemOrderItemRequest,
  TaskGroupItemOrderResponse,
} from '@/types/itemOrder'
import type { TaskUpdateParentRequest } from '@/types/task'
import type { ApiError } from '@/types/apiError'

// コンテナ識別子。'project:{pId}' | 'taskGroup:{tgId}'。
export type ContainerKey = string

export function projectContainerKey(projectId: number): ContainerKey {
  return `project:${projectId}`
}

export function taskGroupContainerKey(taskGroupId: number): ContainerKey {
  return `taskGroup:${taskGroupId}`
}

export interface DragPayload {
  kind: ItemType
  id: number
  sourceContainer: ContainerKey
}

// ドラッグ中にどのコンテナのどの項目の直前へ挿入するか。beforeId===nullは末尾を表す。
export interface DragOverTarget {
  container: ContainerKey
  beforeKey: string | null
}

export const useItemOrderStore = defineStore('itemOrder', {
  state: () => ({
    projectItemOrder: [] as ProjectItemOrderResponse[],
    // taskGroupIdをキーにしたマップ。Project詳細画面で複数のタスクグループを同時に展開できるため（§7.4.4）。
    taskGroupItemOrders: {} as Record<number, TaskGroupItemOrderResponse[]>,
    // 送信中のコンテナ識別子。多重送信防止用。他のコンテナの操作は止めない。
    reorderingContainer: null as ContainerKey | null,
    draggedItem: null as DragPayload | null,
    dragOverTarget: null as DragOverTarget | null,
  }),
  actions: {
    async fetchProjectItemOrder(projectId: number) {
      const res = await itemOrderApi.fetchProjectItemOrder(projectId)
      this.projectItemOrder = res.data
    },

    // 取得済みならスキップする（展開のたびに再取得しない。forceで強制再取得）。
    async fetchTaskGroupItemOrder(taskGroupId: number, force = false) {
      if (!force && this.taskGroupItemOrders[taskGroupId]) return
      const res = await itemOrderApi.fetchTaskGroupItemOrder(taskGroupId)
      this.taskGroupItemOrders = { ...this.taskGroupItemOrders, [taskGroupId]: res.data }
    },

    // 楽観的更新：先にローカルの並び順を入れ替えて描画し、APIを呼ぶ。失敗したら元に戻す（§7.4.4）。
    async reorderProjectItems(projectId: number, items: ProjectItemOrderItemRequest[]) {
      const containerKey = projectContainerKey(projectId)
      const previous = this.projectItemOrder
      this.reorderingContainer = containerKey
      this.projectItemOrder = items.map((item, index) => ({ ...item, position: index }))
      try {
        const res = await itemOrderApi.replaceProjectItemOrder(projectId, { items })
        this.projectItemOrder = res.data
      } catch (e) {
        // 他の場所で変更があり項目が一致しなかった場合（invalid item order）は、ロールバック先の
        // ローカル値自体が古い可能性があるため、素直に戻さず最新のitem-orderを取り直す（§7.4.6）。
        if ((e as ApiError).kind === 'businessRule') {
          await this.fetchProjectItemOrder(projectId).catch(() => {
            this.projectItemOrder = previous
          })
        } else {
          this.projectItemOrder = previous
        }
        throw e
      } finally {
        this.reorderingContainer = null
      }
    },

    async reorderTaskGroupItems(taskGroupId: number, items: TaskGroupItemOrderItemRequest[]) {
      const containerKey = taskGroupContainerKey(taskGroupId)
      const previous = this.taskGroupItemOrders[taskGroupId] ?? []
      this.reorderingContainer = containerKey
      this.taskGroupItemOrders = {
        ...this.taskGroupItemOrders,
        [taskGroupId]: items.map((item, index) => ({ ...item, position: index })),
      }
      try {
        const res = await itemOrderApi.replaceTaskGroupItemOrder(taskGroupId, { items })
        this.taskGroupItemOrders = { ...this.taskGroupItemOrders, [taskGroupId]: res.data }
      } catch (e) {
        if ((e as ApiError).kind === 'businessRule') {
          await this.fetchTaskGroupItemOrder(taskGroupId, true).catch(() => {
            this.taskGroupItemOrders = { ...this.taskGroupItemOrders, [taskGroupId]: previous }
          })
        } else {
          this.taskGroupItemOrders = { ...this.taskGroupItemOrders, [taskGroupId]: previous }
        }
        throw e
      } finally {
        this.reorderingContainer = null
      }
    },

    // メニュー操作での所属変更（ケースB。位置は常に末尾でよい仕様のため §7.4.5 rule 4）。
    // 並べ替えPUTは呼ばず、対象コンテナのitem-orderがキャッシュ済みならローカルで末尾へ追記するだけで
    // 整合する（バックエンドが常に末尾へ追加するため）。移動元コンテナへのリクエストも不要（§7.4.2 #7-b）。
    async moveTaskViaMenu(params: {
      taskId: number
      parentReq: TaskUpdateParentRequest
      sourceContainer: ContainerKey
      targetContainer: ContainerKey
    }) {
      const taskStore = useTaskStore()
      await taskStore.updateTaskParent(params.taskId, params.parentReq)
      this.removeFromContainerOrder(params.sourceContainer, 'TASK', params.taskId)
      this.appendToContainerOrder(params.targetContainer, 'TASK', params.taskId)
    },

    // ドラッグ操作での所属変更（ケースB。ドロップ位置を尊重する）。
    // 所属変更が成功した後に並べ替えが失敗しても、所属変更はロールバックしない（§7.4.5 中間状態）。
    async moveTaskAcrossContainer(params: {
      taskId: number
      parentReq: TaskUpdateParentRequest
      sourceContainer: ContainerKey
      target:
        | { type: 'project'; projectId: number; items: ProjectItemOrderItemRequest[] }
        | { type: 'taskGroup'; taskGroupId: number; items: TaskGroupItemOrderItemRequest[] }
    }): Promise<{ reorderFailed: boolean }> {
      const taskStore = useTaskStore()
      await taskStore.updateTaskParent(params.taskId, params.parentReq)
      this.removeFromContainerOrder(params.sourceContainer, 'TASK', params.taskId)
      try {
        if (params.target.type === 'project') {
          await this.reorderProjectItems(params.target.projectId, params.target.items)
        } else {
          await this.reorderTaskGroupItems(params.target.taskGroupId, params.target.items)
        }
        return { reorderFailed: false }
      } catch {
        return { reorderFailed: true }
      }
    },

    removeFromContainerOrder(containerKey: ContainerKey, kind: ItemType, id: number) {
      if (containerKey.startsWith('project:')) {
        this.projectItemOrder = this.projectItemOrder.filter(
          (o) => !(o.type === kind && o.id === id),
        )
        return
      }
      const taskGroupId = Number(containerKey.split(':')[1])
      const current = this.taskGroupItemOrders[taskGroupId]
      if (!current) return
      this.taskGroupItemOrders = {
        ...this.taskGroupItemOrders,
        [taskGroupId]: current.filter((o) => o.id !== id),
      }
    },

    appendToContainerOrder(containerKey: ContainerKey, kind: ItemType, id: number) {
      if (containerKey.startsWith('project:')) {
        const nextPosition = Math.max(-1, ...this.projectItemOrder.map((o) => o.position)) + 1
        this.projectItemOrder = [
          ...this.projectItemOrder,
          { type: kind, id, position: nextPosition },
        ]
        return
      }
      const taskGroupId = Number(containerKey.split(':')[1])
      const current = this.taskGroupItemOrders[taskGroupId]
      if (!current) return // 未取得（未展開）のコンテナはローカル反映不要。次回展開時に取得される。
      const nextPosition = Math.max(-1, ...current.map((o) => o.position)) + 1
      this.taskGroupItemOrders = {
        ...this.taskGroupItemOrders,
        [taskGroupId]: [...current, { id, position: nextPosition }],
      }
    },

    startDrag(payload: DragPayload) {
      this.draggedItem = payload
    },

    setDragOverTarget(target: DragOverTarget | null) {
      this.dragOverTarget = target
    },

    endDrag() {
      this.draggedItem = null
      this.dragOverTarget = null
    },
  },
})
