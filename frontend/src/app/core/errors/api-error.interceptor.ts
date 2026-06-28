import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { normalizeApiError } from './api-error.util';
import { ToastService } from './toast.service';

export const apiErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const toastService = inject(ToastService);
  return next(req).pipe(
    catchError(error => {
      const apiError = normalizeApiError(error);
      
      // Do not show an error toast for 401 status since auth.interceptor.ts handles this
      // by either refreshing the token or redirecting to the login page.
      if (apiError.status !== 401) {
        toastService.showError(apiError.message);
      }
      
      return throwError(() => apiError);
    })
  );
};
