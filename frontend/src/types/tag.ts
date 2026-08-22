export interface TagResponse {
  id: string
  name: string
  isArchived: boolean
  assignedTaskCount: number
}

export interface TagSummary {
  id: string
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
