<script setup lang="ts">
import { computed, ref, useId } from 'vue'
import type { AccuracyTrendPointResponse, MetricAvailabilityResponse } from '@/types/analytics'
import {
  createChartScale,
  createLinearDomainScale,
  mapScaleValue,
  mapSvgPoint,
  svgPoints,
} from '@/utils/chartScale'
import ChartDataTable from './ChartDataTable.vue'
import MetricUnavailable from './MetricUnavailable.vue'

type TrendSeries = 'factor' | 'variance'

const props = defineProps<{
  points: AccuracyTrendPointResponse[]
  availability: MetricAvailabilityResponse
  thresholdPercent: number
}>()

const tableId = `accuracy-trend-table-${useId().replaceAll(':', '')}`
const active = ref<{ point: AccuracyTrendPointResponse; series: TrendSeries } | null>(null)
const plotBounds = { left: 58, right: 700, top: 20, bottom: 170 }
const dateFormatter = new Intl.DateTimeFormat('ja-JP', {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
})
const numberFormatter = new Intl.NumberFormat('ja-JP', { maximumFractionDigits: 1 })

const chart = computed(() => {
  const maxSequence = Math.max(1, ...props.points.map((point) => point.sequence))
  const xScale = createChartScale(maxSequence, plotBounds.left, plotBounds.right, 'linear')
  const thresholdRatio = props.thresholdPercent / 100
  const factorValues = props.points.map((point) => point.factorMedian)
  const factorMin = Math.min(1 - thresholdRatio, ...factorValues)
  const factorMax = Math.max(1 + thresholdRatio, ...factorValues)
  const factorPadding = Math.max(0.05, (factorMax - factorMin) * 0.12)
  const factorScale = createLinearDomainScale(
    Math.max(0, factorMin - factorPadding),
    factorMax + factorPadding,
    plotBounds.bottom,
    plotBounds.top,
  )
  const varianceMax = Math.max(1, ...props.points.map((point) => point.variancePercent))
  const varianceScale = createChartScale(varianceMax, plotBounds.bottom, plotBounds.top, 'linear')

  return {
    xScale,
    factorScale,
    varianceScale,
    factorLine: svgPoints(
      props.points.map((point) =>
        mapSvgPoint(point.sequence, point.factorMedian, xScale, factorScale),
      ),
    ),
    varianceLine: svgPoints(
      props.points.map((point) =>
        mapSvgPoint(point.sequence, point.variancePercent, xScale, varianceScale),
      ),
    ),
    factorPoints: props.points.map((point) => ({
      point,
      position: mapSvgPoint(point.sequence, point.factorMedian, xScale, factorScale),
    })),
    variancePoints: props.points.map((point) => ({
      point,
      position: mapSvgPoint(point.sequence, point.variancePercent, xScale, varianceScale),
    })),
    zoneTop: mapScaleValue(1 + thresholdRatio, factorScale),
    zoneBottom: mapScaleValue(Math.max(0, 1 - thresholdRatio), factorScale),
    referenceY: mapScaleValue(1, factorScale),
  }
})

const tableColumns = [
  { key: 'sequence', label: '完了順', numeric: true },
  { key: 'window', label: '集計窓の期間' },
  { key: 'factor', label: '代表係数', numeric: true },
  { key: 'variance', label: 'ばらつき', numeric: true },
]

const tableRows = computed(() =>
  props.points.map((point) => ({
    sequence: point.sequence,
    window: windowText(point),
    factor: `${point.factorMedian.toFixed(1)}倍`,
    variance: `${numberFormatter.format(point.variancePercent)}%`,
  })),
)

const activeText = computed(() => {
  if (!active.value) return ''
  const { point, series } = active.value
  const value =
    series === 'factor'
      ? `代表係数 ${point.factorMedian.toFixed(1)}倍`
      : `ばらつき ${numberFormatter.format(point.variancePercent)}%`
  return `完了順 ${point.sequence}、${windowText(point)}、${value}`
})

function windowText(point: AccuracyTrendPointResponse) {
  return `${dateFormatter.format(new Date(point.windowFrom))}〜${dateFormatter.format(new Date(point.finishedAt))}`
}

function pointLabel(point: AccuracyTrendPointResponse, series: TrendSeries) {
  const value =
    series === 'factor'
      ? `代表係数${point.factorMedian.toFixed(1)}倍`
      : `ばらつき${numberFormatter.format(point.variancePercent)}%`
  return `完了順${point.sequence}、${windowText(point)}、${value}`
}

function activate(point: AccuracyTrendPointResponse, series: TrendSeries) {
  active.value = { point, series }
}

function deactivate(point: AccuracyTrendPointResponse, series: TrendSeries) {
  if (active.value?.point === point && active.value.series === series) active.value = null
}
</script>

<template>
  <section class="chart-card" aria-labelledby="accuracy-trend-title">
    <div class="section-heading">
      <h2 id="accuracy-trend-title">精度の推移</h2>
      <p>10件ごとの移動中央値を、タスクの完了順で追います。</p>
    </div>

    <MetricUnavailable v-if="!availability.available" :availability="availability" />
    <p v-else-if="points.length === 0" class="empty">表示できる推移データがありません。</p>
    <template v-else>
      <div class="trend-panel">
        <h3>代表係数</h3>
        <svg
          viewBox="0 0 740 210"
          role="img"
          aria-label="完了順ごとの代表係数の移動中央値"
          :aria-describedby="tableId"
        >
          <title>代表係数の推移</title>
          <rect
            class="on-time-zone"
            :x="plotBounds.left"
            :y="chart.zoneTop"
            :width="plotBounds.right - plotBounds.left"
            :height="chart.zoneBottom - chart.zoneTop"
          />
          <g class="grid-lines">
            <line
              v-for="tick in chart.factorScale.ticks"
              :key="tick"
              :x1="plotBounds.left"
              :x2="plotBounds.right"
              :y1="mapScaleValue(tick, chart.factorScale)"
              :y2="mapScaleValue(tick, chart.factorScale)"
            />
          </g>
          <line
            class="reference-line"
            :x1="plotBounds.left"
            :x2="plotBounds.right"
            :y1="chart.referenceY"
            :y2="chart.referenceY"
          />
          <polyline class="factor-series" :points="chart.factorLine" />
          <circle
            v-for="entry in chart.factorPoints"
            :key="entry.point.sequence"
            class="trend-point factor-point"
            :cx="entry.position.x"
            :cy="entry.position.y"
            r="5"
            tabindex="0"
            :aria-label="pointLabel(entry.point, 'factor')"
            @mouseenter="activate(entry.point, 'factor')"
            @mouseleave="deactivate(entry.point, 'factor')"
            @focus="activate(entry.point, 'factor')"
            @blur="deactivate(entry.point, 'factor')"
          />
          <g class="axis-labels">
            <text
              v-for="tick in chart.factorScale.ticks"
              :key="tick"
              :x="plotBounds.left - 8"
              :y="mapScaleValue(tick, chart.factorScale) + 4"
              text-anchor="end"
            >
              {{ tick }}
            </text>
          </g>
        </svg>
      </div>

      <div class="trend-panel">
        <h3>ばらつき</h3>
        <svg
          viewBox="0 0 740 220"
          role="img"
          aria-label="完了順ごとのばらつきの移動中央値"
          :aria-describedby="tableId"
        >
          <title>ばらつきの推移</title>
          <g class="grid-lines">
            <line
              v-for="tick in chart.varianceScale.ticks"
              :key="tick"
              :x1="plotBounds.left"
              :x2="plotBounds.right"
              :y1="mapScaleValue(tick, chart.varianceScale)"
              :y2="mapScaleValue(tick, chart.varianceScale)"
            />
          </g>
          <polyline class="variance-series" :points="chart.varianceLine" />
          <rect
            v-for="entry in chart.variancePoints"
            :key="entry.point.sequence"
            class="trend-point variance-point"
            :x="entry.position.x - 5"
            :y="entry.position.y - 5"
            width="10"
            height="10"
            tabindex="0"
            :aria-label="pointLabel(entry.point, 'variance')"
            @mouseenter="activate(entry.point, 'variance')"
            @mouseleave="deactivate(entry.point, 'variance')"
            @focus="activate(entry.point, 'variance')"
            @blur="deactivate(entry.point, 'variance')"
          />
          <g class="axis-labels">
            <text
              v-for="tick in chart.varianceScale.ticks"
              :key="tick"
              :x="plotBounds.left - 8"
              :y="mapScaleValue(tick, chart.varianceScale) + 4"
              text-anchor="end"
            >
              {{ tick }}%
            </text>
            <text
              v-for="tick in chart.xScale.ticks"
              :key="`x-${tick}`"
              :x="mapScaleValue(tick, chart.xScale)"
              :y="plotBounds.bottom + 20"
              text-anchor="middle"
            >
              {{ tick }}
            </text>
            <text x="380" y="218" text-anchor="middle">完了順</text>
          </g>
        </svg>
      </div>

      <p class="point-detail" aria-live="polite">
        {{ activeText || '各点にマウスを重ねるか、Tabキーで選択すると詳細を確認できます。' }}
      </p>
    </template>

    <ChartDataTable
      v-if="availability.available"
      :id="tableId"
      caption="精度推移のデータ"
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

.section-heading h2,
h3 {
  margin: 0;
}

.section-heading h2 {
  font-size: 1.15rem;
}

.section-heading p,
.empty {
  margin: 0.3em 0 0;
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.metric-unavailable {
  margin-top: 0.8em;
}

.trend-panel {
  margin-top: 0.8em;
}

h3 {
  font-size: 0.88rem;
}

svg {
  display: block;
  width: 100%;
  height: auto;
  overflow: visible;
}

.grid-lines line {
  stroke: var(--color-surface-muted);
  stroke-width: 1;
}

.on-time-zone {
  fill: var(--color-success);
  fill-opacity: 0.13;
}

.reference-line {
  stroke: var(--color-text);
  stroke-width: 1.8;
  stroke-dasharray: 5 4;
}

.factor-series,
.variance-series {
  fill: none;
  stroke-width: 2.5;
  stroke-linejoin: round;
  stroke-linecap: round;
}

.factor-series {
  stroke: var(--color-accent);
}

.variance-series {
  stroke: var(--color-task-accent);
}

.trend-point {
  stroke: var(--color-surface);
  stroke-width: 2;
  transition: stroke-width 0.1s ease;
}

.factor-point {
  fill: var(--color-accent);
}

.variance-point {
  fill: var(--color-task-accent);
}

.trend-point:hover,
.trend-point:focus-visible {
  outline: none;
  stroke: var(--color-focus);
  stroke-width: 4;
}

.axis-labels text {
  fill: var(--color-text-muted);
  font-size: 12px;
}

.point-detail {
  min-height: 2.8em;
  margin: 0.4em 0 0.7em;
  padding: 0.55em 0.7em;
  border-radius: 6px;
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
  font-size: 0.8rem;
}

@media (prefers-reduced-motion: reduce) {
  .trend-point {
    transition: none;
  }
}

@media (max-width: 640px) {
  .trend-panel {
    overflow-x: auto;
  }

  svg {
    min-width: 520px;
  }
}
</style>
