<script setup lang="ts">
import { computed, ref, useId, watch } from 'vue'
import { useTagStore } from '@/stores/tagStore'
import type { ApiError } from '@/types/apiError'
import BaseButton from '@/components/common/BaseButton.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'

const props = defineProps<{
  actionLabel: string
  retry: () => Promise<unknown>
}>()

const emit = defineEmits<{
  resolved: []
  cancel: []
}>()

const CANDIDATE_LIMIT = 5
const tagStore = useTagStore()
const headingId = useId()
const selectedTagId = ref<number | null>(null)
const resolving = ref(false)
const error = ref<ApiError | null>(null)

const candidates = computed(() =>
  [...tagStore.activeTags]
    .sort(
      (a, b) =>
        a.assignedTaskCount - b.assignedTaskCount ||
        a.name.localeCompare(b.name, 'ja') ||
        a.id - b.id,
    )
    .slice(0, CANDIDATE_LIMIT),
)

watch(candidates, (nextCandidates) => {
  if (!nextCandidates.some((tag) => tag.id === selectedTagId.value)) {
    selectedTagId.value = null
  }
})

async function resolveLimit() {
  if (selectedTagId.value === null || resolving.value) return

  resolving.value = true
  error.value = null
  try {
    await tagStore.setArchived(selectedTagId.value, true)
    await props.retry()
    emit('resolved')
  } catch (e) {
    error.value = e as ApiError
  } finally {
    resolving.value = false
  }
}
</script>

<template>
  <section
    class="tag-limit-resolver"
    :aria-labelledby="headingId"
    :aria-busy="resolving"
    @keydown.esc.stop="emit('cancel')"
  >
    <h3 :id="headingId">タグの上限に達しています</h3>
    <p>タグは50件までです。使っていないタグを1件アーカイブすると{{ actionLabel }}できます。</p>

    <ErrorMessage v-if="error" :error="error" />

    <fieldset v-if="candidates.length > 0">
      <legend>アーカイブするタグを選択</legend>
      <label v-for="tag in candidates" :key="tag.id" class="candidate">
        <input
          v-model="selectedTagId"
          type="radio"
          :name="`archive-candidate-${headingId}`"
          :value="tag.id"
        />
        <span class="candidate-name">{{ tag.name }}</span>
        <span class="candidate-count">{{ tag.assignedTaskCount }}件</span>
      </label>
    </fieldset>
    <p v-else class="empty-candidates">アーカイブできるタグがありません。</p>

    <div class="actions">
      <BaseButton type="button" variant="secondary" :disabled="resolving" @click="emit('cancel')">
        やめる
      </BaseButton>
      <BaseButton
        type="button"
        :disabled="selectedTagId === null || resolving"
        @click="resolveLimit"
      >
        {{ resolving ? '処理中…' : `アーカイブして${actionLabel}` }}
      </BaseButton>
    </div>
  </section>
</template>

<style scoped>
.tag-limit-resolver {
  display: flex;
  flex-direction: column;
  gap: 0.8em;
  padding: 1em;
  border: 1px solid var(--color-accent);
  border-radius: 8px;
  background: var(--color-surface);
}

h3,
p,
fieldset {
  margin: 0;
}

h3 {
  font-size: 1rem;
}

p,
legend {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  line-height: 1.55;
}

fieldset {
  display: flex;
  flex-direction: column;
  gap: 0.35em;
  padding: 0;
  border: 0;
}

legend {
  margin-bottom: 0.35em;
  font-weight: 600;
}

.candidate {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.65em;
  min-height: 42px;
  padding: 0.45em 0.65em;
  border-radius: 6px;
  cursor: pointer;
}

.candidate:hover {
  background: var(--color-surface-muted);
}

.candidate input:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.candidate-name {
  overflow-wrap: anywhere;
}

.candidate-count {
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
}

.empty-candidates {
  color: var(--color-danger);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.6em;
  flex-wrap: wrap;
}
</style>
