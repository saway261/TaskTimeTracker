<script setup lang="ts">
import { computed, ref, useId } from 'vue'
import type { AnalyticsOutcome, ScatterPointResponse } from '@/types/analytics'
import { formatGapRate, formatMinutes } from '@/utils/duration'
import {
  createChartScale,
  factorBandPolygon,
  factorLineSegment,
  mapScaleValue,
  mapSvgPoint,
  svgPoints,
  type ChartScaleMode,
} from '@/utils/chartScale'
import ChartDataTable from './ChartDataTable.vue'
import ScatterPointModal from './ScatterPointModal.vue'

const props = defineProps<{
  points: ScatterPointResponse[]
  thresholdPercent: number
  factorMedian: number | null
  truncated: boolean
}>()

const mode = ref<ChartScaleMode>('linear')
const selectedPoint = ref<ScatterPointResponse | null>(null)
const tableId = `scatter-table-${useId().replaceAll(':', '')}`
const markerPrefix = `scatter-marker-${useId().replaceAll(':', '')}`

const plotBounds = {
  left: 68,
  right: 700,
  top: 24,
  bottom: 352,
}

const outcomeLabels: Record<AnalyticsOutcome, string> = {
  LATE: '超過',
  ON_TIME: 'おおむね見積どおり',
  EARLY: '短縮',
}

const plot = computed(() => {
  const maxValue = Math.max(
    1,
    ...props.points.flatMap((point) => [point.estimatedMinutes, point.actualMinutes]),
  )
  const xScale = createChartScale(maxValue, plotBounds.left, plotBounds.right, mode.value)
  const yScale = createChartScale(maxValue, plotBounds.bottom, plotBounds.top, mode.value)
  const thresholdRatio = props.thresholdPercent / 100

  return {
    xScale,
    yScale,
    points: props.points.map((point) => ({
      point,
      position: mapSvgPoint(point.estimatedMinutes, point.actualMinutes, xScale, yScale),
    })),
    zone: svgPoints(factorBandPolygon(1 - thresholdRatio, 1 + thresholdRatio, xScale, yScale)),
    reference: factorLineSegment(1, xScale, yScale),
    trend:
      props.factorMedian === null ? null : factorLineSegment(props.factorMedian, xScale, yScale),
  }
})

const tableRows = computed(() =>
  props.points.map((point) => ({
    task: point.taskTitle,
    outcome: outcomeLabels[point.outcome],
    estimate: formatMinutes(point.estimatedMinutes),
    actual: formatMinutes(point.actualMinutes),
    gapRate: formatGapRate(point.gapRate),
  })),
)

const tableColumns = [
  { key: 'task', label: 'タスク' },
  { key: 'outcome', label: '判定' },
  { key: 'estimate', label: '見積時間', numeric: true },
  { key: 'actual', label: '実績時間', numeric: true },
  { key: 'gapRate', label: '誤差比', numeric: true },
]

function markerId(outcome: AnalyticsOutcome) {
  return `${markerPrefix}-${outcome.toLowerCase()}`
}

function pointLabel(point: ScatterPointResponse) {
  return `${point.taskTitle}、見積${formatMinutes(point.estimatedMinutes)}、実績${formatMinutes(point.actualMinutes)}、${outcomeLabels[point.outcome]}`
}

function openPoint(point: ScatterPointResponse) {
  selectedPoint.value = point
}

function closeModal(open: boolean) {
  if (!open) selectedPoint.value = null
}
</script>

<template>
  <section class="chart-card" aria-labelledby="scatter-title">
    <div class="section-heading">
      <div>
        <h2 id="scatter-title">見積もりと実績</h2>
        <p>基準線から離れたタスクほど、見積もりと実績の差が大きくなります。</p>
      </div>
      <div class="scale-toggle" role="group" aria-label="軸の種類">
        <button type="button" :aria-pressed="mode === 'linear'" @click="mode = 'linear'">
          線形
        </button>
        <button type="button" :aria-pressed="mode === 'log'" @click="mode = 'log'">対数</button>
      </div>
    </div>

    <p v-if="points.length === 0" class="empty">表示できるデータがありません。</p>
    <div v-else class="chart-content">
      <svg
        viewBox="0 0 740 400"
        role="img"
        aria-label="見積時間を横軸、実績時間を縦軸とした散布図"
        :aria-describedby="tableId"
      >
        <title>見積時間と実績時間の散布図</title>
        <defs>
          <path :id="`${markerPrefix}-late`" d="M 0 -7 L 7 6 L -7 6 Z" />
          <circle :id="`${markerPrefix}-on_time`" cx="0" cy="0" r="6" />
          <rect :id="`${markerPrefix}-early`" x="-6" y="-6" width="12" height="12" />
        </defs>

        <g class="grid-lines">
          <line
            v-for="tick in plot.xScale.ticks"
            :key="`x-grid-${tick}`"
            :x1="mapScaleValue(tick, plot.xScale)"
            :x2="mapScaleValue(tick, plot.xScale)"
            :y1="plotBounds.top"
            :y2="plotBounds.bottom"
          />
          <line
            v-for="tick in plot.yScale.ticks"
            :key="`y-grid-${tick}`"
            :x1="plotBounds.left"
            :x2="plotBounds.right"
            :y1="mapScaleValue(tick, plot.yScale)"
            :y2="mapScaleValue(tick, plot.yScale)"
          />
        </g>

        <polygon v-if="plot.zone" class="on-time-zone" :points="plot.zone" />
        <line
          v-if="plot.reference"
          class="reference-line"
          :x1="plot.reference.start.x"
          :y1="plot.reference.start.y"
          :x2="plot.reference.end.x"
          :y2="plot.reference.end.y"
        />
        <line
          v-if="plot.trend"
          class="trend-line"
          :x1="plot.trend.start.x"
          :y1="plot.trend.start.y"
          :x2="plot.trend.end.x"
          :y2="plot.trend.end.y"
        />

        <g class="axes">
          <line
            :x1="plotBounds.left"
            :x2="plotBounds.right"
            :y1="plotBounds.bottom"
            :y2="plotBounds.bottom"
          />
          <line
            :x1="plotBounds.left"
            :x2="plotBounds.left"
            :y1="plotBounds.top"
            :y2="plotBounds.bottom"
          />
          <g v-for="tick in plot.xScale.ticks" :key="`x-label-${tick}`">
            <text
              :x="mapScaleValue(tick, plot.xScale)"
              :y="plotBounds.bottom + 22"
              text-anchor="middle"
            >
              {{ tick }}
            </text>
          </g>
          <g v-for="tick in plot.yScale.ticks" :key="`y-label-${tick}`">
            <text
              :x="plotBounds.left - 10"
              :y="mapScaleValue(tick, plot.yScale) + 4"
              text-anchor="end"
            >
              {{ tick }}
            </text>
          </g>
          <text x="384" y="397" text-anchor="middle">見積時間（分）</text>
          <text transform="translate(16 190) rotate(-90)" text-anchor="middle">実績時間（分）</text>
        </g>

        <g class="scatter-points">
          <g
            v-for="entry in plot.points"
            :key="entry.point.taskId"
            class="scatter-point"
            :class="entry.point.outcome.toLowerCase().replace('_', '-')"
            tabindex="0"
            role="button"
            :aria-label="pointLabel(entry.point)"
            @click="openPoint(entry.point)"
            @keydown.enter.prevent="openPoint(entry.point)"
            @keydown.space.prevent="openPoint(entry.point)"
          >
            <use
              :href="`#${markerId(entry.point.outcome)}`"
              :x="entry.position.x"
              :y="entry.position.y"
            />
          </g>
        </g>
      </svg>

      <div class="legend" aria-label="凡例">
        <span><i class="marker triangle" />超過</span>
        <span><i class="marker circle" />見積どおり</span>
        <span><i class="marker square" />短縮</span>
        <span><i class="line-sample reference" />見積どおり線</span>
        <span v-if="factorMedian !== null"><i class="line-sample trend" />代表係数の傾向線</span>
      </div>
    </div>

    <p v-if="truncated" class="truncated-note">
      最新500件のみ表示しています。必要に応じて期間を絞ってください。
    </p>

    <ChartDataTable
      :id="tableId"
      caption="見積もりと実績の散布図データ"
      :columns="tableColumns"
      :rows="tableRows"
    />

    <ScatterPointModal
      :model-value="selectedPoint !== null"
      :point="selectedPoint"
      @update:model-value="closeModal"
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

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1em;
}

h2 {
  margin: 0;
  font-size: 1.15rem;
}

.section-heading p {
  margin: 0.3em 0 0;
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.scale-toggle {
  display: inline-flex;
  flex-shrink: 0;
  padding: 0.15em;
  border-radius: 6px;
  background: var(--color-surface-muted);
}

.scale-toggle button {
  padding: 0.35em 0.65em;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  font: inherit;
  font-size: 0.8rem;
}

.scale-toggle button[aria-pressed='true'] {
  background: var(--color-surface);
  color: var(--color-text);
  font-weight: 700;
}

.scale-toggle button:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 1px;
}

.chart-content {
  margin-top: 0.7em;
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

.axes line {
  stroke: var(--color-text-muted);
  stroke-width: 1.5;
}

.axes text {
  fill: var(--color-text-muted);
  font-size: 12px;
}

.on-time-zone {
  fill: var(--color-success);
  fill-opacity: 0.13;
}

.reference-line {
  stroke: var(--color-text);
  stroke-width: 2;
}

.trend-line {
  stroke: var(--color-accent);
  stroke-width: 2.5;
  stroke-dasharray: 8 5;
}

.scatter-point {
  cursor: pointer;
}

.scatter-point use {
  stroke: var(--color-surface);
  stroke-width: 2;
  transition: stroke-width 0.1s ease;
}

.scatter-point.late use {
  fill: var(--color-danger);
}

.scatter-point.on-time use {
  fill: var(--color-success);
}

.scatter-point.early use {
  fill: var(--color-task-accent);
}

.scatter-point:focus-visible {
  outline: none;
}

.scatter-point:hover use,
.scatter-point:focus-visible use {
  stroke: var(--color-focus);
  stroke-width: 4;
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

.marker {
  display: inline-block;
  width: 0.65em;
  height: 0.65em;
  background: var(--color-success);
}

.marker.triangle {
  width: 0;
  height: 0;
  border-right: 0.4em solid transparent;
  border-bottom: 0.7em solid var(--color-danger);
  border-left: 0.4em solid transparent;
  background: transparent;
}

.marker.circle {
  border-radius: 50%;
}

.marker.square {
  background: var(--color-task-accent);
}

.line-sample {
  width: 1.5em;
  border-top: 2px solid var(--color-text);
}

.line-sample.trend {
  border-color: var(--color-accent);
  border-top-style: dashed;
}

.empty,
.truncated-note {
  margin: 0.8em 0 0;
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.truncated-note {
  color: var(--color-danger);
}

@media (max-width: 640px) {
  .section-heading {
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  .scatter-point use {
    transition: none;
  }
}
</style>
