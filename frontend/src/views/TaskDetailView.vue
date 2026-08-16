<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useTaskStore } from '@/stores/taskStore'
import { useProjectStore } from '@/stores/projectStore'
import { useTaskGroupStore } from '@/stores/taskGroupStore'
import { useWorkSessionStore } from '@/stores/workSessionStore'
import { useNotificationStore } from '@/stores/notificationStore'
import { toPositiveInt } from '@/utils/routeParams'
import { isFinished as isTaskFinished } from '@/utils/task'
import type { ApiError } from '@/types/apiError'
import type { TaskUpdateEstimatedMinutesRequest, TaskUpdatePropertyRequest } from '@/types/task'
import type { MemoRequest } from '@/types/memo'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseInput from '@/components/common/BaseInput.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import AppBreadcrumb from '@/components/common/AppBreadcrumb.vue'
import TaskForm from '@/components/task/TaskForm.vue'
import EstimationSummary from '@/components/task/EstimationSummary.vue'
import MemoList from '@/components/memo/MemoList.vue'
import WorkSessionList from '@/components/workSession/WorkSessionList.vue'
import ManualWorkSessionForm from '@/components/workSession/ManualWorkSessionForm.vue'
import WorkTimer from '@/components/workSession/WorkTimer.vue'

const props = defineProps<{
  projectId: string
  taskId: string
  taskGroupId: string | null
}>()

const router = useRouter()
const taskStore = useTaskStore()
const projectStore = useProjectStore()
const taskGroupStore = useTaskGroupStore()
const workSessionStore = useWorkSessionStore()
const notification = useNotificationStore()

const invalidId = ref(false)

const numericTaskId = computed(() => toPositiveInt(props.taskId))
const numericProjectId = computed(() => toPositiveInt(props.projectId))
const numericTaskGroupId = computed(() =>
  props.taskGroupId === null ? null : toPositiveInt(props.taskGroupId),
)

const backLink = computed(() =>
  numericTaskGroupId.value !== null
    ? `/projects/${props.projectId}/task-groups/${props.taskGroupId}`
    : `/projects/${props.projectId}`,
)

const finished = computed(() =>
  taskStore.currentTask ? isTaskFinished(taskStore.currentTask) : false,
)

// タイマー稼働中は完了・削除・手動セッション登録・新規タイマー開始を無効化する（§6.5）。
const hasActiveTimer = computed(() => workSessionStore.activeSession !== null)

const breadcrumbItems = computed(() => {
  const task = taskStore.currentTask
  if (!task) return []
  const projectLabel =
    projectStore.currentProject?.id === numericProjectId.value
      ? projectStore.currentProject.title
      : `プロジェクト #${props.projectId}`
  const items: { label: string; to?: string }[] = [
    { label: 'プロジェクト一覧', to: '/projects' },
    { label: projectLabel, to: `/projects/${props.projectId}` },
  ]
  if (numericTaskGroupId.value !== null) {
    const taskGroupLabel =
      taskGroupStore.currentTaskGroup?.id === numericTaskGroupId.value
        ? taskGroupStore.currentTaskGroup.title
        : `タスクグループ #${props.taskGroupId}`
    items.push({ label: taskGroupLabel, to: backLink.value })
  }
  items.push({ label: task.title })
  return items
})

async function load() {
  const id = numericTaskId.value
  if (id === null) {
    invalidId.value = true
    return
  }
  invalidId.value = false
  await taskStore.fetchTask(id).catch(() => {})

  // パンくず表示用のベストエフォート取得。失敗しても "#id" 表示にフォールバックする。
  if (numericProjectId.value !== null) {
    projectStore.fetchProject(numericProjectId.value).catch(() => {})
  }
  if (numericTaskGroupId.value !== null) {
    taskGroupStore.fetchTaskGroup(numericTaskGroupId.value).catch(() => {})
  }

  // 一覧は完了済みタスクでも読み取り専用で表示する（Q5-A）。実績（total-minutes）は
  // 未完了タスクのみ必要（完了済みは actualMinutesCached を使う。§4.5）。
  await workSessionStore.fetchSessions(id).catch(() => {})
  if (taskStore.currentTask && !isTaskFinished(taskStore.currentTask)) {
    workSessionStore.fetchTotalMinutes(id).catch(() => {})
  }
}

onMounted(load)
watch(() => props.taskId, load)

// --- タイトル・説明の編集（完了済みタスクでも許可。Q5-A） ---
const showEditModal = ref(false)
const updating = ref(false)
const updateError = ref<ApiError | null>(null)

function openEditModal() {
  updateError.value = null
  showEditModal.value = true
}

async function handleUpdate(payload: { title: string; description: string | null }) {
  const id = numericTaskId.value
  if (id === null) return
  updating.value = true
  updateError.value = null
  try {
    await taskStore.updateTaskProperty(id, payload as TaskUpdatePropertyRequest)
    notification.success('タスクを更新しました。')
    showEditModal.value = false
  } catch (e) {
    updateError.value = e as ApiError
  } finally {
    updating.value = false
  }
}

// --- 見積時間の編集（B5対策：完了済みでは出さない。セッションが1件でもあれば無効化） ---
const editingEstimate = ref(false)
const estimateInput = ref('')
const estimateUpdating = ref(false)
const estimateError = ref<ApiError | null>(null)

const canEditEstimate = computed(() => workSessionStore.workSessions.length === 0)

function openEstimateEditor() {
  estimateInput.value = taskStore.currentTask?.estimatedMinutes?.toString() ?? ''
  estimateError.value = null
  editingEstimate.value = true
}

function cancelEstimateEditor() {
  editingEstimate.value = false
}

async function submitEstimate() {
  const id = numericTaskId.value
  const trimmed = estimateInput.value.trim()
  const n = Number(trimmed)
  if (id === null || trimmed === '' || !Number.isInteger(n) || n <= 0) return
  estimateUpdating.value = true
  estimateError.value = null
  try {
    const req: TaskUpdateEstimatedMinutesRequest = { estimatedMinutes: n }
    await taskStore.updateEstimatedMinutes(id, req)
    notification.success('見積時間を更新しました。')
    editingEstimate.value = false
  } catch (e) {
    estimateError.value = e as ApiError
  } finally {
    estimateUpdating.value = false
  }
}

// --- 完了・完了解除・削除 ---
const finishing = ref(false)
const actionError = ref<ApiError | null>(null)

async function toggleFinished() {
  const id = numericTaskId.value
  if (id === null) return
  finishing.value = true
  actionError.value = null
  const wasFinished = finished.value
  try {
    await taskStore.updateFinished(id, { isFinished: !wasFinished })
    notification.success(wasFinished ? '完了を解除しました。' : 'タスクを完了にしました。')
  } catch (e) {
    actionError.value = e as ApiError
  } finally {
    finishing.value = false
  }
}

// 作業中へ戻す方向のときだけ、振り返りが破棄されることの確認を挟む（完了にする方向は従来どおり確認なし）。
const showReopenConfirm = ref(false)

function handleToggleFinishedClick() {
  if (finished.value) {
    showReopenConfirm.value = true
  } else {
    toggleFinished()
  }
}

const showDeleteConfirm = ref(false)

async function handleDelete() {
  const id = numericTaskId.value
  if (id === null) return
  actionError.value = null
  try {
    await taskStore.removeTask(id)
    notification.success('タスクを削除しました。')
    router.push(backLink.value)
  } catch (e) {
    actionError.value = e as ApiError
  }
}

// --- 作業セッション（手動登録） ---
const creatingSession = ref(false)
const createSessionError = ref<ApiError | null>(null)

async function handleCreateManualSession(minutes: number) {
  const id = numericTaskId.value
  if (id === null) return
  creatingSession.value = true
  createSessionError.value = null
  try {
    await workSessionStore.createManualSession(id, minutes)
    await Promise.all([workSessionStore.fetchTotalMinutes(id), taskStore.fetchTask(id)])
    notification.success('作業セッションを記録しました。')
  } catch (e) {
    createSessionError.value = e as ApiError
  } finally {
    creatingSession.value = false
  }
}

// --- メモ ---
function handleMemoCreate(req: MemoRequest) {
  const id = numericTaskId.value
  if (id === null) {
    return Promise.reject(new Error('invalid task id'))
  }
  return taskStore.createTaskMemo(id, req)
}
</script>

<template>
  <div class="task-detail-view">
    <AppBreadcrumb v-if="breadcrumbItems.length > 0" :items="breadcrumbItems" />

    <p v-if="invalidId">不正なタスクIDです。</p>
    <LoadingIndicator v-else-if="taskStore.loading" />
    <ErrorMessage v-else-if="taskStore.error" :error="taskStore.error" />
    <template v-else-if="taskStore.currentTask">
      <div class="header">
        <div>
          <h1>{{ taskStore.currentTask.title }}</h1>
          <span class="status" :class="{ finished }">{{ finished ? '完了' : '未完了' }}</span>
        </div>
        <div class="header-actions">
          <BaseButton variant="secondary" @click="openEditModal">編集</BaseButton>
          <BaseButton
            variant="secondary"
            :disabled="finishing || (!finished && hasActiveTimer)"
            @click="handleToggleFinishedClick"
          >
            {{ finished ? '完了を解除する' : '完了にする' }}
          </BaseButton>
          <BaseButton variant="danger" :disabled="hasActiveTimer" @click="showDeleteConfirm = true">
            削除
          </BaseButton>
        </div>
      </div>

      <ErrorMessage v-if="actionError" :error="actionError" />

      <p v-if="taskStore.currentTask.description">{{ taskStore.currentTask.description }}</p>

      <MemoList
        :memos="taskStore.currentTask.memos"
        :on-create="handleMemoCreate"
        @updated="taskStore.syncMemoUpdated"
        @deleted="taskStore.syncMemoRemoved"
      />

      <section class="estimation-section">
        <h2>見積・実績</h2>
        <EstimationSummary
          :task="taskStore.currentTask"
          :actual-minutes="workSessionStore.totalMinutes"
        />

        <template v-if="!finished">
          <form v-if="editingEstimate" class="estimate-form" @submit.prevent="submitEstimate">
            <ErrorMessage v-if="estimateError" :error="estimateError" />
            <BaseInput
              v-model="estimateInput"
              label="見積時間（分）"
              type="number"
              required
              :error="estimateError?.fieldErrors.estimatedMinutes"
            />
            <div class="actions">
              <BaseButton type="button" variant="secondary" @click="cancelEstimateEditor">
                キャンセル
              </BaseButton>
              <BaseButton type="submit" :disabled="estimateUpdating">更新する</BaseButton>
            </div>
          </form>
          <template v-else>
            <BaseButton
              variant="secondary"
              :disabled="!canEditEstimate"
              @click="openEstimateEditor"
            >
              見積を編集
            </BaseButton>
            <p v-if="!canEditEstimate" class="hint">
              作業記録があるため、見積時間を変更できません。
            </p>
          </template>
        </template>
      </section>

      <section class="work-section">
        <h2>作業セッション</h2>

        <WorkTimer v-if="!finished && numericTaskId !== null" :task-id="numericTaskId" />

        <WorkSessionList
          v-if="numericTaskId !== null"
          :task-id="numericTaskId"
          :task-finished="finished"
        />

        <template v-if="!finished">
          <h3>手動で記録を追加</h3>
          <ManualWorkSessionForm
            :submitting="creatingSession"
            :error="createSessionError"
            :disabled="hasActiveTimer"
            @submit="handleCreateManualSession"
          />
        </template>
      </section>
    </template>

    <BaseModal v-if="taskStore.currentTask" v-model="showEditModal" title="タスクを編集">
      <TaskForm
        :task="taskStore.currentTask"
        :submitting="updating"
        :error="updateError"
        @submit="handleUpdate"
        @cancel="showEditModal = false"
      />
    </BaseModal>

    <ConfirmDialog
      v-model="showDeleteConfirm"
      title="タスクの削除"
      message="このタスクを削除しますか？作業セッションも全て削除されます。"
      confirm-label="削除する"
      danger
      @confirm="handleDelete"
    />

    <ConfirmDialog
      v-model="showReopenConfirm"
      title="タスクを作業中に戻す"
      message="このタスクを作業中に戻します。保存済みの振り返りがある場合は削除され、元に戻せません。"
      confirm-label="作業中に戻す"
      danger
      @confirm="toggleFinished"
    />
  </div>
</template>

<style scoped>
.task-detail-view {
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
  flex-wrap: wrap;
}

.header h1 {
  margin: 0 0 0.3em;
}

.header-actions {
  display: flex;
  gap: 0.6em;
  flex-wrap: wrap;
}

.status {
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.status.finished {
  color: var(--color-success);
}

.estimation-section,
.work-section {
  display: flex;
  flex-direction: column;
  gap: 0.8em;
}

.estimation-section h2,
.work-section h2 {
  margin: 0;
  font-size: 1.05rem;
}

.estimate-form {
  display: flex;
  flex-direction: column;
  gap: 1em;
  max-width: 20em;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.6em;
}

.hint {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}
</style>
