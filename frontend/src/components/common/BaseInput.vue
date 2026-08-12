<script setup lang="ts">
import { computed, useId } from 'vue'

const props = withDefaults(
  defineProps<{
    modelValue: string
    label: string
    error?: string
    maxlength?: number
    required?: boolean
    type?: string
    step?: string | number
  }>(),
  {
    error: undefined,
    maxlength: undefined,
    required: false,
    type: 'text',
    step: undefined,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const inputId = useId()

const remaining = computed(() =>
  props.maxlength === undefined ? null : props.maxlength - props.modelValue.length,
)
</script>

<template>
  <div class="base-input">
    <label :for="inputId">
      {{ label }}
      <span v-if="required" class="required">*</span>
    </label>
    <input
      :id="inputId"
      :type="type"
      :value="modelValue"
      :maxlength="maxlength"
      :step="step"
      :aria-invalid="!!error"
      :aria-describedby="error ? `${inputId}-error` : undefined"
      @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
    <div class="meta">
      <p v-if="error" :id="`${inputId}-error`" class="error">{{ error }}</p>
      <span v-if="remaining !== null" class="remaining">残り{{ remaining }}文字</span>
    </div>
  </div>
</template>

<style scoped>
.base-input {
  display: flex;
  flex-direction: column;
  gap: 0.3em;
}

label {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text);
}

.required {
  color: var(--color-danger);
}

input {
  padding: 0.5em 0.7em;
  border-radius: 6px;
  border: 1px solid var(--color-surface-muted);
  background-color: var(--color-surface);
  color: var(--color-text);
  font-size: 1rem;
}

input:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 1px;
}

input[aria-invalid='true'] {
  border-color: var(--color-danger);
}

.meta {
  display: flex;
  justify-content: space-between;
  gap: 0.5em;
  min-height: 1.2em;
}

.error {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-danger);
}

.remaining {
  margin-left: auto;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}
</style>
