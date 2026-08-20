<script setup lang="ts">
import { computed } from 'vue'
import { estimateOutcome } from '@/utils/duration'
import { useAppSettingsStore } from '@/stores/appSettingsStore'

const props = withDefaults(
  defineProps<{
    gapRate: number | null | undefined
    size?: 'small' | 'medium'
  }>(),
  {
    size: 'medium',
  },
)

const appSettingsStore = useAppSettingsStore()
const outcome = computed(() =>
  estimateOutcome(props.gapRate, appSettingsStore.onTimeThresholdPercent),
)
const label = computed(() => {
  if (outcome.value === 'early') return '見積より早く完了'
  if (outcome.value === 'late') return '見積を超過'
  if (outcome.value === 'on-time') {
    return `おおむね見積どおり（誤差比±${appSettingsStore.onTimeThresholdPercent}%以内）`
  }
  return ''
})
</script>

<template>
  <span
    v-if="outcome !== 'unknown'"
    class="estimate-outcome-icon"
    :class="[outcome, `size-${size}`]"
    role="img"
    :aria-label="label"
    :title="label"
  >
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle class="face-background" cx="12" cy="12" r="9" />
      <circle class="face-outline" cx="12" cy="12" r="9" />
      <circle cx="9" cy="10" :r="outcome === 'early' ? 1.25 : 1" fill="currentColor" />
      <circle cx="15" cy="10" :r="outcome === 'early' ? 1.25 : 1" fill="currentColor" />
      <circle v-if="outcome === 'early'" class="mouth surprised-mouth" cx="12" cy="15" r="1.7" />
      <path v-else-if="outcome === 'late'" class="mouth" d="M7.5 16.5c1.2-3.6 7.8-3.6 9 0" />
      <path v-else class="mouth smile-mouth" d="M7.5 13.5c1.2 3.6 7.8 3.6 9 0" />
    </svg>
  </span>
</template>

<style scoped>
.estimate-outcome-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  vertical-align: middle;
}

.size-small {
  width: 1.1rem;
  height: 1.1rem;
}

.size-medium {
  width: 1.45rem;
  height: 1.45rem;
}

.early {
  color: var(--color-task-accent);
}

.late {
  color: var(--color-danger);
}

.on-time {
  color: var(--color-success);
}

svg {
  display: block;
  width: 100%;
  height: 100%;
}

.face-background {
  fill: currentColor;
  opacity: 0.12;
}

.face-outline,
.mouth {
  fill: none;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}
</style>
