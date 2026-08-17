<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { requestPasswordReset } from '@/api/authApi'
import AuthCard from '@/components/auth/AuthCard.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseInput from '@/components/common/BaseInput.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import type { ApiError } from '@/types/apiError'

const email = ref('')
const submitting = ref(false)
const completed = ref(false)
const error = ref<ApiError | null>(null)

const canSubmit = computed(() => email.value.trim() !== '')

async function handleSubmit() {
  if (!canSubmit.value) return
  submitting.value = true
  error.value = null

  try {
    await requestPasswordReset({ email: email.value.trim() })
    completed.value = true
  } catch (e) {
    error.value = e as ApiError
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthCard
    id="password-reset-request"
    title="パスワード再設定"
    description="登録したメールアドレスを入力してください。"
  >
    <div v-if="completed" class="completion-message" role="status">
      <p>入力したメールアドレスが登録されている場合、パスワード再設定メールを送信しました。</p>
      <p>メールに記載されたリンクから、新しいパスワードを設定してください。</p>
    </div>
    <form v-else class="auth-form" @submit.prevent="handleSubmit">
      <ErrorMessage v-if="error" :error="error" />
      <BaseInput
        v-model="email"
        label="メールアドレス"
        type="email"
        autocomplete="email"
        :maxlength="254"
        required
        :error="error?.fieldErrors.email"
      />
      <BaseButton type="submit" :disabled="submitting || !canSubmit">
        {{ submitting ? '送信中…' : '再設定メールを送信する' }}
      </BaseButton>
    </form>
    <template #footer>
      <RouterLink to="/login">ログインへ戻る</RouterLink>
    </template>
  </AuthCard>
</template>

<style scoped>
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 1em;
}

.completion-message {
  padding: 1em;
  border-radius: 6px;
  background: var(--color-surface-muted);
}

.completion-message p {
  margin: 0;
}

.completion-message p + p {
  margin-top: 0.6em;
}
</style>
