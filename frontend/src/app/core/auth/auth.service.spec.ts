import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
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

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        AuthService
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('stores token after login', () => {
    service.login({ email: 'test@example.com', password: 'secret123' }).subscribe();

    const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush({ accessToken: 'jwt-token', refreshToken: 'refresh-token' });

    flushProfileRequest(httpMock);

    expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBe('jwt-token');
    expect(localStorage.getItem(AppSettings.REFRESH_TOKEN_KEY)).toBe('refresh-token');
  });

  it('does not create a session after register until email is confirmed', () => {
    service.register({ email: 'new@example.com', password: 'secret123', role: 'PLAYER' }).subscribe();

    const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/register`);
    expect(req.request.method).toBe('POST');
    req.flush({
      emailVerificationRequired: true,
      message: 'Cuenta creada. Revisa tu correo para confirmar el email.'
    });

    expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBeNull();
    expect(localStorage.getItem(AppSettings.REFRESH_TOKEN_KEY)).toBeNull();
  });

  it('confirms email with token', () => {
    service.confirmEmail('confirmation-token').subscribe();

    const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/confirm-email?token=confirmation-token`);
    expect(req.request.method).toBe('GET');
    req.flush({
      emailVerificationRequired: false,
      message: 'Email confirmado correctamente.'
    });
  });

  it('refreshes token when refresh token is available', () => {
    localStorage.setItem(AppSettings.REFRESH_TOKEN_KEY, 'old-refresh-token');

    service.refreshToken().subscribe();

    const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/refresh`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ refreshToken: 'old-refresh-token' });

    req.flush({ accessToken: 'new-access-token', refreshToken: 'new-refresh-token' });

    flushProfileRequest(httpMock);

    expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBe('new-access-token');
    expect(localStorage.getItem(AppSettings.REFRESH_TOKEN_KEY)).toBe('new-refresh-token');
  });

  it('refreshes the access token when it has less than five minutes remaining', () => {
    const nearExpiryToken = createJwt({
      exp: Math.floor(Date.now() / 1000) + 240
    });
    localStorage.setItem(AppSettings.TOKEN_KEY, nearExpiryToken);
    localStorage.setItem(AppSettings.REFRESH_TOKEN_KEY, 'soon-refresh-token');

    let emittedToken: string | null | undefined;
    service.getAccessTokenForRequest().subscribe(token => {
      emittedToken = token;
    });

    const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/refresh`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ refreshToken: 'soon-refresh-token' });

    req.flush({ accessToken: 'fresh-access-token', refreshToken: 'fresh-refresh-token' });

    flushProfileRequest(httpMock);

    expect(emittedToken).toBe('fresh-access-token');
    expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBe('fresh-access-token');
    expect(localStorage.getItem(AppSettings.REFRESH_TOKEN_KEY)).toBe('fresh-refresh-token');
  });

  it('returns expired token without clearing session', () => {
    const expiredToken = createJwt({
      exp: Math.floor(Date.now() / 1000) - 60
    });
    localStorage.setItem(AppSettings.TOKEN_KEY, expiredToken);
    localStorage.setItem(AppSettings.REFRESH_TOKEN_KEY, 'refresh-token');
    localStorage.setItem(AppSettings.USER_NAME_KEY, 'Old Name');

    expect(service.getToken()).toBe(expiredToken);
    expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBe(expiredToken);
    expect(localStorage.getItem(AppSettings.REFRESH_TOKEN_KEY)).toBe('refresh-token');
    expect(localStorage.getItem(AppSettings.USER_NAME_KEY)).toBe('Old Name');
  });

  it('allows reactive refresh when access token is completely expired but refresh token is valid', () => {
    const completelyExpiredToken = createJwt({
      exp: Math.floor(Date.now() / 1000) - 600
    });
    localStorage.setItem(AppSettings.TOKEN_KEY, completelyExpiredToken);
    localStorage.setItem(AppSettings.REFRESH_TOKEN_KEY, 'still-valid-refresh');

    let emittedToken: string | null | undefined;
    service.getAccessTokenForRequest().subscribe(token => {
      emittedToken = token;
    });

    const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/refresh`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ refreshToken: 'still-valid-refresh' });

    req.flush({ accessToken: 'brand-new-access-token', refreshToken: 'brand-new-refresh-token' });

    flushProfileRequest(httpMock);

    expect(emittedToken).toBe('brand-new-access-token');
    expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBe('brand-new-access-token');
  });
});
