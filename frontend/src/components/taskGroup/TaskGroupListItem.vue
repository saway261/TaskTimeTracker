<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useTaskStore } from '@/stores/taskStore'
import { useNotificationStore } from '@/stores/notificationStore'
import type { TaskGroupResponse } from '@/types/taskGroup'
import type { TaskCreateRequest } from '@/types/task'
import type { ApiError } from '@/types/apiError'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import TaskForm from '@/components/task/TaskForm.vue'
import TaskListItem from '@/components/task/TaskListItem.vue'

const props = defineProps<{
  taskGroup: TaskGroupResponse
}>()

const taskStore = useTaskStore()
const notification = useNotificationStore()

const isOpen = ref(false)

function toggle() {
  isOpen.value = !isOpen.value
}

// 呼び出し元（ProjectDetailView）が taskStore.fetchTasksInProject() で配下タスクグループ分も
// 含めて取得済みであることを前提に、このタスクグループに属するタスクだけを絞り込む。
const childTasks = computed(() =>
  taskStore.tasks.filter((t) => t.taskGroupId === props.taskGroup.id),
)

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
    await taskStore.createTaskInTaskGroup(props.taskGroup.id, payload as TaskCreateRequest)
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
  <div class="task-group-row" :class="{ finished: taskGroup.isFinished }">
    <button type="button" class="row-header" :aria-expanded="isOpen" @click="toggle">
      <span class="chevron" :class="{ open: isOpen }" aria-hidden="true">›</span>
      <span class="label">タスクグループ</span>
      <span class="title">{{ taskGroup.title }}</span>
      <span class="status" :class="{ finished: taskGroup.isFinished }">
        {{ taskGroup.isFinished ? '完了' : '未完了' }}
      </span>
    </button>

    <div v-if="isOpen" class="row-body">
      <p v-if="taskGroup.description" class="description">{{ taskGroup.description }}</p>

      <div class="child-tasks">
        <p v-if="childTasks.length === 0" class="empty">タスクがまだありません。</p>
        <TaskListItem
          v-for="task in childTasks"
          :key="task.id"
          :task="task"
          :to="`/projects/${taskGroup.projectId}/task-groups/${taskGroup.id}/tasks/${task.id}`"
        />
      </div>

      <div class="row-body-actions">
        <RouterLink
          :to="`/projects/${taskGroup.projectId}/task-groups/${taskGroup.id}`"
          class="detail-link"
        >
          タスクグループの詳細・編集・メモへ →
        </RouterLink>
        <BaseButton variant="secondary" @click="openCreateTaskModal">＋ タスク追加</BaseButton>
      </div>
    </div>

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
.task-group-row {
  border-radius: 8px;
  background-color: var(--color-surface);
  border: 1px solid var(--color-surface-muted);
  border-left: 4px solid var(--color-accent);
  overflow: hidden;
}

.task-group-row.finished {
  border-left-color: var(--color-text-muted);
}

.row-header {
  display: flex;
  align-items: center;
  gap: 0.6em;
  width: 100%;
  padding: 0.8em 1em;
  background: none;
  border: none;
  cursor: pointer;
  text-align: left;
  color: var(--color-text);
  font: inherit;
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
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.status.finished {
  color: var(--color-success);
}

.row-body {
  padding: 0 1em 1em 2.6em;
  display: flex;
  flex-direction: column;
  gap: 0.5em;
}

.description {
  margin: 0;
  color: var(--color-text);
  font-size: 0.9rem;
}

.child-tasks {
  display: flex;
  flex-direction: column;
  gap: 0.5em;
}

.empty {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.row-body-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8em;
  flex-wrap: wrap;
}

.detail-link {
  color: var(--color-accent);
  font-size: 0.9rem;
  text-decoration: none;
}

.detail-link:hover {
  text-decoration: underline;
}

.detail-link:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}
</style>
