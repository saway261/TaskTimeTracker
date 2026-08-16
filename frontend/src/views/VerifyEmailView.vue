<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import * as authApi from '@/api/authApi'
import AuthCard from '@/components/auth/AuthCard.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import { useAuthStore } from '@/stores/authStore'
import type { ApiError } from '@/types/apiError'

type VerificationState = 'checking' | 'success' | 'missing' | 'error'

const route = useRoute()
const authStore = useAuthStore()
const state = ref<VerificationState>('checking')
const error = ref<ApiError | null>(null)
const token = computed(() =>
  typeof route.query.token === 'string' ? route.query.token.trim() : '',
)

async function verify() {
  if (!token.value) {
    state.value = 'missing'
    return
  }

  state.value = 'checking'
  error.value = null
  try {
    await authApi.verifyEmail(token.value)
    authStore.clear()
    state.value = 'success'
  } catch (e) {
    error.value = e as ApiError
    state.value = 'error'
  }
}

onMounted(verify)
</script>

<template>
  <AuthCard
    id="verify-email"
    title="メールアドレスの確認"
    description="確認リンクを検証しています。"
  >
    <p v-if="state === 'checking'" class="status" role="status">確認しています…</p>

    <div v-else-if="state === 'success'" class="result" role="status">
      <p class="success">メールアドレスを確認しました。</p>
      <p>確認を反映するため、もう一度ログインしてください。</p>
      <RouterLink to="/login" class="action-link">ログイン画面へ</RouterLink>
    </div>

    <div v-else-if="state === 'missing'" class="result" role="alert">
      <p class="failure">確認リンクが正しくありません。</p>
      <p>メールに記載されたリンクを、最初から開き直してください。</p>
      <RouterLink to="/login" class="action-link">ログイン画面へ</RouterLink>
    </div>

    <div v-else class="result">
      <ErrorMessage v-if="error" :error="error" />
      <p>リンクが無効か期限切れの場合は、ログイン後に確認メールを再送してください。</p>
      <BaseButton v-if="error?.status === 0" @click="verify">もう一度試す</BaseButton>
      <RouterLink to="/login" class="action-link">ログイン画面へ</RouterLink>
    </div>
  </AuthCard>
</template>

<style scoped>
.status,
.result p {
  margin: 0;
}

.result {
  display: flex;
  flex-direction: column;
  gap: 1em;
}

.success {
  color: var(--color-accent);
  font-weight: 700;
}

.failure {
  color: var(--color-danger);
  font-weight: 700;
}

.action-link {
  color: var(--color-accent);
  font-weight: 600;
}
</style>
