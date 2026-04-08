import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { TournamentResponse, getTournamentSurfaceCategoryLabel } from '../../data/interfaces/tournament.model';
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

  readonly tournament = signal<TournamentResponse | null>(null);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly actionMessage = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly isSubmittingInscription = signal(false);
  readonly activeSection = signal<TournamentDetailSection>('overview');
  readonly currentMemberId = signal<string | null>(null);

  readonly isCreator = computed(() => {
    const tournament = this.tournament();
    const memberId = this.currentMemberId();

    if (!tournament?.providerOrganisationId || !memberId) {
      return false;
    }

    return tournament.providerOrganisationId === memberId;
  });

  readonly canRequestInscription = computed(() => {
    const currentTournament = this.tournament();
    if (!currentTournament) {
      return false;
    }

    return currentTournament.status === 'OPEN' || currentTournament.status === 'ACTIVE';
  });

  readonly getSurfaceLabel = getTournamentSurfaceCategoryLabel;

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

  requestInscription(): void {
    const currentTournament = this.tournament();
    if (!currentTournament) {
      return;
    }

    if (!this.canRequestInscription()) {
      this.actionError.set('Las inscripciones aun no estan habilitadas para este torneo.');
      return;
    }

    this.isSubmittingInscription.set(true);
    this.actionError.set(null);
    this.actionMessage.set(null);

    this.tournamentService.requestInscription(currentTournament.id).subscribe({
      next: () => {
        this.isSubmittingInscription.set(false);
        this.actionMessage.set('Inscripcion realizada correctamente.');
      },
      error: () => {
        this.isSubmittingInscription.set(false);
        this.actionError.set('No se pudo completar la inscripcion para este torneo.');
      }
    });
  }

  private loadTournament(tournamentId: string): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.tournamentService.getTournamentById(tournamentId).subscribe({
      next: tournament => {
        this.tournament.set(tournament);
        this.isLoading.set(false);
        this.activeSection.set(this.isCreator() ? 'setup' : 'overview');
      },
      error: () => {
        this.errorMessage.set('No se pudo cargar el detalle del torneo.');
        this.isLoading.set(false);
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
  }
}