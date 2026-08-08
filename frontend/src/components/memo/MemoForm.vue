<script setup lang="ts">
import { ref } from 'vue'
import BaseTextarea from '@/components/common/BaseTextarea.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import type { ApiError } from '@/types/apiError'
import type { MemoRequest } from '@/types/memo'

const props = withDefaults(
  defineProps<{
    mode?: 'create' | 'edit'
    initialComment?: string
    submitting?: boolean
    error?: ApiError | null
  }>(),
  {
    mode: 'create',
    initialComment: '',
    submitting: false,
    error: null,
  },
)

const emit = defineEmits<{
  submit: [req: MemoRequest]
  cancel: []
}>()

const comment = ref(props.initialComment)

function handleSubmit() {
  if (comment.value.trim() === '') return
  emit('submit', { comment: comment.value })
  if (props.mode === 'create') {
    comment.value = ''
  }
}
</script>

<template>
  <form class="memo-form" @submit.prevent="handleSubmit">
    <ErrorMessage v-if="error" :error="error" />
    <BaseTextarea
      v-model="comment"
      :label="mode === 'edit' ? 'メモを編集' : 'メモを追加'"
      :maxlength="1000"
      :rows="mode === 'edit' ? 4 : 2"
      :error="error?.fieldErrors.comment"
    />
    <div class="actions">
      <BaseButton v-if="mode === 'edit'" type="button" variant="secondary" @click="emit('cancel')">
        キャンセル
      </BaseButton>
      <BaseButton type="submit" :disabled="submitting || comment.trim() === ''">
        {{ mode === 'edit' ? '更新する' : '追加する' }}
      </BaseButton>
    </div>
  </form>
</template>

<style scoped>
.memo-form {
  display: flex;
  flex-direction: column;
  gap: 0.6em;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.6em;
}
</style>
