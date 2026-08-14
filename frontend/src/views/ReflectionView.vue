<script setup lang="ts">
import { onMounted } from 'vue'
import { useProjectStore } from '@/stores/projectStore'
import { useReflectionStore } from '@/stores/reflectionStore'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import ReflectionProjectSelect from '@/components/reflection/ReflectionProjectSelect.vue'

const projectStore = useProjectStore()
const reflectionStore = useReflectionStore()

// 完了状態にかかわらず全プロジェクトを選択肢に出すため、isFinishedを指定せず取得する。
onMounted(() => {
  projectStore.fetchProjects().catch(() => {})
})

function selectProject(projectId: number | null) {
  if (projectId === null) {
    reflectionStore.clear()
    return
  }
  reflectionStore.fetchOverview(projectId).catch(() => {})
}
</script>

<template>
  <div class="reflection-view">
    <h1>振り返り</h1>

    <LoadingIndicator v-if="projectStore.loading" />
    <ErrorMessage v-else-if="projectStore.error" :error="projectStore.error" />
    <template v-else>
      <ReflectionProjectSelect
        :model-value="reflectionStore.selectedProjectId"
        :projects="projectStore.projects"
        @update:model-value="selectProject"
      />

      <p v-if="reflectionStore.selectedProjectId === null" class="guidance">
        振り返りを確認したいプロジェクトを選択してください。
      </p>
      <LoadingIndicator v-else-if="reflectionStore.loading" />
      <ErrorMessage v-else-if="reflectionStore.error" :error="reflectionStore.error" />
    </template>
  </div>
</template>

<style scoped>
.reflection-view {
  padding: 1.2em;
  display: flex;
  flex-direction: column;
  gap: 1em;
}

.reflection-view h1 {
  margin: 0;
}

.guidance {
  color: var(--color-text-muted);
}
</style>
