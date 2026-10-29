import { HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { normalizeApiError } from './api-error.util';

export const apiErrorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError(error => throwError(() => normalizeApiError(error)))
  );
};
