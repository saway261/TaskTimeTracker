<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useProjectStore } from '@/stores/projectStore'
import { useTaskGroupStore } from '@/stores/taskGroupStore'
import { useTaskStore } from '@/stores/taskStore'
import {
  projectContainerKey,
  taskGroupContainerKey,
  useItemOrderStore,
} from '@/stores/itemOrderStore'
import { useNotificationStore } from '@/stores/notificationStore'
import { sortProjectItemsByOrder } from '@/utils/sort'
import { insertStubAt, swapVisibleItems } from '@/utils/dragReorder'
import { formatMinutes } from '@/utils/duration'
import { isFinished as isTaskFinished, sumEstimatedMinutes } from '@/utils/task'
import type { ApiError } from '@/types/apiError'
import type { ProjectUpdateRequest } from '@/types/project'
import type { TaskGroupCreateRequest, TaskGroupResponse } from '@/types/taskGroup'
import type { TaskCreateRequest, TaskResponse } from '@/types/task'
import type { ItemType, ProjectItemOrderItemRequest } from '@/types/itemOrder'
import type { MemoRequest } from '@/types/memo'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import AppBreadcrumb from '@/components/common/AppBreadcrumb.vue'
import FinishedCheckbox from '@/components/common/FinishedCheckbox.vue'
import CompletedItemsToggle from '@/components/common/CompletedItemsToggle.vue'
import ProjectForm from '@/components/project/ProjectForm.vue'
import TaskGroupListItem from '@/components/taskGroup/TaskGroupListItem.vue'
import TaskGroupForm from '@/components/taskGroup/TaskGroupForm.vue'
import TaskListItem from '@/components/task/TaskListItem.vue'
import TaskForm from '@/components/task/TaskForm.vue'
import MemoList from '@/components/memo/MemoList.vue'
import TutorialHelpButton from '@/components/tutorial/TutorialHelpButton.vue'
import { TUTORIAL_SCOPES } from '@/tutorial/scopes'

const props = defineProps<{
  projectId: string
}>()

const projectStore = useProjectStore()
const taskGroupStore = useTaskGroupStore()
const taskStore = useTaskStore()
const itemOrderStore = useItemOrderStore()
const notification = useNotificationStore()

const showEditModal = ref(false)
const updating = ref(false)
const updateError = ref<ApiError | null>(null)
const showCompletedItems = ref(false)

const currentProjectId = computed(() => projectStore.currentProject?.id ?? props.projectId)

const breadcrumbItems = computed(() => {
  const project = projectStore.currentProject
  if (!project) return []
  return [{ label: 'タスク管理', to: '/projects' }, { label: project.title }]
})

const directTasks = computed(() => taskStore.tasks.filter((t) => t.projectId !== null))
const projectEstimatedMinutes = computed(() => sumEstimatedMinutes(taskStore.tasks))

type OrderedItem =
  | { kind: 'TASK_GROUP'; id: string; taskGroup: TaskGroupResponse }
  | { kind: 'TASK'; id: string; task: TaskResponse }

const mergedRawItems = computed<OrderedItem[]>(() => [
  ...taskGroupStore.taskGroups.map((tg) => ({
    kind: 'TASK_GROUP' as const,
    id: tg.id,
    taskGroup: tg,
  })),
  ...directTasks.value.map((t) => ({ kind: 'TASK' as const, id: t.id, task: t })),
])

const orderedItems = computed(() =>
  sortProjectItemsByOrder(mergedRawItems.value, itemOrderStore.projectItemOrder),
)

const visibleOrderedItems = computed(() =>
  showCompletedItems.value
    ? orderedItems.value
    : orderedItems.value.filter((item) =>
        item.kind === 'TASK_GROUP' ? !item.taskGroup.isFinished : !isTaskFinished(item.task),
      ),
)

async function load() {
  const id = props.projectId
  try {
    await projectStore.fetchProject(id)
    await Promise.all([taskGroupStore.fetchTaskGroups(id), taskStore.fetchTasksInProject(id)])
  } catch {
    // エラーは各storeのerrorに保持済み
  }
  // 並び順は無ければid昇順にフォールバックして表示できるため、失敗してもページ全体は止めない。
  itemOrderStore.fetchProjectItemOrder(id).catch(() => {})
}

onMounted(load)
watch(() => props.projectId, load)

function openEditModal() {
  updateError.value = null
  showEditModal.value = true
}

async function handleUpdate(payload: { title: string; description: string | null }) {
  const id = props.projectId
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

// --- 完了状態（未完了のタスクが1件でもあれば完了にできない） ---
const finishedUpdating = ref(false)
const finishedError = ref<ApiError | null>(null)
const hasUnfinishedTasks = computed(() => taskStore.tasks.some((t) => !isTaskFinished(t)))

async function handleFinishedToggle(nextFinished: boolean) {
  const id = props.projectId
  finishedUpdating.value = true
  finishedError.value = null
  try {
    await projectStore.updateFinished(id, { isFinished: nextFinished })
    notification.success(nextFinished ? 'プロジェクトを完了にしました。' : '完了を解除しました。')
  } catch (e) {
    finishedError.value = e as ApiError
  } finally {
    finishedUpdating.value = false
  }
}

function handleMemoCreate(req: MemoRequest) {
  return projectStore.createProjectMemo(props.projectId, req)
}

const showCreateTaskGroupModal = ref(false)
const creatingTaskGroup = ref(false)
const createTaskGroupError = ref<ApiError | null>(null)

function openCreateTaskGroupModal() {
  createTaskGroupError.value = null
  showCreateTaskGroupModal.value = true
}

async function handleCreateTaskGroup(payload: { title: string; description: string | null }) {
  const id = props.projectId
  creatingTaskGroup.value = true
  createTaskGroupError.value = null
  try {
    const taskGroup = await taskGroupStore.createTaskGroup(id, payload as TaskGroupCreateRequest)
    itemOrderStore.appendToContainerOrder(projectContainerKey(id), 'TASK_GROUP', taskGroup.id)
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
  taskGroupId?: string | null
  tagIds?: string[]
}) {
  const id = props.projectId
  creatingTask.value = true
  createTaskError.value = null
  try {
    const { taskGroupId, ...taskRequest } = payload
    const targetTaskGroupId = taskGroupId ?? null
    const task =
      targetTaskGroupId === null
        ? await taskStore.createTaskInProject(id, taskRequest as TaskCreateRequest)
        : await taskStore.createTaskInTaskGroup(targetTaskGroupId, taskRequest as TaskCreateRequest)
    const targetContainer =
      targetTaskGroupId === null
        ? projectContainerKey(id)
        : taskGroupContainerKey(targetTaskGroupId)
    itemOrderStore.appendToContainerOrder(targetContainer, 'TASK', task.id)
    notification.success('タスクを登録しました。')
    showCreateTaskModal.value = false
  } catch (e) {
    createTaskError.value = e as ApiError
  } finally {
    creatingTask.value = false
  }
}

// --- 並べ替え・移動（§7.3・§7.4） ---

async function handleMoveUp(index: number) {
  if (index <= 0) return
  const items = swapVisibleItems(orderedItems.value, visibleOrderedItems.value, index, -1).map(
    (it) => ({ type: it.kind, id: it.id }),
  )
  try {
    await itemOrderStore.reorderProjectItems(currentProjectId.value, items)
  } catch (e) {
    notification.error((e as ApiError).message)
  }
}

async function handleMoveDown(index: number) {
  if (index >= visibleOrderedItems.value.length - 1) return
  const items = swapVisibleItems(orderedItems.value, visibleOrderedItems.value, index, 1).map(
    (it) => ({ type: it.kind, id: it.id }),
  )
  try {
    await itemOrderStore.reorderProjectItems(currentProjectId.value, items)
  } catch (e) {
    notification.error((e as ApiError).message)
  }
}

function handleEndDragOver() {
  if (!itemOrderStore.draggedItem) return
  itemOrderStore.setDragOverTarget({
    container: projectContainerKey(currentProjectId.value),
    beforeKey: null,
  })
}

async function handleItemDrop() {
  const containerKey = projectContainerKey(currentProjectId.value)
  const dragged = itemOrderStore.draggedItem
  const target = itemOrderStore.dragOverTarget
  if (!dragged || !target || target.container !== containerKey) return

  const draggedStub = { key: `${dragged.kind}:${dragged.id}` }
  const currentStubs = orderedItems.value.map((it) => ({ key: `${it.kind}:${it.id}` }))
  const newStubs = insertStubAt(currentStubs, draggedStub, target.beforeKey)
  const items: ProjectItemOrderItemRequest[] = newStubs.map((s) => {
    const [type, idStr] = s.key.split(':')
    return { type: type as ItemType, id: idStr }
  })

  if (dragged.sourceContainer === containerKey) {
    try {
      await itemOrderStore.reorderProjectItems(currentProjectId.value, items)
    } catch (e) {
      notification.error((e as ApiError).message)
    }
    return
  }

  // クロスコンテナは常にタスクグループ配下からの移動（タスクグループ自体はProject直下にしか存在しない）。
  if (dragged.kind !== 'TASK') return
  try {
    const result = await itemOrderStore.moveTaskAcrossContainer({
      taskId: dragged.id,
      parentReq: { projectId: currentProjectId.value, taskGroupId: null },
      sourceContainer: dragged.sourceContainer,
      target: { type: 'project', projectId: currentProjectId.value, items },
    })
    if (result.reorderFailed) {
      notification.info(
        'タスクを移動しましたが、並べ替えには失敗しました。最新の状態を読み込み直してください。',
      )
    } else {
      notification.success('タスクを移動しました。')
    }
  } catch (e) {
    notification.error((e as ApiError).message)
  }
}
</script>

<template>
  <div class="project-detail-view">
    <AppBreadcrumb v-if="breadcrumbItems.length > 0" :items="breadcrumbItems" />

    <LoadingIndicator v-if="projectStore.loading" />
    <ErrorMessage v-else-if="projectStore.error" :error="projectStore.error" />
    <template v-else-if="projectStore.currentProject">
      <div class="header">
        <div>
          <h1>
            {{ projectStore.currentProject.title }}
            <TutorialHelpButton
              chapter-id="tasks"
              chapter-title="タスク管理"
              :scope="TUTORIAL_SCOPES.projectDetail"
            />
          </h1>
          <div class="project-meta">
            <FinishedCheckbox
              :model-value="projectStore.currentProject.isFinished"
              :disabled="
                finishedUpdating || (!projectStore.currentProject.isFinished && hasUnfinishedTasks)
              "
              @update:model-value="handleFinishedToggle"
            />
            <div class="project-estimate">
              <span>プロジェクト全体の見積</span>
              <strong>{{ formatMinutes(projectEstimatedMinutes) }}</strong>
            </div>
          </div>
          <p v-if="!projectStore.currentProject.isFinished && hasUnfinishedTasks" class="hint">
            未完了のタスクがあるため、完了状態にできません。
          </p>
          <ErrorMessage v-if="finishedError" :error="finishedError" />
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
            <CompletedItemsToggle v-model="showCompletedItems" />
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
        <p v-else-if="visibleOrderedItems.length === 0" class="empty">
          タスクグループ・タスクがまだありません。
        </p>
        <div v-else class="entity-rows">
          <template v-for="(item, index) in visibleOrderedItems" :key="`${item.kind}-${item.id}`">
            <TaskGroupListItem
              v-if="item.kind === 'TASK_GROUP'"
              :task-group="item.taskGroup"
              :task-groups="taskGroupStore.taskGroups"
              :show-completed-tasks="showCompletedItems"
              :can-move-up="index > 0"
              :can-move-down="index < visibleOrderedItems.length - 1"
              @move-up="handleMoveUp(index)"
              @move-down="handleMoveDown(index)"
              @item-drop="handleItemDrop"
            />
            <TaskListItem
              v-else
              :task="item.task"
              :to="`/projects/${currentProjectId}/tasks/${item.task.id}`"
              :project-id="currentProjectId"
              :container-key="`project:${currentProjectId}`"
              :task-groups="taskGroupStore.taskGroups"
              :can-move-up="index > 0"
              :can-move-down="index < visibleOrderedItems.length - 1"
              @move-up="handleMoveUp(index)"
              @move-down="handleMoveDown(index)"
              @item-drop="handleItemDrop"
            />
          </template>
          <div
            class="end-drop-zone"
            @dragover.prevent="handleEndDragOver"
            @drop.prevent="handleItemDrop"
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
      <template #title-extra>
        <TutorialHelpButton
          chapter-id="tasks"
          chapter-title="タスク管理"
          :scope="TUTORIAL_SCOPES.taskForm"
        />
      </template>
      <TaskForm
        :submitting="creatingTask"
        :error="createTaskError"
        :task-groups="taskGroupStore.taskGroups"
        show-task-group-selector
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

.project-meta {
  display: flex;
  align-items: center;
  gap: 0.8em;
  flex-wrap: wrap;
}

.hint {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.project-estimate {
  display: inline-flex;
  align-items: baseline;
  gap: 0.55em;
  padding: 0.45em 0.75em;
  border: 1px solid var(--color-accent);
  border-radius: 8px;
  background-color: var(--color-surface);
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.project-estimate strong {
  color: var(--color-accent);
  font-size: 1.1rem;
  font-variant-numeric: tabular-nums;
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

.end-drop-zone {
  min-height: 1.2em;
}

.empty {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}
</style>
