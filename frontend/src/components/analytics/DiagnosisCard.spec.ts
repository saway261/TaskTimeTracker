// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import type { AnalyticsFilter, DiagnosisResponse } from '@/types/analytics'
import DiagnosisCard from './DiagnosisCard.vue'

const diagnosis: DiagnosisResponse = {
  code: 'GOOD',
  biasDirection: 'NONE',
  title: '見積もりは安定しています',
  message: '現在の見積もり方を継続しましょう。',
}

const allFilter: AnalyticsFilter = {
  projectId: null,
  tagId: null,
  period: 'ALL',
  causeCategory: null,
  outcome: 'ALL',
}

describe('DiagnosisCard', () => {
  it('タグ・プロジェクト・期間の対象範囲と件数を表示する', () => {
    const wrapper = mount(DiagnosisCard, {
      props: {
        diagnosis,
        filter: {
          ...allFilter,
          projectId: 3,
          tagId: 5,
          period: 'LAST_90_DAYS',
        },
        analyzedTaskCount: 24,
        projectName: '開発基盤',
        tagName: '調査',
      },
    })

    expect(wrapper.get('.scope-description').text()).toBe(
      'タグ「調査」 / プロジェクト「開発基盤」 / 期間「直近90日」のタスク24件についての診断です',
    )
  })

  it('プロジェクトだけの絞り込みでも表示する', () => {
    const wrapper = mount(DiagnosisCard, {
      props: {
        diagnosis,
        filter: { ...allFilter, projectId: 3 },
        analyzedTaskCount: 8,
        projectName: '開発基盤',
      },
    })

    expect(wrapper.get('.scope-description').text()).toBe(
      'プロジェクト「開発基盤」のタスク8件についての診断です',
    )
  })

  it('共通フィルタがすべての場合は対象範囲を表示しない', () => {
    const wrapper = mount(DiagnosisCard, {
      props: { diagnosis, filter: allFilter, analyzedTaskCount: 30 },
    })

    expect(wrapper.find('.scope-description').exists()).toBe(false)
  })
})
