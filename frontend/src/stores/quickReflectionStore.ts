import { defineStore } from 'pinia'
import type { ReflectionTaskResponse } from '@/types/reflection'

// タスクを完了にした直後に開く「クイック振り返り」の対象を保持する。
//
// 振り返りモーダルの実体をアプリ直下（App.vue）に1つだけ置き、各画面からはこのストア経由で
// 開かせる。以前はタスク操作モーダルの内部に置いていたが、そのモーダルは一覧の行
// （TaskListItem）の子であり、完了した瞬間にタスクが一覧の絞り込みから外れて行ごと
// アンマウントされるため、振り返りモーダルまで道連れで消えていた。
//
// アプリ直下に置くことで、どの画面から完了しても画面遷移は発生せず、モーダルを閉じれば
// 操作前に見ていた画面がそのまま残る。
export const useQuickReflectionStore = defineStore('quickReflection', {
  state: () => ({
    task: null as ReflectionTaskResponse | null,
  }),
  getters: {
    isOpen: (state) => state.task !== null,
  },
  actions: {
    open(task: ReflectionTaskResponse) {
      this.task = task
    },
    close() {
      this.task = null
    },
  },
})
