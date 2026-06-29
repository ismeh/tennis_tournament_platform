import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AccountSettingsComponent } from './account-settings';
import { AuthService, AccountExportData, ConsentHistoryEntry } from '../../core/auth/auth.service';

describe('AccountSettingsComponent', () => {
  let component: AccountSettingsComponent;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', [
      'exportAccountData',
      'getConsentHistory',
      'deleteAccount',
      'logout',
    ]);

    authServiceSpy.getConsentHistory.and.returnValue(of({ history: [] }));

    await TestBed.configureTestingModule({
      imports: [AccountSettingsComponent],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: { navigateByUrl: jasmine.createSpy('navigateByUrl') } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(AccountSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('getEmailLabel', () => {
    it('should mask email', () => {
      expect(component.getEmailLabel('test@example.com')).toBe('test@***');
    });

    it('should return empty for empty email', () => {
      expect(component.getEmailLabel('')).toBe('');
    });

    it('should return raw email if no @', () => {
      expect(component.getEmailLabel('invalidemail')).toBe('invalidemail');
    });
  });

  describe('getRoleLabel', () => {
    it('should return label for ORGANIZER', () => {
      expect(component.getRoleLabel('ORGANIZER')).toBe('Organizador');
    });

    it('should return label for UMPIRE', () => {
      expect(component.getRoleLabel('UMPIRE')).toBe('Árbitro');
    });

    it('should return label for PLAYER', () => {
      expect(component.getRoleLabel('PLAYER')).toBe('Jugador');
    });

    it('should return label for ADMIN', () => {
      expect(component.getRoleLabel('ADMIN')).toBe('Administrador');
    });

    it('should return raw role for unknown', () => {
      expect(component.getRoleLabel('CUSTOM')).toBe('CUSTOM');
    });
  });

  describe('getTierLabel', () => {
    it('should return label for FREE', () => {
      expect(component.getTierLabel('FREE')).toBe('Gratuito');
    });

    it('should return label for INTERMEDIATE', () => {
      expect(component.getTierLabel('INTERMEDIATE')).toBe('Intermedio');
    });

    it('should return label for ADVANCED', () => {
      expect(component.getTierLabel('ADVANCED')).toBe('Avanzado');
    });

    it('should return raw tier for unknown', () => {
      expect(component.getTierLabel('CUSTOM')).toBe('CUSTOM');
    });
  });

  describe('loadExportData', () => {
    it('should load export data', () => {
      const data = {
        account: {
          email: 'test@example.com',
          role: 'PLAYER',
          tier: 'FREE',
          registeredAt: '2024-01-01',
          privacyPolicyAccepted: true,
          privacyPolicyVersion: '1.0',
          termsConditionsAccepted: true,
          termsConditionsVersion: '1.0',
        },
        person: { firstName: 'Test', lastName: 'User', nationality: 'ESP', birthDate: '2000-01-01', gender: 'MALE', tennisId: '' },
        consentHistory: [],
        participations: [],
      } as unknown as AccountExportData;
      authServiceSpy.exportAccountData.and.returnValue(of(data));
      component.loadExportData();
      expect(component.exportData()).toEqual(data);
      expect(component.isLoadingExport()).toBeFalse();
    });

    it('should handle error', () => {
      authServiceSpy.exportAccountData.and.returnValue(throwError(() => new Error('fail')));
      component.loadExportData();
      expect(component.exportError()).toBeTruthy();
    });
  });

  describe('loadConsentHistory', () => {
    it('should load consent history', () => {
      const entries = [{ documentType: 'PRIVACY_POLICY', action: 'GRANTED', createdAt: '2024-01-01' }];
      authServiceSpy.getConsentHistory.and.returnValue(of({ history: entries }));
      component.loadConsentHistory();
      expect(component.consentHistory().length).toBe(1);
    });

    it('should handle error', () => {
      authServiceSpy.getConsentHistory.and.returnValue(throwError(() => new Error('fail')));
      component.loadConsentHistory();
      expect(component.consentHistory().length).toBe(0);
    });
  });

  describe('deleteAccount', () => {
    it('should not delete if form invalid', () => {
      component.deleteAccount();
      expect(authServiceSpy.deleteAccount).not.toHaveBeenCalled();
    });

    it('should not delete if already deleting', () => {
      component.isDeleting.set(true);
      component.deleteForm.get('password')!.setValue('pass');
      component.deleteAccount();
      expect(authServiceSpy.deleteAccount).not.toHaveBeenCalled();
    });

    it('should delete account successfully', () => {
      authServiceSpy.deleteAccount.and.returnValue(of({ message: 'Deleted', processedAt: '2024-01-01' }));
      authServiceSpy.logout.and.returnValue(of(undefined));
      component.deleteForm.get('password')!.setValue('pass');
      component.deleteAccount();
      expect(component.deleteSuccess()).toBe('Deleted');
    });

    it('should handle delete error', () => {
      authServiceSpy.deleteAccount.and.returnValue(throwError(() => new Error('fail')));
      component.deleteForm.get('password')!.setValue('pass');
      component.deleteAccount();
      expect(component.deleteError()).toBeTruthy();
    });
  });

  describe('downloadExport', () => {
    it('should not download if no data', () => {
      expect(() => component.downloadExport()).not.toThrow();
    });

    it('should download if data exists', () => {
      const data = {
        account: {
          email: 'test@example.com',
          role: 'PLAYER',
          tier: 'FREE',
          registeredAt: '2024-01-01',
          privacyPolicyAccepted: true,
          privacyPolicyVersion: '1.0',
          termsConditionsAccepted: true,
          termsConditionsVersion: '1.0',
        },
        person: null,
        consentHistory: [],
        participations: [],
      } as unknown as AccountExportData;
      component.exportData.set(data);
      const anchor = { href: '', download: '', click: jasmine.createSpy('click') };
      spyOn(document, 'createElement').and.returnValue(anchor as any);
      spyOn(document.body, 'appendChild');
      spyOn(document.body, 'removeChild');
      spyOn(URL, 'createObjectURL').and.returnValue('blob:');
      spyOn(URL, 'revokeObjectURL');
      component.downloadExport();
      expect(anchor.click).toHaveBeenCalled();
    });
  });
});
