import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { AppSettings } from '../../shared/constants';

function createJwt(payload: Record<string, unknown>): string {
  const encodedHeader = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const encodedPayload = btoa(JSON.stringify(payload));
  return `${encodedHeader}.${encodedPayload}.signature`;
}

function flushProfileRequest(httpMock: HttpTestingController): void {
  const profileRequest = httpMock.expectOne(AppSettings.API_URL + '/auth/profile');
  expect(profileRequest.request.method).toBe('GET');
  expect(profileRequest.request.headers.get('Authorization')).toBe('Bearer fresh-access-token');
  profileRequest.flush({
    memberId: 'member-id',
    email: 'test@example.com',
    tier: 'BASIC',
    registeredAt: '2026-01-01T00:00:00Z',
    personId: null,
    firstName: 'Test',
    lastName: 'Player',
    gender: null,
    birthDate: null,
    nationality: null,
    federationLicense: null
  });
}

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('clears the session when a protected request returns 401 without refresh token', () => {
    localStorage.setItem(AppSettings.TOKEN_KEY, 'expired-access-token');
    localStorage.setItem(AppSettings.USER_NAME_KEY, 'Player');

    httpClient.get('/api/private').subscribe({
      error: () => undefined
    });

    const request = httpMock.expectOne('/api/private');
    expect(request.request.headers.get('Authorization')).toBe('Bearer expired-access-token');

    request.flush('Unauthorized', {
      status: 401,
      statusText: 'Unauthorized'
    });

    expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBeNull();
    expect(localStorage.getItem(AppSettings.USER_NAME_KEY)).toBeNull();
    expect(localStorage.getItem(AppSettings.REFRESH_TOKEN_KEY)).toBeNull();
  });

  it('refreshes the token before sending a protected request when it is close to expiring', () => {
    const nearExpiryToken = createJwt({
      exp: Math.floor(Date.now() / 1000) + 240
    });
    localStorage.setItem(AppSettings.TOKEN_KEY, nearExpiryToken);
    localStorage.setItem(AppSettings.REFRESH_TOKEN_KEY, 'soon-refresh-token');

    httpClient.get('/api/private').subscribe();

    const refreshRequest = httpMock.expectOne(`${AppSettings.API_URL}/auth/refresh`);
    expect(refreshRequest.request.method).toBe('POST');
    expect(refreshRequest.request.body).toEqual({ refreshToken: 'soon-refresh-token' });

    refreshRequest.flush({ accessToken: 'fresh-access-token', refreshToken: 'fresh-refresh-token' });

    flushProfileRequest(httpMock);

    const request = httpMock.expectOne('/api/private');
    expect(request.request.headers.get('Authorization')).toBe('Bearer fresh-access-token');

    request.flush({ ok: true });

    expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBe('fresh-access-token');
    expect(localStorage.getItem(AppSettings.REFRESH_TOKEN_KEY)).toBe('fresh-refresh-token');
  });

  it('logs out once when refresh token is invalid without retrying refresh for logout', () => {
    const expiredToken = createJwt({
      exp: Math.floor(Date.now() / 1000) - 60
    });
    localStorage.setItem(AppSettings.TOKEN_KEY, expiredToken);
    localStorage.setItem(AppSettings.REFRESH_TOKEN_KEY, 'invalid-refresh-token');
    localStorage.setItem(AppSettings.USER_NAME_KEY, 'Player');

    httpClient.get('/api/private').subscribe({
      error: () => undefined
    });

    const refreshRequest = httpMock.expectOne(`${AppSettings.API_URL}/auth/refresh`);
    expect(refreshRequest.request.method).toBe('POST');
    expect(refreshRequest.request.body).toEqual({ refreshToken: 'invalid-refresh-token' });

    refreshRequest.flush('Unauthorized', {
      status: 401,
      statusText: 'Unauthorized'
    });

    const logoutRequest = httpMock.expectOne(`${AppSettings.API_URL}/auth/logout`);
    expect(logoutRequest.request.method).toBe('POST');
    expect(logoutRequest.request.body).toEqual({ refreshToken: 'invalid-refresh-token' });
    expect(logoutRequest.request.headers.has('Authorization')).toBeFalse();

    logoutRequest.flush(null, {
      status: 204,
      statusText: 'No Content'
    });

    httpMock.expectNone(`${AppSettings.API_URL}/auth/refresh`);
    expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBeNull();
    expect(localStorage.getItem(AppSettings.USER_NAME_KEY)).toBeNull();
    expect(localStorage.getItem(AppSettings.REFRESH_TOKEN_KEY)).toBeNull();
  });
});
