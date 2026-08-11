<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useProjectStore } from '@/stores/projectStore'
import { useTaskGroupStore } from '@/stores/taskGroupStore'
import { useTaskStore } from '@/stores/taskStore'
import { useNotificationStore } from '@/stores/notificationStore'
import { toPositiveInt } from '@/utils/routeParams'
import type { ApiError } from '@/types/apiError'
import type { ProjectUpdateRequest } from '@/types/project'
import type { TaskGroupCreateRequest } from '@/types/taskGroup'
import type { TaskCreateRequest } from '@/types/task'
import type { MemoRequest } from '@/types/memo'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import AppBreadcrumb from '@/components/common/AppBreadcrumb.vue'
import ProjectForm from '@/components/project/ProjectForm.vue'
import TaskGroupListItem from '@/components/taskGroup/TaskGroupListItem.vue'
import TaskGroupForm from '@/components/taskGroup/TaskGroupForm.vue'
import TaskListItem from '@/components/task/TaskListItem.vue'
import TaskForm from '@/components/task/TaskForm.vue'
import MemoList from '@/components/memo/MemoList.vue'

const props = defineProps<{
  projectId: string
}>()

const projectStore = useProjectStore()
const taskGroupStore = useTaskGroupStore()
const taskStore = useTaskStore()
const notification = useNotificationStore()

const invalidId = ref(false)
const showEditModal = ref(false)
const updating = ref(false)
const updateError = ref<ApiError | null>(null)

const numericId = computed(() => toPositiveInt(props.projectId))

const breadcrumbItems = computed(() => {
  const project = projectStore.currentProject
  if (!project) return []
  return [{ label: 'プロジェクト一覧', to: '/projects' }, { label: project.title }]
})

const directTasks = computed(() => taskStore.tasks.filter((t) => t.projectId !== null))

async function load() {
  const id = numericId.value
  if (id === null) {
    invalidId.value = true
    return
  }
  invalidId.value = false
  try {
    await projectStore.fetchProject(id)
    await Promise.all([taskGroupStore.fetchTaskGroups(id), taskStore.fetchTasksInProject(id)])
  } catch {
    // エラーは各storeのerrorに保持済み
  }
}

onMounted(load)
watch(() => props.projectId, load)

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
    await projectStore.updateProject(id, payload as ProjectUpdateRequest)
    notification.success('プロジェクトを更新しました。')
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
    return Promise.reject(new Error('invalid project id'))
  }
  return projectStore.createProjectMemo(id, req)
}

const showCreateTaskGroupModal = ref(false)
const creatingTaskGroup = ref(false)
const createTaskGroupError = ref<ApiError | null>(null)

function openCreateTaskGroupModal() {
  createTaskGroupError.value = null
  showCreateTaskGroupModal.value = true
}

async function handleCreateTaskGroup(payload: { title: string; description: string | null }) {
  const id = numericId.value
  if (id === null) return
  creatingTaskGroup.value = true
  createTaskGroupError.value = null
  try {
    await taskGroupStore.createTaskGroup(id, payload as TaskGroupCreateRequest)
    notification.success('タスクグループを登録しました。')
    showCreateTaskGroupModal.value = false
  } catch (e) {
    createTaskGroupError.value = e as ApiError
  } finally {
    creatingTaskGroup.value = false
  }
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
    await taskStore.createTaskInProject(id, payload as TaskCreateRequest)
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
  <div class="project-detail-view">
    <AppBreadcrumb v-if="breadcrumbItems.length > 0" :items="breadcrumbItems" />

    <p v-if="invalidId">不正なプロジェクトIDです。</p>
    <LoadingIndicator v-else-if="projectStore.loading" />
    <ErrorMessage v-else-if="projectStore.error" :error="projectStore.error" />
    <template v-else-if="projectStore.currentProject">
      <div class="header">
        <div>
          <h1>{{ projectStore.currentProject.title }}</h1>
          <span class="status" :class="{ finished: projectStore.currentProject.isFinished }">
            {{ projectStore.currentProject.isFinished ? '完了' : '未完了' }}
          </span>
        </div>
        <BaseButton variant="secondary" @click="openEditModal">編集</BaseButton>
      </div>

      <p v-if="projectStore.currentProject.description">
        {{ projectStore.currentProject.description }}
      </p>

      <MemoList
        :memos="projectStore.currentProject.memos"
        :on-create="handleMemoCreate"
        @updated="projectStore.syncMemoUpdated"
        @deleted="projectStore.syncMemoRemoved"
      />

      <section class="task-list-section">
        <div class="section-header">
          <h2>タスクグループ・タスク</h2>
          <div class="section-header-actions">
            <BaseButton variant="secondary" @click="openCreateTaskGroupModal">
              ＋ 新規タスクグループ
            </BaseButton>
            <BaseButton variant="secondary" @click="openCreateTaskModal">
              ＋ 新規タスク
            </BaseButton>
          </div>
        </div>

        <LoadingIndicator v-if="taskGroupStore.loading || taskStore.loading" />
        <ErrorMessage v-else-if="taskGroupStore.error" :error="taskGroupStore.error" />
        <ErrorMessage v-else-if="taskStore.error" :error="taskStore.error" />
        <p
          v-else-if="taskGroupStore.taskGroups.length === 0 && directTasks.length === 0"
          class="empty"
        >
          タスクグループ・タスクがまだありません。
        </p>
        <div v-else class="entity-rows">
          <TaskGroupListItem
            v-for="taskGroup in taskGroupStore.taskGroups"
            :key="`tg-${taskGroup.id}`"
            :task-group="taskGroup"
          />
          <TaskListItem
            v-for="task in directTasks"
            :key="`t-${task.id}`"
            :task="task"
            :to="`/projects/${numericId}/tasks/${task.id}`"
          />
        </div>
      </section>
    </template>

    <BaseModal
      v-if="projectStore.currentProject"
      v-model="showEditModal"
      title="プロジェクトを編集"
    >
      <ProjectForm
        :project="projectStore.currentProject"
        :submitting="updating"
        :error="updateError"
        @submit="handleUpdate"
        @cancel="showEditModal = false"
      />
    </BaseModal>

    <BaseModal v-model="showCreateTaskGroupModal" title="新規タスクグループ">
      <TaskGroupForm
        :submitting="creatingTaskGroup"
        :error="createTaskGroupError"
        @submit="handleCreateTaskGroup"
        @cancel="showCreateTaskGroupModal = false"
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
.project-detail-view {
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

.section-header-actions {
  display: flex;
  gap: 0.6em;
  flex-wrap: wrap;
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
