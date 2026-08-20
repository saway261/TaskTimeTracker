<script setup lang="ts">
import { computed, ref } from 'vue'
import { useItemOrderStore } from '@/stores/itemOrderStore'
import type { TaskResponse } from '@/types/task'
import type { TaskGroupResponse } from '@/types/taskGroup'
import { isFinished } from '@/utils/task'
import { formatMinutes } from '@/utils/duration'
import TaskRowMenu from './TaskRowMenu.vue'
import TaskQuickActionModal from './TaskQuickActionModal.vue'

const props = defineProps<{
  task: TaskResponse
  to: string
  // 並べ替え・移動に必要な文脈（§7.4）。
  projectId: number
  containerKey: string // 'project:{pId}' | 'taskGroup:{tgId}'
  taskGroups: TaskGroupResponse[]
  canMoveUp: boolean
  canMoveDown: boolean
}>()

const emit = defineEmits<{
  'move-up': []
  'move-down': []
  'item-drop': []
}>()

const itemOrderStore = useItemOrderStore()

const finished = computed(() => isFinished(props.task))
const itemKey = computed(() => `TASK:${props.task.id}`)
const isDragOverTarget = computed(
  () =>
    itemOrderStore.dragOverTarget?.container === props.containerKey &&
    itemOrderStore.dragOverTarget?.beforeKey === itemKey.value,
)

function handleDragStart(e: DragEvent) {
  itemOrderStore.startDrag({
    kind: 'TASK',
    id: props.task.id,
    sourceContainer: props.containerKey,
  })
  e.dataTransfer?.setData('text/plain', itemKey.value)
  if (e.dataTransfer) e.dataTransfer.effectAllowed = 'move'
}

function handleDragEnd() {
  itemOrderStore.endDrag()
}

function handleDragOver(e: DragEvent) {
  if (!itemOrderStore.draggedItem) return
  e.preventDefault()
  itemOrderStore.setDragOverTarget({ container: props.containerKey, beforeKey: itemKey.value })
}

function handleDrop(e: DragEvent) {
  if (!itemOrderStore.draggedItem) return
  e.preventDefault()
  emit('item-drop')
}

const showMenu = ref(false)
const showQuickActions = ref(false)
</script>

<template>
  <div
    class="task-row-wrapper"
    :class="{ 'drag-over': isDragOverTarget }"
    @dragover="handleDragOver"
    @drop="handleDrop"
  >
    <span
      class="drag-handle"
      draggable="true"
      role="button"
      tabindex="-1"
      aria-label="ドラッグして並べ替え"
      @dragstart="handleDragStart"
      @dragend="handleDragEnd"
      @click.prevent
    >
      ⠿
    </span>

    <button
      type="button"
      class="task-row"
      :class="{ finished }"
      aria-haspopup="dialog"
      @click="showQuickActions = true"
    >
      <span class="label">タスク</span>
      <span class="title">{{ task.title }}</span>
      <span v-if="task.estimatedMinutes !== null" class="estimate">
        見積 {{ formatMinutes(task.estimatedMinutes) }}
      </span>
      <span class="status" :class="{ finished }">
        {{ finished ? '完了' : '未完了' }}
      </span>
    </button>

    <button type="button" class="menu-button" aria-label="タスクを操作" @click="showMenu = true">
      ⋮
    </button>

    <TaskRowMenu
      v-model="showMenu"
      :task-id="task.id"
      :project-id="projectId"
      :container-key="containerKey"
      :task-groups="taskGroups"
      :finished="finished"
      :can-move-up="canMoveUp"
      :can-move-down="canMoveDown"
      @move-up="emit('move-up')"
      @move-down="emit('move-down')"
    />

    <TaskQuickActionModal
      v-model="showQuickActions"
      :task-id="task.id"
      :task-title="task.title"
      :detail-to="to"
    />
  </div>
</template>

<style scoped>
.task-row-wrapper {
  display: flex;
  align-items: stretch;
  gap: 0.3em;
  border-top: 2px solid transparent;
}

.task-row-wrapper.drag-over {
  border-top-color: var(--color-accent);
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

.task-row {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 0.6em;
  min-width: 0;
  padding: 0.8em 1em;
  border-radius: 8px;
  background-color: var(--color-surface);
  border: 1px solid var(--color-surface-muted);
  border-left: 4px solid var(--color-task-accent);
  color: var(--color-text);
  font: inherit;
  text-align: left;
  text-decoration: none;
  cursor: pointer;
}

.task-row:hover {
  background-color: var(--color-surface-muted);
}

.task-row:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: -2px;
}

.task-row.finished {
  border-left-color: var(--color-text-muted);
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

.estimate {
  flex-shrink: 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.status {
  flex-shrink: 0;
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

@media (max-width: 640px) {
  .drag-handle,
  .label,
  .estimate,
  .status {
    display: none;
  }

  .task-row-wrapper {
    gap: 0;
  }

  .task-row {
    padding: 0.75em 0.8em;
  }

  .menu-button {
    width: 2.5em;
  }
}
</style>
