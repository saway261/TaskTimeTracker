<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import AuthCard from '@/components/auth/AuthCard.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseInput from '@/components/common/BaseInput.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import { useAuthStore } from '@/stores/authStore'
import { useNotificationStore } from '@/stores/notificationStore'
import type { ApiError } from '@/types/apiError'
import { validateNewPassword, validatePasswordConfirmation } from '@/utils/authValidation'

const router = useRouter()
const authStore = useAuthStore()
const notification = useNotificationStore()

const email = ref('')
const password = ref('')
const passwordConfirmation = ref('')
const submitting = ref(false)
const error = ref<ApiError | null>(null)

const passwordError = computed(() =>
  password.value === '' ? undefined : validateNewPassword(password.value),
)
const confirmationError = computed(() =>
  passwordConfirmation.value === ''
    ? undefined
    : validatePasswordConfirmation(password.value, passwordConfirmation.value),
)
const canSubmit = computed(
  () =>
    email.value.trim() !== '' &&
    password.value !== '' &&
    passwordConfirmation.value !== '' &&
    !validateNewPassword(password.value) &&
    !validatePasswordConfirmation(password.value, passwordConfirmation.value),
)

async function handleSubmit() {
  if (!canSubmit.value) return
  submitting.value = true
  error.value = null
  try {
    await authStore.register({ email: email.value.trim(), password: password.value })
    notification.success('確認メールを送信しました。')
    await router.replace('/email-verification-pending')
  } catch (e) {
    error.value = e as ApiError
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthCard
    id="register"
    title="ユーザー登録"
    description="新しいアカウントを作成します。パスワードは12文字以上で設定してください。"
  >
    <form class="auth-form" @submit.prevent="handleSubmit">
      <ErrorMessage v-if="error" :error="error" />
      <BaseInput
        v-model="email"
        label="メールアドレス"
        type="email"
        autocomplete="username"
        :maxlength="254"
        required
        :error="error?.fieldErrors.email"
      />
      <BaseInput
        v-model="password"
        label="パスワード"
        type="password"
        autocomplete="new-password"
        :minlength="12"
        required
        :error="error?.fieldErrors.password ?? passwordError"
      />
      <BaseInput
        v-model="passwordConfirmation"
        label="パスワード（確認）"
        type="password"
        autocomplete="new-password"
        :minlength="12"
        required
        :error="confirmationError"
      />
      <BaseButton type="submit" :disabled="submitting || !canSubmit">
        {{ submitting ? '登録中…' : '登録する' }}
      </BaseButton>
    </form>
    <template #footer>
      アカウントをお持ちの方は
      <RouterLink to="/login">ログイン</RouterLink>
    </template>
  </AuthCard>
</template>

<style scoped>
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 1em;
}
</style>
