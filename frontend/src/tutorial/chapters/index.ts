import type { ChapterId, TutorialChapter } from '@/tutorial/types'
import { tasksChapter } from './tasks'

// 残りの章(振り返り・分析・タグ管理・はじめに)はフェーズF5〜F6で追加する。
export const chapters: TutorialChapter[] = [tasksChapter]

export function findChapter(id: ChapterId): TutorialChapter | undefined {
  return chapters.find((chapter) => chapter.id === id)
}
