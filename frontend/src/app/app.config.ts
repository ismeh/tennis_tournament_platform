import {
  APP_INITIALIZER,
  ApplicationConfig,
  ErrorHandler,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection
} from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './core/auth/auth.interceptor';
import { apiErrorInterceptor } from './core/errors/api-error.interceptor';
import { GlobalErrorHandler } from './core/errors/global-error-handler';
import { requestLoggingInterceptor } from './core/logging/request-logging.interceptor';
import { RequestLoggerService } from './core/logging/request-logger.service';
import { environment } from '../environments/environment';
import { AuthService } from './core/auth/auth.service';
import { firstValueFrom } from 'rxjs';
import { AppConfigService } from './core/config/app-config.service';

export const appConfig: ApplicationConfig = {
  providers: [
    {
      provide: ErrorHandler,
      useClass: GlobalErrorHandler
    },
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideClientHydration(withEventReplay()),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([requestLoggingInterceptor, authInterceptor, apiErrorInterceptor])
    ),
    {
      provide: APP_INITIALIZER,
      useFactory: (appConfigService: AppConfigService) => () => appConfigService.load(),
      deps: [AppConfigService],
      multi: true
    },
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
    },
    {
      provide: APP_INITIALIZER,
      useFactory: (authService: AuthService, appConfigService: AppConfigService) => () =>
        appConfigService.load().then(() => firstValueFrom(authService.loadDisplayNameFromProfile())),
      deps: [AuthService, AppConfigService],
      multi: true
    }
  ]
};
