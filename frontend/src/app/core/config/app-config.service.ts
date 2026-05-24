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
  private readonly platformId = inject(PLATFORM_ID);
  private loadPromise?: Promise<void>;

  load(): Promise<void> {
    if (this.loadPromise) {
      return this.loadPromise;
    }

    if (!isPlatformBrowser(this.platformId)) {
      this.loadPromise = Promise.resolve();
      return this.loadPromise;
    }

    this.loadPromise = fetch('/config.json', { cache: 'no-store' })
      .then((response) => (response.ok ? response.json() : {}))
      .then((config: RuntimeConfig) => {
        AppSettings.configureApiUrl(config.apiUrl);
      })
      .catch(() => {
        // Keep defaults when config.json is not reachable.
      });

    return this.loadPromise;
  }
}