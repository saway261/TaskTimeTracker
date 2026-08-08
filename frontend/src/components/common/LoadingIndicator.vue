<template>
  <div class="loading-indicator" role="status">
    <span class="spinner" aria-hidden="true" />
    <span>読み込み中…</span>
  </div>
</template>

<style scoped>
.loading-indicator {
  display: flex;
  align-items: center;
  gap: 0.6em;
  padding: 1em;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.spinner {
  width: 1em;
  height: 1em;
  border-radius: 50%;
  border: 2px solid var(--color-surface-muted);
  border-top-color: var(--color-accent);
  animation: spin 0.8s linear infinite;
}

@media (prefers-reduced-motion: reduce) {
  .spinner {
    animation: none;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
