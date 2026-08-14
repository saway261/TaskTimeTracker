<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import BaseButton from '@/components/common/BaseButton.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import { useAuthStore } from '@/stores/authStore'
import { useNotificationStore } from '@/stores/notificationStore'
import type { ApiError } from '@/types/apiError'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const notification = useNotificationStore()
const loggingOut = ref(false)

// RouterLinkの既定のactiveクラスは完全一致以外にも付くが、aria-currentはexact一致時しか付かない。
// タスク詳細等の配下ルートでも「タスク管理」が現在地だと分かるよう、パスの前方一致で自前判定する。
const isTaskManagementActive = computed(() => route.path.startsWith('/projects'))
const isReflectionActive = computed(() => route.path.startsWith('/reflections'))

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
    <div class="header-start">
      <RouterLink to="/projects" class="brand">Task Time Tracker</RouterLink>
      <nav class="main-nav" aria-label="メインナビゲーション">
        <RouterLink
          to="/projects"
          class="nav-link"
          :class="{ active: isTaskManagementActive }"
          :aria-current="isTaskManagementActive ? 'page' : undefined"
        >
          タスク管理
        </RouterLink>
        <RouterLink
          to="/reflections"
          class="nav-link"
          :class="{ active: isReflectionActive }"
          :aria-current="isReflectionActive ? 'page' : undefined"
        >
          振り返り
        </RouterLink>
      </nav>
    </div>
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
  flex-wrap: wrap;
  gap: 0.6em;
  padding: 0.8em 1.2em;
  background-color: var(--color-surface);
  border-bottom: 1px solid var(--color-surface-muted);
}

.header-start {
  display: flex;
  align-items: center;
  gap: 1.2em;
  flex-wrap: wrap;
}

.main-nav {
  display: flex;
  gap: 0.3em;
}

.nav-link {
  padding: 0.4em 0.8em;
  border-radius: 6px;
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 600;
  text-decoration: none;
}

.nav-link.active {
  color: var(--color-text);
  background-color: var(--color-surface-muted);
}

.nav-link:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
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
