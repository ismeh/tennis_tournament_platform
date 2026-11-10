import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { getApiErrorMessage } from '../../core/errors/api-error.util';
import { ClubResponse } from '../../data/interfaces/club.model';
import { ClubService } from '../../data/services/club.service';

@Component({
  selector: 'app-club-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="min-h-[calc(100vh-4rem)] bg-neutral-50 py-8 sm:py-12">
      <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <header
          class="overflow-hidden rounded-3xl bg-gradient-to-br from-primary-700 via-primary-600 to-accent-600 px-6 py-10 text-white shadow-xl sm:px-10 sm:py-14"
        >
          <p class="text-sm font-semibold uppercase tracking-[0.25em] text-white/70">Comunidad</p>
          <div class="mt-3 flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
            <div class="max-w-2xl">
              <h1 class="text-3xl font-black sm:text-5xl">Clubes de tenis</h1>
              <p class="mt-3 text-base text-white/80 sm:text-lg">
                Descubre clubes, consulta su ranking interno y conoce sus torneos. Cada club tiene
                su propia página.
              </p>
            </div>
            @if (isLoggedIn$ | async) {
              <a
                routerLink="/clubes/crear"
                class="inline-flex w-fit items-center justify-center rounded-xl bg-white px-5 py-3 text-sm font-bold text-primary-700 shadow-lg transition hover:bg-primary-50"
              >
                Crear mi club
              </a>
            } @else {
              <a
                routerLink="/login"
                class="inline-flex w-fit items-center justify-center rounded-xl bg-white px-5 py-3 text-sm font-bold text-primary-700 shadow-lg transition hover:bg-primary-50"
              >
                Entrar para crear un club
              </a>
            }
          </div>
        </header>

        <div class="mt-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 class="text-2xl font-bold text-neutral-950">Directorio de clubes</h2>
            <p class="mt-1 text-sm text-neutral-600">{{ clubs().length }} clubes publicados</p>
          </div>
          <div class="relative w-full sm:max-w-sm">
            <input
              type="search"
              placeholder="Buscar por nombre o ciudad..."
              class="h-11 w-full rounded-xl border border-neutral-300 bg-white pl-10 pr-4 text-sm text-neutral-900 outline-none transition focus:border-primary-500"
              [ngModel]="searchTerm()"
              (ngModelChange)="onSearchChange($event)"
            />
            <svg
              class="pointer-events-none absolute left-3 top-3 h-5 w-5 text-neutral-400"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M21 21l-4.35-4.35M17 10.5a6.5 6.5 0 11-13 0 6.5 6.5 0 0113 0z"
              />
            </svg>
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
            class="mt-8 rounded-2xl border border-dashed border-neutral-300 bg-white p-12 text-center"
          >
            <p class="text-lg font-semibold text-neutral-900">Todavía no hay clubes publicados</p>
            <p class="mt-2 text-sm text-neutral-600">
              Crea el primero y empieza a construir tu comunidad tenística.
            </p>
          </div>
        } @else {
          <div class="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            @for (club of clubs(); track club.id) {
              <a
                [routerLink]="['/clubes', club.slug || club.id]"
                class="group overflow-hidden rounded-2xl border border-neutral-200 bg-white shadow-sm transition hover:-translate-y-0.5 hover:border-primary-300 hover:shadow-lg"
              >
                <div class="h-24" [style.background]="club.primaryColor || '#16a34a'"></div>
                <div class="relative px-5 pb-5">
                  <div class="flex items-end justify-between">
                    <div
                      class="-mt-8 flex h-16 w-16 items-center justify-center rounded-2xl border-4 border-white bg-white shadow-sm"
                    >
                      @if (club.logoUrl) {
                        <img
                          [src]="club.logoUrl"
                          [alt]="club.name"
                          class="h-full w-full rounded-xl object-cover"
                        />
                      } @else {
                        <span class="text-xl font-black text-neutral-700">{{
                          getInitial(club.name)
                        }}</span>
                      }
                    </div>
                    <span
                      class="mt-2 rounded-full bg-neutral-100 px-3 py-1 text-xs font-semibold text-neutral-600"
                    >
                      {{ club.memberCount ?? 0 }} miembros
                    </span>
                  </div>
                  <h3 class="mt-4 text-lg font-bold text-neutral-950 group-hover:text-primary-700">
                    {{ club.name }}
                  </h3>
                  <p class="mt-1 line-clamp-2 min-h-[2.5rem] text-sm text-neutral-600">
                    {{ club.tagline || 'Club de tenis' }}
                  </p>
                  <p class="mt-3 text-xs font-medium uppercase tracking-wide text-neutral-400">
                    {{ club.city || 'Ubicación por confirmar'
                    }}{{ club.country ? ', ' + club.country : '' }}
                  </p>
                </div>
              </a>
            }
          </div>
        }
      </div>
    </section>
  `,
})
export class ClubListComponent implements OnInit {
  private readonly clubService = inject(ClubService);
  private readonly authService = inject(AuthService);

  readonly isLoggedIn$ = this.authService.isLoggedIn$;
  readonly clubs = signal<ClubResponse[]>([]);
  readonly searchTerm = signal('');
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.loadClubs();
  }

  onSearchChange(value: string): void {
    this.searchTerm.set(value);
    this.loadClubs();
  }

  getInitial(name: string): string {
    return name?.charAt(0)?.toUpperCase() || 'C';
  }

  private loadClubs(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.clubService.listPublicClubs(this.searchTerm()).subscribe({
      next: (clubs) => {
        this.clubs.set(clubs);
        this.isLoading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(getApiErrorMessage(error, 'No se pudieron cargar los clubes.'));
        this.isLoading.set(false);
      },
    });
  }
}
