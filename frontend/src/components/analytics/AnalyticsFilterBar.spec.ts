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
        filter: { projectId: null, period: 'ALL', causeCategory: null, outcome: 'ALL' },
        projects: [
          { id: 3, title: 'プロジェクトA', description: null, isFinished: false, memos: [] },
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

    expect(wrapper.emitted('projectChange')?.[0]).toEqual([3])
    expect(wrapper.emitted('periodChange')?.[0]).toEqual(['LAST_90_DAYS'])
  })
})
