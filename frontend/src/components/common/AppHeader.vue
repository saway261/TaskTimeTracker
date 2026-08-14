<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import BaseButton from '@/components/common/BaseButton.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import { useAuthStore } from '@/stores/authStore'
import { useNotificationStore } from '@/stores/notificationStore'
import type { ApiError } from '@/types/apiError'

const router = useRouter()
const authStore = useAuthStore()
const notification = useNotificationStore()
const loggingOut = ref(false)

async function logout() {
  loggingOut.value = true
  try {
    await authStore.logout()
    notification.success('ログアウトしました。')
    await router.replace('/login')
  } catch (e) {
    notification.error((e as ApiError).message)
  } finally {
    loggingOut.value = false
  }
}
</script>

<template>
  <header class="app-header">
    <RouterLink to="/projects" class="brand">Task Time Tracker</RouterLink>
    <div class="header-actions">
      <template v-if="authStore.isAuthenticated">
        <span class="user-email">{{ authStore.currentUser?.email }}</span>
        <RouterLink to="/password-change" class="password-link">パスワード変更</RouterLink>
        <BaseButton variant="secondary" :disabled="loggingOut" @click="logout">
          {{ loggingOut ? 'ログアウト中…' : 'ログアウト' }}
        </BaseButton>
      </template>
      <ThemeToggle class="theme-toggle-slot" />
    </div>
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.8em;
}

.user-email {
  max-width: 240px;
  overflow: hidden;
  color: var(--color-text-muted);
  font-size: 0.85rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.password-link {
  color: var(--color-text);
  font-size: 0.9rem;
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

@media (max-width: 680px) {
  .app-header {
    align-items: flex-start;
  }

  .header-actions {
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .user-email {
    display: none;
  }
}
</style>
