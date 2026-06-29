import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { RequestLoggerService, LoggerConfig } from './request-logger.service';
import { LogLevel } from './log.model';
import { ConsoleLogTransport } from './transports/console-log-transport';

describe('RequestLoggerService', () => {
  let service: RequestLoggerService;
  let consoleTransport: any;

  beforeEach(() => {
    consoleTransport = jasmine.createSpyObj('ConsoleLogTransport', ['log', 'logRequest', 'flush', 'destroy', 'initialize']);

    TestBed.configureTestingModule({
      providers: [
        RequestLoggerService,
        { provide: ConsoleLogTransport, useValue: consoleTransport }
      ]
    });

    service = TestBed.inject(RequestLoggerService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('configure', () => {
    it('merges config with defaults', async () => {
      await service.configure({ enableConsole: true, logRequestBody: true });
      const config = service.getConfig();
      expect(config.enableConsole).toBe(true);
      expect(config.logRequestBody).toBe(true);
      expect(config.minLogLevel).toBe(LogLevel.ERROR);
    });
  });

  describe('log', () => {
    it('dispatches to transports when level meets minimum', async () => {
      await service.configure({ enableConsole: true, minLogLevel: LogLevel.DEBUG });
      await service.log('test message', { key: 'value' }, LogLevel.INFO);

      expect(consoleTransport.log).toHaveBeenCalled();
      const entry = consoleTransport.log.calls.mostRecent().args[0];
      expect(entry.message).toBe('test message');
      expect(entry.data).toEqual({ key: 'value' });
      expect(entry.level).toBe(LogLevel.INFO);
    });

    it('does not dispatch when level is below minimum', async () => {
      await service.configure({ enableConsole: true, minLogLevel: LogLevel.WARN });
      await service.log('debug msg', undefined, LogLevel.DEBUG);

      expect(consoleTransport.log).not.toHaveBeenCalled();
    });

    it('dispatches ERROR level when minLogLevel is WARN', async () => {
      await service.configure({ enableConsole: true, minLogLevel: LogLevel.WARN });
      await service.log('error msg', undefined, LogLevel.ERROR);

      expect(consoleTransport.log).toHaveBeenCalled();
    });
  });

  describe('logRequest', () => {
    it('logs request with status-based level', async () => {
      await service.configure({ enableConsole: true, minLogLevel: LogLevel.DEBUG, logResponseBody: true });
      await service.logRequest('GET', '/api/test', 200, 50, {
        requestBody: { id: 1 },
        responseBody: { result: 'ok' }
      });

      expect(consoleTransport.logRequest).toHaveBeenCalled();
      const entry = consoleTransport.logRequest.calls.mostRecent().args[0];
      expect(entry.method).toBe('GET');
      expect(entry.url).toBe('/api/test');
      expect(entry.status).toBe(200);
      expect(entry.duration).toBe(50);
      expect(entry.responseBody).toEqual({ result: 'ok' });
    });

    it('does not include requestBody when config.logRequestBody is false', async () => {
      await service.configure({ enableConsole: true, minLogLevel: LogLevel.DEBUG, logRequestBody: false });
      await service.logRequest('POST', '/api/test', 201, 100, {
        requestBody: { secret: 'data' }
      });

      const entry = consoleTransport.logRequest.calls.mostRecent().args[0];
      expect(entry.requestBody).toBeUndefined();
    });

    it('includes requestBody when config.logRequestBody is true', async () => {
      await service.configure({ enableConsole: true, minLogLevel: LogLevel.DEBUG, logRequestBody: true });
      await service.logRequest('POST', '/api/test', 201, 100, {
        requestBody: { name: 'test' }
      });

      const entry = consoleTransport.logRequest.calls.mostRecent().args[0];
      expect(entry.requestBody).toEqual({ name: 'test' });
    });

    it('does not include headers by default', async () => {
      await service.configure({ enableConsole: true, minLogLevel: LogLevel.DEBUG });
      await service.logRequest('GET', '/api/test', 200, 50, {
        headers: { Authorization: 'Bearer token' }
      });

      const entry = consoleTransport.logRequest.calls.mostRecent().args[0];
      expect(entry.headers).toBeUndefined();
    });

    it('does not log when minLogLevel is above the level', async () => {
      await service.configure({ enableConsole: true, minLogLevel: LogLevel.ERROR });
      await service.logRequest('GET', '/api/test', 200, 50);

      expect(consoleTransport.logRequest).not.toHaveBeenCalled();
    });

    it('maps status codes to correct log levels', async () => {
      await service.configure({ enableConsole: true, minLogLevel: LogLevel.DEBUG });

      await service.logRequest('GET', '/api/test', 200, 10);
      expect(consoleTransport.logRequest.calls.mostRecent().args[0].level).toBe(LogLevel.INFO);

      await service.logRequest('GET', '/api/test', 301, 10);
      expect(consoleTransport.logRequest.calls.mostRecent().args[0].level).toBe(LogLevel.DEBUG);

      await service.logRequest('GET', '/api/test', 400, 10);
      expect(consoleTransport.logRequest.calls.mostRecent().args[0].level).toBe(LogLevel.WARN);

      await service.logRequest('GET', '/api/test', 500, 10);
      expect(consoleTransport.logRequest.calls.mostRecent().args[0].level).toBe(LogLevel.ERROR);

      await service.logRequest('GET', '/api/test', 0, 10);
      expect(consoleTransport.logRequest.calls.mostRecent().args[0].level).toBe(LogLevel.DEBUG);
    });
  });

  describe('addTransport / removeTransport', () => {
    it('adds and dispatches to a custom transport', async () => {
      const customTransport = jasmine.createSpyObj('CustomTransport', ['log', 'logRequest']);

      await service.configure({ enableConsole: false, minLogLevel: LogLevel.DEBUG });
      service.addTransport(customTransport);
      await service.log('custom', undefined, LogLevel.INFO);

      expect(customTransport.log).toHaveBeenCalled();
    });

    it('removes a transport', async () => {
      const customTransport = jasmine.createSpyObj('CustomTransport', ['log', 'logRequest', 'destroy']);

      await service.configure({ enableConsole: false, minLogLevel: LogLevel.DEBUG });
      service.addTransport(customTransport);
      service.removeTransport(customTransport);
      await service.log('custom', undefined, LogLevel.INFO);

      expect(customTransport.log).not.toHaveBeenCalled();
      expect(customTransport.destroy).toHaveBeenCalled();
    });

    it('calls initialize on addTransport if available', async () => {
      const customTransport = jasmine.createSpyObj('CustomTransport', ['log', 'logRequest', 'initialize']);

      await service.configure({ enableConsole: false, minLogLevel: LogLevel.DEBUG });
      service.addTransport(customTransport);

      expect(customTransport.initialize).toHaveBeenCalled();
    });
  });

  describe('flush', () => {
    it('calls flush on all transports', async () => {
      await service.configure({ enableConsole: true });
      await service.flush();

      expect(consoleTransport.flush).toHaveBeenCalled();
    });
  });

  describe('destroy', () => {
    it('calls destroy on all transports and clears list', async () => {
      await service.configure({ enableConsole: true });
      await service.destroy();

      expect(consoleTransport.destroy).toHaveBeenCalled();
    });
  });

  describe('redactSensitiveHeaders', () => {
    it('redacts sensitive headers when logHeaders is enabled', async () => {
      await service.configure({ enableConsole: true, minLogLevel: LogLevel.DEBUG, logHeaders: true });
      await service.logRequest('GET', '/api/test', 200, 50, {
        headers: { Authorization: 'Bearer secret', 'Content-Type': 'application/json', 'Cookie': 'session=abc' }
      });

      const entry = consoleTransport.logRequest.calls.mostRecent().args[0];
      expect(entry.headers!['Authorization']).toBe('***REDACTED***');
      expect(entry.headers!['Cookie']).toBe('***REDACTED***');
      expect(entry.headers!['Content-Type']).toBe('application/json');
    });
  });
});
