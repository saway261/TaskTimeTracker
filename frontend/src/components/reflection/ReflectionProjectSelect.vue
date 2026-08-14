<script setup lang="ts">
import { useId } from 'vue'
import type { ProjectResponse } from '@/types/project'

defineProps<{
  modelValue: number | null
  projects: ProjectResponse[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number | null]
}>()

const selectId = useId()

function onChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  emit('update:modelValue', value === '' ? null : Number(value))
}
</script>

<template>
  <div class="reflection-project-select">
    <label :for="selectId">プロジェクト</label>
    <select :id="selectId" :value="modelValue ?? ''" @change="onChange">
      <option value="" disabled>選択してください</option>
      <option v-for="project in projects" :key="project.id" :value="project.id">
        {{ project.title }}
      </option>
    </select>
  </div>
</template>

<style scoped>
.reflection-project-select {
  display: flex;
  flex-direction: column;
  gap: 0.3em;
  max-width: 320px;
}

label {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text);
}

select {
  padding: 0.5em 0.7em;
  border-radius: 6px;
  border: 1px solid var(--color-surface-muted);
  background-color: var(--color-surface);
  color: var(--color-text);
  font-size: 1rem;
}

select:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 1px;
}
</style>
