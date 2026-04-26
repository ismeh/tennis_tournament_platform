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
import { environment } from '../environments/environment';

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
          enableConsole: environment.logger.enableConsole,
          minLogLevel: environment.logger.minLogLevel,
          logRequestBody: false,
          logResponseBody: true,
          logHeaders: false
        }),
      deps: [RequestLoggerService],
      multi: true
    }
  ]
};
