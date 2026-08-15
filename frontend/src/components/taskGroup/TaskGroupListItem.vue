<script setup lang="ts">
import { computed, ref } from 'vue'
import { useTaskStore } from '@/stores/taskStore'
import {
  projectContainerKey,
  taskGroupContainerKey,
  useItemOrderStore,
} from '@/stores/itemOrderStore'
import { useNotificationStore } from '@/stores/notificationStore'
import type { TaskGroupResponse } from '@/types/taskGroup'
import type { TaskCreateRequest } from '@/types/task'
import type { ApiError } from '@/types/apiError'
import { sortByItemOrder } from '@/utils/sort'
import { insertStubAt } from '@/utils/dragReorder'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import TaskForm from '@/components/task/TaskForm.vue'
import TaskListItem from '@/components/task/TaskListItem.vue'
import TaskGroupRowMenu from './TaskGroupRowMenu.vue'

const props = defineProps<{
  taskGroup: TaskGroupResponse
  // 兄弟タスクグループ一覧。配下タスクの「移動先」メニュー用にTaskListItemへ渡す。
  taskGroups: TaskGroupResponse[]
  canMoveUp: boolean
  canMoveDown: boolean
}>()

const emit = defineEmits<{
  'move-up': []
  'move-down': []
  'item-drop': []
}>()

const taskStore = useTaskStore()
const itemOrderStore = useItemOrderStore()
const notification = useNotificationStore()

const isOpen = ref(false)

async function toggle() {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    // 展開のたびに再取得しない（取得済みならスキップ）。§7.4.5
    await itemOrderStore.fetchTaskGroupItemOrder(props.taskGroup.id).catch(() => {})
  }
}

// 呼び出し元（ProjectDetailView）が taskStore.fetchTasksInProject() で配下タスクグループ分も
// 含めて取得済みであることを前提に、このタスクグループに属するタスクだけを絞り込む。
const childTasks = computed(() =>
  taskStore.tasks.filter((t) => t.taskGroupId === props.taskGroup.id),
)

const containerKey = computed(() => taskGroupContainerKey(props.taskGroup.id))

const orderedChildTasks = computed(() =>
  sortByItemOrder(childTasks.value, itemOrderStore.taskGroupItemOrders[props.taskGroup.id] ?? []),
)

async function handleChildMoveUp(index: number) {
  if (index <= 0) return
  const items = orderedChildTasks.value.map((t) => ({ id: t.id }))
  ;[items[index - 1], items[index]] = [items[index], items[index - 1]]
  try {
    await itemOrderStore.reorderTaskGroupItems(props.taskGroup.id, items)
  } catch (e) {
    notification.error((e as ApiError).message)
  }
}

async function handleChildMoveDown(index: number) {
  if (index >= orderedChildTasks.value.length - 1) return
  const items = orderedChildTasks.value.map((t) => ({ id: t.id }))
  ;[items[index], items[index + 1]] = [items[index + 1], items[index]]
  try {
    await itemOrderStore.reorderTaskGroupItems(props.taskGroup.id, items)
  } catch (e) {
    notification.error((e as ApiError).message)
  }
}

// 配下タスクの一覧内での並べ替え・別コンテナからの受け入れ（ケースA-2・B-1/B-2）。
async function handleChildDrop() {
  const dragged = itemOrderStore.draggedItem
  const target = itemOrderStore.dragOverTarget
  if (!dragged || !target || target.container !== containerKey.value) return
  if (dragged.kind !== 'TASK') return // タスクグループはタスクグループの中へ入れられない

  const draggedStub = { key: `TASK:${dragged.id}` }
  const currentStubs = orderedChildTasks.value.map((t) => ({ key: `TASK:${t.id}` }))
  const newStubs = insertStubAt(currentStubs, draggedStub, target.beforeKey)
  const items = newStubs.map((s) => ({ id: Number(s.key.split(':')[1]) }))

  if (dragged.sourceContainer === containerKey.value) {
    try {
      await itemOrderStore.reorderTaskGroupItems(props.taskGroup.id, items)
    } catch (e) {
      notification.error((e as ApiError).message)
    }
    return
  }

  try {
    const result = await itemOrderStore.moveTaskAcrossContainer({
      taskId: dragged.id,
      parentReq: { projectId: null, taskGroupId: props.taskGroup.id },
      sourceContainer: dragged.sourceContainer,
      target: { type: 'taskGroup', taskGroupId: props.taskGroup.id, items },
    })
    if (result.reorderFailed) {
      notification.info(
        'タスクを移動しましたが、並べ替えには失敗しました。最新の状態を読み込み直してください。',
      )
    } else {
      notification.success('タスクを移動しました。')
    }
  } catch (e) {
    notification.error((e as ApiError).message)
  }
}

// --- 自分自身（Project直下での並べ替え対象・ドラッグ対象） ---
const projectKey = computed(() => projectContainerKey(props.taskGroup.projectId))
const groupItemKey = computed(() => `TASK_GROUP:${props.taskGroup.id}`)
const isDragOverAsGroup = computed(
  () =>
    itemOrderStore.dragOverTarget?.container === projectKey.value &&
    itemOrderStore.dragOverTarget?.beforeKey === groupItemKey.value,
)
const showTaskDropHighlight = ref(false)

function handleSelfDragStart(e: DragEvent) {
  itemOrderStore.startDrag({
    kind: 'TASK_GROUP',
    id: props.taskGroup.id,
    sourceContainer: projectKey.value,
  })
  e.dataTransfer?.setData('text/plain', groupItemKey.value)
  if (e.dataTransfer) e.dataTransfer.effectAllowed = 'move'
}

function handleSelfDragEnd() {
  itemOrderStore.endDrag()
}

// ヘッダーへのドロップは種別で意味が変わる：
// TASK_GROUP → Project直下でこのグループの直前へ挿入（並べ替え）
// TASK       → このグループの配下へ移動（ケースB-4。位置指定できないため末尾へ入る）
function handleHeaderDragOver(e: DragEvent) {
  const dragged = itemOrderStore.draggedItem
  if (!dragged) return
  e.preventDefault()
  if (dragged.kind === 'TASK_GROUP') {
    itemOrderStore.setDragOverTarget({ container: projectKey.value, beforeKey: groupItemKey.value })
    showTaskDropHighlight.value = false
  } else {
    showTaskDropHighlight.value = true
  }
}

function handleHeaderDragLeave() {
  showTaskDropHighlight.value = false
}

async function handleHeaderDrop(e: DragEvent) {
  const dragged = itemOrderStore.draggedItem
  if (!dragged) return
  e.preventDefault()
  showTaskDropHighlight.value = false

  if (dragged.kind === 'TASK_GROUP') {
    emit('item-drop')
    return
  }

  if (dragged.sourceContainer === containerKey.value) return // 既にこのグループの子

  try {
    await itemOrderStore.moveTaskViaMenu({
      taskId: dragged.id,
      parentReq: { projectId: null, taskGroupId: props.taskGroup.id },
      sourceContainer: dragged.sourceContainer,
      targetContainer: containerKey.value,
    })
    notification.success('タスクを移動しました。')
  } catch (e2) {
    notification.error((e2 as ApiError).message)
  }
}

const showRowMenu = ref(false)

const showCreateTaskModal = ref(false)
const creatingTask = ref(false)
const createTaskError = ref<ApiError | null>(null)

function openCreateTaskModal() {
  createTaskError.value = null
  showCreateTaskModal.value = true
}

async function handleCreateTask(payload: {
  title: string
  description: string | null
  estimatedMinutes?: number
}) {
  creatingTask.value = true
  createTaskError.value = null
  try {
    const task = await taskStore.createTaskInTaskGroup(
      props.taskGroup.id,
      payload as TaskCreateRequest,
    )
    // 新規タスクは必ず末尾に追加される（§7.4.2 #6）。取得済みならローカルでも末尾へ追記する。
    itemOrderStore.appendToContainerOrder(containerKey.value, 'TASK', task.id)
    notification.success('タスクを登録しました。')
    showCreateTaskModal.value = false
  } catch (e) {
    createTaskError.value = e as ApiError
  } finally {
    creatingTask.value = false
  }
}
</script>

<template>
  <div class="task-group-wrapper" :class="{ 'drag-over': isDragOverAsGroup }">
    <div class="task-group-header-row">
      <span
        class="drag-handle"
        draggable="true"
        role="button"
        tabindex="-1"
        aria-label="ドラッグして並べ替え"
        @dragstart="handleSelfDragStart"
        @dragend="handleSelfDragEnd"
        @click.prevent
      >
        ⠿
      </span>

      <div
        class="task-group-card"
        :class="{ finished: taskGroup.isFinished, 'task-drop-target': showTaskDropHighlight }"
      >
        <div class="card-header">
          <button
            type="button"
            class="row-header"
            :aria-expanded="isOpen"
            @click="toggle"
            @dragover="handleHeaderDragOver"
            @dragleave="handleHeaderDragLeave"
            @drop="handleHeaderDrop"
          >
            <span class="chevron" :class="{ open: isOpen }" aria-hidden="true">›</span>
            <span class="label">グループ</span>
            <span class="title">{{ taskGroup.title }}</span>
          </button>

          <BaseButton
            v-if="isOpen"
            variant="secondary"
            class="inline-add-task"
            @click="openCreateTaskModal"
          >
            ＋ タスク追加
          </BaseButton>

          <span class="status" :class="{ finished: taskGroup.isFinished }">
            {{ taskGroup.isFinished ? '完了' : '未完了' }}
          </span>

          <button
            type="button"
            class="menu-button"
            aria-label="タスクグループを操作"
            @click="showRowMenu = true"
          >
            ⋮
          </button>
        </div>

        <p v-if="isOpen && taskGroup.description" class="description">
          {{ taskGroup.description }}
        </p>
      </div>
    </div>

    <div v-if="isOpen" class="child-tasks">
      <p v-if="orderedChildTasks.length === 0" class="empty">タスクがまだありません。</p>
      <TaskListItem
        v-for="(task, index) in orderedChildTasks"
        :key="task.id"
        :task="task"
        :to="`/projects/${taskGroup.projectId}/task-groups/${taskGroup.id}/tasks/${task.id}`"
        :project-id="taskGroup.projectId"
        :container-key="containerKey"
        :task-groups="taskGroups"
        :can-move-up="index > 0"
        :can-move-down="index < orderedChildTasks.length - 1"
        @move-up="handleChildMoveUp(index)"
        @move-down="handleChildMoveDown(index)"
        @item-drop="handleChildDrop"
      />
    </div>

    <TaskGroupRowMenu
      v-model="showRowMenu"
      :detail-to="`/projects/${taskGroup.projectId}/task-groups/${taskGroup.id}`"
      :can-move-up="canMoveUp"
      :can-move-down="canMoveDown"
      @add-task="openCreateTaskModal"
      @move-up="emit('move-up')"
      @move-down="emit('move-down')"
    />

    <BaseModal v-model="showCreateTaskModal" title="新規タスク">
      <TaskForm
        :submitting="creatingTask"
        :error="createTaskError"
        @submit="handleCreateTask"
        @cancel="showCreateTaskModal = false"
      />
    </BaseModal>
  </div>
</template>

<style scoped>
.task-group-wrapper {
  display: flex;
  flex-direction: column;
  gap: 0.5em;
  border-top: 2px solid transparent;
}

.task-group-wrapper.drag-over {
  border-top-color: var(--color-accent);
}

.task-group-header-row {
  display: flex;
  align-items: stretch;
  gap: 0.3em;
}

.drag-handle {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 0 0.3em;
  color: var(--color-text-muted);
  cursor: grab;
  user-select: none;
}

.drag-handle:active {
  cursor: grabbing;
}

.task-group-card {
  flex: 1;
  min-width: 0;
  border-radius: 8px;
  background-color: var(--color-surface);
  border: 1px solid var(--color-surface-muted);
  border-left: 4px solid var(--color-accent);
  overflow: hidden;
}

.task-group-card.finished {
  border-left-color: var(--color-text-muted);
}

.task-group-card.task-drop-target {
  border-left-color: var(--color-task-accent);
  background-color: var(--color-surface-muted);
}

.card-header {
  display: flex;
  align-items: stretch;
  gap: 0.6em;
}

.row-header {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 0.6em;
  padding: 0.8em 1em;
  background: none;
  border: none;
  cursor: pointer;
  text-align: left;
  color: var(--color-text);
  font: inherit;
}

.inline-add-task {
  flex-shrink: 0;
  align-self: center;
}

@media (max-width: 640px) {
  .inline-add-task {
    display: none;
  }
}

.row-header:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: -2px;
}

.chevron {
  display: inline-block;
  font-size: 1.1rem;
  color: var(--color-text-muted);
  transition: transform 0.15s ease;
}

.chevron.open {
  transform: rotate(90deg);
}

.label {
  flex-shrink: 0;
  font-size: 0.75rem;
  padding: 0.15em 0.5em;
  border-radius: 4px;
  background-color: var(--color-surface-muted);
  color: var(--color-text-muted);
}

.title {
  flex: 1;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status {
  flex-shrink: 0;
  align-self: center;
  padding-right: 0.4em;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.status.finished {
  color: var(--color-success);
}

.menu-button {
  flex-shrink: 0;
  width: 2.2em;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.1rem;
  color: var(--color-text-muted);
}

.menu-button:hover {
  color: var(--color-text);
}

.menu-button:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.description {
  margin: 0;
  padding: 0 1em 1em;
  color: var(--color-text);
  font-size: 0.9rem;
}

.child-tasks {
  display: flex;
  flex-direction: column;
  gap: 0.5em;
  padding-left: 2em;
}

.empty {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}
</style>
