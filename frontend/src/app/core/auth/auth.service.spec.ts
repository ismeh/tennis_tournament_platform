import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { AppSettings } from '../../shared/constants';

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

    expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBe('jwt-token');
    expect(localStorage.getItem(AppSettings.REFRESH_TOKEN_KEY)).toBe('refresh-token');
  });

  it('stores token after register', () => {
    service.register({ email: 'new@example.com', password: 'secret123', name: 'New User' }).subscribe();

    const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/register`);
    expect(req.request.method).toBe('POST');
    req.flush({ accessToken: 'registered-token', refreshToken: 'register-refresh-token' });

    expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBe('registered-token');
    expect(localStorage.getItem(AppSettings.REFRESH_TOKEN_KEY)).toBe('register-refresh-token');
  });

  it('refreshes token when refresh token is available', () => {
    localStorage.setItem(AppSettings.REFRESH_TOKEN_KEY, 'old-refresh-token');

    service.refreshToken().subscribe();

    const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/refresh`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ refreshToken: 'old-refresh-token' });

    req.flush({ accessToken: 'new-access-token', refreshToken: 'new-refresh-token' });

    expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBe('new-access-token');
    expect(localStorage.getItem(AppSettings.REFRESH_TOKEN_KEY)).toBe('new-refresh-token');
  });
});
