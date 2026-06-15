import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { MemberService } from '../../data/services/member.service';
import { ReferenceDataService } from '../../data/services/reference-data.service';
import { ProfileComponent } from './profile';

describe('ProfileComponent', () => {
  let fixture: ComponentFixture<ProfileComponent>;
  let component: ProfileComponent;
  let memberServiceSpy: jasmine.SpyObj<MemberService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let referenceDataServiceSpy: jasmine.SpyObj<ReferenceDataService>;
  let router: Router;

  beforeEach(async () => {
    memberServiceSpy = jasmine.createSpyObj<MemberService>('MemberService', ['getMyProfile', 'updateMyProfile']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['setDisplayName', 'setNationality']);
    referenceDataServiceSpy = jasmine.createSpyObj<ReferenceDataService>('ReferenceDataService', ['getNationalities']);

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
      federationLicense: 'RFET-1'
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
      federationLicense: 'RFET-1'
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
        { provide: MemberService, useValue: memberServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ReferenceDataService, useValue: referenceDataServiceSpy }
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
      federationLicense: 'RFET-1'
    });
    expect(authServiceSpy.setDisplayName).toHaveBeenCalledWith('Rafael Nadal');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/torneos');
  });
});
