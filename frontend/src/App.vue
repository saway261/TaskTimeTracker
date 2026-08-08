<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppHeader from '@/components/common/AppHeader.vue'
import ToastHost from '@/components/common/ToastHost.vue'
</script>

<template>
  <AppHeader />
  <RouterView />
  <ToastHost />
</template>
