import type { ChapterId, TutorialChapter } from '@/tutorial/types'
import { tasksChapter } from './tasks'
import { reflectionsChapter } from './reflections'
import { analyticsChapter } from './analytics'
import { tagsChapter } from './tags'

// 残りの章(はじめに)はフェーズF6で追加する。
export const chapters: TutorialChapter[] = [
  tasksChapter,
  reflectionsChapter,
  analyticsChapter,
  tagsChapter,
]

export function findChapter(id: ChapterId): TutorialChapter | undefined {
  return chapters.find((chapter) => chapter.id === id)
}
