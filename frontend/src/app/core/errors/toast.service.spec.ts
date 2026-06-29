import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ToastService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should add a toast and automatically remove it after duration', fakeAsync(() => {
    service.show('Test Message', 'error', 1000);
    
    let activeToasts = service.toasts();
    expect(activeToasts.length).toBe(1);
    expect(activeToasts[0].message).toBe('Test Message');
    expect(activeToasts[0].type).toBe('error');

    // Fast-forward duration
    tick(1000);
    
    activeToasts = service.toasts();
    expect(activeToasts[0].fadingOut).toBeTrue();

    // Fast-forward animation removal delay
    tick(200);
    
    expect(service.toasts().length).toBe(0);
  }));

  it('should remove a toast manually', fakeAsync(() => {
    service.show('Manual', 'info', 5000);
    const toastId = service.toasts()[0].id;
    
    service.remove(toastId);
    expect(service.toasts()[0].fadingOut).toBeTrue();

    tick(200);
    expect(service.toasts().length).toBe(0);
  }));
});
