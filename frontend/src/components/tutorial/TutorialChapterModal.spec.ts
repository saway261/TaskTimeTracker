// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useTutorialStore } from '@/stores/tutorialStore'
import type { TutorialChapter } from '@/tutorial/types'
import TutorialChapterModal from './TutorialChapterModal.vue'

vi.mock('@/tutorial/chapters', () => {
  const chapter = (id: string, title: string, entryRoute: string): TutorialChapter => ({
    id: id as TutorialChapter['id'],
    title,
    summary: `${title}の説明`,
    entryRoute,
    replayable: id !== 'intro',
    steps: [],
  })
  return {
    chapters: [
      chapter('intro', 'はじめに', '/projects'),
      chapter('tags', 'タグ管理', '/tags'),
      chapter('analytics', '分析', '/analytics'),
      chapter('tasks', 'タスク管理', '/projects'),
      chapter('reflections', '振り返り', '/reflections'),
    ],
  }
})

async function mountModal() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/projects', component: { template: '<div />' } },
      { path: '/reflections', component: { template: '<div />' } },
      { path: '/analytics', component: { template: '<div />' } },
      { path: '/tags', component: { template: '<div />' } },
    ],
  })
  await router.push('/projects')
  await router.isReady()
  return {
    router,
    wrapper: mount(TutorialChapterModal, {
      props: { modelValue: true },
      global: { plugins: [router], stubs: { Teleport: true } },
    }),
  }
}

describe('TutorialChapterModal', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('lists only replayable chapters, in the fixed loop order', async () => {
    const { wrapper } = await mountModal()

    const titles = wrapper.findAll('.chapter-item-title').map((el) => el.text())

    expect(titles).toEqual(['タスク管理', '振り返り', '分析', 'タグ管理'])
  })

  it('does not list the intro chapter', async () => {
    const { wrapper } = await mountModal()

    expect(wrapper.text()).not.toContain('はじめに')
  })

  it('navigates to the entry route and starts a replay when a chapter is selected', async () => {
    const { wrapper, router } = await mountModal()
    await router.push('/tags')
    const store = useTutorialStore()

    const analyticsItem = wrapper
      .findAll('.chapter-item')
      .find((item) => item.text().includes('分析'))
    await analyticsItem?.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/analytics')
    expect(store.activeChapterId).toBe('analytics')
    expect(store.mode).toBe('replay')
  })

  it('closes itself when a chapter is selected', async () => {
    const { wrapper } = await mountModal()

    const taskItem = wrapper
      .findAll('.chapter-item')
      .find((item) => item.text().includes('タスク管理'))
    await taskItem?.trigger('click')

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([false])
  })
})
