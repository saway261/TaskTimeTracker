<script setup lang="ts">
import { computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import BaseModal from '@/components/common/BaseModal.vue'
import { useTutorialStore } from '@/stores/tutorialStore'
import { chapters } from '@/tutorial/chapters'
import type { ChapterId, TutorialChapter } from '@/tutorial/types'

// このモーダルはユーザーメニューから明示的に開いたときだけ読み込まれる(遅延読み込み)ため、
// chapters/(章の本文)を直接importしても本体バンドルへは含まれない。
defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const router = useRouter()
const tutorialStore = useTutorialStore()

// §1.1のループの順序で固定する。並び自体が説明になっている(要件 §7.3)。
// intro はここに含まれないため、一覧には自動的に現れない(replayable: falseでもある)。
const CHAPTER_ORDER: ChapterId[] = ['tasks', 'reflections', 'analytics', 'tags']

const replayableChapters = computed<TutorialChapter[]>(() =>
  CHAPTER_ORDER.map((id) => chapters.find((c) => c.id === id && c.replayable)).filter(
    (c): c is TutorialChapter => c !== undefined,
  ),
)

function close() {
  emit('update:modelValue', false)
}

// 選んだ章の起点画面へ遷移してから開始する。ヘルプボタンとはここが異なる(要件 §7.2・§7.3)。
async function selectChapter(chapter: TutorialChapter) {
  close()
  await router.push(chapter.entryRoute)
  await nextTick()
  tutorialStore.start(chapter.id, 'replay')
}
</script>

<template>
  <BaseModal :model-value="modelValue" title="チュートリアル" @update:model-value="close">
    <ul class="chapter-list">
      <li v-for="chapter in replayableChapters" :key="chapter.id">
        <button type="button" class="chapter-item" @click="selectChapter(chapter)">
          <span class="chapter-item-title">{{ chapter.title }}</span>
          <span class="chapter-item-summary">{{ chapter.summary }}</span>
        </button>
      </li>
    </ul>
  </BaseModal>
</template>

<style scoped>
.chapter-list {
  display: flex;
  flex-direction: column;
  gap: 0.5em;
  margin: 0;
  padding: 0;
  list-style: none;
}

.chapter-item {
  display: flex;
  flex-direction: column;
  gap: 0.2em;
  width: 100%;
  padding: 0.8em 1em;
  border: 1px solid var(--color-surface-muted);
  border-radius: 6px;
  background-color: transparent;
  text-align: left;
  cursor: pointer;
}

.chapter-item:hover {
  background-color: var(--color-surface-muted);
}

.chapter-item:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.chapter-item-title {
  font-weight: 600;
  color: var(--color-text);
}

.chapter-item-summary {
  font-size: 0.85rem;
  color: var(--color-text-muted);
}
</style>
