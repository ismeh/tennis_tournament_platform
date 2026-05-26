import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap, finalize, catchError } from 'rxjs';
import { throwError } from 'rxjs';
import { RequestLoggerService } from './request-logger.service';

/**
 * HTTP interceptor that automatically logs all requests and responses
 */
export const requestLoggingInterceptor: HttpInterceptorFn = (req, next) => {
  const logger = inject(RequestLoggerService);
  const startTime = performance.now();
  let status = 0;
  let responseBody: any;
  let errorMessage: string | undefined;

  return next(req).pipe(
    tap({
      next: (event) => {
        // Capture response details
        if (event instanceof HttpResponse) {
          status = event.status;
          responseBody = event.body;
        }
      },
      error: (error) => {
        // Capture error details
        status = error.status || 0;
        errorMessage = error.message || 'Unknown error';
      }
    }),
    finalize(() => {
      const duration = Math.round(performance.now() - startTime);
      logger.logRequest(req.method, req.url, status, duration, {
        requestBody: req.body,
        responseBody,
        error: errorMessage
      });
    }),
    catchError((error) => throwError(() => error))
  );
};
