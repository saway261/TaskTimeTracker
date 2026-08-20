<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useTaskStore } from '@/stores/taskStore'
import { useWorkSessionStore } from '@/stores/workSessionStore'
import { useNotificationStore } from '@/stores/notificationStore'
import { isFinished as isTaskFinished } from '@/utils/task'
import { toReflectionTask } from '@/utils/reflectionTask'
import * as reflectionsApi from '@/api/reflectionsApi'
import type { ApiError } from '@/types/apiError'
import type { MemoRequest } from '@/types/memo'
import type { ReflectionRequest } from '@/types/reflection'
import BaseModal from '@/components/common/BaseModal.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import FinishedCheckbox from '@/components/common/FinishedCheckbox.vue'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import MemoList from '@/components/memo/MemoList.vue'
import EstimationSummary from '@/components/task/EstimationSummary.vue'
import ManualWorkSessionForm from '@/components/workSession/ManualWorkSessionForm.vue'
import WorkSessionList from '@/components/workSession/WorkSessionList.vue'
import WorkTimer from '@/components/workSession/WorkTimer.vue'
import ReflectionModal from '@/components/reflection/ReflectionModal.vue'

const props = defineProps<{
  modelValue: boolean
  taskId: number
  taskTitle: string
  detailTo: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const taskStore = useTaskStore()
const workSessionStore = useWorkSessionStore()
const notification = useNotificationStore()

const loading = ref(false)
const loadError = ref<ApiError | null>(null)

const task = computed(() =>
  taskStore.currentTask?.id === props.taskId ? taskStore.currentTask : null,
)
const finished = computed(() => (task.value ? isTaskFinished(task.value) : false))
const hasActiveTimer = computed(() => workSessionStore.activeSession !== null)
const pastSessionCount = computed(
  () =>
    workSessionStore.workSessions.filter(
      (session) => session.type !== 'TIMER' || session.endedAt !== null,
    ).length,
)

async function load() {
  loading.value = true
  loadError.value = null
  try {
    const [loadedTask] = await Promise.all([
      taskStore.fetchTaskForInteraction(props.taskId),
      workSessionStore.fetchSessions(props.taskId),
    ])
    if (!isTaskFinished(loadedTask)) {
      await workSessionStore.fetchTotalMinutes(props.taskId)
    }
  } catch (e) {
    loadError.value = e as ApiError
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) load()
  },
  { immediate: true },
)

const creatingSession = ref(false)
const createSessionError = ref<ApiError | null>(null)

async function handleCreateManualSession(minutes: number) {
  creatingSession.value = true
  createSessionError.value = null
  try {
    await workSessionStore.createManualSession(props.taskId, minutes)
    await Promise.all([
      workSessionStore.fetchTotalMinutes(props.taskId),
      taskStore.fetchTaskForInteraction(props.taskId),
    ])
    notification.success('作業セッションを記録しました。')
  } catch (e) {
    createSessionError.value = e as ApiError
  } finally {
    creatingSession.value = false
  }
}

function handleMemoCreate(req: MemoRequest) {
  return taskStore.createTaskMemo(props.taskId, req)
}

const finishing = ref(false)
const finishError = ref<ApiError | null>(null)
const showReopenConfirm = ref(false)

async function updateFinishedState(nextFinished: boolean) {
  finishing.value = true
  finishError.value = null
  try {
    await taskStore.updateFinished(props.taskId, { isFinished: nextFinished })
    notification.success(nextFinished ? 'タスクを完了にしました。' : '完了を解除しました。')
    if (nextFinished) {
      reflectionError.value = null
      showReflectionModal.value = true
    }
  } catch (e) {
    finishError.value = e as ApiError
  } finally {
    finishing.value = false
  }
}

// 作業中へ戻す方向のときだけ、振り返りが破棄されることの確認を挟む（完了にする方向は確認なし）。
function handleFinishedToggle(nextFinished: boolean) {
  if (nextFinished) {
    updateFinishedState(true)
  } else {
    showReopenConfirm.value = true
  }
}

function close() {
  emit('update:modelValue', false)
}

// --- クイック振り返り（完了直後に即入力できるようにする） ---
const showReflectionModal = ref(false)
const reflectionSubmitting = ref(false)
const reflectionError = ref<ApiError | null>(null)

const reflectionTask = computed(() => (task.value ? toReflectionTask(task.value) : null))

// 振り返りモーダルを閉じる操作（✖・背景クリック・登録完了）は、そのままタスクモーダルも閉じて
// 元のタスク一覧ページへ戻す（完了操作をタスクモーダルで行った場合の仕様）。
function handleReflectionModalUpdate(open: boolean) {
  showReflectionModal.value = open
  if (!open) {
    close()
  }
}

async function handleReflectionSubmit(payload: ReflectionRequest) {
  reflectionSubmitting.value = true
  reflectionError.value = null
  try {
    await reflectionsApi.create(props.taskId, payload)
    notification.success('振り返りを登録しました。')
    handleReflectionModalUpdate(false)
  } catch (e) {
    reflectionError.value = e as ApiError
  } finally {
    reflectionSubmitting.value = false
  }
}
</script>

<template>
  <BaseModal
    :model-value="modelValue"
    :title="taskTitle"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="task-quick-actions">
      <LoadingIndicator v-if="loading" />
      <ErrorMessage v-else-if="loadError" :error="loadError" />

      <template v-else-if="task">
        <div class="task-state">
          <FinishedCheckbox
            :model-value="finished"
            :disabled="finishing || (!finished && hasActiveTimer)"
            @update:model-value="handleFinishedToggle"
          />
          <p v-if="!finished && hasActiveTimer" class="section-hint">
            タイマーを停止してから完了にしてください。
          </p>
          <ErrorMessage v-if="finishError" :error="finishError" />
        </div>

        <section class="quick-section metrics-section" aria-labelledby="metrics-title">
          <h3 id="metrics-title">見積・誤差</h3>
          <EstimationSummary :task="task" :actual-minutes="workSessionStore.totalMinutes" compact />
        </section>

        <section v-if="!finished" class="quick-section timer-section" aria-labelledby="timer-title">
          <h3 id="timer-title">タイマー</h3>
          <WorkTimer :task-id="taskId" />
        </section>

        <details class="session-history">
          <summary>過去の作業記録（{{ pastSessionCount }}件）</summary>
          <div class="history-content">
            <WorkSessionList :task-id="taskId" :task-finished="finished" exclude-active />
          </div>
        </details>

        <section v-if="!finished" class="quick-section" aria-labelledby="manual-record-title">
          <h3 id="manual-record-title">手動で記録を追加</h3>
          <ManualWorkSessionForm
            :submitting="creatingSession"
            :error="createSessionError"
            :disabled="hasActiveTimer"
            @submit="handleCreateManualSession"
          />
        </section>

        <section class="quick-section" aria-labelledby="memo-title">
          <h3 id="memo-title">メモ</h3>
          <p class="section-hint">次にやることや作業中の気づきを残せます。</p>
          <MemoList
            :memos="task.memos"
            :on-create="handleMemoCreate"
            @updated="taskStore.syncMemoUpdated"
            @deleted="taskStore.syncMemoRemoved"
          />
        </section>

        <footer class="modal-footer">
          <RouterLink :to="detailTo" class="detail-link" @click="close">
            詳細画面で確認・編集する →
          </RouterLink>
        </footer>
      </template>
    </div>

    <ConfirmDialog
      v-model="showReopenConfirm"
      title="タスクを作業中に戻す"
      message="このタスクを作業中に戻します。保存済みの振り返りがある場合は削除され、元に戻せません。"
      confirm-label="作業中に戻す"
      danger
      @confirm="updateFinishedState(false)"
    />

    <ReflectionModal
      :model-value="showReflectionModal"
      :task="reflectionTask"
      :submitting="reflectionSubmitting"
      :error="reflectionError"
      defer-hint
      @update:model-value="handleReflectionModalUpdate"
      @submit="handleReflectionSubmit"
    />
  </BaseModal>
</template>

<style scoped>
.task-quick-actions {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.task-state {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.5rem;
  margin-top: -0.35rem;
}

.quick-section {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.quick-section h3 {
  margin: 0;
  font-size: 1rem;
}

.timer-section h3 {
  text-align: center;
}

.section-hint {
  margin: -0.35rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.session-history {
  border: 1px solid var(--color-surface-muted);
  border-radius: 8px;
  background: var(--color-surface);
}

.session-history summary {
  padding: 0.75rem 0.9rem;
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
}

.session-history summary:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: -2px;
}

.history-content {
  padding: 0 0.9rem 0.9rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 1rem;
  border-top: 1px solid var(--color-surface-muted);
}

.detail-link {
  color: var(--color-accent);
  font-size: 0.9rem;
  font-weight: 600;
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
