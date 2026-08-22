<script setup lang="ts">
import { computed } from 'vue'
import type { TagSummary } from '@/types/tag'

const props = withDefaults(
  defineProps<{
    tags: TagSummary[]
    limit?: number | null
    removable?: boolean
    disabled?: boolean
  }>(),
  {
    limit: 3,
    removable: false,
    disabled: false,
  },
)

const emit = defineEmits<{
  remove: [tagId: string]
}>()

const visibleTags = computed(() =>
  props.limit === null ? props.tags : props.tags.slice(0, props.limit),
)
const remainingCount = computed(() => props.tags.length - visibleTags.value.length)
</script>

<template>
  <span v-if="tags.length > 0" class="tag-badge-list" aria-label="タグ">
    <span v-for="tag in visibleTags" :key="tag.id" class="tag-badge">
      <span class="badge-label">タグ</span>
      {{ tag.name }}
      <button
        v-if="removable"
        type="button"
        class="remove-button"
        :aria-label="`${tag.name}を外す`"
        :disabled="disabled"
        @click.stop="emit('remove', tag.id)"
      >
        ×
      </button>
    </span>
    <span v-if="remainingCount > 0" class="remaining-badge">他{{ remainingCount }}件</span>
  </span>
</template>

<style scoped>
.tag-badge-list {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.3em;
  min-width: 0;
}

.tag-badge,
.remaining-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25em;
  max-width: 100%;
  padding: 0.15em 0.45em;
  border: 1px solid var(--color-text-muted);
  border-radius: 4px;
  color: var(--color-text-muted);
  font-size: 0.72rem;
  font-weight: 600;
  line-height: 1.3;
}

.remove-button {
  display: grid;
  place-items: center;
  width: 1.45em;
  height: 1.45em;
  margin: -0.1em -0.25em -0.1em 0.1em;
  padding: 0;
  border: 0;
  border-radius: 3px;
  background: transparent;
  color: var(--color-text-muted);
  font: inherit;
  line-height: 1;
  cursor: pointer;
}

.remove-button:not(:disabled):hover {
  background: var(--color-surface-muted);
  color: var(--color-text);
}

.remove-button:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 1px;
}

.remove-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.tag-badge {
  overflow-wrap: anywhere;
}

.badge-label {
  color: var(--color-text);
  font-size: 0.65rem;
}

.remaining-badge {
  flex-shrink: 0;
  border-style: dashed;
}
</style>
