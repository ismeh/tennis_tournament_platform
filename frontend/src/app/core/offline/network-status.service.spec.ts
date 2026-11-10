import { TestBed } from '@angular/core/testing';
import { NetworkStatusService } from './network-status.service';

describe('NetworkStatusService', () => {
  let service: NetworkStatusService;
  let originalOnline: PropertyDescriptor | undefined;

  beforeEach(() => {
    originalOnline = Object.getOwnPropertyDescriptor(Navigator.prototype, 'onLine');
    Object.defineProperty(navigator, 'onLine', { configurable: true, value: true });

    TestBed.configureTestingModule({});
    service = TestBed.inject(NetworkStatusService);
  });

  afterEach(() => {
    if (originalOnline) {
      Object.defineProperty(Navigator.prototype, 'onLine', originalOnline);
    }
    Object.defineProperty(navigator, 'onLine', { configurable: true, value: true });
    TestBed.resetTestingModule();
  });

  it('should start online and emit reconnected only after an offline period', () => {
    let reconnections = 0;
    const subscription = service.reconnected$.subscribe(() => reconnections++);

    expect(service.isOnline()).toBeTrue();

    window.dispatchEvent(new Event('offline'));
    expect(service.isOnline()).toBeFalse();

    window.dispatchEvent(new Event('online'));
    expect(service.isOnline()).toBeTrue();
    expect(reconnections).toBe(1);

    window.dispatchEvent(new Event('online'));
    expect(reconnections).toBe(1);

    subscription.unsubscribe();
  });

  it('should emit disconnected events', () => {
    let disconnections = 0;
    const subscription = service.disconnected$.subscribe(() => disconnections++);

    window.dispatchEvent(new Event('offline'));

    expect(disconnections).toBe(1);
    subscription.unsubscribe();
  });
});
