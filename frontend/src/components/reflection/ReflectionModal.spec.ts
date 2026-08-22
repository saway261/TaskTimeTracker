// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { useCauseCategoryStore } from '@/stores/causeCategoryStore'
import { useAppSettingsStore } from '@/stores/appSettingsStore'
import type { ReflectionTaskResponse } from '@/types/reflection'
import ReflectionModal from './ReflectionModal.vue'

const category = {
  code: 'TASK_BREAKDOWN',
  label: '作業の洗い出しが足りなかった',
  direction: 'OVER' as const,
  nextActionHint: '着手前に手順を書き出す',
  requiresCause: false,
}

const otherCategory = {
  code: 'OTHER',
  label: 'その他',
  direction: 'BOTH' as const,
  nextActionHint: null,
  requiresCause: true,
}

const baseTask: ReflectionTaskResponse = {
  id: 'task10',
  title: '画面実装',
  finishedAt: '2026-08-16T00:00:00',
  actualMinutesCached: 90,
  gapMinutesCached: 30,
  gapRateCached: 50,
  reflection: null,
}

function mountModal(task: ReflectionTaskResponse) {
  return mount(ReflectionModal, {
    props: { modelValue: true, task },
    global: {
      stubs: {
        BaseModal: { template: '<div><slot /></div>' },
        EstimateOutcomeIcon: true,
      },
    },
  })
}

function checkboxFor(wrapper: ReturnType<typeof mountModal>, code: string) {
  return wrapper.get(`input[value="${code}"]`)
}

describe('ReflectionModal', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    const store = useCauseCategoryStore()
    store.categories = [category, otherCategory]
    store.initialized = true
  })

  it('カテゴリを必須にし、送信内容へカテゴリコードを含める', async () => {
    const wrapper = mountModal(baseTask)
    const submitButton = wrapper.get('button[type="submit"]')

    await wrapper.get('textarea').setValue(' 手順の確認が不足していた ')
    expect(submitButton.attributes('disabled')).toBeDefined()

    await checkboxFor(wrapper, category.code).setValue(true)
    expect(submitButton.attributes('disabled')).toBeUndefined()
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')).toEqual([
      [
        {
          causeCategoryCodes: [category.code],
          cause: '手順の確認が不足していた',
          nextAction: null,
        },
      ],
    ])
  })

  it('原因カテゴリがrequiresCauseでなければ原因を未入力のまま保存できる', async () => {
    const wrapper = mountModal(baseTask)

    await checkboxFor(wrapper, category.code).setValue(true)
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeUndefined()
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')).toEqual([
      [{ causeCategoryCodes: [category.code], cause: null, nextAction: null }],
    ])
  })

  it('原因の記述を必須とするカテゴリを選ぶと保存できなくなり、外すと任意へ戻って入力済みの原因が残る', async () => {
    const wrapper = mountModal(baseTask)

    await checkboxFor(wrapper, otherCategory.code).setValue(true)
    expect(wrapper.text()).toContain('を選んだ場合は、原因の記述が必要です')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()

    await wrapper.get('textarea').setValue('入力した原因')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeUndefined()

    await checkboxFor(wrapper, otherCategory.code).setValue(false)
    await checkboxFor(wrapper, category.code).setValue(true)

    expect((wrapper.get('textarea').element as HTMLTextAreaElement).value).toBe('入力した原因')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeUndefined()
    expect(wrapper.text()).not.toContain('を選んだ場合は、原因の記述が必要です')
  })

  it('編集時は保存済みカテゴリを初期選択し、未分類の既存データは未選択にする', async () => {
    const reflection = {
      id: 1,
      taskId: baseTask.id,
      causeCategories: [{ code: category.code, label: category.label }],
      cause: '確認不足',
      nextAction: '手順を書き出す',
      createdAt: '2026-08-16T00:00:00',
      updatedAt: '2026-08-16T00:00:00',
    }
    const wrapper = mountModal({ ...baseTask, reflection })

    expect((checkboxFor(wrapper, category.code).element as HTMLInputElement).checked).toBe(true)

    await wrapper.setProps({
      modelValue: false,
      task: {
        ...baseTask,
        reflection: { ...reflection, causeCategories: [] },
      },
    })
    await wrapper.setProps({ modelValue: true })

    expect((checkboxFor(wrapper, category.code).element as HTMLInputElement).checked).toBe(false)
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()
  })

  it('タスク名をサマリー外の上部に表示し、サマリー内に見積時間を表示する', () => {
    const wrapper = mountModal(baseTask)

    expect(wrapper.get('.task-title').text()).toBe(baseTask.title)
    expect(wrapper.find('.reference-info').text()).not.toContain(baseTask.title)

    const summaryText = wrapper.get('.reference-info').text()
    expect(summaryText).toContain('見積時間')
    expect(summaryText).toContain('1時間')
  })

  it('ストアのしきい値をカテゴリの出し分けにも使用する', () => {
    useAppSettingsStore().onTimeThresholdPercent = 60
    const wrapper = mountModal(baseTask)

    expect(wrapper.get('.outcome-reference').classes()).toContain('on-time')
  })

  it('deferHintを指定すると案内文を表示し、キャンセルボタンは出さない', () => {
    const wrapper = mount(ReflectionModal, {
      props: { modelValue: true, task: baseTask, deferHint: true },
      global: {
        stubs: {
          BaseModal: { template: '<div><slot /></div>' },
          EstimateOutcomeIcon: true,
        },
      },
    })

    expect(wrapper.text()).toContain('後で入力する場合は✖ボタンで閉じてください')
    expect(wrapper.findAll('button').some((button) => button.text() === 'キャンセル')).toBe(false)
  })
})
