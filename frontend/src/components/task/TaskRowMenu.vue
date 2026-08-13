<script setup lang="ts">
import { computed, ref } from 'vue'
import { useItemOrderStore } from '@/stores/itemOrderStore'
import { useNotificationStore } from '@/stores/notificationStore'
import type { TaskGroupResponse } from '@/types/taskGroup'
import type { ApiError } from '@/types/apiError'
import BaseModal from '@/components/common/BaseModal.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'

// ドラッグ以外の並べ替え・移動手段（§7.3・§7.4.5 rule 4）。キーボード利用者・タッチ端末向け。
const props = defineProps<{
  modelValue: boolean
  taskId: number
  projectId: number
  containerKey: string // 'project:{pId}' | 'taskGroup:{tgId}'
  taskGroups: TaskGroupResponse[]
  canMoveUp: boolean
  canMoveDown: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'move-up': []
  'move-down': []
}>()

const itemOrderStore = useItemOrderStore()
const notification = useNotificationStore()

const moving = ref(false)
const error = ref<ApiError | null>(null)

// 移動先は末尾でよい仕様のため（§7.4.5）、並べ替えAPIは呼ばずPATCH parentのみで完結する。
const moveTargets = computed(() => {
  const list: { key: string; label: string }[] = []
  const projectKey = `project:${props.projectId}`
  if (props.containerKey !== projectKey) {
    list.push({ key: projectKey, label: 'プロジェクト直下' })
  }
  for (const taskGroup of props.taskGroups) {
    const key = `taskGroup:${taskGroup.id}`
    if (key !== props.containerKey) {
      list.push({ key, label: taskGroup.title })
    }
  }
  return list
})

function close() {
  emit('update:modelValue', false)
}

function handleMoveUp() {
  emit('move-up')
  close()
}

function handleMoveDown() {
  emit('move-down')
  close()
}

async function handleMoveTo(targetKey: string) {
  moving.value = true
  error.value = null
  try {
    const parentReq = targetKey.startsWith('project:')
      ? { projectId: props.projectId, taskGroupId: null }
      : { projectId: null, taskGroupId: Number(targetKey.split(':')[1]) }
    await itemOrderStore.moveTaskViaMenu({
      taskId: props.taskId,
      parentReq,
      sourceContainer: props.containerKey,
      targetContainer: targetKey,
    })
    notification.success('タスクを移動しました。')
    close()
  } catch (e) {
    error.value = e as ApiError
  } finally {
    moving.value = false
  }
}
</script>

<template>
  <BaseModal
    :model-value="modelValue"
    title="タスクの操作"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <ErrorMessage v-if="error" :error="error" />
    <div class="menu-list">
      <BaseButton v-if="canMoveUp" variant="secondary" @click="handleMoveUp">
        ↑ 上へ移動
      </BaseButton>
      <BaseButton v-if="canMoveDown" variant="secondary" @click="handleMoveDown">
        ↓ 下へ移動
      </BaseButton>
      <template v-if="moveTargets.length > 0">
        <p class="section-label">移動先</p>
        <BaseButton
          v-for="target in moveTargets"
          :key="target.key"
          variant="secondary"
          :disabled="moving"
          @click="handleMoveTo(target.key)"
        >
          {{ target.label }}へ移動
        </BaseButton>
      </template>
    </div>
  </BaseModal>
</template>

<style scoped>
.menu-list {
  display: flex;
  flex-direction: column;
  gap: 0.5em;
}

.section-label {
  margin: 0.4em 0 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}
</style>
