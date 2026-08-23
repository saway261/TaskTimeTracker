import { defineStore } from 'pinia'
import type { ChapterId } from '@/tutorial/types'

export const useTutorialStore = defineStore('tutorial', {
  state: () => ({
    activeChapterId: null as ChapterId | null,
    stepIndex: 0,
    mode: null as 'tour' | 'replay' | null,
    // 再生を「今いる画面にある要素の説明」だけに絞り込むためのCSSセレクタ。
    // ヘルプボタンからの再生時に、その画面のルート要素を指定する(要件 §7.2)。
    // nullなら絞り込まず章全体を再生する(章選択モーダル・初回ツアー)。
    scopeSelector: null as string | null,
    // セッション内の初回ツアー再発火抑止（要件 §6.2）。モジュール変数ではなくストアの
    // 状態にするのは、テストからリセットできるようにするため。
    tourAttempted: false,
  }),
  getters: {
    isActive: (state) => state.activeChapterId !== null,
  },
  actions: {
    start(chapterId: ChapterId, mode: 'tour' | 'replay', scopeSelector: string | null = null) {
      this.activeChapterId = chapterId
      this.stepIndex = 0
      this.mode = mode
      this.scopeSelector = scopeSelector
      if (mode === 'tour') {
        this.tourAttempted = true
      }
    },

    // 任意のインデックスへ移動する。前進・後退いずれも1つ隣とは限らない
    // (onMissing:'skip' のステップを飛ばした先へ直接移動するため。TutorialHost.vue参照)。
    goTo(index: number) {
      this.stepIndex = index
    },

    end() {
      this.activeChapterId = null
      this.stepIndex = 0
      this.mode = null
      this.scopeSelector = null
    },
  },
})
