// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ChartDataTable from './ChartDataTable.vue'

describe('ChartDataTable', () => {
  it('表を支援技術から隠さず、トグルで視覚表示を切り替える', async () => {
    const wrapper = mount(ChartDataTable, {
      props: {
        id: 'chart-table',
        caption: 'グラフのデータ',
        columns: [
          { key: 'label', label: '項目' },
          { key: 'value', label: '値', numeric: true },
        ],
        rows: [{ label: '項目A', value: 10 }],
      },
    })

    const table = wrapper.get('table')
    expect(table.attributes('id')).toBe('chart-table')
    expect(table.attributes('aria-hidden')).toBeUndefined()
    expect(table.element.parentElement?.classList.contains('visually-hidden')).toBe(true)
    expect(wrapper.get('button').attributes('aria-expanded')).toBe('false')

    await wrapper.get('button').trigger('click')

    expect(wrapper.get('button').text()).toBe('表を隠す')
    expect(wrapper.get('button').attributes('aria-expanded')).toBe('true')
    expect(table.element.parentElement?.classList.contains('table-scroll')).toBe(true)
  })
})
