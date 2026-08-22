<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useProjectStore } from '@/stores/projectStore'
import { useNotificationStore } from '@/stores/notificationStore'
import type { ApiError } from '@/types/apiError'
import type { ProjectCreateRequest } from '@/types/project'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import CompletedItemsToggle from '@/components/common/CompletedItemsToggle.vue'
import ProjectCard from '@/components/project/ProjectCard.vue'
import ProjectForm from '@/components/project/ProjectForm.vue'

const projectStore = useProjectStore()
const notification = useNotificationStore()

const showCompletedProjects = ref(false)

const visibleProjects = computed(() =>
  showCompletedProjects.value
    ? projectStore.projects
    : projectStore.projects.filter((project) => !project.isFinished),
)

onMounted(() => {
  projectStore.fetchProjects().catch(() => {})
})

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
      <h1>タスク管理</h1>
      <div class="header-actions">
        <CompletedItemsToggle v-model="showCompletedProjects" />
        <BaseButton @click="openCreateModal">＋ 新規プロジェクト</BaseButton>
      </div>
    </div>

    <LoadingIndicator v-if="projectStore.loading" />
    <ErrorMessage v-else-if="projectStore.error" :error="projectStore.error" />
    <p v-else-if="visibleProjects.length === 0" class="empty">プロジェクトがまだありません。</p>
    <div v-else class="projects">
      <ProjectCard v-for="project in visibleProjects" :key="project.id" :project="project" />
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.6em;
  flex-wrap: wrap;
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
