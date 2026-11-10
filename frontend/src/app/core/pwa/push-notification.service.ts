import { isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { SwPush } from '@angular/service-worker';
import { Observable, firstValueFrom } from 'rxjs';
import { AppSettings } from '../../shared/constants';
import {
  PushPublicKeyResponse,
  PushSubscriptionRequest,
  PushTestResponse,
} from '../../data/interfaces/push.model';

/**
 * Registers this browser for Web Push and keeps the backend subscription in sync.
 *
 * The public API (`enable`, `disable`, `sendTest`) is deliberately small so a Capacitor
 * build can replace the Web Push implementation with a native provider later without
 * touching the settings screen.
 */
@Injectable({
  providedIn: 'root',
})
export class PushNotificationService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly http = inject(HttpClient);
  private readonly swPush = inject(SwPush, { optional: true });

  private readonly baseUrl = `${AppSettings.API_URL}/push`;

  readonly supported = signal(false);
  readonly enabled = signal(false);
  readonly isSubscribed = signal(false);
  readonly isLoading = signal(false);
  readonly error = signal<string | null>(null);

  constructor() {
    if (!isPlatformBrowser(this.platformId) || !this.swPush?.isEnabled) {
      return;
    }

    this.supported.set(
      typeof window !== 'undefined' &&
        'PushManager' in window &&
        'Notification' in window &&
        'serviceWorker' in navigator,
    );
  }

  /** True when the current device is an iPhone/iPad. */
  get isIos(): boolean {
    if (!isPlatformBrowser(this.platformId) || typeof navigator === 'undefined') {
      return false;
    }
    return /iPad|iPhone|iPod/.test(navigator.userAgent);
  }

  /** iOS only delivers Web Push to PWAs installed on the home screen. */
  get isStandalone(): boolean {
    if (!isPlatformBrowser(this.platformId) || typeof window === 'undefined') {
      return false;
    }
    return window.matchMedia('(display-mode: standalone)').matches;
  }

  /**
   * Reads the server configuration and reflects the browser's current subscription.
   * Safe to call on every visit to the settings page.
   */
  initialize(): void {
    if (!this.supported()) {
      return;
    }

    this.isLoading.set(true);
    this.error.set(null);

    this.http.get<PushPublicKeyResponse>(`${this.baseUrl}/public-key`).subscribe({
      next: (config) => {
        this.enabled.set(config.enabled && !!config.publicKey);
        if (!this.enabled()) {
          this.isSubscribed.set(false);
          this.isLoading.set(false);
          return;
        }
        void this.syncCurrentSubscription().finally(() => this.isLoading.set(false));
      },
      error: () => {
        this.enabled.set(false);
        this.isLoading.set(false);
      },
    });
  }

  /** Requests notification permission and registers the subscription on the server. */
  enable(): void {
    if (!this.supported() || !this.swPush?.isEnabled) {
      this.error.set('Tu navegador no admite notificaciones push.');
      return;
    }

    this.isLoading.set(true);
    this.error.set(null);

    this.http.get<PushPublicKeyResponse>(`${this.baseUrl}/public-key`).subscribe({
      next: (config) => {
        if (!config.enabled || !config.publicKey) {
          this.enabled.set(false);
          this.isLoading.set(false);
          this.error.set('Las notificaciones push no están configuradas en este entorno.');
          return;
        }

        this.enabled.set(true);
        void this.requestSubscription(config.publicKey);
      },
      error: () => {
        this.isLoading.set(false);
        this.error.set('No se pudo consultar la configuración de notificaciones push.');
      },
    });
  }

  /** Removes the subscription from this browser and from the backend. */
  disable(): void {
    if (!this.swPush?.isEnabled) {
      return;
    }

    this.isLoading.set(true);
    this.error.set(null);

    void firstValueFrom(this.swPush.subscription)
      .then(async (subscription) => {
        if (subscription) {
          await firstValueFrom(
            this.http.delete<void>(`${this.baseUrl}/subscriptions`, {
              params: new HttpParams().set('endpoint', subscription.endpoint),
            }),
          );
        }
        await this.swPush!.unsubscribe();
        this.isSubscribed.set(false);
      })
      .catch(() => {
        this.error.set('No se pudieron desactivar las notificaciones push.');
      })
      .finally(() => this.isLoading.set(false));
  }

  sendTest(): Observable<PushTestResponse> {
    return this.http.post<PushTestResponse>(`${this.baseUrl}/test`, {});
  }

  private async requestSubscription(publicKey: string): Promise<void> {
    try {
      const subscription = await this.swPush!.requestSubscription({
        serverPublicKey: publicKey,
      });
      await this.registerSubscription(subscription);
      this.isSubscribed.set(true);
    } catch {
      this.error.set(
        'No se pudo activar. Revisa que las notificaciones estén permitidas para este sitio.',
      );
    } finally {
      this.isLoading.set(false);
    }
  }

  private async syncCurrentSubscription(): Promise<PushSubscription | null> {
    if (!this.swPush?.isEnabled) {
      return null;
    }

    const subscription = await firstValueFrom(this.swPush.subscription);
    if (!subscription) {
      this.isSubscribed.set(false);
      return null;
    }

    await this.registerSubscription(subscription);
    this.isSubscribed.set(true);
    return subscription;
  }

  private registerSubscription(subscription: PushSubscription): Promise<void> {
    const json = subscription.toJSON() as {
      endpoint?: string;
      keys?: { p256dh?: string; auth?: string };
    };

    if (!json.endpoint || !json.keys?.p256dh || !json.keys?.auth) {
      return Promise.reject(new Error('Suscripción push incompleta'));
    }

    const payload: PushSubscriptionRequest = {
      endpoint: json.endpoint,
      keys: { p256dh: json.keys.p256dh, auth: json.keys.auth },
      userAgent: typeof navigator !== 'undefined' ? navigator.userAgent : undefined,
    };

    return firstValueFrom(this.http.post<void>(`${this.baseUrl}/subscriptions`, payload));
  }
}
