import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ToastService],
    });
    service = TestBed.inject(ToastService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('show', () => {
    it('should add a toast', () => {
      service.show('test message');
      expect(service.toasts().length).toBe(1);
      expect(service.toasts()[0].message).toBe('test message');
      expect(service.toasts()[0].type).toBe('info');
    });

    it('should add toast with custom type', () => {
      service.show('error msg', 'error');
      expect(service.toasts()[0].type).toBe('error');
    });

    it('should auto-remove toast after duration', fakeAsync(() => {
      service.show('temp', 'info', 100);
      expect(service.toasts().length).toBe(1);
      tick(100);
      expect(service.toasts()[0].fadingOut).toBeTrue();
      tick(200);
      expect(service.toasts().length).toBe(0);
    }));

    it('should not auto-remove if duration is 0', fakeAsync(() => {
      service.show('permanent', 'info', 0);
      expect(service.toasts().length).toBe(1);
      tick(10000);
      expect(service.toasts().length).toBe(1);
    }));
  });

  describe('showError', () => {
    it('should add error toast', () => {
      service.showError('error msg');
      expect(service.toasts().length).toBe(1);
      expect(service.toasts()[0].type).toBe('error');
      expect(service.toasts()[0].message).toBe('error msg');
    });

    it('should use default duration 6000', fakeAsync(() => {
      service.showError('e');
      tick(5900);
      expect(service.toasts().length).toBe(1);
      tick(200);
      expect(service.toasts()[0].fadingOut).toBeTrue();
    }));
  });

  describe('showSuccess', () => {
    it('should add success toast', () => {
      service.showSuccess('ok');
      expect(service.toasts()[0].type).toBe('success');
    });
  });

  describe('showWarning', () => {
    it('should add warning toast', () => {
      service.showWarning('warn');
      expect(service.toasts()[0].type).toBe('warning');
    });
  });

  describe('showInfo', () => {
    it('should add info toast', () => {
      service.showInfo('info');
      expect(service.toasts()[0].type).toBe('info');
    });
  });

  describe('remove', () => {
    it('should mark toast as fading out then remove', fakeAsync(() => {
      service.show('test', 'info', 0);
      const id = service.toasts()[0].id;
      service.remove(id);
      expect(service.toasts()[0].fadingOut).toBeTrue();
      tick(200);
      expect(service.toasts().length).toBe(0);
    }));

    it('should not affect other toasts', fakeAsync(() => {
      service.show('a', 'info', 0);
      service.show('b', 'info', 0);
      const idA = service.toasts()[0].id;
      service.remove(idA);
      tick(200);
      expect(service.toasts().length).toBe(1);
      expect(service.toasts()[0].message).toBe('b');
    }));
  });
});
