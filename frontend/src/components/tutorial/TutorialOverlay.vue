<script setup lang="ts">
import { computed } from 'vue'
import type { Rect } from '@/tutorial/position'

const props = defineProps<{
  rect: Rect | null
}>()

// clip-pathで外周と内周(くり抜き)を逆回りに繋いだ一筆書きのポリゴンを作る(非零規則で穴になる)。
// rectがnullのときは全面を暗くする(要件 §9.6・§10.1)。
const clipPath = computed(() => {
  const r = props.rect
  if (!r) return 'none'

  const pad = 6
  const top = Math.max(r.top - pad, 0)
  const left = Math.max(r.left - pad, 0)
  const right = r.right + pad
  const bottom = r.bottom + pad

  // 外周(下→右→上→左)と内周が同じ回転方向だと、非零規則では巻き数が打ち消し合わず
  // 穴が開かない(両方とも塗りつぶし対象になり、画面全体が暗くなる)。内周は外周と
  // 逆回りにする必要がある。
  return (
    `polygon(` +
    `0px 0px, 0px 100vh, 100vw 100vh, 100vw 0px, 0px 0px, ` +
    `${left}px ${top}px, ${right}px ${top}px, ${right}px ${bottom}px, ${left}px ${bottom}px, ${left}px ${top}px` +
    `)`
  )
})
</script>

<template>
  <!-- 下: 全面を覆いすべてのポインタ操作を受け止めるブロッカー。くり抜き内側でも
       誤操作を防ぐため、クリックで閉じる等の挙動は持たせない(要件 §10.1)。 -->
  <div class="tutorial-blocker" />
  <!-- 上: 視覚的な暗幕。pointer-events:noneでクリックを下のブロッカーへ通す。 -->
  <div class="tutorial-scrim" :style="{ clipPath }" />
</template>

<style scoped>
.tutorial-blocker,
.tutorial-scrim {
  position: fixed;
  inset: 0;
  z-index: 300;
}

.tutorial-blocker {
  pointer-events: auto;
  background: transparent;
}

.tutorial-scrim {
  pointer-events: none;
  background-color: rgb(0 0 0 / 55%);
  transition: clip-path 0.2s ease;
}

@media (prefers-reduced-motion: reduce) {
  .tutorial-scrim {
    transition: none;
  }
}
</style>
