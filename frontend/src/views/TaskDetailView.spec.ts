// @vitest-environment jsdom

import { flushPromises, shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as projectsApi from '@/api/projectsApi'
import * as tagsApi from '@/api/tagsApi'
import * as tasksApi from '@/api/tasksApi'
import * as workSessionsApi from '@/api/workSessionsApi'
import type { TaskResponse } from '@/types/task'
import TaskDetailView from './TaskDetailView.vue'

vi.mock('@/api/projectsApi')
vi.mock('@/api/tagsApi')
vi.mock('@/api/tasksApi')
vi.mock('@/api/workSessionsApi')

const completedTask: TaskResponse = {
  id: 10,
  projectId: 1,
  taskGroupId: null,
  title: '完了済みタスク',
  description: null,
  estimatedMinutes: 30,
  createdAt: '2026-08-22T00:00:00',
  finishedAt: '2026-08-22T01:00:00',
  actualMinutesCached: 30,
  gapMinutesCached: 0,
  gapRateCached: 0,
  memos: [],
  tags: [{ id: 1, name: '調査' }],
}

describe('TaskDetailView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('完了済みタスクでもタグを編集して専用APIで更新できる', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div />' } }],
    })
    await router.push('/')
    await router.isReady()
    vi.mocked(tasksApi.fetchById).mockResolvedValue({ data: completedTask } as never)
    vi.mocked(tasksApi.updateTags).mockResolvedValue({
      data: { ...completedTask, tags: [...completedTask.tags, { id: 2, name: '設計' }] },
    } as never)
    vi.mocked(projectsApi.fetchById).mockResolvedValue({
      data: { id: 1, title: 'プロジェクト', description: null, isFinished: false, memos: [] },
    } as never)
    vi.mocked(workSessionsApi.fetchAllInTask).mockResolvedValue({ data: [] } as never)
    vi.mocked(tagsApi.fetchAll).mockResolvedValue({
      data: [
        { id: 1, name: '調査', isArchived: false, assignedTaskCount: 1 },
        { id: 2, name: '設計', isArchived: false, assignedTaskCount: 0 },
      ],
    } as never)
    const wrapper = shallowMount(TaskDetailView, {
      props: { projectId: '1', taskId: '10', taskGroupId: null },
      global: {
        plugins: [pinia, router],
        stubs: {
          BaseButton: false,
          ErrorMessage: false,
          TagBadgeList: false,
          TagSelect: false,
        },
      },
    })
    await flushPromises()

    expect(wrapper.get('.tag-section').text()).toContain('調査')
    await wrapper.get('.tag-section .secondary').trigger('click')
    await wrapper.get('.tag-select input').trigger('focus')
    await wrapper.get('.tag-select .suggestion').trigger('click')
    await wrapper.get('.tag-form').trigger('submit')
    await flushPromises()

    expect(tasksApi.updateTags).toHaveBeenCalledWith(10, { tagIds: [1, 2] })
    expect(wrapper.get('.tag-section').text()).toContain('設計')
  })
})
