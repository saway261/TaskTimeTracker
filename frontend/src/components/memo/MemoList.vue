<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
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
  deleted: [id: string]
}>()

const notification = useNotificationStore()

const memoItems = ref<HTMLElement | null>(null)
const memosExpanded = ref(false)
const hasHiddenMemos = ref(false)
let resizeObserver: ResizeObserver | null = null

function updateMemoOverflow() {
  const notes = Array.from(memoItems.value?.querySelectorAll<HTMLElement>('.memo-note') ?? [])
  const firstTop = notes[0]?.offsetTop
  hasHiddenMemos.value =
    firstTop !== undefined && notes.some((note) => note.offsetTop > firstTop + 2)
  if (!hasHiddenMemos.value) memosExpanded.value = false
}

function toggleMemos() {
  memosExpanded.value = !memosExpanded.value
}

onMounted(() => {
  void nextTick(updateMemoOverflow)
  if (typeof ResizeObserver !== 'undefined' && memoItems.value) {
    resizeObserver = new ResizeObserver(updateMemoOverflow)
    resizeObserver.observe(memoItems.value)
  }
  window.addEventListener('resize', updateMemoOverflow)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('resize', updateMemoOverflow)
})

watch(
  () => props.memos.map((memo) => memo.id).join(','),
  () => void nextTick(updateMemoOverflow),
)

const showCreateModal = ref(false)
const creating = ref(false)
const createError = ref<ApiError | null>(null)

const editingMemo = ref<MemoResponse | null>(null)
const showEditModal = ref(false)
const updating = ref(false)
const editError = ref<ApiError | null>(null)

function previewOf(comment: string): string {
  return comment.length > PREVIEW_LENGTH ? `${comment.slice(0, PREVIEW_LENGTH)}…` : comment
}

function openCreateModal() {
  createError.value = null
  showCreateModal.value = true
}

async function handleCreate(req: MemoRequest) {
  creating.value = true
  createError.value = null
  try {
    const memo = await props.onCreate(req)
    emit('created', memo)
    notification.success('メモを追加しました。')
    showCreateModal.value = false
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
    <div class="memo-notes">
      <div
        v-if="memos.length > 0"
        ref="memoItems"
        class="memo-items"
        :class="{ collapsed: !memosExpanded }"
      >
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
      <button
        type="button"
        class="add-memo"
        :class="{ empty: memos.length === 0 }"
        aria-label="メモを追加"
        @click="openCreateModal"
      >
        ＋<span v-if="memos.length === 0" class="add-memo-label">メモを追加</span>
      </button>
    </div>
    <button
      v-if="hasHiddenMemos"
      type="button"
      class="memo-expand"
      :aria-expanded="memosExpanded"
      @click="toggleMemos"
    >
      {{ memosExpanded ? '閉じる' : 'さらに表示' }}
    </button>

    <BaseModal v-model="showCreateModal" title="メモを追加">
      <MemoForm
        mode="create"
        :submitting="creating"
        :error="createError"
        @submit="handleCreate"
        @cancel="showCreateModal = false"
      />
    </BaseModal>

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
  align-items: flex-start;
  gap: 0.6em;
  padding-top: 0.2em;
}

.memo-items {
  flex: 0 1 auto;
  min-width: 0;
  max-width: calc(100% - 3.35rem);
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 0.8em 0.6em;
  padding: 0.15em 0.25em 0.45em;
}

.memo-items.collapsed {
  max-height: 5.1rem;
  overflow: hidden;
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

.add-memo {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.75rem;
  height: 2.75rem;
  padding: 0;
  border: 1px dashed var(--color-text-muted);
  border-radius: 50%;
  background: var(--color-surface);
  color: var(--color-text-muted);
  font-size: 1.5rem;
  line-height: 1;
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    background-color 0.15s ease,
    color 0.15s ease,
    transform 0.15s ease;
}

.add-memo.empty {
  width: auto;
  padding: 0 1rem;
  border-radius: 1.375rem;
  font-size: 1rem;
}

.add-memo-label {
  margin-left: 0.35rem;
  font-size: 0.9rem;
  font-weight: 600;
}

.add-memo:hover {
  border-color: var(--color-accent);
  background: var(--color-surface-muted);
  color: var(--color-accent);
  transform: scale(1.05);
}

.add-memo:focus-visible {
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

.memo-expand {
  align-self: flex-start;
  padding: 0.35em 0.2em;
  border: 0;
  background: transparent;
  color: var(--color-accent);
  font: inherit;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
}

.memo-expand:hover {
  text-decoration: underline;
}

.memo-expand:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}
</style>
