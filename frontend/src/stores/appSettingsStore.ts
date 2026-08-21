import { defineStore } from 'pinia'
import * as appSettingsApi from '@/api/appSettingsApi'
import { DEFAULT_ON_TIME_THRESHOLD_PERCENT } from '@/utils/duration'

let settingsRequest: Promise<void> | null = null

export const useAppSettingsStore = defineStore('appSettings', {
  state: () => ({
    onTimeThresholdPercent: DEFAULT_ON_TIME_THRESHOLD_PERCENT,
    loaded: false,
  }),
  actions: {
    load() {
      if (this.loaded) return Promise.resolve()
      if (settingsRequest) return settingsRequest

      settingsRequest = appSettingsApi
        .fetchSettings()
        .then(({ data }) => {
          this.onTimeThresholdPercent = data.onTimeThresholdPercent
        })
        .catch(() => {
          // 設定取得失敗で画面を操作不能にせず、バックエンドの既定値と同じ10%で継続する。
          this.onTimeThresholdPercent = DEFAULT_ON_TIME_THRESHOLD_PERCENT
        })
        .finally(() => {
          this.loaded = true
          settingsRequest = null
        })

      return settingsRequest
    },
  },
})
