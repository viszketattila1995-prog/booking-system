export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

export interface ValidationErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  fieldErrors: Record<string, string>;
  path: string;
}
