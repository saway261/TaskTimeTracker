// @vitest-environment jsdom

import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as tasksApi from '@/api/tasksApi'
import type { TaskResponse } from '@/types/task'
import { useTaskStore } from './taskStore'

vi.mock('@/api/tasksApi')

const task: TaskResponse = {
  id: 'task10',
  projectId: 'p1',
  taskGroupId: null,
  title: '実装する',
  description: null,
  estimatedMinutes: 60,
  createdAt: '2026-08-22T00:00:00',
  finishedAt: '2026-08-22T01:00:00',
  actualMinutesCached: 60,
  gapMinutesCached: 0,
  gapRateCached: 0,
  memos: [],
  tags: [],
}

describe('taskStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.resetAllMocks()
  })

  it('完了済みタスクでもタグ更新結果を詳細と一覧へ反映する', async () => {
    const store = useTaskStore()
    store.currentTask = task
    store.tasks = [task]
    const updated = { ...task, tags: [{ id: 'tag2', name: '設計' }] }
    vi.mocked(tasksApi.updateTags).mockResolvedValue({ data: updated } as never)

    await store.updateTaskTags(task.id, { tagIds: ['tag2'] })

    expect(tasksApi.updateTags).toHaveBeenCalledWith(task.id, { tagIds: ['tag2'] })
    expect(store.currentTask?.tags).toEqual(updated.tags)
    expect(store.tasks[0].tags).toEqual(updated.tags)
  })
})
