import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { normalizeApiError } from './api-error.util';
import { ToastService } from './toast.service';
import { AppReadyService } from '../app-ready.service';

export const apiErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const toastService = inject(ToastService);
  const appReady = inject(AppReadyService);
  return next(req).pipe(
    catchError(error => {
      const apiError = normalizeApiError(error);

      // Do not show an error toast for 401 status since auth.interceptor.ts handles this
      // by either refreshing the token or redirecting to the login page.
      // Also skip network errors (status 0) until the app has fully initialized
      // to avoid flooding the user with connection errors during startup.
      if (apiError.status !== 401 && (appReady.isReady() || apiError.status !== 0)) {
        toastService.showError(apiError.message);
      }

      return throwError(() => apiError);
    })
  );
};
