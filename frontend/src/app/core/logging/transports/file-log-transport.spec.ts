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
  public storeNames = new Set<string>();

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
      const spy = spyOn(mockDb, 'transaction').and.callThrough();
      await transport.log({
        timestamp: new Date(),
        level: LogLevel.INFO,
        message: 'test message',
      });
      expect(spy).toHaveBeenCalled();
    });
  });

  describe('logRequest', () => {
    it('should store a request log entry', async () => {
      await transport.initialize();
      const spy = spyOn(mockDb, 'transaction').and.callThrough();
      await transport.logRequest({
        timestamp: new Date(),
        level: LogLevel.INFO,
        method: 'GET',
        url: '/api/test',
        status: 200,
      });
      expect(spy).toHaveBeenCalled();
    });
  });

  describe('storeLog - buffering', () => {
    it('should buffer logs when DB is not initialized', async () => {
      const spy = spyOn(transport as any, 'initialize').and.callThrough();
      await transport.log({
        timestamp: new Date(),
        level: LogLevel.WARN,
        message: 'buffered',
      });
      expect(spy).toHaveBeenCalled();
      expect((transport as any).pendingLogs.length).toBe(1);
    });
  });

  describe('cleanupOldLogs', () => {
    it('should skip cleanup when no db', async () => {
      const spy = spyOn(mockDb, 'transaction').and.callThrough();
      await (transport as any).cleanupOldLogs();
      expect(spy).not.toHaveBeenCalled();
    });

    it('should trigger deletion when logs exceed maxLogs', async () => {
      await transport.initialize();
      (transport as any).maxLogs = 1;
      
      spyOn(MockIDBObjectStore.prototype, 'count').and.callFake(() => {
        const req = { result: 5 } as any;
        setTimeout(() => req.onsuccess?.());
        return req;
      });
      
      const deleteSpy = spyOn(MockIDBObjectStore.prototype, 'delete').and.callThrough();
      await (transport as any).cleanupOldLogs();
      
      // wait for onsuccess setTimeout
      await new Promise(r => setTimeout(r, 10));
      expect(deleteSpy).toHaveBeenCalled();
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
      const spy = spyOn(transport as any, 'storeLog').and.callThrough();
      await transport.flush();
      expect(spy).toHaveBeenCalled();
    });

    it('should not flush when no pending logs', async () => {
      await transport.initialize();
      const spy = spyOn(transport as any, 'storeLog').and.callThrough();
      await transport.flush();
      expect(spy).not.toHaveBeenCalled();
    });
  });

  describe('exportLogs', () => {
    it('should warn when db not initialized', async () => {
      await transport.exportLogs();
      expect(console.warn).toHaveBeenCalledWith('Database not initialized');
    });

    it('should fetch all logs and trigger file download when db is initialized', async () => {
      await transport.initialize();
      const mockElement = jasmine.createSpyObj('a', ['setAttribute', 'click']);
      mockElement.style = {};
      spyOn(document, 'createElement').and.returnValue(mockElement);
      spyOn(document.body, 'appendChild');
      spyOn(document.body, 'removeChild');

      await transport.exportLogs();
      // wait for indexeddb setTimeout
      await new Promise(r => setTimeout(r, 10));

      expect(document.createElement).toHaveBeenCalledWith('a');
      expect(mockElement.click).toHaveBeenCalled();
    });
  });

  describe('getLogs', () => {
    it('should return empty when no db and init fails', async () => {
      (indexedDB.open as jasmine.Spy).and.callFake(() => {
        const req = { onerror: null as any } as any;
        setTimeout(() => req.onerror?.());
        return req;
      });
      const result = await transport.getLogs();
      expect(result).toEqual([]);
    });

    it('should filter by level', async () => {
      await transport.initialize();
      spyOn(MockIDBObjectStore.prototype, 'getAll').and.callFake(() => {
        const req = {
          result: [
            { level: LogLevel.INFO, isRequest: false },
            { level: LogLevel.ERROR, isRequest: true }
          ]
        } as any;
        setTimeout(() => req.onsuccess?.());
        return req;
      });

      const result = await transport.getLogs({ level: LogLevel.INFO });
      expect(result.length).toBe(1);
      expect(result[0].level).toBe(LogLevel.INFO);
    });

    it('should filter by isRequest', async () => {
      await transport.initialize();
      spyOn(MockIDBObjectStore.prototype, 'getAll').and.callFake(() => {
        const req = {
          result: [
            { level: LogLevel.INFO, isRequest: false },
            { level: LogLevel.ERROR, isRequest: true }
          ]
        } as any;
        setTimeout(() => req.onsuccess?.());
        return req;
      });

      const result = await transport.getLogs({ isRequest: true });
      expect(result.length).toBe(1);
      expect(result[0].level).toBe(LogLevel.ERROR);
    });

    it('should limit results', async () => {
      await transport.initialize();
      spyOn(MockIDBObjectStore.prototype, 'getAll').and.callFake(() => {
        const req = {
          result: [
            { level: LogLevel.INFO, isRequest: false },
            { level: LogLevel.ERROR, isRequest: true },
            { level: LogLevel.WARN, isRequest: false }
          ]
        } as any;
        setTimeout(() => req.onsuccess?.());
        return req;
      });

      const result = await transport.getLogs({ limit: 2 });
      expect(result.length).toBe(2);
    });

    it('should handle getLogs transaction error', async () => {
      await transport.initialize();
      spyOn(MockIDBObjectStore.prototype, 'getAll').and.callFake(() => {
        const req = { onerror: null as any } as any;
        setTimeout(() => req.onerror?.());
        return req;
      });

      const result = await transport.getLogs();
      expect(result).toEqual([]);
    });
  });

  describe('clearLogs', () => {
    it('should skip when no db', async () => {
      const spy = spyOn(mockDb, 'transaction').and.callThrough();
      await transport.clearLogs();
      expect(spy).not.toHaveBeenCalled();
    });

    it('should call clear on objectStore', async () => {
      await transport.initialize();
      const spy = spyOn(MockIDBObjectStore.prototype, 'clear').and.callThrough();
      await transport.clearLogs();
      expect(spy).toHaveBeenCalled();
    });
  });

  describe('destroy', () => {
    it('should close the db', async () => {
      await transport.initialize();
      const spy = spyOn(mockDb, 'close').and.callThrough();
      await transport.destroy();
      expect(spy).toHaveBeenCalled();
      expect((transport as any).db).toBeNull();
    });

    it('should handle destroy when no db', async () => {
      await transport.destroy();
      expect((transport as any).db).toBeNull();
    });
  });

  describe('initialize error cases', () => {
    it('should reject when DB open fails', async () => {
      (indexedDB.open as jasmine.Spy).and.callFake(() => {
        const req = { onerror: null as any, error: new Error('IndexedDB blocked') } as any;
        setTimeout(() => req.onerror?.());
        return req;
      });

      try {
        await transport.initialize();
        fail('Should have thrown error');
      } catch (err: any) {
        expect(err.message).toBe('IndexedDB blocked');
      }
    });

    it('should handle upgradeneeded event', async () => {
      (indexedDB.open as jasmine.Spy).and.callFake(() => {
        const req = {
          result: mockDb,
          onupgradeneeded: null as any,
          onsuccess: null as any
        } as any;
        setTimeout(() => {
          req.onupgradeneeded?.({ target: req });
          req.onsuccess?.({ target: req });
        });
        return req;
      });

      const spy = spyOn(mockDb, 'createObjectStore').and.callThrough();
      await transport.initialize();
      expect(spy).toHaveBeenCalled();
    });
  });
});
