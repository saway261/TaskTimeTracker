<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import BaseModal from '@/components/common/BaseModal.vue'
import BaseTextarea from '@/components/common/BaseTextarea.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import CauseCategorySelect from '@/components/reflection/CauseCategorySelect.vue'
import { useCauseCategoryStore } from '@/stores/causeCategoryStore'
import { useAppSettingsStore } from '@/stores/appSettingsStore'
import type { ReflectionRequest, ReflectionTaskResponse } from '@/types/reflection'
import type { ApiError } from '@/types/apiError'
import { estimateOutcome, formatGap, formatGapRate, formatMinutes } from '@/utils/duration'
import EstimateOutcomeIcon from '@/components/common/EstimateOutcomeIcon.vue'
import { CAUSE_CATEGORY_REQUIRED_MESSAGE } from '@/utils/validationMessages'

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

const categoryStore = useCauseCategoryStore()
const appSettingsStore = useAppSettingsStore()

const cause = ref('')
const nextAction = ref('')
const causeCategoryCodes = ref<string[]>([])
const causeCategoryTouched = ref(false)

// 開くたびに対象タスクの現在値へ合わせる。全文をそのまま入れるため、一覧のプレビュー省略は経由しない。
watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    causeCategoryCodes.value = props.task?.reflection?.causeCategories.map((c) => c.code) ?? []
    causeCategoryTouched.value = false
    cause.value = props.task?.reflection?.cause ?? ''
    nextAction.value = props.task?.reflection?.nextAction ?? ''
  },
  { immediate: true },
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
const outcome = computed(() =>
  estimateOutcome(props.task?.gapRateCached, appSettingsStore.onTimeThresholdPercent),
)
const causeCategoryError = computed(() => {
  if (props.error?.fieldErrors.causeCategoryCodes) {
    return props.error.fieldErrors.causeCategoryCodes
  }
  return causeCategoryTouched.value && causeCategoryCodes.value.length === 0
    ? CAUSE_CATEGORY_REQUIRED_MESSAGE
    : undefined
})

// 選択したカテゴリのいずれかが原因の記述を必須とするか（§5.2）。コードではなくカテゴリの属性で判定する。
const selectedCategories = computed(() =>
  categoryStore.categories.filter((category) => causeCategoryCodes.value.includes(category.code)),
)
const causeRequired = computed(() =>
  selectedCategories.value.some((category) => category.requiresCause),
)
const causeRequiredHint = computed(() => {
  const labels = selectedCategories.value
    .filter((category) => category.requiresCause)
    .map((category) => category.label)
  return labels.length === 0
    ? null
    : `「${labels.join('」「')}」を選んだ場合は、原因の記述が必要です。`
})

// バックエンドと同条件の事前検証（§10）。文字数上限はBaseTextareaのmaxlengthでも防いでいる。
const canSubmit = computed(
  () =>
    causeCategoryCodes.value.length >= 1 &&
    causeCategoryCodes.value.length <= 3 &&
    (!causeRequired.value || cause.value.trim() !== '') &&
    cause.value.length <= CAUSE_MAX_LENGTH &&
    nextAction.value.length <= NEXT_ACTION_MAX_LENGTH,
)

function handleSubmit() {
  causeCategoryTouched.value = true
  if (!canSubmit.value) return
  emit('submit', {
    causeCategoryCodes: causeCategoryCodes.value,
    cause: cause.value.trim() === '' ? null : cause.value.trim(),
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
        <div class="outcome-reference" :class="outcome">
          <dt>誤差</dt>
          <dd class="outcome-value">
            <EstimateOutcomeIcon :gap-rate="task.gapRateCached" />
            <span>{{ gapText }}</span>
          </dd>
        </div>
        <div class="outcome-reference" :class="outcome">
          <dt>誤差比</dt>
          <dd>{{ gapRateText }}</dd>
        </div>
      </dl>

      <form class="reflection-form" @submit.prevent="handleSubmit">
        <ErrorMessage v-if="error" :error="error" />
        <CauseCategorySelect
          v-model="causeCategoryCodes"
          :outcome="outcome"
          :error="causeCategoryError"
          @focusout="causeCategoryTouched = true"
        />
        <div class="cause-field">
          <BaseTextarea
            v-model="cause"
            label="原因"
            :required="causeRequired"
            :maxlength="CAUSE_MAX_LENGTH"
            :rows="3"
            :error="error?.fieldErrors.cause"
          />
          <p v-if="causeRequiredHint" class="cause-required-hint">{{ causeRequiredHint }}</p>
        </div>
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

.outcome-value {
  display: flex;
  align-items: center;
  gap: 0.35em;
}

.outcome-reference.early dd {
  color: var(--color-task-accent);
}

.outcome-reference.late dd {
  color: var(--color-danger);
}

.outcome-reference.on-time dd {
  color: var(--color-success);
}

.reflection-form {
  display: flex;
  flex-direction: column;
  gap: 1em;
}

.cause-field {
  display: flex;
  flex-direction: column;
  gap: 0.3em;
}

.cause-required-hint {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.6em;
}
</style>
