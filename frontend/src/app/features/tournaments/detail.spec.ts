import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of, Subject } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { PersonService } from '../../data/services/person.service';
import { ProPlayerService } from '../../data/services/pro-player.service';
import { MemberService } from '../../data/services/member.service';
import { TournamentUpdateEvent } from '../../data/interfaces/tournament.model';
import { TournamentLiveUpdatesService } from '../../data/services/tournament-live-updates.service';
import { TournamentService } from '../../data/services/tournament.service';
import { ReferenceDataService } from '../../data/services/reference-data.service';
import { ClubService } from '../../data/services/club.service';
import { TournamentDetailComponent } from './detail';

describe('TournamentDetailComponent', () => {
  let fixture: ComponentFixture<TournamentDetailComponent>;
  let component: TournamentDetailComponent;
  let tournamentServiceSpy: jasmine.SpyObj<TournamentService>;
  let memberServiceSpy: jasmine.SpyObj<MemberService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let personServiceSpy: jasmine.SpyObj<PersonService>;
  let proPlayerServiceSpy: jasmine.SpyObj<ProPlayerService>;
  let referenceDataServiceSpy: jasmine.SpyObj<ReferenceDataService>;
  let clubServiceSpy: jasmine.SpyObj<ClubService>;
  let tournamentLiveUpdatesServiceSpy: jasmine.SpyObj<TournamentLiveUpdatesService>;
  let liveUpdatesSubject: Subject<TournamentUpdateEvent>;

  beforeEach(async () => {
    tournamentServiceSpy = jasmine.createSpyObj<TournamentService>('TournamentService', [
      'getTournamentById',
      'getEventCatalog',
      'getEventCatalogAll',
      'saveTournamentEvents',
      'requestInscription',
      'addManualInscription',
      'updateTournamentStatus',
      'getTournamentInscriptions',
      'getCourts',
      'createCourt',
      'updateCourt',
      'deleteCourt',
      'submitMatchResult',
      'scheduleMatch',
      'getScheduleConfig',
      'getTournamentUmpires',
      'searchUmpires',
      'addTournamentUmpire',
      'removeTournamentUmpire',
      'updateTournamentGeneralInfo',
      'exportTournamentPdf',
      'updateParticipantsPoints',
      'generateDraws',
      'saveScheduleConfig'
    ]);
    memberServiceSpy = jasmine.createSpyObj<MemberService>('MemberService', ['getMemberByEmail', 'getMyProfile']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUserEmail']);
    personServiceSpy = jasmine.createSpyObj<PersonService>('PersonService', ['searchPersons']);
    proPlayerServiceSpy = jasmine.createSpyObj<ProPlayerService>('ProPlayerService', ['searchProPlayers']);
    referenceDataServiceSpy = jasmine.createSpyObj<ReferenceDataService>('ReferenceDataService', ['getNationalities']);
    clubServiceSpy = jasmine.createSpyObj<ClubService>('ClubService', ['searchClubs']);
    clubServiceSpy.searchClubs.and.returnValue(of([]));
    tournamentLiveUpdatesServiceSpy = jasmine.createSpyObj<TournamentLiveUpdatesService>('TournamentLiveUpdatesService', ['watchTournament']);
    liveUpdatesSubject = new Subject<TournamentUpdateEvent>();
    tournamentLiveUpdatesServiceSpy.watchTournament.and.returnValue(liveUpdatesSubject.asObservable());

    tournamentServiceSpy.getEventCatalog.and.returnValue(of([
      {
        id: 1,
        category: 'Absoluto Individual Masculino',
        description: 'Individual masculino open',
        custom: false
      },
      {
        id: 2,
        category: 'Absoluto Dobles Mixto',
        description: 'Dobles mixto open',
        custom: false
      }
    ]));
    tournamentServiceSpy.getEventCatalogAll.and.returnValue(of([
      {
        id: 1,
        category: 'Absoluto Individual Masculino',
        description: 'Individual masculino open',
        custom: false
      },
      {
        id: 2,
        category: 'Absoluto Dobles Mixto',
        description: 'Dobles mixto open',
        custom: false
      }
    ]));
    tournamentServiceSpy.getScheduleConfig.and.returnValue(of({
      id: null,
      tournamentId: 'tournament-id',
      matchDurationMinutes: 60,
      timeSlots: []
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
    tournamentServiceSpy.getTournamentById.and.returnValue(of({
      id: 'tournament-id',
      formalName: 'Open de Primavera',
      playStartDate: '2026-05-01',
      playEndDate: '2026-05-10',
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
    }));
    tournamentServiceSpy.saveTournamentEvents.and.returnValue(of({
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
    }));
    tournamentServiceSpy.getCourts.and.returnValue(of([]));
    tournamentServiceSpy.createCourt.and.returnValue(of({
      id: 'court-id',
      tournamentId: 'tournament-id',
      name: 'Pista 1',
      active: true
    }));
    tournamentServiceSpy.updateCourt.and.returnValue(of({
      id: 'court-id',
      tournamentId: 'tournament-id',
      name: 'Central',
      active: true
    }));
    tournamentServiceSpy.deleteCourt.and.returnValue(of(void 0));
    tournamentServiceSpy.addManualInscription.and.returnValue(of({
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
    }));
    tournamentServiceSpy.getTournamentInscriptions.and.returnValue(of({
      tournamentId: 'tournament-id',
      selectedEventId: null,
      events: [
        {
          eventId: 'event-1',
          categoryId: 1,
          category: 'Absoluto',
          eventName: 'Absoluto - Masculino',
          eventGender: 'MALE'
        },
        {
          eventId: 'event-2',
          categoryId: 1,
          category: 'Absoluto',
          eventName: 'Absoluto - Mixto',
          eventGender: 'MIXED'
        }
      ],
      categoryCounts: [
        {
          categoryId: 1,
          category: 'Absoluto',
          totalPlayers: 3,
          genders: [
            {
              gender: 'MALE',
              totalPlayers: 2
            },
            {
              gender: 'FEMALE',
              totalPlayers: 1
            }
          ]
        }
      ],
      inscriptions: [
        {
          inscriptionId: 'inscription-1',
          participantId: 'participant-1',
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
    }));
    tournamentServiceSpy.updateTournamentStatus.and.returnValue(of({
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
    }));

    tournamentServiceSpy.getTournamentUmpires.and.returnValue(of([]));
    tournamentServiceSpy.searchUmpires.and.returnValue(of([]));
    tournamentServiceSpy.addTournamentUmpire.and.returnValue(of({
      id: 'umpire-1',
      tournamentId: 'tournament-id',
      umpireId: 'umpire-member-id',
      umpireFirstName: 'Test',
      umpireLastName: 'Umpire',
      umpireEmail: 'umpire@test.com',
      assignedAt: '2026-04-01T00:00:00Z'
    }));
    tournamentServiceSpy.removeTournamentUmpire.and.returnValue(of(void 0));
    tournamentServiceSpy.updateTournamentGeneralInfo.and.returnValue(of({
      id: 'tournament-id',
      formalName: 'Open de Primavera',
      status: 'DRAFT',
      playStartDate: '2026-05-01',
      playEndDate: '2026-05-10',
      inscriptionStartDate: '2026-04-01',
      inscriptionEndDate: '2026-04-20',
      surfaceCategory: 'CLAY',
      maxPlayers: 32,
      location: 'Club Central',
      providerOrganisationId: 'member-id',
      events: []
    }));
    tournamentServiceSpy.exportTournamentPdf.and.returnValue(of(new Blob()));
    tournamentServiceSpy.updateParticipantsPoints.and.returnValue(of(void 0));
    tournamentServiceSpy.generateDraws.and.returnValue(of({
      id: 'tournament-id',
      formalName: 'Open de Primavera',
      status: 'DRAFT',
      playStartDate: '2026-05-01',
      playEndDate: '2026-05-10',
      inscriptionStartDate: '2026-04-01',
      inscriptionEndDate: '2026-04-20',
      surfaceCategory: 'CLAY',
      maxPlayers: 32,
      location: 'Club Central',
      providerOrganisationId: 'member-id',
      events: []
    }));
    tournamentServiceSpy.saveScheduleConfig.and.returnValue(of({
      id: null,
      tournamentId: 'tournament-id',
      matchDurationMinutes: 60,
      timeSlots: []
    }));

    authServiceSpy.getCurrentUserEmail.and.returnValue('organizer@example.com');
    personServiceSpy.searchPersons.and.returnValue(of([
      {
        id: 'person-2',
        tennisId: 'LIC-02',
        firstName: 'Roger',
        lastName: 'Federer',
        nationality: 'SUI',
        birthDate: '1981-08-08',
        gender: 'MALE'
      }
    ]));
    proPlayerServiceSpy.searchProPlayers.and.returnValue(of([
      {
        id: 42,
        license: 'RFET-42',
        fullName: 'ALCARAZ, CARLOS',
        firstName: 'CARLOS',
        lastName: 'ALCARAZ',
        rankingPosition: 1,
        ageCategory: 'Absoluta',
        clubName: 'Club Central',
        birthDate: '2003-05-05',
        gender: 'MALE'
      }
    ]));
    memberServiceSpy.getMemberByEmail.and.returnValue(of({
      id: 'member-id',
      email: 'organizer@example.com',
      username: 'organizer',
      gender: 'MALE',
      tier: 'PRO',
      registeredAt: '2025-01-01T00:00:00Z'
    }));
    memberServiceSpy.getMyProfile.and.returnValue(of({
      memberId: 'member-id',
      email: 'organizer@example.com',
      tier: 'ADVANCED',
      role: 'ORGANIZER',
      registeredAt: '2025-01-01T00:00:00Z',
      personId: 'person-id',
      firstName: 'Organizer',
      lastName: 'User',
      gender: 'MALE',
      birthDate: '1990-01-01',
      nationality: 'ESP',
      federationLicense: 'LIC-01',
      clubId: null,
      clubName: null
    }));

    await TestBed.configureTestingModule({
      imports: [TournamentDetailComponent],
      providers: [
        { provide: TournamentService, useValue: tournamentServiceSpy },
        { provide: MemberService, useValue: memberServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: PersonService, useValue: personServiceSpy },
        { provide: ProPlayerService, useValue: proPlayerServiceSpy },
        { provide: ReferenceDataService, useValue: referenceDataServiceSpy },
        { provide: ClubService, useValue: clubServiceSpy },
        { provide: TournamentLiveUpdatesService, useValue: tournamentLiveUpdatesServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => 'tournament-id'
              }
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TournamentDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load event catalog and tournament on init', () => {
    expect(component.eventCatalog().length).toBe(2);
    expect(component.nationalities().map(nationality => nationality.code)).toEqual(['ESP', 'SUI']);
    expect(component.tournament()?.id).toBe('tournament-id');
    expect(component.selectedEvents().length).toBe(2);
    expect(component.selectedEvents()[0].genders).toEqual(['MALE']);
    expect(component.selectedEvents()[1].genders).toEqual(['MIXED']);
    expect(component.isLoading()).toBeFalse();
    expect(component.isLoadingEvents()).toBeFalse();
    expect(tournamentServiceSpy.getTournamentInscriptions).toHaveBeenCalledWith('tournament-id', undefined);
    expect(tournamentLiveUpdatesServiceSpy.watchTournament).toHaveBeenCalledWith('tournament-id');
  });

  it('should refresh tournament detail when a live match update arrives', () => {
    const initialLoadCount = tournamentServiceSpy.getTournamentById.calls.count();
    tournamentServiceSpy.getTournamentById.and.returnValue(of({
      id: 'tournament-id',
      formalName: 'Open de Primavera Actualizado',
      playStartDate: '2026-05-01',
      playEndDate: '2026-05-10',
      inscriptionStartDate: '2026-04-01',
      inscriptionEndDate: '2026-04-20',
      surfaceCategory: 'CLAY',
      maxPlayers: 32,
      location: 'Club Central',
      status: 'DRAFT',
      providerOrganisationId: 'member-id',
      events: []
    }));

    liveUpdatesSubject.next({
      type: 'MATCH_RESULT_UPDATED',
      tournamentId: 'tournament-id',
      matchId: 'match-1',
      occurredAt: '2026-06-11T10:00:00'
    });

    expect(tournamentServiceSpy.getTournamentById.calls.count()).toBe(initialLoadCount + 1);
    expect(component.tournament()?.formalName).toBe('Open de Primavera Actualizado');
    expect(component.actionMessage()).toBe('Cuadro actualizado');
  });

  it('should keep the optimistic result without reloading the tournament after saving a match result', () => {
    tournamentServiceSpy.submitMatchResult.and.returnValue(of({
      id: 'match-1',
      firstInscriptionId: 'inscription-1',
      secondInscriptionId: 'inscription-2',
      winnerId: 'inscription-2',
      roundNumber: 1,
      bracketPosition: 0,
      scheduledAt: null,
      scheduleTimeType: null,
      courtId: null,
      court: null,
      result: '7-5 6-4'
    }));
    component.tournament.set({
      ...component.tournament()!,
      events: [
        {
          eventId: 'event-1',
          categoryId: 1,
          gender: 'MALE',
          stages: [
            {
              id: 'stage-1',
              eventId: 'event-1',
              stageType: 'SINGLE_ELIMINATION',
              order: 1,
              description: 'Principal',
              strategyName: null,
              draws: [
                {
                  id: 'draw-1',
                  stageId: 'stage-1',
                  drawType: 'ELIMINATION',
                  label: 'Principal',
                  matches: [
                    {
                      id: 'match-1',
                      firstInscriptionId: 'inscription-1',
                      secondInscriptionId: 'inscription-2',
                      winnerId: 'inscription-1',
                      roundNumber: 1,
                      bracketPosition: 0,
                      scheduledAt: null,
                      scheduleTimeType: null,
                      courtId: null,
                      court: null,
                      result: '6-4 6-4',
                      professionalMatch: true,
                      firstWinPoints: 30,
                      secondWinPoints: 20
                    },
                    {
                      id: 'match-2',
                      firstInscriptionId: 'inscription-3',
                      secondInscriptionId: 'inscription-4',
                      winnerId: 'inscription-3',
                      roundNumber: 1,
                      bracketPosition: 1,
                      scheduledAt: null,
                      scheduleTimeType: null,
                      courtId: null,
                      court: null,
                      result: '6-2 6-2',
                      professionalMatch: true,
                      firstWinPoints: 15,
                      secondWinPoints: 40
                    },
                    {
                      id: 'match-3',
                      firstInscriptionId: 'inscription-1',
                      secondInscriptionId: 'inscription-3',
                      winnerId: null,
                      roundNumber: 2,
                      bracketPosition: 0,
                      scheduledAt: null,
                      scheduleTimeType: null,
                      courtId: null,
                      court: null,
                      result: null,
                      professionalMatch: true,
                      firstWinPoints: 40,
                      secondWinPoints: 20
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    });

    const initialTournamentLoadCount = tournamentServiceSpy.getTournamentById.calls.count();

    component.onMatchResultSaved({ matchId: 'match-1', winnerId: 'inscription-2', result: '7-5 6-4', status: 'COMPLETED' });

    expect(tournamentServiceSpy.submitMatchResult).toHaveBeenCalledWith('tournament-id', 'match-1', {
      winnerId: 'inscription-2',
      scoreString: '7-5 6-4',
      sets: undefined,
      notes: undefined,
      firstPlayerPoints: undefined,
      secondPlayerPoints: undefined,
      status: 'COMPLETED'
    });
    expect(tournamentServiceSpy.getTournamentById.calls.count()).toBe(initialTournamentLoadCount);
    const nextMatch = component.tournament()!.events![0].stages![0].draws![0].matches!.find(match => match.id === 'match-3')!;
    expect(nextMatch.firstInscriptionId).toBe('inscription-2');
    expect(nextMatch.firstWinPoints).toBe(40);
    expect(nextMatch.secondWinPoints).toBe(30);
    expect(component.actionMessage()).toContain('Resultado guardado');
  });

  it('should optimistically advance the winner and roll back when saving a result fails', () => {
    const saveResultResponse = new Subject<any>();
    tournamentServiceSpy.submitMatchResult.and.returnValue(saveResultResponse.asObservable());
    component.tournament.set({
      ...component.tournament()!,
      events: [
        {
          eventId: 'event-1',
          categoryId: 1,
          gender: 'MALE',
          stages: [
            {
              id: 'stage-1',
              eventId: 'event-1',
              stageType: 'SINGLE_ELIMINATION',
              order: 1,
              description: 'Principal',
              strategyName: null,
              draws: [
                {
                  id: 'draw-1',
                  stageId: 'stage-1',
                  drawType: 'ELIMINATION',
                  label: 'Principal',
                  matches: [
                    {
                      id: 'match-1',
                      firstInscriptionId: 'inscription-1',
                      secondInscriptionId: 'inscription-2',
                      winnerId: null,
                      roundNumber: 1,
                      bracketPosition: 0,
                      scheduledAt: null,
                      scheduleTimeType: null,
                      courtId: null,
                      court: null,
                      result: null
                    },
                    {
                      id: 'match-3',
                      firstInscriptionId: null,
                      secondInscriptionId: null,
                      winnerId: null,
                      roundNumber: 2,
                      bracketPosition: 0,
                      scheduledAt: null,
                      scheduleTimeType: null,
                      courtId: null,
                      court: null,
                      result: null
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    });

    component.onMatchResultSaved({ matchId: 'match-1', winnerId: 'inscription-2', result: '7-5 6-4', status: 'COMPLETED' });

    const optimisticMatches = component.tournament()!.events![0].stages![0].draws![0].matches!;
    expect(optimisticMatches.find(match => match.id === 'match-1')!.winnerId).toBe('inscription-2');
    expect(optimisticMatches.find(match => match.id === 'match-3')!.firstInscriptionId).toBe('inscription-2');

    saveResultResponse.error(new Error('save failed'));

    const rolledBackMatches = component.tournament()!.events![0].stages![0].draws![0].matches!;
    expect(rolledBackMatches.find(match => match.id === 'match-1')!.winnerId).toBeNull();
    expect(rolledBackMatches.find(match => match.id === 'match-3')!.firstInscriptionId).toBeNull();
    expect(component.actionError()).toBeTruthy();
    expect(component.savingResultMatchId()).toBeNull();
  });

  it('should filter and sort match schedule rows', () => {
    component.courts.set([
      {
        id: 'court-1',
        tournamentId: 'tournament-id',
        name: 'Pista 1',
        active: true
      },
      {
        id: 'court-2',
        tournamentId: 'tournament-id',
        name: 'Pista 2',
        active: true
      }
    ]);
    component.tournament.set({
      ...component.tournament()!,
      events: [
        {
          eventId: 'event-1',
          categoryId: 1,
          gender: 'MALE',
          stages: [
            {
              id: 'stage-1',
              eventId: 'event-1',
              stageType: 'SINGLE_ELIMINATION',
              order: 1,
              description: 'Principal',
              strategyName: null,
              draws: [
                {
                  id: 'draw-1',
                  stageId: 'stage-1',
                  drawType: 'ELIMINATION',
                  label: 'Principal',
                  matches: [
                    {
                      id: 'match-1',
                      firstInscriptionId: 'inscription-1',
                      secondInscriptionId: 'inscription-2',
                      winnerId: null,
                      roundNumber: 2,
                      scheduledAt: '2026-05-03T10:00:00',
                      scheduleTimeType: 'EXACT',
                      courtId: 'court-2',
                      court: 'Pista 2',
                      result: null
                    },
                    {
                      id: 'match-2',
                      firstInscriptionId: 'inscription-3',
                      secondInscriptionId: 'inscription-4',
                      winnerId: null,
                      roundNumber: 1,
                      scheduledAt: '2026-05-01T09:00:00',
                      scheduleTimeType: 'EXACT',
                      courtId: 'court-1',
                      court: 'Pista 1',
                      result: null
                    }
                  ]
                }
              ]
            }
          ]
        },
        {
          eventId: 'event-2',
          categoryId: 2,
          gender: 'MIXED',
          stages: [
            {
              id: 'stage-2',
              eventId: 'event-2',
              stageType: 'SINGLE_ELIMINATION',
              order: 1,
              description: 'Principal',
              strategyName: null,
              draws: [
                {
                  id: 'draw-2',
                  stageId: 'stage-2',
                  drawType: 'ELIMINATION',
                  label: 'Principal',
                  matches: [
                    {
                      id: 'match-3',
                      firstInscriptionId: 'inscription-5',
                      secondInscriptionId: 'inscription-6',
                      winnerId: null,
                      roundNumber: 1,
                      scheduledAt: '2026-05-02T09:00:00',
                      scheduleTimeType: 'EXACT',
                      courtId: 'court-1',
                      court: 'Pista 1',
                      result: null
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    });

    expect(component.filteredTournamentMatchScheduleRows().map(row => row.match.id)).toEqual(['match-2', 'match-3', 'match-1']);

    component.matchScheduleEventFilter.set('Absoluto Individual Masculino - Masculino');
    expect(component.filteredTournamentMatchScheduleRows().map(row => row.match.id)).toEqual(['match-2', 'match-1']);

    component.matchScheduleRoundFilter.set('1');
    expect(component.filteredTournamentMatchScheduleRows().map(row => row.match.id)).toEqual(['match-2']);

    component.matchScheduleDateFilter.set('2026-05-01');
    component.matchScheduleCourtFilter.set('court-1');
    expect(component.filteredTournamentMatchScheduleRows().map(row => row.match.id)).toEqual(['match-2']);

    component.clearMatchScheduleFilters();
    component.setMatchScheduleSort('scheduledAt');
    expect(component.filteredTournamentMatchScheduleRows().map(row => row.match.id)).toEqual(['match-1', 'match-3', 'match-2']);

    const match1 = component.filteredTournamentMatchScheduleRows().find(row => row.match.id === 'match-1')!.match;
    component.updateMatchScheduleDate(match1, '2026-04-30T08:00');
    component.updateMatchScheduleCourt(match1, 'court-1');
    expect(component.filteredTournamentMatchScheduleRows().map(row => row.match.id)).toEqual(['match-1', 'match-3', 'match-2']);
    expect(component.getMatchScheduleDraft(match1).scheduledAt).toBe('2026-04-30T08:00');

    component.matchScheduleCourtFilter.set('court-1');
    expect(component.filteredTournamentMatchScheduleRows().map(row => row.match.id)).toEqual(['match-3', 'match-2']);

    component.matchScheduleCourtFilter.set('');
    component.setMatchScheduleSort('scheduledAt');
    expect(component.filteredTournamentMatchScheduleRows().map(row => row.match.id)).toEqual(['match-2', 'match-3', 'match-1']);
  });

  it('should add and remove events using add/remove buttons', () => {
    const initialCount = component.selectedEvents().length;
    component.addCatalogCategory(
      { id: 1, category: 'Absoluto Individual Masculino', description: 'Individual masculino open', custom: false }
    );

    expect(component.selectedEvents().length).toBe(initialCount + 1);
    expect(component.getEventLabelById(1)).toBe('Absoluto Individual Masculino');

    const uniqueId = component.selectedEvents()[initialCount].uniqueId!;
    component.removeSelectedEvent(uniqueId);

    expect(component.selectedEvents().length).toBe(initialCount);
  });

  it('should save selected tournament events', () => {
    tournamentServiceSpy.saveTournamentEvents.and.returnValue(of({
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
          eventId: 'event-3',
          categoryId: 2,
          gender: 'MIXED'
        }
      ]
    }));
    component.selectedEvents.set([]);
    component.addCatalogCategory(
      { id: 2, category: 'Absoluto Dobles Mixto', description: 'Dobles mixto open', custom: false }
    );
    const uniqueId = component.selectedEvents()[0].uniqueId!;
    component.changeEventGender(uniqueId, 'MIXED');

    component.saveTournamentEvents();

    expect(tournamentServiceSpy.saveTournamentEvents).toHaveBeenCalledWith('tournament-id', {
      events: [
        {
          id: null,
          categoryId: 2,
          gender: 'MIXED',
          stages: ['SINGLE_ELIMINATION']
        }
      ]
    });
    expect(component.eventsSuccessMessage()).toContain('guardadas correctamente');
    expect(component.tournament()?.events?.[0].eventId).toBe('event-3');
    expect(component.selectedInscriptionCategoryId()).toBe(2);
    expect(tournamentServiceSpy.getTournamentInscriptions).toHaveBeenCalledTimes(2);
  });

  it('should update eventsByGender when changing gender', () => {
    component.selectedEvents.set([]);
    component.addCatalogCategory(
      { id: 1, category: 'Absoluto Individual Masculino', description: 'Individual masculino open', custom: false }
    );

    const event = () => component.selectedEvents()[0];
    const uniqueId = event().uniqueId!;
    expect(event().eventsByGender).toEqual([{ gender: 'MALE', eventId: null }]);

    component.changeEventGender(uniqueId, 'FEMALE');
    expect(event().eventsByGender).toEqual([
      { gender: 'MALE', eventId: null },
      { gender: 'FEMALE', eventId: null }
    ]);

    component.changeEventGender(uniqueId, 'MIXED');
    expect(event().eventsByGender).toEqual([
      { gender: 'MALE', eventId: null },
      { gender: 'FEMALE', eventId: null },
      { gender: 'MIXED', eventId: null }
    ]);
  });

  it('should preserve eventId in eventsByGender after hydration when changing genders', () => {
    component.selectedEvents.set([]);
    component.addCatalogCategory(
      { id: 1, category: 'Absoluto Individual Masculino', description: 'Individual masculino open', custom: false }
    );
    const uniqueId = component.selectedEvents()[0].uniqueId!;

    component.selectedEvents.update(events =>
      events.map(event =>
        event.uniqueId === uniqueId
          ? {
              ...event,
              eventsByGender: [
                { gender: 'MALE', eventId: 'saved-male-id' },
                { gender: 'FEMALE', eventId: 'saved-female-id' }
              ]
            }
          : event
      )
    );

    component.changeEventGender(uniqueId, 'FEMALE');
    const eventAfterChange = component.selectedEvents()[0];
    expect(eventAfterChange.genders).toEqual(['FEMALE']);
    expect(eventAfterChange.eventsByGender).toEqual([
      { gender: 'MALE', eventId: 'saved-male-id' },
      { gender: 'FEMALE', eventId: 'saved-female-id' }
    ]);

    component.changeEventGender(uniqueId, 'MALE');
    const eventAfterReChange = component.selectedEvents()[0];
    expect(eventAfterReChange.genders).toEqual(['MALE']);
    expect(eventAfterReChange.eventsByGender).toEqual([
      { gender: 'MALE', eventId: 'saved-male-id' },
      { gender: 'FEMALE', eventId: 'saved-female-id' }
    ]);
  });

  it('should save an empty events configuration when all categories are removed', () => {
    tournamentServiceSpy.saveTournamentEvents.and.returnValue(of({
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
      events: []
    }));

    component.selectedEvents.set([]);
    component.saveTournamentEvents();

    expect(tournamentServiceSpy.saveTournamentEvents).toHaveBeenCalledWith('tournament-id', { events: [] });
    expect(component.eventsSuccessMessage()).toContain('guardadas correctamente');
    expect(component.eventsErrorMessage()).toBeNull();
  });

  it('should require at least one stage per selected event before saving', () => {
    component.selectedEvents.set([]);
    component.addCatalogCategory(
      { id: 1, category: 'Absoluto Individual Masculino', description: 'Individual masculino open', custom: false }
    );

    // Clear stages
    component.selectedEvents.update(events =>
      events.map(event => ({ ...event, stages: [] }))
    );

    component.saveTournamentEvents();

    expect(tournamentServiceSpy.saveTournamentEvents).not.toHaveBeenCalled();
    expect(component.eventsErrorMessage()).toContain('Debes definir al menos un cuadro');
  });

  it('should detect creator when backend sends provider organisation as object', () => {
    tournamentServiceSpy.getTournamentById.and.returnValue(of({
      id: 'tournament-id',
      formalName: 'Open de Primavera',
      playStartDate: '2026-05-01',
      playEndDate: '2026-05-10',
      inscriptionStartDate: '2026-04-01',
      inscriptionEndDate: '2026-04-20',
      surfaceCategory: 'CLAY',
      maxPlayers: 32,
      location: 'Club Central',
      status: 'DRAFT',
      providerOrganisationId: {
        id: 'member-id'
      },
      events: []
    }));

    component.ngOnInit();

    expect(component.isCreator()).toBeTrue();
  });

  it('should update draws and matches when draws are regenerated and they were already generated, even if the backend returns duplicate stages', () => {
    // Initial tournament state with an old match
    const initialTournament = {
      id: 'tournament-id',
      formalName: 'Open de Primavera',
      status: 'OPEN',
      playStartDate: '2026-05-01',
      playEndDate: '2026-05-10',
      inscriptionStartDate: '2026-04-01',
      inscriptionEndDate: '2026-04-20',
      surfaceCategory: 'CLAY',
      maxPlayers: 32,
      location: 'Club Central',
      providerOrganisationId: 'member-id',
      events: [
        {
          eventId: 'event-1',
          categoryId: 1,
          gender: 'MALE',
          stages: [
            {
              id: 'stage-1',
              eventId: 'event-1',
              stageType: 'SINGLE_ELIMINATION',
              order: 1,
              description: 'Principal',
              strategyName: null,
              draws: [
                {
                  id: 'draw-1',
                  stageId: 'stage-1',
                  drawType: 'ELIMINATION',
                  label: 'Principal',
                  matches: [
                    {
                      id: 'match-1',
                      firstInscriptionId: 'inscription-1',
                      secondInscriptionId: 'inscription-2',
                      winnerId: 'inscription-1',
                      roundNumber: 1,
                      bracketPosition: 0,
                      scheduledAt: null,
                      scheduleTimeType: null,
                      courtId: null,
                      court: null,
                      result: '6-4 6-4'
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    };
    component.tournament.set(initialTournament as any);

    // Mock response with duplicate stages: Stage 1 (old) and Stage 1 (new)
    const regeneratedTournamentResponseWithDuplicates = {
      ...initialTournament,
      events: [
        {
          eventId: 'event-1',
          categoryId: 1,
          gender: 'MALE',
          stages: [
            // Stage 1 - Copy 1 (old/stale)
            {
              id: 'stage-1',
              eventId: 'event-1',
              stageType: 'SINGLE_ELIMINATION',
              order: 1,
              description: 'Principal',
              strategyName: null,
              draws: [
                {
                  id: 'draw-1',
                  stageId: 'stage-1',
                  drawType: 'ELIMINATION',
                  label: 'Principal',
                  matches: [
                    {
                      id: 'match-1',
                      firstInscriptionId: 'inscription-1',
                      secondInscriptionId: 'inscription-2',
                      winnerId: 'inscription-1',
                      roundNumber: 1,
                      bracketPosition: 0,
                      scheduledAt: null,
                      scheduleTimeType: null,
                      courtId: null,
                      court: null,
                      result: '6-4 6-4'
                    }
                  ]
                }
              ]
            },
            // Stage 1 - Copy 2 (newly regenerated)
            {
              id: 'stage-1',
              eventId: 'event-1',
              stageType: 'SINGLE_ELIMINATION',
              order: 1,
              description: 'Principal',
              strategyName: null,
              draws: [
                {
                  id: 'draw-new-id',
                  stageId: 'stage-1',
                  drawType: 'ELIMINATION',
                  label: 'Principal',
                  matches: [
                    {
                      id: 'match-new-id',
                      firstInscriptionId: 'inscription-1',
                      secondInscriptionId: 'inscription-2',
                      winnerId: null,
                      roundNumber: 1,
                      bracketPosition: 0,
                      scheduledAt: null,
                      scheduleTimeType: null,
                      courtId: null,
                      court: null,
                      result: null
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    };
    tournamentServiceSpy.generateDraws.and.returnValue(of(regeneratedTournamentResponseWithDuplicates as any));

    // Call onGenerateDraws
    component.onGenerateDraws({ tournamentId: 'tournament-id', stageId: 'stage-1' }, 'event-1');

    // Assertions
    const currentTournament = component.tournament();
    expect(currentTournament).toBeTruthy();
    const event = currentTournament?.events?.find(e => e.eventId === 'event-1');
    expect(event).toBeTruthy();
    const stage = event?.stages?.[0];
    expect(stage).toBeTruthy();
    const draw = stage?.draws?.[0];
    expect(draw).toBeTruthy();
    
    // Check that we got the new draw and match!
    expect(draw?.id).toBe('draw-new-id');
    expect(draw?.matches?.[0].id).toBe('match-new-id');
  });

  it('should update draws and matches when draws are regenerated and they were already generated', () => {
    // Initial tournament state with an old match
    const initialTournament = {
      id: 'tournament-id',
      formalName: 'Open de Primavera',
      status: 'OPEN',
      playStartDate: '2026-05-01',
      playEndDate: '2026-05-10',
      inscriptionStartDate: '2026-04-01',
      inscriptionEndDate: '2026-04-20',
      surfaceCategory: 'CLAY',
      maxPlayers: 32,
      location: 'Club Central',
      providerOrganisationId: 'member-id',
      events: [
        {
          eventId: 'event-1',
          categoryId: 1,
          gender: 'MALE',
          stages: [
            {
              id: 'stage-1',
              eventId: 'event-1',
              stageType: 'SINGLE_ELIMINATION',
              order: 1,
              description: 'Principal',
              strategyName: null,
              draws: [
                {
                  id: 'draw-1',
                  stageId: 'stage-1',
                  drawType: 'ELIMINATION',
                  label: 'Principal',
                  matches: [
                    {
                      id: 'match-1',
                      firstInscriptionId: 'inscription-1',
                      secondInscriptionId: 'inscription-2',
                      winnerId: 'inscription-1',
                      roundNumber: 1,
                      bracketPosition: 0,
                      scheduledAt: null,
                      scheduleTimeType: null,
                      courtId: null,
                      court: null,
                      result: '6-4 6-4'
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    };
    component.tournament.set(initialTournament as any);

    // Mock response with a regenerated draw (new draw id and new matches)
    const regeneratedTournamentResponse = {
      ...initialTournament,
      events: [
        {
          eventId: 'event-1',
          categoryId: 1,
          gender: 'MALE',
          stages: [
            {
              id: 'stage-1',
              eventId: 'event-1',
              stageType: 'SINGLE_ELIMINATION',
              order: 1,
              description: 'Principal',
              strategyName: null,
              draws: [
                {
                  id: 'draw-new-id',
                  stageId: 'stage-1',
                  drawType: 'ELIMINATION',
                  label: 'Principal',
                  matches: [
                    {
                      id: 'match-new-id',
                      firstInscriptionId: 'inscription-1',
                      secondInscriptionId: 'inscription-2',
                      winnerId: null,
                      roundNumber: 1,
                      bracketPosition: 0,
                      scheduledAt: null,
                      scheduleTimeType: null,
                      courtId: null,
                      court: null,
                      result: null
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    };
    tournamentServiceSpy.generateDraws.and.returnValue(of(regeneratedTournamentResponse as any));

    // Call onGenerateDraws
    component.onGenerateDraws({ tournamentId: 'tournament-id', stageId: 'stage-1' }, 'event-1');

    // Assertions
    const currentTournament = component.tournament();
    expect(currentTournament).toBeTruthy();
    const event = currentTournament?.events?.find(e => e.eventId === 'event-1');
    expect(event).toBeTruthy();
    const stage = event?.stages?.[0];
    expect(stage).toBeTruthy();
    const draw = stage?.draws?.[0];
    expect(draw).toBeTruthy();
    
    // Check that we got the new draw and match!
    expect(draw?.id).toBe('draw-new-id');
    expect(draw?.matches?.[0].id).toBe('match-new-id');
  });

  it('should render the registered players section and apply the event filter', () => {
    component.setActiveSection('inscriptions');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Jugadores inscritos');
    expect(component.isRegisteredPlayersPanelExpanded()).toBeFalse();
    expect(fixture.nativeElement.textContent).not.toContain('Carlos Lopez');

    component.toggleRegisteredPlayersPanel();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Carlos Lopez');

    tournamentServiceSpy.getTournamentInscriptions.calls.reset();
    component.onTournamentInscriptionEventChange('event-1');

    expect(tournamentServiceSpy.getTournamentInscriptions).toHaveBeenCalledWith('tournament-id', 'event-1');
  });

  it('should keep manual event selection based on tournament events when inscriptions are empty', () => {
    tournamentServiceSpy.getTournamentInscriptions.and.returnValue(of({
      tournamentId: 'tournament-id',
      selectedEventId: null,
      events: [],
      categoryCounts: [],
      inscriptions: []
    }));

    (component as any).loadTournamentInscriptions();

    expect(component.manualPlayerEventId()).toBe('event-1');
    expect(component.manualPlayerEventOptions().length).toBe(2);
  });

  it('should search existing persons and add a manual inscription', () => {
    component.setActiveSection('inscriptions');
    component.onManualPlayerSourceChange('EXISTING_PERSON');
    component.manualPlayerSearchQuery.set('Roger');

    component.searchExistingPersons();

    expect(personServiceSpy.searchPersons).toHaveBeenCalledWith('Roger');
    expect(component.manualPlayerSearchResults().length).toBe(1);

    component.selectExistingPerson(component.manualPlayerSearchResults()[0]);
    component.manualPlayerEventId.set('event-1');
    component.submitManualPlayer();

    expect(tournamentServiceSpy.addManualInscription).toHaveBeenCalledWith('tournament-id', 'event-1', {
      playerSource: 'EXISTING_PERSON',
      personId: 'person-2'
    });
    expect(component.manualPlayerSuccess()).toContain('Jugador añadido');
  });

  it('should search professional players and add a professional inscription', () => {
    component.setActiveSection('inscriptions');
    component.onManualPlayerSourceChange('PROFESSIONAL');
    component.manualPlayerSearchQuery.set('Carlos');

    component.searchManualPlayerCandidates();

    expect(proPlayerServiceSpy.searchProPlayers).toHaveBeenCalledWith('Carlos', { gender: '', category: '' });
    expect(component.manualPlayerSearchResults()[0].id).toBe('42');

    component.selectExistingPerson(component.manualPlayerSearchResults()[0]);
    component.manualPlayerEventId.set('event-1');
    component.submitManualPlayer();

    expect(tournamentServiceSpy.addManualInscription).toHaveBeenCalledWith('tournament-id', 'event-1', {
      playerSource: 'PROFESSIONAL',
      proPlayerId: 42
    });
  });

  it('should debounce the existing-player search while typing', fakeAsync(() => {
    component.onManualPlayerSourceChange('EXISTING_PERSON');

    component.onManualPlayerSearchQueryChange('Ro');
    tick(299);

    expect(personServiceSpy.searchPersons).not.toHaveBeenCalled();

    tick(1);

    expect(personServiceSpy.searchPersons).toHaveBeenCalledWith('Ro');
    expect(component.manualPlayerSearchResults().length).toBe(1);
  }));

  it('should cap predictive player search results at ten entries', () => {
    personServiceSpy.searchPersons.and.returnValue(of(Array.from({ length: 12 }, (_, index) => ({
      id: `person-${index}`,
      tennisId: `LIC-${index}`,
      firstName: `Player ${index}`,
      lastName: 'Test',
      nationality: 'ESP',
      birthDate: '2000-01-01',
      gender: 'MALE'
    }))));

    component.onManualPlayerSourceChange('EXISTING_PERSON');
    component.manualPlayerSearchQuery.set('Player');
    component.searchManualPlayerCandidates();

    expect(component.manualPlayerSearchResults().length).toBe(10);
  });
});
