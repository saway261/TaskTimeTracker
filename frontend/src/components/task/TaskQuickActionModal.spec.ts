// @vitest-environment jsdom

import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as tasksApi from '@/api/tasksApi'
import * as workSessionsApi from '@/api/workSessionsApi'
import * as reflectionsApi from '@/api/reflectionsApi'
import type { TaskResponse } from '@/types/task'
import type { WorkSession } from '@/types/workSession'
import TaskQuickActionModal from './TaskQuickActionModal.vue'

vi.mock('@/api/tasksApi')
vi.mock('@/api/workSessionsApi')
vi.mock('@/api/reflectionsApi')

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
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(TaskQuickActionModal, {
    props: {
      modelValue: true,
      taskId: task.id,
      taskTitle: task.title,
      projectId: 1,
      detailTo: `/projects/1/tasks/${task.id}`,
    },
    global: {
      plugins: [pinia],
      stubs: {
        Teleport: true,
        WorkTimer: { template: '<div class="work-timer-stub" />' },
        WorkSessionList: { template: '<div class="work-session-list-stub" />' },
        ManualWorkSessionForm: { template: '<div class="manual-form-stub" />' },
        MemoList: { template: '<div class="memo-list-stub" />' },
        CauseCategorySelect: { template: '<div class="cause-category-select-stub" />' },
        EstimateOutcomeIcon: true,
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
    vi.mocked(reflectionsApi.create).mockResolvedValue({ data: {} } as never)
    vi.mocked(reflectionsApi.update).mockResolvedValue({ data: {} } as never)
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

  it('モーダルからタスクを完了にでき、クイック振り返りモーダルが開く', async () => {
    vi.mocked(workSessionsApi.fetchAllInTask).mockResolvedValue({ data: [] } as never)
    vi.mocked(tasksApi.updateFinished).mockResolvedValue({
      data: { ...task, finishedAt: '2026-08-15T02:00:00' },
    } as never)
    const wrapper = mountModal()
    await flushPromises()

    await wrapper.get('.task-state input[type="checkbox"]').setValue(true)
    await flushPromises()

    expect(tasksApi.updateFinished).toHaveBeenCalledWith(task.id, { isFinished: true })
    expect(wrapper.text()).toContain('振り返りを入力')
    expect(wrapper.text()).toContain('後で入力する場合は✖ボタンで閉じてください')
  })

  it('クイック振り返りモーダルを閉じるとタスクモーダルごと閉じる', async () => {
    vi.mocked(workSessionsApi.fetchAllInTask).mockResolvedValue({ data: [] } as never)
    vi.mocked(tasksApi.updateFinished).mockResolvedValue({
      data: { ...task, finishedAt: '2026-08-15T02:00:00' },
    } as never)
    const wrapper = mountModal()
    await flushPromises()

    await wrapper.get('.task-state input[type="checkbox"]').setValue(true)
    await flushPromises()

    const closeButtons = wrapper.findAll('.close-button')
    expect(closeButtons.length).toBe(2)
    await closeButtons[closeButtons.length - 1].trigger('click')

    expect(wrapper.emitted('update:modelValue')).toContainEqual([false])
  })

  it('完了済みタスクのチェックを外すと確認ダイアログが表示され、同意すると未完了に戻ること', async () => {
    const finishedTask = { ...task, finishedAt: '2026-08-15T02:00:00' }
    vi.mocked(tasksApi.fetchById).mockResolvedValue({ data: finishedTask } as never)
    vi.mocked(tasksApi.updateFinished).mockResolvedValue({
      data: { ...task, finishedAt: null },
    } as never)
    const wrapper = mountModal()
    await flushPromises()

    await wrapper.get('.task-state input[type="checkbox"]').setValue(false)
    await flushPromises()

    expect(tasksApi.updateFinished).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('保存済みの振り返りがある場合は削除され')

    const confirmButton = wrapper
      .findAll('button')
      .find((button) => button.text() === '作業中に戻す')
    await confirmButton?.trigger('click')
    await flushPromises()

    expect(tasksApi.updateFinished).toHaveBeenCalledWith(task.id, { isFinished: false })
  })

  it('完了済みタスクでは詳細リンクの下から登録済みの振り返りを開ける', async () => {
    const finishedTask = {
      ...task,
      finishedAt: '2026-08-15T02:00:00',
      actualMinutesCached: 25,
      gapMinutesCached: -35,
      gapRateCached: -58.3,
    }
    vi.mocked(tasksApi.fetchById).mockResolvedValue({ data: finishedTask } as never)
    vi.mocked(reflectionsApi.fetchOverview).mockResolvedValue({
      data: {
        projectId: 1,
        projectTitle: 'プロジェクト',
        tasks: [
          {
            id: task.id,
            title: task.title,
            finishedAt: finishedTask.finishedAt,
            actualMinutesCached: 25,
            gapMinutesCached: -35,
            gapRateCached: -58.3,
            reflection: {
              id: 1,
              taskId: task.id,
              causeCategories: [],
              cause: '確認不足',
              nextAction: '手順を見直す',
              createdAt: '2026-08-15T03:00:00',
              updatedAt: '2026-08-15T03:00:00',
            },
          },
        ],
        taskGroups: [],
      },
    } as never)
    const wrapper = mountModal()
    await flushPromises()

    const footer = wrapper.get('.modal-footer')
    const detailLink = footer.get('.detail-link')
    const reflectionButton = footer.get('.reflection-button')
    expect(detailLink.element.compareDocumentPosition(reflectionButton.element)).toBe(
      Node.DOCUMENT_POSITION_FOLLOWING,
    )

    await reflectionButton.trigger('click')
    await flushPromises()

    expect(reflectionsApi.fetchOverview).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('振り返りの詳細・変更')
  })
})
