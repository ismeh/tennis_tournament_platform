import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { TournamentsListComponent } from './list';
import { TournamentService } from '../../data/services/tournament.service';
import { AuthService } from '../../core/auth/auth.service';
import { BehaviorSubject, of, throwError } from 'rxjs';

function createTournament(overrides: Record<string, any> = {}) {
  return {
    id: 't1',
    formalName: 'Open 2026',
    playStartDate: '2026-06-01',
    playEndDate: '2026-06-15',
    tournamentStartTime: '09:00',
    inscriptionStartDate: '2026-05-01',
    inscriptionEndDate: '2026-05-30',
    surfaceCategory: 'CLAY',
    maxPlayers: 32,
    location: 'Madrid',
    status: 'OPEN',
    professionalTournament: false,
    ...overrides
  };
}

describe('TournamentsListComponent', () => {
  let component: TournamentsListComponent;
  let tournamentService: jasmine.SpyObj<TournamentService>;
  let authService: jasmine.SpyObj<AuthService>;
  let roleSubject: BehaviorSubject<string | null>;

  beforeEach(() => {
    tournamentService = jasmine.createSpyObj('TournamentService', ['getTournaments', 'getUmpireTournaments']);
    roleSubject = new BehaviorSubject<string | null>(null);

    authService = jasmine.createSpyObj('AuthService', [], {
      role$: roleSubject.asObservable()
    });

    TestBed.configureTestingModule({
      imports: [TournamentsListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TournamentService, useValue: tournamentService },
        { provide: AuthService, useValue: authService },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: {} } } }
      ]
    });

    const fixture = TestBed.createComponent(TournamentsListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('getStatusLabel', () => {
    it('returns correct labels for all statuses', () => {
      expect(component.getStatusLabel('DRAFT')).toBe('Borrador');
      expect(component.getStatusLabel('OPEN')).toBe('Inscripciones abiertas');
      expect(component.getStatusLabel('ACTIVE')).toBe('Activo');
      expect(component.getStatusLabel('CLOSED')).toBe('Inscripciones cerradas');
      expect(component.getStatusLabel('IN_PROGRESS')).toBe('En juego');
      expect(component.getStatusLabel('COMPLETED')).toBe('Finalizado');
      expect(component.getStatusLabel('CANCELLED')).toBe('Cancelado');
    });

    it('returns raw status for unknown', () => {
      expect(component.getStatusLabel('UNKNOWN' as any)).toBe('UNKNOWN');
    });
  });

  describe('ngOnInit', () => {
    it('loads tournaments on init', () => {
      const tournaments = [createTournament()];
      tournamentService.getTournaments.and.returnValue(of(tournaments as any));
      tournamentService.getUmpireTournaments.and.returnValue(of([]));

      component.ngOnInit();

      expect(component.tournaments().length).toBe(1);
      expect(component.isLoading()).toBe(false);
    });

    it('sets error message on tournament load failure', () => {
      tournamentService.getTournaments.and.returnValue(throwError(() => new Error('Network error')));
      tournamentService.getUmpireTournaments.and.returnValue(of([]));

      component.ngOnInit();

      expect(component.errorMessage()).toBeTruthy();
      expect(component.isLoading()).toBe(false);
    });

    it('loads umpire tournaments when role is UMPIRE', () => {
      tournamentService.getTournaments.and.returnValue(of([]));
      tournamentService.getUmpireTournaments.and.returnValue(of([createTournament({ id: 'u1', formalName: 'Umpire Tournament' })] as any));

      component.ngOnInit();
      roleSubject.next('UMPIRE');

      expect(component.isUmpire()).toBe(true);
      expect(component.umpireTournaments().length).toBe(1);
    });

    it('does not load umpire tournaments for non-umpire role', () => {
      tournamentService.getTournaments.and.returnValue(of([]));
      tournamentService.getUmpireTournaments.and.returnValue(of([]));

      component.ngOnInit();
      roleSubject.next('PLAYER');

      expect(component.isUmpire()).toBe(false);
      expect(component.isLoadingUmpire()).toBe(false);
    });
  });
});
