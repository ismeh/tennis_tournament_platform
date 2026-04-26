import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { TournamentEventsConfigRequest } from '../interfaces/tournament.model';
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

  it('should get the event catalog from the backend endpoint', () => {
    service.getEventCatalog().subscribe(response => {
      expect(response.length).toBe(1);
      expect(response[0].category).toBe('Absoluto Individual Masculino');
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/age-categories`);
    expect(request.request.method).toBe('GET');

    request.flush([
      {
        id: 1,
        category: 'Absoluto Individual Masculino',
        description: 'Individual masculino open'
      }
    ]);
  });

  it('should post the configured events list to the tournament events endpoint', () => {
    const payload: TournamentEventsConfigRequest = {
      events: [
        {
          categoryId: 1,
          gender: 'MALE'
        },
        {
          categoryId: 1,
          gender: 'MIXED'
        }
      ]
    };

    service.saveTournamentEvents('tournament-id', payload).subscribe(response => {
      expect(response).toBeNull();
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/tournaments/tournament-id/events`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush(null);
  });

  it('should patch tournament status to the backend endpoint', () => {
    service.updateTournamentStatus('tournament-id', { status: 'OPEN' }).subscribe(response => {
      expect(response.status).toBe('OPEN');
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/tournaments/tournament-id/status`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ status: 'OPEN' });

    request.flush({
      id: 'tournament-id',
      formalName: 'Open de Primavera',
      playStartDate: '2026-05-01',
      playEndDate: '2026-05-10',
      inscriptionStartDate: '2026-04-01',
      inscriptionEndDate: '2026-04-20',
      surfaceCategory: 'CLAY',
      maxPlayers: 32,
      location: 'Club Central',
      status: 'OPEN',
      providerOrganisationId: 'member-id',
      events: []
    });
  });
});