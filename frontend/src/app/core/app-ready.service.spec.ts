import { TestBed } from '@angular/core/testing';
import { AppReadyService } from './app-ready.service';

describe('AppReadyService', () => {
  let service: AppReadyService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AppReadyService]
    });
    service = TestBed.inject(AppReadyService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should start not ready and be marked ready', () => {
    expect(service.isReady()).toBeFalse();
    service.markReady();
    expect(service.isReady()).toBeTrue();
  });
});
