<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { httpClient } from '@/api/httpClient'
import { normalizeError, type ApiError } from '@/types/apiError'
import { sortById } from '@/utils/sort'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'

interface ProjectListItem {
  id: number
  title: string
  isFinished: boolean
}

const projects = ref<ProjectListItem[]>([])
const loading = ref(false)
const error = ref<ApiError | null>(null)

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await httpClient.get<ProjectListItem[]>('/projects')
    projects.value = sortById(res.data)
  } catch (e) {
    error.value = normalizeError(e)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="project-list-view">
    <h1>プロジェクト一覧</h1>

    <LoadingIndicator v-if="loading" />
    <ErrorMessage v-else-if="error" :error="error" />
    <p v-else-if="projects.length === 0">プロジェクトがまだありません。</p>
    <ul v-else class="projects">
      <li v-for="project in projects" :key="project.id">
        <RouterLink :to="`/projects/${project.id}`">
          {{ project.title }}
          <span v-if="project.isFinished" class="finished">完了</span>
        </RouterLink>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.project-list-view {
  padding: 1.2em;
}

.projects {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5em;
}

.projects li a {
  display: block;
  padding: 0.8em 1em;
  border-radius: 6px;
  background-color: var(--color-surface);
  border: 1px solid var(--color-surface-muted);
  color: var(--color-text);
  text-decoration: none;
}

.finished {
  margin-left: 0.6em;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}
</style>
