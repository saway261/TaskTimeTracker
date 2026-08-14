<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <main class="auth-page">
    <section class="auth-card" :aria-labelledby="`${$attrs.id ?? 'auth'}-title`">
      <header>
        <h1 :id="`${$attrs.id ?? 'auth'}-title`">{{ title }}</h1>
        <p v-if="description">{{ description }}</p>
      </header>
      <slot />
      <footer v-if="$slots.footer">
        <slot name="footer" />
      </footer>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  min-height: calc(100vh - 64px);
  display: grid;
  place-items: start center;
  padding: 3em 1em;
}

.auth-card {
  width: min(100%, 440px);
  display: flex;
  flex-direction: column;
  gap: 1.4em;
  padding: 2em;
  border: 1px solid var(--color-surface-muted);
  border-radius: 10px;
  background: var(--color-surface);
  box-shadow: 0 4px 16px rgb(0 0 0 / 8%);
}

header {
  display: flex;
  flex-direction: column;
  gap: 0.5em;
}

h1,
p {
  margin: 0;
}

h1 {
  font-size: 1.5rem;
}

p,
footer {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

footer {
  text-align: center;
}

footer :deep(a) {
  color: var(--color-accent);
}

@media (max-width: 480px) {
  .auth-page {
    padding: 1.5em 0.8em;
  }

  .auth-card {
    padding: 1.4em;
  }
}
</style>
