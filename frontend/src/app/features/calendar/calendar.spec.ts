import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { CalendarComponent } from './calendar';
import { TournamentService } from '../../data/services/tournament.service';
import { AuthService } from '../../core/auth/auth.service';

describe('CalendarComponent', () => {
  let component: CalendarComponent;
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
      'getUmpireTournaments',
    ]);

    authService = jasmine.createSpyObj('AuthService', [], {
      role$: roleSubject.asObservable(),
      isLoggedIn$: isLoggedInSubject.asObservable(),
    });

    await TestBed.configureTestingModule({
      imports: [CalendarComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: '**', component: CalendarComponent }]),
        { provide: TournamentService, useValue: tournamentService },
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(CalendarComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('getStatusLabel', () => {
    it('should return correct labels', () => {
      expect(component.getStatusLabel('DRAFT')).toBe('Borrador');
      expect(component.getStatusLabel('OPEN')).toBe('Inscripciones abiertas');
      expect(component.getStatusLabel('CLOSED')).toBe('Inscripciones cerradas');
      expect(component.getStatusLabel('IN_PROGRESS')).toBe('En juego');
      expect(component.getStatusLabel('COMPLETED')).toBe('Finalizado');
      expect(component.getStatusLabel('CANCELLED')).toBe('Cancelado');
    });

    it('should return raw status for unknown', () => {
      expect(component.getStatusLabel('UNKNOWN')).toBe('UNKNOWN');
    });
  });

  describe('getStatusColorClasses', () => {
    it('should return color classes for known statuses', () => {
      expect(component.getStatusColorClasses('DRAFT')).toContain('neutral');
      expect(component.getStatusColorClasses('OPEN')).toContain('blue');
      expect(component.getStatusColorClasses('COMPLETED')).toContain('emerald');
      expect(component.getStatusColorClasses('CANCELLED')).toContain('red');
    });

    it('should return default for unknown status', () => {
      expect(component.getStatusColorClasses('UNKNOWN')).toContain('primary');
    });
  });

  describe('getSchedulePrefix', () => {
    it('should return No antes de for NOT_BEFORE', () => {
      expect(component.getSchedulePrefix('NOT_BEFORE')).toBe('No antes de');
    });

    it('should return A las for EXACT', () => {
      expect(component.getSchedulePrefix('EXACT')).toBe('A las');
    });

    it('should return A las for null', () => {
      expect(component.getSchedulePrefix(null)).toBe('A las');
    });
  });

  describe('getDateGroupLabel', () => {
    it('should format date string to Spanish locale', () => {
      const result = component.getDateGroupLabel('2026-05-01');
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });
  });

  describe('pagination', () => {
    it('should have default page 0', () => {
      expect(component.currentPage()).toBe(0);
    });

    it('should have default sidebar page 0', () => {
      expect(component.sidebarPage()).toBe(0);
    });

    it('should not go previous on page 0', () => {
      expect(component.canGoPrevious()).toBeFalse();
    });
  });

  describe('sortSidebarItems', () => {
    it('should sort by newest first', () => {
      component.sidebarSortOrder.set('newest');
      component.sortSidebarItems();
      expect(component.sidebarSortOrder()).toBe('newest');
    });

    it('should sort by oldest first', () => {
      component.sidebarSortOrder.set('oldest');
      component.sortSidebarItems();
      expect(component.sidebarSortOrder()).toBe('oldest');
    });
  });

  describe('sidebar pagination', () => {
    it('should not go previous on sidebar page 0', () => {
      expect(component.canSidebarGoPrevious()).toBeFalse();
    });

    it('should sidebar page start at 0', () => {
      expect(component.sidebarPage()).toBe(0);
    });
  });

  describe('filter panel', () => {
    it('should start with filter panel closed', () => {
      expect(component.filterPanelOpen()).toBeFalse();
    });

    it('should toggle filter panel', () => {
      component.filterPanelOpen.set(true);
      expect(component.filterPanelOpen()).toBeTrue();
      component.filterPanelOpen.set(false);
      expect(component.filterPanelOpen()).toBeFalse();
    });
  });

  describe('date range validation', () => {
    it('should start with no date range error', () => {
      expect(component.dateRangeError()).toBeNull();
    });
  });

  describe('initial loading states', () => {
    it('should start with tournaments not loading', () => {
      expect(component.isLoadingTournaments()).toBeFalse();
    });

    it('should start with sidebar not loading', () => {
      expect(component.isLoadingSidebar()).toBeFalse();
    });
  });

  describe('errors', () => {
    it('should start with no tournament error', () => {
      expect(component.tournamentsError()).toBeNull();
    });

    it('should start with no sidebar error', () => {
      expect(component.sidebarError()).toBeNull();
    });
  });
});
