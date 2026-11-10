import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NetworkStatusService } from '../core/offline/network-status.service';
import { PwaUpdateService } from '../core/pwa/pwa-update.service';

/**
 * Global status bar for connectivity and application updates.
 *
 * It is intentionally non-blocking: offline users can keep browsing cached content and
 * updating is always an explicit action, so forms in progress are never discarded.
 */
@Component({
  selector: 'app-connection-status-banner',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (!networkStatus.isOnline()) {
      <div
        class="fixed inset-x-0 bottom-0 z-40 border-t border-amber-300 bg-amber-50 px-4 py-3 pb-[calc(0.75rem+env(safe-area-inset-bottom))] text-sm text-amber-900 shadow-lg"
        role="status"
        aria-live="polite"
      >
        <div class="mx-auto flex max-w-6xl items-center gap-3">
          <svg
            class="h-5 w-5 flex-shrink-0"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            aria-hidden="true"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M18.364 5.636a9 9 0 11-12.728 0M12 9v4m0 4h.01"
            />
          </svg>
          <p class="font-semibold">
            Sin conexión. Puedes seguir consultando los datos guardados; las acciones de edición
            requieren conexión.
          </p>
        </div>
      </div>
    } @else if (justReconnected()) {
      <div
        class="fixed inset-x-0 bottom-0 z-40 border-t border-green-300 bg-green-50 px-4 py-3 pb-[calc(0.75rem+env(safe-area-inset-bottom))] text-sm text-green-900 shadow-lg"
        role="status"
        aria-live="polite"
      >
        <div class="mx-auto flex max-w-6xl items-center gap-3">
          <svg
            class="h-5 w-5 flex-shrink-0"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            aria-hidden="true"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M8.111 16.404a5.5 5.5 0 017.778 0M5.636 13.93a9 9 0 0112.728 0M2.808 11.101a13 13 0 0118.384 0M12 20h.01"
            />
          </svg>
          <p class="font-semibold">Conexión restablecida. Actualizando datos del torneo...</p>
        </div>
      </div>
    } @else if (pwaUpdate.updateAvailable()) {
      <div
        class="fixed inset-x-0 bottom-0 z-40 border-t border-primary-300 bg-white px-4 py-3 pb-[calc(0.75rem+env(safe-area-inset-bottom))] text-sm text-neutral-800 shadow-lg"
        role="status"
        aria-live="polite"
      >
        <div class="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3">
          <p class="font-semibold">Hay una nueva versión de PuntoMatch disponible.</p>
          <button
            type="button"
            class="inline-flex items-center rounded-full bg-primary-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-primary-700 disabled:cursor-not-allowed disabled:bg-neutral-400"
            [disabled]="pwaUpdate.isApplying()"
            (click)="applyUpdate()"
          >
            {{ pwaUpdate.isApplying() ? 'Actualizando...' : 'Actualizar ahora' }}
          </button>
        </div>
      </div>
    }
  `,
})
export class ConnectionStatusBannerComponent implements OnInit {
  readonly networkStatus = inject(NetworkStatusService);
  readonly pwaUpdate = inject(PwaUpdateService);

  readonly justReconnected = signal(false);

  private readonly destroyRef = inject(DestroyRef);
  private reconnectTimeout?: ReturnType<typeof setTimeout>;

  ngOnInit(): void {
    const subscription = this.networkStatus.reconnected$.subscribe(() => {
      this.justReconnected.set(true);
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = setTimeout(() => this.justReconnected.set(false), 4000);
      void this.pwaUpdate.checkForUpdate();
    });

    this.destroyRef.onDestroy(() => {
      subscription.unsubscribe();
      clearTimeout(this.reconnectTimeout);
    });
  }

  applyUpdate(): void {
    void this.pwaUpdate.activateUpdate().then((activated) => {
      if (activated && typeof window !== 'undefined') {
        window.location.reload();
      }
    });
  }
}
