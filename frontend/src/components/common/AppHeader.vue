<script setup lang="ts">
import {
  computed,
  defineAsyncComponent,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import ActiveTimerMenu from '@/components/common/ActiveTimerMenu.vue'
import { useAuthStore } from '@/stores/authStore'
import { useNotificationStore } from '@/stores/notificationStore'
import type { ApiError } from '@/types/apiError'

// 開くまでは読み込まない(要件 §11)。章の一覧・本文を扱うため本体バンドルへ含めない。
const TutorialChapterModal = defineAsyncComponent(
  () => import('@/components/tutorial/TutorialChapterModal.vue'),
)

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const notification = useNotificationStore()
const loggingOut = ref(false)
const menuOpen = ref(false)
const menuRoot = ref<HTMLElement | null>(null)
const menuButton = ref<HTMLButtonElement | null>(null)
const navMenuOpen = ref(false)
const navMenuRoot = ref<HTMLElement | null>(null)
const navMenuButton = ref<HTMLButtonElement | null>(null)
const showChapterModal = ref(false)

function toggleMenu() {
  menuOpen.value = !menuOpen.value
  if (menuOpen.value) navMenuOpen.value = false
}

function closeMenu() {
  menuOpen.value = false
}

function openChapterModal() {
  showChapterModal.value = true
  closeMenu()
}

async function closeMenuAndRestoreFocus() {
  closeMenu()
  await nextTick()
  menuButton.value?.focus()
}

function toggleNavMenu() {
  navMenuOpen.value = !navMenuOpen.value
  if (navMenuOpen.value) menuOpen.value = false
}

function closeNavMenu() {
  navMenuOpen.value = false
}

async function closeNavMenuAndRestoreFocus() {
  closeNavMenu()
  await nextTick()
  navMenuButton.value?.focus()
}

function handlePointerDown(event: PointerEvent) {
  if (menuOpen.value && !menuRoot.value?.contains(event.target as Node)) {
    closeMenu()
  }
  if (navMenuOpen.value && !navMenuRoot.value?.contains(event.target as Node)) {
    closeNavMenu()
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key !== 'Escape') return
  if (menuOpen.value) {
    event.preventDefault()
    void closeMenuAndRestoreFocus()
  } else if (navMenuOpen.value) {
    event.preventDefault()
    void closeNavMenuAndRestoreFocus()
  }
}

// RouterLinkの既定のactiveクラスは完全一致以外にも付くが、aria-currentはexact一致時しか付かない。
// タスク詳細等の配下ルートでも「タスク管理」が現在地だと分かるよう、パスの前方一致で自前判定する。
const isTaskManagementActive = computed(() => route.path.startsWith('/projects'))
const isReflectionActive = computed(() => route.path.startsWith('/reflections'))
const isAnalyticsActive = computed(() => route.path.startsWith('/analytics'))

async function logout() {
  closeMenu()
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

watch(
  () => authStore.isAuthenticated,
  (isAuthenticated) => {
    if (!isAuthenticated) {
      closeMenu()
      closeNavMenu()
    }
  },
)

watch(
  () => route.fullPath,
  () => closeNavMenu(),
)

onMounted(() => {
  document.addEventListener('pointerdown', handlePointerDown)
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', handlePointerDown)
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <header class="app-header">
    <div class="header-start">
      <RouterLink to="/projects" class="brand">
        <span class="brand-full">Task Time Tracker</span>
        <span class="brand-short">TTT</span>
      </RouterLink>
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
        <RouterLink
          to="/analytics"
          class="nav-link"
          :class="{ active: isAnalyticsActive }"
          :aria-current="isAnalyticsActive ? 'page' : undefined"
        >
          分析
        </RouterLink>
      </nav>
    </div>
    <div class="header-actions">
      <ActiveTimerMenu v-if="authStore.isAuthenticated" />
      <ThemeToggle class="theme-toggle-slot" />
      <div v-if="authStore.isAuthenticated" ref="menuRoot" class="user-menu">
        <button
          ref="menuButton"
          type="button"
          class="user-menu-trigger"
          aria-label="ユーザーメニューを開く"
          aria-haspopup="menu"
          :aria-expanded="menuOpen"
          aria-controls="user-menu-panel"
          @click="toggleMenu"
        >
          <svg class="user-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path
              fill="currentColor"
              d="M12 12a5 5 0 1 0 0-10 5 5 0 0 0 0 10m0 2c-5.05 0-9 2.55-9 5.8C3 21.02 3.98 22 5.2 22h13.6c1.22 0 2.2-.98 2.2-2.2 0-3.25-3.95-5.8-9-5.8"
            />
          </svg>
        </button>

        <div v-if="menuOpen" id="user-menu-panel" class="user-menu-panel" role="menu">
          <div class="user-menu-identity">
            <span class="identity-label">ログイン中</span>
            <span class="user-email" :title="authStore.currentUser?.email">
              {{ authStore.currentUser?.email }}
            </span>
          </div>
          <div class="user-menu-separator" role="separator"></div>
          <button type="button" class="user-menu-item" role="menuitem" @click="openChapterModal">
            チュートリアル
          </button>
          <RouterLink to="/tags" class="user-menu-item" role="menuitem" @click="closeMenu">
            タグ管理
          </RouterLink>
          <RouterLink to="/email-change" class="user-menu-item" role="menuitem" @click="closeMenu">
            メールアドレス変更
          </RouterLink>
          <RouterLink
            to="/password-change"
            class="user-menu-item"
            role="menuitem"
            @click="closeMenu"
          >
            パスワード変更
          </RouterLink>
          <div class="user-menu-separator" role="separator"></div>
          <button
            type="button"
            class="user-menu-item logout-button"
            role="menuitem"
            :disabled="loggingOut"
            @click="logout"
          >
            {{ loggingOut ? 'ログアウト中…' : 'ログアウト' }}
          </button>
        </div>
      </div>
      <div ref="navMenuRoot" class="mobile-navigation">
        <button
          ref="navMenuButton"
          type="button"
          class="mobile-nav-trigger"
          aria-label="ナビゲーションメニューを開く"
          aria-haspopup="true"
          :aria-expanded="navMenuOpen"
          aria-controls="mobile-nav-panel"
          @click="toggleNavMenu"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path fill="currentColor" d="M3 6h18v2H3zm0 5h18v2H3zm0 5h18v2H3z" />
          </svg>
        </button>
        <nav
          v-if="navMenuOpen"
          id="mobile-nav-panel"
          class="mobile-nav-panel"
          aria-label="モバイルナビゲーション"
        >
          <RouterLink
            to="/projects"
            class="mobile-nav-item"
            :class="{ active: isTaskManagementActive }"
            :aria-current="isTaskManagementActive ? 'page' : undefined"
            @click="closeNavMenu"
          >
            タスク管理
          </RouterLink>
          <RouterLink
            to="/reflections"
            class="mobile-nav-item"
            :class="{ active: isReflectionActive }"
            :aria-current="isReflectionActive ? 'page' : undefined"
            @click="closeNavMenu"
          >
            振り返り
          </RouterLink>
          <RouterLink
            to="/analytics"
            class="mobile-nav-item"
            :class="{ active: isAnalyticsActive }"
            :aria-current="isAnalyticsActive ? 'page' : undefined"
            @click="closeNavMenu"
          >
            分析
          </RouterLink>
        </nav>
      </div>
    </div>
  </header>
  <TutorialChapterModal v-if="showChapterModal" v-model="showChapterModal" />
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

.mobile-navigation,
.brand-short {
  display: none;
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

.user-menu {
  position: relative;
  flex-shrink: 0;
}

.user-menu-trigger {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 1px solid var(--color-surface-muted);
  border-radius: 50%;
  background-color: var(--color-surface);
  color: var(--color-text);
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    border-color 0.15s ease;
}

.user-menu-trigger:hover,
.user-menu-trigger[aria-expanded='true'] {
  border-color: var(--color-accent);
  background-color: var(--color-surface-muted);
}

.user-menu-trigger:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.user-icon {
  width: 23px;
  height: 23px;
}

.user-menu-panel {
  position: absolute;
  top: calc(100% + 0.6em);
  right: 0;
  z-index: 100;
  width: min(320px, calc(100vw - 2em));
  padding: 0.5em;
  border: 1px solid var(--color-surface-muted);
  border-radius: 8px;
  background-color: var(--color-surface);
  box-shadow: 0 8px 24px rgb(0 0 0 / 16%);
}

.mobile-nav-trigger {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 1px solid var(--color-surface-muted);
  border-radius: 50%;
  background: var(--color-surface);
  color: var(--color-text);
  cursor: pointer;
}

.mobile-nav-trigger:hover,
.mobile-nav-trigger[aria-expanded='true'] {
  border-color: var(--color-accent);
  background: var(--color-surface-muted);
}

.mobile-nav-trigger:focus-visible,
.mobile-nav-item:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.mobile-nav-trigger svg {
  width: 23px;
  height: 23px;
}

.mobile-nav-panel {
  position: fixed;
  top: 64px;
  right: 0.75rem;
  left: 0.75rem;
  z-index: 120;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0.55rem;
  border: 1px solid var(--color-surface-muted);
  border-radius: 10px;
  background: var(--color-surface);
  box-shadow: 0 8px 24px rgb(0 0 0 / 16%);
}

.mobile-nav-item {
  min-height: 44px;
  display: flex;
  align-items: center;
  padding: 0.65rem 0.8rem;
  border-radius: 6px;
  color: var(--color-text);
  font-size: 0.95rem;
  font-weight: 600;
  text-decoration: none;
}

.mobile-nav-item:hover,
.mobile-nav-item.active {
  background: var(--color-surface-muted);
}

.user-menu-identity {
  display: flex;
  flex-direction: column;
  gap: 0.2em;
  padding: 0.65em 0.75em;
}

.identity-label {
  color: var(--color-text-muted);
  font-size: 0.75rem;
}

.user-email {
  max-width: 240px;
  overflow: hidden;
  color: var(--color-text-muted);
  font-size: 0.85rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-menu-separator {
  height: 1px;
  margin: 0.35em 0;
  background-color: var(--color-surface-muted);
}

.user-menu-item {
  width: 100%;
  display: flex;
  align-items: center;
  min-height: 40px;
  padding: 0.55em 0.75em;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: var(--color-text);
  font-size: 0.9rem;
  text-align: left;
  text-decoration: none;
  cursor: pointer;
}

.user-menu-item:hover {
  background-color: var(--color-surface-muted);
}

.user-menu-item:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: -2px;
}

.logout-button {
  color: var(--color-danger);
}

.logout-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
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
    align-items: center;
    flex-wrap: nowrap;
    padding: 0.65em 0.75em;
  }

  .header-start {
    min-width: 0;
    flex: 1;
    flex-wrap: nowrap;
  }

  .main-nav {
    display: none;
  }

  .header-actions {
    flex-shrink: 0;
    flex-wrap: nowrap;
    justify-content: flex-end;
    gap: 0.45em;
  }

  .mobile-navigation {
    display: block;
    position: relative;
    flex-shrink: 0;
  }

  .user-menu-panel {
    position: fixed;
    top: 64px;
    right: 0.75rem;
    left: 0.75rem;
    width: auto;
    max-height: calc(100vh - 5rem);
    overflow-y: auto;
    z-index: 120;
  }
}

@media (max-width: 520px) {
  .brand-full {
    display: none;
  }

  .brand-short {
    display: inline;
  }
}
</style>
