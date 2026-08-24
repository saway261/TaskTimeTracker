<script setup lang="ts">
import { useTutorialStore } from '@/stores/tutorialStore'
import type { TutorialScope } from '@/tutorial/scopes'
import type { ChapterId } from '@/tutorial/types'

// chapterTitleを明示的にpropsで受け取るのは、chapters/(章の本文)を直接importしないため。
// このボタンは各画面へ常時表示される(遅延読み込みされない)ので、ここでchaptersをimportすると
// 本文がすべて本体バンドルへ含まれてしまう(要件 §11、実装計画 §0-2-8と同じ理由)。
const props = defineProps<{
  chapterId: ChapterId
  chapterTitle: string
  // 今いる画面のルート要素を指すCSSセレクタ。この配下(とヘッダー)にある要素を指す
  // ステップだけが再生される(要件 §7.2)。省略すると章全体を再生する。
  scope?: TutorialScope
}>()

const tutorialStore = useTutorialStore()

// 画面遷移は行わない。今いる画面に対応する章をその場で再生する(要件 §7.2)。
function open() {
  tutorialStore.start(props.chapterId, 'replay', props.scope ?? null)
}
</script>

<template>
  <button
    type="button"
    class="help-button"
    :aria-label="`${chapterTitle}のチュートリアルを見る`"
    @click="open"
  >
    ?
  </button>
</template>

<style scoped>
.help-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.5rem;
  height: 1.5rem;
  margin-left: 0.4em;
  padding: 0;
  border-radius: 50%;
  border: 1px solid var(--color-text-muted);
  background-color: transparent;
  color: var(--color-text-muted);
  font-size: 0.85rem;
  font-weight: 600;
  line-height: 1;
  vertical-align: middle;
  cursor: pointer;
}

.help-button:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.help-button:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}
</style>
