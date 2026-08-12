import { defineStore } from 'pinia'

export type NotificationKind = 'error' | 'success' | 'info'

export interface Notification {
  id: number
  kind: NotificationKind
  message: string
}

let nextId = 1

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    notifications: [] as Notification[],
  }),
  actions: {
    push(kind: NotificationKind, message: string) {
      const id = nextId++
      this.notifications.push({ id, kind, message })
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
    },
  },
})
