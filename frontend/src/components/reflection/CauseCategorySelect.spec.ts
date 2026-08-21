// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { useCauseCategoryStore } from '@/stores/causeCategoryStore'
import type { CauseDirection, ReflectionCauseCategoryResponse } from '@/types/reflection'
import CauseCategorySelect from './CauseCategorySelect.vue'

const directions: CauseDirection[] = [
  ...Array<CauseDirection>(9).fill('OVER'),
  ...Array<CauseDirection>(7).fill('UNDER'),
  ...Array<CauseDirection>(2).fill('BOTH'),
]

const categories: ReflectionCauseCategoryResponse[] = directions.map((direction, index) => ({
  code: `${direction}_${index}`,
  label: `${direction}カテゴリ${index}`,
  direction,
  nextActionHint: index === 0 ? '次は手順を書き出す' : null,
  requiresCause: false,
}))

function mountSelect(outcome: 'late' | 'on-time' | 'early' | 'unknown', modelValue: string[] = []) {
  return mount(CauseCategorySelect, { props: { modelValue, outcome } })
}

function optionValues(wrapper: ReturnType<typeof mountSelect>) {
  return wrapper
    .findAll('.options input[type="checkbox"]')
    .map((input) => input.attributes('value') ?? '')
}

function checkboxFor(wrapper: ReturnType<typeof mountSelect>, code: string) {
  return wrapper.get(`.options input[value="${code}"]`)
}

describe('CauseCategorySelect', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    const store = useCauseCategoryStore()
    store.categories = categories
    store.initialized = true
  })

  it('超過では超過側と共通だけを表示し、切り替えると全18件を表示する', async () => {
    const wrapper = mountSelect('late')

    expect(optionValues(wrapper)).toHaveLength(11)
    expect(optionValues(wrapper).every((code) => !code.startsWith('UNDER'))).toBe(true)

    await wrapper.get('.show-all input[type="checkbox"]').setValue(true)

    expect(optionValues(wrapper)).toHaveLength(18)
  })

  it('見積もりどおりでは共通カテゴリを先頭にして全件を表示する', () => {
    const wrapper = mountSelect('on-time')

    expect(optionValues(wrapper)).toHaveLength(18)
    expect(optionValues(wrapper).slice(0, 2)).toEqual(['BOTH_16', 'BOTH_17'])
  })

  it('絞り込み対象外でも選択済みカテゴリを残し、選択したカテゴリのヒントを表示する', async () => {
    const wrapper = mountSelect('late', ['UNDER_9'])

    expect(optionValues(wrapper)).toContain('UNDER_9')

    await checkboxFor(wrapper, 'OVER_0').setValue(true)

    // 選択順は保持せず、マスタの表示順（OVER_0が先）へ整列して返す
    expect(wrapper.emitted('update:modelValue')).toEqual([[['OVER_0', 'UNDER_9']]])
    await wrapper.setProps({ modelValue: ['OVER_0', 'UNDER_9'] })
    expect(wrapper.text()).toContain('次のアクションのヒント：次は手順を書き出す')
  })

  it('選択済み件数と上限を常時表示する', () => {
    const wrapper = mountSelect('late', ['OVER_0', 'OVER_1'])

    expect(wrapper.get('.selection-count').text()).toBe('2 / 3 件選択')
  })

  it('3件選択すると4件目が選べず、上限の理由が表示される', () => {
    const wrapper = mountSelect('late', ['OVER_0', 'OVER_1', 'OVER_2'])

    expect(checkboxFor(wrapper, 'OVER_3').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('原因カテゴリは3件まで選べます')
  })

  it('3件選択していても選択済みのチェックボックスは外せる', () => {
    const wrapper = mountSelect('late', ['OVER_0', 'OVER_1', 'OVER_2'])

    expect(checkboxFor(wrapper, 'OVER_0').attributes('disabled')).toBeUndefined()
  })

  it('出し分けの切り替えでは選択済みカテゴリが解除されない', async () => {
    const wrapper = mountSelect('late', ['UNDER_9'])

    await wrapper.get('.show-all input[type="checkbox"]').setValue(true)
    await wrapper.get('.show-all input[type="checkbox"]').setValue(false)

    expect(optionValues(wrapper)).toContain('UNDER_9')
  })
})
