<script setup lang="ts">
import { computed, nextTick, onMounted, ref, useId, watch } from 'vue'
import { useTagStore } from '@/stores/tagStore'
import { normalizeTagName } from '@/utils/tagName'
import type { ApiError } from '@/types/apiError'
import type { TagResponse, TagSummary } from '@/types/tag'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import TagLimitResolver from '@/components/tag/TagLimitResolver.vue'

const props = withDefaults(
  defineProps<{
    modelValue: TagSummary[]
    error?: string
    disabled?: boolean
    showSelected?: boolean
    label?: string
  }>(),
  {
    error: undefined,
    disabled: false,
    showSelected: true,
    label: 'タグ',
  },
)

const emit = defineEmits<{
  'update:modelValue': [tags: TagSummary[]]
}>()

const tagStore = useTagStore()
const root = ref<HTMLElement | null>(null)
const input = ref<HTMLInputElement | null>(null)
const query = ref('')
const suggestionsOpen = ref(false)
const refreshingSuggestions = ref(false)
const activeIndex = ref(-1)
const creating = ref(false)
const createError = ref<ApiError | null>(null)
const limitResolverOpen = ref(false)
const listboxId = useId()
const inputId = useId()

const selectedIds = computed(() => new Set(props.modelValue.map((tag) => tag.id)))
const normalizedQuery = computed(() => normalizeTagName(query.value))
const availableTags = computed(() =>
  tagStore.activeTags.filter((tag) => !selectedIds.value.has(tag.id)),
)
const exactExistingTag = computed(() => {
  if (normalizedQuery.value === '') return null
  return tagStore.tags.find((tag) => normalizeTagName(tag.name) === normalizedQuery.value) ?? null
})
const matchingTags = computed(() => {
  if (normalizedQuery.value === '') return availableTags.value

  const matches = availableTags.value.filter((tag) =>
    normalizeTagName(tag.name).includes(normalizedQuery.value),
  )
  const exact = matches.find((tag) => normalizeTagName(tag.name) === normalizedQuery.value)
  return exact ? [exact, ...matches.filter((tag) => tag.id !== exact.id)] : matches
})
const canCreate = computed(
  () =>
    query.value.trim() !== '' &&
    exactExistingTag.value === null &&
    !props.disabled &&
    !creating.value,
)
const optionCount = computed(() => matchingTags.value.length + (canCreate.value ? 1 : 0))
const activeOptionId = computed(() =>
  activeIndex.value < 0 ? undefined : `${listboxId}-option-${activeIndex.value}`,
)

watch([matchingTags, canCreate], () => {
  activeIndex.value = -1
})

function isArchived(tagId: string) {
  return tagStore.tags.find((tag) => tag.id === tagId)?.isArchived === true
}

function selectTag(tag: TagResponse | TagSummary) {
  if (!selectedIds.value.has(tag.id)) {
    emit('update:modelValue', [...props.modelValue, { id: tag.id, name: tag.name }])
  }
  query.value = ''
  suggestionsOpen.value = false
  activeIndex.value = -1
  createError.value = null
}

function removeTag(tagId: string) {
  emit(
    'update:modelValue',
    props.modelValue.filter((tag) => tag.id !== tagId),
  )
}

function isTagLimitError(error: ApiError) {
  return error.status === 400 && error.fieldErrors.tagLimit !== undefined
}

async function createAndSelect() {
  if (query.value.trim() === '') return

  creating.value = true
  createError.value = null
  try {
    const tag = await tagStore.createTag(query.value)
    selectTag(tag)
  } finally {
    creating.value = false
  }
}

async function handleCreate() {
  try {
    await createAndSelect()
  } catch (e) {
    const error = e as ApiError
    if (isTagLimitError(error)) {
      createError.value = null
      limitResolverOpen.value = true
    } else {
      createError.value = error
    }
  }
}

async function refreshSuggestions() {
  if (refreshingSuggestions.value) return
  refreshingSuggestions.value = true
  try {
    await tagStore.fetchTags(true)
  } catch {
    // 取得済みの候補はそのまま利用し、タグ作成・タスク入力を妨げない。
  } finally {
    refreshingSuggestions.value = false
  }
}

function openSuggestions() {
  if (props.disabled || limitResolverOpen.value) return
  const shouldRefresh = !suggestionsOpen.value
  suggestionsOpen.value = true
  if (shouldRefresh) {
    void refreshSuggestions()
  }
}

function handleInput() {
  activeIndex.value = -1
  createError.value = null
  openSuggestions()
}

function moveActive(delta: number) {
  if (!suggestionsOpen.value) openSuggestions()
  if (optionCount.value === 0) return
  activeIndex.value =
    activeIndex.value < 0
      ? delta > 0
        ? 0
        : optionCount.value - 1
      : (activeIndex.value + delta + optionCount.value) % optionCount.value
}

function chooseActive() {
  if (!suggestionsOpen.value) {
    openSuggestions()
    return
  }
  if (activeIndex.value < 0) return
  if (activeIndex.value < matchingTags.value.length) {
    selectTag(matchingTags.value[activeIndex.value])
  } else if (canCreate.value) {
    void handleCreate()
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && limitResolverOpen.value) {
    event.preventDefault()
    event.stopPropagation()
    void closeLimitResolver()
  } else if (event.key === 'ArrowDown') {
    event.preventDefault()
    moveActive(1)
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    moveActive(-1)
  } else if (event.key === 'Enter' && suggestionsOpen.value) {
    event.preventDefault()
    chooseActive()
  } else if (event.key === 'Escape' && suggestionsOpen.value) {
    event.preventDefault()
    event.stopPropagation()
    suggestionsOpen.value = false
    activeIndex.value = -1
  }
}

function handleRootEscape(event: KeyboardEvent) {
  if (!limitResolverOpen.value) return
  event.preventDefault()
  event.stopPropagation()
  void closeLimitResolver()
}

function handleFocusout() {
  setTimeout(() => {
    if (!root.value?.contains(document.activeElement) && !limitResolverOpen.value) {
      suggestionsOpen.value = false
      activeIndex.value = -1
    }
  }, 0)
}

async function closeLimitResolver() {
  limitResolverOpen.value = false
  await nextTick()
  input.value?.focus()
}

onMounted(() => {
  void tagStore.fetchTags().catch(() => {})
})
</script>

<template>
  <div ref="root" class="tag-select" @focusout="handleFocusout" @keydown.esc="handleRootEscape">
    <label :for="inputId">{{ label }}</label>

    <div
      v-if="showSelected && modelValue.length > 0"
      class="selected-tags"
      aria-label="付与済みタグ"
    >
      <span v-for="tag in modelValue" :key="tag.id" class="selected-tag">
        <span>{{ tag.name }}</span>
        <span v-if="isArchived(tag.id)" class="archived-mark">アーカイブ済み</span>
        <button
          type="button"
          :aria-label="`${tag.name}を外す`"
          :disabled="disabled"
          @click="removeTag(tag.id)"
        >
          ×
        </button>
      </span>
    </div>

    <div class="combobox-wrap">
      <input
        :id="inputId"
        ref="input"
        v-model="query"
        type="text"
        autocomplete="off"
        role="combobox"
        aria-autocomplete="list"
        :aria-controls="listboxId"
        :aria-expanded="suggestionsOpen && !limitResolverOpen"
        :aria-activedescendant="activeOptionId"
        :aria-invalid="!!error"
        :disabled="disabled"
        placeholder="タグ名を入力"
        @focus="openSuggestions"
        @input="handleInput"
        @keydown="handleKeydown"
      />

      <div
        v-if="suggestionsOpen && !limitResolverOpen"
        :id="listboxId"
        class="suggestions"
        role="listbox"
      >
        <button
          v-for="(tag, index) in matchingTags"
          :id="`${listboxId}-option-${index}`"
          :key="tag.id"
          type="button"
          class="suggestion"
          :class="{ active: activeIndex === index }"
          role="option"
          :aria-selected="activeIndex === index"
          @mousedown.prevent
          @click="selectTag(tag)"
        >
          <span>{{ tag.name }}</span>
          <span class="assigned-count">{{ tag.assignedTaskCount }}件</span>
        </button>
        <button
          v-if="canCreate"
          :id="`${listboxId}-option-${matchingTags.length}`"
          type="button"
          class="suggestion create-option"
          :class="{ active: activeIndex === matchingTags.length }"
          role="option"
          :aria-selected="activeIndex === matchingTags.length"
          @mousedown.prevent
          @click="handleCreate"
        >
          ＋「{{ query.trim() }}」を新しいタグとして作成
        </button>
        <p v-if="optionCount === 0" class="no-options">候補がありません。</p>
      </div>
    </div>

    <p v-if="error" class="field-error" role="alert">{{ error }}</p>
    <ErrorMessage v-if="createError" :error="createError" />

    <TagLimitResolver
      v-if="limitResolverOpen"
      action-label="作成"
      :retry="createAndSelect"
      @resolved="closeLimitResolver"
      @cancel="closeLimitResolver"
    />
  </div>
</template>

<style scoped>
.tag-select {
  display: flex;
  flex-direction: column;
  gap: 0.45em;
}

label {
  color: var(--color-text);
  font-size: 0.9rem;
  font-weight: 600;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35em;
}

.selected-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.35em;
  max-width: 100%;
  padding: 0.25em 0.35em 0.25em 0.55em;
  border: 1px solid var(--color-text-muted);
  border-radius: 4px;
  color: var(--color-text);
  font-size: 0.82rem;
  overflow-wrap: anywhere;
}

.selected-tag button {
  display: grid;
  place-items: center;
  width: 1.6em;
  height: 1.6em;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
}

.selected-tag button:hover {
  background: var(--color-surface-muted);
  color: var(--color-text);
}

.selected-tag button:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 1px;
}

.archived-mark {
  color: var(--color-text-muted);
  font-size: 0.68rem;
}

.combobox-wrap {
  position: relative;
}

.combobox-wrap > input {
  width: 100%;
  padding: 0.5em 0.7em;
  border: 1px solid var(--color-surface-muted);
  border-radius: 6px;
  background: var(--color-surface);
  color: var(--color-text);
  font: inherit;
}

.combobox-wrap > input:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 1px;
}

.combobox-wrap > input[aria-invalid='true'] {
  border-color: var(--color-danger);
}

.suggestions {
  position: absolute;
  top: calc(100% + 0.3em);
  right: 0;
  left: 0;
  z-index: 20;
  max-height: 15em;
  overflow-y: auto;
  padding: 0.35em;
  border: 1px solid var(--color-surface-muted);
  border-radius: 8px;
  background: var(--color-surface);
  box-shadow: 0 6px 18px rgb(0 0 0 / 16%);
}

.suggestion {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8em;
  min-height: 40px;
  padding: 0.45em 0.65em;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: var(--color-text);
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.suggestion:hover,
.suggestion.active {
  background: var(--color-surface-muted);
}

.suggestion:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: -2px;
}

.assigned-count {
  flex-shrink: 0;
  color: var(--color-text-muted);
  font-size: 0.8rem;
  font-variant-numeric: tabular-nums;
}

.create-option {
  color: var(--color-accent);
  font-weight: 600;
}

.no-options,
.field-error {
  margin: 0;
  font-size: 0.85rem;
}

.no-options {
  padding: 0.6em;
  color: var(--color-text-muted);
}

.field-error {
  color: var(--color-danger);
}
</style>
