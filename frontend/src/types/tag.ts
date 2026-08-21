export interface TagResponse {
  id: number
  name: string
  isArchived: boolean
  assignedTaskCount: number
}

export interface TagSummary {
  id: number
  name: string
}

export interface TagCreateRequest {
  name: string
}

export interface TagUpdateRequest {
  name: string
}

export interface TagUpdateArchivedRequest {
  isArchived: boolean
}
