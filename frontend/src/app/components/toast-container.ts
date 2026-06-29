import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, Toast } from '../core/errors/toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fixed top-4 right-4 z-[9999] flex w-full max-w-sm flex-col gap-3 pointer-events-none px-4 sm:px-0">
      @for (toast of toasts(); track toast.id) {
        <div
          [class]="getToastClasses(toast)"
          role="alert"
        >
          <!-- Icon -->
          <div class="mr-3 flex items-start flex-shrink-0" [ngClass]="getIconClass(toast.type)">
            @if (toast.type === 'error') {
              <svg class="h-5 w-5 fill-current" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>
              </svg>
            } @else if (toast.type === 'success') {
              <svg class="h-5 w-5 fill-current" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
              </svg>
            } @else if (toast.type === 'warning') {
              <svg class="h-5 w-5 fill-current" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
                <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/>
              </svg>
            } @else {
              <svg class="h-5 w-5 fill-current" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
                <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"/>
              </svg>
            }
          </div>
          <!-- Content -->
          <div class="flex-grow pr-2">
            <h3 class="text-sm font-semibold text-neutral-900 capitalize">{{ getTitle(toast.type) }}</h3>
            <p class="mt-1 text-xs text-neutral-600 break-words line-clamp-3 hover:line-clamp-none transition-all duration-200 cursor-pointer">
              {{ toast.message }}
            </p>
          </div>
          <!-- Close Button -->
          <button
            type="button"
            (click)="remove(toast.id)"
            class="ml-auto flex items-start text-neutral-400 hover:text-neutral-600 transition-colors flex-shrink-0"
            aria-label="Close"
          >
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    @keyframes slideIn {
      from {
        transform: translateX(120%);
        opacity: 0;
      }
      to {
        transform: translateX(0);
        opacity: 1;
      }
    }

    @keyframes fadeOut {
      from {
        transform: scale(1);
        opacity: 1;
      }
      to {
        transform: scale(0.9);
        opacity: 0;
      }
    }

    .animate-slide-in {
      animation: slideIn 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards;
    }

    .animate-fade-out {
      animation: fadeOut 0.2s cubic-bezier(0.7, 0, 0.84, 0) forwards;
    }
  `]
})
export class ToastContainerComponent {
  private readonly toastService = inject(ToastService);
  readonly toasts = this.toastService.toasts;

  remove(id: number): void {
    this.toastService.remove(id);
  }

  getTitle(type: Toast['type']): string {
    if (type === 'error') return 'Error';
    if (type === 'success') return 'Éxito';
    if (type === 'warning') return 'Advertencia';
    return 'Información';
  }

  getToastClasses(toast: Toast): string {
    const baseClasses = 'pointer-events-auto flex w-full overflow-hidden rounded-xl border bg-white/95 p-4 shadow-lg backdrop-blur-md transition-all duration-300';
    const animClass = toast.fadingOut ? 'animate-fade-out' : 'animate-slide-in';
    
    let borderClass = 'border-neutral-200';
    if (toast.type === 'error') {
      borderClass = 'border-red-500/25 border-l-4 border-l-red-500';
    } else if (toast.type === 'success') {
      borderClass = 'border-green-500/25 border-l-4 border-l-green-500';
    } else if (toast.type === 'warning') {
      borderClass = 'border-amber-500/25 border-l-4 border-l-amber-500';
    } else if (toast.type === 'info') {
      borderClass = 'border-blue-500/25 border-l-4 border-l-blue-500';
    }

    return `${baseClasses} ${animClass} ${borderClass}`;
  }

  getIconClass(type: Toast['type']): string {
    if (type === 'error') return 'text-red-500';
    if (type === 'success') return 'text-green-500';
    if (type === 'warning') return 'text-amber-500';
    return 'text-blue-500';
  }
}
