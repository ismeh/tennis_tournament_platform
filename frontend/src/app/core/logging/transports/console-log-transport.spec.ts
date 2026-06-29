import { TestBed } from '@angular/core/testing';
import { ConsoleLogTransport } from '../transports/console-log-transport';
import { LogLevel, LogEntry, RequestLogEntry } from '../log.model';

describe('ConsoleLogTransport', () => {
  let transport: ConsoleLogTransport;
  let consoleLogSpy: jasmine.Spy;
  let consoleErrorSpy: jasmine.Spy;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ConsoleLogTransport]
    });

    transport = TestBed.inject(ConsoleLogTransport);
    consoleLogSpy = spyOn(console, 'log');
    consoleErrorSpy = spyOn(console, 'error');
  });

  describe('log', () => {
    it('logs message with data when provided', () => {
      const entry: LogEntry = {
        timestamp: new Date('2026-01-15T10:30:45.123'),
        level: LogLevel.DEBUG,
        message: 'Debug msg',
        data: { key: 'value' }
      };

      transport.log(entry);

      expect(consoleLogSpy).toHaveBeenCalled();
      const args = consoleLogSpy.calls.mostRecent().args;
      expect(args[2]).toBe('Debug msg');
      expect(args[3]).toEqual({ key: 'value' });
    });

    it('logs error to console.error when entry has error', () => {
      const entry: LogEntry = {
        timestamp: new Date(),
        level: LogLevel.ERROR,
        message: 'Error occurred',
        error: new Error('test')
      };

      transport.log(entry);

      expect(consoleErrorSpy).toHaveBeenCalled();
    });

    it('does not log error when entry has no error', () => {
      const entry: LogEntry = {
        timestamp: new Date(),
        level: LogLevel.WARN,
        message: 'Warning'
      };

      transport.log(entry);

      expect(consoleErrorSpy).not.toHaveBeenCalled();
    });
  });

  describe('logRequest', () => {
    it('logs request with status and duration', () => {
      const entry: RequestLogEntry = {
        timestamp: new Date('2026-01-15T10:30:45.123'),
        level: LogLevel.INFO,
        method: 'GET',
        url: '/api/test',
        status: 200,
        duration: 50
      };

      transport.logRequest(entry);

      expect(consoleLogSpy).toHaveBeenCalled();
      const args = consoleLogSpy.calls.mostRecent().args;
      const message = args[0];
      expect(message).toContain('GET');
      expect(message).toContain('200');
      expect(message).toContain('50ms');
      expect(args[2]).toBe('/api/test');
    });

    it('logs request body when provided', () => {
      const entry: RequestLogEntry = {
        timestamp: new Date(),
        level: LogLevel.INFO,
        method: 'POST',
        url: '/api/test',
        status: 201,
        requestBody: { name: 'test' }
      };

      transport.logRequest(entry);

      expect(consoleLogSpy).toHaveBeenCalledTimes(2);
    });

    it('logs response body when provided', () => {
      const entry: RequestLogEntry = {
        timestamp: new Date(),
        level: LogLevel.INFO,
        method: 'GET',
        url: '/api/test',
        status: 200,
        responseBody: { result: 'ok' }
      };

      transport.logRequest(entry);

      expect(consoleLogSpy).toHaveBeenCalledTimes(2);
    });

    it('logs error when provided', () => {
      const entry: RequestLogEntry = {
        timestamp: new Date(),
        level: LogLevel.ERROR,
        method: 'GET',
        url: '/api/test',
        status: 500,
        error: 'Internal server error'
      };

      transport.logRequest(entry);

      expect(consoleErrorSpy).toHaveBeenCalled();
    });

    it('does not log error when not provided', () => {
      const entry: RequestLogEntry = {
        timestamp: new Date(),
        level: LogLevel.INFO,
        method: 'GET',
        url: '/api/test',
        status: 200
      };

      transport.logRequest(entry);

      expect(consoleErrorSpy).not.toHaveBeenCalled();
    });
  });

  describe('getStatusColor', () => {
    it('returns green for 2xx status', () => {
      const entry: RequestLogEntry = {
        timestamp: new Date(), level: LogLevel.INFO, method: 'GET', url: '/', status: 200
      };
      transport.logRequest(entry);
      const style = consoleLogSpy.calls.mostRecent().args[1];
      expect(style).toContain('#10B981');
    });

    it('returns blue for 3xx status', () => {
      const entry: RequestLogEntry = {
        timestamp: new Date(), level: LogLevel.INFO, method: 'GET', url: '/', status: 301
      };
      transport.logRequest(entry);
      const style = consoleLogSpy.calls.mostRecent().args[1];
      expect(style).toContain('#3B82F6');
    });

    it('returns amber for 4xx status', () => {
      const entry: RequestLogEntry = {
        timestamp: new Date(), level: LogLevel.INFO, method: 'GET', url: '/', status: 404
      };
      transport.logRequest(entry);
      const style = consoleLogSpy.calls.mostRecent().args[1];
      expect(style).toContain('#F59E0B');
    });

    it('returns red for 5xx status', () => {
      const entry: RequestLogEntry = {
        timestamp: new Date(), level: LogLevel.INFO, method: 'GET', url: '/', status: 500
      };
      transport.logRequest(entry);
      const style = consoleLogSpy.calls.mostRecent().args[1];
      expect(style).toContain('#EF4444');
    });

    it('returns gray for undefined status', () => {
      const entry: RequestLogEntry = {
        timestamp: new Date(), level: LogLevel.INFO, method: 'GET', url: '/'
      };
      transport.logRequest(entry);
      const style = consoleLogSpy.calls.mostRecent().args[1];
      expect(style).toContain('#6B7280');
    });
  });
});
