import { inject, Injectable, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { AppSettings } from '../../shared/constants';

interface RuntimeConfig {
  apiUrl?: string;
  production?: string | boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AppConfigService {
  private static readonly STORAGE_KEY = 'puntomatch_runtime_config';
  private static readonly SSR_DEFAULT_API_URL = 'http://localhost:8080/api';

  private readonly platformId = inject(PLATFORM_ID);
  private loadPromise?: Promise<void>;

  load(): Promise<void> {
    if (this.loadPromise) {
      return this.loadPromise;
    }

    if (!isPlatformBrowser(this.platformId)) {
      // Server-side rendering needs an absolute backend URL.
      AppSettings.configureApiUrl(process.env['API_URL'] ?? AppConfigService.SSR_DEFAULT_API_URL);
      this.loadPromise = Promise.resolve();
      return this.loadPromise;
    }

    this.loadPromise = fetch('/config.json', { cache: 'no-store' })
      .then((response) => (response.ok ? response.json() : {}))
      .then((config: RuntimeConfig) => {
        if (config.apiUrl?.trim()) {
          AppSettings.configureApiUrl(config.apiUrl);
          this.persistConfig(config);
          return;
        }

        this.applyCachedConfig();
      })
      .catch(() => {
        // When offline (or before the first successful load) reuse the last known config.
        this.applyCachedConfig();
      });

    return this.loadPromise;
  }

  private persistConfig(config: RuntimeConfig): void {
    try {
      window.localStorage.setItem(
        AppConfigService.STORAGE_KEY,
        JSON.stringify({ apiUrl: config.apiUrl, production: config.production })
      );
    } catch {
      // Storage can be unavailable in private mode; runtime defaults still work.
    }
  }

  private applyCachedConfig(): void {
    try {
      const rawConfig = window.localStorage.getItem(AppConfigService.STORAGE_KEY);
      if (!rawConfig) {
        return;
      }

      const config = JSON.parse(rawConfig) as RuntimeConfig;
      if (config.apiUrl?.trim()) {
        AppSettings.configureApiUrl(config.apiUrl);
      }
    } catch {
      // Ignore corrupted cached configuration.
    }
  }
}
