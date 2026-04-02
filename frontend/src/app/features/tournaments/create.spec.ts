import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { CreateTournamentComponent } from './create';
import { TournamentService } from '../../data/services/tournament.service';

describe('CreateTournamentComponent', () => {
  let fixture: ComponentFixture<CreateTournamentComponent>;
  let component: CreateTournamentComponent;
  let tournamentServiceSpy: jasmine.SpyObj<TournamentService>;

  beforeEach(async () => {
    tournamentServiceSpy = jasmine.createSpyObj<TournamentService>('TournamentService', ['createTournament']);

    await TestBed.configureTestingModule({
      imports: [CreateTournamentComponent],
      providers: [{ provide: TournamentService, useValue: tournamentServiceSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(CreateTournamentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create a tournament and show success feedback', () => {
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
    expect(component.successMessage()).toBe('Torneo creado correctamente.');
    expect(component.createdTournament()?.id).toBe('tournament-id');
    expect(component.errorMessage()).toBeNull();
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
});