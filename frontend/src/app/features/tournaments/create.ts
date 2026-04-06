import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TournamentService } from '../../data/services/tournament.service';
import {
  getTournamentSurfaceCategoryLabel,
  TournamentCreateRequest,
  TournamentResponse,
  TournamentSurfaceCategory
} from '../../data/interfaces/tournament.model';

@Component({
  selector: 'app-create-tournament-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="relative overflow-hidden bg-gradient-to-br from-neutral-50 via-white to-primary-50/60 py-10 sm:py-14">
      <div class="absolute inset-0 -z-10 opacity-60">
        <div class="absolute left-0 top-20 h-72 w-72 rounded-full bg-primary-200 blur-3xl"></div>
        <div class="absolute bottom-0 right-0 h-80 w-80 rounded-full bg-accent-200 blur-3xl"></div>
      </div>

      <div class="mx-auto grid max-w-7xl gap-8 px-4 sm:px-6 lg:grid-cols-[0.9fr_1.1fr] lg:px-8">
        <aside class="rounded-3xl border border-white/60 bg-white/80 p-8 shadow-xl shadow-primary-100 backdrop-blur">
          <p class="text-sm font-semibold uppercase tracking-[0.25em] text-primary-600">Nuevo torneo</p>
          <h1 class="mt-4 text-4xl font-black leading-tight text-neutral-900 sm:text-5xl">
            Crea el cuadro desde el primer clic.
          </h1>
          <p class="mt-5 text-base leading-7 text-neutral-600">
            Configura fechas, superficie y capacidad del torneo con un formulario claro y listo para operar sobre JWT.
          </p>

          <div class="mt-8 grid gap-4 sm:grid-cols-3 lg:grid-cols-1">
            <div class="rounded-2xl border border-primary-100 bg-primary-50 p-4">
              <p class="text-xs font-semibold uppercase tracking-widest text-primary-700">Fechas</p>
              <p class="mt-2 text-sm text-neutral-700">Ventana de juego e inscripción separadas para una gestión precisa.</p>
            </div>
            <div class="rounded-2xl border border-accent-100 bg-accent-50 p-4">
              <p class="text-xs font-semibold uppercase tracking-widest text-accent-700">Superficie</p>
              <p class="mt-2 text-sm text-neutral-700">Clay, hard, grass o carpet desde el formulario.</p>
            </div>
            <div class="rounded-2xl border border-neutral-200 bg-white p-4">
              <p class="text-xs font-semibold uppercase tracking-widest text-neutral-600">Estado</p>
              <p class="mt-2 text-sm text-neutral-700">El backend crea el torneo en borrador y asigna el organizador autenticado.</p>
            </div>
          </div>
        </aside>

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
                <input
                  type="date"
                  formControlName="playStartDate"
                  class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white"
                />
              </label>
              <label class="block">
                <span class="mb-1 block text-sm font-medium text-neutral-700">Fin del torneo</span>
                <input
                  type="date"
                  formControlName="playEndDate"
                  class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white"
                />
              </label>
            </div>

            <div class="grid gap-4 md:grid-cols-2">
              <label class="block">
                <span class="mb-1 block text-sm font-medium text-neutral-700">Inicio de inscripciones</span>
                <input
                  type="date"
                  formControlName="inscriptionStartDate"
                  class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white"
                />
              </label>
              <label class="block">
                <span class="mb-1 block text-sm font-medium text-neutral-700">Cierre de inscripciones</span>
                <input
                  type="date"
                  formControlName="inscriptionEndDate"
                  class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white"
                />
              </label>
            </div>

            <div class="grid gap-4 md:grid-cols-2">
              <label class="block">
                <span class="mb-1 block text-sm font-medium text-neutral-700">Capacidad máxima</span>
                <input
                  type="number"
                  formControlName="maxPlayers"
                  min="2"
                  step="1"
                  class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white"
                  placeholder="32"
                />
              </label>

              <label class="block">
                <span class="mb-1 block text-sm font-medium text-neutral-700">Ubicación</span>
                <input
                  type="text"
                  formControlName="location"
                  class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white"
                  placeholder="Club de Tenis Principal"
                />
              </label>
            </div>

            <div class="rounded-2xl border border-neutral-200 bg-neutral-50 p-4 text-sm text-neutral-600">
              El organizador se toma del usuario autenticado. El torneo se crea con estado <span class="font-semibold text-neutral-900">DRAFT</span>.
            </div>

            @if (errorMessage()) {
              <div class="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                {{ errorMessage() }}
              </div>
            }

            @if (successMessage()) {
              <div class="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                {{ successMessage() }}
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
                {{ isSubmitting() ? 'Creando torneo...' : 'Crear torneo' }}
              </button>
            </div>
          </form>

          @if (createdTournament()) {
            <div class="mt-8 rounded-3xl border border-primary-100 bg-primary-50 p-6">
              <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-700">Creado</p>
              <div class="mt-3 grid gap-3 sm:grid-cols-2">
                <div>
                  <p class="text-xs uppercase tracking-widest text-neutral-500">Nombre</p>
                  <p class="font-semibold text-neutral-900">{{ createdTournament()?.formalName }}</p>
                </div>
                <div>
                  <p class="text-xs uppercase tracking-widest text-neutral-500">Estado</p>
                  <p class="font-semibold text-neutral-900">{{ createdTournament()?.status }}</p>
                </div>
                <div>
                  <p class="text-xs uppercase tracking-widest text-neutral-500">Superficie</p>
                  <p class="font-semibold text-neutral-900">{{ createdTournament() ? getSurfaceLabel(createdTournament()!.surfaceCategory) : '' }}</p>
                </div>
                <div>
                  <p class="text-xs uppercase tracking-widest text-neutral-500">Ubicación</p>
                  <p class="font-semibold text-neutral-900">{{ createdTournament()?.location }}</p>
                </div>
                <div>
                  <p class="text-xs uppercase tracking-widest text-neutral-500">Organizador</p>
                  <p class="font-semibold text-neutral-900">{{ createdTournament()?.providerOrganisationId ?? 'Autenticado' }}</p>
                </div>
              </div>
            </div>
          }
        </div>
      </div>
    </section>
  `
})
export class CreateTournamentComponent {
  private readonly fb = inject(FormBuilder);
  private readonly tournamentService = inject(TournamentService);

  readonly surfaceOptions: TournamentSurfaceCategory[] = ['CLAY', 'HARD', 'GRASS', 'CARPET'];
  readonly getSurfaceLabel = getTournamentSurfaceCategoryLabel;
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly createdTournament = signal<TournamentResponse | null>(null);

  readonly form = this.fb.nonNullable.group({
    formalName: ['', [Validators.required, Validators.minLength(3)]],
    playStartDate: ['', [Validators.required]],
    playEndDate: ['', [Validators.required]],
    inscriptionStartDate: ['', [Validators.required]],
    inscriptionEndDate: ['', [Validators.required]],
    surfaceCategory: ['CLAY' as TournamentSurfaceCategory, [Validators.required]],
    maxPlayers: [32, [Validators.required, Validators.min(2)]],
    location: ['', [Validators.required, Validators.minLength(3)]]
  });

  submit(): void {
    if (this.form.invalid || this.isSubmitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = this.form.getRawValue() satisfies TournamentCreateRequest;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.tournamentService.createTournament(payload).subscribe({
      next: tournament => {
        this.isSubmitting.set(false);
        this.createdTournament.set(tournament);
        this.successMessage.set('Torneo creado correctamente.');
      },
      error: () => {
        this.isSubmitting.set(false);
        this.errorMessage.set('No se pudo crear el torneo. Revisa los datos e inténtalo de nuevo.');
      }
    });
  }

  resetForm(): void {
    this.form.reset({
      formalName: '',
      playStartDate: '',
      playEndDate: '',
      inscriptionStartDate: '',
      inscriptionEndDate: '',
      surfaceCategory: 'CLAY',
      maxPlayers: 32,
      location: ''
    });
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.createdTournament.set(null);
  }
}