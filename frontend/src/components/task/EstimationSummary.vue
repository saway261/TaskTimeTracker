<script setup lang="ts">
import { computed } from 'vue'
import type { TaskResponse } from '@/types/task'
import { isFinished } from '@/utils/task'
import { formatGap, formatGapRate, formatMinutes } from '@/utils/duration'

// 完了済みタスクは cached 値（gapRateCachedは既に％）、未完了タスクは
// total-minutes から実績を受け取りフロントで誤差・誤差率を計算する（§4.5）。
const props = defineProps<{
  task: TaskResponse
  actualMinutes: number | null
}>()

const finished = computed(() => isFinished(props.task))

const actual = computed(() =>
  finished.value ? props.task.actualMinutesCached : props.actualMinutes,
)

const gap = computed(() => {
  if (finished.value) return props.task.gapMinutesCached
  if (actual.value === null || props.task.estimatedMinutes === null) return null
  return actual.value - props.task.estimatedMinutes
})

const gapRate = computed(() => {
  if (finished.value) return props.task.gapRateCached
  if (gap.value === null || !props.task.estimatedMinutes) return null
  return (gap.value / props.task.estimatedMinutes) * 100
})
</script>

<template>
  <dl class="estimation-summary">
    <div class="item">
      <dt>見積</dt>
      <dd>{{ task.estimatedMinutes !== null ? formatMinutes(task.estimatedMinutes) : '-' }}</dd>
    </div>
    <div class="item">
      <dt>実績</dt>
      <dd>{{ actual !== null ? formatMinutes(actual) : '-' }}</dd>
    </div>
    <div class="item">
      <dt>見積との差</dt>
      <dd>{{ gap !== null ? formatGap(gap) : '-' }}</dd>
    </div>
    <div class="item">
      <dt>誤差率</dt>
      <dd>{{ gapRate !== null ? formatGapRate(gapRate) : '-' }}</dd>
    </div>
  </dl>
</template>

<style scoped>
.estimation-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(8em, 1fr));
  gap: 0.8em;
  margin: 0;
  padding: 0.9em 1em;
  border-radius: 8px;
  background-color: var(--color-surface);
  border: 1px solid var(--color-surface-muted);
}

.item {
  display: flex;
  flex-direction: column;
  gap: 0.2em;
}

dt {
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

dd {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--color-text);
}
</style>
