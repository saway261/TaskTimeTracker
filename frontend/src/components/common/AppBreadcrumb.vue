<script setup lang="ts">
import { RouterLink } from 'vue-router'

export interface BreadcrumbItem {
  label: string
  to?: string
}

defineProps<{
  items: BreadcrumbItem[]
}>()
</script>

<template>
  <nav class="app-breadcrumb" aria-label="パンくずリスト">
    <ol>
      <li v-for="(item, i) in items" :key="i">
        <RouterLink v-if="item.to" :to="item.to">{{ item.label }}</RouterLink>
        <span v-else aria-current="page">{{ item.label }}</span>
        <span v-if="i < items.length - 1" class="separator" aria-hidden="true">›</span>
      </li>
    </ol>
  </nav>
</template>

<style scoped>
.app-breadcrumb ol {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  list-style: none;
  margin: 0;
  padding: 0;
  gap: 0.4em;
  font-size: 0.9rem;
}

.app-breadcrumb li {
  display: flex;
  align-items: center;
  gap: 0.4em;
}

.app-breadcrumb a {
  color: var(--color-text-muted);
  text-decoration: none;
}

.app-breadcrumb a:hover {
  color: var(--color-accent);
  text-decoration: underline;
}

.app-breadcrumb a:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.app-breadcrumb [aria-current='page'] {
  color: var(--color-text);
  font-weight: 600;
}

.separator {
  color: var(--color-text-muted);
}
</style>
