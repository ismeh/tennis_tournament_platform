import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { MemberService } from '../../data/services/member.service';
import { ReferenceDataService } from '../../data/services/reference-data.service';
import { ClubService } from '../../data/services/club.service';
import { ProfileComponent } from './profile';

describe('ProfileComponent', () => {
  let fixture: ComponentFixture<ProfileComponent>;
  let component: ProfileComponent;
  let memberServiceSpy: jasmine.SpyObj<MemberService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let referenceDataServiceSpy: jasmine.SpyObj<ReferenceDataService>;
  let clubServiceSpy: jasmine.SpyObj<ClubService>;
  let router: Router;

  beforeEach(async () => {
    memberServiceSpy = jasmine.createSpyObj<MemberService>('MemberService', ['getMyProfile', 'updateMyProfile']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['setDisplayName', 'setNationality']);
    referenceDataServiceSpy = jasmine.createSpyObj<ReferenceDataService>('ReferenceDataService', ['getNationalities']);
    clubServiceSpy = jasmine.createSpyObj<ClubService>('ClubService', ['searchClubs']);
    clubServiceSpy.searchClubs.and.returnValue(of([]));

    memberServiceSpy.getMyProfile.and.returnValue(of({
      memberId: 'member-id',
      email: 'player@example.com',
      tier: 'FREE',
      role: 'PLAYER',
      registeredAt: '2026-01-01T00:00:00Z',
      personId: 'person-id',
      firstName: 'Rafael',
      lastName: 'Nadal',
      gender: 'MALE',
      birthDate: '1986-06-03',
      nationality: 'ESP',
      federationLicense: 'RFET-1',
      clubId: null,
      clubName: null
    }));
    memberServiceSpy.updateMyProfile.and.returnValue(of({
      memberId: 'member-id',
      email: 'player@example.com',
      tier: 'FREE',
      role: 'PLAYER',
      registeredAt: '2026-01-01T00:00:00Z',
      personId: 'person-id',
      firstName: 'Rafael',
      lastName: 'Nadal',
      gender: 'MALE',
      birthDate: '1986-06-03',
      nationality: 'SUI',
      federationLicense: 'RFET-1',
      clubId: null,
      clubName: null
    }));
    referenceDataServiceSpy.getNationalities.and.returnValue(of([
      {
        code: 'ESP',
        name: 'España'
      },
      {
        code: 'SUI',
        name: 'Suiza'
      }
    ]));

    await TestBed.configureTestingModule({
      imports: [ProfileComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        { provide: MemberService, useValue: memberServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ReferenceDataService, useValue: referenceDataServiceSpy },
        { provide: ClubService, useValue: clubServiceSpy }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load nationalities and submit the selected code', () => {
    expect(component.nationalities().map(nationality => nationality.code)).toEqual(['ESP', 'SUI']);
    expect(component.form.value.nationality).toBe('ESP');

    component.form.patchValue({
      nationality: 'SUI'
    });
    component.submit();

    expect(memberServiceSpy.updateMyProfile).toHaveBeenCalledWith({
      firstName: 'Rafael',
      lastName: 'Nadal',
      gender: 'MALE',
      birthDate: '1986-06-03',
      nationality: 'SUI',
      federationLicense: 'RFET-1',
      clubName: null
    });
    expect(authServiceSpy.setDisplayName).toHaveBeenCalledWith('Rafael Nadal');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/torneos');
  });

  describe('role labelling branches', () => {
    it('labels role as Arbitro when UMPIRE', () => {
      memberServiceSpy.getMyProfile.and.returnValue(of({ role: 'UMPIRE' } as any));
      component.ngOnInit();
      expect(component.roleLabel()).toBe('Árbitro');
    });

    it('labels role as Jugador when PLAYER', () => {
      memberServiceSpy.getMyProfile.and.returnValue(of({ role: 'PLAYER' } as any));
      component.ngOnInit();
      expect(component.roleLabel()).toBe('Jugador');
    });
  });

  describe('ngOnInit errors', () => {
    it('sets nationalities to empty list if getNationalities fails', () => {
      referenceDataServiceSpy.getNationalities.and.returnValue(throwError(() => new Error('Reference error')));
      component.ngOnInit();
      expect(component.nationalities()).toEqual([]);
    });

    it('sets errorMessage if getMyProfile fails', () => {
      memberServiceSpy.getMyProfile.and.returnValue(throwError(() => new Error('Load failed')));
      component.ngOnInit();
      expect(component.errorMessage()).toContain('No se pudo cargar tu perfil actual');
    });
  });

  describe('submit error', () => {
    it('sets error message and sets isSubmitting to false on failure', () => {
      memberServiceSpy.updateMyProfile.and.returnValue(throwError(() => new Error('Save failed')));
      component.submit();
      expect(component.isSubmitting()).toBeFalse();
      expect(component.errorMessage()).toContain('No se pudo guardar el perfil');
    });
  });
});
