<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useReflectionStore } from '@/stores/reflectionStore'
import { useNotificationStore } from '@/stores/notificationStore'
import type { ReflectionRequest, ReflectionTaskResponse } from '@/types/reflection'
import type { ApiError } from '@/types/apiError'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import AppBreadcrumb from '@/components/common/AppBreadcrumb.vue'
import ReflectionTaskRow from '@/components/reflection/ReflectionTaskRow.vue'
import ReflectionTaskGroupSection from '@/components/reflection/ReflectionTaskGroupSection.vue'
import ReflectionModal from '@/components/reflection/ReflectionModal.vue'
import ReflectionAggregateSummary from '@/components/reflection/ReflectionAggregateSummary.vue'
import TutorialHelpButton from '@/components/tutorial/TutorialHelpButton.vue'
import { TUTORIAL_SCOPES } from '@/tutorial/scopes'

const props = defineProps<{
  projectId: string
}>()

const reflectionStore = useReflectionStore()
const notification = useNotificationStore()

const overview = computed(() => reflectionStore.overview)
const allTasks = computed(() => {
  const currentOverview = overview.value
  if (!currentOverview) return []
  return [...currentOverview.tasks, ...currentOverview.taskGroups.flatMap((group) => group.tasks)]
})
const isEmpty = computed(
  (): boolean =>
    overview.value !== null &&
    overview.value.tasks.length === 0 &&
    overview.value.taskGroups.length === 0,
)

const breadcrumbItems = computed(() => {
  const currentOverview = overview.value
  if (!currentOverview) return []
  return [{ label: '振り返り', to: '/reflections' }, { label: currentOverview.projectTitle }]
})

const expandedGroupIds = ref(new Set<string>())

function toggleGroup(id: string) {
  const next = new Set(expandedGroupIds.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  expandedGroupIds.value = next
}

// --- 振り返りモーダル ---
const activeTask = ref<ReflectionTaskResponse | null>(null)
const submittingReflection = ref(false)
const reflectionError = ref<ApiError | null>(null)

function openReflectionModal(task: ReflectionTaskResponse) {
  reflectionError.value = null
  activeTask.value = task
}

function closeReflectionModal() {
  activeTask.value = null
}

async function handleReflectionSubmit(payload: ReflectionRequest) {
  const task = activeTask.value
  if (!task) return
  submittingReflection.value = true
  reflectionError.value = null
  try {
    if (task.reflection) {
      await reflectionStore.updateReflection(task.id, payload)
      notification.success('振り返りを更新しました。')
    } else {
      await reflectionStore.createReflection(task.id, payload)
      notification.success('振り返りを登録しました。')
    }
    activeTask.value = null
  } catch (e) {
    reflectionError.value = e as ApiError
  } finally {
    submittingReflection.value = false
  }
}

async function load() {
  expandedGroupIds.value = new Set()
  closeReflectionModal()
  await reflectionStore.fetchOverview(props.projectId).catch(() => {})
}

onMounted(load)
watch(() => props.projectId, load)
</script>

<template>
  <div class="reflection-detail-view">
    <AppBreadcrumb v-if="breadcrumbItems.length > 0" :items="breadcrumbItems" />

    <LoadingIndicator v-if="reflectionStore.loading" />
    <ErrorMessage v-else-if="reflectionStore.error" :error="reflectionStore.error" />
    <template v-else-if="overview">
      <h1>
        {{ overview.projectTitle }}
        <TutorialHelpButton
          chapter-id="reflections"
          chapter-title="振り返り"
          :scope="TUTORIAL_SCOPES.reflectionDetail"
        />
      </h1>

      <p v-if="isEmpty" class="empty">このプロジェクトには完了したタスクがまだありません。</p>
      <template v-else>
        <section class="project-summary" aria-labelledby="project-summary-title">
          <h2 id="project-summary-title">プロジェクト全体</h2>
          <ReflectionAggregateSummary :tasks="allTasks" show-actual prominent />
        </section>

        <div class="reflection-rows">
          <ReflectionTaskRow
            v-for="task in overview.tasks"
            :key="`task-${task.id}`"
            :task="task"
            @open="openReflectionModal(task)"
          />
          <ReflectionTaskGroupSection
            v-for="group in overview.taskGroups"
            :key="`group-${group.id}`"
            :task-group="group"
            :is-open="expandedGroupIds.has(group.id)"
            @toggle="toggleGroup(group.id)"
            @open="openReflectionModal"
          />
        </div>
      </template>
    </template>

    <ReflectionModal
      :model-value="activeTask !== null"
      :task="activeTask"
      :submitting="submittingReflection"
      :error="reflectionError"
      @update:model-value="closeReflectionModal"
      @submit="handleReflectionSubmit"
    />
  </div>
</template>

<style scoped>
.reflection-detail-view {
  padding: 1.2em;
  display: flex;
  flex-direction: column;
  gap: 1em;
}

.reflection-detail-view h1 {
  margin: 0;
}

.empty {
  color: var(--color-text-muted);
}

.project-summary {
  display: flex;
  flex-direction: column;
  gap: 0.6em;
}

.project-summary h2 {
  margin: 0;
  font-size: 1.05rem;
}

.reflection-rows {
  display: flex;
  flex-direction: column;
  gap: 0.6em;
}
</style>
