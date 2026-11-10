import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { getApiErrorMessage } from '../../core/errors/api-error.util';
import { CLUB_ROLE_LABELS, ClubMembershipResponse } from '../../data/interfaces/club.model';
import { ClubService } from '../../data/services/club.service';

@Component({
  selector: 'app-my-clubs',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="min-h-[calc(100vh-4rem)] bg-neutral-50 py-8 sm:py-12">
      <div class="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
        <div class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-600">
              Mi comunidad
            </p>
            <h1 class="mt-2 text-3xl font-black text-neutral-950">Mis clubes</h1>
            <p class="mt-2 text-sm text-neutral-600">
              Gestiona las páginas y los equipos de los clubes a los que perteneces.
            </p>
          </div>
          <div class="flex gap-2">
            <a
              routerLink="/clubes"
              class="rounded-xl border border-neutral-300 bg-white px-4 py-2.5 text-sm font-semibold text-neutral-700 hover:bg-neutral-50"
              >Directorio</a
            >
            <a
              routerLink="/clubes/crear"
              class="rounded-xl bg-primary-500 px-4 py-2.5 text-sm font-bold text-white hover:bg-primary-600"
              >Crear club</a
            >
          </div>
        </div>

        @if (isLoading()) {
          <div class="mt-10 flex justify-center py-16">
            <div
              class="h-8 w-8 animate-spin rounded-full border-2 border-primary-500 border-t-transparent"
            ></div>
          </div>
        } @else if (errorMessage()) {
          <div class="mt-8 rounded-2xl border border-red-200 bg-red-50 p-6 text-sm text-red-700">
            {{ errorMessage() }}
          </div>
        } @else if (clubs().length === 0) {
          <div
            class="mt-8 rounded-3xl border border-dashed border-neutral-300 bg-white p-12 text-center"
          >
            <p class="text-lg font-semibold text-neutral-950">
              Todavía no perteneces a ningún club
            </p>
            <p class="mt-2 text-sm text-neutral-600">
              Crea un club o pide a un administrador que te añada.
            </p>
            <a
              routerLink="/clubes/crear"
              class="mt-5 inline-flex rounded-xl bg-primary-500 px-5 py-2.5 text-sm font-bold text-white hover:bg-primary-600"
              >Crear mi club</a
            >
          </div>
        } @else {
          <div class="mt-8 space-y-4">
            @for (membership of clubs(); track membership.membershipId) {
              <article
                class="flex flex-col gap-4 rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm sm:flex-row sm:items-center sm:justify-between"
              >
                <div class="flex items-center gap-4">
                  <div
                    class="flex h-14 w-14 items-center justify-center overflow-hidden rounded-xl bg-neutral-100"
                  >
                    @if (membership.logoUrl) {
                      <img
                        [src]="membership.logoUrl"
                        [alt]="membership.name"
                        class="h-full w-full object-cover"
                      />
                    } @else {
                      <span class="text-xl font-black text-neutral-600">{{
                        getInitial(membership.name)
                      }}</span>
                    }
                  </div>
                  <div>
                    <h2 class="text-lg font-bold text-neutral-950">{{ membership.name }}</h2>
                    <p class="mt-0.5 text-sm text-neutral-600">
                      {{ membership.city || 'Sin ubicación' }} ·
                      {{ membership.memberCount }} miembros
                    </p>
                    <span
                      class="mt-2 inline-flex rounded-full bg-primary-50 px-3 py-1 text-xs font-bold text-primary-700"
                    >
                      {{ getRoleLabel(membership.role) }}
                    </span>
                  </div>
                </div>
                <div class="flex flex-wrap gap-2">
                  <a
                    [routerLink]="['/clubes', membership.slug]"
                    class="rounded-xl border border-neutral-300 px-4 py-2 text-sm font-semibold text-neutral-700 hover:bg-neutral-50"
                    >Ver página</a
                  >
                  <a
                    [routerLink]="['/clubes', membership.clubId, 'gestion']"
                    class="rounded-xl bg-neutral-900 px-4 py-2 text-sm font-bold text-white hover:bg-neutral-800"
                    >Gestionar</a
                  >
                </div>
              </article>
            }
          </div>
        }
      </div>
    </section>
  `,
})
export class MyClubsComponent implements OnInit {
  private readonly clubService = inject(ClubService);

  readonly clubs = signal<ClubMembershipResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.clubService.getMyClubs().subscribe({
      next: (clubs) => {
        this.clubs.set(clubs);
        this.isLoading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(getApiErrorMessage(error, 'No se pudieron cargar tus clubes.'));
        this.isLoading.set(false);
      },
    });
  }

  getInitial(name: string): string {
    return name?.charAt(0)?.toUpperCase() || 'C';
  }

  getRoleLabel(role: ClubMembershipResponse['role']): string {
    return CLUB_ROLE_LABELS[role] ?? role;
  }
}
