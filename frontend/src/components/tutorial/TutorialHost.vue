<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/authStore'
import { useNotificationStore } from '@/stores/notificationStore'
import { useTutorialStore } from '@/stores/tutorialStore'
import { resolveAnchor } from '@/tutorial/anchor'
import { findChapter } from '@/tutorial/chapters'
import type { Rect } from '@/tutorial/position'
import type { TutorialScope } from '@/tutorial/scopes'
import type { TutorialStep } from '@/tutorial/types'
import TutorialOverlay from './TutorialOverlay.vue'
import TutorialCard from './TutorialCard.vue'

// activeChapterIdからの章解決はここで行う(App.vue側では行わない)。App.vueがchapters/を
// importすると、そこに積まれる本文が常時ロードされる本体バンドルへ含まれてしまい、
// 動的importで分離した意味がなくなるため(要件 §11・実装計画 §0-2-6)。
const store = useTutorialStore()

const chapter = computed(() =>
  store.activeChapterId ? findChapter(store.activeChapterId) : undefined,
)
// スコープ指定がある場合は、絞り込んだ結果をここへ入れる(onMountedで一度だけ決める)。
// 再生中に絞り込み結果が変わるとステップ番号がずれるため、開始時点で固定する。
const scopedSteps = ref<TutorialStep[] | null>(null)
const steps = computed<TutorialStep[]>(() => scopedSteps.value ?? chapter.value?.steps ?? [])
const chapterTitle = computed(() => chapter.value?.title ?? '')

const currentStep = computed<TutorialStep | null>(() => steps.value[store.stepIndex] ?? null)

const resolvedEl = ref<HTMLElement | null>(null)
const anchorRect = ref<Rect | null>(null)
const cardRef = ref<InstanceType<typeof TutorialCard> | null>(null)
let previouslyFocused: HTMLElement | null = null
let rafId = 0

// 今いる画面の範囲。ヘッダーは全画面に常駐する共通UIなので常にスコープへ含める
// (稼働中タイマーの説明などが、どの画面から再生しても出るようにするため)。
// スコープ要素が見つからないときはundefinedを返し、絞り込みを行わない(安全側)。
function currentScopeRoots(): Element[] | undefined {
  if (!store.scopeSelector) return undefined
  const screen = document.querySelector(store.scopeSelector)
  if (!screen) return undefined
  const header = document.querySelector('.app-header')
  return header ? [screen, header] : [screen]
}

// onMissing:'skip' はデバイス上に存在しない機能向け。before で開くメニューの中身を
// 対象にする場合、この判定は before 実行前のDOM状態を見るため、
// onMissing:'skip' と before を同じステップで組み合わせてはならない(§F2実装注記)。
function isUnavailable(step: TutorialStep): boolean {
  return step.onMissing === 'skip' && resolveAnchor(step.targets, currentScopeRoots()) === null
}

function findNextAvailable(from: number): number | null {
  let idx = from
  while (idx < steps.value.length && isUnavailable(steps.value[idx])) idx += 1
  return idx < steps.value.length ? idx : null
}

function findPreviousAvailable(from: number): number | null {
  let idx = from
  while (idx >= 0 && isUnavailable(steps.value[idx])) idx -= 1
  return idx >= 0 ? idx : null
}

async function callHook(hook?: () => Promise<void> | void) {
  if (!hook) return
  try {
    await hook()
  } catch {
    // フックが失敗してもチュートリアルを止めない。
  }
}

// DOMRectはwidth/height/x/y/toJSONも持つため、Rect型の4フィールドへ narrow する。
function toRect(domRect: DOMRect): Rect {
  return { top: domRect.top, left: domRect.left, right: domRect.right, bottom: domRect.bottom }
}

function reresolveAnchor() {
  const step = currentStep.value
  if (!step) {
    resolvedEl.value = null
    anchorRect.value = null
    return
  }
  const el = resolveAnchor(step.targets, currentScopeRoots())
  resolvedEl.value = el
  if (el) {
    el.scrollIntoView({ block: 'center' })
    anchorRect.value = toRect(el.getBoundingClientRect())
  } else {
    anchorRect.value = null
  }
}

function remeasureAnchor() {
  if (resolvedEl.value) {
    anchorRect.value = toRect(resolvedEl.value.getBoundingClientRect())
  }
}

async function focusCard() {
  await nextTick()
  cardRef.value?.focus()
}

async function goToStep(index: number, hook?: () => Promise<void> | void) {
  store.goTo(index)
  await callHook(hook)
  reresolveAnchor()
  await focusCard()
}

// 初回ツアー(mode: 'tour')は、完走・スキップのいずれでも完了記録を行う(要件 §6.3)。
// 章再生(mode: 'replay')では呼ばない。既にonboardingCompletedがtrueであり、
// 状態を変える必要がないため。
async function finish() {
  await callHook(currentStep.value?.after)
  const isTour = store.mode === 'tour'
  store.end()
  if (!isTour) return

  // UIを閉じた後に完了記録を行う。APIが失敗してもユーザーの操作は妨げない
  // (要件 §6.4)。フラグが立たなければ次回ログイン時に再発火するが、
  // tourAttemptedにより同一セッション中の再発火は防がれる。
  try {
    await useAuthStore().completeOnboarding()
  } catch {
    // エラー通知は出さない(要件 §6.4)。ユーザーが取れる対処が無いため。
  }
  useNotificationStore().info('チュートリアルはユーザーメニューからいつでも見られます。')
}

async function handleNext() {
  const target = findNextAvailable(store.stepIndex + 1)
  if (target === null) {
    await finish()
    return
  }
  await callHook(currentStep.value?.after)
  await goToStep(target, steps.value[target].before)
}

async function handleBack() {
  const target = findPreviousAvailable(store.stepIndex - 1)
  if (target === null) return
  await callHook(currentStep.value?.after)
  await goToStep(target, steps.value[target].before)
}

async function handleSkip() {
  await finish()
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    e.preventDefault()
    void handleSkip()
    return
  }
  if (e.key === 'ArrowRight') {
    e.preventDefault()
    void handleNext()
    return
  }
  if (e.key === 'ArrowLeft') {
    e.preventDefault()
    void handleBack()
    return
  }
  // ボタンにフォーカスがある状態のEnterはネイティブのボタン活性化に任せ、二重発火を避ける。
  if (e.key === 'Enter' && !(e.target instanceof HTMLButtonElement)) {
    e.preventDefault()
    void handleNext()
  }
}

function onResize() {
  cancelAnimationFrame(rafId)
  rafId = requestAnimationFrame(reresolveAnchor)
}

function onScroll() {
  cancelAnimationFrame(rafId)
  rafId = requestAnimationFrame(remeasureAnchor)
}

// スコープ指定時は「今いる画面にある要素を指すステップ」だけへ絞り込む(要件 §7.2)。
// アンカーを持たない概念だけのステップは、画面の説明ではないためスコープ再生から外す。
// 絞り込んだ結果が0件になる場合は、何も出ないより良いので章全体へフォールバックする。
function applyScope() {
  const roots = currentScopeRoots()
  if (!roots || !chapter.value) return
  const scope = store.scopeSelector
  const filtered = chapter.value.steps.filter((step) => {
    // scopes を持つステップは、列挙された画面から再生したときだけ表示する。
    // ヘッダー常駐の要素はどの画面でもアンカーが解決できてしまうため、
    // アンカーの有無だけでは「意味のある画面」を絞れないケースがある。
    if (step.scopes && (scope === null || !step.scopes.includes(scope as TutorialScope))) {
      return false
    }
    return step.targets !== undefined && resolveAnchor(step.targets, roots) !== null
  })
  scopedSteps.value = filtered.length > 0 ? filtered : null
}

onMounted(async () => {
  if (!chapter.value) {
    // activeChapterIdに対応する章が見つからない(通常起こらない防御的分岐)。
    store.end()
    return
  }

  previouslyFocused = document.activeElement as HTMLElement | null

  applyScope()

  const start = findNextAvailable(store.stepIndex)
  if (start === null) {
    store.end()
    return
  }
  if (start !== store.stepIndex) {
    store.goTo(start)
  }
  await callHook(currentStep.value?.before)
  reresolveAnchor()
  await focusCard()

  window.addEventListener('keydown', onKeydown)
  window.addEventListener('resize', onResize)
  window.addEventListener('orientationchange', onResize)
  window.addEventListener('scroll', onScroll, { capture: true, passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('resize', onResize)
  window.removeEventListener('orientationchange', onResize)
  window.removeEventListener('scroll', onScroll, true)
  cancelAnimationFrame(rafId)
  previouslyFocused?.focus()
})
</script>

<template>
  <template v-if="chapter">
    <TutorialOverlay :rect="anchorRect" />
    <TutorialCard
      v-if="currentStep"
      ref="cardRef"
      :chapter-title="chapterTitle"
      :step-title="currentStep.title"
      :body="currentStep.body"
      :step-index="store.stepIndex"
      :step-count="steps.length"
      :mode="store.mode ?? 'replay'"
      :rect="anchorRect"
      @next="handleNext"
      @back="handleBack"
      @skip="handleSkip"
    />
  </template>
</template>
