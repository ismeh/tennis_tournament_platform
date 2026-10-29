import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { apiErrorInterceptor } from './api-error.interceptor';
import { ToastService } from './toast.service';
import { ApiRequestError } from './api-error.model';
import { AppReadyService } from '../app-ready.service';

describe('apiErrorInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let toastService: jasmine.SpyObj<ToastService>;

  beforeEach(() => {
    toastService = jasmine.createSpyObj('ToastService', ['showError']);

    TestBed.configureTestingModule({
      providers: [
        { provide: ToastService, useValue: toastService },
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting()
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('shows toast for non-401 errors', () => {
    httpClient.get('/api/test').subscribe({
      error: () => undefined
    });

    const request = httpMock.expectOne('/api/test');
    request.flush('Not Found', { status: 404, statusText: 'Not Found' });

    expect(toastService.showError).toHaveBeenCalled();
  });

  it('does not show toast for 401 errors', () => {
    httpClient.get('/api/test').subscribe({
      error: () => undefined
    });

    const request = httpMock.expectOne('/api/test');
    request.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(toastService.showError).not.toHaveBeenCalled();
  });

  it('rethrows error as ApiRequestError', () => {
    httpClient.get('/api/test').subscribe({
      error: (error) => {
        expect(error).toBeInstanceOf(ApiRequestError);
        expect(error.status).toBe(500);
      }
    });

    const request = httpMock.expectOne('/api/test');
    request.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
  });

  it('passes through successful responses without error', () => {
    httpClient.get('/api/test').subscribe(response => {
      expect(response).toEqual({ ok: true });
    });

    const request = httpMock.expectOne('/api/test');
    request.flush({ ok: true });

    expect(toastService.showError).not.toHaveBeenCalled();
  });

  it('does not show toast for status 0 error when app is not ready', () => {
    httpClient.get('/api/test').subscribe({
      error: () => undefined
    });

    const request = httpMock.expectOne('/api/test');
    request.error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });

    expect(toastService.showError).not.toHaveBeenCalled();
  });

  it('shows toast for status 0 error on POST requests when app is ready', () => {
    const appReady = TestBed.inject(AppReadyService);
    appReady.markReady();

    httpClient.post('/api/test', {}).subscribe({
      error: () => undefined
    });

    const request = httpMock.expectOne('/api/test');
    request.error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });

    expect(toastService.showError).toHaveBeenCalled();
  });

  it('does not show toast for status 0 error on GET requests even when app is ready', () => {
    const appReady = TestBed.inject(AppReadyService);
    appReady.markReady();

    httpClient.get('/api/test').subscribe({
      error: () => undefined
    });

    const request = httpMock.expectOne('/api/test');
    request.error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });

    expect(toastService.showError).not.toHaveBeenCalled();
  });
});
