import { defineStore } from 'pinia'
import type { ChapterId } from '@/tutorial/types'

export const useTutorialStore = defineStore('tutorial', {
  state: () => ({
    activeChapterId: null as ChapterId | null,
    stepIndex: 0,
    mode: null as 'tour' | 'replay' | null,
    // セッション内の初回ツアー再発火抑止（要件 §6.2）。モジュール変数ではなくストアの
    // 状態にするのは、テストからリセットできるようにするため。
    tourAttempted: false,
  }),
  getters: {
    isActive: (state) => state.activeChapterId !== null,
  },
  actions: {
    start(chapterId: ChapterId, mode: 'tour' | 'replay') {
      this.activeChapterId = chapterId
      this.stepIndex = 0
      this.mode = mode
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
    },
  },
})
