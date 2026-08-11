<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useTaskGroupStore } from '@/stores/taskGroupStore'
import { useProjectStore } from '@/stores/projectStore'
import { useTaskStore } from '@/stores/taskStore'
import { useNotificationStore } from '@/stores/notificationStore'
import { toPositiveInt } from '@/utils/routeParams'
import type { ApiError } from '@/types/apiError'
import type { TaskGroupUpdateRequest } from '@/types/taskGroup'
import type { TaskCreateRequest } from '@/types/task'
import type { MemoRequest } from '@/types/memo'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import AppBreadcrumb from '@/components/common/AppBreadcrumb.vue'
import TaskGroupForm from '@/components/taskGroup/TaskGroupForm.vue'
import TaskListItem from '@/components/task/TaskListItem.vue'
import TaskForm from '@/components/task/TaskForm.vue'
import MemoList from '@/components/memo/MemoList.vue'

const props = defineProps<{
  projectId: string
  taskGroupId: string
}>()

const taskGroupStore = useTaskGroupStore()
const projectStore = useProjectStore()
const taskStore = useTaskStore()
const notification = useNotificationStore()

const invalidId = ref(false)
const showEditModal = ref(false)
const updating = ref(false)
const updateError = ref<ApiError | null>(null)

const numericId = computed(() => toPositiveInt(props.taskGroupId))

const breadcrumbItems = computed(() => {
  const taskGroup = taskGroupStore.currentTaskGroup
  if (!taskGroup) return []
  const projectLabel =
    projectStore.currentProject?.id === taskGroup.projectId
      ? projectStore.currentProject.title
      : `プロジェクト #${taskGroup.projectId}`
  return [
    { label: 'プロジェクト一覧', to: '/projects' },
    { label: projectLabel, to: `/projects/${taskGroup.projectId}` },
    { label: taskGroup.title },
  ]
})

async function load() {
  const id = numericId.value
  if (id === null) {
    invalidId.value = true
    return
  }
  invalidId.value = false
  await taskGroupStore.fetchTaskGroup(id).catch(() => {})

  const taskGroup = taskGroupStore.currentTaskGroup
  if (taskGroup) {
    // パンくずに実際のプロジェクト名を出すためのベストエフォート取得。失敗しても "#id" 表示にフォールバックする。
    projectStore.fetchProject(taskGroup.projectId).catch(() => {})
    taskStore.fetchTasksInTaskGroup(id).catch(() => {})
  }
}

onMounted(load)
watch(() => props.taskGroupId, load)

function openEditModal() {
  updateError.value = null
  showEditModal.value = true
}

async function handleUpdate(payload: {
  title: string
  description: string | null
  isFinished?: boolean
}) {
  const id = numericId.value
  if (id === null) return
  updating.value = true
  updateError.value = null
  try {
    await taskGroupStore.updateTaskGroup(id, payload as TaskGroupUpdateRequest)
    notification.success('タスクグループを更新しました。')
    showEditModal.value = false
  } catch (e) {
    updateError.value = e as ApiError
  } finally {
    updating.value = false
  }
}

function handleMemoCreate(req: MemoRequest) {
  const id = numericId.value
  if (id === null) {
    return Promise.reject(new Error('invalid task group id'))
  }
  return taskGroupStore.createTaskGroupMemo(id, req)
}

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
  const id = numericId.value
  if (id === null) return
  creatingTask.value = true
  createTaskError.value = null
  try {
    await taskStore.createTaskInTaskGroup(id, payload as TaskCreateRequest)
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
  <div class="task-group-detail-view">
    <AppBreadcrumb v-if="breadcrumbItems.length > 0" :items="breadcrumbItems" />

    <p v-if="invalidId">不正なタスクグループIDです。</p>
    <LoadingIndicator v-else-if="taskGroupStore.loading" />
    <ErrorMessage v-else-if="taskGroupStore.error" :error="taskGroupStore.error" />
    <template v-else-if="taskGroupStore.currentTaskGroup">
      <div class="header">
        <div>
          <h1>{{ taskGroupStore.currentTaskGroup.title }}</h1>
          <span class="status" :class="{ finished: taskGroupStore.currentTaskGroup.isFinished }">
            {{ taskGroupStore.currentTaskGroup.isFinished ? '完了' : '未完了' }}
          </span>
        </div>
        <BaseButton variant="secondary" @click="openEditModal">編集</BaseButton>
      </div>

      <p v-if="taskGroupStore.currentTaskGroup.description">
        {{ taskGroupStore.currentTaskGroup.description }}
      </p>

      <MemoList
        :memos="taskGroupStore.currentTaskGroup.memos"
        :on-create="handleMemoCreate"
        @updated="taskGroupStore.syncMemoUpdated"
        @deleted="taskGroupStore.syncMemoRemoved"
      />

      <section class="task-list-section">
        <div class="section-header">
          <h2>タスク</h2>
          <BaseButton variant="secondary" @click="openCreateTaskModal">＋ 新規タスク</BaseButton>
        </div>

        <LoadingIndicator v-if="taskStore.loading" />
        <ErrorMessage v-else-if="taskStore.error" :error="taskStore.error" />
        <p v-else-if="taskStore.tasks.length === 0" class="empty">タスクがまだありません。</p>
        <div v-else class="entity-rows">
          <TaskListItem
            v-for="task in taskStore.tasks"
            :key="task.id"
            :task="task"
            :to="`/projects/${taskGroupStore.currentTaskGroup.projectId}/task-groups/${numericId}/tasks/${task.id}`"
          />
        </div>
      </section>
    </template>

    <BaseModal
      v-if="taskGroupStore.currentTaskGroup"
      v-model="showEditModal"
      title="タスクグループを編集"
    >
      <TaskGroupForm
        :task-group="taskGroupStore.currentTaskGroup"
        :submitting="updating"
        :error="updateError"
        @submit="handleUpdate"
        @cancel="showEditModal = false"
      />
    </BaseModal>

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
.task-group-detail-view {
  padding: 1.2em;
  display: flex;
  flex-direction: column;
  gap: 1em;
}

.header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1em;
}

.header h1 {
  margin: 0 0 0.3em;
}

.status {
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.status.finished {
  color: var(--color-success);
}

.hint {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1em;
  flex-wrap: wrap;
}

.section-header h2 {
  margin: 0;
  font-size: 1.05rem;
}

.task-list-section {
  display: flex;
  flex-direction: column;
  gap: 0.8em;
}

.entity-rows {
  display: flex;
  flex-direction: column;
  gap: 0.6em;
}

.empty {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}
</style>
