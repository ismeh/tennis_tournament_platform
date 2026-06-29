import { TestBed } from '@angular/core/testing';
import { ToastContainerComponent } from './toast-container';
import { ToastService, Toast } from '../core/errors/toast.service';

describe('ToastContainerComponent', () => {
  let component: ToastContainerComponent;
  let toastService: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ToastService]
    });

    const fixture = TestBed.createComponent(ToastContainerComponent);
    component = fixture.componentInstance;
    toastService = TestBed.inject(ToastService);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('getTitle', () => {
    it('returns correct titles for each type', () => {
      expect(component.getTitle('error')).toBe('Error');
      expect(component.getTitle('success')).toBe('Éxito');
      expect(component.getTitle('warning')).toBe('Advertencia');
      expect(component.getTitle('info')).toBe('Información');
    });
  });

  describe('getToastClasses', () => {
    it('returns error border classes for error toast', () => {
      const toast: Toast = { id: 1, message: 'err', type: 'error', fadingOut: false };
      const classes = component.getToastClasses(toast);
      expect(classes).toContain('border-red-500/25');
      expect(classes).toContain('border-l-red-500');
      expect(classes).toContain('animate-slide-in');
    });

    it('returns success border classes for success toast', () => {
      const toast: Toast = { id: 1, message: 'ok', type: 'success', fadingOut: false };
      const classes = component.getToastClasses(toast);
      expect(classes).toContain('border-green-500/25');
      expect(classes).toContain('border-l-green-500');
    });

    it('returns warning border classes for warning toast', () => {
      const toast: Toast = { id: 1, message: 'warn', type: 'warning', fadingOut: false };
      const classes = component.getToastClasses(toast);
      expect(classes).toContain('border-amber-500/25');
      expect(classes).toContain('border-l-amber-500');
    });

    it('returns info border classes for info toast', () => {
      const toast: Toast = { id: 1, message: 'info', type: 'info', fadingOut: false };
      const classes = component.getToastClasses(toast);
      expect(classes).toContain('border-blue-500/25');
      expect(classes).toContain('border-l-blue-500');
    });

    it('returns fade-out animation when fadingOut is true', () => {
      const toast: Toast = { id: 1, message: 'msg', type: 'error', fadingOut: true };
      const classes = component.getToastClasses(toast);
      expect(classes).toContain('animate-fade-out');
    });
  });

  describe('getIconClass', () => {
    it('returns correct icon classes for each type', () => {
      expect(component.getIconClass('error')).toBe('text-red-500');
      expect(component.getIconClass('success')).toBe('text-green-500');
      expect(component.getIconClass('warning')).toBe('text-amber-500');
      expect(component.getIconClass('info')).toBe('text-blue-500');
    });
  });

  describe('remove', () => {
    it('delegates to toastService.remove', () => {
      spyOn(toastService, 'remove');
      component.remove(42);
      expect(toastService.remove).toHaveBeenCalledWith(42);
    });
  });
});
