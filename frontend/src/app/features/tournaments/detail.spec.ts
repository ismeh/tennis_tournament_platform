import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { PersonService } from '../../data/services/person.service';
import { MemberService } from '../../data/services/member.service';
import { TournamentService } from '../../data/services/tournament.service';
import { TournamentDetailComponent } from './detail';

describe('TournamentDetailComponent', () => {
  let fixture: ComponentFixture<TournamentDetailComponent>;
  let component: TournamentDetailComponent;
  let tournamentServiceSpy: jasmine.SpyObj<TournamentService>;
  let memberServiceSpy: jasmine.SpyObj<MemberService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let personServiceSpy: jasmine.SpyObj<PersonService>;

  beforeEach(async () => {
    tournamentServiceSpy = jasmine.createSpyObj<TournamentService>('TournamentService', [
      'getTournamentById',
      'getEventCatalog',
      'saveTournamentEvents',
      'requestInscription',
      'addManualInscription',
      'updateTournamentStatus',
      'getTournamentInscriptions'
    ]);
    memberServiceSpy = jasmine.createSpyObj<MemberService>('MemberService', ['getMemberByEmail', 'getMyProfile']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUserEmail']);
    personServiceSpy = jasmine.createSpyObj<PersonService>('PersonService', ['searchPersons']);

    tournamentServiceSpy.getEventCatalog.and.returnValue(of([
      {
        id: 1,
        category: 'Absoluto Individual Masculino',
        description: 'Individual masculino open'
      },
      {
        id: 2,
        category: 'Absoluto Dobles Mixto',
        description: 'Dobles mixto open'
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
    tournamentServiceSpy.saveTournamentEvents.and.returnValue(of(void 0));
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
      registeredAt: '2025-01-01T00:00:00Z',
      personId: 'person-id',
      firstName: 'Organizer',
      lastName: 'User',
      gender: 'MALE',
      birthDate: '1990-01-01',
      nationality: 'ESP',
      federationLicense: 'LIC-01'
    }));

    await TestBed.configureTestingModule({
      imports: [TournamentDetailComponent],
      providers: [
        { provide: TournamentService, useValue: tournamentServiceSpy },
        { provide: MemberService, useValue: memberServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: PersonService, useValue: personServiceSpy },
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
    expect(component.tournament()?.id).toBe('tournament-id');
    expect(component.selectedEvents().length).toBe(1);
    expect(component.selectedEvents()[0].genders).toEqual(['MALE', 'MIXED']);
    expect(component.isLoading()).toBeFalse();
    expect(component.isLoadingEvents()).toBeFalse();
    expect(tournamentServiceSpy.getTournamentInscriptions).toHaveBeenCalledWith('tournament-id', undefined);
  });

  it('should add and remove events using checkbox toggle', () => {
    component.toggleCatalogEvent(
      { id: 1, category: 'Absoluto Individual Masculino', description: 'Individual masculino open' },
      true
    );

    expect(component.selectedEvents().length).toBe(1);
    expect(component.getEventLabelById(1)).toBe('Absoluto Individual Masculino');

    component.toggleCatalogEvent(
      { id: 1, category: 'Absoluto Individual Masculino', description: 'Individual masculino open' },
      false
    );

    expect(component.selectedEvents().length).toBe(0);
  });

  it('should save selected tournament events', () => {
    component.clearSelectedEvents();
    component.toggleCatalogEvent(
      { id: 2, category: 'Absoluto Dobles Mixto', description: 'Dobles mixto open' },
      true
    );
    component.toggleEventGender(2, 'MIXED', true);

    component.saveTournamentEvents();

    expect(tournamentServiceSpy.saveTournamentEvents).toHaveBeenCalledWith('tournament-id', {
      events: [
        {
          categoryId: 2,
          gender: 'MIXED',
          stages: ['SINGLE_ELIMINATION']
        }
      ]
    });
    expect(component.eventsSuccessMessage()).toContain('guardados correctamente');
  });

  it('should show validation error when saving with no selected events', () => {
    component.clearSelectedEvents();
    component.saveTournamentEvents();

    expect(tournamentServiceSpy.saveTournamentEvents).not.toHaveBeenCalled();
    expect(component.eventsErrorMessage()).toContain('Selecciona al menos un evento');
  });

  it('should require at least one gender per selected event before saving', () => {
    component.clearSelectedEvents();
    component.toggleCatalogEvent(
      { id: 1, category: 'Absoluto Individual Masculino', description: 'Individual masculino open' },
      true
    );

    component.saveTournamentEvents();

    expect(tournamentServiceSpy.saveTournamentEvents).not.toHaveBeenCalled();
    expect(component.eventsErrorMessage()).toContain('al menos un género en cada evento');
  });

  it('should update tournament status when a valid transition is selected', () => {
    component.onSelectedStatusChange('OPEN');

    component.updateTournamentStatus();

    expect(tournamentServiceSpy.updateTournamentStatus).toHaveBeenCalledWith('tournament-id', { status: 'OPEN' });
    expect(component.tournament()?.status).toBe('OPEN');
    expect(component.actionMessage()).toContain('Estado del torneo actualizado');
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

  it('should render the registered players section and apply the event filter', () => {
    component.setActiveSection('registeredPlayers');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Jugadores inscritos');
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

  it('should debounce the existing-player search while typing', fakeAsync(() => {
    component.onManualPlayerSourceChange('EXISTING_PERSON');

    component.onManualPlayerSearchQueryChange('Ro');
    tick(299);

    expect(personServiceSpy.searchPersons).not.toHaveBeenCalled();

    tick(1);

    expect(personServiceSpy.searchPersons).toHaveBeenCalledWith('Ro');
    expect(component.manualPlayerSearchResults().length).toBe(1);
  }));
});
