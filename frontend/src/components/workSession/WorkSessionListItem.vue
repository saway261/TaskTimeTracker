<script setup lang="ts">
import { computed } from 'vue'
import type { WorkSession } from '@/types/workSession'
import { formatMinutes } from '@/utils/duration'
import BaseButton from '@/components/common/BaseButton.vue'

const props = defineProps<{
  session: WorkSession
  // 完了済みタスクでは false（読み取り専用の一覧のみ、Q5-A）。
  editable: boolean
}>()

const emit = defineEmits<{
  edit: [session: WorkSession]
  delete: [session: WorkSession]
}>()

// 稼働中TIMERは編集不可（先に停止させる必要がある。フェーズ6でstop機能を追加するまでは停止手段が無い）。
const isActiveTimer = computed(
  () => props.session.type === 'TIMER' && props.session.endedAt === null,
)
const canModify = computed(() => props.editable && !isActiveTimer.value)

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('ja-JP', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}
</script>

<template>
  <div class="work-session-item" :class="{ timer: session.type === 'TIMER' }">
    <span class="label">{{ session.type === 'TIMER' ? 'タイマー' : '手動' }}</span>

    <span v-if="session.type === 'TIMER'" class="range">
      {{ session.startedAt ? formatDateTime(session.startedAt) : '-' }}
      〜
      {{ session.endedAt ? formatDateTime(session.endedAt) : '稼働中' }}
    </span>

    <span class="minutes">
      {{ session.minutes !== null ? formatMinutes(session.minutes) : '-' }}
    </span>

    <div class="actions">
      <template v-if="canModify">
        <BaseButton variant="secondary" @click="emit('edit', session)">編集</BaseButton>
        <BaseButton variant="danger" @click="emit('delete', session)">削除</BaseButton>
      </template>
      <span v-else-if="editable && isActiveTimer" class="hint">
        稼働中は停止後に編集・削除できます
      </span>
    </div>
  </div>
</template>

<style scoped>
.work-session-item {
  display: flex;
  align-items: center;
  gap: 0.6em;
  padding: 0.7em 1em;
  border-radius: 8px;
  background-color: var(--color-surface);
  border: 1px solid var(--color-surface-muted);
  flex-wrap: wrap;
}

.label {
  flex-shrink: 0;
  font-size: 0.75rem;
  padding: 0.15em 0.5em;
  border-radius: 4px;
  background-color: var(--color-surface-muted);
  color: var(--color-text-muted);
}

.range {
  flex: 1;
  min-width: 14em;
  font-size: 0.9rem;
  color: var(--color-text);
}

.minutes {
  flex-shrink: 0;
  font-weight: 600;
  color: var(--color-text);
}

.actions {
  display: flex;
  align-items: center;
  gap: 0.5em;
  margin-left: auto;
}

.hint {
  font-size: 0.8rem;
  color: var(--color-text-muted);
}
</style>
