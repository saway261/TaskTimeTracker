<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useProjectStore } from '@/stores/projectStore'
import { useTaskGroupStore } from '@/stores/taskGroupStore'
import { useTaskStore } from '@/stores/taskStore'
import { projectContainerKey, useItemOrderStore } from '@/stores/itemOrderStore'
import { useNotificationStore } from '@/stores/notificationStore'
import { toPositiveInt } from '@/utils/routeParams'
import { sortProjectItemsByOrder } from '@/utils/sort'
import { insertStubAt } from '@/utils/dragReorder'
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
const itemOrderStore = useItemOrderStore()
const notification = useNotificationStore()

const invalidId = ref(false)
const showEditModal = ref(false)
const updating = ref(false)
const updateError = ref<ApiError | null>(null)

const numericId = computed(() => toPositiveInt(props.projectId))
// currentProjectが読み込まれた時点でnumericIdは必ず有効なため、テンプレート内の型絞り込み用に用意する。
const currentProjectId = computed(() => projectStore.currentProject?.id ?? numericId.value ?? 0)

const breadcrumbItems = computed(() => {
  const project = projectStore.currentProject
  if (!project) return []
  return [{ label: 'プロジェクト一覧', to: '/projects' }, { label: project.title }]
})

const directTasks = computed(() => taskStore.tasks.filter((t) => t.projectId !== null))

type OrderedItem =
  | { kind: 'TASK_GROUP'; id: number; taskGroup: TaskGroupResponse }
  | { kind: 'TASK'; id: number; task: TaskResponse }

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
  // 並び順は無ければid昇順にフォールバックして表示できるため、失敗してもページ全体は止めない。
  itemOrderStore.fetchProjectItemOrder(id).catch(() => {})
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
}) {
  const id = numericId.value
  if (id === null) return
  creatingTask.value = true
  createTaskError.value = null
  try {
    const task = await taskStore.createTaskInProject(id, payload as TaskCreateRequest)
    itemOrderStore.appendToContainerOrder(projectContainerKey(id), 'TASK', task.id)
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
  const items = orderedItems.value.map((it) => ({ type: it.kind, id: it.id }))
  ;[items[index - 1], items[index]] = [items[index], items[index - 1]]
  try {
    await itemOrderStore.reorderProjectItems(currentProjectId.value, items)
  } catch (e) {
    notification.error((e as ApiError).message)
  }
}

async function handleMoveDown(index: number) {
  if (index >= orderedItems.value.length - 1) return
  const items = orderedItems.value.map((it) => ({ type: it.kind, id: it.id }))
  ;[items[index], items[index + 1]] = [items[index + 1], items[index]]
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
    return { type: type as ItemType, id: Number(idStr) }
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
        <p v-else-if="orderedItems.length === 0" class="empty">
          タスクグループ・タスクがまだありません。
        </p>
        <div v-else class="entity-rows">
          <template v-for="(item, index) in orderedItems" :key="`${item.kind}-${item.id}`">
            <TaskGroupListItem
              v-if="item.kind === 'TASK_GROUP'"
              :task-group="item.taskGroup"
              :task-groups="taskGroupStore.taskGroups"
              :can-move-up="index > 0"
              :can-move-down="index < orderedItems.length - 1"
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
              :can-move-down="index < orderedItems.length - 1"
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

.end-drop-zone {
  min-height: 1.2em;
}

.empty {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}
</style>
