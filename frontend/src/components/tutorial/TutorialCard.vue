<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import BaseButton from '@/components/common/BaseButton.vue'
import { resolveCardPosition, type Rect } from '@/tutorial/position'

const props = defineProps<{
  chapterTitle: string
  stepTitle: string
  body: string
  stepIndex: number
  stepCount: number
  mode: 'tour' | 'replay'
  rect: Rect | null
}>()

const emit = defineEmits<{
  next: []
  back: []
  skip: []
}>()

const rootRef = ref<HTMLElement | null>(null)
// カード自身の実測サイズが判明するまでの見積り値。フローティングカードの想定幅に合わせている。
const cardSize = ref({ width: 320, height: 160 })
let resizeObserver: ResizeObserver | null = null

function measureCardSize() {
  const el = rootRef.value
  if (!el) return
  const r = el.getBoundingClientRect()
  if (r.width > 0 && r.height > 0) {
    cardSize.value = { width: r.width, height: r.height }
  }
}

const isLastStep = computed(() => props.stepIndex >= props.stepCount - 1)
const nextLabel = computed(() => {
  if (!isLastStep.value) return '次へ'
  return props.mode === 'tour' ? 'はじめる' : '閉じる'
})

// rectがあれば実測サイズをもとに画面内へ収まる位置を計算し、なければCSSでの中央寄せに任せる。
const cardStyle = computed(() => {
  if (!props.rect) return {}
  const viewport = { width: window.innerWidth, height: window.innerHeight }
  const pos = resolveCardPosition(props.rect, cardSize.value, viewport)
  return { top: `${pos.top}px`, left: `${pos.left}px` }
})

function focusableElements(): HTMLElement[] {
  if (!rootRef.value) return []
  return Array.from(
    rootRef.value.querySelectorAll<HTMLElement>(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])',
    ),
  ).filter((el) => !el.hasAttribute('disabled'))
}

// フォーカストラップはBaseModal.vueと同じ方式(window keydown + Tabループ)。新しい流儀を持ち込まない。
function onKeydown(e: KeyboardEvent) {
  if (e.key !== 'Tab') return
  const focusables = focusableElements()
  if (focusables.length === 0) return
  const first = focusables[0]
  const last = focusables[focusables.length - 1]
  if (e.shiftKey && document.activeElement === first) {
    e.preventDefault()
    last.focus()
  } else if (!e.shiftKey && document.activeElement === last) {
    e.preventDefault()
    first.focus()
  }
}

function focus() {
  rootRef.value?.focus()
}

defineExpose({ focus })

onMounted(() => {
  void nextTick(measureCardSize)
  if (typeof ResizeObserver !== 'undefined' && rootRef.value) {
    resizeObserver = new ResizeObserver(measureCardSize)
    resizeObserver.observe(rootRef.value)
  }
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <div
    ref="rootRef"
    class="tutorial-card"
    :class="{ centered: !rect }"
    role="dialog"
    aria-modal="true"
    :aria-label="stepTitle"
    tabindex="-1"
    :style="cardStyle"
  >
    <div class="tutorial-card-meta">
      <p class="chapter-title">{{ chapterTitle }}</p>
      <p class="progress">{{ stepIndex + 1 }} / {{ stepCount }}</p>
    </div>
    <h3 class="step-title">{{ stepTitle }}</h3>
    <p class="body">{{ body }}</p>
    <div class="tutorial-card-actions">
      <button type="button" class="skip-button" @click="emit('skip')">スキップ</button>
      <div class="nav-buttons">
        <BaseButton type="button" variant="secondary" @click="emit('back')">戻る</BaseButton>
        <BaseButton type="button" variant="primary" @click="emit('next')">
          {{ nextLabel }}
        </BaseButton>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tutorial-card {
  position: fixed;
  z-index: 301;
  width: 100%;
  max-width: 320px;
  padding: 1em 1.2em;
  border-radius: 8px;
  background-color: var(--color-surface);
  box-shadow: 0 8px 24px rgb(0 0 0 / 25%);
}

.tutorial-card.centered {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.tutorial-card:focus-visible {
  outline: none;
}

.tutorial-card-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1em;
  margin-bottom: 0.4em;
}

.chapter-title {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.progress {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
  white-space: nowrap;
}

.step-title {
  margin: 0 0 0.4em;
  font-size: 1rem;
  color: var(--color-text);
}

.body {
  margin: 0 0 1em;
  font-size: 0.9rem;
  line-height: 1.6;
  color: var(--color-text);
}

.tutorial-card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1em;
}

.skip-button {
  background: none;
  border: none;
  padding: 0;
  font-size: 0.85rem;
  color: var(--color-text-muted);
  cursor: pointer;
  text-decoration: underline;
}

.skip-button:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.nav-buttons {
  display: flex;
  gap: 0.5em;
}
</style>
