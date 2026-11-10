import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { take } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { getApiErrorMessage } from '../../core/errors/api-error.util';
import {
  ClubMembershipResponse,
  ClubPageResponse,
  ClubRankingResponse,
  ClubTournamentSummary,
} from '../../data/interfaces/club.model';
import { ClubService } from '../../data/services/club.service';

@Component({
  selector: 'app-club-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="min-h-[calc(100vh-4rem)] bg-neutral-50 pb-16">
      @if (isLoading()) {
        <div class="flex justify-center py-24">
          <div
            class="h-8 w-8 animate-spin rounded-full border-2 border-primary-500 border-t-transparent"
          ></div>
        </div>
      } @else if (errorMessage()) {
        <div class="mx-auto max-w-3xl px-4 py-16">
          <div class="rounded-2xl border border-red-200 bg-red-50 p-8 text-center">
            <p class="text-lg font-semibold text-red-800">{{ errorMessage() }}</p>
            <a
              routerLink="/clubes"
              class="mt-4 inline-flex rounded-lg bg-white px-4 py-2 text-sm font-semibold text-red-700 shadow-sm"
            >
              Volver al directorio
            </a>
          </div>
        </div>
      } @else if (club(); as currentClub) {
        <div
          class="relative h-56 w-full overflow-hidden bg-neutral-900 sm:h-72"
          [style.background]="currentClub.bannerUrl ? null : getHeroGradient(currentClub)"
        >
          @if (currentClub.bannerUrl) {
            <img
              [src]="currentClub.bannerUrl"
              [alt]="currentClub.name"
              class="h-full w-full object-cover opacity-60"
            />
          }
          <div
            class="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent"
          ></div>
        </div>

        <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div
            class="-mt-16 flex flex-col gap-6 sm:-mt-20 sm:flex-row sm:items-end sm:justify-between"
          >
            <div class="flex items-end gap-4">
              <div
                class="flex h-24 w-24 items-center justify-center overflow-hidden rounded-3xl border-4 border-white bg-white shadow-lg sm:h-32 sm:w-32"
              >
                @if (currentClub.logoUrl) {
                  <img
                    [src]="currentClub.logoUrl"
                    [alt]="currentClub.name"
                    class="h-full w-full object-cover"
                  />
                } @else {
                  <span class="text-4xl font-black text-neutral-700">{{
                    getInitial(currentClub.name)
                  }}</span>
                }
              </div>
              <div class="pb-1">
                <h1 class="text-2xl font-black text-white drop-shadow sm:text-4xl">
                  {{ currentClub.name }}
                </h1>
                <p class="mt-1 text-sm font-medium text-white/80 sm:text-base">
                  {{ currentClub.tagline || 'Club de tenis' }}
                </p>
              </div>
            </div>

            <div class="flex flex-wrap gap-2 pb-2">
              @if (membership(); as myMembership) {
                <a
                  [routerLink]="['/clubes', currentClub.id, 'gestion']"
                  class="inline-flex items-center rounded-xl px-4 py-2.5 text-sm font-bold text-white shadow-lg transition hover:opacity-90"
                  [style.background]="currentClub.primaryColor || '#16a34a'"
                >
                  Gestionar club
                </a>
              }
              <a
                routerLink="/clubes"
                class="inline-flex items-center rounded-xl border border-white/40 bg-white/10 px-4 py-2.5 text-sm font-semibold text-white backdrop-blur transition hover:bg-white/20"
              >
                Directorio
              </a>
            </div>
          </div>

          <div class="mt-8 grid gap-6 lg:grid-cols-[1fr_20rem]">
            <div class="space-y-6">
              <article class="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
                <h2 class="text-xl font-bold text-neutral-950">Sobre el club</h2>
                <p class="mt-3 whitespace-pre-line text-neutral-700">
                  {{
                    currentClub.description || 'Este club todavía no ha añadido una descripción.'
                  }}
                </p>
                <div class="mt-6 grid gap-3 sm:grid-cols-3">
                  <div class="rounded-xl bg-neutral-50 px-4 py-3">
                    <p class="text-xs font-semibold uppercase tracking-wide text-neutral-500">
                      Miembros
                    </p>
                    <p class="mt-1 text-2xl font-black text-neutral-950">
                      {{ currentClub.memberCount }}
                    </p>
                  </div>
                  <div class="rounded-xl bg-neutral-50 px-4 py-3">
                    <p class="text-xs font-semibold uppercase tracking-wide text-neutral-500">
                      Jugadores ranking
                    </p>
                    <p class="mt-1 text-2xl font-black text-neutral-950">{{ ranking().length }}</p>
                  </div>
                  <div class="rounded-xl bg-neutral-50 px-4 py-3">
                    <p class="text-xs font-semibold uppercase tracking-wide text-neutral-500">
                      Torneos
                    </p>
                    <p class="mt-1 text-2xl font-black text-neutral-950">
                      {{ tournaments().length }}
                    </p>
                  </div>
                </div>
              </article>

              <article class="rounded-2xl border border-neutral-200 bg-white shadow-sm">
                <div
                  class="flex flex-col gap-3 border-b border-neutral-200 px-6 py-5 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div>
                    <h2 class="text-xl font-bold text-neutral-950">Ranking del club</h2>
                    <p class="mt-1 text-sm text-neutral-600">
                      Clasificación interna por puntos y victorias.
                    </p>
                  </div>
                  <select
                    class="h-10 rounded-lg border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none focus:border-primary-500"
                    [value]="rankingGender()"
                    (change)="onRankingGenderChange($event)"
                  >
                    <option value="">Todos</option>
                    <option value="MALE">Masculino</option>
                    <option value="FEMALE">Femenino</option>
                    <option value="MIXED">Mixto</option>
                  </select>
                </div>

                @if (ranking().length === 0) {
                  <div class="p-8 text-center text-sm text-neutral-600">
                    Aún no hay resultados de jugadores del club.
                  </div>
                } @else {
                  <div class="overflow-x-auto">
                    <table class="min-w-full divide-y divide-neutral-200 text-sm">
                      <thead
                        class="bg-neutral-50 text-left text-xs font-semibold uppercase tracking-widest text-neutral-500"
                      >
                        <tr>
                          <th class="px-6 py-3">#</th>
                          <th class="px-6 py-3">Jugador</th>
                          <th class="px-6 py-3 text-right">Puntos</th>
                          <th class="px-6 py-3 text-right">Victorias</th>
                          <th class="px-6 py-3 text-right">Torneos</th>
                        </tr>
                      </thead>
                      <tbody class="divide-y divide-neutral-100">
                        @for (player of ranking(); track player.personId) {
                          <tr class="hover:bg-neutral-50">
                            <td class="px-6 py-4 text-base font-black text-neutral-950">
                              {{ player.position }}
                            </td>
                            <td class="px-6 py-4">
                              <p class="font-semibold text-neutral-950">
                                {{ getPlayerName(player) }}
                              </p>
                              <p class="mt-0.5 text-xs text-neutral-500">
                                {{ player.license || 'Sin licencia' }}
                              </p>
                            </td>
                            <td class="px-6 py-4 text-right font-semibold text-neutral-950">
                              {{ player.points }}
                            </td>
                            <td class="px-6 py-4 text-right text-neutral-700">
                              {{ player.victories }}
                            </td>
                            <td class="px-6 py-4 text-right text-neutral-700">
                              {{ player.tournaments }}
                            </td>
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>
                }
              </article>

              <article class="rounded-2xl border border-neutral-200 bg-white shadow-sm">
                <div class="border-b border-neutral-200 px-6 py-5">
                  <h2 class="text-xl font-bold text-neutral-950">Torneos del club</h2>
                  <p class="mt-1 text-sm text-neutral-600">
                    Competiciones organizadas por el club.
                  </p>
                </div>
                @if (tournaments().length === 0) {
                  <div class="p-8 text-center text-sm text-neutral-600">
                    Todavía no hay torneos asociados a este club.
                  </div>
                } @else {
                  <ul class="divide-y divide-neutral-100">
                    @for (tournament of tournaments(); track tournament.id) {
                      <li>
                        <a
                          [routerLink]="['/torneos', tournament.id]"
                          class="flex items-center justify-between gap-4 px-6 py-4 transition hover:bg-neutral-50"
                        >
                          <div>
                            <p class="font-semibold text-neutral-950">
                              {{ tournament.formalName }}
                            </p>
                            <p class="mt-0.5 text-xs text-neutral-500">
                              {{ tournament.playStartDate }} · {{ tournament.location }}
                            </p>
                          </div>
                          <span
                            class="rounded-full bg-neutral-100 px-3 py-1 text-xs font-semibold text-neutral-600"
                          >
                            {{ getStatusLabel(tournament.status) }}
                          </span>
                        </a>
                      </li>
                    }
                  </ul>
                }
              </article>
            </div>

            <aside class="space-y-6">
              <section class="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
                <h2 class="text-lg font-bold text-neutral-950">Información</h2>
                <dl class="mt-4 space-y-3 text-sm">
                  @if (currentClub.city || currentClub.country) {
                    <div>
                      <dt class="text-xs font-semibold uppercase tracking-wide text-neutral-500">
                        Ubicación
                      </dt>
                      <dd class="mt-0.5 text-neutral-800">
                        {{ currentClub.city
                        }}{{ currentClub.country ? ', ' + currentClub.country : '' }}
                      </dd>
                    </div>
                  }
                  @if (currentClub.foundedYear) {
                    <div>
                      <dt class="text-xs font-semibold uppercase tracking-wide text-neutral-500">
                        Fundado en
                      </dt>
                      <dd class="mt-0.5 text-neutral-800">{{ currentClub.foundedYear }}</dd>
                    </div>
                  }
                  @if (currentClub.contactEmail) {
                    <div>
                      <dt class="text-xs font-semibold uppercase tracking-wide text-neutral-500">
                        Email
                      </dt>
                      <dd class="mt-0.5 break-all text-neutral-800">
                        <a
                          [href]="'mailto:' + currentClub.contactEmail"
                          class="hover:text-primary-700"
                          >{{ currentClub.contactEmail }}</a
                        >
                      </dd>
                    </div>
                  }
                  @if (currentClub.phone) {
                    <div>
                      <dt class="text-xs font-semibold uppercase tracking-wide text-neutral-500">
                        Teléfono
                      </dt>
                      <dd class="mt-0.5 text-neutral-800">{{ currentClub.phone }}</dd>
                    </div>
                  }
                </dl>

                <div class="mt-5 flex flex-wrap gap-2">
                  @if (currentClub.website) {
                    <a
                      [href]="currentClub.website"
                      target="_blank"
                      rel="noopener"
                      class="rounded-lg border border-neutral-300 px-3 py-1.5 text-xs font-semibold text-neutral-700 hover:bg-neutral-50"
                      >Web</a
                    >
                  }
                  @if (currentClub.instagramUrl) {
                    <a
                      [href]="currentClub.instagramUrl"
                      target="_blank"
                      rel="noopener"
                      class="rounded-lg border border-neutral-300 px-3 py-1.5 text-xs font-semibold text-neutral-700 hover:bg-neutral-50"
                      >Instagram</a
                    >
                  }
                  @if (currentClub.facebookUrl) {
                    <a
                      [href]="currentClub.facebookUrl"
                      target="_blank"
                      rel="noopener"
                      class="rounded-lg border border-neutral-300 px-3 py-1.5 text-xs font-semibold text-neutral-700 hover:bg-neutral-50"
                      >Facebook</a
                    >
                  }
                  @if (currentClub.xUrl) {
                    <a
                      [href]="currentClub.xUrl"
                      target="_blank"
                      rel="noopener"
                      class="rounded-lg border border-neutral-300 px-3 py-1.5 text-xs font-semibold text-neutral-700 hover:bg-neutral-50"
                      >X</a
                    >
                  }
                </div>
              </section>

              <section class="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
                <h2 class="text-lg font-bold text-neutral-950">Comparte el club</h2>
                <p class="mt-2 text-sm text-neutral-600">Dirección pública:</p>
                <code
                  class="mt-2 block break-all rounded-lg bg-neutral-100 px-3 py-2 text-xs text-neutral-700"
                  >/clubes/{{ currentClub.slug }}</code
                >
              </section>
            </aside>
          </div>
        </div>
      }
    </section>
  `,
})
export class ClubDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly clubService = inject(ClubService);
  private readonly authService = inject(AuthService);

  readonly club = signal<ClubPageResponse | null>(null);
  readonly ranking = signal<ClubRankingResponse[]>([]);
  readonly tournaments = signal<ClubTournamentSummary[]>([]);
  readonly membership = signal<ClubMembershipResponse | null>(null);
  readonly rankingGender = signal('');
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug') ?? '';
    this.loadClub(slug);
  }

  onRankingGenderChange(event: Event): void {
    const gender = (event.target as HTMLSelectElement).value;
    this.rankingGender.set(gender);
    this.loadRanking(gender);
  }

  getInitial(name: string): string {
    return name?.charAt(0)?.toUpperCase() || 'C';
  }

  getHeroGradient(club: ClubPageResponse): string {
    const primary = club.primaryColor || '#16a34a';
    const secondary = club.secondaryColor || '#0284c7';
    return `linear-gradient(135deg, ${primary} 0%, ${secondary} 100%)`;
  }

  getPlayerName(player: ClubRankingResponse): string {
    return [player.firstName, player.lastName].filter(Boolean).join(' ') || 'Jugador sin nombre';
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      DRAFT: 'Borrador',
      OPEN: 'Inscripciones abiertas',
      CLOSED: 'Inscripciones cerradas',
      IN_PROGRESS: 'En juego',
      COMPLETED: 'Finalizado',
      CANCELLED: 'Cancelado',
    };
    return labels[status] ?? status;
  }

  private loadClub(slug: string): void {
    if (!slug) {
      this.errorMessage.set('No se indicó ningún club.');
      this.isLoading.set(false);
      return;
    }

    this.isLoading.set(true);
    this.clubService.getPublicClub(slug).subscribe({
      next: (club) => {
        this.club.set(club);
        this.loadRanking('');
        this.loadTournaments(slug);
        this.loadMembership();
        this.isLoading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(getApiErrorMessage(error, 'No se pudo cargar la página del club.'));
        this.isLoading.set(false);
      },
    });
  }

  private loadRanking(gender: string): void {
    const slug = this.club()?.slug;
    if (!slug) {
      return;
    }
    this.clubService.getPublicRanking(slug, gender || null).subscribe({
      next: (ranking) => this.ranking.set(ranking),
      error: () => this.ranking.set([]),
    });
  }

  private loadTournaments(slug: string): void {
    this.clubService.getPublicTournaments(slug).subscribe({
      next: (tournaments) => this.tournaments.set(tournaments),
      error: () => this.tournaments.set([]),
    });
  }

  private loadMembership(): void {
    this.authService.isLoggedIn$.pipe(take(1)).subscribe((isLoggedIn) => {
      if (!isLoggedIn) {
        return;
      }
      this.clubService.getMyClubs().subscribe({
        next: (memberships) => {
          const clubId = this.club()?.id;
          if (clubId) {
            this.membership.set(memberships.find((item) => item.clubId === clubId) ?? null);
          }
        },
        error: () => this.membership.set(null),
      });
    });
  }
}
