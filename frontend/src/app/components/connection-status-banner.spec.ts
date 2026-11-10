import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { NetworkStatusService } from '../core/offline/network-status.service';
import { PwaUpdateService } from '../core/pwa/pwa-update.service';
import { ConnectionStatusBannerComponent } from './connection-status-banner';

class MockNetworkStatusService {
  readonly isOnline = signal(true);
  readonly reconnected$ = new Subject<void>();
}

class MockPwaUpdateService {
  readonly updateAvailable = signal(false);
  readonly isApplying = signal(false);
  checkForUpdate = jasmine.createSpy('checkForUpdate').and.resolveTo(undefined);
  activateUpdate = jasmine.createSpy('activateUpdate').and.resolveTo(true);
}

describe('ConnectionStatusBannerComponent', () => {
  let fixture: ComponentFixture<ConnectionStatusBannerComponent>;
  let networkStatus: MockNetworkStatusService;
  let pwaUpdate: MockPwaUpdateService;

  beforeEach(async () => {
    networkStatus = new MockNetworkStatusService();
    pwaUpdate = new MockPwaUpdateService();

    await TestBed.configureTestingModule({
      imports: [ConnectionStatusBannerComponent],
      providers: [
        { provide: NetworkStatusService, useValue: networkStatus },
        { provide: PwaUpdateService, useValue: pwaUpdate },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConnectionStatusBannerComponent);
    fixture.detectChanges();
  });

  it('should explain that the app is offline', () => {
    networkStatus.isOnline.set(false);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Sin conexión');
  });

  it('should show the reconnection notice after going back online', () => {
    networkStatus.isOnline.set(false);
    fixture.detectChanges();

    networkStatus.isOnline.set(true);
    networkStatus.reconnected$.next();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Conexión restablecida');
    expect(pwaUpdate.checkForUpdate).toHaveBeenCalled();
  });

  it('should offer an explicit update action', () => {
    pwaUpdate.activateUpdate.and.resolveTo(false);
    pwaUpdate.updateAvailable.set(true);
    fixture.detectChanges();

    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    expect(button.textContent).toContain('Actualizar ahora');

    button.click();

    expect(pwaUpdate.activateUpdate).toHaveBeenCalled();
  });
});
