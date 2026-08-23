import type { ChapterId, TutorialChapter } from '@/tutorial/types'
import { tasksChapter } from './tasks'
import { reflectionsChapter } from './reflections'
import { analyticsChapter } from './analytics'
import { tagsChapter } from './tags'
import { introChapter } from './intro'

export const chapters: TutorialChapter[] = [
  introChapter,
  tasksChapter,
  reflectionsChapter,
  analyticsChapter,
  tagsChapter,
]

export function findChapter(id: ChapterId): TutorialChapter | undefined {
  return chapters.find((chapter) => chapter.id === id)
}
