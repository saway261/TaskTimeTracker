export interface FieldError {
  field: string
  message: string
}

export interface ErrorResponse {
  status: string
  message: string
  errors: FieldError[]
}
