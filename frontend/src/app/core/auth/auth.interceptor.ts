import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  if (isPublicAuthRequest(req.url)) {
    return next(req);
  }

  return authService.getAccessTokenForRequest().pipe(
    switchMap(token => {
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
            return authService.logout().pipe(
              switchMap(() => throwError(() => error))
            );
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
              return authService.logout().pipe(
                switchMap(() => throwError(() => refreshError))
              );
            })
          );
        })
      );
    })
  );
};

function isPublicAuthRequest(url: string): boolean {
  return [
    '/auth/login',
    '/auth/register',
    '/auth/refresh',
    '/auth/logout',
    '/auth/confirm-email',
    '/auth/resend-confirmation'
  ].some(path => url.endsWith(path) || url.includes(`${path}?`));
}
