import { httpClient } from './httpClient'
import type {
  ProjectReflectionOverviewResponse,
  ReflectionCauseCategoryResponse,
  ReflectionRequest,
  ReflectionResponse,
} from '@/types/reflection'

export function fetchCauseCategories() {
  return httpClient.get<ReflectionCauseCategoryResponse[]>('/reflection-cause-categories')
}

export function fetchOverview(projectId: number) {
  return httpClient.get<ProjectReflectionOverviewResponse>(`/projects/${projectId}/reflections`)
}

export function create(taskId: number, req: ReflectionRequest) {
  return httpClient.post<ReflectionResponse>(`/tasks/${taskId}/reflection`, req)
}

export function update(taskId: number, req: ReflectionRequest) {
  return httpClient.put<ReflectionResponse>(`/tasks/${taskId}/reflection`, req)
}
