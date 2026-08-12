<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import type { TaskResponse } from '@/types/task'
import { isFinished } from '@/utils/task'
import { formatMinutes } from '@/utils/duration'

const props = defineProps<{
  task: TaskResponse
  to: string
}>()

const finished = computed(() => isFinished(props.task))
</script>

<template>
  <RouterLink :to="to" class="task-row" :class="{ finished }">
    <span class="label">タスク</span>
    <span class="title">{{ task.title }}</span>
    <span v-if="task.estimatedMinutes !== null" class="estimate">
      見積 {{ formatMinutes(task.estimatedMinutes) }}
    </span>
    <span class="status" :class="{ finished }">
      {{ finished ? '完了' : '未完了' }}
    </span>
  </RouterLink>
</template>

<style scoped>
.task-row {
  display: flex;
  align-items: center;
  gap: 0.6em;
  padding: 0.8em 1em;
  border-radius: 8px;
  background-color: var(--color-surface);
  border: 1px solid var(--color-surface-muted);
  border-left: 4px solid var(--color-task-accent);
  color: var(--color-text);
  text-decoration: none;
}

.task-row:hover {
  background-color: var(--color-surface-muted);
}

.task-row:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: -2px;
}

.task-row.finished {
  border-left-color: var(--color-text-muted);
}

.label {
  flex-shrink: 0;
  font-size: 0.75rem;
  padding: 0.15em 0.5em;
  border-radius: 4px;
  background-color: var(--color-surface-muted);
  color: var(--color-text-muted);
}

.title {
  flex: 1;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.estimate {
  flex-shrink: 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.status {
  flex-shrink: 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.status.finished {
  color: var(--color-success);
}
</style>
