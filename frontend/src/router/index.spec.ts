// @vitest-environment jsdom

import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '@/api/authApi'
import * as appSettingsApi from '@/api/appSettingsApi'
import { useAuthStore } from '@/stores/authStore'
import { useAppSettingsStore } from '@/stores/appSettingsStore'
import { useTutorialStore } from '@/stores/tutorialStore'
import { router } from './index'

vi.mock('@/api/appSettingsApi')

const user = {
  id: 1,
  email: 'user@example.com',
  passwordChangeRequired: false,
  emailVerified: true,
  onboardingCompleted: false,
}

describe('authentication navigation guard', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    vi.resetAllMocks()
    setActivePinia(createPinia())
    useAuthStore().initialized = true
    vi.mocked(appSettingsApi.fetchSettings).mockResolvedValue({
      data: { onTimeThresholdPercent: 10 },
    } as never)
  })

  it('redirects unauthenticated users from protected routes to login', async () => {
    await router.push('/projects/10')

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/projects/10')
  })

  it('redirects authenticated users away from guest-only routes', async () => {
    useAuthStore().currentUser = user

    await router.push('/register')

    expect(router.currentRoute.value.name).toBe('project-list')
  })

  it('opens the project list from the application root', async () => {
    useAuthStore().currentUser = user

    await router.push('/')

    expect(router.currentRoute.value.name).toBe('project-list')
  })

  it('allows authenticated users to open the analytics route', async () => {
    useAuthStore().currentUser = user

    await router.push('/analytics')

    expect(router.currentRoute.value.name).toBe('analytics')
  })

  it('認証済みユーザーはタグ管理画面を開ける', async () => {
    useAuthStore().currentUser = user

    await router.push('/tags')

    expect(router.currentRoute.value.name).toBe('tag-management')
  })

  it('forces users who must change their password to the password change route', async () => {
    useAuthStore().currentUser = {
      ...user,
      passwordChangeRequired: true,
      emailVerified: false,
    }

    await router.push('/projects/10')

    expect(router.currentRoute.value.name).toBe('password-change')
  })

  it('forces unverified users to the email verification pending route', async () => {
    useAuthStore().currentUser = { ...user, emailVerified: false }

    await router.push('/projects/10')

    expect(router.currentRoute.value.name).toBe('email-verification-pending')
  })

  it('allows unverified users to open the email change route', async () => {
    useAuthStore().currentUser = { ...user, emailVerified: false }

    await router.push('/email-change')

    expect(router.currentRoute.value.name).toBe('email-change')
  })

  it('allows password-change-required users to open the email change route', async () => {
    useAuthStore().currentUser = {
      ...user,
      passwordChangeRequired: true,
      emailVerified: false,
    }

    await router.push('/email-change')

    expect(router.currentRoute.value.name).toBe('email-change')
  })

  it('allows unauthenticated users to open email confirmation routes', async () => {
    await router.push('/verify-email?token=verification-token')
    expect(router.currentRoute.value.name).toBe('verify-email')

    await router.push('/verify-email-change?token=email-change-token')
    expect(router.currentRoute.value.name).toBe('verify-email-change')
  })

  it('allows unauthenticated users to open password reset routes', async () => {
    await router.push('/password-reset-request')
    expect(router.currentRoute.value.name).toBe('password-reset-request')

    await router.push('/password-reset?token=password-reset-token')
    expect(router.currentRoute.value.name).toBe('password-reset')
  })

  it('keeps the password reset route after initial session detection returns 401', async () => {
    const authStore = useAuthStore()
    await router.push('/login')
    authStore.initialized = false
    vi.spyOn(authApi, 'fetchMe').mockRejectedValue({ status: 401 })

    await router.push('/password-reset?token=direct-access-token')

    expect(router.currentRoute.value.name).toBe('password-reset')
    expect(router.currentRoute.value.query.token).toBe('direct-access-token')
  })

  it('allows authenticated users to open the password change route', async () => {
    useAuthStore().currentUser = user

    await router.push('/password-change')

    expect(router.currentRoute.value.name).toBe('password-change')
  })

  it('認証済みなら画面遷移前にアプリ設定を取得する', async () => {
    useAuthStore().currentUser = user
    vi.mocked(appSettingsApi.fetchSettings).mockResolvedValue({
      data: { onTimeThresholdPercent: 20 },
    } as never)

    await router.push('/projects?settings=success')

    expect(appSettingsApi.fetchSettings).toHaveBeenCalledTimes(1)
    expect(useAppSettingsStore().onTimeThresholdPercent).toBe(20)
    expect(useAppSettingsStore().loaded).toBe(true)
  })

  it('設定取得に失敗しても既定値で画面遷移を継続する', async () => {
    useAuthStore().currentUser = user
    vi.mocked(appSettingsApi.fetchSettings).mockRejectedValue(new Error('network error'))

    await router.push('/projects?settings=fallback')

    expect(router.currentRoute.value.name).toBe('project-list')
    expect(useAppSettingsStore().onTimeThresholdPercent).toBe(10)
    expect(useAppSettingsStore().loaded).toBe(true)
  })
})

describe('initial tour firing (要件 §6.1〜§6.2)', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    vi.resetAllMocks()
    setActivePinia(createPinia())
    useAuthStore().initialized = true
    vi.mocked(appSettingsApi.fetchSettings).mockResolvedValue({
      data: { onTimeThresholdPercent: 10 },
    } as never)
  })

  it('starts the intro tour, unscoped, when an onboardingCompleted:false user reaches /projects', async () => {
    useAuthStore().currentUser = user
    await router.push('/tags')

    await router.push('/projects')

    const tutorialStore = useTutorialStore()
    expect(tutorialStore.activeChapterId).toBe('intro')
    expect(tutorialStore.mode).toBe('tour')
    // スコープ指定なしで開始する。「はじめに」章のwelcome/loop/f-reflectは
    // アンカーを持たないため、スコープ付きだと再生対象から外れてしまう。
    expect(tutorialStore.scopeSelector).toBeNull()
  })

  it('does not fire for users who must change their password first', async () => {
    useAuthStore().currentUser = { ...user, passwordChangeRequired: true, emailVerified: false }
    await router.push('/tags').catch(() => {})

    await router.push('/projects')

    expect(router.currentRoute.value.name).toBe('password-change')
    expect(useTutorialStore().activeChapterId).toBeNull()
  })

  it('does not fire for users who have not verified their email', async () => {
    useAuthStore().currentUser = { ...user, emailVerified: false }
    await router.push('/tags').catch(() => {})

    await router.push('/projects')

    expect(router.currentRoute.value.name).toBe('email-verification-pending')
    expect(useTutorialStore().activeChapterId).toBeNull()
  })

  it('does not fire for users who already completed onboarding', async () => {
    useAuthStore().currentUser = { ...user, onboardingCompleted: true }
    await router.push('/tags')

    await router.push('/projects')

    expect(useTutorialStore().activeChapterId).toBeNull()
  })

  it('does not fire for unauthenticated visitors (redirected to login before reaching /projects)', async () => {
    // 直前のテストが /projects に留まっている場合、同一ルートへのpushは
    // ナビゲーションガードを再実行しない(vue-routerの重複ナビゲーション扱い)ため、
    // 先に無関係のルートへ移動してから改めて /projects へ push する。
    await router.push('/password-reset-request')

    await router.push('/projects')

    expect(router.currentRoute.value.name).toBe('login')
    expect(useTutorialStore().activeChapterId).toBeNull()
  })

  it('does not re-fire on a second visit to /projects within the same session', async () => {
    useAuthStore().currentUser = user
    await router.push('/tags')
    await router.push('/projects')

    const tutorialStore = useTutorialStore()
    expect(tutorialStore.activeChapterId).toBe('intro')
    // ユーザーがツアーを閉じた状態を模す。
    tutorialStore.end()

    await router.push('/analytics')
    await router.push('/projects')

    expect(tutorialStore.activeChapterId).toBeNull()
    expect(tutorialStore.tourAttempted).toBe(true)
  })
})
