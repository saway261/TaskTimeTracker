<script setup lang="ts">
import { ref, watch } from 'vue'
import BaseInput from '@/components/common/BaseInput.vue'
import BaseTextarea from '@/components/common/BaseTextarea.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import type { ApiError } from '@/types/apiError'
import type { ProjectResponse } from '@/types/project'

const props = withDefaults(
  defineProps<{
    project?: ProjectResponse | null
    submitting?: boolean
    error?: ApiError | null
  }>(),
  {
    project: null,
    submitting: false,
    error: null,
  },
)

const emit = defineEmits<{
  submit: [payload: { title: string; description: string | null }]
  cancel: []
}>()

const title = ref(props.project?.title ?? '')
const description = ref(props.project?.description ?? '')

watch(
  () => props.project,
  (project) => {
    title.value = project?.title ?? ''
    description.value = project?.description ?? ''
  },
)

function handleSubmit() {
  emit('submit', {
    title: title.value,
    description: description.value === '' ? null : description.value,
  })
}
</script>

<template>
  <form class="project-form" @submit.prevent="handleSubmit">
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
    <div class="actions">
      <BaseButton type="button" variant="secondary" @click="emit('cancel')">
        キャンセル
      </BaseButton>
      <BaseButton type="submit" :disabled="submitting || title.trim() === ''">
        {{ project ? '更新する' : '登録する' }}
      </BaseButton>
    </div>
  </form>
</template>

<style scoped>
.project-form {
  display: flex;
  flex-direction: column;
  gap: 1em;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.6em;
}
</style>
