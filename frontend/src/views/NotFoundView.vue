<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <div class="not-found">
    <h1>ページが見つかりません</h1>
    <p>指定されたURLに対応するページは存在しません。</p>
    <RouterLink to="/projects">タスク管理へ戻る</RouterLink>
  </div>
</template>

<style scoped>
.not-found {
  padding: 2em;
  text-align: center;
}
</style>
