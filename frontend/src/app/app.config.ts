import {
  APP_INITIALIZER,
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection
} from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './core/auth/auth.interceptor';
import { requestLoggingInterceptor } from './core/logging/request-logging.interceptor';
import { RequestLoggerService } from './core/logging/request-logger.service';
import { LogLevel } from './core/logging/log.model';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideClientHydration(withEventReplay()),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([requestLoggingInterceptor, authInterceptor])
    ),
    {
      provide: APP_INITIALIZER,
      useFactory: (logger: RequestLoggerService) => () =>
        logger.configure({
          enableConsole: true,
          minLogLevel: LogLevel.INFO,
          logRequestBody: false,
          logResponseBody: true,
          logHeaders: false
        }),
      deps: [RequestLoggerService],
      multi: true
    }
  ]
};
