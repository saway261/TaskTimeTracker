<script setup lang="ts">
withDefaults(
  defineProps<{
    variant?: 'primary' | 'secondary' | 'danger'
    disabled?: boolean
    type?: 'button' | 'submit'
  }>(),
  {
    variant: 'primary',
    disabled: false,
    type: 'button',
  },
)
</script>

<template>
  <button :type="type" class="base-button" :class="variant" :disabled="disabled">
    <slot />
  </button>
</template>

<style scoped>
.base-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5em;
  padding: 0.5em 1.2em;
  border-radius: 6px;
  border: 1px solid transparent;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.base-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.base-button:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.primary {
  background-color: var(--color-accent);
  color: #ffffff;
}

.primary:not(:disabled):hover {
  background-color: var(--color-accent-hover);
}

.secondary {
  background-color: transparent;
  border-color: var(--color-text-muted);
  color: var(--color-text);
}

.secondary:not(:disabled):hover {
  background-color: var(--color-surface-muted);
}

.danger {
  background-color: transparent;
  border-color: var(--color-danger);
  color: var(--color-danger);
}

.danger:not(:disabled):hover {
  background-color: var(--color-danger);
  color: #ffffff;
}
</style>
