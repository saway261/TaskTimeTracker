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
import BaseButton from '@/components/common/BaseButton.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
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

async function finishTask() {
  finishing.value = true
  finishError.value = null
  try {
    await taskStore.updateFinished(props.taskId, { isFinished: true })
    notification.success('タスクを完了にしました。')
    reflectionError.value = null
    showReflectionModal.value = true
  } catch (e) {
    finishError.value = e as ApiError
  } finally {
    finishing.value = false
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
          <span class="status" :class="{ finished }">{{ finished ? '完了' : '未完了' }}</span>
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

        <ErrorMessage v-if="finishError" :error="finishError" />
        <div v-if="!finished" class="finish-action">
          <BaseButton :disabled="finishing || hasActiveTimer" @click="finishTask">
            完了にする
          </BaseButton>
          <p v-if="hasActiveTimer" class="section-hint">
            タイマーを停止してから完了にしてください。
          </p>
        </div>

        <footer class="modal-footer">
          <RouterLink :to="detailTo" class="detail-link" @click="close">
            詳細画面で確認・編集する →
          </RouterLink>
        </footer>
      </template>
    </div>

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
  justify-content: flex-end;
  margin-top: -0.35rem;
}

.status {
  padding: 0.2rem 0.65rem;
  border-radius: 999px;
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
  font-size: 0.8rem;
  font-weight: 600;
}

.status.finished {
  color: var(--color-success);
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

.finish-action {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.6rem;
  padding-top: 0.25rem;
}

.finish-action .section-hint {
  margin: 0;
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
