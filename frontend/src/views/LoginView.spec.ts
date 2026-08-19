// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia, type Pinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '@/api/authApi'
import { router } from '@/router'
import { useAuthStore } from '@/stores/authStore'
import LoginView from './LoginView.vue'

vi.mock('@/api/authApi')

const user = {
  id: 1,
  email: 'user@example.com',
  passwordChangeRequired: false,
  emailVerified: true,
}

describe('LoginView', () => {
  let pinia: Pinia

  beforeEach(async () => {
    vi.resetAllMocks()
    pinia = createPinia()
    setActivePinia(pinia)
    useAuthStore().initialized = true
    vi.mocked(authApi.login).mockResolvedValue({ data: user } as never)
    await router.push('/login')
  })

  it('通常ログイン後は未完了プロジェクト一覧を開く', async () => {
    const wrapper = mount(LoginView, {
      global: { plugins: [pinia, router] },
    })

    await wrapper.get('input[type="email"]').setValue(user.email)
    await wrapper.get('input[type="password"]').setValue('password')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(authApi.login).toHaveBeenCalledWith({ email: user.email, password: 'password' })
    await expect.poll(() => router.currentRoute.value.path).toBe('/projects')
    expect(router.currentRoute.value.path).toBe('/projects')
    expect(router.currentRoute.value.query.isFinished).toBe('false')
    wrapper.unmount()
  })
})
