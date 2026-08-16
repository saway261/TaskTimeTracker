<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import * as authApi from '@/api/authApi'
import AuthCard from '@/components/auth/AuthCard.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import { useAuthStore } from '@/stores/authStore'
import { useNotificationStore } from '@/stores/notificationStore'
import type { ApiError } from '@/types/apiError'

const authStore = useAuthStore()
const notification = useNotificationStore()
const resending = ref(false)
const error = ref<ApiError | null>(null)

async function resend() {
  resending.value = true
  error.value = null
  try {
    await authApi.resendVerificationEmail()
    notification.success('確認メールを再送しました。')
  } catch (e) {
    error.value = e as ApiError
  } finally {
    resending.value = false
  }
}
</script>

<template>
  <AuthCard
    id="email-verification-pending"
    title="確認メールを送信しました"
    description="メールに記載されたリンクを開いて、メールアドレスの確認を完了してください。"
  >
    <div class="pending-content">
      <ErrorMessage v-if="error" :error="error" />
      <p v-if="authStore.currentUser?.email" class="destination">
        送信先：<strong>{{ authStore.currentUser.email }}</strong>
      </p>
      <p class="hint">
        メールが届かない場合は、迷惑メールフォルダーを確認してから再送してください。
      </p>
      <BaseButton :disabled="resending" @click="resend">
        {{ resending ? '再送中…' : '確認メールを再送する' }}
      </BaseButton>
    </div>
    <template #footer>
      メールアドレスを間違えた場合は
      <RouterLink to="/email-change">こちらから変更</RouterLink>
    </template>
  </AuthCard>
</template>

<style scoped>
.pending-content {
  display: flex;
  flex-direction: column;
  gap: 1em;
}

.destination,
.hint {
  margin: 0;
}

.destination {
  overflow-wrap: anywhere;
}

.hint {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}
</style>
