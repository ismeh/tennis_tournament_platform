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
      // Also skip network errors (status 0) until the app has fully initialized,
      // and do not show them for GET requests to avoid toast flooding on startup/page loads.
      if (
        apiError.status !== 401 &&
        (appReady.isReady() || apiError.status !== 0) &&
        (req.method !== 'GET' || apiError.status !== 0)
      ) {
        toastService.showError(apiError.message);
      }

      return throwError(() => apiError);
    })
  );
};
