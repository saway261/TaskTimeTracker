<script setup lang="ts">
import { computed } from 'vue'
import type { AccuracySummaryResponse, RecentTrend } from '@/types/analytics'
import EstimateOutcomeIcon from '@/components/common/EstimateOutcomeIcon.vue'
import MetricUnavailable from './MetricUnavailable.vue'

const props = defineProps<{
  summary: AccuracySummaryResponse
}>()

const numberFormatter = new Intl.NumberFormat('ja-JP', { maximumFractionDigits: 1 })
const factorText = computed(() =>
  props.summary.factorMedian === null ? '-' : `${props.summary.factorMedian.toFixed(1)}倍`,
)
const factorGapRate = computed(() =>
  props.summary.factorMedian === null ? null : (props.summary.factorMedian - 1) * 100,
)
const factorDescription = computed(() => {
  const factor = props.summary.factorMedian
  if (factor === null) return ''
  if (factor > 1) return `見積もりの${factor.toFixed(1)}倍かかっています`
  if (factor < 1) return `見積もりの${factor.toFixed(1)}倍で完了しています`
  return '見積もりと実績が一致しています'
})
const quartileText = computed(() => {
  const { factorP25, factorP75 } = props.summary
  return factorP25 === null || factorP75 === null
    ? ''
    : `係数の中央50%: ${factorP25.toFixed(1)}〜${factorP75.toFixed(1)}倍`
})

const trendLabels: Record<RecentTrend, string> = {
  IMPROVED: '改善',
  STABLE: '横ばい',
  WORSENED: '悪化',
}
const trendText = computed(() =>
  props.summary.recentTrend === null ? null : trendLabels[props.summary.recentTrend],
)
const trendDescription = computed(() => {
  const { recentVariancePercent, previousVariancePercent } = props.summary
  if (recentVariancePercent === null || previousVariancePercent === null) return ''
  return `直近10件 ${numberFormatter.format(recentVariancePercent)}% / その前 ${numberFormatter.format(previousVariancePercent)}%`
})
</script>

<template>
  <section class="accuracy-summary" aria-labelledby="accuracy-summary-title">
    <h2 id="accuracy-summary-title">見積もり精度サマリー</h2>
    <div class="summary-tiles">
      <article class="summary-tile">
        <h3>オンタイム率</h3>
        <MetricUnavailable
          v-if="!summary.availability.available"
          :availability="summary.availability"
        />
        <template v-else>
          <p class="value">{{ numberFormatter.format(summary.onTimeRate ?? 0) }}%</p>
        </template>
        <p class="support breakdown">
          <span>超過 {{ summary.outcomeBreakdown.lateCount }}件</span>
          <span>見積どおり {{ summary.outcomeBreakdown.onTimeCount }}件</span>
          <span>短縮 {{ summary.outcomeBreakdown.earlyCount }}件</span>
        </p>
      </article>

      <article class="summary-tile">
        <h3>代表係数</h3>
        <MetricUnavailable
          v-if="!summary.availability.available"
          :availability="summary.availability"
        />
        <template v-else>
          <p class="value factor-value">
            <EstimateOutcomeIcon :gap-rate="factorGapRate" />
            <span>{{ factorText }}</span>
          </p>
          <p class="support">{{ factorDescription }}</p>
        </template>
      </article>

      <article class="summary-tile">
        <h3>ばらつき</h3>
        <MetricUnavailable
          v-if="!summary.availability.available"
          :availability="summary.availability"
        />
        <template v-else>
          <p class="value">±{{ numberFormatter.format(summary.variancePercent ?? 0) }}%</p>
          <p class="support">{{ quartileText }}</p>
        </template>
      </article>

      <article class="summary-tile" :class="summary.recentTrend?.toLowerCase()">
        <h3>直近の傾向</h3>
        <MetricUnavailable
          v-if="!summary.availability.available"
          :availability="summary.availability"
        />
        <template v-else-if="trendText">
          <p class="value">{{ trendText }}</p>
          <p class="support">{{ trendDescription }}</p>
        </template>
        <p v-else class="support">比較できるデータがまだありません</p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.accuracy-summary {
  display: flex;
  flex-direction: column;
  gap: 0.7em;
}

h2 {
  margin: 0;
  font-size: 1.15rem;
}

.summary-tiles {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.8em;
}

.summary-tile {
  min-width: 0;
  padding: 1em;
  border: 1px solid var(--color-surface-muted);
  border-radius: 8px;
  background: var(--color-surface);
}

h3 {
  margin: 0 0 0.65em;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.value {
  margin: 0;
  color: var(--color-text);
  font-size: clamp(1.35rem, 3vw, 1.8rem);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.factor-value {
  display: flex;
  align-items: center;
  gap: 0.35em;
}

.support {
  margin: 0.55em 0 0;
  color: var(--color-text-muted);
  font-size: 0.8rem;
  line-height: 1.45;
}

.breakdown {
  display: flex;
  flex-direction: column;
  gap: 0.15em;
}

.improved .value {
  color: var(--color-success);
}

.worsened .value {
  color: var(--color-danger);
}

@media (max-width: 900px) {
  .summary-tiles {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 420px) {
  .summary-tile {
    padding: 0.8em;
  }
}
</style>
