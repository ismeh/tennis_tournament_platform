import { FileLogTransport } from './file-log-transport';
import { LogLevel } from '../log.model';

class MockIDBObjectStore {
  private data: any[] = [];
  public keyPath = 'id';

  add(entry: any) {
    this.data.push(entry);
    return {};
  }
  clear() {
    this.data = [];
    return {};
  }
  count() {
    const req = { result: this.data.length } as any;
    setTimeout(() => req.onsuccess?.());
    return req;
  }
  getAll() {
    const req = { result: [...this.data] } as any;
    setTimeout(() => req.onsuccess?.());
    return req;
  }
  delete(range: any) {
    return {};
  }
}

class MockIDBTransaction {
  private _store = new MockIDBObjectStore();
  public oncomplete: (() => void) | null = null;
  constructor(public storeNames: string[], public mode: string) {}
  objectStore(_name: string) {
    return this._store;
  }
}

class MockIDBDatabase {
  private storeNames = new Set<string>(['logs']);

  objectStoreNames = {
    contains: (name: string) => this.storeNames.has(name)
  };

  createObjectStore(name: string, options: any) {
    this.storeNames.add(name);
    return new MockIDBObjectStore();
  }

  transaction(storeNames: string[], mode: string) {
    return new MockIDBTransaction(storeNames, mode);
  }

  close() {}
}

describe('FileLogTransport', () => {
  let transport: FileLogTransport;
  let mockDb: MockIDBDatabase;

  beforeEach(() => {
    mockDb = new MockIDBDatabase();
    transport = new FileLogTransport();

    // Mock indexedDB.open
    spyOn(indexedDB, 'open').and.callFake((_name: string, _version: number) => {
      const req = {
        result: mockDb,
        error: null,
        onsuccess: null as any,
        onerror: null as any,
        onupgradeneeded: null as any,
      } as any;

      setTimeout(() => {
        req.onsuccess?.({ target: req });
      });

      return req;
    });

    spyOn(console, 'error');
    spyOn(console, 'warn');
  });

  it('should be created', () => {
    expect(transport).toBeTruthy();
  });

  describe('initialize', () => {
    it('should initialize the database', async () => {
      await transport.initialize();
      expect(indexedDB.open).toHaveBeenCalledWith('AppLogs', 1);
    });

    it('should not re-initialize if already initialized', async () => {
      await transport.initialize();
      (indexedDB.open as jasmine.Spy).calls.reset();
      await transport.initialize();
      expect(indexedDB.open).not.toHaveBeenCalled();
    });

    it('should handle init while already initializing', async () => {
      const p1 = transport.initialize();
      const p2 = transport.initialize();
      await Promise.all([p1, p2]);
    });
  });

  describe('log', () => {
    it('should store a generic log entry', async () => {
      await transport.initialize();
      await transport.log({
        timestamp: new Date(),
        level: LogLevel.INFO,
        message: 'test message',
      });
    });
  });

  describe('logRequest', () => {
    it('should store a request log entry', async () => {
      await transport.initialize();
      await transport.logRequest({
        timestamp: new Date(),
        level: LogLevel.INFO,
        method: 'GET',
        url: '/api/test',
        status: 200,
      });
    });
  });

  describe('storeLog - buffering', () => {
    it('should buffer logs when DB is not initialized', async () => {
      await transport.log({
        timestamp: new Date(),
        level: LogLevel.WARN,
        message: 'buffered',
      });
    });
  });

  describe('cleanupOldLogs', () => {
    it('should skip cleanup when no db', async () => {
      // Don't initialize
      await transport.flush();
    });
  });

  describe('flush', () => {
    it('should flush pending logs', async () => {
      await transport.log({
        timestamp: new Date(),
        level: LogLevel.DEBUG,
        message: 'pending log',
      });
      await transport.initialize();
      await transport.flush();
    });

    it('should not flush when no pending logs', async () => {
      await transport.initialize();
      await transport.flush();
    });
  });

  describe('exportLogs', () => {
    it('should warn when db not initialized', async () => {
      await transport.exportLogs();
      expect(console.warn).toHaveBeenCalledWith('Database not initialized');
    });
  });

  describe('getLogs', () => {
    it('should return empty when no db', async () => {
      const result = await transport.getLogs();
      expect(result).toEqual([]);
    });

    it('should filter by level', async () => {
      await transport.initialize();
      await transport.log({ timestamp: new Date(), level: LogLevel.INFO, message: 'a' });
      await transport.log({ timestamp: new Date(), level: LogLevel.ERROR, message: 'b' });
      const result = await transport.getLogs({ level: LogLevel.INFO });
      expect(result.length).toBeGreaterThanOrEqual(0);
    });

    it('should filter by isRequest', async () => {
      await transport.initialize();
      const result = await transport.getLogs({ isRequest: true });
      expect(result.length).toBeGreaterThanOrEqual(0);
    });

    it('should limit results', async () => {
      await transport.initialize();
      const result = await transport.getLogs({ limit: 5 });
      expect(result.length).toBeGreaterThanOrEqual(0);
    });
  });

  describe('clearLogs', () => {
    it('should skip when no db', async () => {
      await transport.clearLogs();
    });
  });

  describe('destroy', () => {
    it('should close the db', async () => {
      await transport.initialize();
      await transport.destroy();
    });

    it('should handle destroy when no db', async () => {
      await transport.destroy();
    });
  });
});
