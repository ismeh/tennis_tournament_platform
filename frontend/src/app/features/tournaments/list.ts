import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { getTournamentSurfaceCategoryLabel, TournamentResponse } from '../../data/interfaces/tournament.model';
import { TournamentService } from '../../data/services/tournament.service';
import { getApiErrorMessage } from '../../core/errors/api-error.util';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-tournaments-list-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="relative overflow-hidden bg-gradient-to-b from-neutral-50 via-white to-white py-10 sm:py-14">
      <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <header class="mb-8">
          <p class="text-sm font-semibold uppercase tracking-[0.22em] text-primary-600">Torneos</p>
          <h1 class="mt-2 text-3xl font-black text-neutral-900 sm:text-4xl">Calendario de torneos</h1>
          <p class="mt-3 text-neutral-600">Consulta los torneos del club y prepara uno nuevo cuando lo necesites.</p>
        </header>

        <a
          routerLink="/torneos/crear"
          class="group relative mb-8 block overflow-hidden rounded-3xl border border-primary-200 bg-gradient-to-r from-primary-600 to-accent-600 p-8 text-white shadow-xl transition-transform hover:scale-[1.01]"
        >
          <div class="absolute -right-8 -top-10 h-36 w-36 rounded-full bg-white/15 blur-2xl"></div>
          <div class="absolute -bottom-10 left-0 h-32 w-32 rounded-full bg-white/10 blur-2xl"></div>
          <div class="relative flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.2em] text-white/80">Gestión</p>
              <h2 class="mt-2 text-2xl font-extrabold">Crear torneo</h2>
              <p class="mt-2 text-white/90">Define fechas, superficie, pistas y límite de inscritos.</p>
            </div>
            <span class="inline-flex items-center rounded-full border border-white/50 bg-white/10 px-5 py-3 font-semibold">
              Nuevo torneo
            </span>
          </div>
        </a>

        <div class="grid grid-cols-1 gap-8 lg:grid-cols-4">
          <div [ngClass]="{'lg:col-span-3': isUmpire(), 'lg:col-span-4': !isUmpire()}">
            @if (isLoading()) {
              <div class="rounded-2xl border border-neutral-200 bg-white p-6 text-neutral-600">Cargando torneos...</div>
            } @else if (errorMessage()) {
              <div class="rounded-2xl border border-red-200 bg-red-50 p-6 text-red-700">{{ errorMessage() }}</div>
            } @else if (tournaments().length === 0) {
              <div class="rounded-2xl border border-neutral-200 bg-white p-8 text-center">
                <p class="text-lg font-semibold text-neutral-900">Aún no se han creado torneos</p>
                <p class="mt-2 text-neutral-600">Crea el primer torneo para abrir pruebas, inscripciones y cuadros.</p>
              </div>
            } @else {
              <div class="grid gap-4 sm:grid-cols-2" [ngClass]="{'lg:grid-cols-2': isUmpire(), 'lg:grid-cols-3': !isUmpire()}">
                @for (tournament of tournaments(); track tournament.id) {
                  <a
                    [routerLink]="['/torneos', tournament.id]"
                    class="group block rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm transition-all hover:-translate-y-0.5 hover:border-primary-300 hover:shadow-md"
                  >
                    <div class="flex flex-wrap items-center gap-2">
                      <p class="text-xs font-semibold uppercase tracking-widest text-primary-600">{{ getStatusLabel(tournament.status) }}</p>
                      @if (tournament.professionalTournament) {
                        <span class="rounded-full bg-neutral-900 px-2.5 py-1 text-xs font-bold uppercase tracking-widest text-white">
                          PRO
                        </span>
                      }
                    </div>
                    <h3 class="mt-2 text-lg font-bold text-neutral-900">{{ tournament.formalName }}</h3>
                    <p class="mt-1 text-sm text-neutral-600">{{ tournament.location }}</p>

                    <div class="mt-4 space-y-1 text-sm text-neutral-700">
                      <p><span class="font-medium">Superficie:</span> {{ getSurfaceLabel(tournament.surfaceCategory) }}</p>
                      <p><span class="font-medium">Límite de inscritos:</span> {{ tournament.maxPlayers }}</p>
                      <p><span class="font-medium">Fechas de juego:</span> {{ tournament.playStartDate }} - {{ tournament.playEndDate }}</p>
                    </div>

                    <p class="mt-4 text-sm font-semibold text-primary-700 transition-colors group-hover:text-primary-800">Abrir torneo -></p>
                  </a>
                }
              </div>
            }
          </div>

          @if (isUmpire()) {
            <aside class="lg:col-span-1 space-y-6">
              <div class="rounded-3xl border border-primary-100 bg-primary-50/30 p-6 shadow-sm">
                <div class="flex items-center gap-2 mb-4">
                  <svg class="h-5 w-5 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                  </svg>
                  <h2 class="text-lg font-bold text-neutral-900">Mis Arbitrajes</h2>
                </div>

                @if (isLoadingUmpire()) {
                  <div class="text-sm text-neutral-600">Cargando tus torneos...</div>
                } @else if (umpireTournaments().length === 0) {
                  <p class="text-sm text-neutral-500">No tienes torneos asignados para arbitrar.</p>
                } @else {
                  <div class="space-y-3">
                    @for (ut of umpireTournaments(); track ut.id) {
                      <a
                        [routerLink]="['/torneos', ut.id]"
                        class="block p-4 rounded-2xl bg-white border border-neutral-200 shadow-xs hover:border-primary-400 hover:shadow-sm transition-all"
                      >
                        <h3 class="text-sm font-bold text-neutral-900 line-clamp-2 hover:text-primary-700 transition-colors">{{ ut.formalName }}</h3>
                        <p class="text-xs text-neutral-500 mt-1">{{ ut.location }}</p>
                        <div class="flex justify-between items-center mt-3 text-xs text-neutral-600">
                          <span>{{ ut.playStartDate }}</span>
                          <span class="px-2 py-0.5 rounded-full bg-primary-50 text-primary-700 font-semibold">Árbitro</span>
                        </div>
                      </a>
                    }
                  </div>
                }
              </div>
            </aside>
          }
        </div>
      </div>
    </section>
  `
})
export class TournamentsListComponent implements OnInit {
  private readonly tournamentService = inject(TournamentService);
  private readonly authService = inject(AuthService);

  readonly tournaments = signal<TournamentResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly umpireTournaments = signal<TournamentResponse[]>([]);
  readonly isLoadingUmpire = signal(false);
  readonly isUmpire = signal(false);

  getSurfaceLabel = getTournamentSurfaceCategoryLabel;

  getStatusLabel(status: TournamentResponse['status']): string {
    const labels: Record<TournamentResponse['status'], string> = {
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

  ngOnInit(): void {
    this.tournamentService.getTournaments().subscribe({
      next: data => {
        this.tournaments.set(data);
        this.isLoading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(getApiErrorMessage(error, 'No se pudo cargar el listado de torneos.'));
        this.isLoading.set(false);
      }
    });

    this.authService.role$.subscribe(role => {
      const umpire = role === 'UMPIRE';
      this.isUmpire.set(umpire);
      if (umpire) {
        this.isLoadingUmpire.set(true);
        this.tournamentService.getUmpireTournaments().subscribe({
          next: data => {
            this.umpireTournaments.set(data);
            this.isLoadingUmpire.set(false);
          },
          error: () => {
            this.isLoadingUmpire.set(false);
          }
        });
      }
    });
  }
}
