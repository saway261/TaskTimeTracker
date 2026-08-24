<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useTagStore } from '@/stores/tagStore'
import { useNotificationStore } from '@/stores/notificationStore'
import type { ApiError } from '@/types/apiError'
import type { TagResponse } from '@/types/tag'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseInput from '@/components/common/BaseInput.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import TagLimitResolver from '@/components/tag/TagLimitResolver.vue'
import TutorialHelpButton from '@/components/tutorial/TutorialHelpButton.vue'
import { TUTORIAL_SCOPES } from '@/tutorial/scopes'

const MAX_ACTIVE_TAGS = 50
const tagStore = useTagStore()
const notification = useNotificationStore()

const showArchived = ref(false)
const loadError = ref<ApiError | null>(null)
const createName = ref('')
const creating = ref(false)
const createError = ref<ApiError | null>(null)
const createResolverOpen = ref(false)
const editingTagId = ref<string | null>(null)
const editName = ref('')
const busyTagId = ref<string | null>(null)
const rowErrors = ref<Record<string, ApiError | undefined>>({})
const restoreResolverTagId = ref<string | null>(null)

const visibleTags = computed(() =>
  showArchived.value ? tagStore.tags : tagStore.tags.filter((tag) => !tag.isArchived),
)

function isTagLimitError(error: ApiError) {
  return error.status === 400 && error.fieldErrors.tagLimit !== undefined
}

function clearRowError(tagId: string) {
  const next = { ...rowErrors.value }
  delete next[tagId]
  rowErrors.value = next
}

function rowErrorFor(tagId: string): ApiError {
  return rowErrors.value[tagId] as ApiError
}

async function createTag() {
  creating.value = true
  createError.value = null
  try {
    await tagStore.createTag(createName.value)
    createName.value = ''
    notification.success('タグを登録しました。')
  } finally {
    creating.value = false
  }
}

async function handleCreate() {
  try {
    await createTag()
  } catch (e) {
    const error = e as ApiError
    if (isTagLimitError(error)) {
      createError.value = null
      restoreResolverTagId.value = null
      createResolverOpen.value = true
    } else {
      createError.value = error
    }
  }
}

function startRename(tag: TagResponse) {
  editingTagId.value = tag.id
  editName.value = tag.name
  clearRowError(tag.id)
}

function cancelRename() {
  editingTagId.value = null
  editName.value = ''
}

async function renameTag(tag: TagResponse) {
  busyTagId.value = tag.id
  clearRowError(tag.id)
  try {
    await tagStore.renameTag(tag.id, editName.value)
    cancelRename()
    notification.success('タグ名を変更しました。')
  } catch (e) {
    rowErrors.value = { ...rowErrors.value, [tag.id]: e as ApiError }
  } finally {
    busyTagId.value = null
  }
}

async function setArchived(tag: TagResponse, isArchived: boolean) {
  busyTagId.value = tag.id
  clearRowError(tag.id)
  try {
    await tagStore.setArchived(tag.id, isArchived)
    notification.success(isArchived ? 'タグをアーカイブしました。' : 'アーカイブを解除しました。')
  } catch (e) {
    const error = e as ApiError
    if (!isArchived && isTagLimitError(error)) {
      createResolverOpen.value = false
      restoreResolverTagId.value = tag.id
    } else {
      rowErrors.value = { ...rowErrors.value, [tag.id]: error }
    }
    throw e
  } finally {
    busyTagId.value = null
  }
}

async function handleArchiveChange(tag: TagResponse) {
  try {
    await setArchived(tag, !tag.isArchived)
  } catch {
    // エラーは行内、または上限解決パネルへ表示する。
  }
}

function retryRestore(tag: TagResponse) {
  return setArchived(tag, false)
}

onMounted(async () => {
  loadError.value = null
  try {
    await tagStore.fetchTags(true)
  } catch (e) {
    loadError.value = e as ApiError
  }
})
</script>

<template>
  <main class="tag-management-view">
    <header class="page-header">
      <div>
        <h1>
          タグ管理
          <TutorialHelpButton
            chapter-id="tags"
            chapter-title="タグ管理"
            :scope="TUTORIAL_SCOPES.tags"
          />
        </h1>
        <p>
          タスクの分類に使うタグを管理します。タグは削除せず、使わなくなったらアーカイブできます。
        </p>
      </div>
      <p class="active-count" aria-live="polite">
        <strong>{{ tagStore.activeTags.length }}</strong> / {{ MAX_ACTIVE_TAGS }} 件
      </p>
    </header>

    <section class="create-section" aria-labelledby="create-tag-title">
      <h2 id="create-tag-title">新しいタグ</h2>
      <form class="create-form" @submit.prevent="handleCreate">
        <div class="create-input">
          <BaseInput
            v-model="createName"
            label="タグ名"
            required
            :maxlength="20"
            :error="createError?.fieldErrors.name"
          />
        </div>
        <BaseButton type="submit" :disabled="creating || createName.trim() === ''">
          {{ creating ? '登録中…' : '登録する' }}
        </BaseButton>
      </form>
      <ErrorMessage v-if="createError" :error="createError" />
      <TagLimitResolver
        v-if="createResolverOpen"
        action-label="作成"
        :retry="createTag"
        @resolved="createResolverOpen = false"
        @cancel="createResolverOpen = false"
      />
    </section>

    <section class="list-section" aria-labelledby="tag-list-title">
      <div class="list-header">
        <h2 id="tag-list-title">タグ一覧</h2>
        <label class="archive-toggle">
          <input v-model="showArchived" type="checkbox" />
          アーカイブ済みを表示
        </label>
      </div>

      <LoadingIndicator v-if="tagStore.loading && !tagStore.initialized" />
      <template v-else>
        <ErrorMessage v-if="loadError" :error="loadError" />
        <p v-if="visibleTags.length === 0 && !loadError" class="empty">
          {{ showArchived ? 'タグがまだありません。' : 'アクティブなタグがありません。' }}
        </p>
      </template>
      <ul v-if="visibleTags.length > 0" class="tag-list">
        <li v-for="tag in visibleTags" :key="tag.id" class="tag-row">
          <div class="tag-content">
            <form
              v-if="editingTagId === tag.id"
              class="rename-form"
              @submit.prevent="renameTag(tag)"
            >
              <BaseInput
                v-model="editName"
                label="タグ名"
                required
                :maxlength="20"
                :error="rowErrors[tag.id]?.fieldErrors.name"
              />
              <div class="row-actions">
                <BaseButton type="button" variant="secondary" @click="cancelRename">
                  キャンセル
                </BaseButton>
                <BaseButton
                  type="submit"
                  :disabled="busyTagId === tag.id || editName.trim() === ''"
                >
                  保存する
                </BaseButton>
              </div>
            </form>
            <template v-else>
              <div class="tag-name-status">
                <span class="tag-name">{{ tag.name }}</span>
                <span v-if="tag.isArchived" class="archived-badge">アーカイブ済み</span>
              </div>
              <p class="assigned-count">
                <strong>{{ tag.assignedTaskCount }}</strong> 件のタスク
              </p>
              <div class="row-actions">
                <BaseButton
                  type="button"
                  variant="secondary"
                  :disabled="busyTagId === tag.id"
                  @click="startRename(tag)"
                >
                  名前を変更
                </BaseButton>
                <BaseButton
                  type="button"
                  variant="secondary"
                  :disabled="busyTagId === tag.id"
                  @click="handleArchiveChange(tag)"
                >
                  {{ tag.isArchived ? 'アーカイブを解除' : 'アーカイブ' }}
                </BaseButton>
              </div>
            </template>
          </div>

          <ErrorMessage v-if="rowErrors[tag.id]" :error="rowErrorFor(tag.id)" />
          <TagLimitResolver
            v-if="restoreResolverTagId === tag.id"
            action-label="解除"
            :retry="() => retryRestore(tag)"
            @resolved="restoreResolverTagId = null"
            @cancel="restoreResolverTagId = null"
          />
        </li>
      </ul>
    </section>
  </main>
</template>

<style scoped>
.tag-management-view {
  width: min(900px, 100%);
  margin: 0 auto;
  padding: 1.2em;
  display: flex;
  flex-direction: column;
  gap: 1.2em;
}

.page-header,
.list-header,
.tag-content,
.row-actions,
.create-form {
  display: flex;
  align-items: center;
  gap: 0.8em;
}

.page-header,
.list-header,
.tag-content {
  justify-content: space-between;
}

h1,
h2,
.page-header p,
.assigned-count {
  margin: 0;
}

h2 {
  font-size: 1.05rem;
}

.page-header > div {
  display: flex;
  flex-direction: column;
  gap: 0.4em;
}

.page-header > div p,
.assigned-count,
.empty {
  color: var(--color-text-muted);
}

.active-count {
  flex-shrink: 0;
  padding: 0.55em 0.8em;
  border-radius: 999px;
  background: var(--color-surface-muted);
  font-variant-numeric: tabular-nums;
}

.active-count strong {
  color: var(--color-text);
}

.create-section,
.list-section {
  display: flex;
  flex-direction: column;
  gap: 0.9em;
  padding: 1em;
  border: 1px solid var(--color-surface-muted);
  border-radius: 8px;
  background: var(--color-surface);
}

.create-form {
  align-items: flex-start;
}

.create-input {
  flex: 1;
}

.create-form > :last-child {
  margin-top: 1.55em;
}

.archive-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.45em;
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
}

.archive-toggle input:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.tag-list {
  display: flex;
  flex-direction: column;
  gap: 0.65em;
  margin: 0;
  padding: 0;
  list-style: none;
}

.tag-row {
  display: flex;
  flex-direction: column;
  gap: 0.75em;
  padding: 0.85em;
  border: 1px solid var(--color-surface-muted);
  border-radius: 8px;
}

.tag-content {
  min-width: 0;
}

.tag-name-status {
  display: flex;
  align-items: center;
  gap: 0.55em;
  min-width: 0;
  flex: 1;
}

.tag-name {
  overflow-wrap: anywhere;
  font-weight: 700;
}

.archived-badge {
  flex-shrink: 0;
  padding: 0.15em 0.5em;
  border: 1px solid var(--color-text-muted);
  border-radius: 999px;
  color: var(--color-text-muted);
  font-size: 0.75rem;
}

.assigned-count {
  min-width: 7em;
  text-align: right;
  font-size: 0.9rem;
}

.assigned-count strong {
  color: var(--color-text);
  font-variant-numeric: tabular-nums;
}

.row-actions {
  justify-content: flex-end;
  flex-wrap: wrap;
}

.rename-form {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 0.8em;
}

@media (max-width: 680px) {
  .tag-management-view {
    padding: 0.9em 0.75em;
  }

  .page-header,
  .list-header,
  .tag-content,
  .create-form {
    align-items: stretch;
    flex-direction: column;
  }

  .active-count {
    align-self: flex-start;
  }

  .create-form > :last-child {
    margin-top: 0;
  }

  .assigned-count {
    text-align: left;
  }

  .row-actions > * {
    flex: 1;
  }

  .rename-form {
    grid-template-columns: 1fr;
  }
}
</style>
