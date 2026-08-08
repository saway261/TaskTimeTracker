<script setup lang="ts">
import { RouterLink } from 'vue-router'
import type { ProjectResponse } from '@/types/project'

defineProps<{
  project: ProjectResponse
}>()
</script>

<template>
  <RouterLink :to="`/projects/${project.id}`" class="project-card">
    <h2>{{ project.title }}</h2>
    <p v-if="project.description" class="description">{{ project.description }}</p>
    <span class="status" :class="{ finished: project.isFinished }">
      {{ project.isFinished ? '完了' : '未完了' }}
    </span>
  </RouterLink>
</template>

<style scoped>
.project-card {
  display: flex;
  flex-direction: column;
  gap: 0.5em;
  padding: 1em 1.2em;
  border-radius: 8px;
  background-color: var(--color-surface);
  border: 1px solid var(--color-surface-muted);
  color: var(--color-text);
  text-decoration: none;
}

.project-card:hover {
  border-color: var(--color-accent);
}

.project-card:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.project-card h2 {
  margin: 0;
  font-size: 1.05rem;
}

.description {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.status {
  align-self: flex-start;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.status.finished {
  color: var(--color-success);
}
</style>
