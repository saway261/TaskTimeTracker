<script setup lang="ts">
import { computed } from 'vue'
import type { AnalyticsFilter, AnalyticsPeriod, DiagnosisResponse } from '@/types/analytics'

const props = defineProps<{
  diagnosis: DiagnosisResponse
  filter: AnalyticsFilter
  analyzedTaskCount: number
  projectName?: string
  tagName?: string
}>()

const periodLabels: Record<AnalyticsPeriod, string> = {
  ALL: '全期間',
  LAST_30_DAYS: '直近30日',
  LAST_90_DAYS: '直近90日',
  LAST_YEAR: '直近1年',
}

const scopeDescription = computed(() => {
  const conditions: string[] = []
  if (props.filter.tagId !== null) {
    conditions.push(`タグ「${props.tagName ?? `#${props.filter.tagId}`}」`)
  }
  if (props.filter.projectId !== null) {
    conditions.push(`プロジェクト「${props.projectName ?? `#${props.filter.projectId}`}」`)
  }
  if (props.filter.period !== 'ALL') {
    conditions.push(`期間「${periodLabels[props.filter.period]}」`)
  }
  if (conditions.length === 0) return null
  return `${conditions.join(' / ')}のタスク${props.analyzedTaskCount}件についての診断です`
})
</script>

<template>
  <section
    class="diagnosis-card"
    :class="diagnosis.biasDirection.toLowerCase()"
    aria-labelledby="diagnosis-title"
  >
    <p class="eyebrow">診断</p>
    <h2 id="diagnosis-title">{{ diagnosis.title }}</h2>
    <p class="message">{{ diagnosis.message }}</p>
    <p v-if="scopeDescription" class="scope-description">{{ scopeDescription }}</p>
  </section>
</template>

<style scoped>
.diagnosis-card {
  padding: 1em 1.2em;
  border: 1px solid var(--color-success);
  border-left-width: 4px;
  border-radius: 8px;
  background: var(--color-surface);
}

.diagnosis-card.late {
  border-color: var(--color-danger);
}

.diagnosis-card.early {
  border-color: var(--color-task-accent);
}

.eyebrow {
  margin: 0 0 0.25em;
  color: var(--color-text-muted);
  font-size: 0.75rem;
  font-weight: 700;
}

h2 {
  margin: 0;
  font-size: 1.05rem;
}

.message {
  margin: 0.55em 0 0;
  color: var(--color-text-muted);
  line-height: 1.6;
}

.scope-description {
  margin: 0.7em 0 0;
  padding-top: 0.7em;
  border-top: 1px solid var(--color-surface-muted);
  color: var(--color-text-muted);
  font-size: 0.85rem;
  line-height: 1.5;
}
</style>
