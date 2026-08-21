<script setup lang="ts">
defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()
</script>

<template>
  <button
    type="button"
    class="completed-items-toggle"
    :class="{ active: modelValue }"
    :aria-pressed="modelValue"
    :aria-label="modelValue ? '完了済みの項目を非表示にする' : '完了済みの項目を表示する'"
    @click="emit('update:modelValue', !modelValue)"
  >
    <span class="icon-wrap" aria-hidden="true">
      <svg class="icon" viewBox="0 0 24 24">
        <path
          d="M12 5C4.4 5 1 12 1 12s3.4 7 11 7 11-7 11-7-3.4-7-11-7Zm0 11.5A4.5 4.5 0 1 1 12 7a4.5 4.5 0 0 1 0 9.5Zm0-7A2.5 2.5 0 1 0 12 14a2.5 2.5 0 0 0 0-5Z"
        />
      </svg>
      <span v-if="!modelValue" class="icon-slash" />
    </span>
    <span>完了済み</span>
    <span class="state-label">{{ modelValue ? '表示中' : '非表示' }}</span>
  </button>
</template>

<style scoped>
.completed-items-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.5em;
  min-height: 2.35rem;
  padding: 0.4em 0.55em 0.4em 0.7em;
  border: 1px solid var(--color-text-muted);
  border-radius: 999px;
  background-color: transparent;
  color: var(--color-text-muted);
  font: inherit;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    border-color 0.15s ease,
    color 0.15s ease;
}

.completed-items-toggle:hover {
  background-color: var(--color-surface-muted);
  color: var(--color-text);
}

.completed-items-toggle.active {
  border-color: var(--color-accent);
  background-color: var(--color-surface);
  color: var(--color-accent);
}

.completed-items-toggle:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.icon-wrap {
  position: relative;
  display: inline-flex;
  width: 1.45em;
  height: 1.45em;
}

.icon {
  width: 100%;
  height: 100%;
  fill: currentColor;
}

.icon-slash {
  position: absolute;
  top: 50%;
  left: -0.12em;
  width: 1.7em;
  height: 2px;
  border-radius: 2px;
  background-color: currentColor;
  box-shadow: 0 0 0 2px var(--color-bg);
  transform: rotate(45deg);
}

.state-label {
  padding: 0.15em 0.5em;
  border-radius: 999px;
  background-color: var(--color-surface-muted);
  color: var(--color-text-muted);
  font-size: 0.78rem;
}

.active .state-label {
  background-color: var(--color-accent);
  color: #ffffff;
}
</style>
