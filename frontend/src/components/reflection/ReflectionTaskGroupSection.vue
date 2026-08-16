<script setup lang="ts">
import { useId } from 'vue'
import type { ReflectionTaskGroupResponse, ReflectionTaskResponse } from '@/types/reflection'
import ReflectionAggregateSummary from './ReflectionAggregateSummary.vue'
import ReflectionTaskRow from './ReflectionTaskRow.vue'

defineProps<{
  taskGroup: ReflectionTaskGroupResponse
  isOpen: boolean
}>()

const emit = defineEmits<{
  toggle: []
  open: [task: ReflectionTaskResponse]
}>()

const panelId = useId()
</script>

<template>
  <div class="task-group-wrapper">
    <div class="task-group-card">
      <button
        type="button"
        class="row-header"
        :aria-expanded="isOpen"
        :aria-controls="panelId"
        @click="emit('toggle')"
      >
        <span class="chevron" :class="{ open: isOpen }" aria-hidden="true">›</span>
        <span class="label">タスクグループ</span>
        <span class="title">{{ taskGroup.title }}</span>
      </button>
    </div>

    <div v-if="isOpen" :id="panelId" class="child-tasks">
      <ReflectionAggregateSummary :tasks="taskGroup.tasks" />
      <ReflectionTaskRow
        v-for="task in taskGroup.tasks"
        :key="task.id"
        :task="task"
        @open="emit('open', task)"
      />
    </div>
  </div>
</template>

<style scoped>
.task-group-wrapper {
  display: flex;
  flex-direction: column;
  gap: 0.5em;
}

.task-group-card {
  border-radius: 8px;
  background-color: var(--color-surface);
  border: 1px solid var(--color-surface-muted);
  border-left: 4px solid var(--color-accent);
  overflow: hidden;
}

.row-header {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 0.6em;
  padding: 0.8em 1em;
  background: none;
  border: none;
  cursor: pointer;
  text-align: left;
  color: var(--color-text);
  font: inherit;
}

.row-header:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: -2px;
}

.chevron {
  display: inline-block;
  font-size: 1.1rem;
  color: var(--color-text-muted);
  transition: transform 0.15s ease;
}

.chevron.open {
  transform: rotate(90deg);
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
  min-width: 0;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.child-tasks {
  display: flex;
  flex-direction: column;
  gap: 0.5em;
  padding-left: 2em;
}
</style>
