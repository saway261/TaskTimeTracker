<script setup lang="ts">
import { useNotificationStore } from '@/stores/notificationStore'

const store = useNotificationStore()
</script>

<template>
  <div class="toast-host" role="region" aria-live="polite">
    <div v-for="n in store.notifications" :key="n.id" class="toast" :class="n.kind">
      <span>{{ n.message }}</span>
      <button type="button" aria-label="閉じる" @click="store.dismiss(n.id)">×</button>
    </div>
  </div>
</template>

<style scoped>
.toast-host {
  position: fixed;
  bottom: 1em;
  right: 1em;
  display: flex;
  flex-direction: column;
  gap: 0.5em;
  z-index: 100;
}

.toast {
  display: flex;
  align-items: center;
  gap: 0.8em;
  padding: 0.6em 0.9em;
  border-radius: 6px;
  background-color: var(--color-surface);
  border: 1px solid var(--color-surface-muted);
  box-shadow: 0 2px 8px rgb(0 0 0 / 15%);
  color: var(--color-text);
  font-size: 0.9rem;
}

.toast.error {
  border-color: var(--color-danger);
  color: var(--color-danger);
}

.toast.success {
  border-color: var(--color-success);
  color: var(--color-success);
}

.toast.info {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.toast button {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1rem;
  line-height: 1;
  color: inherit;
}
</style>
