import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { TournamentsListComponent } from './list';
import { TournamentService } from '../../data/services/tournament.service';
import { AuthService } from '../../core/auth/auth.service';
import { TournamentResponse } from '../../data/interfaces/tournament.model';
import { BehaviorSubject, of, throwError } from 'rxjs';

function createTournament(overrides: Partial<TournamentResponse> = {}): TournamentResponse {
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
  } as TournamentResponse;
}

describe('TournamentsListComponent', () => {
  let fixture: ComponentFixture<TournamentsListComponent>;
  let component: TournamentsListComponent;
  let tournamentService: jasmine.SpyObj<TournamentService>;
  let authService: jasmine.SpyObj<AuthService>;
  let roleSubject: BehaviorSubject<string | null>;

  beforeEach(() => {
    tournamentService = jasmine.createSpyObj('TournamentService', ['getTournaments', 'getUmpireTournaments']);
    tournamentService.getTournaments.and.returnValue(of([]));
    tournamentService.getUmpireTournaments.and.returnValue(of([]));
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

    fixture = TestBed.createComponent(TournamentsListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('getStatusLabel', () => {
    it('returns correct labels for all statuses', () => {
      expect(component.getStatusLabel('DRAFT')).toBe('Borrador');
      expect(component.getStatusLabel('OPEN')).toBe('Inscripciones abiertas');
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

    it('handles failure when loading umpire tournaments', () => {
      tournamentService.getTournaments.and.returnValue(of([]));
      tournamentService.getUmpireTournaments.and.returnValue(throwError(() => new Error('Umpire error')));

      component.ngOnInit();
      roleSubject.next('UMPIRE');

      expect(component.isUmpire()).toBe(true);
      expect(component.isLoadingUmpire()).toBe(false);
      expect(component.umpireTournaments().length).toBe(0);
    });
  });

  describe('getSurfaceLabel', () => {
    it('returns the correct label for surface category', () => {
      expect(component.getSurfaceLabel('CLAY')).toBe('Tierra batida');
      expect(component.getSurfaceLabel('HARD')).toBe('Pista dura');
      expect(component.getSurfaceLabel('GRASS')).toBe('Cesped');
    });
  });

  describe('Template / DOM Rendering', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('renders loading state', () => {
      component.isLoading.set(true);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Cargando torneos...');
    });

    it('renders error message', () => {
      component.isLoading.set(false);
      component.errorMessage.set('Error de prueba');
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Error de prueba');
    });

    it('renders empty tournaments message', () => {
      component.isLoading.set(false);
      component.errorMessage.set(null);
      component.tournaments.set([]);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Aún no se han creado torneos');
    });

    it('renders tournament cards when tournaments are present', () => {
      component.isLoading.set(false);
      component.errorMessage.set(null);
      component.tournaments.set([createTournament({ formalName: 'Torneo Especial' })]);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Torneo Especial');
    });

    it('renders umpire panel when isUmpire is true and loading', () => {
      component.isLoading.set(false);
      component.tournaments.set([]);
      component.isUmpire.set(true);
      component.isLoadingUmpire.set(true);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Cargando tus torneos...');
    });

    it('renders umpire empty message', () => {
      component.isLoading.set(false);
      component.tournaments.set([]);
      component.isUmpire.set(true);
      component.isLoadingUmpire.set(false);
      component.umpireTournaments.set([]);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('No tienes torneos asignados para arbitrar.');
    });

    it('renders umpire tournaments list', () => {
      component.isLoading.set(false);
      component.tournaments.set([]);
      component.isUmpire.set(true);
      component.isLoadingUmpire.set(false);
      component.umpireTournaments.set([createTournament({ formalName: 'Arbitraje Pro 2026' })]);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Arbitraje Pro 2026');
    });
  });
});
