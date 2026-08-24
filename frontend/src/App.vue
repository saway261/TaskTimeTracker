<script setup lang="ts">
import { defineAsyncComponent } from 'vue'
import { RouterView } from 'vue-router'
import AppHeader from '@/components/common/AppHeader.vue'
import ToastHost from '@/components/common/ToastHost.vue'
import { useQuickReflectionStore } from '@/stores/quickReflectionStore'
import { useTutorialStore } from '@/stores/tutorialStore'

const tutorialStore = useTutorialStore()
const quickReflectionStore = useQuickReflectionStore()

// 初回ツアー・章再生のいずれも発火しないかぎり読み込まない(要件 §11)。
const TutorialHost = defineAsyncComponent(() => import('@/components/tutorial/TutorialHost.vue'))

// タスクを完了にするまで読み込まない。RouterViewの外に置くことで、完了によって
// 一覧の行が消えても振り返りモーダルが道連れでアンマウントされない。
const QuickReflectionHost = defineAsyncComponent(
  () => import('@/components/reflection/QuickReflectionHost.vue'),
)
</script>

<template>
  <AppHeader />
  <RouterView />
  <ToastHost />
  <TutorialHost v-if="tutorialStore.activeChapterId !== null" />
  <QuickReflectionHost v-if="quickReflectionStore.task !== null" />
</template>
