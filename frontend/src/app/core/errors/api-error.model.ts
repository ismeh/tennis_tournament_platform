export interface ApiErrorResponse {
  code: string;
  message: string;
  status: number;
  timestamp?: string;
  path?: string;
  details?: unknown;
}

export class ApiRequestError extends Error {
  constructor(
    readonly code: string,
    message: string,
    readonly status: number,
    readonly details?: unknown,
    readonly path?: string,
    readonly originalError?: unknown
  ) {
    super(message);
    this.name = 'ApiRequestError';
  }
}
