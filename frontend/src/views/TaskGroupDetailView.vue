<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useTaskGroupStore } from '@/stores/taskGroupStore'
import { useProjectStore } from '@/stores/projectStore'
import { useNotificationStore } from '@/stores/notificationStore'
import { toPositiveInt } from '@/utils/routeParams'
import type { ApiError } from '@/types/apiError'
import type { TaskGroupUpdateRequest } from '@/types/taskGroup'
import type { MemoRequest } from '@/types/memo'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import AppBreadcrumb from '@/components/common/AppBreadcrumb.vue'
import TaskGroupForm from '@/components/taskGroup/TaskGroupForm.vue'
import MemoList from '@/components/memo/MemoList.vue'

const props = defineProps<{
  projectId: string
  taskGroupId: string
}>()

const taskGroupStore = useTaskGroupStore()
const projectStore = useProjectStore()
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

  // パンくずに実際のプロジェクト名を出すためのベストエフォート取得。失敗しても "#id" 表示にフォールバックする。
  const taskGroup = taskGroupStore.currentTaskGroup
  if (taskGroup) {
    projectStore.fetchProject(taskGroup.projectId).catch(() => {})
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

      <MemoList
        :memos="taskGroupStore.currentTaskGroup.memos"
        :on-create="handleMemoCreate"
        @updated="taskGroupStore.syncMemoUpdated"
        @deleted="taskGroupStore.syncMemoRemoved"
      />

      <p v-if="taskGroupStore.currentTaskGroup.description">
        {{ taskGroupStore.currentTaskGroup.description }}
      </p>

      <p class="hint">タスク一覧はフェーズ4で追加する。</p>
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
</style>
