import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { MemberService } from '../../data/services/member.service';
import { TournamentService } from '../../data/services/tournament.service';
import { TournamentDetailComponent } from './detail';

describe('TournamentDetailComponent', () => {
  let fixture: ComponentFixture<TournamentDetailComponent>;
  let component: TournamentDetailComponent;
  let tournamentServiceSpy: jasmine.SpyObj<TournamentService>;
  let memberServiceSpy: jasmine.SpyObj<MemberService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    tournamentServiceSpy = jasmine.createSpyObj<TournamentService>('TournamentService', [
      'getTournamentById',
      'getEventCatalog',
      'saveTournamentEvents',
      'requestInscription',
      'updateTournamentStatus'
    ]);
    memberServiceSpy = jasmine.createSpyObj<MemberService>('MemberService', ['getMemberByEmail']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUserEmail']);

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
          categoryId: 1,
          gender: 'MALE'
        },
        {
          categoryId: 1,
          gender: 'MIXED'
        }
      ]
    }));
    tournamentServiceSpy.saveTournamentEvents.and.returnValue(of(void 0));
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
          categoryId: 1,
          gender: 'MALE'
        },
        {
          categoryId: 1,
          gender: 'MIXED'
        }
      ]
    }));

    authServiceSpy.getCurrentUserEmail.and.returnValue('organizer@example.com');
    memberServiceSpy.getMemberByEmail.and.returnValue(of({
      id: 'member-id',
      email: 'organizer@example.com',
      username: 'organizer',
      gender: 'MALE',
      tier: 'PRO',
      registeredAt: '2025-01-01T00:00:00Z'
    }));

    await TestBed.configureTestingModule({
      imports: [TournamentDetailComponent],
      providers: [
        { provide: TournamentService, useValue: tournamentServiceSpy },
        { provide: MemberService, useValue: memberServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
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
          gender: 'MIXED'
        }
      ]
    });
    expect(component.eventsSuccessMessage()).toContain('guardados correctamente');
  });

  it('should show validation error when saving with no selected events', () => {
    component.saveTournamentEvents();

    expect(tournamentServiceSpy.saveTournamentEvents).not.toHaveBeenCalled();
    expect(component.eventsErrorMessage()).toContain('Selecciona al menos un evento');
  });

  it('should require at least one gender per selected event before saving', () => {
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
});
