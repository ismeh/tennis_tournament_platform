import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { requestLoggingInterceptor } from './request-logging.interceptor';
import { RequestLoggerService } from './request-logger.service';

describe('requestLoggingInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let loggerService: jasmine.SpyObj<RequestLoggerService>;

  beforeEach(() => {
    loggerService = jasmine.createSpyObj('RequestLoggerService', ['logRequest']);

    TestBed.configureTestingModule({
      providers: [
        { provide: RequestLoggerService, useValue: loggerService },
        provideHttpClient(withInterceptors([requestLoggingInterceptor])),
        provideHttpClientTesting()
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('logs successful request with status and duration', () => {
    httpClient.get('/api/test').subscribe();

    const request = httpMock.expectOne('/api/test');
    request.flush({ ok: true });

    expect(loggerService.logRequest).toHaveBeenCalled();
    const args = loggerService.logRequest.calls.mostRecent().args;
    expect(args[0]).toBe('GET');
    expect(args[1]).toBe('/api/test');
    expect(args[2]).toBe(200);
    expect(typeof args[3]).toBe('number');
  });

  it('logs request body when present', () => {
    httpClient.post('/api/test', { name: 'test' }).subscribe();

    const request = httpMock.expectOne('/api/test');
    request.flush({ ok: true });

    expect(loggerService.logRequest).toHaveBeenCalled();
    const args = loggerService.logRequest.calls.mostRecent().args;
    expect(args[4]?.requestBody).toEqual({ name: 'test' });
  });

  it('logs error response', () => {
    httpClient.get('/api/test').subscribe({
      error: () => undefined
    });

    const request = httpMock.expectOne('/api/test');
    request.flush('Not Found', { status: 404, statusText: 'Not Found' });

    expect(loggerService.logRequest).toHaveBeenCalled();
    const args = loggerService.logRequest.calls.mostRecent().args;
    expect(args[2]).toBe(404);
  });
});
