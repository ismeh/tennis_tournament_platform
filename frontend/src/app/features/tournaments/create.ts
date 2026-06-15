import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, of, switchMap } from 'rxjs';
import { TournamentService } from '../../data/services/tournament.service';
import { AuthService } from '../../core/auth/auth.service';
import { getApiErrorMessage } from '../../core/errors/api-error.util';
import { LocationInputComponent } from '../../components/location-input';
import {
  getTournamentEventGenderLabel,
  getTournamentStageTypeLabel,
  getTournamentSurfaceCategoryLabel,
  TournamentCreateRequest,
  TournamentEventCatalogItem,
  TournamentEventGender,
  TournamentEventSelection,
  TournamentEventStageSelection,
  TournamentEventsConfigRequest,
  TournamentResponse,
  TournamentStageType,
  TournamentSurfaceCategory,
  validateStageSequence,
  isConsolationDisabled,
  getAvailableStageOptions
} from '../../data/interfaces/tournament.model';

@Component({
  selector: 'app-create-tournament-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, LocationInputComponent],
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
            Crea el torneo y define sus pruebas.
          </h1>
          <p class="mt-5 text-base leading-7 text-neutral-600">
            Primero se crea el torneo y después se añaden las pruebas: categoría, modalidad y formato de cuadro.
          </p>

          <div class="mt-8 grid gap-4 sm:grid-cols-3 lg:grid-cols-1">
            <div class="rounded-2xl border border-primary-100 bg-primary-50 p-4">
              <p class="text-xs font-semibold uppercase tracking-widest text-primary-700">Pruebas</p>
              <p class="mt-2 text-sm text-neutral-700">Selecciona las categorías que se jugarán en el torneo.</p>
            </div>
            <div class="rounded-2xl border border-accent-100 bg-accent-50 p-4">
              <p class="text-xs font-semibold uppercase tracking-widest text-accent-700">Modalidades</p>
              <p class="mt-2 text-sm text-neutral-700">Marca si la prueba será masculina, femenina o mixta.</p>
            </div>
            <div class="rounded-2xl border border-neutral-200 bg-white p-4">
              <p class="text-xs font-semibold uppercase tracking-widest text-neutral-600">Cuadros</p>
              <p class="mt-2 text-sm text-neutral-700">Elige las fases que tendrá cada prueba antes de abrir inscripciones.</p>
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
                <span class="mb-1 block text-sm font-medium text-neutral-700">Capacidad máxima</span>
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

            <div class="rounded-2xl border border-neutral-200 bg-neutral-50 p-4 text-sm text-neutral-600">
              El organizador se toma del usuario autenticado. El torneo se crea como <span class="font-semibold text-neutral-900">borrador</span> y después se añaden sus pruebas.
            </div>

            <div class="rounded-3xl border border-neutral-200 bg-white p-5">
              <div class="flex items-start justify-between gap-4">
                <div>
                  <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-600">Pruebas del torneo</p>
                  <h3 class="mt-2 text-xl font-bold text-neutral-900">Selecciona las categorías del cuadro</h3>
                  <p class="mt-2 text-sm text-neutral-600">Cada categoría seleccionada puede jugarse en una o varias modalidades.</p>
                </div>
                <span class="rounded-full bg-primary-50 px-3 py-1 text-xs font-semibold uppercase tracking-widest text-primary-700">
                  {{ selectedEvents().length }} seleccionados
                </span>
              </div>

              @if (isLoadingEvents()) {
                <div class="mt-4 rounded-2xl border border-dashed border-neutral-300 bg-neutral-50 px-4 py-3 text-sm text-neutral-600">
                  Cargando catálogo de categorías...
                </div>
              }

              @if (eventCatalogError()) {
                <div class="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                  {{ eventCatalogError() }}
                </div>
              }

              <div class="mt-4 grid gap-4 lg:grid-cols-[1fr_1.2fr]">
                <label class="block">
                  <span class="mb-1 block text-sm font-medium text-neutral-700">Categorías disponibles</span>
                  <div class="max-h-64 space-y-2 overflow-y-auto rounded-2xl border border-neutral-300 bg-neutral-50 p-3">
                    @for (event of eventCatalog(); track event.id) {
                      <label class="flex cursor-pointer items-center gap-3 rounded-xl bg-white px-3 py-2 hover:bg-primary-50">
                        <input
                          type="checkbox"
                          class="h-4 w-4 rounded border-neutral-300 text-primary-600 focus:ring-primary-500"
                          [checked]="isEventSelected(event.id)"
                          (change)="toggleCatalogEvent(event, $any($event.target).checked)"
                        />
                        <span class="text-sm text-neutral-800">{{ event.category }}</span>
                      </label>
                    }
                  </div>
                </label>

                <div class="rounded-2xl border border-dashed border-neutral-300 bg-neutral-50 p-4">
                  @if (selectedEvents().length === 0) {
                    <p class="text-sm text-neutral-500">No hay pruebas seleccionadas todavía.</p>
                  } @else {
                    <div class="space-y-3">
                      @for (event of selectedEvents(); track event.categoryId) {
                        <div class="rounded-2xl border border-neutral-200 bg-white p-4 shadow-sm">
                          <div class="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
                            <div>
                              <p class="text-sm font-semibold text-neutral-900">{{ getEventLabelById(event.categoryId) }}</p>
                            </div>

                            <div class="min-w-52">
                                <div class="mb-2 flex items-center gap-2">
                                  <span class="text-xs font-semibold uppercase tracking-widest text-neutral-500">Modalidades</span>
                                  <span
                                    class="inline-flex h-4 w-4 items-center justify-center rounded-full border border-neutral-300 text-[10px] font-bold text-neutral-500"
                                    title="Por cada modalidad seleccionada se creará una prueba."
                                    aria-label="Información sobre modalidades"
                                  >
                                    i
                                  </span>
                                </div>
                              <div class="flex flex-wrap gap-2">
                                @for (gender of eventGenderOptions; track gender) {
                                  <label class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-neutral-300 bg-white px-3 py-1.5 text-xs font-medium text-neutral-700 hover:border-primary-400">
                                    <input
                                      type="checkbox"
                                      class="h-3.5 w-3.5 rounded border-neutral-300 text-primary-600 focus:ring-primary-500"
                                      [checked]="hasEventGender(event.categoryId, gender)"
                                      (change)="toggleEventGender(event.categoryId, gender, $any($event.target).checked)"
                                    />
                                    <span>{{ getGenderLabel(gender) }}</span>
                                  </label>
                                }
                              </div>
                            </div>
                          </div>

                          <div class="mt-4 rounded-2xl border border-dashed border-neutral-200 bg-neutral-50 p-4">
                            <div class="flex items-center justify-between gap-3">
                              <div>
                                <p class="text-xs font-semibold uppercase tracking-widest text-neutral-500">Formato de cuadro</p>
                                <p class="mt-1 text-sm text-neutral-600">Ordena las fases que se jugarán en esta prueba.</p>
                              </div>
                              <button
                                type="button"
                                class="rounded-full border border-neutral-300 bg-white px-3 py-1.5 text-xs font-semibold text-neutral-700 transition hover:border-primary-400 hover:text-primary-700"
                                (click)="addEventStage(event.categoryId)"
                              >
                                Añadir fase
                              </button>
                            </div>

                            <div class="mt-4 space-y-3">
                              @for (stage of event.stages; track $index; let stageIndex = $index) {
                                <div class="rounded-xl border border-neutral-200 bg-white p-3 shadow-sm">
                                  <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                                    <div class="flex-1">
                                      <label class="block">
                                        <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Tipo de fase {{ stageIndex + 1 }}</span>
                                        <select
                                          class="w-full rounded-xl border border-neutral-300 bg-neutral-50 px-3 py-2 text-sm text-neutral-800 outline-none transition focus:border-primary-500 focus:bg-white"
                                          [value]="stage.stageType"
                                          (change)="updateEventStageType(event.categoryId, stageIndex, $any($event.target).value)"
                                        >
                                          @for (option of getAvailableStageOptions(event.stages, stageIndex); track option) {
                                            <option [value]="option">{{ getStageLabel(option) }}</option>
                                          }
                                        </select>
                                      </label>
                                    </div>

                                    <div class="flex items-center gap-2">
                                      <button
                                        type="button"
                                        class="rounded-full border border-red-200 bg-red-50 px-3 py-1.5 text-xs font-semibold text-red-700 transition hover:border-red-300 hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-40"
                                        [disabled]="event.stages.length === 1"
                                        (click)="removeEventStage(event.categoryId, stageIndex)"
                                      >
                                        Eliminar
                                      </button>
                                    </div>
                                  </div>
                                </div>
                              }
                            </div>
                          </div>
                        </div>
                      }
                    </div>
                  }
                </div>
              </div>
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
                [disabled]="form.invalid || isSubmitting() || isLoadingEvents()"
              >
                {{ isSubmitting() ? 'Guardando...' : 'Crear torneo y guardar pruebas' }}
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
                  <p class="font-semibold text-neutral-900">
                    {{ createdTournament() ? getSurfaceLabel(createdTournament()!.surfaceCategory) : '' }}
                  </p>
                </div>
                <div>
                  <p class="text-xs uppercase tracking-widest text-neutral-500">Ubicación</p>
                  <p class="font-semibold text-neutral-900">{{ createdTournament()?.location }}</p>
                </div>
                <div>
                  <p class="text-xs uppercase tracking-widest text-neutral-500">Pruebas seleccionadas</p>
                  <p class="font-semibold text-neutral-900">{{ selectedEvents().length }}</p>
                </div>
              </div>
            </div>
          }
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
  readonly eventGenderOptions: TournamentEventGender[] = ['MALE', 'FEMALE', 'MIXED'];
  readonly stageOptions: TournamentStageType[] = ['SINGLE_ELIMINATION', 'ROUND_ROBIN', 'DOUBLE_ELIMINATION', 'CONSOLATION'];
  readonly getSurfaceLabel = getTournamentSurfaceCategoryLabel;
  readonly getGenderLabel = getTournamentEventGenderLabel;
  readonly getStageLabel = getTournamentStageTypeLabel;
  readonly isSubmitting = signal(false);
  readonly isLoadingEvents = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly eventCatalogError = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly createdTournament = signal<TournamentResponse | null>(null);
  readonly eventCatalog = signal<TournamentEventCatalogItem[]>([]);
  readonly selectedEvents = signal<TournamentEventSelection[]>([]);

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
    courtCount: [4, [Validators.required, Validators.min(0)]]
  });

  constructor() {
    this.loadEventCatalog();
  }

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
    const configuredEvents = this.selectedEvents();

    if (configuredEvents.some(event => event.genders.length === 0)) {
      this.errorMessage.set('Debes seleccionar al menos una modalidad en cada prueba antes de guardar.');
      return;
    }

    if (configuredEvents.some(event => event.stages.length === 0)) {
      this.errorMessage.set('Debes definir al menos una fase en cada prueba antes de guardar.');
      return;
    }

    for (const event of configuredEvents) {
      const stageTypes = event.stages.map(s => s.stageType);
      const errors = validateStageSequence(stageTypes);
      if (errors.length > 0) {
        this.errorMessage.set(`Prueba "${event.eventCategory}": ${errors[0].message}`);
        return;
      }
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.tournamentService
      .createTournament(payload)
      .pipe(
        switchMap(tournament => {
          this.createdTournament.set(tournament);

          if (configuredEvents.length === 0) {
            return of(tournament);
          }

          const eventPayload: TournamentEventsConfigRequest = {
            events: configuredEvents.flatMap(event =>
              event.genders.map(gender => ({
                id: null,
                categoryId: event.categoryId,
                gender,
                stages: event.stages.map(stage => stage.stageType)
              }))
            )
          };

          return this.tournamentService.saveTournamentEvents(tournament.id, eventPayload).pipe(
            switchMap(() => of(tournament))
          );
        }),
        finalize(() => {
          this.isSubmitting.set(false);
        })
      )
      .subscribe({
        next: tournament => {
          this.successMessage.set('Torneo creado correctamente con sus pruebas.');
          void this.router.navigate(['/torneos', tournament.id]);
        },
        error: (error) => {
          this.errorMessage.set(getApiErrorMessage(error, 'No se pudo crear el torneo o guardar sus pruebas. Revisa los datos e inténtalo de nuevo.'));
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
      courtCount: 4
    });
    this.errorMessage.set(null);
    this.eventCatalogError.set(null);
    this.successMessage.set(null);
    this.createdTournament.set(null);
    this.selectedEvents.set([]);
  }

  toggleCatalogEvent(catalogEvent: TournamentEventCatalogItem, checked: boolean): void {
    if (checked) {
      if (this.isEventSelected(catalogEvent.id)) {
        return;
      }

      this.selectedEvents.update(events => [
        ...events,
        {
          categoryId: catalogEvent.id,
          eventCategory: catalogEvent.category,
          eventsByGender: [],
          genders: [],
          stages: [
            {
              stageType: 'SINGLE_ELIMINATION'
            }
          ]
        }
      ]);
      return;
    }

    this.selectedEvents.update(events => events.filter(event => event.categoryId !== catalogEvent.id));
  }

  toggleEventGender(categoryId: number, gender: TournamentEventGender, checked: boolean): void {
    this.selectedEvents.update(events =>
      events.map(event =>
        event.categoryId === categoryId
          ? {
            ...event,
            genders: checked
              ? Array.from(new Set([...event.genders, gender]))
              : event.genders.filter(currentGender => currentGender !== gender)
          }
          : event
      )
    );
  }

  addEventStage(categoryId: number): void {
    this.selectedEvents.update(events =>
      events.map(event => {
        if (event.categoryId !== categoryId) {
          return event;
        }
        const newStageType: TournamentStageType = isConsolationDisabled(event.stages.map(s => s.stageType))
          ? 'SINGLE_ELIMINATION'
          : 'SINGLE_ELIMINATION';
        return {
          ...event,
          stages: [...event.stages, { stageType: newStageType }]
        };
      })
    );
  }

  removeEventStage(categoryId: number, stageIndex: number): void {
    this.selectedEvents.update(events =>
      events.map(event => {
        if (event.categoryId !== categoryId || event.stages.length <= 1) {
          return event;
        }

        const removedType = event.stages[stageIndex].stageType;
        const nextIndex = stageIndex + 1;
        const hasConsolationAfter = removedType === 'SINGLE_ELIMINATION'
          && nextIndex < event.stages.length
          && event.stages[nextIndex].stageType === 'CONSOLATION';

        if (hasConsolationAfter) {
          const confirmed = window.confirm(
            'Al eliminar la fase SINGLE_ELIMINATION, la fase CONSOLATION siguiente también será eliminada. ¿Deseas continuar?'
          );
          if (!confirmed) {
            return event;
          }
          const newStages = event.stages.filter((_, index) => index !== stageIndex && index !== nextIndex);
          return {
            ...event,
            stages: newStages.length > 0 ? newStages : [{ stageType: 'SINGLE_ELIMINATION' }]
          };
        }

        return {
          ...event,
          stages: event.stages.filter((_, index) => index !== stageIndex)
        };
      })
    );
  }

  updateEventStageType(categoryId: number, stageIndex: number, stageType: TournamentStageType): void {
    this.selectedEvents.update(events =>
      events.map(event => {
        if (event.categoryId !== categoryId) {
          return event;
        }

        const newStages = event.stages.map((stage, index) => (index === stageIndex ? { stageType } : stage));
        const stageTypes = newStages.map(s => s.stageType);
        const errors = validateStageSequence(stageTypes);

        if (errors.length > 0) {
          this.errorMessage.set(errors[0].message);
          return event;
        }

        this.errorMessage.set(null);
        return {
          ...event,
          stages: newStages
        };
      })
    );
  }

  getAvailableStageOptions(stages: TournamentEventStageSelection[], currentIndex: number): TournamentStageType[] {
    return getAvailableStageOptions(stages.map(s => s.stageType), currentIndex);
  }

  hasEventGender(categoryId: number, gender: TournamentEventGender): boolean {
    return this.selectedEvents()
      .find(event => event.categoryId === categoryId)
      ?.genders.includes(gender) ?? false;
  }

  isEventSelected(categoryId: number): boolean {
    return this.selectedEvents().some(event => event.categoryId === categoryId);
  }

  getEventLabelById(categoryId: number): string {
    return this.eventCatalog().find(event => event.id === categoryId)?.category ?? String(categoryId);
  }

  private loadEventCatalog(): void {
    this.isLoadingEvents.set(true);
    this.eventCatalogError.set(null);

    const isOrganizer = this.authService.currentRole === 'ORGANIZER';
    const catalog$ = isOrganizer
      ? this.tournamentService.getEventCatalogAll()
      : this.tournamentService.getEventCatalog();

    catalog$.subscribe({
      next: catalog => {
        this.eventCatalog.set(catalog);
        this.isLoadingEvents.set(false);
      },
      error: (error) => {
        this.eventCatalog.set([]);
        this.eventCatalogError.set(getApiErrorMessage(error, 'No se pudo cargar el catálogo de categorías.'));
        this.isLoadingEvents.set(false);
      }
    });
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
