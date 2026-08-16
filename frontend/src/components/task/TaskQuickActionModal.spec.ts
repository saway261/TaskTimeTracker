// @vitest-environment jsdom

import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as tasksApi from '@/api/tasksApi'
import * as workSessionsApi from '@/api/workSessionsApi'
import type { TaskResponse } from '@/types/task'
import type { WorkSession } from '@/types/workSession'
import TaskQuickActionModal from './TaskQuickActionModal.vue'

vi.mock('@/api/tasksApi')
vi.mock('@/api/workSessionsApi')

const task: TaskResponse = {
  id: 10,
  projectId: 1,
  taskGroupId: null,
  title: '実装する',
  description: null,
  estimatedMinutes: 60,
  createdAt: '2026-08-15T00:00:00',
  finishedAt: null,
  actualMinutesCached: null,
  gapMinutesCached: null,
  gapRateCached: null,
  memos: [],
}

const pastSession: WorkSession = {
  id: 1,
  taskId: task.id,
  minutes: 25,
  startedAt: null,
  endedAt: null,
  createdAt: '2026-08-15T00:00:00',
  updatedAt: '2026-08-15T00:00:00',
  type: 'MANUAL',
}

const activeSession: WorkSession = {
  ...pastSession,
  id: 2,
  minutes: null,
  startedAt: '2026-08-15T01:00:00',
  type: 'TIMER',
}

function mountModal() {
  return mount(TaskQuickActionModal, {
    props: {
      modelValue: true,
      taskId: task.id,
      taskTitle: task.title,
      detailTo: `/projects/1/tasks/${task.id}`,
    },
    global: {
      plugins: [createPinia()],
      stubs: {
        Teleport: true,
        WorkTimer: { template: '<div class="work-timer-stub" />' },
        WorkSessionList: { template: '<div class="work-session-list-stub" />' },
        ManualWorkSessionForm: { template: '<div class="manual-form-stub" />' },
        MemoList: { template: '<div class="memo-list-stub" />' },
        RouterLink: {
          props: ['to'],
          template: '<a :href="to"><slot /></a>',
        },
      },
    },
  })
}

describe('TaskQuickActionModal', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(tasksApi.fetchById).mockResolvedValue({ data: task } as never)
    vi.mocked(workSessionsApi.fetchAllInTask).mockResolvedValue({
      data: [pastSession, activeSession],
    } as never)
    vi.mocked(workSessionsApi.fetchTotalMinutes).mockResolvedValue({ data: 25 } as never)
  })

  it('基本操作と詳細画面へのリンクを表示し、過去の記録を折りたたむ', async () => {
    const wrapper = mountModal()
    await flushPromises()

    expect(wrapper.find('.work-timer-stub').exists()).toBe(true)
    expect(wrapper.find('.manual-form-stub').exists()).toBe(true)
    expect(wrapper.find('.memo-list-stub').exists()).toBe(true)

    const metrics = wrapper.get('.metrics-section')
    expect(metrics.text()).toContain('見積との差')
    expect(metrics.text()).not.toContain('実績')
    expect(metrics.text()).not.toContain('誤差率')
    expect(metrics.element.compareDocumentPosition(wrapper.get('.timer-section').element)).toBe(
      Node.DOCUMENT_POSITION_FOLLOWING,
    )

    const history = wrapper.get('details.session-history')
    expect(history.attributes('open')).toBeUndefined()
    expect(history.get('summary').text()).toContain('過去の作業記録（1件）')

    const detailLink = wrapper.get('.detail-link')
    expect(detailLink.attributes('href')).toBe(`/projects/1/tasks/${task.id}`)
  })

  it('モーダルからタスクを完了にできる', async () => {
    vi.mocked(workSessionsApi.fetchAllInTask).mockResolvedValue({ data: [] } as never)
    vi.mocked(tasksApi.updateFinished).mockResolvedValue({
      data: { ...task, finishedAt: '2026-08-15T02:00:00' },
    } as never)
    const wrapper = mountModal()
    await flushPromises()

    await wrapper.get('.finish-action button').trigger('click')
    await flushPromises()

    expect(tasksApi.updateFinished).toHaveBeenCalledWith(task.id, { isFinished: true })
    expect(wrapper.get('.status').text()).toBe('完了')
    expect(wrapper.find('.finish-action').exists()).toBe(false)
  })
})
