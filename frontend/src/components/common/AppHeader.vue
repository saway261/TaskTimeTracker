<script setup lang="ts">
import { RouterLink } from 'vue-router'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
</script>

<template>
  <header class="app-header">
    <RouterLink to="/projects" class="brand">Task Time Tracker</RouterLink>
    <ThemeToggle class="theme-toggle-slot" />
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.8em 1.2em;
  background-color: var(--color-surface);
  border-bottom: 1px solid var(--color-surface-muted);
}

.theme-toggle-slot {
  flex-shrink: 0;
}

.brand {
  font-weight: 700;
  font-size: 1.1rem;
  color: var(--color-text);
  text-decoration: none;
}

.brand:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}
</style>
