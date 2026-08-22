// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as workSessionsApi from '@/api/workSessionsApi'
import type { ActiveTimer } from '@/types/workSession'
import ActiveTimerMenu from './ActiveTimerMenu.vue'

vi.mock('@/api/workSessionsApi')

const now = new Date('2026-08-18T15:00:00+09:00')
const timers: ActiveTimer[] = [
  {
    sessionId: 'ws1',
    taskId: 'task10',
    taskTitle: '通常のタイマー',
    projectId: 'p2',
    taskGroupId: null,
    startedAt: '2026-08-18T14:00:00+09:00',
  },
  {
    sessionId: 'ws2',
    taskId: 'task20',
    taskTitle: '停止忘れのタイマー',
    projectId: 'p3',
    taskGroupId: 'tg4',
    startedAt: '2026-08-18T09:30:00+09:00',
  },
]

function mountMenu() {
  return mount(ActiveTimerMenu, {
    attachTo: document.body,
    global: {
      plugins: [createPinia()],
      stubs: {
        TaskQuickActionModal: {
          props: ['taskId', 'taskTitle', 'detailTo'],
          template:
            '<div class="quick-modal-stub" :data-task-id="taskId" :data-detail-to="detailTo">{{ taskTitle }}</div>',
        },
      },
    },
  })
}

describe('ActiveTimerMenu', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(now)
    vi.mocked(workSessionsApi.fetchActiveTimers).mockResolvedValue({ data: timers } as never)
  })

  afterEach(() => {
    vi.useRealTimers()
    document.body.innerHTML = ''
  })

  it('数値のないバッジを表示し、5時間以上のタイマーを警告する', async () => {
    const wrapper = mountMenu()
    await flushPromises()

    const trigger = wrapper.get('.timer-menu-trigger')
    expect(trigger.attributes('aria-label')).toBe(
      '稼働中タイマーあり、5時間以上連続稼働中のタイマーがあります',
    )
    const badge = wrapper.get('.timer-badge')
    expect(badge.text()).toBe('')
    expect(badge.classes()).toContain('danger')

    await trigger.trigger('click')
    await flushPromises()

    const items = wrapper.findAll('.timer-item')
    expect(items).toHaveLength(2)
    expect(items[0].text()).toContain('停止忘れのタイマー')
    expect(items[0].text()).toContain('05:30:00')
    expect(items[0].text()).toContain('5時間以上連続で稼働しています')
    expect(items[0].classes()).toContain('warning')
    wrapper.unmount()
  })

  it('5時間未満の稼働中タイマーは数値のないオレンジバッジで示す', async () => {
    vi.mocked(workSessionsApi.fetchActiveTimers).mockResolvedValue({ data: [timers[0]] } as never)
    const wrapper = mountMenu()
    await flushPromises()

    expect(wrapper.get('.timer-menu-trigger').attributes('aria-label')).toBe('稼働中タイマーあり')
    const badge = wrapper.get('.timer-badge')
    expect(badge.text()).toBe('')
    expect(badge.classes()).not.toContain('danger')
    wrapper.unmount()
  })

  it('一覧のタイマーから該当タスクのクイック操作モーダルを開く', async () => {
    const wrapper = mountMenu()
    await flushPromises()
    await wrapper.get('.timer-menu-trigger').trigger('click')
    await flushPromises()

    await wrapper.findAll('.timer-item')[0].trigger('click')

    const modal = wrapper.get('.quick-modal-stub')
    expect(modal.text()).toBe('停止忘れのタイマー')
    expect(modal.attributes('data-task-id')).toBe('task20')
    expect(modal.attributes('data-detail-to')).toBe('/projects/p3/task-groups/tg4/tasks/task20')
    expect(wrapper.find('.timer-panel').exists()).toBe(false)
    wrapper.unmount()
  })
})
