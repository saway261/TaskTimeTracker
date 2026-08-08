<script setup lang="ts">
import { RouterLink } from 'vue-router'

defineProps<{
  projectId: string
  taskGroupId: string
}>()
</script>

<template>
  <div class="task-group-detail-view">
    <RouterLink :to="`/projects/${projectId}`">← プロジェクトへ戻る</RouterLink>
    <p class="hint">タスクグループ詳細はフェーズ3で実装する。</p>
  </div>
</template>

<style scoped>
.task-group-detail-view {
  padding: 1.2em;
  display: flex;
  flex-direction: column;
  gap: 1em;
}

.hint {
  color: var(--color-text-muted);
}
</style>
