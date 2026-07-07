import { HttpErrorResponse } from '@angular/common/http';
import { normalizeApiError, getApiErrorMessage } from './api-error.util';
import { ApiRequestError } from './api-error.model';

describe('normalizeApiError', () => {
  it('returns the same ApiRequestError if already an ApiRequestError', () => {
    const original = new ApiRequestError('TEST', 'msg', 400);
    const result = normalizeApiError(original);
    expect(result).toBe(original);
  });

  it('normalizes HttpErrorResponse with valid JSON body', () => {
    const httpError = new HttpErrorResponse({
      error: { code: 'VALIDATION', message: 'Bad data', status: 400, path: '/api/test' },
      status: 400,
      url: '/api/test'
    });

    const result = normalizeApiError(httpError);
    expect(result.code).toBe('VALIDATION');
    expect(result.message).toBe('Bad data');
    expect(result.status).toBe(400);
    expect(result.path).toBe('/api/test');
    expect(result.originalError).toBe(httpError);
  });

  it('normalizes HttpErrorResponse with string body that is valid JSON', () => {
    const jsonBody = JSON.stringify({ code: 'CONFLICT', message: 'Duplicate', status: 409 });
    const httpError = new HttpErrorResponse({
      error: jsonBody,
      status: 409,
      url: '/api/resource'
    });

    const result = normalizeApiError(httpError);
    expect(result.code).toBe('CONFLICT');
    expect(result.message).toBe('Duplicate');
    expect(result.status).toBe(409);
  });

  it('normalizes HttpErrorResponse with string body that is not JSON', () => {
    const httpError = new HttpErrorResponse({
      error: 'Something went wrong',
      status: 500,
      url: '/api/test'
    });

    const result = normalizeApiError(httpError);
    expect(result.code).toBe('INTERNAL_SERVER_ERROR');
    expect(result.status).toBe(500);
    expect(result.originalError).toBe(httpError);
  });

  it('normalizes HttpErrorResponse with empty string body', () => {
    const httpError = new HttpErrorResponse({
      error: '',
      status: 404
    });

    const result = normalizeApiError(httpError);
    expect(result.code).toBe('RESOURCE_NOT_FOUND');
    expect(result.status).toBe(404);
  });

  it('normalizes HttpErrorResponse with whitespace-only string body', () => {
    const httpError = new HttpErrorResponse({
      error: '   ',
      status: 403
    });

    const result = normalizeApiError(httpError);
    expect(result.code).toBe('ACCESS_DENIED');
    expect(result.status).toBe(403);
  });

  it('normalizes HttpErrorResponse with null body', () => {
    const httpError = new HttpErrorResponse({
      error: null,
      status: 401
    });

    const result = normalizeApiError(httpError);
    expect(result.code).toBe('AUTHENTICATION_FAILED');
    expect(result.status).toBe(401);
  });

  it('normalizes HttpErrorResponse with status 0 (network error)', () => {
    const httpError = new HttpErrorResponse({
      error: null,
      status: 0
    });

    const result = normalizeApiError(httpError);
    expect(result.code).toBe('NETWORK_ERROR');
    expect(result.status).toBe(0);
  });

  it('normalizes HttpErrorResponse with unknown status', () => {
    const httpError = new HttpErrorResponse({
      error: null,
      status: 418
    });

    const result = normalizeApiError(httpError);
    expect(result.code).toBe('UNKNOWN_ERROR');
    expect(result.status).toBe(418);
  });

  it('normalizes Error with JSON-parseable message', () => {
    const jsonMsg = JSON.stringify({ code: 'PARSE_ERR', message: 'Parse failed', status: 422 });
    const error = new Error(jsonMsg);

    const result = normalizeApiError(error);
    expect(result.code).toBe('PARSE_ERR');
    expect(result.message).toBe('Parse failed');
    expect(result.status).toBe(422);
    expect(result.originalError).toBe(error);
  });

  it('normalizes Error with non-JSON message', () => {
    const error = new Error('Simple error');

    const result = normalizeApiError(error);
    expect(result.code).toBe('CLIENT_ERROR');
    expect(result.message).toBe('Simple error');
    expect(result.status).toBe(0);
  });

  it('normalizes unknown input to UNKNOWN_ERROR', () => {
    const result = normalizeApiError('some string');
    expect(result.code).toBe('UNKNOWN_ERROR');
    expect(result.status).toBe(0);
  });

  it('normalizes null to UNKNOWN_ERROR', () => {
    const result = normalizeApiError(null);
    expect(result.code).toBe('UNKNOWN_ERROR');
  });

  it('normalizes undefined to UNKNOWN_ERROR', () => {
    const result = normalizeApiError(undefined);
    expect(result.code).toBe('UNKNOWN_ERROR');
  });

  it('normalizes number to UNKNOWN_ERROR', () => {
    const result = normalizeApiError(42);
    expect(result.code).toBe('UNKNOWN_ERROR');
  });

  it('maps status 400 to INVALID_REQUEST', () => {
    const httpError = new HttpErrorResponse({ error: null, status: 400 });
    const result = normalizeApiError(httpError);
    expect(result.code).toBe('INVALID_REQUEST');
  });

  it('maps status 409 to DATA_CONFLICT', () => {
    const httpError = new HttpErrorResponse({ error: null, status: 409 });
    const result = normalizeApiError(httpError);
    expect(result.code).toBe('DATA_CONFLICT');
  });
});

describe('getApiErrorMessage', () => {
  it('returns message from ApiRequestError with known code', () => {
    const error = new ApiRequestError('VALIDATION', 'Bad data', 400);
    expect(getApiErrorMessage(error)).toBe('Bad data');
  });

  it('returns fallback for CLIENT_ERROR code', () => {
    const error = new ApiRequestError('CLIENT_ERROR', 'Simple error', 0);
    expect(getApiErrorMessage(error, 'Custom fallback')).toBe('Custom fallback');
  });

  it('returns fallback for UNKNOWN_ERROR code', () => {
    const error = new ApiRequestError('UNKNOWN_ERROR', 'Unknown', 0);
    expect(getApiErrorMessage(error)).toBe('No se pudo completar la operación.');
  });

  it('returns fallback when message is empty', () => {
    const error = new ApiRequestError('VALIDATION', '', 400);
    expect(getApiErrorMessage(error, 'Empty msg')).toBe('Empty msg');
  });

  it('returns fallback when message is whitespace only', () => {
    const error = new ApiRequestError('VALIDATION', '   ', 400);
    expect(getApiErrorMessage(error, 'Whitespace')).toBe('Whitespace');
  });

  it('uses default fallback when not provided', () => {
    const error = new ApiRequestError('CLIENT_ERROR', 'err', 0);
    expect(getApiErrorMessage(error)).toBe('No se pudo completar la operación.');
  });

  it('handles parses of valid JSON that is not ApiErrorResponse', () => {
    const httpError = new HttpErrorResponse({
      error: '{"hello": "world"}',
      status: 400
    });
    const result = normalizeApiError(httpError);
    expect(result.code).toBe('INVALID_REQUEST');
  });

  it('handles objects that fail isApiErrorResponse partial matches', () => {
    const httpError = new HttpErrorResponse({
      error: { code: 'MY_CODE', message: 'No status' }, // missing status
      status: 400
    });
    const result = normalizeApiError(httpError);
    expect(result.code).toBe('INVALID_REQUEST');
  });
});
