<script setup lang="ts">
import type { ApiError } from '@/types/apiError'

defineProps<{
  error: ApiError
}>()
</script>

<template>
  <div class="error-message" role="alert">
    <p class="message">{{ error.message }}</p>
    <ul v-if="error.formErrors.length > 0" class="form-errors">
      <li v-for="(msg, i) in error.formErrors" :key="i">{{ msg }}</li>
    </ul>
  </div>
</template>

<style scoped>
.error-message {
  padding: 0.8em 1em;
  border-radius: 6px;
  border: 1px solid var(--color-danger);
  background-color: var(--color-surface);
  color: var(--color-danger);
}

.message {
  margin: 0;
  font-weight: 600;
}

.form-errors {
  margin: 0.4em 0 0;
  padding-left: 1.2em;
  font-size: 0.9rem;
}
</style>
