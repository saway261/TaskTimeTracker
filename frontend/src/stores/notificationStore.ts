import { defineStore } from 'pinia'

export type NotificationKind = 'error' | 'success' | 'info'

export interface Notification {
  id: number
  kind: NotificationKind
  message: string
}

const AUTO_DISMISS_MS = 5000

let nextId = 1
// setTimeoutのハンドルはPiniaの状態（永続化・DevTools追跡対象）に入れる必要が無いため、store外で保持する。
const dismissTimers = new Map<number, ReturnType<typeof setTimeout>>()

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    notifications: [] as Notification[],
  }),
  actions: {
    push(kind: NotificationKind, message: string) {
      const id = nextId++
      this.notifications.push({ id, kind, message })
      dismissTimers.set(
        id,
        setTimeout(() => this.dismiss(id), AUTO_DISMISS_MS),
      )
      return id
    },
    error(message: string) {
      return this.push('error', message)
    },
    success(message: string) {
      return this.push('success', message)
    },
    info(message: string) {
      return this.push('info', message)
    },
    dismiss(id: number) {
      this.notifications = this.notifications.filter((n) => n.id !== id)
      const timer = dismissTimers.get(id)
      if (timer !== undefined) {
        clearTimeout(timer)
        dismissTimers.delete(id)
      }
    },
  },
})
