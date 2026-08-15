<script setup lang="ts">
import { onMounted } from 'vue'
import { useProjectStore } from '@/stores/projectStore'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import ReflectionProjectCard from '@/components/reflection/ReflectionProjectCard.vue'

const projectStore = useProjectStore()

// 完了状態にかかわらず全プロジェクトを選択肢に出すため、isFinishedを指定せず取得する。
onMounted(() => {
  projectStore.fetchProjects().catch(() => {})
})
</script>

<template>
  <div class="reflection-view">
    <h1>振り返り</h1>

    <LoadingIndicator v-if="projectStore.loading" />
    <ErrorMessage v-else-if="projectStore.error" :error="projectStore.error" />
    <p v-else-if="projectStore.projects.length === 0" class="empty">
      プロジェクトがまだありません。
    </p>
    <div v-else class="project-cards">
      <ReflectionProjectCard
        v-for="project in projectStore.projects"
        :key="project.id"
        :project="project"
      />
    </div>
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

.empty {
  color: var(--color-text-muted);
}

.project-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 1em;
}
</style>
