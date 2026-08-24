<script setup lang="ts">
import { ref } from 'vue'
import * as reflectionsApi from '@/api/reflectionsApi'
import { useNotificationStore } from '@/stores/notificationStore'
import { useQuickReflectionStore } from '@/stores/quickReflectionStore'
import type { ApiError } from '@/types/apiError'
import type { ReflectionRequest } from '@/types/reflection'
import ReflectionModal from './ReflectionModal.vue'

// クイック振り返りの唯一の実体。App.vue から遅延読み込みで差し込まれる。
// 完了操作を行った画面ではなくアプリ直下に置くことで、完了によって一覧の行が
// 消えてもモーダルが巻き添えでアンマウントされない（quickReflectionStore 参照）。
const store = useQuickReflectionStore()
const notification = useNotificationStore()

const submitting = ref(false)
const error = ref<ApiError | null>(null)

// ✖・背景クリック・Escapeのいずれで閉じても、画面遷移はせず元の画面に戻るだけにする。
// 後で入力し直せることは defer-hint の案内文で伝える。
function handleUpdate(open: boolean) {
  if (open) return
  store.close()
  error.value = null
}

async function handleSubmit(payload: ReflectionRequest) {
  const task = store.task
  if (!task) return
  submitting.value = true
  error.value = null
  try {
    if (task.reflection) {
      await reflectionsApi.update(task.id, payload)
      notification.success('振り返りを更新しました。')
    } else {
      await reflectionsApi.create(task.id, payload)
      notification.success('振り返りを登録しました。')
    }
    store.close()
  } catch (e) {
    error.value = e as ApiError
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <ReflectionModal
    :model-value="store.task !== null"
    :task="store.task"
    :submitting="submitting"
    :error="error"
    defer-hint
    @update:model-value="handleUpdate"
    @submit="handleSubmit"
  />
</template>
