import axios from 'axios'
import { normalizeError } from '@/types/apiError'

export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
})

httpClient.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(normalizeError(error)),
)
