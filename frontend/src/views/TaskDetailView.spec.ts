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
  id: 'task10',
  projectId: 'p1',
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
  tags: [{ id: 'tag1', name: '調査' }],
}

describe('TaskDetailView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('完了済みタスクでもタグをその場で追加・削除できる', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div />' } }],
    })
    await router.push('/')
    await router.isReady()
    vi.mocked(tasksApi.fetchById).mockResolvedValue({ data: completedTask } as never)
    vi.mocked(tasksApi.updateTags).mockImplementation((_taskId, request) =>
      Promise.resolve({
        data: {
          ...completedTask,
          tags: request.tagIds.map((id) => ({ id, name: id === 'tag1' ? '調査' : '設計' })),
        },
      } as never),
    )
    vi.mocked(projectsApi.fetchById).mockResolvedValue({
      data: { id: 'p1', title: 'プロジェクト', description: null, isFinished: false, memos: [] },
    } as never)
    vi.mocked(workSessionsApi.fetchAllInTask).mockResolvedValue({ data: [] } as never)
    vi.mocked(tagsApi.fetchAll).mockResolvedValue({
      data: [
        { id: 'tag1', name: '調査', isArchived: false, assignedTaskCount: 1 },
        { id: 'tag2', name: '設計', isArchived: false, assignedTaskCount: 0 },
      ],
    } as never)
    const wrapper = shallowMount(TaskDetailView, {
      props: { projectId: 'p1', taskId: 'task10', taskGroupId: null },
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
    expect(wrapper.find('.tag-section h2').exists()).toBe(false)
    expect(wrapper.get('.tag-add-button').text()).toContain('タグを追加')
    expect(wrapper.find('.tag-add-button .tag-icon').exists()).toBe(true)
    expect(wrapper.find('[aria-label="調査を外す"]').exists()).toBe(true)

    await wrapper.get('.tag-add-button').trigger('click')
    await flushPromises()
    expect(wrapper.get('.tag-select label').text()).toBe('追加するタグ')
    await wrapper.get('.tag-select input').trigger('focus')
    await wrapper.get('.tag-select .suggestion').trigger('click')
    await flushPromises()

    expect(tasksApi.updateTags).toHaveBeenCalledWith('task10', { tagIds: ['tag1', 'tag2'] })
    expect(wrapper.get('.tag-section').text()).toContain('設計')
    expect(wrapper.find('.tag-select .selected-tags').exists()).toBe(false)

    await wrapper.get('[aria-label="調査を外す"]').trigger('click')
    await flushPromises()

    expect(tasksApi.updateTags).toHaveBeenLastCalledWith('task10', { tagIds: ['tag2'] })
    expect(wrapper.find('[aria-label="調査を外す"]').exists()).toBe(false)
  })
})
