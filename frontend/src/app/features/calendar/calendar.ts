import { CommonModule, DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../../core/auth/auth.service';
import { UserRole } from '../../core/auth/auth.model';
import { getApiErrorMessage } from '../../core/errors/api-error.util';
import {
  PlayerMatchCalendarResponse,
  TournamentCalendarFilters,
  TournamentCalendarResponse,
  TournamentStatus,
  TournamentSurfaceCategory,
  getTournamentSurfaceCategoryLabel,
  getSurfaceBackgroundImage
} from '../../data/interfaces/tournament.model';
import { TournamentService } from '../../data/services/tournament.service';

type TournamentCalendarGroup = {
  date: string;
  tournaments: TournamentCalendarResponse[];
};

@Component({
  selector: 'app-calendar-page',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule, RouterLink],
  template: `
    <section class="min-h-[calc(100vh-4rem)] bg-neutral-50 py-8 sm:py-10">
      <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <header class="border-b border-neutral-200 pb-6">
          <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-600">Torneos</p>
          <h1 class="mt-2 text-3xl font-black text-neutral-950 sm:text-4xl">Calendario de torneos</h1>
          <p class="mt-2 max-w-2xl text-sm text-neutral-600">
            Busca torneos por fecha, nombre, lugar, estado y tipo de participantes.
          </p>
        </header>

        @if (dateRangeError()) {
          <div class="mt-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{{ dateRangeError() }}</div>
        }

        <div class="mt-6 grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
          <section class="min-w-0">
            <div class="flex items-center gap-3">
              <h2 class="text-xl font-bold text-neutral-950">Torneos</h2>
              <div class="h-6 w-px bg-neutral-300"></div>
              <span class="text-sm text-neutral-500">{{ totalElements() }} torneos</span>
              <div class="flex-1"></div>
              <button
                type="button"
                (click)="filterPanelOpen.set(true)"
                class="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-neutral-900 text-white shadow-sm transition hover:bg-neutral-800"
                title="Filtros"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="4" y1="21" x2="4" y2="14"></line>
                  <line x1="4" y1="10" x2="4" y2="3"></line>
                  <line x1="12" y1="21" x2="12" y2="12"></line>
                  <line x1="12" y1="8" x2="12" y2="3"></line>
                  <line x1="20" y1="21" x2="20" y2="16"></line>
                  <line x1="20" y1="12" x2="20" y2="3"></line>
                  <line x1="1" y1="14" x2="7" y2="14"></line>
                  <line x1="9" y1="8" x2="15" y2="8"></line>
                  <line x1="17" y1="16" x2="23" y2="16"></line>
                </svg>
                @if (activeFilterCount() > 0) {
                  <span class="absolute -right-1 -top-1 flex h-5 w-5 items-center justify-center rounded-full bg-primary-600 text-[10px] font-bold text-white">
                    {{ activeFilterCount() }}
                  </span>
                }
              </button>
            </div>

            @if (isLoadingTournaments()) {
              <div class="mt-4 rounded-lg border border-neutral-200 bg-white p-5 text-sm text-neutral-600 shadow-sm">Cargando calendario...</div>
            } @else if (tournamentsError()) {
              <div class="mt-4 rounded-lg border border-red-200 bg-red-50 p-5 text-sm text-red-700">{{ tournamentsError() }}</div>
            } @else if (tournamentGroups().length === 0) {
              <div class="mt-4 rounded-lg border border-neutral-200 bg-white p-8 text-center shadow-sm">
                <p class="text-base font-semibold text-neutral-950">No hay torneos</p>
                <p class="mt-1 text-sm text-neutral-600">Ajusta las fechas o filtros para ampliar la busqueda.</p>
              </div>
            } @else {
              <div class="mt-4 space-y-5">
                @for (group of tournamentGroups(); track group.date) {
                  <div class="rounded-lg border border-neutral-200 bg-white shadow-sm">
                    <div class="border-b border-neutral-200 px-5 py-3">
                      <h3 class="text-sm font-bold uppercase tracking-widest text-neutral-500">{{ getDateGroupLabel(group.date) }}</h3>
                    </div>
                    <div class="divide-y divide-neutral-100">
                      @for (tournament of group.tournaments; track tournament.id) {
                        <a
                          [routerLink]="['/torneos', tournament.id]"
                          class="surface-card-bg block rounded-lg px-5 py-4 transition hover:brightness-110"
                          [style.backgroundImage]="'url(' + getSurfaceImage(tournament.surfaceCategory) + ')'"
                        >
                          <div class="flex items-center justify-between gap-3">
                            <div class="flex flex-wrap items-center gap-2">
                              <span class="rounded-full px-3 py-1 text-xs font-semibold bg-white/20 text-white backdrop-blur-sm">{{ getStatusLabel(tournament.status) }}</span>
                              @if (tournament.professionalTournament) {
                                <span class="rounded-full bg-white/25 px-3 py-1 text-xs font-semibold text-white backdrop-blur-sm">PRO</span>
                              }
                            </div>
                            <div class="shrink-0 text-right text-xs text-white/80">
                              <span class="font-semibold text-white drop-shadow-sm">{{ tournament.playStartDate | date: 'dd/MM' }} - {{ tournament.playEndDate | date: 'dd/MM' }}</span>
                              <span class="mx-1.5 text-white/40">|</span>
                              <span class="font-semibold text-white drop-shadow-sm">{{ tournament.tournamentStartTime || 'Por definir' }}</span>
                            </div>
                          </div>
                          <h4 class="mt-2 text-lg font-bold text-white drop-shadow-md">{{ tournament.formalName }}</h4>
                          <p class="mt-1 text-sm text-white/85 drop-shadow-sm">{{ tournament.location }}</p>
                        </a>
                      }
                    </div>
                  </div>
                }
              </div>

              @if (totalTournamentPages() > 1) {
                <nav class="mt-6 flex items-center justify-between rounded-lg border border-neutral-200 bg-white px-4 py-3 shadow-sm">
                  <span class="text-sm text-neutral-600">
                    Pagina {{ currentPage() + 1 }} de {{ totalTournamentPages() }}
                  </span>
                  <div class="flex items-center gap-2">
                    <button
                      type="button"
                      (click)="goToPreviousPage()"
                      [disabled]="!canGoPrevious()"
                      class="h-9 rounded-lg border border-neutral-300 bg-white px-3 text-sm font-semibold text-neutral-700 transition hover:bg-neutral-50 disabled:cursor-not-allowed disabled:opacity-40"
                    >
                      Anterior
                    </button>
                    <button
                      type="button"
                      (click)="goToNextPage()"
                      [disabled]="!canGoNext()"
                      class="h-9 rounded-lg border border-neutral-300 bg-white px-3 text-sm font-semibold text-neutral-700 transition hover:bg-neutral-50 disabled:cursor-not-allowed disabled:opacity-40"
                    >
                      Siguiente
                    </button>
                  </div>
                </nav>
              }
            }
          </section>

          <aside class="min-w-0">
            <div class="rounded-lg border border-neutral-200 bg-white p-5 shadow-sm">
              <div class="flex items-start justify-between gap-3">
                <div>
                  <h2 class="text-xl font-bold text-neutral-950">{{ isOrganizer() ? 'Mis torneos' : 'Mis partidos' }}</h2>
                  <p class="mt-1 text-sm text-neutral-600">{{ isOrganizer() ? 'Torneos que has creado.' : 'Partidos con horario asignado dentro del rango.' }}</p>
                </div>
                <span class="rounded-full bg-neutral-100 px-3 py-1 text-xs font-semibold text-neutral-700">{{ sidebarItems().length }}</span>
              </div>

              @if (!isLoggedIn()) {
                <div class="mt-4 rounded-lg border border-dashed border-neutral-300 bg-neutral-50 p-4 text-sm text-neutral-600">
                  {{ isOrganizer() ? 'Inicia sesion para ver tus torneos.' : 'Inicia sesion para ver tus partidos asignados.' }}
                </div>
              } @else if (isLoadingSidebar()) {
                <div class="mt-4 rounded-lg border border-neutral-200 bg-neutral-50 p-4 text-sm text-neutral-600">{{ isOrganizer() ? 'Cargando torneos...' : 'Cargando partidos...' }}</div>
              } @else if (sidebarError()) {
                <div class="mt-4 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{{ sidebarError() }}</div>
              } @else if (sidebarItems().length === 0) {
                <div class="mt-4 rounded-lg border border-dashed border-neutral-300 bg-neutral-50 p-4 text-sm text-neutral-600">
                  {{ isOrganizer() ? 'No tienes torneos creados en estas fechas.' : 'No tienes partidos programados en estas fechas.' }}
                </div>
              } @else {
                <div class="mt-3 mb-3">
                  <select
                    class="h-9 w-full rounded-lg border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition focus:border-primary-500"
                    [ngModel]="sidebarSortOrder()"
                    (ngModelChange)="sidebarSortOrder.set($event); sortSidebarItems()"
                  >
                    <option value="newest">Mas recientes primero</option>
                    <option value="oldest">Mas antiguos primero</option>
                  </select>
                </div>
                <div class="mt-4 space-y-3">
                  @if (isOrganizer()) {
                    @for (tournament of paginatedSidebarTournaments(); track tournament.id) {
                      <a
                        [routerLink]="['/torneos', tournament.id]"
                        class="surface-card-bg block rounded-lg px-4 py-4 transition hover:brightness-110"
                        [style.backgroundImage]="'url(' + getSurfaceImage(tournament.surfaceCategory) + ')'"
                      >
                        <div class="flex items-center justify-between gap-3">
                          <div class="flex flex-wrap items-center gap-2">
                            <span class="rounded-full px-3 py-1 text-xs font-semibold bg-white/20 text-white backdrop-blur-sm">{{ getStatusLabel(tournament.status) }}</span>
                            @if (tournament.professionalTournament) {
                              <span class="rounded-full bg-white/25 px-3 py-1 text-xs font-semibold text-white backdrop-blur-sm">PRO</span>
                            }
                          </div>
                          <div class="shrink-0 text-right text-xs text-white/80">
                            <span class="font-semibold text-white drop-shadow-sm">{{ tournament.playStartDate | date: 'dd/MM' }} - {{ tournament.playEndDate | date: 'dd/MM' }}</span>
                            <span class="mx-1.5 text-white/40">|</span>
                            <span class="font-semibold text-white drop-shadow-sm">{{ tournament.tournamentStartTime || 'Por definir' }}</span>
                          </div>
                        </div>
                        <h4 class="mt-2 text-lg font-bold text-white drop-shadow-md">{{ tournament.formalName }}</h4>
                        <p class="mt-1 text-sm text-white/85 drop-shadow-sm">{{ tournament.location }}</p>
                      </a>
                    }
                  } @else {
                    @for (match of paginatedSidebarMatches(); track match.matchId) {
                      <a
                        [routerLink]="['/torneos', match.tournamentId]"
                        class="block rounded-lg border border-neutral-200 bg-neutral-50 p-4 transition hover:border-primary-300 hover:bg-primary-50"
                      >
                        <div class="flex items-start justify-between gap-3">
                          <div class="min-w-0">
                            <p class="truncate text-sm font-bold text-neutral-950">{{ match.tournamentName }}</p>
                            <p class="mt-1 truncate text-xs font-medium uppercase tracking-widest text-neutral-500">{{ match.eventName }}</p>
                          </div>
                          <span class="shrink-0 rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-neutral-700">R{{ match.roundNumber }}</span>
                        </div>

                        <div class="mt-3 text-sm text-neutral-700">
                          <p class="font-semibold text-neutral-950">{{ match.firstParticipantName }}</p>
                          <p class="text-neutral-600">{{ match.secondParticipantName }}</p>
                        </div>

                        <div class="mt-3 grid grid-cols-2 gap-3 text-sm">
                          <div>
                            <p class="text-xs uppercase tracking-widest text-neutral-500">Horario</p>
                            <p class="mt-1 font-semibold text-neutral-900">
                              {{ getSchedulePrefix(match.scheduleTimeType) }} {{ match.scheduledAt | date: 'dd/MM HH:mm' }}
                            </p>
                          </div>
                          <div>
                            <p class="text-xs uppercase tracking-widest text-neutral-500">Pista</p>
                            <p class="mt-1 font-semibold text-neutral-900">{{ match.court || 'Por definir' }}</p>
                          </div>
                        </div>
                      </a>
                    }
                  }
                </div>

                @if (totalSidebarPages() > 1) {
                  <nav class="mt-4 flex items-center justify-between border-t border-neutral-200 pt-3">
                    <span class="text-xs text-neutral-500">
                      Pagina {{ currentSidebarPage() + 1 }} de {{ totalSidebarPages() }}
                    </span>
                    <div class="flex items-center gap-1">
                      <button
                        type="button"
                        (click)="sidebarPrevPage()"
                        [disabled]="!canSidebarGoPrevious()"
                        class="h-8 rounded-md border border-neutral-300 bg-white px-2.5 text-xs font-semibold text-neutral-700 transition hover:bg-neutral-50 disabled:cursor-not-allowed disabled:opacity-40"
                      >
                        Ant
                      </button>
                      <button
                        type="button"
                        (click)="sidebarNextPage()"
                        [disabled]="!canSidebarGoNext()"
                        class="h-8 rounded-md border border-neutral-300 bg-white px-2.5 text-xs font-semibold text-neutral-700 transition hover:bg-neutral-50 disabled:cursor-not-allowed disabled:opacity-40"
                      >
                        Sig
                      </button>
                    </div>
                  </nav>
                }
              }
            </div>
          </aside>
        </div>
      </div>
    </section>

    @if (filterPanelOpen()) {
      <div class="fixed inset-0 z-50 flex justify-end">
        <div class="absolute inset-0 bg-black/40" (click)="filterPanelOpen.set(false)"></div>
        <div class="relative flex h-full w-full max-w-md flex-col bg-white shadow-xl">
          <div class="flex items-center justify-between border-b border-neutral-200 px-6 py-4">
            <h2 class="text-lg font-bold text-neutral-950">Filtros</h2>
            <button
              type="button"
              (click)="filterPanelOpen.set(false)"
              class="flex h-9 w-9 items-center justify-center rounded-lg text-neutral-500 transition hover:bg-neutral-100 hover:text-neutral-900"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>

          <div class="flex-1 overflow-y-auto px-6 py-5">
            <form (ngSubmit)="applyFilters()" class="space-y-5">
              <label class="block">
                <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Desde</span>
                <input
                  type="date"
                  class="h-11 w-full rounded-lg border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition focus:border-primary-500"
                  [ngModel]="fromDate()"
                  (ngModelChange)="fromDate.set($event)"
                  name="fromDate"
                />
              </label>

              <label class="block">
                <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Hasta</span>
                <input
                  type="date"
                  class="h-11 w-full rounded-lg border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition focus:border-primary-500"
                  [ngModel]="toDate()"
                  (ngModelChange)="toDate.set($event)"
                  name="toDate"
                />
              </label>

              <label class="block">
                <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Superficie</span>
                <select
                  class="h-11 w-full rounded-lg border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition focus:border-primary-500"
                  [ngModel]="surface() ?? ''"
                  (ngModelChange)="onSurfaceChange($event)"
                  name="surface"
                >
                  <option value="">Todas</option>
                  @for (option of surfaceOptions; track option) {
                    <option [value]="option">{{ getSurfaceLabel(option) }}</option>
                  }
                </select>
              </label>

              <label class="block">
                <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Nombre</span>
                <input
                  type="search"
                  class="h-11 w-full rounded-lg border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition focus:border-primary-500"
                  placeholder="Nombre del torneo"
                  [ngModel]="name()"
                  (ngModelChange)="name.set($event)"
                  name="name"
                />
              </label>

              <label class="block">
                <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Ubicacion</span>
                <input
                  type="search"
                  class="h-11 w-full rounded-lg border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition focus:border-primary-500"
                  placeholder="Club, ciudad o sede"
                  [ngModel]="location()"
                  (ngModelChange)="location.set($event)"
                  name="location"
                />
              </label>

              <label class="block">
                <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Estado</span>
                <select
                  class="h-11 w-full rounded-lg border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition focus:border-primary-500"
                  [ngModel]="status() ?? ''"
                  (ngModelChange)="onStatusChange($event)"
                  name="status"
                >
                  <option value="">Todos</option>
                  @for (option of statusOptions; track option) {
                    <option [value]="option">{{ getStatusLabel(option) }}</option>
                  }
                </select>
              </label>

              <label class="block">
                <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Partido pro</span>
                <select
                  class="h-11 w-full rounded-lg border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition focus:border-primary-500"
                  [ngModel]="professionalTournamentFilter()"
                  (ngModelChange)="onProfessionalTournamentChange($event)"
                  name="professionalTournament"
                >
                  <option value="">Todos</option>
                  <option value="true">PRO</option>
                  <option value="false">No PRO</option>
                </select>
              </label>
            </form>
          </div>

          <div class="flex gap-3 border-t border-neutral-200 px-6 py-4">
            <button
              type="button"
              (click)="resetFilters(); filterPanelOpen.set(false)"
              class="h-11 flex-1 rounded-lg border border-neutral-300 bg-white px-4 text-sm font-semibold text-neutral-700 transition hover:bg-neutral-50"
            >
              Limpiar
            </button>
            <button
              type="button"
              (click)="applyFilters(); filterPanelOpen.set(false)"
              [disabled]="isLoadingTournaments() || isLoadingSidebar()"
              class="h-11 flex-1 rounded-lg bg-primary-600 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700 disabled:cursor-not-allowed disabled:opacity-60"
            >
              Aplicar
            </button>
          </div>
        </div>
      </div>
    }
  `
})
export class CalendarComponent implements OnInit {
  private readonly tournamentService = inject(TournamentService);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly surfaceOptions: TournamentSurfaceCategory[] = ['CLAY', 'HARD', 'GRASS', 'CARPET'];
  readonly statusOptions: TournamentStatus[] = ['DRAFT', 'OPEN', 'CLOSED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];
  readonly fromDate = signal(this.toDateInputValue(new Date()));
  readonly toDate = signal(this.toDateInputValue(this.addDays(new Date(), 90)));
  readonly surface = signal<TournamentSurfaceCategory | null>(null);
  readonly status = signal<TournamentStatus | null>(null);
  readonly professionalTournament = signal<boolean | null>(null);
  readonly professionalTournamentFilter = signal('');
  readonly name = signal('');
  readonly location = signal('');
  readonly tournaments = signal<TournamentCalendarResponse[]>([]);
  readonly myMatches = signal<PlayerMatchCalendarResponse[]>([]);
  readonly myTournaments = signal<TournamentCalendarResponse[]>([]);
  readonly isLoadingTournaments = signal(false);
  readonly isLoadingSidebar = signal(false);
  readonly tournamentsError = signal<string | null>(null);
  readonly sidebarError = signal<string | null>(null);
  readonly dateRangeError = signal<string | null>(null);
  readonly isLoggedIn = signal(false);
  readonly userRole = signal<UserRole | null>(null);
  readonly sidebarSortOrder = signal<'newest' | 'oldest'>('newest');
  readonly filterPanelOpen = signal(false);

  readonly currentPage = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly pageSize = 10;

  readonly sidebarPageSize = 5;
  readonly sidebarPage = signal(0);

  readonly isOrganizer = computed(() => this.userRole() === 'ORGANIZER');

  readonly sidebarItems = computed(() => {
    return this.isOrganizer() ? this.myTournaments() : this.myMatches();
  });

  readonly sortedSidebarTournaments = signal<TournamentCalendarResponse[]>([]);
  readonly sortedSidebarMatches = signal<PlayerMatchCalendarResponse[]>([]);

  readonly tournamentGroups = computed<TournamentCalendarGroup[]>(() => {
    const groups = new Map<string, TournamentCalendarResponse[]>();
    for (const tournament of this.tournaments()) {
      const date = tournament.playStartDate;
      groups.set(date, [...(groups.get(date) ?? []), tournament]);
    }
    return Array.from(groups.entries()).map(([date, tournaments]) => ({
      date,
      tournaments
    }));
  });

  readonly totalTournamentPages = computed(() => this.totalPages());
  readonly canGoPrevious = computed(() => this.currentPage() > 0);
  readonly canGoNext = computed(() => this.currentPage() + 1 < this.totalTournamentPages());

  readonly totalSidebarPages = computed(() => {
    return Math.max(1, Math.ceil(this.sidebarItems().length / this.sidebarPageSize));
  });

  readonly currentSidebarPage = computed(() => {
    return Math.min(this.sidebarPage(), this.totalSidebarPages() - 1);
  });

  readonly canSidebarGoPrevious = computed(() => this.currentSidebarPage() > 0);
  readonly canSidebarGoNext = computed(() => this.currentSidebarPage() + 1 < this.totalSidebarPages());

  readonly paginatedSidebarTournaments = computed(() => {
    const start = this.currentSidebarPage() * this.sidebarPageSize;
    return this.sortedSidebarTournaments().slice(start, start + this.sidebarPageSize);
  });

  readonly paginatedSidebarMatches = computed(() => {
    const start = this.currentSidebarPage() * this.sidebarPageSize;
    return this.sortedSidebarMatches().slice(start, start + this.sidebarPageSize);
  });

  readonly activeFilterCount = computed(() => {
    let count = 0;
    const today = this.toDateInputValue(new Date());
    const defaultTo = this.toDateInputValue(this.addDays(new Date(), 90));
    if (this.fromDate() !== today) count++;
    if (this.toDate() !== defaultTo) count++;
    if (this.surface()) count++;
    if (this.status()) count++;
    if (this.professionalTournament() !== null) count++;
    if (this.name().trim()) count++;
    if (this.location().trim()) count++;
    return count;
  });

  getSurfaceLabel = getTournamentSurfaceCategoryLabel;
  getSurfaceImage = getSurfaceBackgroundImage;

  ngOnInit(): void {
    this.loadTournaments();
    this.authService.isLoggedIn$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(isLoggedIn => {
        this.isLoggedIn.set(isLoggedIn);
        if (isLoggedIn) {
          this.loadSidebarData();
        } else {
          this.myMatches.set([]);
          this.myTournaments.set([]);
          this.sortedSidebarMatches.set([]);
          this.sortedSidebarTournaments.set([]);
          this.sidebarError.set(null);
          this.isLoadingSidebar.set(false);
        }
      });

    this.authService.role$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(role => {
        this.userRole.set(role);
        if (this.isLoggedIn()) {
          this.loadSidebarData();
        }
      });
  }

  applyFilters(): void {
    if (!this.isDateRangeValid()) {
      return;
    }
    this.currentPage.set(0);
    this.sidebarPage.set(0);
    this.loadTournaments();
    if (this.isLoggedIn()) {
      this.loadSidebarData();
    }
  }

  resetFilters(): void {
    this.fromDate.set(this.toDateInputValue(new Date()));
    this.toDate.set(this.toDateInputValue(this.addDays(new Date(), 90)));
    this.surface.set(null);
    this.status.set(null);
    this.professionalTournament.set(null);
    this.professionalTournamentFilter.set('');
    this.name.set('');
    this.location.set('');
    this.applyFilters();
  }

  onSurfaceChange(value: string): void {
    this.surface.set(value ? value as TournamentSurfaceCategory : null);
  }

  onStatusChange(value: string): void {
    this.status.set(value ? value as TournamentStatus : null);
  }

  onProfessionalTournamentChange(value: string): void {
    this.professionalTournamentFilter.set(value);
    this.professionalTournament.set(value === '' ? null : value === 'true');
  }

  goToPreviousPage(): void {
    if (this.canGoPrevious()) {
      this.currentPage.update(p => p - 1);
      this.loadTournaments();
    }
  }

  goToNextPage(): void {
    if (this.canGoNext()) {
      this.currentPage.update(p => p + 1);
      this.loadTournaments();
    }
  }

  sidebarPrevPage(): void {
    if (this.canSidebarGoPrevious()) {
      this.sidebarPage.update(p => p - 1);
    }
  }

  sidebarNextPage(): void {
    if (this.canSidebarGoNext()) {
      this.sidebarPage.update(p => p + 1);
    }
  }

  sortSidebarItems(): void {
    const order = this.sidebarSortOrder();
    this.sidebarPage.set(0);
    if (this.isOrganizer()) {
      const sorted = [...this.myTournaments()].sort((a, b) => {
        const dateA = new Date(a.playStartDate).getTime();
        const dateB = new Date(b.playStartDate).getTime();
        return order === 'newest' ? dateB - dateA : dateA - dateB;
      });
      this.sortedSidebarTournaments.set(sorted);
    } else {
      const sorted = [...this.myMatches()].sort((a, b) => {
        const dateA = new Date(a.scheduledAt).getTime();
        const dateB = new Date(b.scheduledAt).getTime();
        return order === 'newest' ? dateB - dateA : dateA - dateB;
      });
      this.sortedSidebarMatches.set(sorted);
    }
  }

  getDateGroupLabel(date: string): string {
    return new Intl.DateTimeFormat('es-ES', {
      weekday: 'long',
      day: 'numeric',
      month: 'long'
    }).format(new Date(`${date}T00:00:00`));
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      DRAFT: 'Borrador',
      OPEN: 'Inscripciones abiertas',
      ACTIVE: 'Activo',
      CLOSED: 'Inscripciones cerradas',
      IN_PROGRESS: 'En juego',
      COMPLETED: 'Finalizado',
      CANCELLED: 'Cancelado'
    };
    return labels[status] ?? status;
  }

  getStatusColorClasses(status: string): string {
    const colors: Record<string, string> = {
      DRAFT: 'bg-neutral-100 text-neutral-600',
      OPEN: 'bg-blue-100 text-blue-700',
      ACTIVE: 'bg-green-100 text-green-700',
      CLOSED: 'bg-amber-100 text-amber-700',
      IN_PROGRESS: 'bg-sky-100 text-sky-700',
      COMPLETED: 'bg-emerald-100 text-emerald-700',
      CANCELLED: 'bg-red-100 text-red-700'
    };
    return colors[status] ?? 'bg-primary-50 text-primary-700';
  }

  getSchedulePrefix(scheduleTimeType: string | null | undefined): string {
    return scheduleTimeType === 'NOT_BEFORE' ? 'No antes de' : 'A las';
  }

  private loadTournaments(): void {
    if (!this.isDateRangeValid()) {
      return;
    }

    this.isLoadingTournaments.set(true);
    this.tournamentsError.set(null);

    this.tournamentService.getPublishedTournamentCalendar({
      ...this.currentFilters(),
      page: this.currentPage(),
      size: this.pageSize
    }).subscribe({
      next: result => {
        this.tournaments.set(result.content);
        this.totalElements.set(result.totalElements);
        this.totalPages.set(result.totalPages);
        this.isLoadingTournaments.set(false);
      },
      error: error => {
        this.tournamentsError.set(getApiErrorMessage(error, 'No se pudo cargar el calendario de torneos.'));
        this.isLoadingTournaments.set(false);
      }
    });
  }

  private loadSidebarData(): void {
    if (!this.isDateRangeValid()) {
      return;
    }

    this.isLoadingSidebar.set(true);
    this.sidebarError.set(null);

    if (this.isOrganizer()) {
      this.loadMyTournaments();
    } else {
      this.loadMyMatches();
    }
  }

  private loadMyMatches(): void {
    this.tournamentService.getMyMatchCalendar(this.currentFilters()).subscribe({
      next: matches => {
        this.myMatches.set(matches);
        this.sortSidebarItems();
        this.isLoadingSidebar.set(false);
      },
      error: error => {
        this.sidebarError.set(getApiErrorMessage(error, 'No se pudieron cargar tus partidos.'));
        this.isLoadingSidebar.set(false);
      }
    });
  }

  private loadMyTournaments(): void {
    this.tournamentService.getMyTournamentCalendar(this.currentFilters()).subscribe({
      next: tournaments => {
        this.myTournaments.set(tournaments);
        this.sortSidebarItems();
        this.isLoadingSidebar.set(false);
      },
      error: error => {
        this.sidebarError.set(getApiErrorMessage(error, 'No se pudieron cargar tus torneos.'));
        this.isLoadingSidebar.set(false);
      }
    });
  }

  private currentFilters(): TournamentCalendarFilters {
    return {
      from: this.fromDate(),
      to: this.toDate(),
      surface: this.surface(),
      location: this.location(),
      name: this.name(),
      professionalTournament: this.professionalTournament(),
      status: this.status()
    };
  }

  private isDateRangeValid(): boolean {
    if (this.fromDate() && this.toDate() && this.toDate() < this.fromDate()) {
      this.dateRangeError.set('La fecha final debe ser posterior a la fecha inicial.');
      return false;
    }
    this.dateRangeError.set(null);
    return true;
  }

  private addDays(date: Date, days: number): Date {
    const nextDate = new Date(date);
    nextDate.setDate(nextDate.getDate() + days);
    return nextDate;
  }

  private toDateInputValue(date: Date): string {
    const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return localDate.toISOString().slice(0, 10);
  }
}
