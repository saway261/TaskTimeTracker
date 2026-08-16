<script setup lang="ts">
import { computed } from 'vue'
import type { ReflectionTaskResponse } from '@/types/reflection'
import { estimateOutcome, formatGap, formatGapRate, formatMinutes } from '@/utils/duration'
import { aggregateReflectionMetrics } from '@/utils/reflectionMetrics'
import EstimateOutcomeIcon from '@/components/common/EstimateOutcomeIcon.vue'

const props = withDefaults(
  defineProps<{
    tasks: ReflectionTaskResponse[]
    showActual?: boolean
    prominent?: boolean
  }>(),
  {
    showActual: false,
    prominent: false,
  },
)

const metrics = computed(() => aggregateReflectionMetrics(props.tasks))
const actualText = computed(() =>
  metrics.value.actualMinutes === null ? '-' : formatMinutes(metrics.value.actualMinutes),
)
const gapText = computed(() =>
  metrics.value.gapMinutes === null ? '-' : formatGap(metrics.value.gapMinutes),
)
const gapRateText = computed(() =>
  metrics.value.gapRate === null ? '-' : formatGapRate(metrics.value.gapRate),
)
const outcome = computed(() => estimateOutcome(metrics.value.gapRate))
</script>

<template>
  <div class="aggregate-summary" :class="{ prominent }">
    <p class="scope">完了タスク{{ tasks.length }}件の合計</p>
    <dl class="metrics">
      <div v-if="showActual" class="metric">
        <dt>合計実績</dt>
        <dd>{{ actualText }}</dd>
      </div>
      <div class="metric outcome-metric" :class="outcome">
        <dt>合計誤差</dt>
        <dd class="outcome-value">
          <EstimateOutcomeIcon :gap-rate="metrics.gapRate" />
          <span>{{ gapText }}</span>
        </dd>
      </div>
      <div class="metric outcome-metric" :class="outcome">
        <dt>合計誤差比</dt>
        <dd>{{ gapRateText }}</dd>
      </div>
    </dl>
    <p v-if="metrics.excludedTaskCount > 0" class="note">
      集計値がない{{ metrics.excludedTaskCount }}件は除外しています。
    </p>
  </div>
</template>

<style scoped>
.aggregate-summary {
  display: flex;
  flex-direction: column;
  gap: 0.65em;
  padding: 0.8em 0.9em;
  border: 1px solid var(--color-surface-muted);
  border-radius: 8px;
  background-color: var(--color-surface);
}

.aggregate-summary.prominent {
  padding: 1em 1.1em;
  border-color: var(--color-accent);
}

.scope {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.8rem;
  font-weight: 600;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(8em, 1fr));
  gap: 0.8em;
  margin: 0;
}

.metric {
  display: flex;
  flex-direction: column;
  gap: 0.2em;
}

dt {
  color: var(--color-text-muted);
  font-size: 0.8rem;
}

dd {
  margin: 0;
  color: var(--color-text);
  font-size: 1rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.prominent dd {
  color: var(--color-accent);
  font-size: 1.15rem;
}

.outcome-value {
  display: flex;
  align-items: center;
  gap: 0.4em;
}

.outcome-metric.early dd {
  color: var(--color-task-accent);
}

.outcome-metric.late dd {
  color: var(--color-danger);
}

.outcome-metric.on-time dd {
  color: var(--color-success);
}

.note {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.75rem;
}
</style>
