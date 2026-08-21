<script setup lang="ts">
import { useId } from 'vue'
import { RouterLink } from 'vue-router'
import type { ReflectionOutcomeFilter, ReflectionTimelineResponse } from '@/types/analytics'
import type { ReflectionCauseCategoryResponse } from '@/types/reflection'
import BaseButton from '@/components/common/BaseButton.vue'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import ReflectionTimelineItem from './ReflectionTimelineItem.vue'

defineProps<{
  timeline: ReflectionTimelineResponse | null
  categories: ReflectionCauseCategoryResponse[]
  causeCategory: string | null
  outcome: ReflectionOutcomeFilter
  loading: boolean
}>()

const emit = defineEmits<{
  causeCategoryChange: [causeCategory: string | null]
  outcomeChange: [outcome: ReflectionOutcomeFilter]
  loadMore: []
}>()

const categoryId = useId()
const outcomeId = useId()

function handleCategoryChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  emit('causeCategoryChange', value === '' ? null : value)
}

function handleOutcomeChange(event: Event) {
  emit('outcomeChange', (event.target as HTMLSelectElement).value as ReflectionOutcomeFilter)
}
</script>

<template>
  <section class="reflection-timeline" aria-labelledby="reflection-timeline-title">
    <div class="section-heading">
      <div>
        <h2 id="reflection-timeline-title">振り返りタイムライン</h2>
        <p>完了日時が新しい順に、振り返りの全文を表示しています。</p>
      </div>
      <RouterLink to="/reflections" class="edit-link">振り返り画面で編集する</RouterLink>
    </div>

    <div class="timeline-filters">
      <div class="filter-field">
        <label :for="categoryId">原因カテゴリ</label>
        <select
          :id="categoryId"
          :value="causeCategory ?? ''"
          :disabled="loading"
          @change="handleCategoryChange"
        >
          <option value="">すべて</option>
          <option v-for="category in categories" :key="category.code" :value="category.code">
            {{ category.label }}
          </option>
        </select>
      </div>
      <div class="filter-field">
        <label :for="outcomeId">判定区分</label>
        <select :id="outcomeId" :value="outcome" :disabled="loading" @change="handleOutcomeChange">
          <option value="ALL">すべて</option>
          <option value="LATE">超過</option>
          <option value="ON_TIME">おおむね見積どおり</option>
          <option value="EARLY">短縮</option>
        </select>
      </div>
      <span v-if="timeline" class="total-count">{{ timeline.totalCount }}件</span>
    </div>

    <LoadingIndicator v-if="loading && !timeline" />
    <p v-else-if="timeline && timeline.items.length === 0" class="empty">
      条件に一致する振り返りはありません。
    </p>
    <div v-else-if="timeline" class="timeline-items">
      <ReflectionTimelineItem v-for="item in timeline.items" :key="item.taskId" :item="item" />
    </div>

    <div v-if="timeline?.hasNext" class="load-more">
      <BaseButton :disabled="loading" @click="emit('loadMore')">
        {{ loading ? '読み込み中…' : 'さらに読み込む' }}
      </BaseButton>
    </div>
  </section>
</template>

<style scoped>
.reflection-timeline {
  display: flex;
  flex-direction: column;
  gap: 0.8em;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1em;
}

h2 {
  margin: 0;
  font-size: 1.15rem;
}

.section-heading p {
  margin: 0.3em 0 0;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.edit-link {
  flex-shrink: 0;
  color: var(--color-accent);
  font-size: 0.85rem;
  font-weight: 600;
}

.edit-link:focus-visible,
select:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.timeline-filters {
  display: flex;
  align-items: flex-end;
  gap: 0.8em;
  flex-wrap: wrap;
  padding: 0.8em;
  border-radius: 8px;
  background: var(--color-surface-muted);
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 0.25em;
}

label {
  color: var(--color-text-muted);
  font-size: 0.78rem;
  font-weight: 600;
}

select {
  max-width: 100%;
  min-height: 38px;
  padding: 0.4em 0.6em;
  border: 1px solid var(--color-surface-muted);
  border-radius: 6px;
  background: var(--color-surface);
  color: var(--color-text);
  font: inherit;
}

.total-count {
  margin-left: auto;
  color: var(--color-text-muted);
  font-size: 0.85rem;
  font-variant-numeric: tabular-nums;
}

.timeline-items {
  display: flex;
  flex-direction: column;
  gap: 0.7em;
}

.empty {
  margin: 0;
  padding: 1em;
  border: 1px dashed var(--color-surface-muted);
  border-radius: 8px;
  color: var(--color-text-muted);
  text-align: center;
}

.load-more {
  display: flex;
  justify-content: center;
}

@media (max-width: 640px) {
  .section-heading {
    flex-direction: column;
  }

  .timeline-filters,
  .filter-field,
  select {
    width: 100%;
  }

  .total-count {
    margin-left: 0;
  }
}
</style>
