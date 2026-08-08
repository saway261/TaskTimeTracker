<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

const props = defineProps<{
  projectId: string
  taskId: string
  taskGroupId: string | null
}>()

const backLink = computed(() =>
  props.taskGroupId
    ? `/projects/${props.projectId}/task-groups/${props.taskGroupId}`
    : `/projects/${props.projectId}`,
)
</script>

<template>
  <div class="task-detail-view">
    <RouterLink :to="backLink">← 戻る</RouterLink>
    <p class="hint">タスク詳細はフェーズ4で実装する。</p>
  </div>
</template>

<style scoped>
.task-detail-view {
  padding: 1.2em;
  display: flex;
  flex-direction: column;
  gap: 1em;
}

.hint {
  color: var(--color-text-muted);
}
</style>
