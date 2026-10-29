import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AppSettings } from '../../shared/constants';
import { InvitationService, InvitationPreview, InviteParticipantResponse, ClaimInvitationResponse } from './invitation.service';

describe('InvitationService', () => {
  let service: InvitationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        InvitationService
      ]
    });

    service = TestBed.inject(InvitationService);
    httpMock = TestBed.inject(HttpTestingController);
    
    // Clear session storage before each test
    sessionStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should preview invitation via GET', () => {
    const mockPreview: InvitationPreview = {
      tournamentName: 'Roland Garros',
      playerDisplayName: 'Rafael Nadal',
      expired: false,
      claimed: false
    };

    service.previewInvitation('test-token').subscribe(res => {
      expect(res).toEqual(mockPreview);
    });

    const req = httpMock.expectOne(`${AppSettings.API_URL}/invitations/preview?token=test-token`);
    expect(req.request.method).toBe('GET');
    req.flush(mockPreview);
  });

  it('should generate invitation via POST', () => {
    const mockResponse: InviteParticipantResponse = {
      invitationUrl: 'http://localhost/invite/abc'
    };

    service.generateInvitation('tourn-123', 'part-456').subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(`${AppSettings.API_URL}/tournaments/tourn-123/participants/part-456/invite`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(mockResponse);
  });

  it('should claim invitation via POST', () => {
    const mockResponse: ClaimInvitationResponse = {
      message: 'Invitation claimed successfully'
    };

    service.claimInvitation('test-token').subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(`${AppSettings.API_URL}/auth/claim-invitation`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ token: 'test-token' });
    req.flush(mockResponse);
  });

  it('should manage pending token in sessionStorage', () => {
    expect(service.hasPendingToken()).toBeFalse();
    expect(service.consumePendingToken()).toBeNull();

    service.storePendingToken('token-123');
    expect(service.hasPendingToken()).toBeTrue();
    
    const consumed = service.consumePendingToken();
    expect(consumed).toBe('token-123');
    expect(service.hasPendingToken()).toBeFalse();
    expect(service.consumePendingToken()).toBeNull();
  });
});
