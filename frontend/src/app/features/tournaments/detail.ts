import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import {
  TournamentEventCategoryGender,
  TournamentEventCatalogItem,
  TournamentEventGender,
  TournamentEventSelection,
  TournamentEventsConfigRequest,
  TournamentProviderSummary,
  TournamentStatus,
  TournamentResponse,
  getTournamentEventGenderLabel,
  getTournamentSurfaceCategoryLabel
} from '../../data/interfaces/tournament.model';
import { MemberService } from '../../data/services/member.service';
import { TournamentService } from '../../data/services/tournament.service';

type TournamentDetailSection = 'overview' | 'setup' | 'inscriptions';

@Component({
  selector: 'app-tournament-detail-page',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe],
  template: `
    <section class="relative overflow-hidden bg-gradient-to-b from-neutral-50 via-white to-white py-10 sm:py-14">
      <div class="mx-auto max-w-6xl px-4 sm:px-6 lg:px-8">
        <a routerLink="/torneos" class="inline-flex items-center text-sm font-semibold text-primary-700 hover:text-primary-800">
          <- Volver al listado
        </a>

        @if (isLoading()) {
          <div class="mt-5 rounded-2xl border border-neutral-200 bg-white p-6 text-neutral-600">Cargando torneo...</div>
        } @else if (errorMessage()) {
          <div class="mt-5 rounded-2xl border border-red-200 bg-red-50 p-6 text-red-700">{{ errorMessage() }}</div>
        } @else if (tournament()) {
          <header class="mt-5 rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
            <p class="text-xs font-semibold uppercase tracking-[0.22em] text-primary-600">Detalle de torneo</p>
            <div class="mt-3 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <h1 class="text-3xl font-black text-neutral-900 sm:text-4xl">{{ tournament()!.formalName }}</h1>
                <p class="mt-2 text-neutral-600">{{ tournament()!.location }}</p>
              </div>
              <span class="inline-flex w-fit rounded-full border border-primary-200 bg-primary-50 px-4 py-2 text-sm font-semibold text-primary-700">
                Estado: {{ tournament()!.status }}
              </span>
            </div>

            <div class="mt-6 rounded-2xl border border-neutral-200 bg-neutral-50 p-2">
              <div class="flex flex-wrap gap-2">
                <button
                  type="button"
                  (click)="setActiveSection('overview')"
                  [class]="activeSection() === 'overview' ? 'rounded-xl bg-white px-4 py-2 text-sm font-semibold text-primary-700 shadow-sm' : 'rounded-xl px-4 py-2 text-sm font-semibold text-neutral-600 hover:bg-white/70'"
                >
                  Informacion general
                </button>

                @if (isCreator()) {
                  <button
                    type="button"
                    (click)="setActiveSection('setup')"
                    [class]="activeSection() === 'setup' ? 'rounded-xl bg-white px-4 py-2 text-sm font-semibold text-primary-700 shadow-sm' : 'rounded-xl px-4 py-2 text-sm font-semibold text-neutral-600 hover:bg-white/70'"
                  >
                    Finalizar configuracion
                  </button>
                }

                <button
                  type="button"
                  (click)="setActiveSection('inscriptions')"
                  [class]="activeSection() === 'inscriptions' ? 'rounded-xl bg-white px-4 py-2 text-sm font-semibold text-primary-700 shadow-sm' : 'rounded-xl px-4 py-2 text-sm font-semibold text-neutral-600 hover:bg-white/70'"
                >
                  Inscripciones
                </button>
              </div>
            </div>
          </header>

          @if (activeSection() === 'overview') {
            <section class="mt-6 rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
              <h2 class="text-xl font-bold text-neutral-900">Informacion del torneo</h2>
              <div class="mt-5 grid gap-4 sm:grid-cols-2">
                <div class="rounded-2xl border border-neutral-200 p-4">
                  <p class="text-xs uppercase tracking-widest text-neutral-500">Superficie</p>
                  <p class="mt-1 font-semibold text-neutral-900">{{ getSurfaceLabel(tournament()!.surfaceCategory) }}</p>
                </div>
                <div class="rounded-2xl border border-neutral-200 p-4">
                  <p class="text-xs uppercase tracking-widest text-neutral-500">Capacidad</p>
                  <p class="mt-1 font-semibold text-neutral-900">{{ tournament()!.maxPlayers }} jugadores</p>
                </div>
                <div class="rounded-2xl border border-neutral-200 p-4">
                  <p class="text-xs uppercase tracking-widest text-neutral-500">Periodo de juego</p>
                  <p class="mt-1 font-semibold text-neutral-900">
                    {{ tournament()!.playStartDate | date: 'dd/MM/yyyy' }} - {{ tournament()!.playEndDate | date: 'dd/MM/yyyy' }}
                  </p>
                </div>
                <div class="rounded-2xl border border-neutral-200 p-4">
                  <p class="text-xs uppercase tracking-widest text-neutral-500">Periodo de inscripcion</p>
                  <p class="mt-1 font-semibold text-neutral-900">
                    {{ tournament()!.inscriptionStartDate | date: 'dd/MM/yyyy' }} - {{ tournament()!.inscriptionEndDate | date: 'dd/MM/yyyy' }}
                  </p>
                </div>
              </div>
            </section>
          }

          @if (activeSection() === 'setup') {
            <section class="mt-6 rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
              @if (isCreator()) {
                <h2 class="text-xl font-bold text-neutral-900">Panel de configuracion del creador</h2>
                <p class="mt-2 text-neutral-600">Gestiona el torneo antes de abrir o durante el proceso de inscripcion.</p>

                <div class="mt-6 rounded-3xl border border-neutral-200 bg-neutral-50 p-5 sm:p-6">
                  <div class="rounded-2xl border border-neutral-200 bg-white p-4">
                    <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-600">Estado del torneo</p>
                    <div class="mt-3 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                      <label class="block min-w-56">
                        <span class="mb-1 block text-sm font-medium text-neutral-700">Nuevo estado</span>
                        <select
                          class="w-full rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                          [disabled]="isUpdatingStatus() || allowedStatusTransitions().length === 0"
                          [value]="selectedStatus() ?? ''"
                          (change)="onSelectedStatusChange($any($event.target).value)"
                        >
                          @for (status of allowedStatusTransitions(); track status) {
                            <option [value]="status">{{ status }}</option>
                          }
                        </select>
                      </label>

                      <button
                        type="button"
                        (click)="updateTournamentStatus()"
                        class="rounded-2xl bg-white px-5 py-3 font-semibold text-neutral-800 ring-1 ring-neutral-300 transition-colors hover:bg-neutral-50 disabled:cursor-not-allowed disabled:opacity-60"
                        [disabled]="!canUpdateStatus()"
                      >
                        {{ isUpdatingStatus() ? 'Actualizando estado...' : 'Actualizar estado' }}
                      </button>
                    </div>
                    @if (allowedStatusTransitions().length === 0) {
                      <p class="mt-2 text-xs text-neutral-500">No hay transiciones de estado disponibles para el estado actual.</p>
                    }
                  </div>

                  <div class="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
                    <div>
                      <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-600">Eventos del torneo</p>
                      <h3 class="mt-2 text-xl font-bold text-neutral-900">Configura la lista de eventos aquí mismo</h3>
                      <p class="mt-2 text-sm text-neutral-600">Selecciona varios eventos del catálogo, marca uno o varios géneros y guarda la configuración sin salir del detalle.</p>
                    </div>
                    <span class="rounded-full bg-white px-3 py-1 text-xs font-semibold uppercase tracking-widest text-neutral-600">
                      {{ selectedEvents().length }} seleccionados
                    </span>
                  </div>

                  @if (isLoadingEvents()) {
                    <div class="mt-4 rounded-2xl border border-dashed border-neutral-300 bg-white px-4 py-3 text-sm text-neutral-600">
                      Cargando catálogo de eventos...
                    </div>
                  }

                  @if (eventCatalogError()) {
                    <div class="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                      {{ eventCatalogError() }}
                    </div>
                  }

                  <div class="mt-4 grid gap-4 lg:grid-cols-[1fr_1.15fr]">
                    <label class="block">
                      <span class="mb-1 block text-sm font-medium text-neutral-700">Catálogo disponible</span>
                      <div class="max-h-64 space-y-2 overflow-y-auto rounded-2xl border border-neutral-300 bg-white p-3">
                        @for (event of eventCatalog(); track event.id) {
                          <label class="flex cursor-pointer items-center gap-3 rounded-xl bg-neutral-50 px-3 py-2 hover:bg-primary-50">
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

                    <div class="rounded-2xl border border-dashed border-neutral-300 bg-white p-4">
                      @if (selectedEvents().length === 0) {
                        <p class="text-sm text-neutral-500">No hay eventos seleccionados todavía.</p>
                      } @else {
                        <div class="space-y-3">
                          @for (event of selectedEvents(); track event.categoryId) {
                            <div class="rounded-2xl border border-neutral-200 bg-neutral-50 p-4">
                              <div class="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
                                <div>
                                  <p class="text-sm font-semibold text-neutral-900">{{ getEventLabelById(event.categoryId) }}</p>
                                </div>
                                <div class="min-w-52">
                                  <div class="mb-2 flex items-center gap-2">
                                    <span class="text-xs font-semibold uppercase tracking-widest text-neutral-500">Géneros</span>
                                    <span
                                      class="inline-flex h-4 w-4 items-center justify-center rounded-full border border-neutral-300 text-[10px] font-bold text-neutral-500"
                                      title="Por cada género seleccionado se creará un evento extra."
                                      aria-label="Información sobre géneros"
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
                            </div>
                          }
                        </div>
                      }
                    </div>
                  </div>

                  @if (eventsSuccessMessage()) {
                    <div class="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                      {{ eventsSuccessMessage() }}
                    </div>
                  }

                  @if (eventsErrorMessage()) {
                    <div class="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                      {{ eventsErrorMessage() }}
                    </div>
                  }

                  <div class="mt-4 flex flex-col gap-3 sm:flex-row sm:justify-end">
                    <button
                      type="button"
                      (click)="clearSelectedEvents()"
                      class="rounded-2xl border border-neutral-300 px-5 py-3 font-semibold text-neutral-700 transition-colors hover:border-neutral-400 hover:bg-white"
                    >
                      Limpiar eventos
                    </button>
                    <button
                      type="button"
                      (click)="saveTournamentEvents()"
                      class="rounded-2xl bg-gradient-to-r from-primary-600 to-accent-600 px-5 py-3 font-semibold text-white shadow-lg shadow-primary-200 transition-all hover:scale-[1.01] hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-60"
                      [disabled]="isSavingEvents() || isLoadingEvents() || selectedEvents().length === 0"
                    >
                      {{ isSavingEvents() ? 'Guardando eventos...' : 'Guardar eventos del torneo' }}
                    </button>
                  </div>
                </div>

                <div class="mt-5 grid gap-3">
                  <div class="rounded-2xl border border-primary-100 bg-primary-50 p-4 text-sm text-neutral-700">
                    Verifica fechas de juego e inscripcion para evitar solapes.
                  </div>
                  <div class="rounded-2xl border border-accent-100 bg-accent-50 p-4 text-sm text-neutral-700">
                    Comprueba capacidad y ubicacion antes de publicar.
                  </div>
                  <div class="rounded-2xl border border-neutral-200 bg-white p-4 text-sm text-neutral-700">
                    Siguiente paso sugerido: abrir inscripciones cuando el estado sea OPEN.
                  </div>
                </div>
              } @else {
                <div class="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-amber-800">
                  Solo el creador puede acceder a las opciones de configuracion del torneo.
                </div>
              }
            </section>
          }

          @if (activeSection() === 'inscriptions') {
            <section class="mt-6 rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
              <h2 class="text-xl font-bold text-neutral-900">Inscripciones</h2>
              <p class="mt-2 text-neutral-600">
                @if (isCreator()) {
                  Administra el proceso de inscripciones de tu torneo.
                } @else {
                  Solicita tu inscripcion si el torneo esta abierto.
                }
              </p>

              @if (isCreator()) {
                <div class="mt-5 rounded-2xl border border-neutral-200 bg-neutral-50 p-4 text-sm text-neutral-700">
                  Desde esta seccion podras revisar participantes inscritos y su estado conforme se habilite el flujo completo.
                </div>
              } @else {
                <div class="mt-5 rounded-2xl border border-neutral-200 bg-neutral-50 p-4 text-sm text-neutral-700">
                  Estado actual del torneo: <span class="font-semibold text-neutral-900">{{ tournament()!.status }}</span>
                </div>

                @if (!isProfileComplete()) {
                  <div class="mt-4 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
                    Debes completar tu perfil para inscribirte.
                    <a routerLink="/perfil" class="ml-2 font-semibold underline">Ir a completar perfil</a>
                  </div>
                }

                <div class="mt-4 grid gap-3 sm:grid-cols-2">
                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700">Categoria</span>
                    <select
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [value]="selectedInscriptionCategoryId() ?? ''"
                      (change)="onInscriptionCategoryChange($any($event.target).value)"
                    >
                      <option value="">Selecciona categoria</option>
                      @for (category of inscriptionCategories(); track category.categoryId) {
                        <option [value]="category.categoryId">{{ category.eventCategory }}</option>
                      }
                    </select>
                  </label>

                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700">Genero del evento</span>
                    <select
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [value]="selectedInscriptionGender() ?? ''"
                      (change)="onInscriptionGenderChange($any($event.target).value)"
                    >
                      <option value="">Selecciona genero</option>
                      @for (gender of inscriptionGenderOptions(); track gender) {
                        <option [value]="gender">{{ getGenderLabel(gender) }}</option>
                      }
                    </select>
                  </label>
                </div>

                <button
                  type="button"
                  class="mt-4 rounded-2xl bg-gradient-to-r from-primary-600 to-accent-600 px-5 py-3 font-semibold text-white shadow-lg shadow-primary-200 transition-all hover:scale-[1.01] hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-60"
                  [disabled]="!canRequestInscription() || isSubmittingInscription()"
                  (click)="requestInscription()"
                >
                  {{ isSubmittingInscription() ? 'Tramitando inscripcion...' : 'Realizar inscripcion' }}
                </button>
              }

              @if (actionMessage()) {
                <div class="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                  {{ actionMessage() }}
                </div>
              }

              @if (actionError()) {
                <div class="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                  {{ actionError() }}
                </div>
              }
            </section>
          }
        }
      </div>
    </section>
  `
})
export class TournamentDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly tournamentService = inject(TournamentService);
  private readonly memberService = inject(MemberService);
  private readonly authService = inject(AuthService);

  readonly eventGenderOptions: TournamentEventGender[] = ['MALE', 'FEMALE', 'MIXED'];
  readonly tournament = signal<TournamentResponse | null>(null);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly actionMessage = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly isSubmittingInscription = signal(false);
  readonly activeSection = signal<TournamentDetailSection>('overview');
  readonly currentMemberId = signal<string | null>(null);
  readonly isLoadingEvents = signal(true);
  readonly isSavingEvents = signal(false);
  readonly eventCatalog = signal<TournamentEventCatalogItem[]>([]);
  readonly selectedEvents = signal<TournamentEventSelection[]>([]);
  readonly eventsSuccessMessage = signal<string | null>(null);
  readonly eventsErrorMessage = signal<string | null>(null);
  readonly eventCatalogError = signal<string | null>(null);
  readonly selectedStatus = signal<TournamentStatus | null>(null);
  readonly isUpdatingStatus = signal(false);
  readonly isProfileComplete = signal(false);
  readonly selectedInscriptionCategoryId = signal<number | null>(null);
  readonly selectedInscriptionGender = signal<TournamentEventGender | null>(null);

  readonly isCreator = computed(() => {
    const tournament = this.tournament();
    const memberId = this.currentMemberId();
    const providerOrganisationId = this.getProviderOrganisationId(tournament?.providerOrganisationId);

    if (!providerOrganisationId || !memberId) {
      return false;
    }

    return providerOrganisationId === memberId;
  });

  readonly canRequestInscription = computed(() => {
    const currentTournament = this.tournament();
    if (!currentTournament) {
      return false;
    }

    if (!this.isProfileComplete()) {
      return false;
    }

    return currentTournament.status === 'OPEN' && !!this.selectedEventId();
  });

  readonly inscriptionCategories = computed<TournamentEventSelection[]>(() => {
    const groupedEvents = new Map<number, Set<TournamentEventGender>>();
    const events = this.tournament()?.events ?? [];

    events.forEach(event => {
      const normalizedGender = event.gender.toUpperCase() as TournamentEventGender;
      if (!this.eventGenderOptions.includes(normalizedGender)) {
        return;
      }

      const currentGenders = groupedEvents.get(event.categoryId) ?? new Set<TournamentEventGender>();
      currentGenders.add(normalizedGender);
      groupedEvents.set(event.categoryId, currentGenders);
    });

    return Array.from(groupedEvents.entries()).map(([categoryId, genders]) => ({
      categoryId,
      eventCategory: this.getEventLabelById(categoryId),
      genders: Array.from(genders)
    }));
  });

  readonly inscriptionGenderOptions = computed<TournamentEventGender[]>(() => {
    const selectedCategoryId = this.selectedInscriptionCategoryId();
    if (!selectedCategoryId) {
      return [];
    }

    return this.inscriptionCategories().find(event => event.categoryId === selectedCategoryId)?.genders ?? [];
  });

  readonly selectedEventId = computed<string | null>(() => {
    const selectedCategoryId = this.selectedInscriptionCategoryId();
    const selectedGender = this.selectedInscriptionGender();
    const events = this.tournament()?.events ?? [];

    if (!selectedCategoryId || !selectedGender) {
      return null;
    }

    const matchedEvent = events.find(event => event.categoryId === selectedCategoryId && event.gender.toUpperCase() === selectedGender);
    return matchedEvent?.eventId ?? null;
  });

  readonly allowedStatusTransitions = computed<TournamentStatus[]>(() => {
    const currentTournament = this.tournament();
    if (!currentTournament) {
      return [];
    }

    const transitionsByStatus: Record<TournamentStatus, TournamentStatus[]> = {
      DRAFT: ['OPEN', 'CANCELLED'],
      OPEN: ['CLOSED', 'CANCELLED'],
      ACTIVE: ['CLOSED', 'CANCELLED'],
      CLOSED: ['IN_PROGRESS', 'CANCELLED'],
      IN_PROGRESS: ['COMPLETED', 'CANCELLED'],
      COMPLETED: [],
      CANCELLED: []
    };

    return transitionsByStatus[currentTournament.status] ?? [];
  });

  readonly getSurfaceLabel = getTournamentSurfaceCategoryLabel;
  readonly getGenderLabel = getTournamentEventGenderLabel;

  constructor() {
    this.loadEventCatalog();
  }

  ngOnInit(): void {
    const tournamentId = this.route.snapshot.paramMap.get('id');
    if (!tournamentId) {
      this.errorMessage.set('No se encontro el identificador del torneo.');
      this.isLoading.set(false);
      return;
    }

    this.resolveCurrentMemberId();
    this.loadTournament(tournamentId);
  }

  setActiveSection(section: TournamentDetailSection): void {
    this.activeSection.set(section);
    this.actionMessage.set(null);
    this.actionError.set(null);
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
          genders: []
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

  hasEventGender(categoryId: number, gender: TournamentEventGender): boolean {
    return this.selectedEvents()
      .find(event => event.categoryId === categoryId)
      ?.genders.includes(gender) ?? false;
  }

  clearSelectedEvents(): void {
    this.selectedEvents.set([]);
    this.eventsSuccessMessage.set(null);
    this.eventsErrorMessage.set(null);
  }

  isEventSelected(categoryId: number): boolean {
    return this.selectedEvents().some(event => event.categoryId === categoryId);
  }

  getEventLabelById(categoryId: number): string {
    return this.eventCatalog().find(event => event.id === categoryId)?.category ?? String(categoryId);
  }

  onSelectedStatusChange(nextStatus: string): void {
    this.selectedStatus.set(nextStatus as TournamentStatus);
  }

  canUpdateStatus(): boolean {
    const tournament = this.tournament();
    const selectedStatus = this.selectedStatus();
    if (!tournament || !selectedStatus || this.isUpdatingStatus()) {
      return false;
    }

    return this.allowedStatusTransitions().includes(selectedStatus);
  }

  updateTournamentStatus(): void {
    const currentTournament = this.tournament();
    const nextStatus = this.selectedStatus();

    if (!currentTournament || !nextStatus) {
      return;
    }

    if (!this.allowedStatusTransitions().includes(nextStatus)) {
      this.actionError.set('No se puede realizar la transición de estado seleccionada.');
      return;
    }

    this.isUpdatingStatus.set(true);
    this.actionError.set(null);
    this.actionMessage.set(null);

    this.tournamentService.updateTournamentStatus(currentTournament.id, { status: nextStatus }).subscribe({
      next: updatedTournament => {
        this.tournament.set(updatedTournament);
        this.selectedStatus.set(this.allowedStatusTransitions()[0] ?? null);
        this.actionMessage.set('Estado del torneo actualizado correctamente.');
        this.isUpdatingStatus.set(false);
      },
      error: () => {
        this.actionError.set('No se pudo actualizar el estado del torneo.');
        this.isUpdatingStatus.set(false);
      }
    });
  }

  saveTournamentEvents(): void {
    const currentTournament = this.tournament();
    const selectedEvents = this.selectedEvents();

    if (!currentTournament) {
      return;
    }

    if (selectedEvents.length === 0) {
      this.eventsErrorMessage.set('Selecciona al menos un evento antes de guardar.');
      return;
    }

    if (selectedEvents.some(event => event.genders.length === 0)) {
      this.eventsErrorMessage.set('Debes seleccionar al menos un género en cada evento antes de guardar.');
      return;
    }

    const payload: TournamentEventsConfigRequest = {
      events: selectedEvents.flatMap(event =>
        event.genders.map(gender => ({
          categoryId: event.categoryId,
          gender
        }))
      )
    };

    this.isSavingEvents.set(true);
    this.eventsErrorMessage.set(null);
    this.eventsSuccessMessage.set(null);

    this.tournamentService.saveTournamentEvents(currentTournament.id, payload).subscribe({
      next: () => {
        this.isSavingEvents.set(false);
        this.eventsSuccessMessage.set('Eventos del torneo guardados correctamente.');
      },
      error: () => {
        this.isSavingEvents.set(false);
        this.eventsErrorMessage.set('No se pudieron guardar los eventos del torneo.');
      }
    });
  }

  requestInscription(): void {
    const currentTournament = this.tournament();
    const eventId = this.selectedEventId();
    const categoryId = this.selectedInscriptionCategoryId();

    if (!currentTournament) {
      return;
    }

    if (!eventId || !categoryId) {
      this.actionError.set('Selecciona categoria y genero antes de solicitar la inscripcion.');
      return;
    }

    if (!this.canRequestInscription()) {
      this.actionError.set('No cumples los requisitos para inscribirte en este evento.');
      return;
    }

    this.isSubmittingInscription.set(true);
    this.actionError.set(null);
    this.actionMessage.set(null);

    this.tournamentService.requestInscription(currentTournament.id, eventId, { categoryId, partnerId: null }).subscribe({
      next: () => {
        this.isSubmittingInscription.set(false);
        this.actionMessage.set('Inscripcion realizada correctamente para el evento seleccionado.');
      },
      error: () => {
        this.isSubmittingInscription.set(false);
        this.actionError.set('No se pudo completar la inscripcion para este evento.');
      }
    });
  }

  onInscriptionCategoryChange(rawCategoryId: string): void {
    if (!rawCategoryId) {
      this.selectedInscriptionCategoryId.set(null);
      this.selectedInscriptionGender.set(null);
      return;
    }

    const categoryId = Number(rawCategoryId);
    this.selectedInscriptionCategoryId.set(Number.isNaN(categoryId) ? null : categoryId);
    this.selectedInscriptionGender.set(null);
  }

  onInscriptionGenderChange(rawGender: string): void {
    if (!rawGender) {
      this.selectedInscriptionGender.set(null);
      return;
    }

    this.selectedInscriptionGender.set(rawGender as TournamentEventGender);
  }

  private loadTournament(tournamentId: string): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.tournamentService.getTournamentById(tournamentId).subscribe({
      next: tournament => {
        this.tournament.set(tournament);
        this.hydrateSelectedEventsFromTournament(tournament.events ?? []);
        this.initializeInscriptionSelection();
        this.selectedStatus.set(this.getDefaultStatusSelection(tournament.status));
        this.isLoading.set(false);
        this.activeSection.set(this.isCreator() ? 'setup' : 'overview');
      },
      error: () => {
        this.errorMessage.set('No se pudo cargar el detalle del torneo.');
        this.isLoading.set(false);
      }
    });
  }

  private loadEventCatalog(): void {
    this.isLoadingEvents.set(true);
    this.eventCatalogError.set(null);

    this.tournamentService.getEventCatalog().subscribe({
      next: catalog => {
        this.eventCatalog.set(catalog);
        this.isLoadingEvents.set(false);
      },
      error: () => {
        this.eventCatalog.set([]);
        this.eventCatalogError.set('No se pudo cargar el catálogo de eventos desde backend.');
        this.isLoadingEvents.set(false);
      }
    });
  }

  private resolveCurrentMemberId(): void {
    const email = this.authService.getCurrentUserEmail();
    if (!email) {
      return;
    }

    this.memberService.getMemberByEmail(email).subscribe({
      next: member => {
        this.currentMemberId.set(member.id);
      },
      error: () => {
        this.currentMemberId.set(null);
      }
    });

    this.memberService.getMyProfile().subscribe({
      next: profile => {
        const complete = !!profile.firstName && !!profile.gender && !!profile.birthDate;
        this.isProfileComplete.set(complete);
      },
      error: () => {
        this.isProfileComplete.set(false);
      }
    });
  }

  private hydrateSelectedEventsFromTournament(events: TournamentEventCategoryGender[]): void {
    const groupedEvents = new Map<number, Set<TournamentEventGender>>();

    events.forEach(event => {
      const normalizedGender = event.gender.toUpperCase() as TournamentEventGender;
      if (!this.eventGenderOptions.includes(normalizedGender)) {
        return;
      }

      const currentGenders = groupedEvents.get(event.categoryId) ?? new Set<TournamentEventGender>();
      currentGenders.add(normalizedGender);
      groupedEvents.set(event.categoryId, currentGenders);
    });

    const selections: TournamentEventSelection[] = Array.from(groupedEvents.entries()).map(([categoryId, genders]) => ({
      categoryId,
      eventCategory: this.getEventLabelById(categoryId),
      genders: Array.from(genders)
    }));

    this.selectedEvents.set(selections);
  }

  private getDefaultStatusSelection(currentStatus: TournamentStatus): TournamentStatus | null {
    const transitionsByStatus: Record<TournamentStatus, TournamentStatus[]> = {
      DRAFT: ['OPEN', 'CANCELLED'],
      OPEN: ['CLOSED', 'CANCELLED'],
      ACTIVE: ['CLOSED', 'CANCELLED'],
      CLOSED: ['IN_PROGRESS', 'CANCELLED'],
      IN_PROGRESS: ['COMPLETED', 'CANCELLED'],
      COMPLETED: [],
      CANCELLED: []
    };

    return transitionsByStatus[currentStatus]?.[0] ?? null;
  }

  private getProviderOrganisationId(providerOrganisation: string | TournamentProviderSummary | null | undefined): string | null {
    if (!providerOrganisation) {
      return null;
    }

    if (typeof providerOrganisation === 'string') {
      return providerOrganisation;
    }

    return providerOrganisation.id ?? null;
  }

  private initializeInscriptionSelection(): void {
    const firstCategory = this.inscriptionCategories()[0];
    if (!firstCategory) {
      this.selectedInscriptionCategoryId.set(null);
      this.selectedInscriptionGender.set(null);
      return;
    }

    this.selectedInscriptionCategoryId.set(firstCategory.categoryId);
    this.selectedInscriptionGender.set(firstCategory.genders[0] ?? null);
  }
}