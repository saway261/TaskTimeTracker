<script setup lang="ts">
import type { DiagnosisResponse } from '@/types/analytics'

defineProps<{
  diagnosis: DiagnosisResponse
}>()
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
</style>
