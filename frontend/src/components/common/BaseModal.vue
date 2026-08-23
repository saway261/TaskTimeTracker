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
    <Transition name="base-modal">
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
            <h2>
              {{ title }}
              <slot name="title-extra" />
            </h2>
            <button type="button" class="close-button" aria-label="閉じる" @click="close">×</button>
          </header>
          <div class="base-modal-body">
            <slot />
          </div>
        </div>
      </div>
    </Transition>
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

/* モバイルは下からのシート、PCは中央（§7.2） */
@media (max-width: 640px) {
  .base-modal-backdrop {
    align-items: flex-end;
    padding: 0;
  }

  .base-modal {
    max-width: 100%;
    max-height: 85vh;
    border-radius: 12px 12px 0 0;
  }
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

/* 開閉アニメーション150〜200ms（§7.2）。prefers-reduced-motionで無効化する。 */
.base-modal-enter-active,
.base-modal-leave-active {
  transition: opacity 0.18s ease;
}

.base-modal-enter-active .base-modal,
.base-modal-leave-active .base-modal {
  transition: transform 0.18s ease;
}

.base-modal-enter-from,
.base-modal-leave-to {
  opacity: 0;
}

.base-modal-enter-from .base-modal,
.base-modal-leave-to .base-modal {
  transform: translateY(12px) scale(0.98);
}

@media (max-width: 640px) {
  .base-modal-enter-from .base-modal,
  .base-modal-leave-to .base-modal {
    transform: translateY(100%);
  }
}

@media (prefers-reduced-motion: reduce) {
  .base-modal-enter-active,
  .base-modal-leave-active,
  .base-modal-enter-active .base-modal,
  .base-modal-leave-active .base-modal {
    transition: none;
  }
}
</style>
