import { httpClient } from './httpClient'
import type {
  ProjectCreateRequest,
  ProjectResponse,
  ProjectUpdateFinishedRequest,
  ProjectUpdateRequest,
} from '@/types/project'

export function fetchAll(isFinished?: boolean) {
  return httpClient.get<ProjectResponse[]>('/projects', { params: { isFinished } })
}

export function fetchById(id: string) {
  return httpClient.get<ProjectResponse>(`/projects/${id}`)
}

export function create(req: ProjectCreateRequest) {
  return httpClient.post<ProjectResponse>('/projects', req)
}

export function update(id: string, req: ProjectUpdateRequest) {
  return httpClient.put<ProjectResponse>(`/projects/${id}`, req)
}

export function updateFinished(id: string, req: ProjectUpdateFinishedRequest) {
  return httpClient.patch<ProjectResponse>(`/projects/${id}/finished`, req)
}
