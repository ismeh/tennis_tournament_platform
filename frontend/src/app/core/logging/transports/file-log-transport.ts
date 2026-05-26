import { Injectable } from '@angular/core';
import { LogEntry, LogLevel, RequestLogEntry } from '../log.model';
import { LogTransport } from '../log-transport.interface';

/**
 * Logs to IndexedDB for persistent storage and enables file download
 */
@Injectable({
  providedIn: 'root'
})
export class FileLogTransport implements LogTransport {
  private dbName = 'AppLogs';
  private storeName = 'logs';
  private maxLogs = 10000; // Maximum logs to retain
  private db: IDBDatabase | null = null;
  private pendingLogs: (LogEntry | RequestLogEntry)[] = [];
  private isInitializing = false;
  private initPromise: Promise<void> | null = null;

  async initialize(): Promise<void> {
    if (this.db) return;
    if (this.isInitializing) return this.initPromise || Promise.resolve();

    this.isInitializing = true;
    this.initPromise = new Promise((resolve, reject) => {
      const request = indexedDB.open(this.dbName, 1);

      request.onerror = () => {
        console.error('Failed to open IndexedDB:', request.error);
        reject(request.error);
      };

      request.onsuccess = () => {
        this.db = request.result;
        resolve();
      };

      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result;
        if (!db.objectStoreNames.contains(this.storeName)) {
          db.createObjectStore(this.storeName, { keyPath: 'id', autoIncrement: true });
        }
      };
    });

    return this.initPromise;
  }

  async log(entry: LogEntry): Promise<void> {
    const logEntry = { ...entry, type: 'generic', isRequest: false };
    await this.storeLog(logEntry);
  }

  async logRequest(entry: RequestLogEntry): Promise<void> {
    const logEntry = { ...entry, type: 'request', isRequest: true };
    await this.storeLog(logEntry);
  }

  private async storeLog(logEntry: any): Promise<void> {
    if (!this.db) {
      // Buffer logs until DB is initialized
      this.pendingLogs.push(logEntry);
      if (!this.isInitializing) {
        await this.initialize();
      }
      return;
    }

    try {
      const transaction = this.db.transaction([this.storeName], 'readwrite');
      const store = transaction.objectStore(this.storeName);
      store.add(logEntry);

      // Cleanup old logs if we exceed max
      transaction.oncomplete = () => {
        this.cleanupOldLogs();
      };
    } catch (error) {
      console.error('Failed to store log:', error);
    }
  }

  private async cleanupOldLogs(): Promise<void> {
    if (!this.db) return;

    try {
      const transaction = this.db.transaction([this.storeName], 'readwrite');
      const store = transaction.objectStore(this.storeName);
      const countRequest = store.count();

      countRequest.onsuccess = () => {
        if (countRequest.result > this.maxLogs) {
          const deleteCount = countRequest.result - this.maxLogs;
          const range = IDBKeyRange.upperBound(deleteCount);
          store.delete(range);
        }
      };
    } catch (error) {
      console.error('Failed to cleanup logs:', error);
    }
  }

  async flush(): Promise<void> {
    if (this.pendingLogs.length > 0 && this.db) {
      const logs = [...this.pendingLogs];
      this.pendingLogs = [];

      for (const logEntry of logs) {
        await this.storeLog(logEntry);
      }
    }
  }

  /**
   * Export all logs as JSON file
   */
  async exportLogs(): Promise<void> {
    if (!this.db) {
      console.warn('Database not initialized');
      return;
    }

    const logs: any[] = [];
    const transaction = this.db.transaction([this.storeName], 'readonly');
    const store = transaction.objectStore(this.storeName);

    const request = store.getAll();
    request.onsuccess = () => {
      const allLogs = request.result;
      const json = JSON.stringify(allLogs, null, 2);
      this.downloadFile(json, `logs-${new Date().toISOString()}.json`);
    };
  }

  /**
   * Clear all logs from storage
   */
  async clearLogs(): Promise<void> {
    if (!this.db) return;

    const transaction = this.db.transaction([this.storeName], 'readwrite');
    const store = transaction.objectStore(this.storeName);
    store.clear();
  }

  /**
   * Get logs filtered by criteria
   */
  async getLogs(filter?: { level?: LogLevel; isRequest?: boolean; limit?: number }): Promise<any[]> {
    if (!this.db) {
      await this.initialize();
      if (!this.db) return [];
    }

    return new Promise((resolve) => {
      const transaction = this.db!.transaction([this.storeName], 'readonly');
      const store = transaction.objectStore(this.storeName);
      const request = store.getAll();

      request.onsuccess = () => {
        let results = request.result;

        if (filter?.level) {
          results = results.filter(log => log.level === filter.level);
        }

        if (filter?.isRequest !== undefined) {
          results = results.filter(log => log.isRequest === filter.isRequest);
        }

        if (filter?.limit) {
          results = results.slice(-filter.limit);
        }

        resolve(results);
      };

      request.onerror = () => {
        console.error('Failed to retrieve logs:', request.error);
        resolve([]);
      };
    });
  }

  async destroy(): Promise<void> {
    if (this.db) {
      this.db.close();
      this.db = null;
    }
  }

  private downloadFile(content: string, filename: string): void {
    const element = document.createElement('a');
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(content));
    element.setAttribute('download', filename);
    element.style.display = 'none';
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  }
}
