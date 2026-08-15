<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useReflectionStore } from '@/stores/reflectionStore'
import { toPositiveInt } from '@/utils/routeParams'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import AppBreadcrumb from '@/components/common/AppBreadcrumb.vue'
import ReflectionTaskRow from '@/components/reflection/ReflectionTaskRow.vue'
import ReflectionTaskGroupSection from '@/components/reflection/ReflectionTaskGroupSection.vue'

const props = defineProps<{
  projectId: string
}>()

const reflectionStore = useReflectionStore()

const invalidId = ref(false)
const numericId = computed(() => toPositiveInt(props.projectId))

const overview = computed(() => reflectionStore.overview)
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

const expandedGroupIds = ref(new Set<number>())

function toggleGroup(id: number) {
  const next = new Set(expandedGroupIds.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  expandedGroupIds.value = next
}

async function load() {
  const id = numericId.value
  if (id === null) {
    invalidId.value = true
    return
  }
  invalidId.value = false
  expandedGroupIds.value = new Set()
  await reflectionStore.fetchOverview(id).catch(() => {})
}

onMounted(load)
watch(() => props.projectId, load)
</script>

<template>
  <div class="reflection-detail-view">
    <AppBreadcrumb v-if="breadcrumbItems.length > 0" :items="breadcrumbItems" />

    <p v-if="invalidId">不正なプロジェクトIDです。</p>
    <LoadingIndicator v-else-if="reflectionStore.loading" />
    <ErrorMessage v-else-if="reflectionStore.error" :error="reflectionStore.error" />
    <template v-else-if="overview">
      <h1>{{ overview.projectTitle }}</h1>

      <p v-if="isEmpty" class="empty">このプロジェクトには完了したタスクがまだありません。</p>
      <div v-else class="reflection-rows">
        <ReflectionTaskRow v-for="task in overview.tasks" :key="`task-${task.id}`" :task="task" />
        <ReflectionTaskGroupSection
          v-for="group in overview.taskGroups"
          :key="`group-${group.id}`"
          :task-group="group"
          :is-open="expandedGroupIds.has(group.id)"
          @toggle="toggleGroup(group.id)"
        />
      </div>
    </template>
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

.reflection-rows {
  display: flex;
  flex-direction: column;
  gap: 0.6em;
}
</style>
