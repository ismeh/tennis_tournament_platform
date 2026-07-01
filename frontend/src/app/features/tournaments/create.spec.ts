import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CreateTournamentComponent } from './create';
import { TournamentService } from '../../data/services/tournament.service';
import { PlaceService } from '../../data/services/place.service';

describe('CreateTournamentComponent', () => {
  let fixture: ComponentFixture<CreateTournamentComponent>;
  let component: CreateTournamentComponent;
  let tournamentServiceSpy: jasmine.SpyObj<TournamentService>;
  let placeServiceSpy: jasmine.SpyObj<PlaceService>;
  const fixedNow = new Date('2026-04-12T10:00:00');

  beforeEach(async () => {
    jasmine.clock().install();
    jasmine.clock().mockDate(fixedNow);

    tournamentServiceSpy = jasmine.createSpyObj<TournamentService>('TournamentService', [
      'createTournament'
    ]);

    placeServiceSpy = jasmine.createSpyObj<PlaceService>('PlaceService', ['search']);
    placeServiceSpy.search.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [CreateTournamentComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        { provide: TournamentService, useValue: tournamentServiceSpy },
        { provide: PlaceService, useValue: placeServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreateTournamentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    jasmine.clock().uninstall();
  });

  it('should initialize inscription and play dates relative to today', () => {
    expect(component.form.controls.inscriptionStartDate.value).toBe('2026-04-12');
    expect(component.form.controls.inscriptionEndDate.value).toBe('2026-04-19');
    expect(component.form.controls.playStartDate.value).toBe('2026-04-20');
    expect(component.form.controls.playEndDate.value).toBe('2026-04-21');
  });

  it('should create a tournament and navigate to its detail page', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = spyOn(router, 'navigate').and.resolveTo(true);

    tournamentServiceSpy.createTournament.and.returnValue(of({
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
      providerOrganisationId: 'member-id'
    }));

    component.form.setValue({
      formalName: 'Open de Primavera',
      playStartDate: '2026-05-01',
      playEndDate: '2026-05-10',
      tournamentStartTime: '09:00',
      inscriptionStartDate: '2026-04-01',
      inscriptionEndDate: '2026-04-20',
      surfaceCategory: 'CLAY',
      maxPlayers: 32,
      location: 'Club Central',
      locationLatitude: null,
      locationLongitude: null,
      locationPlaceId: null,
      locationFormattedAddress: null,
      courtCount: 4,
      setsPerMatch: 3,
      decisiveTiebreakPoints: 7
    });

    component.submit();

    expect(tournamentServiceSpy.createTournament).toHaveBeenCalledWith({
      formalName: 'Open de Primavera',
      playStartDate: '2026-05-01',
      playEndDate: '2026-05-10',
      tournamentStartTime: '09:00',
      inscriptionStartDate: '2026-04-01',
      inscriptionEndDate: '2026-04-20',
      surfaceCategory: 'CLAY',
      maxPlayers: 32,
      location: 'Club Central',
      locationLatitude: null,
      locationLongitude: null,
      locationPlaceId: null,
      locationFormattedAddress: null,
      courtCount: 4,
      setsPerMatch: 3,
      decisiveTiebreakPoints: 7
    });
    expect(component.errorMessage()).toBeNull();
    expect(navigateSpy).toHaveBeenCalledWith(['/torneos', 'tournament-id']);
  });

  it('should show an error message when creation fails', () => {
    tournamentServiceSpy.createTournament.and.returnValue(throwError(() => new Error('Bad Request')));

    component.form.setValue({
      formalName: 'Open de Primavera',
      playStartDate: '2026-05-01',
      playEndDate: '2026-05-10',
      tournamentStartTime: '09:00',
      inscriptionStartDate: '2026-04-01',
      inscriptionEndDate: '2026-04-20',
      surfaceCategory: 'CLAY',
      maxPlayers: 32,
      location: 'Club Central',
      locationLatitude: null,
      locationLongitude: null,
      locationPlaceId: null,
      locationFormattedAddress: null,
      courtCount: 4,
      setsPerMatch: 3,
      decisiveTiebreakPoints: 7
    });

    component.submit();

    expect(component.errorMessage()).toContain('No se pudo crear el torneo');
  });

  it('should restore the initial date defaults when resetting the form', () => {
    component.form.patchValue({
      playStartDate: '2026-06-01',
      playEndDate: '2026-06-02',
      tournamentStartTime: '11:30',
      inscriptionStartDate: '2026-05-01',
      inscriptionEndDate: '2026-05-02',
      courtCount: 8
    });

    component.resetForm();

    expect(component.form.controls.inscriptionStartDate.value).toBe('2026-04-12');
    expect(component.form.controls.inscriptionEndDate.value).toBe('2026-04-19');
    expect(component.form.controls.playStartDate.value).toBe('2026-04-20');
    expect(component.form.controls.playEndDate.value).toBe('2026-04-21');
    expect(component.form.controls.tournamentStartTime.value).toBe('09:00');
    expect(component.form.controls.courtCount.value).toBe(4);
  });

  it('should not submit when the form is invalid', () => {
    component.form.controls.formalName.setValue('');

    component.submit();

    expect(tournamentServiceSpy.createTournament).not.toHaveBeenCalled();
  });
});
