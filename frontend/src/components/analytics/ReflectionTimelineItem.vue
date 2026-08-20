<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import type { AnalyticsOutcome, ReflectionTimelineItemResponse } from '@/types/analytics'
import { formatGap, formatGapRate, formatMinutes } from '@/utils/duration'
import EstimateOutcomeIcon from '@/components/common/EstimateOutcomeIcon.vue'

const props = defineProps<{
  item: ReflectionTimelineItemResponse
}>()

const outcomeLabels: Record<AnalyticsOutcome, string> = {
  LATE: '超過',
  ON_TIME: 'おおむね見積どおり',
  EARLY: '短縮',
}

const outcomeClass = computed(
  () => props.item.outcome?.toLowerCase().replace('_', '-') ?? 'unknown',
)
const finishedAtText = computed(() =>
  new Intl.DateTimeFormat('ja-JP', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(props.item.finishedAt)),
)
const taskPath = computed(() => `/projects/${props.item.projectId}/tasks/${props.item.taskId}`)
</script>

<template>
  <article class="timeline-item">
    <header>
      <div class="item-heading">
        <p class="project-title">{{ item.projectTitle }}</p>
        <h3>
          <RouterLink :to="taskPath">{{ item.taskTitle }}</RouterLink>
        </h3>
      </div>
      <time :datetime="item.finishedAt">{{ finishedAtText }}</time>
    </header>

    <div class="outcome-row">
      <span v-if="item.outcome" class="outcome-badge" :class="outcomeClass">
        <EstimateOutcomeIcon :gap-rate="item.gapRate" size="small" />
        {{ outcomeLabels[item.outcome] }}
      </span>
      <span v-else class="outcome-badge unknown" aria-label="判定なし">-</span>
      <div v-if="item.causeCategories.length > 0" class="category-badges">
        <span v-for="category in item.causeCategories" :key="category.code" class="category-badge">
          {{ category.label }}
        </span>
      </div>
    </div>

    <dl class="task-metrics">
      <div>
        <dt>見積</dt>
        <dd>{{ formatMinutes(item.estimatedMinutes) }}</dd>
      </div>
      <div>
        <dt>実績</dt>
        <dd>{{ item.actualMinutes === null ? '-' : formatMinutes(item.actualMinutes) }}</dd>
      </div>
      <div>
        <dt>誤差</dt>
        <dd>{{ item.gapMinutes === null ? '-' : formatGap(item.gapMinutes) }}</dd>
      </div>
      <div>
        <dt>誤差比</dt>
        <dd>{{ item.gapRate === null ? '-' : formatGapRate(item.gapRate) }}</dd>
      </div>
    </dl>

    <div class="reflection-copy">
      <section>
        <h4>原因</h4>
        <p v-if="item.cause" class="preserve-lines">{{ item.cause }}</p>
        <p v-else class="not-recorded">原因の記述はありません。</p>
      </section>
      <section v-if="item.nextAction">
        <h4>改善アクション</h4>
        <p class="preserve-lines">{{ item.nextAction }}</p>
      </section>
    </div>
  </article>
</template>

<style scoped>
.timeline-item {
  min-width: 0;
  padding: 1em;
  border: 1px solid var(--color-surface-muted);
  border-left: 3px solid var(--color-accent);
  border-radius: 8px;
  background: var(--color-surface);
}

header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1em;
}

.item-heading {
  min-width: 0;
}

.project-title {
  margin: 0 0 0.2em;
  color: var(--color-text-muted);
  font-size: 0.75rem;
}

h3 {
  margin: 0;
  font-size: 1rem;
}

h3 a {
  color: var(--color-text);
  text-decoration-color: var(--color-accent);
  text-underline-offset: 0.2em;
}

h3 a:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

time {
  flex-shrink: 0;
  color: var(--color-text-muted);
  font-size: 0.78rem;
}

.outcome-row,
.category-badges {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.35em;
}

.outcome-row {
  margin-top: 0.7em;
}

.outcome-badge,
.category-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.3em;
  padding: 0.2em 0.6em;
  border-radius: 999px;
  background: var(--color-surface-muted);
  color: var(--color-text);
  font-size: 0.75rem;
  font-weight: 600;
}

.outcome-badge.late {
  color: var(--color-danger);
}

.outcome-badge.early {
  color: var(--color-task-accent);
}

.outcome-badge.on-time {
  color: var(--color-success);
}

.outcome-badge.unknown {
  color: var(--color-text-muted);
}

.task-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.7em;
  margin: 0.8em 0 0;
  padding: 0.65em 0;
  border-block: 1px solid var(--color-surface-muted);
}

.task-metrics div {
  min-width: 0;
}

dt {
  color: var(--color-text-muted);
  font-size: 0.72rem;
}

dd {
  margin: 0.2em 0 0;
  color: var(--color-text);
  font-size: 0.85rem;
  font-variant-numeric: tabular-nums;
  overflow-wrap: anywhere;
}

.reflection-copy {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1em;
  margin-top: 0.8em;
}

h4 {
  margin: 0 0 0.3em;
  color: var(--color-text-muted);
  font-size: 0.78rem;
}

.reflection-copy p {
  margin: 0;
  color: var(--color-text);
  font-size: 0.9rem;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.preserve-lines {
  white-space: pre-wrap;
}

.reflection-copy .not-recorded {
  color: var(--color-text-muted);
  font-style: italic;
}

@media (max-width: 640px) {
  header {
    flex-direction: column;
    gap: 0.35em;
  }

  .task-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .reflection-copy {
    grid-template-columns: 1fr;
  }
}
</style>
