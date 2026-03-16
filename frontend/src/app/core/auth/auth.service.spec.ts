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
    req.flush({ token: 'jwt-token' });

    expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBe('jwt-token');
  });

  it('stores token after register', () => {
    service.register({ email: 'new@example.com', password: 'secret123', name: 'New User' }).subscribe();

    const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/register`);
    expect(req.request.method).toBe('POST');
    req.flush({ token: 'registered-token' });

    expect(localStorage.getItem(AppSettings.TOKEN_KEY)).toBe('registered-token');
  });
});
