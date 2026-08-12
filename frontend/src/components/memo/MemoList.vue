<script setup lang="ts">
import { ref } from 'vue'
import * as memosApi from '@/api/memosApi'
import type { MemoRequest, MemoResponse } from '@/types/memo'
import type { ApiError } from '@/types/apiError'
import { useNotificationStore } from '@/stores/notificationStore'
import MemoForm from './MemoForm.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const PREVIEW_LENGTH = 18

const props = defineProps<{
  memos: MemoResponse[]
  onCreate: (req: MemoRequest) => Promise<MemoResponse>
}>()

const emit = defineEmits<{
  created: [memo: MemoResponse]
  updated: [memo: MemoResponse]
  deleted: [id: number]
}>()

const notification = useNotificationStore()

const creating = ref(false)
const createError = ref<ApiError | null>(null)

const editingMemo = ref<MemoResponse | null>(null)
const showEditModal = ref(false)
const updating = ref(false)
const editError = ref<ApiError | null>(null)

function previewOf(comment: string): string {
  return comment.length > PREVIEW_LENGTH ? `${comment.slice(0, PREVIEW_LENGTH)}…` : comment
}

async function handleCreate(req: MemoRequest) {
  creating.value = true
  createError.value = null
  try {
    const memo = await props.onCreate(req)
    emit('created', memo)
    notification.success('メモを追加しました。')
  } catch (e) {
    createError.value = e as ApiError
  } finally {
    creating.value = false
  }
}

function openEditModal(memo: MemoResponse) {
  editingMemo.value = memo
  editError.value = null
  showEditModal.value = true
}

async function handleUpdate(req: MemoRequest) {
  if (!editingMemo.value) return
  updating.value = true
  editError.value = null
  try {
    const res = await memosApi.updateMemo(editingMemo.value.id, req)
    emit('updated', res.data)
    notification.success('メモを更新しました。')
    showEditModal.value = false
  } catch (e) {
    editError.value = e as ApiError
  } finally {
    updating.value = false
  }
}

const showDeleteConfirm = ref(false)

async function handleDelete() {
  if (!editingMemo.value) return
  editError.value = null
  try {
    await memosApi.deleteMemo(editingMemo.value.id)
    emit('deleted', editingMemo.value.id)
    notification.success('メモを削除しました。')
    showEditModal.value = false
  } catch (e) {
    editError.value = e as ApiError
  }
}
</script>

<template>
  <section class="memo-list" aria-label="メモ">
    <div v-if="memos.length > 0" class="memo-notes">
      <button
        v-for="memo in memos"
        :key="memo.id"
        type="button"
        class="memo-note"
        @click="openEditModal(memo)"
      >
        {{ previewOf(memo.comment) }}
      </button>
    </div>

    <MemoForm mode="create" :submitting="creating" :error="createError" @submit="handleCreate" />

    <BaseModal v-model="showEditModal" title="メモを編集">
      <MemoForm
        v-if="editingMemo"
        mode="edit"
        :initial-comment="editingMemo.comment"
        :submitting="updating"
        :error="editError"
        @submit="handleUpdate"
        @cancel="showEditModal = false"
      />
      <button type="button" class="delete-memo" @click="showDeleteConfirm = true">
        このメモを削除する
      </button>
    </BaseModal>

    <ConfirmDialog
      v-model="showDeleteConfirm"
      title="メモの削除"
      message="このメモを削除しますか？"
      confirm-label="削除する"
      danger
      @confirm="handleDelete"
    />
  </section>
</template>

<style scoped>
.memo-list {
  display: flex;
  flex-direction: column;
  gap: 0.7em;
}

.memo-notes {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8em 0.6em;
  padding-top: 0.2em;
}

.memo-note {
  width: 7em;
  min-height: 5.5em;
  padding: 0.6em 0.7em;
  border: none;
  border-radius: 2px 10px 2px 10px;
  background: var(--color-memo-note-bg);
  color: var(--color-memo-note-text);
  font-size: 0.8rem;
  line-height: 1.3;
  text-align: left;
  cursor: pointer;
  box-shadow: 2px 3px 6px rgb(0 0 0 / 20%);
  transform: rotate(-1.5deg);
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  word-break: break-word;
}

.memo-note:nth-child(even) {
  transform: rotate(1.5deg);
}

.memo-note:hover,
.memo-note:focus-visible {
  transform: scale(1.05);
  box-shadow: 3px 5px 10px rgb(0 0 0 / 25%);
}

.memo-note:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.delete-memo {
  margin-top: 1em;
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  font-size: 0.85rem;
  color: var(--color-danger);
  text-decoration: underline;
}
</style>
