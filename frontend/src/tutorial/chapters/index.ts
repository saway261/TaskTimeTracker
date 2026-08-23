import type { ChapterId, TutorialChapter } from '@/tutorial/types'

// 章の内容はフェーズF4〜F6で追加する(タスク管理・振り返り・分析・タグ管理・はじめに)。
// ここではApp.vueがactiveChapterIdから章データを引けるようにする土台だけを用意する。
export const chapters: TutorialChapter[] = []

export function findChapter(id: ChapterId): TutorialChapter | undefined {
  return chapters.find((chapter) => chapter.id === id)
}
