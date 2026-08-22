import { httpClient } from './httpClient'
import type {
  TagCreateRequest,
  TagResponse,
  TagUpdateArchivedRequest,
  TagUpdateRequest,
} from '@/types/tag'

export function fetchAll(includeArchived = false) {
  return httpClient.get<TagResponse[]>('/tags', { params: { includeArchived } })
}

export function create(req: TagCreateRequest) {
  return httpClient.post<TagResponse>('/tags', req)
}

export function update(id: string, req: TagUpdateRequest) {
  return httpClient.put<TagResponse>(`/tags/${id}`, req)
}

export function updateArchived(id: string, req: TagUpdateArchivedRequest) {
  return httpClient.patch<TagResponse>(`/tags/${id}/archived`, req)
}
