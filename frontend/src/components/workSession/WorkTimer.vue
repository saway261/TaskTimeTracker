<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useWorkSessionStore } from '@/stores/workSessionStore'
import { useTaskStore } from '@/stores/taskStore'
import { useNotificationStore } from '@/stores/notificationStore'
import type { ApiError } from '@/types/apiError'
import BaseButton from '@/components/common/BaseButton.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'

const props = defineProps<{
  taskId: number
}>()

const workSessionStore = useWorkSessionStore()
const taskStore = useTaskStore()
const notification = useNotificationStore()

const elapsedMs = ref(0)
const isNegative = ref(false)
let intervalId: ReturnType<typeof setInterval> | null = null

function tick() {
  const session = workSessionStore.activeSession
  if (!session?.startedAt) return
  const diff = Date.now() - Date.parse(session.startedAt)
  if (diff < 0) {
    // 案Bが未適用のまま時計がずれている場合の検知。カウントを止めて警告を出す（§6.2）。
    isNegative.value = true
    stopInterval()
    return
  }
  isNegative.value = false
  elapsedMs.value = diff
}

function stopInterval() {
  if (intervalId !== null) {
    clearInterval(intervalId)
    intervalId = null
  }
}

function startInterval() {
  stopInterval()
  tick()
  intervalId = setInterval(tick, 1000)
}

function handleVisibilityChange() {
  if (document.visibilityState === 'visible') {
    tick()
  }
}

watch(
  () => workSessionStore.activeSession,
  (session) => {
    if (session) {
      startInterval()
    } else {
      stopInterval()
      elapsedMs.value = 0
      isNegative.value = false
    }
  },
  { immediate: true },
)

function handleBeforeUnload(e: BeforeUnloadEvent) {
  if (workSessionStore.activeSession) {
    e.preventDefault()
    e.returnValue = ''
  }
}

onMounted(() => {
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onUnmounted(() => {
  stopInterval()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

const elapsedLabel = computed(() => {
  const totalSeconds = Math.floor(elapsedMs.value / 1000)
  const h = Math.floor(totalSeconds / 3600)
  const m = Math.floor((totalSeconds % 3600) / 60)
  const s = totalSeconds % 60
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${pad(h)}:${pad(m)}:${pad(s)}`
})

const starting = ref(false)
const startError = ref<ApiError | null>(null)

async function handleStart() {
  starting.value = true
  startError.value = null
  try {
    await workSessionStore.startTimer(props.taskId)
    notification.success('タイマーを開始しました。')
  } catch (e) {
    startError.value = e as ApiError
  } finally {
    starting.value = false
  }
}

const stopping = ref(false)
const stopError = ref<ApiError | null>(null)

async function handleStop() {
  const session = workSessionStore.activeSession
  if (!session) return
  if (!window.confirm('タイマーを停止しますか？')) return
  stopping.value = true
  stopError.value = null
  try {
    await workSessionStore.stopTimer(session.id)
    await Promise.all([
      workSessionStore.fetchTotalMinutes(props.taskId),
      taskStore.fetchTask(props.taskId),
    ])
    notification.success('タイマーを停止しました。')
  } catch (e) {
    const err = e as ApiError
    if (err.kind === 'businessRule') {
      // 別タブなどで既に停止済み・タスクが完了済みのケース。エラーを出しっぱなしにせず
      // 状態を取り直して整合させる（§6.3）。
      await workSessionStore.fetchSessions(props.taskId).catch(() => {})
      notification.info('このタイマーは既に停止されていました。')
    } else {
      stopError.value = err
    }
  } finally {
    stopping.value = false
  }
}
</script>

<template>
  <div class="work-timer">
    <div class="display" role="status" aria-live="polite">
      <span class="clock" :class="{ negative: isNegative }">{{ elapsedLabel }}</span>
      <span v-if="isNegative" class="warning">
        時刻がずれているため計測を停止しました。時計の設定を確認してください。
      </span>
      <span v-else-if="workSessionStore.multipleActiveSessions" class="warning">
        複数の未終了タイマーが見つかりました。最新のものを表示しています。
      </span>
    </div>

    <ErrorMessage v-if="startError" :error="startError" />
    <ErrorMessage v-if="stopError" :error="stopError" />

    <BaseButton v-if="!workSessionStore.activeSession" :disabled="starting" @click="handleStart">
      開始
    </BaseButton>
    <BaseButton v-else variant="danger" :disabled="stopping" @click="handleStop"> 停止 </BaseButton>
  </div>
</template>

<style scoped>
.work-timer {
  display: flex;
  flex-direction: column;
  gap: 0.6em;
  padding: 0.9em 1em;
  border-radius: 8px;
  background-color: var(--color-surface);
  border: 1px solid var(--color-surface-muted);
}

.display {
  display: flex;
  align-items: baseline;
  gap: 0.8em;
  flex-wrap: wrap;
}

.clock {
  font-size: 1.6rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--color-text);
}

.clock.negative {
  color: var(--color-danger);
}

.warning {
  font-size: 0.85rem;
  color: var(--color-danger);
}
</style>
