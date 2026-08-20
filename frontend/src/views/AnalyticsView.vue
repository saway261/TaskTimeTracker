<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useAnalyticsStore } from '@/stores/analyticsStore'
import { useProjectStore } from '@/stores/projectStore'
import { useCauseCategoryStore } from '@/stores/causeCategoryStore'
import type { AnalyticsPeriod, ReflectionOutcomeFilter } from '@/types/analytics'
import AnalyticsFilterBar from '@/components/analytics/AnalyticsFilterBar.vue'
import AccuracySummaryTiles from '@/components/analytics/AccuracySummaryTiles.vue'
import DiagnosisCard from '@/components/analytics/DiagnosisCard.vue'
import AccuracyTrendChart from '@/components/analytics/AccuracyTrendChart.vue'
import EstimateActualScatter from '@/components/analytics/EstimateActualScatter.vue'
import SizeBucketChart from '@/components/analytics/SizeBucketChart.vue'
import GapCauseChart from '@/components/analytics/GapCauseChart.vue'
import ReflectionTimeline from '@/components/analytics/ReflectionTimeline.vue'
import LoadingIndicator from '@/components/common/LoadingIndicator.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import BaseButton from '@/components/common/BaseButton.vue'

const analyticsStore = useAnalyticsStore()
const projectStore = useProjectStore()
const categoryStore = useCauseCategoryStore()

const initialLoading = computed(() => analyticsStore.refreshing)
const empty = computed(
  () =>
    analyticsStore.accuracy?.analyzedTaskCount === 0 && analyticsStore.timeline?.totalCount === 0,
)

function changeProject(projectId: number | null) {
  void analyticsStore.setProject(projectId)
}

function changePeriod(period: AnalyticsPeriod) {
  void analyticsStore.setPeriod(period)
}

function changeCauseCategory(causeCategory: string | null) {
  void analyticsStore.setCauseCategory(causeCategory)
}

function changeOutcome(outcome: ReflectionOutcomeFilter) {
  void analyticsStore.setOutcome(outcome)
}

function load() {
  void projectStore.fetchProjects().catch(() => {})
  void categoryStore.fetchCategories().catch(() => {})
  void analyticsStore.refresh()
}

onMounted(load)
</script>

<template>
  <main class="analytics-view">
    <h1>分析</h1>

    <AnalyticsFilterBar
      :filter="analyticsStore.filter"
      :projects="projectStore.projects"
      :accuracy="analyticsStore.accuracy"
      :disabled="analyticsStore.refreshing"
      @project-change="changeProject"
      @period-change="changePeriod"
    />
    <p v-if="projectStore.error" class="filter-note">
      プロジェクト一覧を取得できなかったため、全プロジェクトの分析を表示しています。
    </p>

    <ErrorMessage v-if="analyticsStore.error" :error="analyticsStore.error" />
    <BaseButton
      v-if="analyticsStore.error"
      class="retry-button"
      variant="secondary"
      :disabled="analyticsStore.refreshing"
      @click="analyticsStore.refresh"
    >
      再試行
    </BaseButton>

    <LoadingIndicator v-if="initialLoading" />
    <template v-else>
      <div v-if="empty" class="empty-state">
        <h2>分析できるタスクがまだありません</h2>
        <p>タスクを完了させると、ここに見積もり精度と振り返りが表示されます。</p>
      </div>
      <template v-else>
        <AccuracySummaryTiles
          v-if="analyticsStore.accuracy"
          :summary="analyticsStore.accuracy.summary"
        />
        <DiagnosisCard
          v-if="analyticsStore.accuracy?.diagnosis"
          :diagnosis="analyticsStore.accuracy.diagnosis"
        />
        <div v-if="analyticsStore.accuracy" class="analytics-charts">
          <AccuracyTrendChart
            :points="analyticsStore.accuracy.trend"
            :availability="analyticsStore.accuracy.trendAvailability"
            :threshold-percent="analyticsStore.accuracy.onTimeThresholdPercent"
          />
          <EstimateActualScatter
            :points="analyticsStore.accuracy.scatter"
            :threshold-percent="analyticsStore.accuracy.onTimeThresholdPercent"
            :factor-median="analyticsStore.accuracy.summary.factorMedian"
            :truncated="analyticsStore.accuracy.scatterTruncated"
          />
          <SizeBucketChart
            :buckets="analyticsStore.accuracy.sizeBuckets"
            :threshold-percent="analyticsStore.accuracy.onTimeThresholdPercent"
          />
          <GapCauseChart
            v-if="analyticsStore.gapCauses"
            :aggregate="analyticsStore.gapCauses"
            :selected-cause-category="analyticsStore.filter.causeCategory"
            @cause-category-change="changeCauseCategory"
          />
        </div>
        <ReflectionTimeline
          :timeline="analyticsStore.timeline"
          :categories="categoryStore.categories"
          :cause-category="analyticsStore.filter.causeCategory"
          :outcome="analyticsStore.filter.outcome"
          :loading="analyticsStore.loadingTimeline"
          @cause-category-change="changeCauseCategory"
          @outcome-change="changeOutcome"
          @load-more="analyticsStore.loadMoreTimeline"
        />
      </template>
    </template>
  </main>
</template>

<style scoped>
.analytics-view {
  width: min(1200px, 100%);
  margin: 0 auto;
  padding: 1.2em;
  display: flex;
  flex-direction: column;
  gap: 1em;
  overflow-x: clip;
}

h1 {
  margin: 0;
}

.filter-note {
  margin: -0.4em 0 0;
  color: var(--color-text-muted);
  font-size: 0.8rem;
}

.retry-button {
  align-self: flex-start;
}

.analytics-charts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1em;
}

.empty-state {
  padding: 1.2em;
  border: 1px dashed var(--color-surface-muted);
  border-radius: 8px;
  background: var(--color-surface);
  text-align: center;
}

.empty-state h2 {
  margin: 0;
  font-size: 1.05rem;
}

.empty-state p {
  margin: 0.5em 0 0;
  color: var(--color-text-muted);
}

@media (max-width: 640px) {
  .analytics-view {
    padding: 0.9em 0.75em;
  }
}

@media (max-width: 960px) {
  .analytics-charts {
    grid-template-columns: 1fr;
  }
}
</style>
