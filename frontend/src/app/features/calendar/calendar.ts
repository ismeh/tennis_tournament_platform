import { CommonModule, DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../../core/auth/auth.service';
import { getApiErrorMessage } from '../../core/errors/api-error.util';
import {
  PlayerMatchCalendarResponse,
  TournamentCalendarFilters,
  TournamentCalendarResponse,
  TournamentSurfaceCategory,
  getTournamentSurfaceCategoryLabel
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
        <header class="flex flex-col gap-4 border-b border-neutral-200 pb-6 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-600">Calendario</p>
            <h1 class="mt-2 text-3xl font-black text-neutral-950 sm:text-4xl">Torneos y partidos</h1>
            <p class="mt-2 max-w-2xl text-sm text-neutral-600">
              Próximos torneos publicados y horarios asignados a tus partidos.
            </p>
          </div>

          <a
            routerLink="/torneos"
            class="inline-flex w-fit items-center justify-center rounded-xl border border-neutral-300 bg-white px-4 py-2 text-sm font-semibold text-neutral-800 shadow-sm transition hover:border-primary-300 hover:text-primary-700"
          >
            Gestionar torneos
          </a>
        </header>

        <form class="mt-6 grid gap-3 rounded-lg border border-neutral-200 bg-white p-4 shadow-sm lg:grid-cols-[1fr_1fr_1fr_1.4fr_auto]" (ngSubmit)="applyFilters()">
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
            <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Ubicación</span>
            <input
              type="search"
              class="h-11 w-full rounded-lg border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition focus:border-primary-500"
              placeholder="Club, ciudad o sede"
              [ngModel]="location()"
              (ngModelChange)="location.set($event)"
              name="location"
            />
          </label>

          <div class="flex items-end gap-2">
            <button
              type="submit"
              class="h-11 rounded-lg bg-primary-600 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700 disabled:cursor-not-allowed disabled:opacity-60"
              [disabled]="isLoadingTournaments() || isLoadingMatches()"
            >
              Filtrar
            </button>
            <button
              type="button"
              class="h-11 rounded-lg border border-neutral-300 bg-white px-4 text-sm font-semibold text-neutral-700 transition hover:bg-neutral-50"
              (click)="resetFilters()"
            >
              Limpiar
            </button>
          </div>
        </form>

        @if (dateRangeError()) {
          <div class="mt-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{{ dateRangeError() }}</div>
        }

        <div class="mt-6 grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
          <section class="min-w-0">
            <div class="flex items-center justify-between gap-4">
              <div>
                <h2 class="text-xl font-bold text-neutral-950">Próximos torneos publicados</h2>
                <p class="mt-1 text-sm text-neutral-600">{{ tournaments().length }} torneos en la ventana seleccionada</p>
              </div>
            </div>

            @if (isLoadingTournaments()) {
              <div class="mt-4 rounded-lg border border-neutral-200 bg-white p-5 text-sm text-neutral-600 shadow-sm">Cargando calendario...</div>
            } @else if (tournamentsError()) {
              <div class="mt-4 rounded-lg border border-red-200 bg-red-50 p-5 text-sm text-red-700">{{ tournamentsError() }}</div>
            } @else if (tournamentGroups().length === 0) {
              <div class="mt-4 rounded-lg border border-neutral-200 bg-white p-8 text-center shadow-sm">
                <p class="text-base font-semibold text-neutral-950">No hay torneos publicados</p>
                <p class="mt-1 text-sm text-neutral-600">Ajusta las fechas o filtros para ampliar la búsqueda.</p>
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
                          class="grid gap-4 px-5 py-4 transition hover:bg-primary-50/60 md:grid-cols-[1fr_auto]"
                        >
                          <div class="min-w-0">
                            <div class="flex flex-wrap items-center gap-2">
                              <span class="rounded-full bg-primary-50 px-3 py-1 text-xs font-semibold text-primary-700">{{ getStatusLabel(tournament.status) }}</span>
                              <span class="text-xs font-medium text-neutral-500">{{ getSurfaceLabel(tournament.surfaceCategory) }}</span>
                            </div>
                            <h4 class="mt-2 truncate text-lg font-bold text-neutral-950">{{ tournament.formalName }}</h4>
                            <p class="mt-1 text-sm text-neutral-600">{{ tournament.location }}</p>
                          </div>

                          <div class="grid grid-cols-2 gap-3 text-sm md:min-w-52">
                            <div>
                              <p class="text-xs uppercase tracking-widest text-neutral-500">Juego</p>
                              <p class="mt-1 font-semibold text-neutral-900">{{ tournament.playStartDate | date: 'dd/MM' }} - {{ tournament.playEndDate | date: 'dd/MM' }}</p>
                            </div>
                            <div>
                              <p class="text-xs uppercase tracking-widest text-neutral-500">Inicio</p>
                              <p class="mt-1 font-semibold text-neutral-900">{{ tournament.tournamentStartTime || 'Por definir' }}</p>
                            </div>
                          </div>
                        </a>
                      }
                    </div>
                  </div>
                }
              </div>
            }
          </section>

          <aside class="min-w-0">
            <div class="rounded-lg border border-neutral-200 bg-white p-5 shadow-sm">
              <div class="flex items-start justify-between gap-3">
                <div>
                  <h2 class="text-xl font-bold text-neutral-950">Mis partidos</h2>
                  <p class="mt-1 text-sm text-neutral-600">Partidos con horario asignado dentro del rango.</p>
                </div>
                <span class="rounded-full bg-neutral-100 px-3 py-1 text-xs font-semibold text-neutral-700">{{ myMatches().length }}</span>
              </div>

              @if (!isLoggedIn()) {
                <div class="mt-4 rounded-lg border border-dashed border-neutral-300 bg-neutral-50 p-4 text-sm text-neutral-600">
                  Inicia sesión para ver tus partidos asignados.
                </div>
              } @else if (isLoadingMatches()) {
                <div class="mt-4 rounded-lg border border-neutral-200 bg-neutral-50 p-4 text-sm text-neutral-600">Cargando partidos...</div>
              } @else if (matchesError()) {
                <div class="mt-4 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{{ matchesError() }}</div>
              } @else if (myMatches().length === 0) {
                <div class="mt-4 rounded-lg border border-dashed border-neutral-300 bg-neutral-50 p-4 text-sm text-neutral-600">
                  No tienes partidos programados en estas fechas.
                </div>
              } @else {
                <div class="mt-4 space-y-3">
                  @for (match of myMatches(); track match.matchId) {
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
                </div>
              }
            </div>
          </aside>
        </div>
      </div>
    </section>
  `
})
export class CalendarComponent implements OnInit {
  private readonly tournamentService = inject(TournamentService);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly surfaceOptions: TournamentSurfaceCategory[] = ['CLAY', 'HARD', 'GRASS', 'CARPET'];
  readonly fromDate = signal(this.toDateInputValue(new Date()));
  readonly toDate = signal(this.toDateInputValue(this.addDays(new Date(), 90)));
  readonly surface = signal<TournamentSurfaceCategory | null>(null);
  readonly location = signal('');
  readonly tournaments = signal<TournamentCalendarResponse[]>([]);
  readonly myMatches = signal<PlayerMatchCalendarResponse[]>([]);
  readonly isLoadingTournaments = signal(false);
  readonly isLoadingMatches = signal(false);
  readonly tournamentsError = signal<string | null>(null);
  readonly matchesError = signal<string | null>(null);
  readonly dateRangeError = signal<string | null>(null);
  readonly isLoggedIn = signal(false);

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

  getSurfaceLabel = getTournamentSurfaceCategoryLabel;

  ngOnInit(): void {
    this.loadTournaments();
    this.authService.isLoggedIn$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(isLoggedIn => {
        this.isLoggedIn.set(isLoggedIn);
        if (isLoggedIn) {
          this.loadMatches();
        } else {
          this.myMatches.set([]);
          this.matchesError.set(null);
          this.isLoadingMatches.set(false);
        }
      });
  }

  applyFilters(): void {
    if (!this.isDateRangeValid()) {
      return;
    }

    this.loadTournaments();
    if (this.isLoggedIn()) {
      this.loadMatches();
    }
  }

  resetFilters(): void {
    this.fromDate.set(this.toDateInputValue(new Date()));
    this.toDate.set(this.toDateInputValue(this.addDays(new Date(), 90)));
    this.surface.set(null);
    this.location.set('');
    this.applyFilters();
  }

  onSurfaceChange(value: string): void {
    this.surface.set(value ? value as TournamentSurfaceCategory : null);
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
      OPEN: 'Inscripciones abiertas',
      CLOSED: 'Inscripciones cerradas',
      IN_PROGRESS: 'En juego'
    };

    return labels[status] ?? status;
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

    this.tournamentService.getPublishedTournamentCalendar(this.currentFilters()).subscribe({
      next: tournaments => {
        this.tournaments.set(tournaments);
        this.isLoadingTournaments.set(false);
      },
      error: error => {
        this.tournamentsError.set(getApiErrorMessage(error, 'No se pudo cargar el calendario de torneos.'));
        this.isLoadingTournaments.set(false);
      }
    });
  }

  private loadMatches(): void {
    if (!this.isDateRangeValid()) {
      return;
    }

    this.isLoadingMatches.set(true);
    this.matchesError.set(null);

    this.tournamentService.getMyMatchCalendar(this.currentFilters()).subscribe({
      next: matches => {
        this.myMatches.set(matches);
        this.isLoadingMatches.set(false);
      },
      error: error => {
        this.matchesError.set(getApiErrorMessage(error, 'No se pudieron cargar tus partidos.'));
        this.isLoadingMatches.set(false);
      }
    });
  }

  private currentFilters(): TournamentCalendarFilters {
    return {
      from: this.fromDate(),
      to: this.toDate(),
      surface: this.surface(),
      location: this.location()
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
