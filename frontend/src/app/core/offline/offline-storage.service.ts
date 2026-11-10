import { Injectable } from '@angular/core';

export interface OfflineRecord<T> {
  key: string;
  data: T;
  cachedAt: string;
}

/**
 * Minimal IndexedDB key/value store used to keep read-only snapshots available offline.
 *
 * The Angular service worker already caches HTTP responses; this store adds a durable
 * per-entity fallback that survives cache eviction and lets the UI explain how old the
 * displayed data is.
 */
@Injectable({
  providedIn: 'root',
})
export class OfflineStorageService {
  private static readonly DB_NAME = 'puntomatch-offline';
  private static readonly DB_VERSION = 1;
  private static readonly STORE_NAME = 'snapshots';

  private databasePromise: Promise<IDBDatabase | null> | null = null;

  isSupported(): boolean {
    return typeof indexedDB !== 'undefined';
  }

  async read<T>(key: string): Promise<OfflineRecord<T> | null> {
    const database = await this.openDatabase();
    if (!database) {
      return null;
    }

    try {
      const record = await this.runRequest<OfflineRecord<T> | undefined>(
        database
          .transaction(OfflineStorageService.STORE_NAME, 'readonly')
          .objectStore(OfflineStorageService.STORE_NAME)
          .get(key),
      );
      return record ?? null;
    } catch {
      return null;
    }
  }

  async write<T>(key: string, data: T): Promise<void> {
    const database = await this.openDatabase();
    if (!database) {
      return;
    }

    const record: OfflineRecord<T> = {
      key,
      data,
      cachedAt: new Date().toISOString(),
    };

    try {
      await this.runRequest(
        database
          .transaction(OfflineStorageService.STORE_NAME, 'readwrite')
          .objectStore(OfflineStorageService.STORE_NAME)
          .put(record),
      );
    } catch {
      // Offline caching is best-effort: private mode or full storage must not break the app.
    }
  }

  async remove(key: string): Promise<void> {
    const database = await this.openDatabase();
    if (!database) {
      return;
    }

    try {
      await this.runRequest(
        database
          .transaction(OfflineStorageService.STORE_NAME, 'readwrite')
          .objectStore(OfflineStorageService.STORE_NAME)
          .delete(key),
      );
    } catch {
      // Best-effort cleanup.
    }
  }

  async clear(): Promise<void> {
    const database = await this.openDatabase();
    if (!database) {
      return;
    }

    try {
      await this.runRequest(
        database
          .transaction(OfflineStorageService.STORE_NAME, 'readwrite')
          .objectStore(OfflineStorageService.STORE_NAME)
          .clear(),
      );
    } catch {
      // Best-effort cleanup.
    }
  }

  private openDatabase(): Promise<IDBDatabase | null> {
    if (!this.isSupported()) {
      return Promise.resolve(null);
    }

    if (!this.databasePromise) {
      this.databasePromise = new Promise<IDBDatabase | null>((resolve) => {
        const request = indexedDB.open(
          OfflineStorageService.DB_NAME,
          OfflineStorageService.DB_VERSION,
        );

        request.onupgradeneeded = () => {
          const database = request.result;
          if (!database.objectStoreNames.contains(OfflineStorageService.STORE_NAME)) {
            database.createObjectStore(OfflineStorageService.STORE_NAME, { keyPath: 'key' });
          }
        };

        request.onsuccess = () => resolve(request.result);
        request.onerror = () => resolve(null);
        request.onblocked = () => resolve(null);
      });
    }

    return this.databasePromise;
  }

  private runRequest<T>(request: IDBRequest<T>): Promise<T> {
    return new Promise<T>((resolve, reject) => {
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
  }
}
