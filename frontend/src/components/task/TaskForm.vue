<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import BaseInput from '@/components/common/BaseInput.vue'
import BaseTextarea from '@/components/common/BaseTextarea.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import type { ApiError } from '@/types/apiError'
import type { TaskResponse } from '@/types/task'

const props = withDefaults(
  defineProps<{
    task?: TaskResponse | null
    submitting?: boolean
    error?: ApiError | null
  }>(),
  {
    task: null,
    submitting: false,
    error: null,
  },
)

const emit = defineEmits<{
  submit: [payload: { title: string; description: string | null; estimatedMinutes?: number }]
  cancel: []
}>()

const title = ref(props.task?.title ?? '')
const description = ref(props.task?.description ?? '')
// 見積時間は登録時のみ入力する。更新時は別API・別UI（§4.4）のためここでは扱わない。
const estimatedMinutes = ref(props.task?.estimatedMinutes?.toString() ?? '')

watch(
  () => props.task,
  (task) => {
    title.value = task?.title ?? ''
    description.value = task?.description ?? ''
    estimatedMinutes.value = task?.estimatedMinutes?.toString() ?? ''
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
    ...(props.task ? {} : { estimatedMinutes: Number(estimatedMinutes.value) }),
  })
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

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.6em;
}
</style>
