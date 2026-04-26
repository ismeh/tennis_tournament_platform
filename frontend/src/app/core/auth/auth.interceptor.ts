import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  if (req.url.endsWith('/auth/refresh')) {
    return next(req);
  }

  const token = authService.getToken();
  const requestWithAuth = token
    ? req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    })
    : req;

  return next(requestWithAuth).pipe(
    catchError(error => {
      if (error.status !== 401) {
        return throwError(() => error);
      }

      const refreshToken = authService.getRefreshToken();
      if (!refreshToken) {
        authService.logout();
        return throwError(() => error);
      }

      return authService.refreshToken().pipe(
        switchMap(response => {
          const retriedRequest = req.clone({
            setHeaders: {
              Authorization: `Bearer ${response.accessToken}`
            }
          });
          return next(retriedRequest);
        }),
        catchError(refreshError => {
          authService.logout();
          return throwError(() => refreshError);
        })
      );
    })
  );
};