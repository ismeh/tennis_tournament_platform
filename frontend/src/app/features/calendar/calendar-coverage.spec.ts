import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { CalendarComponent } from './calendar';
import { AuthService } from '../../core/auth/auth.service';
import { TournamentService } from '../../data/services/tournament.service';
import { PlayerMatchCalendarResponse, TournamentCalendarPageResponse, TournamentCalendarResponse } from '../../data/interfaces/tournament.model';

describe('CalendarComponent branch coverage', () => {
  let tournamentService: jasmine.SpyObj<TournamentService>;
  let authService: jasmine.SpyObj<AuthService>;
  let roleSubject: BehaviorSubject<string | null>;
  let isLoggedInSubject: BehaviorSubject<boolean>;

  beforeEach(async () => {
    roleSubject = new BehaviorSubject<string | null>(null);
    isLoggedInSubject = new BehaviorSubject<boolean>(false);

    tournamentService = jasmine.createSpyObj('TournamentService', [
      'getPublishedTournamentCalendar',
      'getMyMatchCalendar',
      'getMyTournamentCalendar',
      'getUmpireTournaments'
    ]);

    tournamentService.getPublishedTournamentCalendar.and.returnValue(of({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 1 } as TournamentCalendarPageResponse));
    tournamentService.getMyMatchCalendar.and.returnValue(of([] as any));
    tournamentService.getMyTournamentCalendar.and.returnValue(of([] as any));
    tournamentService.getUmpireTournaments.and.returnValue(of([] as any));

    authService = jasmine.createSpyObj('AuthService', [], {
      role$: roleSubject.asObservable(),
      isLoggedIn$: isLoggedInSubject.asObservable()
    });

    await TestBed.configureTestingModule({
      imports: [CalendarComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: '**', component: CalendarComponent }]),
        { provide: TournamentService, useValue: tournamentService },
        { provide: AuthService, useValue: authService }
      ]
    }).compileComponents();
  });

  function createComponent(): CalendarComponent {
    const fixture = TestBed.createComponent(CalendarComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('covers helper fallbacks and filter counters', () => {
    const component = TestBed.createComponent(CalendarComponent).componentInstance;

    expect(component.getStatusLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(component.getStatusColorClasses('UNKNOWN')).toContain('primary');
    expect(component.getSchedulePrefix('NOT_BEFORE')).toBe('No antes de');
    expect(component.getSchedulePrefix(null)).toBe('A las');
    expect(component.getSurfaceLabel('UNKNOWN' as never)).toBe('UNKNOWN');
    expect(component.getSurfaceImage('UNKNOWN' as never)).toBe('');

    expect(component.activeFilterCount()).toBe(0);
    component.fromDate.set('2026-01-01');
    component.toDate.set('2026-01-02');
    component.surface.set('CLAY');
    component.status.set('OPEN');
    component.professionalTournament.set(true);
    component.name.set('  Open  ');
    component.location.set('Madrid');
    expect(component.activeFilterCount()).toBe(7);
  });

  it('rejects invalid ranges and loads calendars on valid applyFilters', () => {
    const component = createComponent();
    component.fromDate.set('2026-07-10');
    component.toDate.set('2026-07-09');

    component.applyFilters();
    expect(component.dateRangeError()).toBe('La fecha final debe ser posterior a la fecha inicial.');
    expect(tournamentService.getPublishedTournamentCalendar).toHaveBeenCalledTimes(1);

    component.fromDate.set('2026-07-01');
    component.toDate.set('2026-07-10');
    component.currentPage.set(3);
    component.sidebarPage.set(2);
    isLoggedInSubject.next(true);

    component.applyFilters();

    expect(component.currentPage()).toBe(0);
    expect(component.sidebarPage()).toBe(0);
    expect(tournamentService.getPublishedTournamentCalendar).toHaveBeenCalledTimes(2);
  });

  it('moves main and sidebar pagination only when allowed', () => {
    const component = createComponent();

    component.currentPage.set(0);
    component.totalPages.set(2);
    expect(component.canGoPrevious()).toBeFalse();
    expect(component.canGoNext()).toBeTrue();
    component.goToNextPage();
    expect(component.currentPage()).toBe(1);
    component.goToPreviousPage();
    expect(component.currentPage()).toBe(0);

    component.myMatches.set([
      {
        matchId: '1',
        tournamentId: 't1',
        tournamentName: 'T1',
        eventId: 'e1',
        eventName: 'E1',
        roundNumber: 1,
        scheduledAt: '2026-01-01T10:00:00Z',
        firstParticipantName: 'A',
        secondParticipantName: 'B'
      },
      {
        matchId: '2',
        tournamentId: 't2',
        tournamentName: 'T2',
        eventId: 'e2',
        eventName: 'E2',
        roundNumber: 1,
        scheduledAt: '2026-01-02T10:00:00Z',
        firstParticipantName: 'C',
        secondParticipantName: 'D'
      },
      {
        matchId: '3',
        tournamentId: 't3',
        tournamentName: 'T3',
        eventId: 'e3',
        eventName: 'E3',
        roundNumber: 1,
        scheduledAt: '2026-01-03T10:00:00Z',
        firstParticipantName: 'E',
        secondParticipantName: 'F'
      },
      {
        matchId: '4',
        tournamentId: 't4',
        tournamentName: 'T4',
        eventId: 'e4',
        eventName: 'E4',
        roundNumber: 1,
        scheduledAt: '2026-01-04T10:00:00Z',
        firstParticipantName: 'G',
        secondParticipantName: 'H'
      },
      {
        matchId: '5',
        tournamentId: 't5',
        tournamentName: 'T5',
        eventId: 'e5',
        eventName: 'E5',
        roundNumber: 1,
        scheduledAt: '2026-01-05T10:00:00Z',
        firstParticipantName: 'I',
        secondParticipantName: 'J'
      },
      {
        matchId: '6',
        tournamentId: 't6',
        tournamentName: 'T6',
        eventId: 'e6',
        eventName: 'E6',
        roundNumber: 1,
        scheduledAt: '2026-01-06T10:00:00Z',
        firstParticipantName: 'K',
        secondParticipantName: 'L'
      }
    ] as PlayerMatchCalendarResponse[]);
    component.sortSidebarItems();
    expect(component.totalSidebarPages()).toBe(2);
    expect(component.canSidebarGoPrevious()).toBeFalse();
    expect(component.canSidebarGoNext()).toBeTrue();
    component.sidebarNextPage();
    expect(component.sidebarPage()).toBe(1);
    component.sidebarPrevPage();
    expect(component.sidebarPage()).toBe(0);
  });

  it('sorts sidebar data for every role', () => {
    const component = createComponent();

    component.userRole.set('ORGANIZER');
    component.myTournaments.set([
      { id: '1', formalName: 'A', playStartDate: '2026-01-01', playEndDate: '2026-01-02', location: 'L1', surfaceCategory: 'CLAY', maxPlayers: 8, status: 'OPEN' },
      { id: '2', formalName: 'B', playStartDate: '2026-02-01', playEndDate: '2026-02-02', location: 'L2', surfaceCategory: 'HARD', maxPlayers: 8, status: 'OPEN' }
    ] as TournamentCalendarResponse[]);
    component.sidebarSortOrder.set('newest');
    component.sortSidebarItems();
    expect(component.paginatedSidebarTournaments()[0].id).toBe('2');
    component.sidebarSortOrder.set('oldest');
    component.sortSidebarItems();
    expect(component.paginatedSidebarTournaments()[0].id).toBe('1');

    component.userRole.set('UMPIRE');
    component.myUmpireTournaments.set([
      { id: '3', formalName: 'C', playStartDate: '2026-01-01', playEndDate: '2026-01-02', location: 'L3', surfaceCategory: 'GRASS', maxPlayers: 8, status: 'OPEN' },
      { id: '4', formalName: 'D', playStartDate: '2026-03-01', playEndDate: '2026-03-02', location: 'L4', surfaceCategory: 'CARPET', maxPlayers: 8, status: 'OPEN' }
    ] as TournamentCalendarResponse[]);
    component.sidebarSortOrder.set('newest');
    component.sortSidebarItems();
    expect(component.paginatedSidebarUmpireTournaments()[0].id).toBe('4');

    component.userRole.set(null);
    component.myMatches.set([
      {
        matchId: 'm1',
        tournamentId: 't1',
        tournamentName: 'T1',
        eventId: 'e1',
        eventName: 'E1',
        roundNumber: 1,
        scheduledAt: '2026-01-01T10:00:00Z',
        firstParticipantName: 'A',
        secondParticipantName: 'B'
      },
      {
        matchId: 'm2',
        tournamentId: 't2',
        tournamentName: 'T2',
        eventId: 'e2',
        eventName: 'E2',
        roundNumber: 1,
        scheduledAt: '2026-01-03T10:00:00Z',
        firstParticipantName: 'C',
        secondParticipantName: 'D'
      }
    ] as PlayerMatchCalendarResponse[]);
    component.sidebarSortOrder.set('newest');
    component.sortSidebarItems();
    expect(component.paginatedSidebarMatches()[0].matchId).toBe('m2');
  });

  it('loads sidebar data for each role and handles errors', () => {
    let component = createComponent();
    expect(component.isLoggedIn()).toBeFalse();
    expect(component.sidebarItems().length).toBe(0);

    roleSubject.next('ORGANIZER');
    isLoggedInSubject.next(true);
    tournamentService.getMyTournamentCalendar.and.returnValue(of([
      { id: '1', formalName: 'Organizer Cup', playStartDate: '2026-01-01', playEndDate: '2026-01-02', location: 'Madrid', surfaceCategory: 'CLAY', maxPlayers: 8, status: 'OPEN' }
    ] as any));
    component = createComponent();
    expect(component.isOrganizer()).toBeTrue();
    expect(tournamentService.getMyTournamentCalendar).toHaveBeenCalled();
    expect(component.sidebarItems().length).toBe(1);

    roleSubject.next('UMPIRE');
    tournamentService.getUmpireTournaments.and.returnValue(of([
      { id: '2', formalName: 'Umpire Cup', playStartDate: '2026-02-01', playEndDate: '2026-02-02', location: 'Seville', surfaceCategory: 'HARD', maxPlayers: 8, status: 'OPEN' }
    ] as any));
    component = createComponent();
    expect(component.isUmpire()).toBeTrue();
    expect(tournamentService.getUmpireTournaments).toHaveBeenCalled();
    expect(component.sidebarItems().length).toBe(1);

    roleSubject.next(null);
    tournamentService.getMyMatchCalendar.and.returnValue(of([
      {
        matchId: 'm1',
        tournamentId: 't1',
        tournamentName: 'Match Cup',
        eventId: 'e1',
        eventName: 'Singles',
        roundNumber: 1,
        scheduledAt: '2026-01-01T10:00:00Z',
        firstParticipantName: 'A',
        secondParticipantName: 'B'
      }
    ] as any));
    component = createComponent();
    expect(component.isOrganizer()).toBeFalse();
    expect(component.isUmpire()).toBeFalse();
    expect(tournamentService.getMyMatchCalendar).toHaveBeenCalled();
    expect(component.sidebarItems().length).toBe(1);

    tournamentService.getMyMatchCalendar.and.returnValue(throwError(() => new Error('boom')));
    component = createComponent();
    expect(component.sidebarError()).toContain('No se pudieron cargar tus partidos.');
    expect(component.isLoadingSidebar()).toBeFalse();
  });
});
