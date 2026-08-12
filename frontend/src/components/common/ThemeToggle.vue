<script setup lang="ts">
import { ref } from 'vue'
import { getTheme, setTheme } from '@/theme'

const theme = ref(getTheme())

function toggle() {
  const next = theme.value === 'dark' ? 'light' : 'dark'
  setTheme(next)
  theme.value = next
}
</script>

<template>
  <button
    type="button"
    class="theme-toggle"
    :aria-pressed="theme === 'dark'"
    :aria-label="theme === 'dark' ? 'ライトモードに切り替える' : 'ダークモードに切り替える'"
    @click="toggle"
  >
    {{ theme === 'dark' ? '🌙' : '☀️' }}
  </button>
</template>

<style scoped>
.theme-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.2em;
  height: 2.2em;
  border-radius: 50%;
  border: 1px solid var(--color-surface-muted);
  background-color: var(--color-surface);
  cursor: pointer;
  font-size: 1rem;
  line-height: 1;
}

.theme-toggle:hover {
  background-color: var(--color-surface-muted);
}

.theme-toggle:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}
</style>
