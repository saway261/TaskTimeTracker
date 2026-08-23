// @vitest-environment jsdom

import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { useTutorialStore } from './tutorialStore'

describe('tutorialStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('is inactive by default', () => {
    const store = useTutorialStore()

    expect(store.isActive).toBe(false)
    expect(store.activeChapterId).toBeNull()
    expect(store.mode).toBeNull()
    expect(store.tourAttempted).toBe(false)
  })

  it('activates the chosen chapter and resets the step index on start', () => {
    const store = useTutorialStore()
    store.stepIndex = 3

    store.start('tasks', 'replay')

    expect(store.isActive).toBe(true)
    expect(store.activeChapterId).toBe('tasks')
    expect(store.mode).toBe('replay')
    expect(store.stepIndex).toBe(0)
  })

  it('marks tourAttempted when starting in tour mode', () => {
    const store = useTutorialStore()

    store.start('intro', 'tour')

    expect(store.tourAttempted).toBe(true)
  })

  it('does not mark tourAttempted when starting in replay mode', () => {
    const store = useTutorialStore()

    store.start('tags', 'replay')

    expect(store.tourAttempted).toBe(false)
  })

  it('moves the step index to an arbitrary target (not necessarily adjacent)', () => {
    const store = useTutorialStore()
    store.start('tasks', 'replay')

    store.goTo(2)
    expect(store.stepIndex).toBe(2)

    // onMissing:'skip' のステップを飛ばした先へ直接移動するケース(TutorialHost.vue参照)。
    store.goTo(5)
    expect(store.stepIndex).toBe(5)

    store.goTo(1)
    expect(store.stepIndex).toBe(1)
  })

  it('closes the tutorial and resets chapter/step/mode on end', () => {
    const store = useTutorialStore()
    store.start('analytics', 'tour')
    store.goTo(1)

    store.end()

    expect(store.isActive).toBe(false)
    expect(store.activeChapterId).toBeNull()
    expect(store.stepIndex).toBe(0)
    expect(store.mode).toBeNull()
  })

  it('keeps tourAttempted after end so the tour does not refire this session', () => {
    const store = useTutorialStore()
    store.start('intro', 'tour')

    store.end()

    expect(store.tourAttempted).toBe(true)
  })
})
