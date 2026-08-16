<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { resetPassword } from '@/api/authApi'
import AuthCard from '@/components/auth/AuthCard.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseInput from '@/components/common/BaseInput.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import { useAuthStore } from '@/stores/authStore'
import type { ApiError } from '@/types/apiError'
import { validateNewPassword, validatePasswordConfirmation } from '@/utils/authValidation'

const route = useRoute()
const authStore = useAuthStore()

const token = computed(() => (typeof route.query.token === 'string' ? route.query.token : ''))
const newPassword = ref('')
const passwordConfirmation = ref('')
const submitting = ref(false)
const completed = ref(false)
const error = ref<ApiError | null>(null)

let referrerMeta: HTMLMetaElement | null = null
let originalReferrerPolicy: string | null = null
let createdReferrerMeta = false

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
    token.value !== '' &&
    newPassword.value !== '' &&
    passwordConfirmation.value !== '' &&
    !validateNewPassword(newPassword.value) &&
    !validatePasswordConfirmation(newPassword.value, passwordConfirmation.value),
)

onMounted(() => {
  referrerMeta = document.head.querySelector<HTMLMetaElement>('meta[name="referrer"]')
  if (referrerMeta) {
    originalReferrerPolicy = referrerMeta.getAttribute('content')
  } else {
    referrerMeta = document.createElement('meta')
    referrerMeta.setAttribute('name', 'referrer')
    document.head.append(referrerMeta)
    createdReferrerMeta = true
  }
  referrerMeta.setAttribute('content', 'no-referrer')
})

onBeforeUnmount(() => {
  if (!referrerMeta) return
  if (createdReferrerMeta) {
    referrerMeta.remove()
  } else if (originalReferrerPolicy === null) {
    referrerMeta.removeAttribute('content')
  } else {
    referrerMeta.setAttribute('content', originalReferrerPolicy)
  }
})

async function handleSubmit() {
  if (!canSubmit.value) return
  submitting.value = true
  error.value = null

  try {
    await resetPassword({ token: token.value, newPassword: newPassword.value })
    authStore.clear()
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
    id="password-reset"
    title="新しいパスワードの設定"
    description="新しいパスワードを入力してください。"
  >
    <div v-if="!token" class="invalid-link" role="alert">
      <p>パスワード再設定リンクが無効です。</p>
      <RouterLink to="/password-reset-request">再設定メールをもう一度送信する</RouterLink>
    </div>
    <div v-else-if="completed" class="completion-message" role="status">
      <p>パスワードを再設定しました。</p>
      <p>新しいパスワードでログインしてください。</p>
    </div>
    <form v-else class="auth-form" @submit.prevent="handleSubmit">
      <ErrorMessage v-if="error" :error="error" />
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
        {{ submitting ? '設定中…' : '新しいパスワードを設定する' }}
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

.invalid-link,
.completion-message {
  padding: 1em;
  border-radius: 6px;
  background: var(--color-surface-muted);
}

.invalid-link p,
.completion-message p {
  margin: 0;
}

.invalid-link a {
  display: inline-block;
  margin-top: 0.6em;
  color: var(--color-accent);
}

.completion-message p + p {
  margin-top: 0.6em;
}
</style>
