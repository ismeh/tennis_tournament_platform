import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { TournamentService } from '../../data/services/tournament.service';
import { AuthService } from '../../core/auth/auth.service';
import { getApiErrorMessage } from '../../core/errors/api-error.util';
import { LocationInputComponent } from '../../components/location-input';
import {
  getTournamentSurfaceCategoryLabel,
  TournamentCreateRequest,
  TournamentSurfaceCategory,
} from '../../data/interfaces/tournament.model';

@Component({
  selector: 'app-create-tournament-page',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink, LocationInputComponent],
  template: `
    <section class="relative overflow-hidden bg-gradient-to-br from-neutral-50 via-white to-primary-50/60 py-10 sm:py-14">
      <div class="absolute inset-0 -z-10 opacity-60">
        <div class="absolute left-0 top-20 h-72 w-72 rounded-full bg-primary-200 blur-3xl"></div>
        <div class="absolute bottom-0 right-0 h-80 w-80 rounded-full bg-accent-200 blur-3xl"></div>
      </div>

      <div class="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8">
        <div class="rounded-3xl border border-neutral-200 bg-white p-6 shadow-2xl shadow-neutral-200 sm:p-8">
          <div class="flex items-start justify-between gap-4">
            <div>
              <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-600">Formulario</p>
              <h2 class="mt-2 text-2xl font-bold text-neutral-900">Datos del torneo</h2>
            </div>
            <a routerLink="/torneos" class="rounded-full border border-neutral-200 px-4 py-2 text-sm font-medium text-neutral-600 transition-colors hover:border-primary-300 hover:text-primary-700">
              Cancelar
            </a>
          </div>

          <form class="mt-8 space-y-6" [formGroup]="form" (ngSubmit)="submit()">
            <div class="grid gap-4 md:grid-cols-2">
              <label class="block">
                <span class="mb-1 block text-sm font-medium text-neutral-700">Nombre formal</span>
                <input
                  type="text"
                  formControlName="formalName"
                  class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white"
                  placeholder="Open de Primavera 2026"
                />
                @if (form.controls.formalName.touched && form.controls.formalName.invalid) {
                  <p class="mt-2 text-xs text-red-600">El nombre es obligatorio y debe tener al menos 3 caracteres.</p>
                }
              </label>

              <label class="block">
                <span class="mb-1 block text-sm font-medium text-neutral-700">Superficie</span>
                <select
                  formControlName="surfaceCategory"
                  class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white"
                >
                  @for (surface of surfaceOptions; track surface) {
                    <option [value]="surface">{{ getSurfaceLabel(surface) }}</option>
                  }
                </select>
              </label>
            </div>

            <div class="grid gap-4 md:grid-cols-2">
              <label class="block">
                <span class="mb-1 block text-sm font-medium text-neutral-700">Inicio del torneo</span>
                <input type="date" formControlName="playStartDate" class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white" />
              </label>
              <label class="block">
                <span class="mb-1 block text-sm font-medium text-neutral-700">Hora de inicio</span>
                <input type="time" formControlName="tournamentStartTime" class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white" />
              </label>
            </div>

            <div class="grid gap-4 md:grid-cols-2">
              <label class="block">
                <span class="mb-1 block text-sm font-medium text-neutral-700">Fin del torneo</span>
                <input type="date" formControlName="playEndDate" class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white" />
              </label>
              <label class="block">
                <span class="mb-1 block text-sm font-medium text-neutral-700">Número de pistas</span>
                <input type="number" formControlName="courtCount" min="0" step="1" class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white" placeholder="4" />
              </label>
            </div>

            <div class="grid gap-4 md:grid-cols-2">
              <label class="block">
                <span class="mb-1 block text-sm font-medium text-neutral-700">Inicio de inscripciones</span>
                <input type="date" formControlName="inscriptionStartDate" class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white" />
              </label>
              <label class="block">
                <span class="mb-1 block text-sm font-medium text-neutral-700">Cierre de inscripciones</span>
                <input type="date" formControlName="inscriptionEndDate" class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white" />
              </label>
            </div>

            <div class="grid gap-4 md:grid-cols-2">
              <label class="block">
                <span class="mb-1 block text-sm font-medium text-neutral-700">Plazas</span>
                <input type="number" formControlName="maxPlayers" min="2" step="1" class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white" placeholder="32" />
              </label>

              <label class="block">
                <span class="mb-1 block text-sm font-medium text-neutral-700">Ubicación</span>
                <app-location-input
                  formControlName="location"
                  placeholder="Club de Tenis Principal"
                  (locationSelected)="onLocationSelected($event)"
                ></app-location-input>
                @if (form.controls.location.touched && form.controls.location.invalid) {
                  <p class="mt-2 text-xs text-red-600">La ubicación es obligatoria y debe tener al menos 3 caracteres.</p>
                }
              </label>
            </div>

            <div class="grid gap-4 md:grid-cols-2">
              <label class="block">
                <span class="mb-1 block text-sm font-medium text-neutral-700">Formato de partido</span>
                <select
                  formControlName="setsPerMatch"
                  class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white"
                >
                  <option [value]="3">Al mejor de 3 sets</option>
                  <option [value]="5">Al mejor de 5 sets</option>
                </select>
              </label>

              <label class="block">
                <span class="mb-1 block text-sm font-medium text-neutral-700">Tiebreak set decisivo</span>
                <select
                  formControlName="decisiveTiebreakPoints"
                  class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white"
                >
                  <option [value]="7">Tiebreak normal (a 7 puntos)</option>
                  <option [value]="10">Super Tiebreak (a 10 puntos)</option>
                </select>
              </label>
            </div>

            <div class="rounded-2xl border border-neutral-200 bg-neutral-50 p-4 text-sm text-neutral-600">
              El organizador se toma del usuario autenticado. El torneo se crea como <span class="font-semibold text-neutral-900">borrador</span>. Las pruebas se configurarán después.
            </div>

            @if (errorMessage()) {
              <div class="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                {{ errorMessage() }}
              </div>
            }

            <div class="flex flex-col gap-3 sm:flex-row sm:justify-end">
              <button
                type="button"
                (click)="resetForm()"
                class="rounded-2xl border border-neutral-300 px-5 py-3 font-semibold text-neutral-700 transition-colors hover:border-neutral-400 hover:bg-neutral-50"
              >
                Limpiar
              </button>
              <button
                type="submit"
                class="rounded-2xl bg-gradient-to-r from-primary-600 to-accent-600 px-5 py-3 font-semibold text-white shadow-lg shadow-primary-200 transition-all hover:scale-[1.01] hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-60"
                [disabled]="form.invalid || isSubmitting()"
              >
                {{ isSubmitting() ? 'Guardando...' : 'Crear torneo' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </section>
  `
})
export class CreateTournamentComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly tournamentService = inject(TournamentService);
  private readonly authService = inject(AuthService);

  readonly surfaceOptions: TournamentSurfaceCategory[] = ['CLAY', 'HARD', 'GRASS', 'CARPET'];
  readonly getSurfaceLabel = getTournamentSurfaceCategoryLabel;
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    formalName: ['', [Validators.required, Validators.minLength(3)]],
    ...this.buildInitialDateValues(),
    tournamentStartTime: ['09:00', [Validators.required]],
    surfaceCategory: ['CLAY' as TournamentSurfaceCategory, [Validators.required]],
    maxPlayers: [32, [Validators.required, Validators.min(2)]],
    location: ['', [Validators.required, Validators.minLength(3)]],
    locationLatitude: [null as number | null],
    locationLongitude: [null as number | null],
    locationPlaceId: [null as string | null],
    locationFormattedAddress: [null as string | null],
    courtCount: [4, [Validators.required, Validators.min(0)]],
    setsPerMatch: [3, [Validators.required]],
    decisiveTiebreakPoints: [7, [Validators.required]]
  });

  constructor() {}

  ngOnInit(): void {
    const role = this.authService.currentRole;
    if (role !== 'ORGANIZER') {
      this.router.navigateByUrl('/torneos');
    }
  }

  onLocationSelected(suggestion: any): void {
    this.form.patchValue({
      locationLatitude: suggestion.latitude,
      locationLongitude: suggestion.longitude,
      locationPlaceId: suggestion.placeId,
      locationFormattedAddress: suggestion.formattedAddress
    });
  }

  submit(): void {
    if (this.form.invalid || this.isSubmitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = this.form.value as TournamentCreateRequest;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    this.tournamentService
      .createTournament(payload)
      .pipe(
        finalize(() => {
          this.isSubmitting.set(false);
        })
      )
      .subscribe({
        next: tournament => {
          void this.router.navigate(['/torneos', tournament.id]);
        },
        error: (error) => {
          this.errorMessage.set(getApiErrorMessage(error, 'No se pudo crear el torneo. Revisa los datos e inténtalo de nuevo.'));
        }
      });
  }

  resetForm(): void {
    this.form.reset({
      formalName: '',
      ...this.buildInitialDateValues(),
      tournamentStartTime: '09:00',
      surfaceCategory: 'CLAY',
      maxPlayers: 32,
      location: '',
      locationLatitude: null,
      locationLongitude: null,
      locationPlaceId: null,
      locationFormattedAddress: null,
      courtCount: 4,
      setsPerMatch: 3,
      decisiveTiebreakPoints: 7
    });
    this.errorMessage.set(null);
  }

  private buildInitialDateValues(): Pick<TournamentCreateRequest, 'playStartDate' | 'playEndDate' | 'inscriptionStartDate' | 'inscriptionEndDate'> {
    const today = this.startOfDay(new Date());
    const inscriptionStartDate = this.formatDate(today);
    const inscriptionEndDate = this.formatDate(this.addDays(today, 7));
    const playStartDate = this.formatDate(this.addDays(today, 8));
    const playEndDate = this.formatDate(this.addDays(today, 9));

    return {
      playStartDate,
      playEndDate,
      inscriptionStartDate,
      inscriptionEndDate
    };
  }

  private addDays(date: Date, days: number): Date {
    const nextDate = new Date(date);
    nextDate.setDate(nextDate.getDate() + days);
    return nextDate;
  }

  private startOfDay(date: Date): Date {
    const nextDate = new Date(date);
    nextDate.setHours(0, 0, 0, 0);
    return nextDate;
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }
}
