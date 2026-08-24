import type { RouteLocationRaw } from 'vue-router'
import type { TutorialScope } from './scopes'

export type ChapterId = 'intro' | 'tasks' | 'reflections' | 'analytics' | 'tags'

export interface TutorialStep {
  id: string
  title: string
  body: string
  targets?: string[]
  onMissing?: 'center' | 'skip'
  // 指定すると、列挙した画面から再生したときだけ表示する。
  // アンカーが複数画面に存在する(ヘッダー常駐の要素など)が、説明として意味を持つ画面は
  // 限られる場合に使う。未指定ならアンカーが解決できる画面すべてで表示する。
  scopes?: TutorialScope[]
  before?: () => Promise<void> | void
  after?: () => Promise<void> | void
}

export interface TutorialChapter {
  id: ChapterId
  title: string
  summary: string
  entryRoute: RouteLocationRaw
  replayable: boolean
  steps: TutorialStep[]
}
