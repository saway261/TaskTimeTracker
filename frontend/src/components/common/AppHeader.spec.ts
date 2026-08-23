// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia, type Pinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '@/api/authApi'
import * as workSessionsApi from '@/api/workSessionsApi'
import { router } from '@/router'
import { useAppSettingsStore } from '@/stores/appSettingsStore'
import { useAuthStore } from '@/stores/authStore'
import AppHeader from './AppHeader.vue'

vi.mock('@/api/authApi')
vi.mock('@/api/workSessionsApi')

const user = {
  id: 1,
  email: 'long-user-address@example.com',
  passwordChangeRequired: false,
  emailVerified: true,
  onboardingCompleted: false,
}

describe('AppHeader user menu', () => {
  let pinia: Pinia

  beforeEach(async () => {
    vi.resetAllMocks()
    pinia = createPinia()
    setActivePinia(pinia)
    const authStore = useAuthStore()
    authStore.currentUser = user
    authStore.initialized = true
    useAppSettingsStore().loaded = true
    vi.mocked(workSessionsApi.fetchActiveTimers).mockResolvedValue({ data: [] } as never)
    await router.push('/projects')
  })

  it('shows all account actions only after the user icon is pressed', async () => {
    const wrapper = mount(AppHeader, {
      attachTo: document.body,
      global: { plugins: [pinia, router] },
    })
    const trigger = wrapper.get('button[aria-haspopup="menu"]')

    expect(trigger.find('svg').exists()).toBe(true)
    expect(trigger.attributes('aria-expanded')).toBe('false')
    expect(wrapper.find('[role="menu"]').exists()).toBe(false)

    await trigger.trigger('click')

    expect(trigger.attributes('aria-expanded')).toBe('true')
    expect(wrapper.get('[role="menu"]').text()).toContain(user.email)
    expect(wrapper.get('a[href="/tags"]').text()).toBe('タグ管理')
    expect(wrapper.get('a[href="/email-change"]').text()).toBe('メールアドレス変更')
    expect(wrapper.get('a[href="/password-change"]').text()).toBe('パスワード変更')
    expect(wrapper.get('.logout-button').text()).toBe('ログアウト')
    wrapper.unmount()
  })

  it('タスク管理リンクはタスク管理トップを開く', () => {
    const wrapper = mount(AppHeader, {
      global: { plugins: [pinia, router] },
    })

    expect(wrapper.get('.main-nav a').attributes('href')).toBe('/projects')
    wrapper.unmount()
  })

  it('ブランドリンクにロゴとタイトル画像を表示する', () => {
    const wrapper = mount(AppHeader, {
      global: { plugins: [pinia, router] },
    })

    const brand = wrapper.get('.brand')
    expect(brand.attributes('href')).toBe('/projects')
    expect(brand.attributes('aria-label')).toBe('Task Time Tracker ホーム')
    expect(brand.get('.brand-logo').attributes('src')).toBe('/ttt-logo.svg')
    expect(brand.get('.brand-title').attributes('src')).toBe('/title.svg')
    expect(brand.findAll('img').every((image) => image.attributes('alt') === '')).toBe(true)
    wrapper.unmount()
  })

  it('ハンバーガーメニューからタスク管理・振り返り・分析へ移動できる', async () => {
    const wrapper = mount(AppHeader, {
      global: { plugins: [pinia, router] },
    })
    const trigger = wrapper.get('.mobile-nav-trigger')

    expect(trigger.attributes('aria-expanded')).toBe('false')
    expect(wrapper.find('.mobile-nav-panel').exists()).toBe(false)

    await trigger.trigger('click')

    const links = wrapper.findAll('.mobile-nav-item')
    expect(trigger.attributes('aria-expanded')).toBe('true')
    expect(links).toHaveLength(3)
    expect(links[0].text()).toBe('タスク管理')
    expect(links[0].attributes('href')).toBe('/projects')
    expect(links[1].text()).toBe('振り返り')
    expect(links[1].attributes('href')).toBe('/reflections')
    expect(links[2].text()).toBe('分析')
    expect(links[2].attributes('href')).toBe('/analytics')

    await links[1].trigger('click')
    expect(wrapper.find('.mobile-nav-panel').exists()).toBe(false)
    wrapper.unmount()
  })

  it('分析画面ではPCナビゲーションの分析リンクを現在地として表示する', async () => {
    await router.push('/analytics')
    const wrapper = mount(AppHeader, {
      global: { plugins: [pinia, router] },
    })

    const link = wrapper.get('.main-nav a[href="/analytics"]')
    expect(link.classes()).toContain('active')
    expect(link.attributes('aria-current')).toBe('page')
    wrapper.unmount()
  })

  it('closes with Escape and restores focus to the user icon', async () => {
    const wrapper = mount(AppHeader, {
      attachTo: document.body,
      global: { plugins: [pinia, router] },
    })
    const trigger = wrapper.get<HTMLButtonElement>('button[aria-haspopup="menu"]')
    await trigger.trigger('click')

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await flushPromises()

    expect(wrapper.find('[role="menu"]').exists()).toBe(false)
    expect(document.activeElement).toBe(trigger.element)
    wrapper.unmount()
  })

  it('closes when a pointer interaction occurs outside the menu', async () => {
    const wrapper = mount(AppHeader, {
      attachTo: document.body,
      global: { plugins: [pinia, router] },
    })
    await wrapper.get('button[aria-haspopup="menu"]').trigger('click')

    document.body.dispatchEvent(new Event('pointerdown', { bubbles: true }))
    await flushPromises()

    expect(wrapper.find('[role="menu"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('チュートリアル項目はタグ管理の上にあり、選ぶと章選択モーダルを開く', async () => {
    const wrapper = mount(AppHeader, {
      attachTo: document.body,
      global: { plugins: [pinia, router] },
    })
    await wrapper.get('button[aria-haspopup="menu"]').trigger('click')

    const items = wrapper.findAll('.user-menu-item')
    const tutorialIndex = items.findIndex((item) => item.text() === 'チュートリアル')
    const tagIndex = items.findIndex((item) => item.text() === 'タグ管理')
    expect(tutorialIndex).toBeGreaterThanOrEqual(0)
    expect(tutorialIndex).toBeLessThan(tagIndex)

    await items[tutorialIndex].trigger('click')

    expect(wrapper.find('[role="menu"]').exists()).toBe(false)
    // defineAsyncComponentの解決はマイクロタスクのflushPromises()だけでは追いつかないため、
    // 実タイマーでポーリングするvi.waitForを使う。
    await vi.waitFor(() => {
      expect(
        document.body.querySelector('[role="dialog"][aria-label="チュートリアル"]'),
      ).not.toBeNull()
    })
    wrapper.unmount()
  })

  it('logs out from the menu and navigates to login', async () => {
    vi.mocked(authApi.logout).mockResolvedValue({} as never)
    const wrapper = mount(AppHeader, {
      attachTo: document.body,
      global: { plugins: [pinia, router] },
    })
    await wrapper.get('button[aria-haspopup="menu"]').trigger('click')

    await wrapper.get('.logout-button').trigger('click')
    await flushPromises()

    expect(authApi.logout).toHaveBeenCalledOnce()
    expect(useAuthStore().currentUser).toBeNull()
    await expect.poll(() => router.currentRoute.value.name).toBe('login')
    wrapper.unmount()
  })
})
