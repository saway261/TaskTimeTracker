<script setup lang="ts">
import BaseModal from '@/components/common/BaseModal.vue'
import BaseButton from '@/components/common/BaseButton.vue'

// タスクグループはProject直下でのみ並べ替えられる（他コンテナへは移動できない）ため、
// TaskRowMenuと違い上下の並べ替えのみを提供する。
defineProps<{
  modelValue: boolean
  canMoveUp: boolean
  canMoveDown: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'move-up': []
  'move-down': []
}>()

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
</script>

<template>
  <BaseModal
    :model-value="modelValue"
    title="タスクグループの操作"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="menu-list">
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
</style>
