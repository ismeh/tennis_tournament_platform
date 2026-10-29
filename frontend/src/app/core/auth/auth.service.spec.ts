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
    service.register({ email: 'new@example.com', password: 'secret123', role: 'PLAYER', privacyPolicyAccepted: true }).subscribe();

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

  describe('getCurrentUserEmail', () => {
    it('returns null when no token is present', () => {
      expect(service.getCurrentUserEmail()).toBeNull();
    });

    it('returns null when token payload is invalid', () => {
      localStorage.setItem(AppSettings.TOKEN_KEY, 'invalid-jwt-format');
      expect(service.getCurrentUserEmail()).toBeNull();
    });

    it('returns email from sub claim', () => {
      const token = createJwt({ sub: 'user@example.com' });
      localStorage.setItem(AppSettings.TOKEN_KEY, token);
      expect(service.getCurrentUserEmail()).toBe('user@example.com');
    });

    it('returns email from email claim', () => {
      const token = createJwt({ email: 'user2@example.com' });
      localStorage.setItem(AppSettings.TOKEN_KEY, token);
      expect(service.getCurrentUserEmail()).toBe('user2@example.com');
    });

    it('returns email from preferred_username claim', () => {
      const token = createJwt({ preferred_username: 'user3@example.com' });
      localStorage.setItem(AppSettings.TOKEN_KEY, token);
      expect(service.getCurrentUserEmail()).toBe('user3@example.com');
    });

    it('returns null when sub claim has no email format', () => {
      const token = createJwt({ sub: 'justname' });
      localStorage.setItem(AppSettings.TOKEN_KEY, token);
      expect(service.getCurrentUserEmail()).toBeNull();
    });
  });

  describe('logout', () => {
    it('performs logout and clears session when refresh token is available', () => {
      localStorage.setItem(AppSettings.REFRESH_TOKEN_KEY, 'refresh-token');
      localStorage.setItem(AppSettings.TOKEN_KEY, 'access-token');

      service.logout().subscribe();

      const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/logout`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ refreshToken: 'refresh-token' });
      req.flush(null);

      expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBeNull();
      expect(localStorage.getItem(AppSettings.REFRESH_TOKEN_KEY)).toBeNull();
    });

    it('performs logout and clears session when refresh token is missing', () => {
      localStorage.setItem(AppSettings.TOKEN_KEY, 'access-token');

      service.logout().subscribe();

      httpMock.expectNone(`${AppSettings.API_URL}/auth/logout`);
      expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBeNull();
    });

    it('clears session even if logout API request fails', () => {
      localStorage.setItem(AppSettings.REFRESH_TOKEN_KEY, 'refresh-token');
      localStorage.setItem(AppSettings.TOKEN_KEY, 'access-token');

      service.logout().subscribe();

      const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/logout`);
      req.flush('Error', { status: 500, statusText: 'Server Error' });

      expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBeNull();
    });
  });

  describe('API endpoints', () => {
    it('calls resendConfirmation', () => {
      service.resendConfirmation('test@test.com').subscribe();
      const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/resend-confirmation`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ email: 'test@test.com' });
      req.flush({ emailVerificationRequired: true, message: 'Sent' });
    });

    it('calls exportAccountData', () => {
      service.exportAccountData().subscribe();
      const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/account/export`);
      expect(req.request.method).toBe('GET');
      req.flush({});
    });

    it('calls deleteAccount', () => {
      service.deleteAccount('mypass').subscribe();
      const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/account`);
      expect(req.request.method).toBe('DELETE');
      expect(req.request.body).toEqual({ password: 'mypass' });
      req.flush({});
    });

    it('calls updatePrivacyConsent', () => {
      service.updatePrivacyConsent(true, 'v1').subscribe();
      const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/account/consent`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ accepted: true, privacyPolicyVersion: 'v1' });
      req.flush({});
    });

    it('calls updateTermsConsent', () => {
      service.updateTermsConsent(true, 'v1').subscribe();
      const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/account/consent/terms`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ accepted: true, termsConditionsVersion: 'v1' });
      req.flush({});
    });

    it('calls getConsentHistory', () => {
      service.getConsentHistory().subscribe();
      const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/account/consent/history`);
      expect(req.request.method).toBe('GET');
      req.flush({ history: [] });
    });

    it('calls getLegalDocument', () => {
      service.getLegalDocument('privacy').subscribe();
      const req = httpMock.expectOne(`${AppSettings.API_URL}/legal/privacy/current`);
      expect(req.request.method).toBe('GET');
      req.flush({});
    });
  });

  describe('verifyTokensOnStartup', () => {
    it('clears session when no tokens are present', () => {
      localStorage.setItem(AppSettings.TOKEN_KEY, '');
      localStorage.setItem(AppSettings.REFRESH_TOKEN_KEY, '');
      (service as any).verifyTokensOnStartup();
      expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBeNull();
    });

    it('proactively refreshes token if close to expiry and refresh token exists', () => {
      const nearExpiryToken = createJwt({ exp: Math.floor(Date.now() / 1000) + 200 });
      localStorage.setItem(AppSettings.TOKEN_KEY, nearExpiryToken);
      localStorage.setItem(AppSettings.REFRESH_TOKEN_KEY, 'my-refresh');

      (service as any).verifyTokensOnStartup();

      const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/refresh`);
      expect(req.request.method).toBe('POST');
      req.flush({ accessToken: 'new-token', refreshToken: 'new-refresh' });
      flushProfileRequest(httpMock);

      expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBe('new-token');
    });

    it('proactively refreshes token if expired and refresh token exists', () => {
      const expiredToken = createJwt({ exp: Math.floor(Date.now() / 1000) - 10 });
      localStorage.setItem(AppSettings.TOKEN_KEY, expiredToken);
      localStorage.setItem(AppSettings.REFRESH_TOKEN_KEY, 'my-refresh');

      (service as any).verifyTokensOnStartup();

      const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/refresh`);
      req.flush({ accessToken: 'new-token2', refreshToken: 'new-refresh2' });
      flushProfileRequest(httpMock);

      expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBe('new-token2');
    });

    it('proactively refreshes token if access token missing but refresh exists', () => {
      localStorage.setItem(AppSettings.REFRESH_TOKEN_KEY, 'my-refresh');

      (service as any).verifyTokensOnStartup();

      const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/refresh`);
      req.flush({ accessToken: 'new-token3', refreshToken: 'new-refresh3' });
      flushProfileRequest(httpMock);

      expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBe('new-token3');
    });

    it('updates status and displayName when valid token present', () => {
      const validToken = createJwt({ exp: Math.floor(Date.now() / 1000) + 1000, name: 'Startup User' });
      localStorage.setItem(AppSettings.TOKEN_KEY, validToken);

      (service as any).verifyTokensOnStartup();

      let isLoggedIn: boolean | undefined;
      service.isLoggedIn$.subscribe(val => isLoggedIn = val);
      expect(isLoggedIn).toBeTrue();

      let displayName: string | null | undefined;
      service.displayName$.subscribe(val => displayName = val);
      expect(displayName).toBe('Startup User');
    });
  });

  describe('resolveProfileDisplayName', () => {
    it('returns email prefix if fullName is empty and email contains @', () => {
      const profile = {
        firstName: '',
        lastName: '',
        email: 'prefix@example.com'
      } as any;
      const resolved = (service as any).resolveProfileDisplayName(profile);
      expect(resolved).toBe('prefix');
    });

    it('returns email directly if fullName is empty and email does not contain @', () => {
      const profile = {
        firstName: '',
        lastName: '',
        email: 'noatsymbol'
      } as any;
      const resolved = (service as any).resolveProfileDisplayName(profile);
      expect(resolved).toBe('noatsymbol');
    });
  });

  describe('additional branch coverage tests', () => {
    it('clears session when verifyTokensOnStartup throws error', () => {
      spyOn(service as any, 'shouldRefreshToken').and.callFake(() => {
        throw new Error('Verification crash');
      });
      localStorage.setItem(AppSettings.TOKEN_KEY, 'some-token');
      localStorage.setItem(AppSettings.REFRESH_TOKEN_KEY, 'refresh');
      (service as any).verifyTokensOnStartup();
      expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBeNull();
    });

    it('returns null for invalid stored roles', () => {
      localStorage.setItem(AppSettings.USER_ROLE_KEY, 'SUPERADMIN');
      const role = (service as any).getStoredRole();
      expect(role).toBeNull();
    });

    it('returns null for decodeJwtPayload with invalid token formats', () => {
      expect((service as any).decodeJwtPayload('one.two.three.four')).toBeNull();
      expect((service as any).decodeJwtPayload('one.two')).toBeNull();
      expect((service as any).decodeJwtPayload('one.notbase64!.three')).toBeNull();
    });

    it('logs out and throws error when getAccessTokenForRequest token is close to expiry and refresh token is missing', (done) => {
      const nearExpiryToken = createJwt({ exp: Math.floor(Date.now() / 1000) + 100 });
      localStorage.setItem(AppSettings.TOKEN_KEY, nearExpiryToken);
      localStorage.removeItem(AppSettings.REFRESH_TOKEN_KEY);

      service.getAccessTokenForRequest().subscribe({
        error: (err) => {
          expect(err.message).toContain('No refresh token available');
          expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBeNull();
          done();
        }
      });
    });

    it('logs out and propagates error when getAccessTokenForRequest refresh fails', (done) => {
      const nearExpiryToken = createJwt({ exp: Math.floor(Date.now() / 1000) + 100 });
      localStorage.setItem(AppSettings.TOKEN_KEY, nearExpiryToken);
      localStorage.setItem(AppSettings.REFRESH_TOKEN_KEY, 'my-refresh-token');

      service.getAccessTokenForRequest().subscribe({
        error: (err) => {
          expect(err.status).toBe(400);
          expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBeNull();
          done();
        }
      });

      const refreshReq = httpMock.expectOne(`${AppSettings.API_URL}/auth/refresh`);
      refreshReq.flush('Bad request', { status: 400, statusText: 'Bad Request' });

      const logoutReq = httpMock.expectOne(`${AppSettings.API_URL}/auth/logout`);
      logoutReq.flush(null);
    });

    it('returns empty observable when loadDisplayNameFromProfile is called on server', (done) => {
      (service as any).platformId = 'server';
      service.loadDisplayNameFromProfile().subscribe(() => {
        done();
      });
    });

    it('removes displayName and nationality when set to null', () => {
      localStorage.setItem(AppSettings.USER_NAME_KEY, 'Test User');
      localStorage.setItem(AppSettings.USER_NATIONALITY_KEY, 'ES');
      service.setDisplayName(null);
      service.setNationality(null);
      expect(localStorage.getItem(AppSettings.USER_NAME_KEY)).toBeNull();
      expect(localStorage.getItem(AppSettings.USER_NATIONALITY_KEY)).toBeNull();
    });
  });
});
