<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useProjectStore } from '@/stores/projectStore'
import { useTaskGroupStore } from '@/stores/taskGroupStore'
import { useNotificationStore } from '@/stores/notificationStore'
import { toPositiveInt } from '@/utils/routeParams'
import type { ApiError } from '@/types/apiError'
import type { ProjectUpdateRequest } from '@/types/project'
import type { TaskGroupCreateRequest } from '@/types/taskGroup'
import type { MemoRequest } from '@/types/memo'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import AppBreadcrumb from '@/components/common/AppBreadcrumb.vue'
import ProjectForm from '@/components/project/ProjectForm.vue'
import TaskGroupListItem from '@/components/taskGroup/TaskGroupListItem.vue'
import TaskGroupForm from '@/components/taskGroup/TaskGroupForm.vue'
import MemoList from '@/components/memo/MemoList.vue'

const props = defineProps<{
  projectId: string
}>()

const projectStore = useProjectStore()
const taskGroupStore = useTaskGroupStore()
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

async function load() {
  const id = numericId.value
  if (id === null) {
    invalidId.value = true
    return
  }
  invalidId.value = false
  try {
    await projectStore.fetchProject(id)
    await taskGroupStore.fetchTaskGroups(id)
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

      <MemoList
        :memos="projectStore.currentProject.memos"
        :on-create="handleMemoCreate"
        @updated="projectStore.syncMemoUpdated"
        @deleted="projectStore.syncMemoRemoved"
      />

      <p v-if="projectStore.currentProject.description">
        {{ projectStore.currentProject.description }}
      </p>

      <section class="task-list-section">
        <div class="section-header">
          <h2>タスクグループ・タスク</h2>
          <BaseButton variant="secondary" @click="openCreateTaskGroupModal">
            ＋ 新規タスクグループ
          </BaseButton>
        </div>

        <LoadingIndicator v-if="taskGroupStore.loading" />
        <ErrorMessage v-else-if="taskGroupStore.error" :error="taskGroupStore.error" />
        <p v-else-if="taskGroupStore.taskGroups.length === 0" class="empty">
          タスクグループがまだありません。
        </p>
        <div v-else class="entity-rows">
          <TaskGroupListItem
            v-for="taskGroup in taskGroupStore.taskGroups"
            :key="taskGroup.id"
            :task-group="taskGroup"
          />
        </div>

        <div class="entity-row-placeholder">
          プロジェクト直下タスク（どのタスクグループにも属さないタスク）はここに表示される。フェーズ4で追加する。
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

.entity-row-placeholder {
  padding: 0.9em 1em;
  border-radius: 8px;
  border: 1px dashed var(--color-surface-muted);
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.empty {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}
</style>
