<script setup lang="ts">
import { computed } from 'vue'
import type { AnalyticsOutcome, ScatterPointResponse } from '@/types/analytics'
import { formatGap, formatGapRate, formatMinutes } from '@/utils/duration'
import BaseModal from '@/components/common/BaseModal.vue'
import EstimateOutcomeIcon from '@/components/common/EstimateOutcomeIcon.vue'

const props = defineProps<{
  modelValue: boolean
  point: ScatterPointResponse | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const outcomeLabels: Record<AnalyticsOutcome, string> = {
  LATE: '超過',
  ON_TIME: 'おおむね見積どおり',
  EARLY: '短縮',
}

const gapMinutes = computed(() =>
  props.point ? props.point.actualMinutes - props.point.estimatedMinutes : 0,
)
</script>

<template>
  <BaseModal
    :model-value="modelValue"
    :title="point?.taskTitle ?? 'タスク詳細'"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-if="point" class="scatter-point-detail">
      <p class="outcome" :class="point.outcome.toLowerCase().replace('_', '-')">
        <EstimateOutcomeIcon :gap-rate="point.gapRate" />
        <strong>{{ outcomeLabels[point.outcome] }}</strong>
      </p>
      <dl>
        <div>
          <dt>見積時間</dt>
          <dd>{{ formatMinutes(point.estimatedMinutes) }}</dd>
        </div>
        <div>
          <dt>実績時間</dt>
          <dd>{{ formatMinutes(point.actualMinutes) }}</dd>
        </div>
        <div>
          <dt>誤差</dt>
          <dd>{{ formatGap(gapMinutes) }}</dd>
        </div>
        <div>
          <dt>誤差比</dt>
          <dd>{{ formatGapRate(point.gapRate) }}</dd>
        </div>
      </dl>
      <p class="reflection-note">振り返りの内容は、この画面のタイムラインで確認できます。</p>
    </div>
  </BaseModal>
</template>

<style scoped>
.scatter-point-detail {
  display: flex;
  flex-direction: column;
  gap: 1em;
}

.outcome {
  display: flex;
  align-items: center;
  gap: 0.45em;
  margin: 0;
  color: var(--color-text);
}

.outcome.late {
  color: var(--color-danger);
}

.outcome.on-time {
  color: var(--color-success);
}

.outcome.early {
  color: var(--color-task-accent);
}

dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8em;
  margin: 0;
}

dl div {
  padding: 0.7em;
  border-radius: 6px;
  background: var(--color-surface-muted);
}

dt {
  color: var(--color-text-muted);
  font-size: 0.78rem;
}

dd {
  margin: 0.25em 0 0;
  color: var(--color-text);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.reflection-note {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}
</style>
