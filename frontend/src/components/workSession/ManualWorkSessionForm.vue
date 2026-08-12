<script setup lang="ts">
import { computed, ref } from 'vue'
import BaseInput from '@/components/common/BaseInput.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import type { ApiError } from '@/types/apiError'

const props = withDefaults(
  defineProps<{
    submitting?: boolean
    error?: ApiError | null
    // タイマー稼働中は同一タスクへの手動セッション登録を無効化する（§6.5）。
    disabled?: boolean
  }>(),
  {
    submitting: false,
    error: null,
    disabled: false,
  },
)

const emit = defineEmits<{
  submit: [minutes: number]
}>()

const minutes = ref('')

const canSubmit = computed(() => {
  const trimmed = minutes.value.trim()
  if (trimmed === '') return false
  const n = Number(trimmed)
  return Number.isInteger(n) && n > 0
})

function handleSubmit() {
  if (!canSubmit.value) return
  emit('submit', Number(minutes.value))
  minutes.value = ''
}
</script>

<template>
  <form class="manual-work-session-form" @submit.prevent="handleSubmit">
    <ErrorMessage v-if="error" :error="error" />
    <div class="row">
      <BaseInput
        v-model="minutes"
        label="作業時間（分）"
        type="number"
        required
        :error="error?.fieldErrors.minutes"
      />
      <BaseButton type="submit" :disabled="props.submitting || props.disabled || !canSubmit">
        記録を追加
      </BaseButton>
    </div>
    <p v-if="disabled" class="hint">タイマー稼働中は手動記録を追加できません。</p>
  </form>
</template>

<style scoped>
.manual-work-session-form {
  display: flex;
  flex-direction: column;
  gap: 0.6em;
}

.row {
  display: flex;
  align-items: flex-end;
  gap: 0.8em;
  flex-wrap: wrap;
}

.row :deep(.base-input) {
  flex: 1;
  min-width: 10em;
}

.hint {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}
</style>
