<script setup lang="ts">
import { computed, useId } from 'vue'
import type { SizeBucketResponse } from '@/types/analytics'
import { createChartScale, evenlySpacedValues, mapScaleValue } from '@/utils/chartScale'
import ChartDataTable from './ChartDataTable.vue'

const props = defineProps<{
  buckets: SizeBucketResponse[]
  thresholdPercent: number
}>()

const tableId = `size-bucket-table-${useId().replaceAll(':', '')}`
const plotBounds = {
  left: 150,
  right: 550,
  top: 28,
  bottom: 310,
}

const numberFormatter = new Intl.NumberFormat('ja-JP', { maximumFractionDigits: 1 })

const chart = computed(() => {
  const thresholdRatio = props.thresholdPercent / 100
  const maxFactor = Math.max(
    1 + thresholdRatio,
    ...props.buckets.map((bucket) => bucket.factorMedian ?? 0),
  )
  const scale = createChartScale(maxFactor, plotBounds.left, plotBounds.right, 'linear')
  const rowPositions = evenlySpacedValues(props.buckets.length, 68, 270)

  return {
    scale,
    zoneStart: mapScaleValue(Math.max(0, 1 - thresholdRatio), scale),
    zoneEnd: mapScaleValue(1 + thresholdRatio, scale),
    referenceX: mapScaleValue(1, scale),
    rows: props.buckets.map((bucket, index) => ({
      bucket,
      y: rowPositions[index] ?? 0,
      barEnd:
        bucket.factorMedian === null ? plotBounds.left : mapScaleValue(bucket.factorMedian, scale),
    })),
  }
})

const tableColumns = [
  { key: 'bucket', label: '見積時間帯' },
  { key: 'count', label: '件数', numeric: true },
  { key: 'factor', label: '代表係数', numeric: true },
  { key: 'onTimeRate', label: 'オンタイム率', numeric: true },
]

const tableRows = computed(() =>
  props.buckets.map((bucket) => ({
    bucket: bucket.label,
    count: `${bucket.taskCount}件`,
    factor: bucket.factorMedian === null ? '-' : `${bucket.factorMedian.toFixed(1)}倍`,
    onTimeRate: bucket.onTimeRate === null ? '-' : `${numberFormatter.format(bucket.onTimeRate)}%`,
  })),
)

function rowSummary(bucket: SizeBucketResponse) {
  if (bucket.taskCount === 0) return 'データなし'
  if (bucket.taskCount < 3) return `データ不足（${bucket.taskCount}件）`
  return `${bucket.factorMedian?.toFixed(1)}倍・オンタイム率 ${numberFormatter.format(bucket.onTimeRate ?? 0)}%・${bucket.taskCount}件`
}
</script>

<template>
  <section class="chart-card" aria-labelledby="size-bucket-title">
    <div class="section-heading">
      <h2 id="size-bucket-title">タスクサイズ帯別の精度</h2>
      <p>長さの異なるタスクで、見積もりの傾向を比較します。</p>
    </div>

    <div class="chart-scroll">
      <svg
        viewBox="0 0 760 350"
        role="img"
        aria-label="見積時間帯ごとの代表係数、オンタイム率、件数を示す横棒グラフ"
        :aria-describedby="tableId"
      >
        <title>タスクサイズ帯別の精度</title>

        <rect
          class="on-time-zone"
          :x="chart.zoneStart"
          :y="plotBounds.top"
          :width="chart.zoneEnd - chart.zoneStart"
          :height="plotBounds.bottom - plotBounds.top"
        />

        <g class="grid-lines">
          <line
            v-for="tick in chart.scale.ticks"
            :key="`grid-${tick}`"
            :x1="mapScaleValue(tick, chart.scale)"
            :x2="mapScaleValue(tick, chart.scale)"
            :y1="plotBounds.top"
            :y2="plotBounds.bottom"
          />
        </g>
        <line
          class="reference-line"
          :x1="chart.referenceX"
          :x2="chart.referenceX"
          :y1="plotBounds.top"
          :y2="plotBounds.bottom"
        />

        <g v-for="row in chart.rows" :key="row.bucket.bucketCode" class="bucket-row">
          <text class="bucket-label" x="138" :y="row.y + 4" text-anchor="end">
            {{ row.bucket.label }}
          </text>
          <rect
            v-if="row.bucket.factorMedian !== null"
            class="factor-bar"
            :x="plotBounds.left"
            :y="row.y - 9"
            :width="Math.max(0, row.barEnd - plotBounds.left)"
            height="18"
          />
          <text class="row-summary" x="570" :y="row.y + 4">
            {{ rowSummary(row.bucket) }}
          </text>
        </g>

        <g class="axis">
          <line
            :x1="plotBounds.left"
            :x2="plotBounds.right"
            :y1="plotBounds.bottom"
            :y2="plotBounds.bottom"
          />
          <text
            v-for="tick in chart.scale.ticks"
            :key="`tick-${tick}`"
            :x="mapScaleValue(tick, chart.scale)"
            :y="plotBounds.bottom + 20"
            text-anchor="middle"
          >
            {{ tick }}
          </text>
          <text x="350" y="348" text-anchor="middle">代表係数</text>
        </g>
      </svg>
    </div>

    <div class="legend" aria-label="凡例">
      <span><i class="bar-sample" />代表係数</span>
      <span><i class="zone-sample" />見積どおりの範囲</span>
      <span><i class="line-sample" />係数1.0</span>
    </div>

    <ChartDataTable
      :id="tableId"
      caption="タスクサイズ帯別の精度データ"
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

svg {
  display: block;
  width: 100%;
  height: auto;
  margin-top: 0.7em;
  overflow: visible;
}

.on-time-zone {
  fill: var(--color-success);
  fill-opacity: 0.13;
}

.grid-lines line {
  stroke: var(--color-surface-muted);
  stroke-width: 1;
}

.reference-line {
  stroke: var(--color-text);
  stroke-width: 2;
  stroke-dasharray: 5 4;
}

.factor-bar {
  fill: var(--color-accent);
}

.bucket-label,
.row-summary,
.axis text {
  fill: var(--color-text-muted);
  font-size: 12px;
}

.bucket-label {
  fill: var(--color-text);
  font-weight: 600;
}

.axis line {
  stroke: var(--color-text-muted);
  stroke-width: 1.5;
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45em 1em;
  margin: 0.2em 0 0.7em;
  color: var(--color-text-muted);
  font-size: 0.75rem;
}

.legend span {
  display: inline-flex;
  align-items: center;
  gap: 0.35em;
}

.bar-sample,
.zone-sample {
  display: inline-block;
  width: 1.2em;
  height: 0.65em;
  background: var(--color-accent);
}

.zone-sample {
  background: color-mix(in srgb, var(--color-success) 20%, transparent);
}

.line-sample {
  width: 1.2em;
  border-top: 2px dashed var(--color-text);
}

@media (max-width: 640px) {
  .chart-scroll {
    overflow-x: auto;
  }

  svg {
    min-width: 560px;
  }
}
</style>
