import { isPlatformBrowser } from '@angular/common';
import { DestroyRef, Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { Subject } from 'rxjs';

/**
 * Tracks the browser connectivity state and exposes transition events.
 *
 * `reconnected$` only emits when the device goes from offline to online, which is the
 * signal used by views to refresh cached data and restart live updates.
 */
@Injectable({
  providedIn: 'root',
})
export class NetworkStatusService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly destroyRef = inject(DestroyRef);

  private readonly onlineState = signal(true);
  private readonly reconnectedSubject = new Subject<void>();
  private readonly disconnectedSubject = new Subject<void>();

  readonly isOnline = this.onlineState.asReadonly();
  readonly reconnected$ = this.reconnectedSubject.asObservable();
  readonly disconnected$ = this.disconnectedSubject.asObservable();

  constructor() {
    if (!isPlatformBrowser(this.platformId) || typeof window === 'undefined') {
      return;
    }

    this.onlineState.set(navigator.onLine);

    const handleOnline = () => {
      const wasOffline = !this.onlineState();
      this.onlineState.set(true);
      if (wasOffline) {
        this.reconnectedSubject.next();
      }
    };

    const handleOffline = () => {
      this.onlineState.set(false);
      this.disconnectedSubject.next();
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    this.destroyRef.onDestroy(() => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      this.reconnectedSubject.complete();
      this.disconnectedSubject.complete();
    });
  }
}
