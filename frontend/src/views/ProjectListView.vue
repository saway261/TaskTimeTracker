<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/projectStore'
import { useNotificationStore } from '@/stores/notificationStore'
import type { ApiError } from '@/types/apiError'
import type { ProjectCreateRequest } from '@/types/project'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import ProjectCard from '@/components/project/ProjectCard.vue'
import ProjectForm from '@/components/project/ProjectForm.vue'

type FilterTab = 'unfinished' | 'finished' | 'all'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()
const notification = useNotificationStore()

const tab = computed<FilterTab>(() => {
  const q = route.query.isFinished
  if (q === 'true') return 'finished'
  if (q === 'false') return 'unfinished'
  return 'all'
})

function selectTab(next: FilterTab) {
  router.replace({
    query: next === 'all' ? {} : { isFinished: next === 'finished' ? 'true' : 'false' },
  })
}

function load() {
  const isFinished = tab.value === 'all' ? undefined : tab.value === 'finished'
  projectStore.fetchProjects(isFinished).catch(() => {})
}

onMounted(load)
watch(tab, load)

const showCreateModal = ref(false)
const creating = ref(false)
const createError = ref<ApiError | null>(null)

function openCreateModal() {
  createError.value = null
  showCreateModal.value = true
}

async function handleCreate(payload: { title: string; description: string | null }) {
  creating.value = true
  createError.value = null
  try {
    await projectStore.createProject(payload as ProjectCreateRequest)
    notification.success('プロジェクトを登録しました。')
    showCreateModal.value = false
  } catch (e) {
    createError.value = e as ApiError
  } finally {
    creating.value = false
  }
}
</script>

<template>
  <div class="project-list-view">
    <div class="header">
      <h1>プロジェクト一覧</h1>
      <BaseButton @click="openCreateModal">＋ 新規プロジェクト</BaseButton>
    </div>

    <div class="tabs" role="tablist">
      <button
        type="button"
        role="tab"
        :aria-selected="tab === 'unfinished'"
        :class="{ active: tab === 'unfinished' }"
        @click="selectTab('unfinished')"
      >
        未完了
      </button>
      <button
        type="button"
        role="tab"
        :aria-selected="tab === 'finished'"
        :class="{ active: tab === 'finished' }"
        @click="selectTab('finished')"
      >
        完了
      </button>
      <button
        type="button"
        role="tab"
        :aria-selected="tab === 'all'"
        :class="{ active: tab === 'all' }"
        @click="selectTab('all')"
      >
        全件
      </button>
    </div>

    <LoadingIndicator v-if="projectStore.loading" />
    <ErrorMessage v-else-if="projectStore.error" :error="projectStore.error" />
    <p v-else-if="projectStore.projects.length === 0" class="empty">
      プロジェクトがまだありません。
    </p>
    <div v-else class="projects">
      <ProjectCard v-for="project in projectStore.projects" :key="project.id" :project="project" />
    </div>

    <BaseModal v-model="showCreateModal" title="新規プロジェクト">
      <ProjectForm
        :submitting="creating"
        :error="createError"
        @submit="handleCreate"
        @cancel="showCreateModal = false"
      />
    </BaseModal>
  </div>
</template>

<style scoped>
.project-list-view {
  padding: 1.2em;
  display: flex;
  flex-direction: column;
  gap: 1em;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1em;
}

.header h1 {
  margin: 0;
}

.tabs {
  display: flex;
  gap: 0.4em;
  border-bottom: 1px solid var(--color-surface-muted);
}

.tabs button {
  padding: 0.6em 1em;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  color: var(--color-text-muted);
  font-size: 0.95rem;
}

.tabs button.active {
  color: var(--color-text);
  border-bottom-color: var(--color-accent);
  font-weight: 600;
}

.empty {
  color: var(--color-text-muted);
}

.projects {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 1em;
}
</style>
