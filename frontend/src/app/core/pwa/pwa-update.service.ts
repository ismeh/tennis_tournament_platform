import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { SwUpdate } from '@angular/service-worker';

/**
 * Bridges the Angular service worker update lifecycle with the UI.
 *
 * The service is optional-injected so the app keeps working in development builds where
 * the service worker is disabled.
 */
@Injectable({
  providedIn: 'root',
})
export class PwaUpdateService {
  private readonly swUpdate = inject(SwUpdate, { optional: true });
  private readonly destroyRef = inject(DestroyRef);

  private readonly updateAvailableState = signal(false);
  private readonly applyingState = signal(false);

  readonly updateAvailable = this.updateAvailableState.asReadonly();
  readonly isApplying = this.applyingState.asReadonly();

  constructor() {
    if (!this.swUpdate?.isEnabled) {
      return;
    }

    const subscription = this.swUpdate.versionUpdates.subscribe((event) => {
      if (event.type === 'VERSION_READY') {
        this.updateAvailableState.set(true);
      }
    });

    const failures = this.swUpdate.unrecoverable.subscribe(() => {
      this.updateAvailableState.set(false);
      this.applyingState.set(false);
    });

    this.destroyRef.onDestroy(() => {
      subscription.unsubscribe();
      failures.unsubscribe();
    });
  }

  async checkForUpdate(): Promise<void> {
    if (!this.swUpdate?.isEnabled) {
      return;
    }

    try {
      await this.swUpdate.checkForUpdate();
    } catch {
      // Offline or a transient network error: the next check will retry.
    }
  }

  async activateUpdate(): Promise<boolean> {
    if (!this.swUpdate?.isEnabled) {
      return false;
    }

    this.applyingState.set(true);
    try {
      await this.swUpdate.activateUpdate();
      this.updateAvailableState.set(false);
      return true;
    } catch {
      return false;
    } finally {
      this.applyingState.set(false);
    }
  }
}
