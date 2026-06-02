import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorResponse, ApiRequestError } from './api-error.model';

export function normalizeApiError(error: unknown): ApiRequestError {
  if (error instanceof ApiRequestError) {
    return error;
  }

  if (error instanceof HttpErrorResponse) {
    const body = error.error;
    if (isApiErrorResponse(body)) {
      return buildApiRequestError(body, error);
    }

    if (typeof body === 'string' && body.trim()) {
      const parsedBody = parseApiErrorResponse(body);
      if (parsedBody) {
        return buildApiRequestError(parsedBody, error);
      }

      return new ApiRequestError(
        fallbackCode(error.status),
        fallbackMessage(error.status),
        error.status,
        undefined,
        error.url ?? undefined,
        error
      );
    }

    return new ApiRequestError(
      fallbackCode(error.status),
      fallbackMessage(error.status),
      error.status,
      undefined,
      error.url ?? undefined,
      error
    );
  }

  if (error instanceof Error) {
    const parsedMessage = parseApiErrorResponse(error.message);
    if (parsedMessage) {
      return new ApiRequestError(
        parsedMessage.code,
        parsedMessage.message,
        parsedMessage.status,
        parsedMessage.details,
        parsedMessage.path,
        error
      );
    }

    return new ApiRequestError('CLIENT_ERROR', error.message, 0, undefined, undefined, error);
  }

  return new ApiRequestError('UNKNOWN_ERROR', 'No se pudo completar la operación.', 0, undefined, undefined, error);
}

export function getApiErrorMessage(error: unknown, fallback = 'No se pudo completar la operación.'): string {
  const normalized = normalizeApiError(error);
  if (normalized.code === 'CLIENT_ERROR' || normalized.code === 'UNKNOWN_ERROR') {
    return fallback;
  }
  return normalized.message?.trim() || fallback;
}

function buildApiRequestError(body: ApiErrorResponse, error: HttpErrorResponse): ApiRequestError {
  return new ApiRequestError(
    body.code,
    body.message,
    body.status ?? error.status,
    body.details,
    body.path,
    error
  );
}

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const candidate = value as Partial<ApiErrorResponse>;
  return typeof candidate.code === 'string'
    && typeof candidate.message === 'string'
    && typeof candidate.status === 'number';
}

function parseApiErrorResponse(value: string): ApiErrorResponse | null {
  try {
    const parsed = JSON.parse(value);
    return isApiErrorResponse(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

function fallbackCode(status: number): string {
  return switchByStatus(status, {
    0: 'NETWORK_ERROR',
    400: 'INVALID_REQUEST',
    401: 'AUTHENTICATION_FAILED',
    403: 'ACCESS_DENIED',
    404: 'RESOURCE_NOT_FOUND',
    409: 'DATA_CONFLICT',
    500: 'INTERNAL_SERVER_ERROR'
  }, 'UNKNOWN_ERROR');
}

function fallbackMessage(status: number): string {
  return switchByStatus(status, {
    0: 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.',
    400: 'Revisa los datos enviados antes de continuar.',
    401: 'Inicia sesión para continuar.',
    403: 'No tienes permisos para realizar esta acción.',
    404: 'No se encontró la información solicitada.',
    409: 'La información entra en conflicto con datos existentes.',
    500: 'No se pudo completar la operación. Inténtalo de nuevo más tarde.'
  }, 'No se pudo completar la operación.');
}

function switchByStatus<T>(status: number, values: Record<number, T>, fallback: T): T {
  return values[status] ?? fallback;
}
