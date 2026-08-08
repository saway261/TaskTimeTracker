<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps<{
  modelValue: boolean
  title: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const dialogRef = ref<HTMLElement | null>(null)
let previouslyFocused: HTMLElement | null = null

function close() {
  emit('update:modelValue', false)
}

function focusableElements(): HTMLElement[] {
  if (!dialogRef.value) return []
  return Array.from(
    dialogRef.value.querySelectorAll<HTMLElement>(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])',
    ),
  ).filter((el) => !el.hasAttribute('disabled'))
}

function onKeydown(e: KeyboardEvent) {
  if (!props.modelValue) return

  if (e.key === 'Escape') {
    close()
    return
  }

  if (e.key === 'Tab') {
    const focusables = focusableElements()
    if (focusables.length === 0) return
    const first = focusables[0]
    const last = focusables[focusables.length - 1]
    if (e.shiftKey && document.activeElement === first) {
      e.preventDefault()
      last.focus()
    } else if (!e.shiftKey && document.activeElement === last) {
      e.preventDefault()
      first.focus()
    }
  }
}

watch(
  () => props.modelValue,
  async (open) => {
    if (open) {
      previouslyFocused = document.activeElement as HTMLElement | null
      await nextTick()
      const [first] = focusableElements()
      ;(first ?? dialogRef.value)?.focus()
    } else {
      previouslyFocused?.focus()
      previouslyFocused = null
    }
  },
)

onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="base-modal-backdrop" @mousedown.self="close">
      <div
        ref="dialogRef"
        class="base-modal"
        role="dialog"
        aria-modal="true"
        :aria-label="title"
        tabindex="-1"
      >
        <header class="base-modal-header">
          <h2>{{ title }}</h2>
          <button type="button" class="close-button" aria-label="閉じる" @click="close">×</button>
        </header>
        <div class="base-modal-body">
          <slot />
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.base-modal-backdrop {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1em;
  background-color: rgb(0 0 0 / 45%);
  z-index: 200;
}

.base-modal {
  width: 100%;
  max-width: 32em;
  max-height: calc(100vh - 2em);
  overflow-y: auto;
  border-radius: 8px;
  background-color: var(--color-surface);
  box-shadow: 0 8px 24px rgb(0 0 0 / 25%);
}

.base-modal:focus-visible {
  outline: none;
}

.base-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1em;
  padding: 1em 1.2em;
  border-bottom: 1px solid var(--color-surface-muted);
}

.base-modal-header h2 {
  margin: 0;
  font-size: 1.1rem;
  color: var(--color-text);
}

.close-button {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.2rem;
  line-height: 1;
  color: var(--color-text-muted);
}

.close-button:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.base-modal-body {
  padding: 1.2em;
}
</style>
