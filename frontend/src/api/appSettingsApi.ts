import { httpClient } from './httpClient'
import type { AppSettingsResponse } from '@/types/settings'

export function fetchSettings() {
  return httpClient.get<AppSettingsResponse>('/app-settings')
}
