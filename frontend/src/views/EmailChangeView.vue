<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import * as authApi from '@/api/authApi'
import AuthCard from '@/components/auth/AuthCard.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseInput from '@/components/common/BaseInput.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import { useAuthStore } from '@/stores/authStore'
import type { ApiError } from '@/types/apiError'

const authStore = useAuthStore()
const newEmail = ref('')
const currentPassword = ref('')
const pendingEmail = ref('')
const submitting = ref(false)
const error = ref<ApiError | null>(null)

const canSubmit = computed(() => newEmail.value.trim() !== '' && currentPassword.value !== '')

async function handleSubmit() {
  if (!canSubmit.value) return
  submitting.value = true
  error.value = null
  try {
    const { data } = await authApi.requestEmailChange({
      newEmail: newEmail.value.trim(),
      currentPassword: currentPassword.value,
    })
    pendingEmail.value = data.pendingEmail
    currentPassword.value = ''
  } catch (e) {
    error.value = e as ApiError
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthCard
    id="email-change"
    title="メールアドレス変更"
    description="新しいメールアドレスへ確認メールを送信します。確認が完了するまで、現在のメールアドレスは変更されません。"
  >
    <div v-if="pendingEmail" class="result" role="status">
      <p class="success">確認メールを送信しました。</p>
      <p>
        <strong>{{ pendingEmail }}</strong>
        に届いたリンクを24時間以内に開いてください。
      </p>
      <p>変更を確定すると、すべての端末で再ログインが必要になります。</p>
      <RouterLink
        :to="authStore.currentUser?.emailVerified ? '/projects' : '/email-verification-pending'"
        class="action-link"
      >
        戻る
      </RouterLink>
    </div>

    <form v-else class="auth-form" @submit.prevent="handleSubmit">
      <ErrorMessage v-if="error" :error="error" />
      <BaseInput
        v-model="newEmail"
        label="新しいメールアドレス"
        type="email"
        autocomplete="email"
        :maxlength="254"
        required
        :error="error?.fieldErrors.newEmail"
      />
      <BaseInput
        v-model="currentPassword"
        label="現在のパスワード"
        type="password"
        autocomplete="current-password"
        required
        :error="error?.fieldErrors.currentPassword"
      />
      <BaseButton type="submit" :disabled="submitting || !canSubmit">
        {{ submitting ? '送信中…' : '確認メールを送信する' }}
      </BaseButton>
    </form>
  </AuthCard>
</template>

<style scoped>
.auth-form,
.result {
  display: flex;
  flex-direction: column;
  gap: 1em;
}

.result p {
  margin: 0;
  overflow-wrap: anywhere;
}

.success {
  color: var(--color-accent);
  font-weight: 700;
}

.action-link {
  color: var(--color-accent);
  font-weight: 600;
}
</style>
