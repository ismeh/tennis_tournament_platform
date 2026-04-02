import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { TournamentService } from './tournament.service';
import { AppSettings } from '../../shared/constants';

describe('TournamentService', () => {
  let service: TournamentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), TournamentService]
    });

    service = TestBed.inject(TournamentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should post the tournament payload to the tournaments endpoint', () => {
    const payload = {
      formalName: 'Open de Primavera',
      playStartDate: '2026-05-01',
      playEndDate: '2026-05-10',
      inscriptionStartDate: '2026-04-01',
      inscriptionEndDate: '2026-04-20',
      surfaceCategory: 'CLAY' as const,
      maxPlayers: 32,
      location: 'Club Central'
    };

    service.createTournament(payload).subscribe(response => {
      expect(response.id).toBe('tournament-id');
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/tournaments`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush({
      id: 'tournament-id',
      ...payload,
      status: 'DRAFT',
      providerOrganisationId: 'member-id'
    });
  });
});