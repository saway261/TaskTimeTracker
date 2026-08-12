<script setup lang="ts">
import { ref } from 'vue'
import { useWorkSessionStore } from '@/stores/workSessionStore'
import { useTaskStore } from '@/stores/taskStore'
import { useNotificationStore } from '@/stores/notificationStore'
import type { WorkSession, WorkSessionUpdateRequest } from '@/types/workSession'
import type { ApiError } from '@/types/apiError'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import WorkSessionListItem from './WorkSessionListItem.vue'
import WorkSessionForm from './WorkSessionForm.vue'

const props = defineProps<{
  taskId: number
  // 完了済みタスクではセッション操作UIを表示しない（読み取り専用の一覧のみ、Q5-A）。
  taskFinished: boolean
}>()

const workSessionStore = useWorkSessionStore()
const taskStore = useTaskStore()
const notification = useNotificationStore()

const editingSession = ref<WorkSession | null>(null)
const showEditModal = ref(false)
const updating = ref(false)
const updateError = ref<ApiError | null>(null)

function openEdit(session: WorkSession) {
  editingSession.value = session
  updateError.value = null
  showEditModal.value = true
}

// セッションの追加・更新・削除・終了の後に必要な再取得（§5.3）。
async function refreshAfterMutation() {
  await Promise.all([
    workSessionStore.fetchTotalMinutes(props.taskId),
    taskStore.fetchTask(props.taskId),
  ])
}

async function handleUpdate(req: WorkSessionUpdateRequest) {
  if (!editingSession.value) return
  updating.value = true
  updateError.value = null
  try {
    await workSessionStore.updateSession(editingSession.value.id, req)
    await refreshAfterMutation()
    notification.success('作業セッションを更新しました。')
    showEditModal.value = false
  } catch (e) {
    updateError.value = e as ApiError
  } finally {
    updating.value = false
  }
}

async function handleDelete(session: WorkSession) {
  if (!window.confirm('この作業セッションを削除しますか？')) return
  try {
    await workSessionStore.removeSession(session.id)
    await refreshAfterMutation()
    notification.success('作業セッションを削除しました。')
  } catch (e) {
    notification.error((e as ApiError).message)
  }
}
</script>

<template>
  <div class="work-session-list">
    <LoadingIndicator v-if="workSessionStore.loading" />
    <ErrorMessage v-else-if="workSessionStore.error" :error="workSessionStore.error" />
    <p v-else-if="workSessionStore.workSessions.length === 0" class="empty">
      作業セッションがまだありません。
    </p>
    <div v-else class="sessions">
      <WorkSessionListItem
        v-for="session in workSessionStore.workSessions"
        :key="session.id"
        :session="session"
        :editable="!taskFinished"
        @edit="openEdit"
        @delete="handleDelete"
      />
    </div>

    <BaseModal v-model="showEditModal" title="作業セッションを編集">
      <WorkSessionForm
        v-if="editingSession"
        :session="editingSession"
        :submitting="updating"
        :error="updateError"
        @submit="handleUpdate"
        @cancel="showEditModal = false"
      />
    </BaseModal>
  </div>
</template>

<style scoped>
.work-session-list {
  display: flex;
  flex-direction: column;
  gap: 0.6em;
}

.sessions {
  display: flex;
  flex-direction: column;
  gap: 0.5em;
}

.empty {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}
</style>
