<script setup lang="ts">
import { ref, watch } from 'vue'
import BaseInput from '@/components/common/BaseInput.vue'
import BaseTextarea from '@/components/common/BaseTextarea.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import type { ApiError } from '@/types/apiError'
import type { TaskGroupResponse } from '@/types/taskGroup'

const props = withDefaults(
  defineProps<{
    taskGroup?: TaskGroupResponse | null
    submitting?: boolean
    error?: ApiError | null
  }>(),
  {
    taskGroup: null,
    submitting: false,
    error: null,
  },
)

const emit = defineEmits<{
  submit: [payload: { title: string; description: string | null; isFinished?: boolean }]
  cancel: []
}>()

const title = ref(props.taskGroup?.title ?? '')
const description = ref(props.taskGroup?.description ?? '')
const isFinished = ref(props.taskGroup?.isFinished ?? false)

watch(
  () => props.taskGroup,
  (taskGroup) => {
    title.value = taskGroup?.title ?? ''
    description.value = taskGroup?.description ?? ''
    isFinished.value = taskGroup?.isFinished ?? false
  },
)

function handleSubmit() {
  emit('submit', {
    title: title.value,
    description: description.value === '' ? null : description.value,
    ...(props.taskGroup ? { isFinished: isFinished.value } : {}),
  })
}
</script>

<template>
  <form class="task-group-form" @submit.prevent="handleSubmit">
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
    <label v-if="taskGroup" class="finished-checkbox">
      <input v-model="isFinished" type="checkbox" />
      完了にする
    </label>
    <div class="actions">
      <BaseButton type="button" variant="secondary" @click="emit('cancel')">
        キャンセル
      </BaseButton>
      <BaseButton type="submit" :disabled="submitting || title.trim() === ''">
        {{ taskGroup ? '更新する' : '登録する' }}
      </BaseButton>
    </div>
  </form>
</template>

<style scoped>
.task-group-form {
  display: flex;
  flex-direction: column;
  gap: 1em;
}

.finished-checkbox {
  display: flex;
  align-items: center;
  gap: 0.5em;
  font-size: 0.9rem;
  color: var(--color-text);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.6em;
}
</style>
