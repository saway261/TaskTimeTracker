// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import type { EstimationAccuracyResponse } from '@/types/analytics'
import AnalyticsFilterBar from './AnalyticsFilterBar.vue'

const accuracy = {
  analyzedTaskCount: 8,
  excluded: { total: 3, missingGapRate: 2, missingActualMinutes: 1 },
} as EstimationAccuracyResponse

describe('AnalyticsFilterBar', () => {
  it('分析対象・除外理由を表示してプロジェクトと期間を通知する', async () => {
    const wrapper = mount(AnalyticsFilterBar, {
      props: {
        filter: {
          projectId: null,
          tagId: null,
          period: 'ALL',
          causeCategory: null,
          outcome: 'ALL',
        },
        projects: [
          { id: 3, title: 'プロジェクトA', description: null, isFinished: false, memos: [] },
        ],
        tags: [
          { id: 5, name: '調査' },
          { id: 6, name: '設計' },
        ],
        accuracy,
      },
    })
    const selects = wrapper.findAll('select')

    expect(wrapper.text()).toContain('分析対象 8件')
    expect(wrapper.text()).toContain('除外 3件')
    expect(wrapper.text()).toContain('誤差率を算出できない: 2件')
    expect(wrapper.text()).toContain('実績時間が記録されていない: 1件')

    await selects[0].setValue('3')
    await selects[1].setValue('LAST_90_DAYS')
    await selects[2].setValue('5')

    expect(wrapper.emitted('projectChange')?.[0]).toEqual([3])
    expect(wrapper.emitted('periodChange')?.[0]).toEqual(['LAST_90_DAYS'])
    expect(wrapper.emitted('tagChange')?.[0]).toEqual([5])
    expect(selects[2].findAll('option').map((option) => option.text())).toEqual([
      'すべてのタグ',
      '調査',
      '設計',
    ])
    expect(wrapper.text()).not.toContain('タグ未設定')
  })

  it('タグ絞り込み時だけ上位3プロジェクトと残りの合計・注記を表示する', async () => {
    const filter = {
      projectId: null,
      tagId: 5,
      period: 'ALL' as const,
      causeCategory: null,
      outcome: 'ALL' as const,
    }
    const filteredAccuracy = {
      ...accuracy,
      projectBreakdown: [
        { projectId: 4, projectTitle: '改善', count: 2 },
        { projectId: 1, projectTitle: '開発基盤', count: 18 },
        { projectId: 5, projectTitle: '運用', count: 1 },
        { projectId: 3, projectTitle: '学習', count: 3 },
        { projectId: 2, projectTitle: '社内ツール', count: 4 },
      ],
    } as EstimationAccuracyResponse
    const wrapper = mount(AnalyticsFilterBar, {
      props: {
        filter,
        projects: [],
        tags: [{ id: 5, name: '調査' }],
        accuracy: filteredAccuracy,
      },
    })

    const breakdown = wrapper.get('.project-breakdown')
    expect(breakdown.text()).toContain('タグ「調査」の内訳:')
    expect(breakdown.findAll('.breakdown-item').map((item) => item.text())).toEqual([
      '開発基盤 18件',
      '社内ツール 4件',
      '学習 3件',
      '他2プロジェクト 3件',
    ])
    expect(breakdown.get('.confounding-note').text()).toContain(
      '特定プロジェクトの傾向を強く反映している場合があります',
    )

    await wrapper.setProps({ filter: { ...filter, projectId: 1 } })
    expect(wrapper.find('.project-breakdown').exists()).toBe(false)

    await wrapper.setProps({ filter: { ...filter, tagId: null } })
    expect(wrapper.find('.project-breakdown').exists()).toBe(false)
  })

  it('タグ絞り込みの分析対象が0件でも内訳と注記を表示する', () => {
    const wrapper = mount(AnalyticsFilterBar, {
      props: {
        filter: {
          projectId: null,
          tagId: 5,
          period: 'ALL',
          causeCategory: null,
          outcome: 'ALL',
        },
        projects: [],
        tags: [{ id: 5, name: '調査' }],
        accuracy: { ...accuracy, analyzedTaskCount: 0, projectBreakdown: [] },
      },
    })

    expect(wrapper.get('.breakdown-empty').text()).toBe('対象なし')
    expect(wrapper.find('.confounding-note').exists()).toBe(true)
  })
})
