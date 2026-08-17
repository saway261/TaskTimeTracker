<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import AuthCard from '@/components/auth/AuthCard.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseInput from '@/components/common/BaseInput.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import { useAuthStore } from '@/stores/authStore'
import type { ApiError } from '@/types/apiError'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const submitting = ref(false)
const error = ref<ApiError | null>(null)

const canSubmit = computed(() => email.value.trim() !== '' && password.value !== '')

function redirectAfterLogin() {
  if (authStore.currentUser?.passwordChangeRequired) return '/password-change'
  if (authStore.currentUser && !authStore.currentUser.emailVerified) {
    return '/email-verification-pending'
  }

  const redirect = route.query.redirect
  if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')) {
    return redirect
  }
  return '/projects'
}

async function handleSubmit() {
  if (!canSubmit.value) return
  submitting.value = true
  error.value = null
  try {
    await authStore.login({ email: email.value.trim(), password: password.value })
    await router.replace(redirectAfterLogin())
  } catch (e) {
    error.value = e as ApiError
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthCard
    id="login"
    title="ログイン"
    description="メールアドレスとパスワードを入力してください。"
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
        autocomplete="current-password"
        required
        :error="error?.fieldErrors.password"
      />
      <BaseButton type="submit" :disabled="submitting || !canSubmit">
        {{ submitting ? 'ログイン中…' : 'ログイン' }}
      </BaseButton>
    </form>
    <template #footer>
      <div class="auth-links">
        <RouterLink to="/password-reset-request">パスワードをお忘れですか</RouterLink>
        <span>
          アカウントをお持ちでない方は
          <RouterLink to="/register">ユーザー登録</RouterLink>
        </span>
      </div>
    </template>
  </AuthCard>
</template>

<style scoped>
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 1em;
}

.auth-links {
  display: flex;
  flex-direction: column;
  gap: 0.7em;
}
</style>
