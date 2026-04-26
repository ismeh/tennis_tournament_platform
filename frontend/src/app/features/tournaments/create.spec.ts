import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CreateTournamentComponent } from './create';
import { TournamentService } from '../../data/services/tournament.service';

describe('CreateTournamentComponent', () => {
  let fixture: ComponentFixture<CreateTournamentComponent>;
  let component: CreateTournamentComponent;
  let tournamentServiceSpy: jasmine.SpyObj<TournamentService>;
  const fixedNow = new Date('2026-04-12T10:00:00');

  beforeEach(async () => {
    jasmine.clock().install();
    jasmine.clock().mockDate(fixedNow);

    tournamentServiceSpy = jasmine.createSpyObj<TournamentService>('TournamentService', [
      'createTournament',
      'getEventCatalog',
      'saveTournamentEvents'
    ]);
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
    tournamentServiceSpy.saveTournamentEvents.and.returnValue(of(void 0));

    await TestBed.configureTestingModule({
      imports: [CreateTournamentComponent],
      providers: [
        provideRouter([]),
        { provide: TournamentService, useValue: tournamentServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreateTournamentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    jasmine.clock().uninstall();
  });

  it('should load the event catalog on init', () => {
    expect(component.eventCatalog().length).toBe(2);
    expect(component.isLoadingEvents()).toBeFalse();
  });

  it('should initialize inscription and play dates relative to today', () => {
    expect(component.form.controls.inscriptionStartDate.value).toBe('2026-04-12');
    expect(component.form.controls.inscriptionEndDate.value).toBe('2026-04-19');
    expect(component.form.controls.playStartDate.value).toBe('2026-04-20');
    expect(component.form.controls.playEndDate.value).toBe('2026-04-21');
  });

  it('should add checked events to the configuration list and allow multiple genders', () => {
    component.toggleCatalogEvent(
      { id: 1, category: 'Absoluto Individual Masculino', description: 'Individual masculino open' },
      true
    );
    component.toggleCatalogEvent(
      { id: 2, category: 'Absoluto Dobles Mixto', description: 'Dobles mixto open' },
      true
    );

    expect(component.selectedEvents().length).toBe(2);
    expect(component.selectedEvents()[0].genders).toEqual([]);

    component.toggleEventGender(2, 'FEMALE', true);

    expect(component.selectedEvents().find(item => item.categoryId === 2)?.genders).toEqual(['FEMALE']);
  });

  it('should create a tournament and save the selected events', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = spyOn(router, 'navigate').and.resolveTo(true);

    tournamentServiceSpy.createTournament.and.returnValue(of({
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
      providerOrganisationId: 'member-id'
    }));

    component.selectedEvents.set([
      {
        categoryId: 1,
        eventCategory: 'Absoluto Individual Masculino',
        genders: ['MALE', 'MIXED']
      }
    ]);

    component.form.setValue({
      formalName: 'Open de Primavera',
      playStartDate: '2026-05-01',
      playEndDate: '2026-05-10',
      inscriptionStartDate: '2026-04-01',
      inscriptionEndDate: '2026-04-20',
      surfaceCategory: 'CLAY',
      maxPlayers: 32,
      location: 'Club Central'
    });

    component.submit();

    expect(tournamentServiceSpy.createTournament).toHaveBeenCalledWith({
      formalName: 'Open de Primavera',
      playStartDate: '2026-05-01',
      playEndDate: '2026-05-10',
      inscriptionStartDate: '2026-04-01',
      inscriptionEndDate: '2026-04-20',
      surfaceCategory: 'CLAY',
      maxPlayers: 32,
      location: 'Club Central'
    });
    expect(tournamentServiceSpy.saveTournamentEvents).toHaveBeenCalledWith('tournament-id', {
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
    });
    expect(component.successMessage()).toBe('Torneo creado correctamente y eventos preparados para guardar en backend.');
    expect(component.createdTournament()?.id).toBe('tournament-id');
    expect(component.errorMessage()).toBeNull();
    expect(navigateSpy).toHaveBeenCalledWith(['/torneos', 'tournament-id']);
  });

  it('should show an error message when creation fails', () => {
    tournamentServiceSpy.createTournament.and.returnValue(throwError(() => new Error('Bad Request')));

    component.form.setValue({
      formalName: 'Open de Primavera',
      playStartDate: '2026-05-01',
      playEndDate: '2026-05-10',
      inscriptionStartDate: '2026-04-01',
      inscriptionEndDate: '2026-04-20',
      surfaceCategory: 'CLAY',
      maxPlayers: 32,
      location: 'Club Central'
    });

    component.submit();

    expect(component.errorMessage()).toContain('No se pudo crear el torneo');
  });

  it('should restore the initial date defaults when resetting the form', () => {
    component.form.patchValue({
      playStartDate: '2026-06-01',
      playEndDate: '2026-06-02',
      inscriptionStartDate: '2026-05-01',
      inscriptionEndDate: '2026-05-02'
    });

    component.resetForm();

    expect(component.form.controls.inscriptionStartDate.value).toBe('2026-04-12');
    expect(component.form.controls.inscriptionEndDate.value).toBe('2026-04-19');
    expect(component.form.controls.playStartDate.value).toBe('2026-04-20');
    expect(component.form.controls.playEndDate.value).toBe('2026-04-21');
  });

  it('should require at least one gender per selected event before submit', () => {
    component.selectedEvents.set([
      {
        categoryId: 1,
        eventCategory: 'Absoluto Individual Masculino',
        genders: []
      }
    ]);

    component.form.setValue({
      formalName: 'Open de Primavera',
      playStartDate: '2026-05-01',
      playEndDate: '2026-05-10',
      inscriptionStartDate: '2026-04-01',
      inscriptionEndDate: '2026-04-20',
      surfaceCategory: 'CLAY',
      maxPlayers: 32,
      location: 'Club Central'
    });

    component.submit();

    expect(tournamentServiceSpy.createTournament).not.toHaveBeenCalled();
    expect(component.errorMessage()).toContain('al menos un género en cada evento');
  });
});