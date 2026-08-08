<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { httpClient } from '@/api/httpClient'
import { normalizeError, type ApiError } from '@/types/apiError'
import { toPositiveInt } from '@/utils/routeParams'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'

interface ProjectDetail {
  id: number
  title: string
  description: string | null
  isFinished: boolean
}

const props = defineProps<{
  projectId: string
}>()

const project = ref<ProjectDetail | null>(null)
const loading = ref(false)
const error = ref<ApiError | null>(null)
const invalidId = ref(false)

async function load() {
  const id = toPositiveInt(props.projectId)
  if (id === null) {
    invalidId.value = true
    return
  }
  invalidId.value = false

  loading.value = true
  error.value = null
  try {
    const res = await httpClient.get<ProjectDetail>(`/projects/${id}`)
    project.value = res.data
  } catch (e) {
    error.value = normalizeError(e)
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => props.projectId, load)
</script>

<template>
  <div class="project-detail-view">
    <RouterLink to="/projects">← プロジェクト一覧</RouterLink>

    <p v-if="invalidId">不正なプロジェクトIDです。</p>
    <LoadingIndicator v-else-if="loading" />
    <ErrorMessage v-else-if="error" :error="error" />
    <template v-else-if="project">
      <h1>{{ project.title }}</h1>
      <p v-if="project.description">{{ project.description }}</p>
      <p class="hint">タスクグループ・タスク欄はフェーズ3・4で追加する。</p>
    </template>
  </div>
</template>

<style scoped>
.project-detail-view {
  padding: 1.2em;
  display: flex;
  flex-direction: column;
  gap: 1em;
}

.hint {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}
</style>
