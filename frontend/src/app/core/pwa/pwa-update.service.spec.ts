import { TestBed } from '@angular/core/testing';
import { SwUpdate } from '@angular/service-worker';
import { Subject } from 'rxjs';
import { PwaUpdateService } from './pwa-update.service';

class MockSwUpdate {
  readonly isEnabled = true;
  readonly versionUpdates = new Subject<any>();
  readonly unrecoverable = new Subject<any>();
  checkForUpdate = jasmine.createSpy('checkForUpdate').and.resolveTo(undefined);
  activateUpdate = jasmine.createSpy('activateUpdate').and.resolveTo(undefined);
}

describe('PwaUpdateService', () => {
  let service: PwaUpdateService;
  let swUpdate: MockSwUpdate;

  beforeEach(() => {
    swUpdate = new MockSwUpdate();

    TestBed.configureTestingModule({
      providers: [{ provide: SwUpdate, useValue: swUpdate }],
    });

    service = TestBed.inject(PwaUpdateService);
  });

  it('should flag an update when a new version is ready', () => {
    expect(service.updateAvailable()).toBeFalse();

    swUpdate.versionUpdates.next({ type: 'VERSION_READY' });

    expect(service.updateAvailable()).toBeTrue();
  });

  it('should activate the update and clear the pending flag', async () => {
    swUpdate.versionUpdates.next({ type: 'VERSION_READY' });

    const activated = await service.activateUpdate();

    expect(activated).toBeTrue();
    expect(swUpdate.activateUpdate).toHaveBeenCalled();
    expect(service.updateAvailable()).toBeFalse();
  });

  it('should delegate update checks to the service worker', async () => {
    await service.checkForUpdate();

    expect(swUpdate.checkForUpdate).toHaveBeenCalled();
  });
});
