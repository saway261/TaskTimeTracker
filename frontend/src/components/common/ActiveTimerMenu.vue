<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useActiveTimerStore } from '@/stores/activeTimerStore'
import type { ActiveTimer } from '@/types/workSession'
import TaskQuickActionModal from '@/components/task/TaskQuickActionModal.vue'

const WARNING_THRESHOLD_MS = 5 * 60 * 60 * 1000

const activeTimerStore = useActiveTimerStore()
const menuOpen = ref(false)
const selectedTimer = ref<ActiveTimer | null>(null)
const now = ref(Date.now())
const menuRoot = ref<HTMLElement | null>(null)
const menuButton = ref<HTMLButtonElement | null>(null)
let clockIntervalId: ReturnType<typeof setInterval> | null = null
let refreshIntervalId: ReturnType<typeof setInterval> | null = null

function elapsedMs(timer: ActiveTimer) {
  return Math.max(0, now.value - Date.parse(timer.startedAt))
}

function isOverdue(timer: ActiveTimer) {
  return elapsedMs(timer) >= WARNING_THRESHOLD_MS
}

const sortedTimers = computed(() =>
  [...activeTimerStore.activeTimers].sort((a, b) => {
    const warningOrder = Number(isOverdue(b)) - Number(isOverdue(a))
    return warningOrder || Date.parse(a.startedAt) - Date.parse(b.startedAt)
  }),
)
const overdueCount = computed(
  () => activeTimerStore.activeTimers.filter((timer) => isOverdue(timer)).length,
)
const hasOverdueTimer = computed(() => overdueCount.value > 0)

function formatElapsed(timer: ActiveTimer) {
  const totalSeconds = Math.floor(elapsedMs(timer) / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  const pad = (value: number) => value.toString().padStart(2, '0')
  return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
}

function detailTo(timer: ActiveTimer) {
  return timer.taskGroupId === null
    ? `/projects/${timer.projectId}/tasks/${timer.taskId}`
    : `/projects/${timer.projectId}/task-groups/${timer.taskGroupId}/tasks/${timer.taskId}`
}

function refresh() {
  if (!activeTimerStore.loading) {
    void activeTimerStore.fetchActiveTimers().catch(() => {})
  }
}

function toggleMenu() {
  menuOpen.value = !menuOpen.value
  if (menuOpen.value) refresh()
}

function closeMenu() {
  menuOpen.value = false
}

async function closeMenuAndRestoreFocus() {
  closeMenu()
  await nextTick()
  menuButton.value?.focus()
}

function openTask(timer: ActiveTimer) {
  selectedTimer.value = timer
  closeMenu()
}

function handlePointerDown(event: PointerEvent) {
  if (menuOpen.value && !menuRoot.value?.contains(event.target as Node)) {
    closeMenu()
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && menuOpen.value) {
    event.preventDefault()
    void closeMenuAndRestoreFocus()
  }
}

function handleVisibilityChange() {
  if (document.visibilityState === 'visible') {
    now.value = Date.now()
    refresh()
  }
}

onMounted(() => {
  activeTimerStore.clear()
  refresh()
  clockIntervalId = setInterval(() => {
    now.value = Date.now()
  }, 1000)
  refreshIntervalId = setInterval(refresh, 60_000)
  document.addEventListener('pointerdown', handlePointerDown)
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  if (clockIntervalId !== null) clearInterval(clockIntervalId)
  if (refreshIntervalId !== null) clearInterval(refreshIntervalId)
  document.removeEventListener('pointerdown', handlePointerDown)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('keydown', handleKeydown)
  activeTimerStore.clear()
})
</script>

<template>
  <div ref="menuRoot" class="active-timer-menu">
    <button
      ref="menuButton"
      type="button"
      class="timer-menu-trigger"
      :class="{ warning: hasOverdueTimer }"
      :aria-label="
        hasOverdueTimer
          ? '稼働中タイマーあり、5時間以上連続稼働中のタイマーがあります'
          : activeTimerStore.hasActiveTimers
            ? '稼働中タイマーあり'
            : '稼働中タイマーなし'
      "
      aria-haspopup="dialog"
      :aria-expanded="menuOpen"
      aria-controls="active-timer-panel"
      @click="toggleMenu"
    >
      <svg class="timer-icon" viewBox="0 0 24 24" aria-hidden="true">
        <path
          fill="currentColor"
          d="M9 2h6v2H9zm3 4a8 8 0 1 0 8 8 8 8 0 0 0-8-8m0 14a6 6 0 1 1 6-6 6 6 0 0 1-6 6m1-11h-2v6l4.7 2.8 1-1.7-3.7-2.2z"
        />
      </svg>
      <span
        v-if="activeTimerStore.hasActiveTimers"
        class="timer-badge"
        :class="{ danger: hasOverdueTimer }"
        aria-hidden="true"
      ></span>
    </button>

    <section
      v-if="menuOpen"
      id="active-timer-panel"
      class="timer-panel"
      role="dialog"
      aria-label="稼働中タイマー"
    >
      <header class="panel-header">
        <div>
          <h2>稼働中タイマー</h2>
        </div>
        <button
          type="button"
          class="refresh-button"
          :disabled="activeTimerStore.loading"
          @click="refresh"
        >
          {{ activeTimerStore.loading ? '更新中…' : '更新' }}
        </button>
      </header>

      <p v-if="activeTimerStore.error && sortedTimers.length === 0" class="load-error">
        タイマー一覧を取得できませんでした。
      </p>
      <p v-else-if="!activeTimerStore.loading && sortedTimers.length === 0" class="empty-message">
        稼働中のタイマーはありません。
      </p>

      <ul v-else class="timer-list">
        <li v-for="timer in sortedTimers" :key="timer.sessionId">
          <button
            type="button"
            class="timer-item"
            :class="{ warning: isOverdue(timer) }"
            @click="openTask(timer)"
          >
            <span class="timer-item-main">
              <span class="task-title">{{ timer.taskTitle }}</span>
              <span class="elapsed-time">{{ formatElapsed(timer) }}</span>
            </span>
            <span v-if="isOverdue(timer)" class="warning-message">
              5時間以上連続で稼働しています。停止忘れがないか確認してください。
            </span>
          </button>
        </li>
      </ul>
    </section>
  </div>

  <TaskQuickActionModal
    v-if="selectedTimer"
    :model-value="selectedTimer !== null"
    :task-id="selectedTimer.taskId"
    :task-title="selectedTimer.taskTitle"
    :detail-to="detailTo(selectedTimer)"
    @update:model-value="selectedTimer = null"
  />
</template>

<style scoped>
.active-timer-menu {
  position: relative;
  flex-shrink: 0;
}

.timer-menu-trigger {
  position: relative;
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

.timer-menu-trigger:hover,
.timer-menu-trigger[aria-expanded='true'] {
  border-color: var(--color-accent);
  background: var(--color-surface-muted);
}

.timer-menu-trigger.warning {
  border-color: var(--color-danger);
  color: var(--color-danger);
}

.timer-menu-trigger:focus-visible,
.refresh-button:focus-visible,
.timer-item:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.timer-icon {
  width: 24px;
  height: 24px;
}

.timer-badge {
  position: absolute;
  top: -5px;
  right: -7px;
  width: 13px;
  height: 13px;
  border: 2px solid var(--color-surface);
  border-radius: 999px;
  background: var(--color-accent);
}

.timer-badge.danger {
  background: var(--color-danger);
}

.timer-panel {
  position: absolute;
  top: calc(100% + 0.6em);
  right: 0;
  z-index: 110;
  width: min(390px, calc(100vw - 2em));
  max-height: min(560px, calc(100vh - 6em));
  overflow-y: auto;
  padding: 0.75em;
  border: 1px solid var(--color-surface-muted);
  border-radius: 10px;
  background: var(--color-surface);
  box-shadow: 0 8px 24px rgb(0 0 0 / 16%);
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1em;
  padding: 0.15em 0.2em 0.7em;
}

.panel-header h2 {
  margin: 0;
}

.panel-header h2 {
  font-size: 1rem;
}

.refresh-button {
  padding: 0.35em 0.55em;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: var(--color-accent);
  cursor: pointer;
}

.refresh-button:disabled {
  cursor: default;
  opacity: 0.6;
}

.timer-list {
  display: flex;
  flex-direction: column;
  gap: 0.45em;
  margin: 0;
  padding: 0;
  list-style: none;
}

.timer-item {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 0.4em;
  padding: 0.75em;
  border: 1px solid var(--color-surface-muted);
  border-radius: 8px;
  background: transparent;
  color: var(--color-text);
  text-align: left;
  cursor: pointer;
}

.timer-item:hover {
  background: var(--color-surface-muted);
}

.timer-item.warning {
  border-color: var(--color-danger);
  background: color-mix(in srgb, var(--color-danger) 8%, var(--color-surface));
}

.timer-item-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8em;
}

.task-title {
  min-width: 0;
  overflow: hidden;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.elapsed-time {
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
  font-weight: 700;
}

.warning-message {
  color: var(--color-danger);
  font-size: 0.78rem;
  font-weight: 600;
}

.empty-message,
.load-error {
  margin: 0;
  padding: 1.3em 0.75em;
  color: var(--color-text-muted);
  font-size: 0.88rem;
  text-align: center;
}

.load-error {
  color: var(--color-danger);
}

@media (max-width: 520px) {
  .timer-panel {
    position: fixed;
    top: 64px;
    right: 0.75em;
    left: 0.75em;
    width: auto;
  }
}
</style>
