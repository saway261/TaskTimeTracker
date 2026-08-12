<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import BaseInput from '@/components/common/BaseInput.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import type { ApiError } from '@/types/apiError'
import type { WorkSession, WorkSessionUpdateRequest } from '@/types/workSession'
import { fromDatetimeLocalValue, toDatetimeLocalValue } from '@/utils/datetimeLocal'

// 既存セッションの編集専用（新規登録はManualWorkSessionForm）。typeを変更する導線は作らない。
const props = withDefaults(
  defineProps<{
    session: WorkSession
    submitting?: boolean
    error?: ApiError | null
  }>(),
  {
    submitting: false,
    error: null,
  },
)

const emit = defineEmits<{
  submit: [req: WorkSessionUpdateRequest]
  cancel: []
}>()

const isManual = computed(() => props.session.type === 'MANUAL')

const minutes = ref(props.session.minutes?.toString() ?? '')
const startedAtLocal = ref(
  props.session.startedAt ? toDatetimeLocalValue(props.session.startedAt) : '',
)
const endedAtLocal = ref(props.session.endedAt ? toDatetimeLocalValue(props.session.endedAt) : '')

watch(
  () => props.session,
  (session) => {
    minutes.value = session.minutes?.toString() ?? ''
    startedAtLocal.value = session.startedAt ? toDatetimeLocalValue(session.startedAt) : ''
    endedAtLocal.value = session.endedAt ? toDatetimeLocalValue(session.endedAt) : ''
  },
)

// B3はサーバ側で修正済みだが、サーバ往復を待たず即時フィードバックするためフロントでも検証する。
const timeOrderValid = computed(() => {
  if (isManual.value) return true
  if (!startedAtLocal.value || !endedAtLocal.value) return false
  return new Date(endedAtLocal.value) >= new Date(startedAtLocal.value)
})

const canSubmit = computed(() => {
  if (isManual.value) {
    const trimmed = minutes.value.trim()
    if (trimmed === '') return false
    const n = Number(trimmed)
    return Number.isInteger(n) && n > 0
  }
  return startedAtLocal.value !== '' && endedAtLocal.value !== '' && timeOrderValid.value
})

function handleSubmit() {
  if (!canSubmit.value) return
  if (isManual.value) {
    emit('submit', { type: 'MANUAL', minutes: Number(minutes.value) })
  } else {
    emit('submit', {
      type: 'TIMER',
      startedAt: fromDatetimeLocalValue(startedAtLocal.value),
      endedAt: fromDatetimeLocalValue(endedAtLocal.value),
    })
  }
}
</script>

<template>
  <form class="work-session-form" @submit.prevent="handleSubmit">
    <ErrorMessage v-if="error" :error="error" />

    <BaseInput
      v-if="isManual"
      v-model="minutes"
      label="作業時間（分）"
      type="number"
      required
      :error="error?.fieldErrors.minutes"
    />

    <template v-else>
      <BaseInput
        v-model="startedAtLocal"
        label="開始日時"
        type="datetime-local"
        step="1"
        required
        :error="error?.fieldErrors.startedAt"
      />
      <BaseInput
        v-model="endedAtLocal"
        label="終了日時"
        type="datetime-local"
        step="1"
        required
        :error="error?.fieldErrors.endedAt"
      />
      <p v-if="startedAtLocal && endedAtLocal && !timeOrderValid" class="validation-error">
        終了日時は開始日時以降にしてください。
      </p>
    </template>

    <div class="actions">
      <BaseButton type="button" variant="secondary" @click="emit('cancel')">
        キャンセル
      </BaseButton>
      <BaseButton type="submit" :disabled="submitting || !canSubmit">更新する</BaseButton>
    </div>
  </form>
</template>

<style scoped>
.work-session-form {
  display: flex;
  flex-direction: column;
  gap: 1em;
}

.validation-error {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-danger);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.6em;
}
</style>
