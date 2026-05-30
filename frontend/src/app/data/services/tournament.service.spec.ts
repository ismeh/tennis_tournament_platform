import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { ManualEventInscriptionRequest, TournamentEventsConfigRequest } from '../interfaces/tournament.model';
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
      tournamentStartTime: '09:00',
      inscriptionStartDate: '2026-04-01',
      inscriptionEndDate: '2026-04-20',
      surfaceCategory: 'CLAY' as const,
      maxPlayers: 32,
      location: 'Club Central',
      courtCount: 4
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

  it('should get tournament courts from the backend endpoint', () => {
    service.getCourts('tournament-id').subscribe(response => {
      expect(response.length).toBe(1);
      expect(response[0].name).toBe('Pista 1');
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/tournaments/tournament-id/courts`);
    expect(request.request.method).toBe('GET');

    request.flush([
      {
        id: 'court-id',
        tournamentId: 'tournament-id',
        name: 'Pista 1',
        active: true
      }
    ]);
  });

  it('should post a tournament court to the backend endpoint', () => {
    service.createCourt('tournament-id', { name: 'Pista 2' }).subscribe(response => {
      expect(response.id).toBe('court-id');
      expect(response.name).toBe('Pista 2');
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/tournaments/tournament-id/courts`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ name: 'Pista 2' });

    request.flush({
      id: 'court-id',
      tournamentId: 'tournament-id',
      name: 'Pista 2',
      active: true
    });
  });

  it('should patch a tournament court to the backend endpoint', () => {
    service.updateCourt('tournament-id', 'court-id', { name: 'Central' }).subscribe(response => {
      expect(response.id).toBe('court-id');
      expect(response.name).toBe('Central');
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/tournaments/tournament-id/courts/court-id`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ name: 'Central' });

    request.flush({
      id: 'court-id',
      tournamentId: 'tournament-id',
      name: 'Central',
      active: true
    });
  });

  it('should delete a tournament court through the backend endpoint', () => {
    service.deleteCourt('tournament-id', 'court-id').subscribe(response => {
      expect(response).toBeNull();
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/tournaments/tournament-id/courts/court-id`);
    expect(request.request.method).toBe('DELETE');

    request.flush(null);
  });

  it('should post the configured events list to the tournament events endpoint', () => {
    const payload: TournamentEventsConfigRequest = {
      events: [
        {
          categoryId: 1,
          gender: 'MALE',
          stages: ['SINGLE_ELIMINATION']
        },
        {
          categoryId: 1,
          gender: 'MIXED',
          stages: ['ROUND_ROBIN', 'CONSOLATION']
        }
      ]
    };

    service.saveTournamentEvents('tournament-id', payload).subscribe(response => {
      expect(response.id).toBe('tournament-id');
      expect(response.events?.length).toBe(2);
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/tournaments/tournament-id/events`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush({
      id: 'tournament-id',
      formalName: 'Open de Primavera',
      playStartDate: '2026-05-01',
      playEndDate: '2026-05-10',
      tournamentStartTime: '09:00',
      inscriptionStartDate: '2026-04-01',
      inscriptionEndDate: '2026-04-20',
      surfaceCategory: 'CLAY',
      maxPlayers: 32,
      location: 'Club Central',
      status: 'DRAFT',
      providerOrganisationId: 'member-id',
      events: [
        {
          eventId: 'event-1',
          categoryId: 1,
          gender: 'MALE'
        },
        {
          eventId: 'event-2',
          categoryId: 1,
          gender: 'MIXED'
        }
      ]
    });
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

  it('should get tournament inscriptions with optional event filter', () => {
    service.getTournamentInscriptions('tournament-id', 'event-1').subscribe(response => {
      expect(response.selectedEventId).toBe('event-1');
      expect(response.inscriptions.length).toBe(1);
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/tournaments/tournament-id/inscriptions?eventId=event-1`);
    expect(request.request.method).toBe('GET');

    request.flush({
      tournamentId: 'tournament-id',
      selectedEventId: 'event-1',
      events: [
        {
          eventId: 'event-1',
          categoryId: 1,
          category: 'Absoluto',
          eventName: 'Absoluto - Masculino',
          eventGender: 'MALE'
        }
      ],
      categoryCounts: [
        {
          categoryId: 1,
          category: 'Absoluto',
          totalPlayers: 1,
          genders: [
            {
              gender: 'MALE',
              totalPlayers: 1
            }
          ]
        }
      ],
      inscriptions: [
        {
          inscriptionId: 'inscription-1',
          eventId: 'event-1',
          categoryId: 1,
          category: 'Absoluto',
          eventName: 'Absoluto - Masculino',
          eventGender: 'MALE',
          firstName: 'Carlos',
          lastName: 'Lopez',
          gender: 'MALE'
        }
      ]
    });
  });

  it('should post a manual inscription to the dedicated endpoint', () => {
    const payload: ManualEventInscriptionRequest = {
      playerSource: 'MANUAL',
      firstName: 'Lucia',
      lastName: 'Perez',
      gender: 'FEMALE',
      birthDate: '1998-03-05',
      nationality: 'ESP',
      tennisId: 'LIC-77'
    };

    service.addManualInscription('tournament-id', 'event-1', payload).subscribe(response => {
      expect(response.id).toBe('inscription-created');
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/tournaments/tournament-id/events/event-1/manual-inscriptions`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush({
      id: 'inscription-created',
      tournamentId: 'tournament-id',
      eventId: 'event-1',
      categoryId: 1,
      memberId: 'person-2',
      participantId: 'participant-id',
      partnerId: null,
      status: 'PENDING',
      paymentStatus: 'UNPAID',
      registeredAt: '2026-04-29T00:00:00Z'
    });
  });

  it('should post a match result to the tournament-scoped endpoint', () => {
    const payload = {
      winnerId: 'winner-id',
      scoreString: '6-4 6-3'
    };

    service.submitMatchResult('tournament-id', 'match-id', payload).subscribe(response => {
      expect(response.id).toBe('match-id');
      expect(response.winnerId).toBe('winner-id');
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/tournaments/tournament-id/matches/match-id/result`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush({
      id: 'match-id',
      firstInscriptionId: 'winner-id',
      secondInscriptionId: 'loser-id',
      winnerId: 'winner-id',
      roundNumber: 1,
      scheduledAt: null,
      scheduleTimeType: null,
      courtId: null,
      court: null,
      result: '6-4 6-3'
    });
  });

  it('should patch a match schedule to the tournament-scoped endpoint', () => {
    const payload = {
      courtId: 'court-id',
      scheduledAt: '2026-05-02T10:00',
      scheduleTimeType: 'NOT_BEFORE' as const
    };

    service.scheduleMatch('tournament-id', 'match-id', payload).subscribe(response => {
      expect(response.id).toBe('match-id');
      expect(response.courtId).toBe('court-id');
      expect(response.scheduleTimeType).toBe('NOT_BEFORE');
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/tournaments/tournament-id/matches/match-id/schedule`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual(payload);

    request.flush({
      id: 'match-id',
      firstInscriptionId: 'player-1',
      secondInscriptionId: 'player-2',
      winnerId: null,
      roundNumber: 1,
      scheduledAt: '2026-05-02T10:00:00',
      scheduleTimeType: 'NOT_BEFORE',
      courtId: 'court-id',
      court: 'Pista 1',
      result: null
    });
  });
});
