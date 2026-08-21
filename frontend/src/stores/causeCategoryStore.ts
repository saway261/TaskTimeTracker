import { defineStore } from 'pinia'
import * as reflectionsApi from '@/api/reflectionsApi'
import type { ReflectionCauseCategoryResponse } from '@/types/reflection'
import type { ApiError } from '@/types/apiError'

let categoryRequest: Promise<void> | null = null

export const useCauseCategoryStore = defineStore('causeCategory', {
  state: () => ({
    categories: [] as ReflectionCauseCategoryResponse[],
    loading: false,
    initialized: false,
    error: null as ApiError | null,
  }),
  actions: {
    fetchCategories() {
      if (this.initialized) return Promise.resolve()
      if (categoryRequest) return categoryRequest

      this.loading = true
      this.error = null
      categoryRequest = reflectionsApi
        .fetchCauseCategories()
        .then(({ data }) => {
          this.categories = data
        })
        .catch((e) => {
          this.error = e as ApiError
          throw e
        })
        .finally(() => {
          this.loading = false
          this.initialized = true
          categoryRequest = null
        })

      return categoryRequest
    },
  },
})
