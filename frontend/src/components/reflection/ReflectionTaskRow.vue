<script setup lang="ts">
import { computed } from 'vue'
import type { ReflectionTaskResponse } from '@/types/reflection'
import { estimateOutcome, formatGap, formatGapRate, formatMinutes } from '@/utils/duration'
import { truncateForPreview } from '@/utils/reflectionPreview'
import EstimateOutcomeIcon from '@/components/common/EstimateOutcomeIcon.vue'

const props = defineProps<{
  task: ReflectionTaskResponse
}>()

const emit = defineEmits<{
  open: [task: ReflectionTaskResponse]
}>()

const hasReflection = computed(() => props.task.reflection !== null)

// 過去データ不整合などでキャッシュ値が欠けていても画面を止めないよう、ここで一括して「-」に落とす。
const actualText = computed(() =>
  props.task.actualMinutesCached === null ? '-' : formatMinutes(props.task.actualMinutesCached),
)
const gapText = computed(() =>
  props.task.gapMinutesCached === null ? '-' : formatGap(props.task.gapMinutesCached),
)
const gapRateText = computed(() =>
  props.task.gapRateCached === null ? '-' : formatGapRate(props.task.gapRateCached),
)
const outcome = computed(() => estimateOutcome(props.task.gapRateCached))

const causePreview = computed(() => truncateForPreview(props.task.reflection?.cause ?? null))
const nextActionPreview = computed(() => {
  const nextAction = props.task.reflection?.nextAction
  return nextAction ? truncateForPreview(nextAction) : null
})

const actionLabel = computed(() =>
  hasReflection.value
    ? `${props.task.title} の振り返りを確認・変更する`
    : `${props.task.title} の振り返りを入力する`,
)
</script>

<template>
  <div class="reflection-task-row" :class="{ filled: hasReflection, unfilled: !hasReflection }">
    <div class="row-main">
      <span class="title">{{ task.title }}</span>
      <button
        type="button"
        class="action-button"
        :aria-label="actionLabel"
        @click="emit('open', task)"
      >
        {{ hasReflection ? '詳細・変更' : '入力する' }}
      </button>
    </div>

    <div v-if="hasReflection" class="preview">
      <p class="preview-line"><span class="preview-label">原因：</span>{{ causePreview }}</p>
      <p v-if="nextActionPreview" class="preview-line">
        <span class="preview-label">改善：</span>{{ nextActionPreview }}
      </p>
    </div>

    <div class="row-meta">
      <span class="meta-item meta-gap outcome-meta" :class="outcome">
        <EstimateOutcomeIcon :gap-rate="task.gapRateCached" size="small" />
        <span class="meta-gap-text">誤差 {{ gapText }}</span>
      </span>
      <span class="meta-item meta-gap-rate outcome-meta" :class="outcome">
        誤差比 {{ gapRateText }}
      </span>
      <span class="meta-item meta-actual">実績 {{ actualText }}</span>
    </div>
  </div>
</template>

<style scoped>
.reflection-task-row {
  display: flex;
  flex-direction: column;
  gap: 0.5em;
  min-width: 0;
  padding: 0.7em 1em;
  border-radius: 8px;
  background-color: var(--color-surface);
  border: 1px solid var(--color-surface-muted);
}

/* 未入力／入力済みは色だけでなく枠線の種類・太さでも判別できるようにする（§5.2）。 */
.reflection-task-row.unfilled {
  border-style: dashed;
  border-color: var(--color-text-muted);
}

.reflection-task-row.filled {
  border-left: 3px solid var(--color-accent);
}

.row-main {
  display: flex;
  align-items: center;
  gap: 0.8em;
}

.title {
  flex: 1;
  min-width: 0;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text);
}

.action-button {
  flex-shrink: 0;
  padding: 0.4em 0.9em;
  border-radius: 6px;
  border: 1px solid var(--color-text-muted);
  background-color: transparent;
  color: var(--color-text);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
}

.action-button:hover {
  background-color: var(--color-surface-muted);
}

.action-button:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.preview {
  display: flex;
  flex-direction: column;
  gap: 0.3em;
}

/* 改行を含む場合も文字数で切っているため、崩れないよう表示側で最大3行に制限する（§5.4）。 */
.preview-line {
  margin: 0;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  white-space: pre-line;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.preview-label {
  font-weight: 600;
  color: var(--color-text);
}

.row-meta {
  display: flex;
  gap: 1em;
  flex-wrap: wrap;
}

.meta-item {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  white-space: nowrap;
}

.outcome-meta {
  display: inline-flex;
  align-items: center;
  gap: 0.3em;
}

.outcome-meta.early {
  color: var(--color-task-accent);
}

.outcome-meta.late {
  color: var(--color-danger);
}

.outcome-meta.on-time {
  color: var(--color-success);
}

/* §5.2のレスポンシブ優先順位に沿って低優先度の情報を順に省略する。
   狭い画面でも見積結果の表情アイコンだけは残す。 */
@media (max-width: 900px) {
  .meta-actual {
    display: none;
  }
}

@media (max-width: 760px) {
  .meta-gap-rate {
    display: none;
  }
}

@media (max-width: 640px) {
  .meta-gap-text {
    display: none;
  }
}

@media (max-width: 480px) {
  .preview {
    display: none;
  }
}
</style>
