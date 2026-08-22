<script setup lang="ts">
import { computed, ref, useId, watch } from 'vue'
import BaseInput from '@/components/common/BaseInput.vue'
import BaseTextarea from '@/components/common/BaseTextarea.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import TagSelect from '@/components/tag/TagSelect.vue'
import TagBadgeList from '@/components/tag/TagBadgeList.vue'
import type { ApiError } from '@/types/apiError'
import type { TaskResponse } from '@/types/task'
import type { TaskGroupResponse } from '@/types/taskGroup'
import type { TagSummary } from '@/types/tag'

const props = withDefaults(
  defineProps<{
    task?: TaskResponse | null
    submitting?: boolean
    error?: ApiError | null
    taskGroups?: TaskGroupResponse[]
    showTaskGroupSelector?: boolean
  }>(),
  {
    task: null,
    submitting: false,
    error: null,
    taskGroups: () => [],
    showTaskGroupSelector: false,
  },
)

const emit = defineEmits<{
  submit: [
    payload: {
      title: string
      description: string | null
      estimatedMinutes?: number
      taskGroupId?: string | null
      tagIds?: string[]
    },
  ]
  cancel: []
}>()

const title = ref(props.task?.title ?? '')
const description = ref(props.task?.description ?? '')
// 見積時間は登録時のみ入力する。更新時は別API・別UI（§4.4）のためここでは扱わない。
const estimatedMinutes = ref(props.task?.estimatedMinutes?.toString() ?? '')
const selectedTaskGroupId = ref<string>('')
const selectedTags = ref<TagSummary[]>([])
const taskGroupSelectId = useId()

watch(
  () => props.task,
  (task) => {
    title.value = task?.title ?? ''
    description.value = task?.description ?? ''
    estimatedMinutes.value = task?.estimatedMinutes?.toString() ?? ''
    selectedTags.value = []
  },
)

const canSubmit = computed(() => {
  if (title.value.trim() === '') return false
  if (props.task) return true
  const trimmed = estimatedMinutes.value.trim()
  if (trimmed === '') return false
  const n = Number(trimmed)
  return Number.isInteger(n) && n > 0
})

function handleSubmit() {
  if (!canSubmit.value) return
  emit('submit', {
    title: title.value,
    description: description.value === '' ? null : description.value,
    ...(props.task
      ? {}
      : {
          estimatedMinutes: Number(estimatedMinutes.value),
          tagIds: selectedTags.value.map((tag) => tag.id),
        }),
    ...(props.showTaskGroupSelector
      ? { taskGroupId: selectedTaskGroupId.value === '' ? null : selectedTaskGroupId.value }
      : {}),
  })
}

function removeSelectedTag(tagId: string) {
  selectedTags.value = selectedTags.value.filter((tag) => tag.id !== tagId)
}
</script>

<template>
  <form class="task-form" @submit.prevent="handleSubmit">
    <ErrorMessage v-if="error" :error="error" />
    <BaseInput
      v-model="title"
      label="タイトル"
      required
      :maxlength="20"
      :error="error?.fieldErrors.title"
    />
    <BaseTextarea
      v-model="description"
      label="説明"
      :maxlength="200"
      :error="error?.fieldErrors.description"
    />
    <BaseInput
      v-if="!task"
      v-model="estimatedMinutes"
      label="見積時間（分）"
      required
      type="number"
      :error="error?.fieldErrors.estimatedMinutes"
    />
    <template v-if="!task">
      <div class="selected-task-tags" aria-live="polite">
        <span class="selected-task-tags-label">選択中のタグ</span>
        <TagBadgeList
          v-if="selectedTags.length > 0"
          :tags="selectedTags"
          :limit="null"
          removable
          :disabled="submitting"
          @remove="removeSelectedTag"
        />
        <span v-else class="selected-task-tags-empty">なし</span>
      </div>
      <TagSelect
        v-model="selectedTags"
        :disabled="submitting"
        :error="error?.fieldErrors.tagIds"
        :show-selected="false"
        label="タグを追加"
      />
    </template>
    <div v-if="!task && showTaskGroupSelector" class="select-field">
      <label :for="taskGroupSelectId">作成先</label>
      <select :id="taskGroupSelectId" v-model="selectedTaskGroupId" :disabled="submitting">
        <option value="">プロジェクト直下（タスクグループに属さない）</option>
        <option v-for="taskGroup in taskGroups" :key="taskGroup.id" :value="taskGroup.id">
          {{ taskGroup.title }}
        </option>
      </select>
    </div>
    <div class="actions">
      <BaseButton type="button" variant="secondary" @click="emit('cancel')">
        キャンセル
      </BaseButton>
      <BaseButton type="submit" :disabled="submitting || !canSubmit">
        {{ task ? '更新する' : '登録する' }}
      </BaseButton>
    </div>
  </form>
</template>

<style scoped>
.task-form {
  display: flex;
  flex-direction: column;
  gap: 1em;
}

.select-field {
  display: flex;
  flex-direction: column;
  gap: 0.3em;
}

.selected-task-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5em;
  min-height: 2em;
  padding: 0.55em 0.65em;
  border: 1px solid var(--color-surface-muted);
  border-radius: 6px;
  background: var(--color-surface);
}

.selected-task-tags-label {
  flex-shrink: 0;
  color: var(--color-text);
  font-size: 0.85rem;
  font-weight: 600;
}

.selected-task-tags-empty {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.select-field label {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text);
}

.select-field select {
  padding: 0.5em 0.7em;
  border: 1px solid var(--color-surface-muted);
  border-radius: 6px;
  background-color: var(--color-surface);
  color: var(--color-text);
  font: inherit;
}

.select-field select:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 1px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.6em;
}
</style>
