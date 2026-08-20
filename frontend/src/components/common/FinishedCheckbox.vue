<script setup lang="ts">
withDefaults(
  defineProps<{
    modelValue: boolean
    disabled?: boolean
    label?: string
  }>(),
  {
    disabled: false,
    label: '完了',
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

function handleChange(e: Event) {
  emit('update:modelValue', (e.target as HTMLInputElement).checked)
}
</script>

<template>
  <label class="finished-checkbox" :class="{ disabled }">
    <input type="checkbox" :checked="modelValue" :disabled="disabled" @change="handleChange" />
    <span>{{ label }}</span>
  </label>
</template>

<style scoped>
.finished-checkbox {
  display: inline-flex;
  align-items: center;
  gap: 0.5em;
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--color-text);
  cursor: pointer;
}

.finished-checkbox.disabled {
  color: var(--color-text-muted);
  cursor: not-allowed;
}

.finished-checkbox input {
  width: 1.4em;
  height: 1.4em;
  accent-color: var(--color-accent);
  cursor: inherit;
}

.finished-checkbox input:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}
</style>
