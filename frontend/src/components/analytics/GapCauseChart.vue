<script setup lang="ts">
import { computed, useId } from 'vue'
import { RouterLink } from 'vue-router'
import type { GapCauseAggregateResponse, GapCauseItemResponse } from '@/types/analytics'
import { formatGapRate } from '@/utils/duration'
import { createChartScale, evenlySpacedValues, mapScaleValue } from '@/utils/chartScale'
import ChartDataTable from './ChartDataTable.vue'

const props = defineProps<{
  aggregate: GapCauseAggregateResponse
  selectedCauseCategory: string | null
}>()

const emit = defineEmits<{
  causeCategoryChange: [causeCategory: string | null]
}>()

const tableId = `gap-cause-table-${useId().replaceAll(':', '')}`
const plotBounds = { left: 238, right: 570 }
const numberFormatter = new Intl.NumberFormat('ja-JP', { maximumFractionDigits: 1 })

const chart = computed(() => {
  const maxCount = Math.max(
    1,
    ...props.aggregate.groups.flatMap((group) => group.items.map((item) => item.taskCount)),
  )
  const scale = createChartScale(maxCount, plotBounds.left, plotBounds.right, 'linear')

  return {
    scale,
    groups: props.aggregate.groups.map((group) => {
      const height = Math.max(90, group.items.length * 50 + 40)
      const positions = evenlySpacedValues(group.items.length, 48, height - 28)
      return {
        ...group,
        height,
        rows: group.items.map((item, index) => ({
          item,
          y: positions[index] ?? 0,
          barWidth: Math.max(0, mapScaleValue(item.taskCount, scale) - plotBounds.left),
        })),
      }
    }),
  }
})

const unclassifiedCount = computed(() =>
  props.aggregate.groups
    .flatMap((group) => group.items)
    .filter((item) => item.causeCategoryCode === null)
    .reduce((sum, item) => sum + item.taskCount, 0),
)

const selectedLabel = computed(() => {
  if (!props.selectedCauseCategory) return null
  return (
    props.aggregate.groups
      .flatMap((group) => group.items)
      .find((item) => item.causeCategoryCode === props.selectedCauseCategory)?.causeCategoryLabel ??
    props.selectedCauseCategory
  )
})

const tableColumns = [
  { key: 'group', label: 'グループ' },
  { key: 'groupTotal', label: 'グループ延べ件数・付与率', numeric: true },
  { key: 'category', label: '原因カテゴリ' },
  { key: 'count', label: '延べ件数', numeric: true },
  { key: 'share', label: '付与率', numeric: true },
  { key: 'gapRate', label: '代表誤差率', numeric: true },
]

const tableRows = computed(() =>
  props.aggregate.groups.flatMap((group) =>
    group.items.map((item) => ({
      group: group.label,
      groupTotal: `${group.totalCount}件・${numberFormatter.format(group.sharePercent)}%`,
      category: item.causeCategoryLabel,
      count: `${item.taskCount}件`,
      share: `${numberFormatter.format(item.sharePercent)}%`,
      gapRate: item.gapRateMedian === null ? '-' : formatGapRate(item.gapRateMedian),
    })),
  ),
)

function itemSummary(item: GapCauseItemResponse) {
  const gapRate =
    item.gapRateMedian === null ? '誤差率 -' : `誤差率 ${formatGapRate(item.gapRateMedian)}`
  return `${item.taskCount}件・${numberFormatter.format(item.sharePercent)}%・${gapRate}`
}

function selectItem(item: GapCauseItemResponse) {
  if (item.causeCategoryCode !== null) emit('causeCategoryChange', item.causeCategoryCode)
}
</script>

<template>
  <section class="chart-card" aria-labelledby="gap-cause-title">
    <div class="section-heading">
      <h2 id="gap-cause-title">原因カテゴリ別の傾向</h2>
      <p>原因が関わった延べ件数と、分析対象に対する付与率を示します。</p>
    </div>

    <div class="aggregate-note">
      <strong>
        分析対象 {{ aggregate.analyzedTaskCount }}件 / 原因の延べ {{ aggregate.totalLinkCount }}件
      </strong>
      <span>合計が分析対象件数を超えるのは、1つのタスクに複数の原因を選べるためです。</span>
    </div>

    <div v-if="selectedCauseCategory" class="active-filter" aria-live="polite">
      <span>タイムラインを「{{ selectedLabel }}」で絞り込み中</span>
      <button type="button" @click="emit('causeCategoryChange', null)">絞り込みを解除</button>
    </div>

    <div class="cause-groups">
      <section
        v-for="group in chart.groups"
        :key="group.direction"
        class="cause-group"
        :class="group.direction.toLowerCase()"
      >
        <h3>
          {{ group.label }}
          <span
            >{{ group.totalCount }}件・付与率
            {{ numberFormatter.format(group.sharePercent) }}%</span
          >
        </h3>
        <p v-if="group.items.length === 0" class="empty-group">該当する原因はありません。</p>
        <svg
          v-else
          :viewBox="`0 0 800 ${group.height}`"
          role="img"
          :aria-label="`${group.label}の原因カテゴリ別延べ件数`"
          :aria-describedby="tableId"
        >
          <title>{{ group.label }}の原因カテゴリ別集計</title>
          <g class="grid-lines">
            <line
              v-for="tick in chart.scale.ticks"
              :key="tick"
              :x1="mapScaleValue(tick, chart.scale)"
              :x2="mapScaleValue(tick, chart.scale)"
              y1="20"
              :y2="group.height - 16"
            />
          </g>
          <g
            v-for="row in group.rows"
            :key="row.item.causeCategoryCode ?? 'unclassified'"
            class="cause-row"
            :class="{
              selectable: row.item.causeCategoryCode !== null,
              selected: row.item.causeCategoryCode === selectedCauseCategory,
              unclassified: row.item.causeCategoryCode === null,
            }"
            :tabindex="row.item.causeCategoryCode === null ? undefined : 0"
            :role="row.item.causeCategoryCode === null ? undefined : 'button'"
            :aria-pressed="
              row.item.causeCategoryCode === null
                ? undefined
                : row.item.causeCategoryCode === selectedCauseCategory
            "
            :aria-label="`${row.item.causeCategoryLabel}、${itemSummary(row.item)}${row.item.causeCategoryCode === null ? '、絞り込み不可' : '、タイムラインを絞り込む'}`"
            @click="selectItem(row.item)"
            @keydown.enter.prevent="selectItem(row.item)"
            @keydown.space.prevent="selectItem(row.item)"
          >
            <text class="category-label" x="226" :y="row.y + 4" text-anchor="end">
              {{ row.item.causeCategoryLabel }}
            </text>
            <rect
              class="count-bar"
              :x="plotBounds.left"
              :y="row.y - 9"
              :width="row.barWidth"
              height="18"
            />
            <text class="item-summary" x="590" :y="row.y + 4">
              {{ itemSummary(row.item) }}
            </text>
          </g>
        </svg>
      </section>
    </div>

    <p v-if="unclassifiedCount > 0" class="unclassified-note">
      {{ unclassifiedCount }}件が未分類です。
      <RouterLink to="/reflections">振り返り画面からカテゴリを付けられます</RouterLink>
    </p>

    <ChartDataTable
      :id="tableId"
      caption="原因カテゴリ別の集計データ"
      :columns="tableColumns"
      :rows="tableRows"
    />
  </section>
</template>

<style scoped>
.chart-card {
  min-width: 0;
  padding: 1em;
  border: 1px solid var(--color-surface-muted);
  border-radius: 8px;
  background: var(--color-surface);
}

.section-heading h2 {
  margin: 0;
  font-size: 1.15rem;
}

.section-heading p {
  margin: 0.3em 0 0;
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.aggregate-note {
  display: flex;
  flex-direction: column;
  gap: 0.25em;
  margin-top: 0.8em;
  padding: 0.7em;
  border-radius: 6px;
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.aggregate-note strong {
  color: var(--color-text);
}

.active-filter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.7em;
  margin-top: 0.7em;
  padding: 0.55em 0.7em;
  border: 1px solid var(--color-accent);
  border-radius: 6px;
  color: var(--color-text);
  font-size: 0.82rem;
}

.active-filter button {
  flex-shrink: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--color-accent);
  cursor: pointer;
  font: inherit;
  font-weight: 600;
  text-decoration: underline;
  text-underline-offset: 0.2em;
}

.active-filter button:focus-visible,
.unclassified-note a:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.cause-groups {
  display: flex;
  flex-direction: column;
  gap: 0.8em;
  margin-top: 0.8em;
}

.cause-group {
  padding: 0.7em;
  border-left: 4px solid var(--color-danger);
  border-radius: 6px;
  background: color-mix(in srgb, var(--color-danger) 5%, var(--color-surface));
}

.cause-group.under {
  border-left-color: var(--color-task-accent);
  background: color-mix(in srgb, var(--color-task-accent) 5%, var(--color-surface));
}

.cause-group.both {
  border-left-color: var(--color-text-muted);
  background: var(--color-surface);
}

h3 {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.7em;
  margin: 0;
  font-size: 0.92rem;
}

h3 span {
  color: var(--color-text-muted);
  font-size: 0.75rem;
  font-weight: 500;
}

.empty-group {
  margin: 0.6em 0 0;
  color: var(--color-text-muted);
  font-size: 0.8rem;
}

svg {
  display: block;
  width: 100%;
  min-width: 0;
  margin-top: 0.35em;
  overflow: visible;
}

.grid-lines line {
  stroke: var(--color-surface-muted);
  stroke-width: 1;
}

.category-label,
.item-summary {
  fill: var(--color-text);
  font-size: 12px;
}

.item-summary {
  fill: var(--color-text-muted);
}

.count-bar {
  fill: var(--color-danger);
}

.under .count-bar {
  fill: var(--color-task-accent);
}

.both .count-bar {
  fill: var(--color-text-muted);
}

.cause-row.selectable {
  cursor: pointer;
}

.cause-row.selectable .count-bar {
  transition: stroke-width 0.1s ease;
}

.cause-row.selectable:hover .count-bar,
.cause-row.selectable:focus-visible .count-bar,
.cause-row.selected .count-bar {
  stroke: var(--color-focus);
  stroke-width: 4;
}

.cause-row:focus-visible {
  outline: none;
}

.cause-row.unclassified {
  opacity: 0.75;
}

.unclassified-note {
  margin: 0.8em 0;
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.unclassified-note a {
  color: var(--color-accent);
  font-weight: 600;
}

@media (max-width: 520px) {
  .active-filter,
  h3 {
    align-items: flex-start;
    flex-direction: column;
  }

  .cause-group {
    overflow-x: auto;
  }

  svg {
    min-width: 600px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .cause-row.selectable .count-bar {
    transition: none;
  }
}
</style>
