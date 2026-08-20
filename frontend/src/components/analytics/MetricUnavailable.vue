<script setup lang="ts">
import { computed } from 'vue'
import type { MetricAvailabilityResponse } from '@/types/analytics'

const props = defineProps<{
  availability: MetricAvailabilityResponse
}>()

const remaining = computed(() =>
  Math.max(0, props.availability.requiredCount - props.availability.currentCount),
)
</script>

<template>
  <p class="metric-unavailable">あと{{ remaining }}件でここに数値が表示されます</p>
</template>

<style scoped>
.metric-unavailable {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.85rem;
  line-height: 1.5;
}
</style>
