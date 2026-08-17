<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import * as authApi from '@/api/authApi'
import AuthCard from '@/components/auth/AuthCard.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import { useAuthStore } from '@/stores/authStore'
import type { ApiError } from '@/types/apiError'

type ConfirmationState = 'checking' | 'success' | 'missing' | 'invalid' | 'conflict' | 'error'

const route = useRoute()
const authStore = useAuthStore()
const state = ref<ConfirmationState>('checking')
const error = ref<ApiError | null>(null)
const token = computed(() =>
  typeof route.query.token === 'string' ? route.query.token.trim() : '',
)

async function confirm() {
  if (!token.value) {
    state.value = 'missing'
    return
  }

  state.value = 'checking'
  error.value = null
  try {
    await authApi.confirmEmailChange(token.value)
    authStore.clear()
    state.value = 'success'
  } catch (e) {
    error.value = e as ApiError
    if (error.value.status === 409) {
      state.value = 'conflict'
    } else if (error.value.status === 400) {
      state.value = 'invalid'
    } else {
      state.value = 'error'
    }
  }
}

onMounted(confirm)
</script>

<template>
  <AuthCard
    id="verify-email-change"
    title="メールアドレス変更の確認"
    description="変更確認リンクを検証しています。"
  >
    <p v-if="state === 'checking'" class="status" role="status">確認しています…</p>

    <div v-else-if="state === 'success'" class="result" role="status">
      <p class="success">メールアドレスを変更しました。</p>
      <p>新しいメールアドレスと現在のパスワードでログインしてください。</p>
      <RouterLink to="/login" class="action-link">ログイン画面へ</RouterLink>
    </div>

    <div v-else-if="state === 'conflict'" class="result" role="alert">
      <p class="failure">そのメールアドレスは他の利用者に使用されています。</p>
      <p>別のメールアドレスで、変更を最初からやり直してください。</p>
      <RouterLink to="/login" class="action-link">ログイン画面へ</RouterLink>
    </div>

    <div v-else-if="state === 'invalid' || state === 'missing'" class="result" role="alert">
      <p class="failure">リンクが無効か期限切れです。</p>
      <p>
        既に変更が完了している可能性があります。新しいメールアドレスでログインをお試しください。
      </p>
      <RouterLink to="/login" class="action-link">ログイン画面へ</RouterLink>
    </div>

    <div v-else class="result" role="alert">
      <p class="failure">メールアドレスの変更を確認できませんでした。</p>
      <p>{{ error?.message }}</p>
      <BaseButton @click="confirm">もう一度試す</BaseButton>
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
