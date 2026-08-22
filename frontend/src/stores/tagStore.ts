import { defineStore } from 'pinia'
import * as tagsApi from '@/api/tagsApi'
import { useAuthStore } from '@/stores/authStore'
import type { TagResponse } from '@/types/tag'
import type { ApiError } from '@/types/apiError'

interface PendingTagRequest {
  userId: number | null
  promise: Promise<void>
}

let tagRequest: PendingTagRequest | null = null

function currentUserId() {
  return useAuthStore().currentUser?.id ?? null
}

// 付与件数の降順 → タグ名の昇順。名前は (user_id, name_normalized) が一意なため、
// この2キーで順序が確定する。
function sortTags(tags: TagResponse[]) {
  return [...tags].sort(
    (a, b) => b.assignedTaskCount - a.assignedTaskCount || a.name.localeCompare(b.name, 'ja'),
  )
}

function mergeTag(tags: TagResponse[], updated: TagResponse) {
  const found = tags.some((tag) => tag.id === updated.id)
  return sortTags(
    found ? tags.map((tag) => (tag.id === updated.id ? updated : tag)) : [...tags, updated],
  )
}

export const useTagStore = defineStore('tag', {
  state: () => ({
    tags: [] as TagResponse[],
    loading: false,
    error: null as ApiError | null,
    initialized: false,
    loadedForUserId: null as number | null,
  }),
  getters: {
    activeTags: (state) => state.tags.filter((tag) => !tag.isArchived),
  },
  actions: {
    fetchTags(force = false) {
      const userId = currentUserId()
      if (tagRequest?.userId === userId) return tagRequest.promise
      if (this.initialized && this.loadedForUserId === userId && !force) {
        return Promise.resolve()
      }
      if (this.loadedForUserId !== userId) {
        this.tags = []
        this.initialized = false
        this.loadedForUserId = null
      }

      this.loading = true
      this.error = null
      const request = tagsApi
        .fetchAll(true)
        .then(({ data }) => {
          if (currentUserId() !== userId) return
          this.tags = sortTags(data)
          this.initialized = true
          this.loadedForUserId = userId
        })
        .catch((error) => {
          if (currentUserId() === userId) {
            this.error = error as ApiError
          }
          throw error
        })
        .finally(() => {
          if (tagRequest?.promise === request) {
            this.loading = false
            tagRequest = null
          }
        })
      tagRequest = { userId, promise: request }

      return request
    },

    async createTag(name: string) {
      const userId = currentUserId()
      this.error = null
      try {
        const { data } = await tagsApi.create({ name })
        if (currentUserId() === userId) {
          this.tags = mergeTag(this.tags, data)
        }
        return data
      } catch (error) {
        if (currentUserId() === userId) this.error = error as ApiError
        throw error
      }
    },

    async renameTag(id: string, name: string) {
      const userId = currentUserId()
      this.error = null
      try {
        const { data } = await tagsApi.update(id, { name })
        if (currentUserId() === userId) {
          this.tags = mergeTag(this.tags, data)
        }
        return data
      } catch (error) {
        if (currentUserId() === userId) this.error = error as ApiError
        throw error
      }
    },

    async setArchived(id: string, isArchived: boolean) {
      const userId = currentUserId()
      this.error = null
      try {
        const { data } = await tagsApi.updateArchived(id, { isArchived })
        if (currentUserId() === userId) {
          this.tags = mergeTag(this.tags, data)
        }
        return data
      } catch (error) {
        if (currentUserId() === userId) this.error = error as ApiError
        throw error
      }
    },
  },
})
