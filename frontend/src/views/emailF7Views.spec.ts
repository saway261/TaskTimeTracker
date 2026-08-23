// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia, type Pinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '@/api/authApi'
import { router } from '@/router'
import { useAuthStore } from '@/stores/authStore'
import type { ApiError } from '@/types/apiError'
import EmailChangeView from './EmailChangeView.vue'
import EmailVerificationPendingView from './EmailVerificationPendingView.vue'
import VerifyEmailChangeView from './VerifyEmailChangeView.vue'
import VerifyEmailView from './VerifyEmailView.vue'

vi.mock('@/api/authApi')

const user = {
  id: 1,
  email: 'user@example.com',
  passwordChangeRequired: false,
  emailVerified: false,
  onboardingCompleted: false,
}

function error(status: number, message: string): ApiError {
  return {
    status,
    kind: 'unknown',
    message,
    fieldErrors: {},
    formErrors: [],
  }
}

describe('FE F7 email verification and change views', () => {
  let pinia: Pinia

  beforeEach(async () => {
    vi.resetAllMocks()
    pinia = createPinia()
    setActivePinia(pinia)
    useAuthStore().initialized = true
    await router.push('/login')
  })

  it('confirms an email token and clears the invalidated session state', async () => {
    useAuthStore().currentUser = user
    vi.mocked(authApi.verifyEmail).mockResolvedValue({} as never)
    await router.push('/verify-email?token=verification-token')

    const wrapper = mount(VerifyEmailView, { global: { plugins: [pinia, router] } })
    await flushPromises()

    expect(authApi.verifyEmail).toHaveBeenCalledWith('verification-token')
    expect(wrapper.text()).toContain('メールアドレスを確認しました。')
    expect(useAuthStore().currentUser).toBeNull()
    wrapper.unmount()
  })

  it('resends the verification email from the pending view', async () => {
    useAuthStore().currentUser = user
    vi.mocked(authApi.resendVerificationEmail).mockResolvedValue({} as never)
    await router.push('/email-verification-pending')

    const wrapper = mount(EmailVerificationPendingView, {
      global: { plugins: [pinia, router] },
    })
    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(authApi.resendVerificationEmail).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('メールアドレスを間違えた場合は')
    wrapper.unmount()
  })

  it('requests an email change and shows the pending address', async () => {
    useAuthStore().currentUser = user
    vi.mocked(authApi.requestEmailChange).mockResolvedValue({
      data: { pendingEmail: 'new@example.com' },
    } as never)
    await router.push('/email-change')

    const wrapper = mount(EmailChangeView, { global: { plugins: [pinia, router] } })
    const inputs = wrapper.findAll('input')
    await inputs[0]?.setValue(' new@example.com ')
    await inputs[1]?.setValue('current-password')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(authApi.requestEmailChange).toHaveBeenCalledWith({
      newEmail: 'new@example.com',
      currentPassword: 'current-password',
    })
    expect(wrapper.text()).toContain('new@example.com')
    expect(wrapper.text()).toContain('確認メールを送信しました。')
    wrapper.unmount()
  })

  it('distinguishes an email-change conflict from an invalid token', async () => {
    vi.mocked(authApi.confirmEmailChange).mockRejectedValue(
      error(409, 'このメールアドレスは使用できません。'),
    )
    await router.push('/verify-email-change?token=email-change-token')

    const wrapper = mount(VerifyEmailChangeView, {
      global: { plugins: [pinia, router] },
    })
    await flushPromises()

    expect(authApi.confirmEmailChange).toHaveBeenCalledWith('email-change-token')
    expect(wrapper.text()).toContain('そのメールアドレスは他の利用者に使用されています。')
    wrapper.unmount()
  })
})
