<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import BaseModal from '@/components/common/BaseModal.vue'
import BaseTextarea from '@/components/common/BaseTextarea.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import type { ReflectionRequest, ReflectionTaskResponse } from '@/types/reflection'
import type { ApiError } from '@/types/apiError'
import { formatGap, formatGapRate, formatMinutes } from '@/utils/duration'

const CAUSE_MAX_LENGTH = 200
const NEXT_ACTION_MAX_LENGTH = 1000

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    task: ReflectionTaskResponse | null
    submitting?: boolean
    error?: ApiError | null
  }>(),
  {
    submitting: false,
    error: null,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [payload: ReflectionRequest]
}>()

const mode = computed<'create' | 'edit'>(() => (props.task?.reflection ? 'edit' : 'create'))
const title = computed(() => (mode.value === 'create' ? '振り返りを入力' : '振り返りの詳細・変更'))

const cause = ref('')
const nextAction = ref('')

// 開くたびに対象タスクの現在値へ合わせる。全文をそのまま入れるため、一覧のプレビュー省略は経由しない。
watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    cause.value = props.task?.reflection?.cause ?? ''
    nextAction.value = props.task?.reflection?.nextAction ?? ''
  },
)

const actualText = computed(() => {
  const minutes = props.task?.actualMinutesCached
  return minutes === undefined || minutes === null ? '-' : formatMinutes(minutes)
})
const gapText = computed(() => {
  const minutes = props.task?.gapMinutesCached
  return minutes === undefined || minutes === null ? '-' : formatGap(minutes)
})
const gapRateText = computed(() => {
  const rate = props.task?.gapRateCached
  return rate === undefined || rate === null ? '-' : formatGapRate(rate)
})

// バックエンドと同条件の事前検証（§10）。文字数上限はBaseTextareaのmaxlengthでも防いでいる。
const canSubmit = computed(
  () =>
    cause.value.trim() !== '' &&
    cause.value.length <= CAUSE_MAX_LENGTH &&
    nextAction.value.length <= NEXT_ACTION_MAX_LENGTH,
)

function handleSubmit() {
  if (!canSubmit.value) return
  emit('submit', {
    cause: cause.value.trim(),
    nextAction: nextAction.value.trim() === '' ? null : nextAction.value.trim(),
  })
}

function close() {
  emit('update:modelValue', false)
}
</script>

<template>
  <BaseModal :model-value="modelValue" :title="title" @update:model-value="close">
    <template v-if="task">
      <dl class="reference-info">
        <div>
          <dt>タスク</dt>
          <dd>{{ task.title }}</dd>
        </div>
        <div>
          <dt>実績時間</dt>
          <dd>{{ actualText }}</dd>
        </div>
        <div>
          <dt>誤差</dt>
          <dd>{{ gapText }}</dd>
        </div>
        <div>
          <dt>誤差比</dt>
          <dd>{{ gapRateText }}</dd>
        </div>
      </dl>

      <form class="reflection-form" @submit.prevent="handleSubmit">
        <ErrorMessage v-if="error" :error="error" />
        <BaseTextarea
          v-model="cause"
          label="原因"
          required
          :maxlength="CAUSE_MAX_LENGTH"
          :rows="3"
          :error="error?.fieldErrors.cause"
        />
        <BaseTextarea
          v-model="nextAction"
          label="改善アクション"
          :maxlength="NEXT_ACTION_MAX_LENGTH"
          :rows="4"
          :error="error?.fieldErrors.nextAction"
        />
        <div class="actions">
          <BaseButton type="button" variant="secondary" :disabled="submitting" @click="close">
            キャンセル
          </BaseButton>
          <BaseButton type="submit" :disabled="submitting || !canSubmit">
            {{ mode === 'create' ? '登録する' : '更新する' }}
          </BaseButton>
        </div>
      </form>
    </template>
  </BaseModal>
</template>

<style scoped>
.reference-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 0.6em 1em;
  margin: 0 0 1em;
  padding: 0.8em 1em;
  border-radius: 8px;
  background-color: var(--color-bg);
}

.reference-info div {
  display: flex;
  flex-direction: column;
  gap: 0.15em;
}

.reference-info dt {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.reference-info dd {
  margin: 0;
  font-weight: 600;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reflection-form {
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
