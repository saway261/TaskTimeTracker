// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import type { GapCauseAggregateResponse } from '@/types/analytics'
import GapCauseChart from './GapCauseChart.vue'

const aggregate: GapCauseAggregateResponse = {
  analyzedTaskCount: 10,
  totalLinkCount: 15,
  groups: [
    {
      outcome: 'LATE',
      label: '超過',
      totalCount: 7,
      sharePercent: 70,
      items: [
        {
          causeCategoryCode: 'SCOPE_CREEP',
          causeCategoryLabel: '想定外の作業',
          taskCount: 7,
          sharePercent: 70,
          gapRateMedian: 35,
        },
      ],
    },
    {
      outcome: 'ON_TIME',
      label: 'おおむね見積どおり',
      totalCount: 3,
      sharePercent: 30,
      items: [
        {
          causeCategoryCode: 'UNCLEAR_GOAL',
          causeCategoryLabel: 'ゴール・完了条件が曖昧だった',
          taskCount: 3,
          sharePercent: 30,
          gapRateMedian: 5,
        },
      ],
    },
    {
      outcome: 'EARLY',
      label: '短縮',
      totalCount: 5,
      sharePercent: 50,
      items: [
        {
          causeCategoryCode: 'CONDITION',
          causeCategoryLabel: '体調・コンディション',
          taskCount: 2,
          sharePercent: 20,
          gapRateMedian: null,
        },
        {
          causeCategoryCode: null,
          causeCategoryLabel: '未分類',
          taskCount: 3,
          sharePercent: 30,
          gapRateMedian: null,
        },
      ],
    },
  ],
}

const mountChart = (selectedCauseCategory: string | null = null) =>
  mount(GapCauseChart, {
    props: { aggregate, selectedCauseCategory },
    global: {
      stubs: {
        RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
      },
    },
  })

describe('GapCauseChart', () => {
  it('3グループを同時表示して延べ件数が増える理由を常時説明する', () => {
    const wrapper = mountChart()

    expect(wrapper.findAll('.cause-group')).toHaveLength(3)
    expect(wrapper.text()).toContain('超過')
    expect(wrapper.text()).toContain('おおむね見積どおり')
    expect(wrapper.text()).toContain('短縮')
    expect(wrapper.text()).toContain('分析対象 10件 / 原因の延べ 15件')
    expect(wrapper.text()).toContain(
      '合計が分析対象件数を超えるのは、1つのタスクに複数の原因を選べるためです。',
    )
    expect(wrapper.text()).toContain('7件・70%・誤差率 +35.0%')
    expect(wrapper.text()).toContain('2件・20%・誤差率 -')
  })

  it('カテゴリをクリックまたはキーボードで選択するとタイムライン絞り込みを通知する', async () => {
    const wrapper = mountChart()
    const rows = wrapper.findAll('.cause-row.selectable')

    await rows[0].trigger('click')
    await rows[1].trigger('keydown', { key: 'Enter' })

    expect(wrapper.emitted('causeCategoryChange')?.[0]).toEqual(['SCOPE_CREEP'])
    expect(wrapper.emitted('causeCategoryChange')?.[1]).toEqual(['UNCLEAR_GOAL'])
  })

  it('未分類は選択不可にして振り返り画面への後付け導線を表示する', async () => {
    const wrapper = mountChart()
    const unclassified = wrapper.get('.cause-row.unclassified')

    expect(unclassified.attributes('role')).toBeUndefined()
    expect(unclassified.attributes('tabindex')).toBeUndefined()
    await unclassified.trigger('click')
    expect(wrapper.emitted('causeCategoryChange')).toBeUndefined()
    expect(wrapper.get('.unclassified-note').text()).toContain('3件が未分類です')
    expect(wrapper.get('.unclassified-note a').attributes('href')).toBe('/reflections')
  })

  it('絞り込み中のカテゴリを表示して解除できる', async () => {
    const wrapper = mountChart('SCOPE_CREEP')

    expect(wrapper.get('.active-filter').text()).toContain('「想定外の作業」で絞り込み中')
    expect(wrapper.get('.cause-row.selected').attributes('aria-pressed')).toBe('true')

    await wrapper.get('.active-filter button').trigger('click')

    expect(wrapper.emitted('causeCategoryChange')?.[0]).toEqual([null])
  })

  it('各グラフを同じ内容の代替表へ関連付ける', () => {
    const wrapper = mountChart()
    const tableId = wrapper.get('table').attributes('id')

    expect(wrapper.findAll('svg')).toHaveLength(3)
    expect(
      wrapper.findAll('svg').every((svg) => svg.attributes('aria-describedby') === tableId),
    ).toBe(true)
    expect(wrapper.text()).toContain('原因カテゴリ別の集計データ')
  })
})
