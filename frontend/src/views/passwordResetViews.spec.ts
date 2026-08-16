// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia, type Pinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '@/api/authApi'
import { router } from '@/router'
import { useAuthStore } from '@/stores/authStore'
import LoginView from './LoginView.vue'
import PasswordResetRequestView from './PasswordResetRequestView.vue'
import PasswordResetView from './PasswordResetView.vue'

vi.mock('@/api/authApi')

describe('FE F8 password reset views', () => {
  let pinia: Pinia

  beforeEach(async () => {
    vi.resetAllMocks()
    pinia = createPinia()
    setActivePinia(pinia)
    useAuthStore().initialized = true
    await router.push('/login')
  })

  it('requests a reset and always shows the fixed completion message', async () => {
    vi.mocked(authApi.requestPasswordReset).mockResolvedValue({
      data: { message: 'account exists' },
    } as never)
    await router.push('/password-reset-request')

    const wrapper = mount(PasswordResetRequestView, {
      global: { plugins: [pinia, router] },
    })
    await wrapper.get('input').setValue(' user@example.com ')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(authApi.requestPasswordReset).toHaveBeenCalledWith({ email: 'user@example.com' })
    expect(wrapper.text()).toContain(
      '入力したメールアドレスが登録されている場合、パスワード再設定メールを送信しました。',
    )
    expect(wrapper.text()).not.toContain('account exists')
    wrapper.unmount()
  })

  it('submits the URL token and new password, then clears the session state', async () => {
    useAuthStore().currentUser = {
      id: 1,
      email: 'user@example.com',
      passwordChangeRequired: false,
      emailVerified: true,
    }
    vi.mocked(authApi.resetPassword).mockResolvedValue({} as never)
    await router.push('/password-reset?token=password-reset-token')

    const wrapper = mount(PasswordResetView, { global: { plugins: [pinia, router] } })
    const inputs = wrapper.findAll('input')
    await inputs[0]?.setValue('new-password1234')
    await inputs[1]?.setValue('new-password1234')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(authApi.resetPassword).toHaveBeenCalledWith({
      token: 'password-reset-token',
      newPassword: 'new-password1234',
    })
    expect(useAuthStore().currentUser).toBeNull()
    expect(wrapper.text()).toContain('パスワードを再設定しました。')
    wrapper.unmount()
  })

  it('sets no-referrer while the reset token view is mounted', async () => {
    await router.push('/password-reset?token=password-reset-token')

    const wrapper = mount(PasswordResetView, { global: { plugins: [pinia, router] } })

    expect(document.head.querySelector('meta[name="referrer"]')?.getAttribute('content')).toBe(
      'no-referrer',
    )

    wrapper.unmount()
    expect(document.head.querySelector('meta[name="referrer"]')).toBeNull()
  })

  it('does not submit when the reset token is missing', async () => {
    await router.push('/password-reset')

    const wrapper = mount(PasswordResetView, { global: { plugins: [pinia, router] } })

    expect(wrapper.text()).toContain('パスワード再設定リンクが無効です。')
    expect(wrapper.find('form').exists()).toBe(false)
    expect(authApi.resetPassword).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('provides the forgot-password link from the login view', async () => {
    const wrapper = mount(LoginView, { global: { plugins: [pinia, router] } })

    expect(wrapper.get('a[href="/password-reset-request"]').text()).toBe('パスワードをお忘れですか')
    wrapper.unmount()
  })
})
