import type { RouteLocationRaw } from 'vue-router'

export type ChapterId = 'intro' | 'tasks' | 'reflections' | 'analytics' | 'tags'

export interface TutorialStep {
  id: string
  title: string
  body: string
  targets?: string[]
  onMissing?: 'center' | 'skip'
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
