import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { AppSettings } from '../../shared/constants';
import { MemberService } from './member.service';

describe('MemberService', () => {
  let service: MemberService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), MemberService]
    });

    service = TestBed.inject(MemberService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should get member by email', () => {
    service.getMemberByEmail('test@example.com').subscribe(response => {
      expect(response.id).toBe('m1');
      expect(response.email).toBe('test@example.com');
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/members/test%40example.com`);
    expect(request.request.method).toBe('GET');

    request.flush({
      id: 'm1',
      email: 'test@example.com',
      username: 'testuser',
      gender: null,
      tier: 'BASIC',
      registeredAt: '2026-01-01T00:00:00Z'
    });
  });

  it('should get my profile', () => {
    service.getMyProfile().subscribe(response => {
      expect(response.memberId).toBe('m1');
      expect(response.firstName).toBe('Test');
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/auth/profile`);
    expect(request.request.method).toBe('GET');

    request.flush({
      memberId: 'm1',
      email: 'test@example.com',
      tier: 'BASIC',
      role: 'PLAYER',
      registeredAt: '2026-01-01T00:00:00Z',
      personId: null,
      firstName: 'Test',
      lastName: 'Player',
      gender: null,
      birthDate: null,
      nationality: null,
      federationLicense: null
    });
  });

  it('should update my profile', () => {
    const payload = { firstName: 'Updated', lastName: 'Name', gender: 'MALE', birthDate: '2000-01-01' };

    service.updateMyProfile(payload).subscribe(response => {
      expect(response.firstName).toBe('Updated');
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/auth/profile`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(payload);

    request.flush({
      memberId: 'm1',
      email: 'test@example.com',
      tier: 'BASIC',
      role: 'PLAYER',
      registeredAt: '2026-01-01T00:00:00Z',
      personId: null,
      firstName: 'Updated',
      lastName: 'Name',
      gender: 'MALE',
      birthDate: '2000-01-01',
      nationality: null,
      federationLicense: null
    });
  });
});
