// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { useTutorialStore } from '@/stores/tutorialStore'
import TutorialHelpButton from './TutorialHelpButton.vue'

describe('TutorialHelpButton', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('labels itself with the given chapter title', () => {
    const wrapper = mount(TutorialHelpButton, {
      props: { chapterId: 'tasks', chapterTitle: 'タスク管理' },
    })

    expect(wrapper.get('.help-button').attributes('aria-label')).toBe(
      'タスク管理のチュートリアルを見る',
    )
  })

  it('starts a replay of the given chapter without navigating on click', async () => {
    const store = useTutorialStore()
    const wrapper = mount(TutorialHelpButton, {
      props: { chapterId: 'analytics', chapterTitle: '分析' },
    })

    await wrapper.get('.help-button').trigger('click')

    expect(store.activeChapterId).toBe('analytics')
    expect(store.mode).toBe('replay')
  })
})
