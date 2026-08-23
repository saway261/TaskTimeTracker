<script setup lang="ts">
import { defineAsyncComponent } from 'vue'
import { RouterView } from 'vue-router'
import AppHeader from '@/components/common/AppHeader.vue'
import ToastHost from '@/components/common/ToastHost.vue'
import { useTutorialStore } from '@/stores/tutorialStore'

const tutorialStore = useTutorialStore()

// 初回ツアー・章再生のいずれも発火しないかぎり読み込まない(要件 §11)。
const TutorialHost = defineAsyncComponent(() => import('@/components/tutorial/TutorialHost.vue'))
</script>

<template>
  <AppHeader />
  <RouterView />
  <ToastHost />
  <TutorialHost v-if="tutorialStore.activeChapterId !== null" />
</template>
