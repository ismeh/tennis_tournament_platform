import { ErrorHandler, Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ToastService } from './toast.service';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private readonly toastService = inject(ToastService);
  private readonly platformId = inject(PLATFORM_ID);

  handleError(error: any): void {
    // Log the error to console for development and diagnostic purposes
    console.error('Unhandled runtime error:', error);

    // Do not attempt to show toast messages during server-side rendering (SSR)
    if (isPlatformBrowser(this.platformId)) {
      let message = 'Ocurrió un error inesperado en la aplicación.';
      
      if (error) {
        if (error instanceof Error) {
          message = error.message;
        } else if (typeof error === 'string') {
          message = error;
        } else if (typeof error.message === 'string') {
          message = error.message;
        } else {
          try {
            message = String(error);
          } catch {
            // Use default fallback message
          }
        }
      }

      this.toastService.showError(message);
    }
  }
}
