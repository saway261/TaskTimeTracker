// @vitest-environment jsdom

import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { computed, defineComponent, h } from 'vue'
import * as tasksApi from '@/api/tasksApi'
import * as workSessionsApi from '@/api/workSessionsApi'
import { useQuickReflectionStore } from '@/stores/quickReflectionStore'
import { useTaskStore } from '@/stores/taskStore'
import { isFinished } from '@/utils/task'
import type { TaskResponse } from '@/types/task'
import TaskListItem from './TaskListItem.vue'

vi.mock('@/api/tasksApi')
vi.mock('@/api/workSessionsApi')

const task: TaskResponse = {
  id: 'task10',
  projectId: 'p1',
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
  tags: [
    { id: 'tag1', name: '調査' },
    { id: 'tag2', name: '設計' },
    { id: 'tag3', name: '実装' },
    { id: 'tag4', name: '検証' },
    { id: 'tag5', name: '改善' },
  ],
}

describe('TaskListItem', () => {
  it('タスク行を押すと詳細遷移ではなく操作モーダルを開く', async () => {
    const wrapper = mount(TaskListItem, {
      props: {
        task,
        to: `/projects/p1/tasks/${task.id}`,
        projectId: 'p1',
        containerKey: 'project:p1',
        taskGroups: [],
        canMoveUp: false,
        canMoveDown: false,
      },
      global: {
        plugins: [createPinia()],
        stubs: {
          TaskRowMenu: {
            props: ['modelValue', 'detailTo'],
            template:
              '<div v-if="modelValue" class="task-row-menu-stub" :data-detail-to="detailTo" />',
          },
          TaskQuickActionModal: {
            props: ['modelValue'],
            template: '<div v-if="modelValue" class="quick-action-modal-stub" />',
          },
        },
      },
    })

    expect(wrapper.get('.task-row').element.tagName).toBe('BUTTON')
    expect(wrapper.find('.quick-action-modal-stub').exists()).toBe(false)
    expect(wrapper.findAll('.tag-badge').map((badge) => badge.text())).toEqual([
      'タグ 調査',
      'タグ 設計',
      'タグ 実装',
    ])
    expect(wrapper.get('.remaining-badge').text()).toBe('他2件')

    await wrapper.get('.task-row').trigger('click')

    expect(wrapper.find('.quick-action-modal-stub').exists()).toBe(true)
  })

  it('ケバブボタンを押すとタスク詳細の遷移先を渡した操作メニューを開く', async () => {
    const detailTo = `/projects/p1/tasks/${task.id}`
    const wrapper = mount(TaskListItem, {
      props: {
        task,
        to: detailTo,
        projectId: 'p1',
        containerKey: 'project:p1',
        taskGroups: [],
        canMoveUp: false,
        canMoveDown: false,
      },
      global: {
        plugins: [createPinia()],
        stubs: {
          TaskRowMenu: {
            props: ['modelValue', 'detailTo'],
            template:
              '<div v-if="modelValue" class="task-row-menu-stub" :data-detail-to="detailTo" />',
          },
          TaskQuickActionModal: true,
        },
      },
    })

    expect(wrapper.find('.task-row-menu-stub').exists()).toBe(false)

    await wrapper.get('.menu-button').trigger('click')

    expect(wrapper.get('.task-row-menu-stub').attributes('data-detail-to')).toBe(detailTo)
  })
})

// 実際に報告された不具合の回帰テスト。
// 振り返りモーダルをタスク操作モーダル（＝この行の子）の中に置いていたころは、完了にした
// 瞬間にタスクが一覧の絞り込みから外れて行ごとアンマウントされ、振り返りモーダルまで
// 道連れで消えていた。モーダルの実体をアプリ直下へ移したことで消えなくなる。
describe('TaskListItem（完了して行が一覧から消えるとき）', () => {
  function mountFilteredList(stubQuickActionModal = true) {
    const pinia = createPinia()
    setActivePinia(pinia)
    // ProjectDetailView の visibleOrderedItems を最小構成で再現する。
    // 「完了を表示」OFF（既定）なので、完了したタスクは一覧から外れる。
    const host = defineComponent({
      setup() {
        const taskStore = useTaskStore()
        const visibleTasks = computed(() =>
          taskStore.tasks.filter((t) => !isFinished(t as TaskResponse)),
        )
        return () =>
          h(
            'div',
            visibleTasks.value.map((t) =>
              h(TaskListItem, {
                key: t.id,
                task: t as TaskResponse,
                to: `/projects/p1/tasks/${t.id}`,
                projectId: 'p1',
                containerKey: 'project:p1',
                taskGroups: [],
                canMoveUp: false,
                canMoveDown: false,
              }),
            ),
          )
      },
    })

    useTaskStore().tasks = [{ ...task }]
    return mount(host, {
      global: {
        plugins: [pinia],
        stubs: {
          ...(stubQuickActionModal ? { TaskQuickActionModal: true } : {}),
          WorkTimer: { template: '<div />' },
          WorkSessionList: { template: '<div />' },
          ManualWorkSessionForm: { template: '<div />' },
          MemoList: { template: '<div />' },
          RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
          BaseModal: {
            props: ['modelValue'],
            template: '<div v-if="modelValue"><slot /></div>',
          },
        },
      },
    })
  }

  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(tasksApi.updateFinished).mockResolvedValue({
      data: { ...task, finishedAt: '2026-08-15T02:00:00' },
    } as never)
    vi.mocked(tasksApi.fetchById).mockResolvedValue({ data: task } as never)
    vi.mocked(workSessionsApi.fetchAllInTask).mockResolvedValue({ data: [] } as never)
    vi.mocked(workSessionsApi.fetchTotalMinutes).mockResolvedValue({ data: 0 } as never)
  })

  it('ケバブから完了にすると、行が消えても振り返りの対象がストアに残る', async () => {
    const wrapper = mountFilteredList()

    await wrapper.get('.menu-button').trigger('click')
    const finishButton = wrapper.findAll('button').find((b) => b.text() === '完了にする')
    await finishButton!.trigger('click')
    await flushPromises()

    // 行は一覧から外れて消える。
    expect(wrapper.find('.task-row-wrapper').exists()).toBe(false)
    // それでも振り返りの対象はアプリ直下のストアに残っているため、モーダルは開いたままになる。
    const quickReflection = useQuickReflectionStore()
    expect(quickReflection.task).toMatchObject({ id: task.id, title: task.title })
    expect(quickReflection.isOpen).toBe(true)
  })

  it('タスク操作モーダルから完了にすると、行が消えても振り返りの対象がストアに残る', async () => {
    const wrapper = mountFilteredList(false)
    await flushPromises()

    await wrapper.get('.task-row').trigger('click')
    await flushPromises()

    await wrapper.get('.task-state input[type="checkbox"]').setValue(true)
    await flushPromises()

    expect(wrapper.find('.task-row-wrapper').exists()).toBe(false)
    const quickReflection = useQuickReflectionStore()
    expect(quickReflection.task).toMatchObject({ id: task.id, title: task.title })
    expect(quickReflection.isOpen).toBe(true)
  })
})
