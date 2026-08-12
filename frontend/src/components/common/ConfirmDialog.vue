<script setup lang="ts">
import BaseModal from './BaseModal.vue'
import BaseButton from './BaseButton.vue'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    title?: string
    message: string
    confirmLabel?: string
    cancelLabel?: string
    danger?: boolean
  }>(),
  {
    title: '確認',
    confirmLabel: '実行する',
    cancelLabel: 'キャンセル',
    danger: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
}>()

function close() {
  emit('update:modelValue', false)
}

function handleConfirm() {
  emit('confirm')
  close()
}
</script>

<template>
  <BaseModal
    :model-value="modelValue"
    :title="title"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <p class="message">{{ props.message }}</p>
    <div class="actions">
      <BaseButton type="button" variant="secondary" @click="close">{{ cancelLabel }}</BaseButton>
      <BaseButton type="button" :variant="danger ? 'danger' : 'primary'" @click="handleConfirm">
        {{ confirmLabel }}
      </BaseButton>
    </div>
  </BaseModal>
</template>

<style scoped>
.message {
  margin: 0 0 1em;
  color: var(--color-text);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.6em;
}
</style>
