<script setup lang="ts">
import { computed, ref } from 'vue'
import { useWorkSessionStore } from '@/stores/workSessionStore'
import { useTaskStore } from '@/stores/taskStore'
import { useNotificationStore } from '@/stores/notificationStore'
import type { WorkSession, WorkSessionUpdateRequest } from '@/types/workSession'
import type { ApiError } from '@/types/apiError'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import WorkSessionListItem from './WorkSessionListItem.vue'
import WorkSessionForm from './WorkSessionForm.vue'

const props = defineProps<{
  taskId: number
  // 完了済みタスクではセッション操作UIを表示しない（読み取り専用の一覧のみ、Q5-A）。
  taskFinished: boolean
  // 操作モーダルでは、稼働中タイマーを「過去の記録」から除外する。
  excludeActive?: boolean
}>()

const workSessionStore = useWorkSessionStore()
const taskStore = useTaskStore()
const notification = useNotificationStore()

const visibleSessions = computed(() =>
  props.excludeActive
    ? workSessionStore.workSessions.filter(
        (session) => session.type !== 'TIMER' || session.endedAt !== null,
      )
    : workSessionStore.workSessions,
)

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
    taskStore.fetchTaskForInteraction(props.taskId),
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

const pendingDeleteSession = ref<WorkSession | null>(null)
const showDeleteConfirm = ref(false)

function confirmDelete(session: WorkSession) {
  pendingDeleteSession.value = session
  showDeleteConfirm.value = true
}

async function handleDelete() {
  const session = pendingDeleteSession.value
  if (!session) return
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
    <p v-else-if="visibleSessions.length === 0" class="empty">作業セッションがまだありません。</p>
    <div v-else class="sessions">
      <WorkSessionListItem
        v-for="session in visibleSessions"
        :key="session.id"
        :session="session"
        :editable="!taskFinished"
        @edit="openEdit"
        @delete="confirmDelete"
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

    <ConfirmDialog
      v-model="showDeleteConfirm"
      title="作業セッションの削除"
      message="この作業セッションを削除しますか？"
      confirm-label="削除する"
      danger
      @confirm="handleDelete"
    />
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
