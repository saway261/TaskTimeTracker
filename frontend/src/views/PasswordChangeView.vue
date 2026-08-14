<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
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

const currentPassword = ref('')
const newPassword = ref('')
const passwordConfirmation = ref('')
const submitting = ref(false)
const error = ref<ApiError | null>(null)

const newPasswordError = computed(() =>
  newPassword.value === '' ? undefined : validateNewPassword(newPassword.value),
)
const confirmationError = computed(() =>
  passwordConfirmation.value === ''
    ? undefined
    : validatePasswordConfirmation(newPassword.value, passwordConfirmation.value),
)
const canSubmit = computed(
  () =>
    currentPassword.value !== '' &&
    newPassword.value !== '' &&
    passwordConfirmation.value !== '' &&
    !validateNewPassword(newPassword.value) &&
    !validatePasswordConfirmation(newPassword.value, passwordConfirmation.value),
)

async function handleSubmit() {
  if (!canSubmit.value) return
  submitting.value = true
  error.value = null
  try {
    await authStore.changePassword({
      currentPassword: currentPassword.value,
      newPassword: newPassword.value,
    })
    notification.success('パスワードを変更しました。新しいパスワードでログインしてください。')
    await router.replace('/login')
  } catch (e) {
    error.value = e as ApiError
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthCard
    id="password-change"
    title="パスワード変更"
    :description="
      authStore.currentUser?.passwordChangeRequired
        ? '仮パスワードを新しいパスワードへ変更してください。変更するまで他の機能は利用できません。'
        : '現在のパスワードを確認して、新しいパスワードへ変更します。'
    "
  >
    <form class="auth-form" @submit.prevent="handleSubmit">
      <ErrorMessage v-if="error" :error="error" />
      <BaseInput
        v-model="currentPassword"
        label="現在のパスワード"
        type="password"
        autocomplete="current-password"
        required
        :error="error?.fieldErrors.currentPassword"
      />
      <BaseInput
        v-model="newPassword"
        label="新しいパスワード"
        type="password"
        autocomplete="new-password"
        :minlength="12"
        required
        :error="error?.fieldErrors.newPassword ?? newPasswordError"
      />
      <BaseInput
        v-model="passwordConfirmation"
        label="新しいパスワード（確認）"
        type="password"
        autocomplete="new-password"
        :minlength="12"
        required
        :error="confirmationError"
      />
      <BaseButton type="submit" :disabled="submitting || !canSubmit">
        {{ submitting ? '変更中…' : 'パスワードを変更する' }}
      </BaseButton>
    </form>
  </AuthCard>
</template>

<style scoped>
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 1em;
}
</style>
