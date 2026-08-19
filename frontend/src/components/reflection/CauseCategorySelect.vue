<script setup lang="ts">
import { computed, onMounted, ref, useId } from 'vue'
import { useCauseCategoryStore } from '@/stores/causeCategoryStore'
import type { CauseDirection } from '@/types/reflection'
import type { EstimateOutcome } from '@/utils/duration'
import { CAUSE_CATEGORY_MAX_MESSAGE } from '@/utils/validationMessages'

const MAX_SELECTABLE = 3

const props = withDefaults(
  defineProps<{
    modelValue: string[]
    outcome: EstimateOutcome
    error?: string
  }>(),
  {
    error: undefined,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const categoryStore = useCauseCategoryStore()
const groupId = useId()
const showAll = ref(false)

const directionByOutcome: Partial<Record<EstimateOutcome, CauseDirection>> = {
  late: 'OVER',
  early: 'UNDER',
}

const displayedCategories = computed(() => {
  const categories = categoryStore.categories
  if (showAll.value || props.outcome === 'unknown') return categories

  if (props.outcome === 'on-time') {
    return [
      ...categories.filter((category) => category.direction === 'BOTH'),
      ...categories.filter((category) => category.direction !== 'BOTH'),
    ]
  }

  const direction = directionByOutcome[props.outcome]
  return categories.filter(
    (category) =>
      category.direction === direction ||
      category.direction === 'BOTH' ||
      props.modelValue.includes(category.code),
  )
})

const selectedCount = computed(() => props.modelValue.length)
const atMax = computed(() => selectedCount.value >= MAX_SELECTABLE)

const selectedCategories = computed(() =>
  categoryStore.categories.filter((category) => props.modelValue.includes(category.code)),
)
const selectedHints = computed(() =>
  selectedCategories.value
    .map((category) => category.nextActionHint)
    .filter((hint): hint is string => hint !== null),
)

const errorId = `${groupId}-error`
const hintId = `${groupId}-hint`
const maxHintId = `${groupId}-max-hint`
const loadErrorId = `${groupId}-load-error`

const describedBy = computed(() => {
  if (props.error) return errorId
  if (selectedHints.value.length > 0) return hintId
  if (categoryStore.error) return loadErrorId
  return undefined
})

function isDisabled(code: string) {
  if (categoryStore.loading) return true
  return !props.modelValue.includes(code) && atMax.value
}

// 選択順は保持せず、常にマスタの表示順へ整列する（要件 §5.2。主従関係を持たせない）。
function toggle(code: string, checked: boolean) {
  const order = categoryStore.categories.map((category) => category.code)
  const next = checked
    ? [...props.modelValue.filter((c) => c !== code), code]
    : props.modelValue.filter((c) => c !== code)
  next.sort((a, b) => order.indexOf(a) - order.indexOf(b))
  emit('update:modelValue', next)
}

onMounted(() => {
  void categoryStore.fetchCategories().catch(() => {})
})
</script>

<template>
  <fieldset class="cause-category-select" :aria-describedby="describedBy">
    <legend>原因カテゴリ <span class="required">*</span></legend>

    <p class="selection-count" aria-live="polite">
      {{ categoryStore.loading ? '読み込み中…' : `${selectedCount} / ${MAX_SELECTABLE} 件選択` }}
    </p>

    <div class="options">
      <label
        v-for="category in displayedCategories"
        :key="category.code"
        class="option"
        :class="{ disabled: isDisabled(category.code) }"
      >
        <input
          type="checkbox"
          :value="category.code"
          :checked="modelValue.includes(category.code)"
          :disabled="isDisabled(category.code)"
          :aria-describedby="
            isDisabled(category.code) && !modelValue.includes(category.code) ? maxHintId : undefined
          "
          @change="toggle(category.code, ($event.target as HTMLInputElement).checked)"
        />
        <span>{{ category.label }}</span>
      </label>
    </div>

    <p v-if="atMax" :id="maxHintId" class="hint">{{ CAUSE_CATEGORY_MAX_MESSAGE }}</p>

    <label class="show-all">
      <input v-model="showAll" type="checkbox" />
      <span>すべてのカテゴリを表示</span>
    </label>

    <p v-if="error" :id="errorId" class="error" role="alert">{{ error }}</p>
    <ul v-else-if="selectedHints.length > 0" :id="hintId" class="hint-list">
      <li v-for="hint in selectedHints" :key="hint">次のアクションのヒント：{{ hint }}</li>
    </ul>
    <p v-else-if="categoryStore.error" :id="loadErrorId" class="error" role="alert">
      原因カテゴリを読み込めませんでした。ページを再読み込みしてください。
    </p>
  </fieldset>
</template>

<style scoped>
.cause-category-select {
  display: flex;
  flex-direction: column;
  gap: 0.4em;
  margin: 0;
  padding: 0;
  border: none;
}

legend {
  padding: 0;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text);
}

.required,
.error {
  color: var(--color-danger);
}

.selection-count {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.options {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4em 0.8em;
}

.option {
  display: inline-flex;
  align-items: center;
  gap: 0.35em;
  font-size: 0.9rem;
  font-weight: 400;
  color: var(--color-text);
  cursor: pointer;
}

.option.disabled {
  color: var(--color-text-muted);
  cursor: not-allowed;
}

.option input:focus-visible,
.show-all input:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 1px;
}

.option input {
  margin: 0;
}

.show-all {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  gap: 0.4em;
  font-size: 0.85rem;
  font-weight: 400;
  cursor: pointer;
}

.show-all input {
  margin: 0;
}

.hint,
.error,
.hint-list {
  margin: 0;
  font-size: 0.85rem;
}

.hint,
.hint-list {
  color: var(--color-text-muted);
}

.hint-list {
  padding-left: 1.2em;
}
</style>
