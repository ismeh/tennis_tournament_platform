import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  message: string;
  type: 'error' | 'success' | 'info' | 'warning';
  duration?: number;
  fadingOut?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private currentId = 0;
  readonly toasts = signal<Toast[]>([]);

  show(message: string, type: Toast['type'] = 'info', duration = 5000): void {
    const id = ++this.currentId;
    this.toasts.update(list => [...list, { id, message, type, duration }]);

    if (duration > 0) {
      setTimeout(() => {
        this.remove(id);
      }, duration);
    }
  }

  showError(message: string, duration = 6000): void {
    this.show(message, 'error', duration);
  }

  showSuccess(message: string, duration = 4000): void {
    this.show(message, 'success', duration);
  }

  showWarning(message: string, duration = 5000): void {
    this.show(message, 'warning', duration);
  }

  showInfo(message: string, duration = 4000): void {
    this.show(message, 'info', duration);
  }

  remove(id: number): void {
    this.toasts.update(list =>
      list.map(t => t.id === id ? { ...t, fadingOut: true } : t)
    );

    setTimeout(() => {
      this.toasts.update(list => list.filter(t => t.id !== id));
    }, 200);
  }
}
