<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useTaskGroupStore } from '@/stores/taskGroupStore'
import { useNotificationStore } from '@/stores/notificationStore'
import type { ApiError } from '@/types/apiError'
import BaseModal from '@/components/common/BaseModal.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'

// タスクグループはProject直下でのみ並べ替えられる（他コンテナへは移動できない）ため、
// TaskRowMenuと違い上下の並べ替えのみを提供する。
const props = defineProps<{
  modelValue: boolean
  taskGroupId: number
  detailTo: string
  finished: boolean
  canMoveUp: boolean
  canMoveDown: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'add-task': []
  'move-up': []
  'move-down': []
}>()

const taskGroupStore = useTaskGroupStore()
const notification = useNotificationStore()

const finishing = ref(false)
const error = ref<ApiError | null>(null)

function close() {
  emit('update:modelValue', false)
}

function moveUp() {
  emit('move-up')
  close()
}

function moveDown() {
  emit('move-down')
  close()
}

function addTask() {
  emit('add-task')
  close()
}

async function finishTaskGroup() {
  finishing.value = true
  error.value = null
  try {
    await taskGroupStore.updateFinished(props.taskGroupId, { isFinished: true })
    notification.success('タスクグループを完了にしました。')
    close()
  } catch (e) {
    error.value = e as ApiError
  } finally {
    finishing.value = false
  }
}
</script>

<template>
  <BaseModal
    :model-value="modelValue"
    title="タスクグループの操作"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <ErrorMessage v-if="error" :error="error" />
    <div class="menu-list">
      <BaseButton v-if="!finished" :disabled="finishing" @click="finishTaskGroup">
        完了にする
      </BaseButton>
      <BaseButton @click="addTask">＋ タスク追加</BaseButton>
      <RouterLink :to="detailTo" class="detail-link" @click="close">
        タスクグループの詳細・編集・メモへ →
      </RouterLink>
      <BaseButton v-if="canMoveUp" variant="secondary" @click="moveUp">↑ 上へ移動</BaseButton>
      <BaseButton v-if="canMoveDown" variant="secondary" @click="moveDown">↓ 下へ移動</BaseButton>
      <p v-if="!canMoveUp && !canMoveDown" class="empty">これ以上移動できません。</p>
    </div>
  </BaseModal>
</template>

<style scoped>
.menu-list {
  display: flex;
  flex-direction: column;
  gap: 0.5em;
}

.empty {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.detail-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5em;
  padding: 0.5em 1.2em;
  border-radius: 6px;
  border: 1px solid var(--color-text-muted);
  background-color: transparent;
  color: var(--color-text);
  font-size: 0.95rem;
  font-weight: 600;
  text-decoration: none;
}

.detail-link:hover {
  background-color: var(--color-surface-muted);
}

.detail-link:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}
</style>
