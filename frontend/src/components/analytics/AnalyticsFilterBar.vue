<script setup lang="ts">
import { useId } from 'vue'
import type {
  AnalyticsFilter,
  AnalyticsPeriod,
  EstimationAccuracyResponse,
} from '@/types/analytics'
import type { ProjectResponse } from '@/types/project'
import type { TagSummary } from '@/types/tag'

defineProps<{
  filter: AnalyticsFilter
  projects: ProjectResponse[]
  tags: TagSummary[]
  accuracy: EstimationAccuracyResponse | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  projectChange: [projectId: number | null]
  periodChange: [period: AnalyticsPeriod]
  tagChange: [tagId: number | null]
}>()

const projectId = useId()
const periodId = useId()
const tagId = useId()

function handleProjectChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  emit('projectChange', value === '' ? null : Number(value))
}

function handlePeriodChange(event: Event) {
  emit('periodChange', (event.target as HTMLSelectElement).value as AnalyticsPeriod)
}

function handleTagChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  emit('tagChange', value === '' ? null : Number(value))
}
</script>

<template>
  <section class="analytics-filter-bar" aria-label="分析条件">
    <div class="filters">
      <div class="filter-field">
        <label :for="projectId">プロジェクト</label>
        <select
          :id="projectId"
          :value="filter.projectId ?? ''"
          :disabled="disabled"
          @change="handleProjectChange"
        >
          <option value="">すべてのプロジェクト</option>
          <option v-for="project in projects" :key="project.id" :value="project.id">
            {{ project.title }}
          </option>
        </select>
      </div>

      <div class="filter-field">
        <label :for="periodId">期間</label>
        <select
          :id="periodId"
          :value="filter.period"
          :disabled="disabled"
          @change="handlePeriodChange"
        >
          <option value="ALL">全期間</option>
          <option value="LAST_30_DAYS">直近30日</option>
          <option value="LAST_90_DAYS">直近90日</option>
          <option value="LAST_YEAR">直近1年</option>
        </select>
      </div>

      <div class="filter-field">
        <label :for="tagId">タグ</label>
        <select
          :id="tagId"
          :value="filter.tagId ?? ''"
          :disabled="disabled"
          @change="handleTagChange"
        >
          <option value="">すべてのタグ</option>
          <option v-for="tag in tags" :key="tag.id" :value="tag.id">
            {{ tag.name }}
          </option>
        </select>
      </div>
    </div>

    <div class="counts" aria-live="polite">
      <span
        >分析対象 <strong>{{ accuracy?.analyzedTaskCount ?? '-' }}件</strong></span
      >
      <span
        >除外 <strong>{{ accuracy?.excluded.total ?? '-' }}件</strong></span
      >
      <details v-if="accuracy && accuracy.excluded.total > 0">
        <summary>除外理由</summary>
        <ul>
          <li>誤差率を算出できない: {{ accuracy.excluded.missingGapRate }}件</li>
          <li>実績時間が記録されていない: {{ accuracy.excluded.missingActualMinutes }}件</li>
        </ul>
        <p>同じタスクが複数の理由に該当する場合があります。</p>
      </details>
    </div>
  </section>
</template>

<style scoped>
.analytics-filter-bar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1em;
  padding: 1em;
  border: 1px solid var(--color-surface-muted);
  border-radius: 8px;
  background: var(--color-surface);
}

.filters,
.counts {
  display: flex;
  align-items: flex-end;
  gap: 0.8em 1.2em;
  flex-wrap: wrap;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 0.3em;
  min-width: min(15em, 100%);
}

label {
  color: var(--color-text);
  font-size: 0.85rem;
  font-weight: 600;
}

select {
  max-width: 100%;
  min-height: 40px;
  padding: 0.45em 0.65em;
  border: 1px solid var(--color-surface-muted);
  border-radius: 6px;
  background: var(--color-surface);
  color: var(--color-text);
  font: inherit;
}

select:focus-visible,
summary:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.counts {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.counts strong {
  color: var(--color-text);
  font-variant-numeric: tabular-nums;
}

details {
  position: relative;
}

summary {
  color: var(--color-accent);
  cursor: pointer;
  font-weight: 600;
}

details ul {
  margin: 0.5em 0 0;
  padding-left: 1.2em;
}

details p {
  max-width: 28em;
  margin: 0.4em 0 0;
  font-size: 0.8rem;
}

@media (max-width: 760px) {
  .analytics-filter-bar,
  .filters {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-field,
  select {
    width: 100%;
  }
}
</style>
