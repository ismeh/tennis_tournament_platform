import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { getApiErrorMessage } from '../../core/errors/api-error.util';
import {
  RankingGender,
  RankingSortDirection,
  RankingTournamentResponse,
  TournamentRankingResponse
} from '../../data/interfaces/ranking.model';
import {
  TournamentEventCatalogItem,
  TournamentEventGender,
  getTournamentEventGenderLabel
} from '../../data/interfaces/tournament.model';
import { RankingService } from '../../data/services/ranking.service';
import { TournamentService } from '../../data/services/tournament.service';

@Component({
  selector: 'app-ranking-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="min-h-[calc(100vh-4rem)] bg-neutral-50 py-8 sm:py-10">
      <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <header class="border-b border-neutral-200 pb-6">
          <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-600">Ranking</p>
          <div class="mt-2 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <h1 class="text-3xl font-black text-neutral-950 sm:text-4xl">Clasificaciones</h1>
              <p class="mt-2 max-w-2xl text-sm text-neutral-600">
                Clasificación por victorias dentro de cada torneo.
              </p>
            </div>

            <a
              routerLink="/torneos"
              class="inline-flex w-fit items-center justify-center rounded-lg border border-neutral-300 bg-white px-4 py-2 text-sm font-semibold text-neutral-800 shadow-sm transition hover:border-primary-300 hover:text-primary-700"
            >
              Ver torneos
            </a>
          </div>
        </header>

        <div class="mt-6 grid gap-5 lg:grid-cols-[18rem_1fr]">
          <aside class="rounded-lg border border-neutral-200 bg-white p-4 shadow-sm">
            <div class="mt-4 space-y-4">
              <label class="block">
                <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Torneo</span>
                <select
                  class="h-11 w-full rounded-lg border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition focus:border-primary-500"
                  [ngModel]="selectedTournamentId()"
                  (ngModelChange)="onTournamentChange($event)"
                  name="tournamentId"
                >
                  <option value="">Seleccionar</option>
                  @for (tournament of tournaments(); track tournament.id) {
                    <option [value]="tournament.id">{{ tournament.formalName }}</option>
                  }
                </select>
              </label>

              <label class="block">
                <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Género</span>
                <select
                  class="h-11 w-full rounded-lg border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition focus:border-primary-500"
                  [ngModel]="selectedGender() ?? ''"
                  (ngModelChange)="onGenderChange($event)"
                  name="gender"
                >
                  <option value="">Todos</option>
                  @for (gender of genderOptions(); track gender) {
                    <option [value]="gender">{{ getGenderLabel(gender) }}</option>
                  }
                </select>
              </label>

              <label class="block">
                <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Categoría</span>
                  <select
                    class="h-11 w-full rounded-lg border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition focus:border-primary-500"
                    [ngModel]="selectedCategoryId()"
                    (ngModelChange)="onCategoryChange($event)"
                    name="category"
                  >
                  <option value="">Todas</option>
                  @for (category of categories(); track category.id) {
                    <option [value]="category.id">{{ category.category }}</option>
                  }
                </select>
              </label>

              <div class="pt-1">
                <button
                  type="button"
                  class="h-11 w-full rounded-lg border border-neutral-300 bg-white px-4 text-sm font-semibold text-neutral-700 transition hover:bg-neutral-50"
                  (click)="resetFilters()"
                >
                  Limpiar
                </button>
              </div>
            </div>
          </aside>

          <main class="min-w-0">
            <div class="rounded-lg border border-neutral-200 bg-white shadow-sm">
              <div class="flex flex-col gap-3 border-b border-neutral-200 px-5 py-4 md:flex-row md:items-center md:justify-between">
                <div>
                  <h2 class="text-xl font-bold text-neutral-950">{{ rankingTitle() }}</h2>
                  <p class="mt-1 text-sm text-neutral-600">{{ rankingSubtitle() }}</p>
                </div>
                <span class="w-fit rounded-full bg-neutral-100 px-3 py-1 text-xs font-semibold text-neutral-700">{{ rowCount() }} jugadores</span>
              </div>

              <div class="grid gap-3 border-b border-neutral-200 bg-neutral-50 px-5 py-3 md:grid-cols-[1fr_auto_auto] md:items-center">
                <div class="text-sm text-neutral-600">
                  Página <span class="font-semibold text-neutral-950">{{ currentPageLabel() }}</span> de
                  <span class="font-semibold text-neutral-950">{{ totalPagesLabel() }}</span>
                  · {{ pageRangeLabel() }} de {{ formatNumber(totalItems()) }}
                </div>

                <label class="flex items-center gap-2 text-sm text-neutral-700">
                  <span>Mostrar</span>
                  <select
                    class="h-9 rounded-lg border border-neutral-300 bg-white px-2 text-sm text-neutral-900 outline-none transition focus:border-primary-500"
                    [ngModel]="pageSize()"
                    (ngModelChange)="onPageSizeChange($event)"
                    name="pageSize"
                  >
                    @for (size of pageSizeOptions; track size) {
                      <option [value]="size">{{ size }}</option>
                    }
                  </select>
                </label>

                <div class="flex items-center gap-2">
                  <button
                    type="button"
                    class="h-9 rounded-lg border border-neutral-300 bg-white px-3 text-sm font-semibold text-neutral-700 transition hover:bg-white disabled:cursor-not-allowed disabled:opacity-50"
                    [disabled]="!canGoPrevious() || isLoading()"
                    (click)="goToPreviousPage()"
                  >
                    Anterior
                  </button>
                  <button
                    type="button"
                    class="h-9 rounded-lg border border-neutral-300 bg-white px-3 text-sm font-semibold text-neutral-700 transition hover:bg-white disabled:cursor-not-allowed disabled:opacity-50"
                    [disabled]="!canGoNext() || isLoading()"
                    (click)="goToNextPage()"
                  >
                    Siguiente
                  </button>
                </div>
              </div>

              @if (isLoading()) {
                <div class="p-6 text-sm text-neutral-600">Cargando ranking...</div>
              } @else if (errorMessage()) {
                <div class="m-5 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{{ errorMessage() }}</div>
              } @else if (!selectedTournamentId()) {
                <div class="p-8 text-center">
                  <p class="text-base font-semibold text-neutral-950">Selecciona un torneo</p>
                  <p class="mt-1 text-sm text-neutral-600">El ranking de torneo se calcula con las victorias registradas.</p>
                </div>
              } @else if (rowCount() === 0) {
                <div class="p-8 text-center">
                  <p class="text-base font-semibold text-neutral-950">Sin resultados</p>
                  <p class="mt-1 text-sm text-neutral-600">Ajusta los filtros o registra resultados de partidos.</p>
                </div>
              } @else {
                <div class="overflow-x-auto">
                  <table class="min-w-full divide-y divide-neutral-200 text-sm">
                    <thead class="bg-neutral-50 text-left text-xs font-semibold uppercase tracking-widest text-neutral-500">
                      <tr>
                        <th class="px-5 py-3">
                          <button type="button" class="font-semibold uppercase tracking-widest" (click)="setSort('position')">Posición {{ getSortIndicator('position') }}</button>
                        </th>
                        <th class="px-5 py-3">
                          <button type="button" class="font-semibold uppercase tracking-widest" (click)="setSort('name')">Jugador {{ getSortIndicator('name') }}</button>
                        </th>
                        <th class="px-5 py-3">
                          <button type="button" class="font-semibold uppercase tracking-widest" (click)="setSort('gender')">Género {{ getSortIndicator('gender') }}</button>
                        </th>
                        <th class="px-5 py-3 text-right">
                          <button type="button" class="font-semibold uppercase tracking-widest" (click)="setSort('victories')">Victorias {{ getSortIndicator('victories') }}</button>
                        </th>
                      </tr>
                    </thead>
                    <tbody class="divide-y divide-neutral-100">
                      @for (player of tournamentRows(); track player.participantId) {
                        <tr class="hover:bg-primary-50/50">
                          <td class="whitespace-nowrap px-5 py-4 text-base font-black text-neutral-950">#{{ player.position }}</td>
                          <td class="px-5 py-4">
                            <p class="font-semibold text-neutral-950">{{ getTournamentPlayerName(player) }}</p>
                            <p class="mt-1 text-xs text-neutral-500">{{ player.license || 'Sin licencia' }}</p>
                          </td>
                          <td class="whitespace-nowrap px-5 py-4 text-neutral-700">{{ getGenderLabel(player.gender) }}</td>
                          <td class="whitespace-nowrap px-5 py-4 text-right font-semibold text-neutral-950">{{ player.victories }}</td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              }
            </div>
          </main>
        </div>
      </div>
    </section>
  `
})
export class RankingComponent implements OnInit {
  private readonly rankingService = inject(RankingService);
  private readonly tournamentService = inject(TournamentService);

  readonly pageSizeOptions = [10, 25, 50, 100];
  readonly selectedGender = signal<RankingGender | null>(null);
  readonly selectedCategoryId = signal('');
  readonly selectedTournamentId = signal('');
  readonly page = signal(0);
  readonly pageSize = signal(10);
  readonly totalItems = signal(0);
  readonly totalPages = signal(0);
  readonly sortBy = signal('victories');
  readonly sortDirection = signal<RankingSortDirection>('desc');
  readonly categories = signal<TournamentEventCatalogItem[]>([]);
  readonly tournaments = signal<RankingTournamentResponse[]>([]);
  readonly tournamentRows = signal<TournamentRankingResponse[]>([]);
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly genderOptions = computed<RankingGender[]>(() => ['MALE', 'FEMALE', 'MIXED']);
  readonly rowCount = computed(() => this.tournamentRows().length);
  readonly currentPageLabel = computed(() => this.totalItems() === 0 ? 0 : this.page() + 1);
  readonly totalPagesLabel = computed(() => this.totalPages());
  readonly canGoPrevious = computed(() => this.page() > 0);
  readonly canGoNext = computed(() => this.totalPages() > 0 && this.page() + 1 < this.totalPages());
  readonly pageRangeLabel = computed(() => {
    if (this.totalItems() === 0) {
      return '0-0';
    }

    const first = this.page() * this.pageSize() + 1;
    const last = Math.min(first + this.rowCount() - 1, this.totalItems());
    return `${first}-${last}`;
  });
  readonly rankingTitle = computed(() => 'Ranking del torneo');
  readonly rankingSubtitle = computed(() => {
    const tournament = this.tournaments().find(item => item.id === this.selectedTournamentId());
    return tournament
      ? `${tournament.formalName} · solo victorias registradas`
      : 'Selecciona un torneo para ver la clasificación por victorias.';
  });

  ngOnInit(): void {
    this.loadCatalogs();
  }

  applyFilters(): void {
    this.loadTournamentRanking();
  }

  resetFilters(): void {
    this.selectedGender.set(null);
    this.selectedCategoryId.set('');
    this.page.set(0);
    this.applyFilters();
  }

  onGenderChange(value: string): void {
    this.selectedGender.set(value ? (value as RankingGender) : null);
    this.onFiltersChanged();
  }

  onCategoryChange(value: string): void {
    this.selectedCategoryId.set(value);
    this.onFiltersChanged();
  }

  onTournamentChange(value: string): void {
    this.selectedTournamentId.set(value);
    this.page.set(0);
    this.loadTournamentRanking();
  }

  onPageSizeChange(value: string | number): void {
    const nextSize = Number(value);
    this.pageSize.set(Number.isNaN(nextSize) ? 10 : nextSize);
    this.page.set(0);
    this.applyFilters();
  }

  goToPreviousPage(): void {
    if (!this.canGoPrevious()) {
      return;
    }

    this.page.update(current => current - 1);
    this.applyFilters();
  }

  goToNextPage(): void {
    if (!this.canGoNext()) {
      return;
    }

    this.page.update(current => current + 1);
    this.applyFilters();
  }

  setSort(field: string): void {
    if (!this.isSortFieldAvailable(field)) {
      return;
    }

    if (this.sortBy() === field) {
      this.sortDirection.update(current => (current === 'asc' ? 'desc' : 'asc'));
    } else {
      this.sortBy.set(field);
      this.sortDirection.set(this.getDefaultSortDirection(field));
    }

    this.page.set(0);
    this.applyFilters();
  }

  getSortIndicator(field: string): string {
    if (this.sortBy() !== field) {
      return '';
    }

    return this.sortDirection() === 'asc' ? '↑' : '↓';
  }

  getGenderLabel(gender: string | null): string {
    if (!gender) {
      return '-';
    }

    return getTournamentEventGenderLabel(gender as TournamentEventGender);
  }

  getTournamentPlayerName(player: TournamentRankingResponse): string {
    return [player.firstName, player.lastName].filter(Boolean).join(' ') || 'Jugador sin nombre';
  }

  formatNumber(value: number | null): string {
    return value == null ? '-' : new Intl.NumberFormat('es-ES').format(value);
  }

  private loadCatalogs(): void {
    this.tournamentService.getEventCatalog().subscribe({
      next: categories => this.categories.set(categories),
      error: () => this.categories.set([])
    });

    this.rankingService.getRankingTournaments().subscribe({
      next: tournaments => {
        this.tournaments.set(tournaments);
        this.ensureSelectedTournament();
      },
      error: () => this.tournaments.set([])
    });
  }

  private loadTournamentRanking(): void {
    const tournamentId = this.selectedTournamentId();
    if (!tournamentId) {
      this.tournamentRows.set([]);
      this.applyPageMetadata(0, this.pageSize(), 0, 0);
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    const selectedCategory = this.getSelectedCategory();

    this.rankingService
      .getTournamentRanking(tournamentId, {
        gender: this.selectedGender(),
        categoryId: selectedCategory?.id ?? null,
        page: this.page(),
        size: this.pageSize(),
        sortBy: this.sortBy(),
        sortDirection: this.sortDirection()
      })
      .subscribe({
        next: response => {
          this.tournamentRows.set(response.items);
          this.applyPageMetadata(response.page, response.size, response.totalItems, response.totalPages);
          this.isLoading.set(false);
        },
        error: error => {
          this.errorMessage.set(getApiErrorMessage(error, 'No se pudo cargar el ranking del torneo.'));
          this.isLoading.set(false);
        }
      });
  }

  private ensureSelectedTournament(): void {
    if (this.selectedTournamentId() || this.tournaments().length === 0) {
      return;
    }

    this.selectedTournamentId.set(this.tournaments()[0].id);
  }

  private onFiltersChanged(): void {
    this.page.set(0);
    this.applyFilters();
  }

  private applyPageMetadata(page: number, size: number, totalItems: number, totalPages: number): void {
    this.page.set(page);
    this.pageSize.set(size);
    this.totalItems.set(totalItems);
    this.totalPages.set(totalPages);
  }

  private isSortFieldAvailable(field: string): boolean {
    const tournamentFields = ['position', 'name', 'gender', 'victories'];
    return tournamentFields.includes(field);
  }

  private getDefaultSortDirection(field: string): RankingSortDirection {
    return field === 'victories' ? 'desc' : 'asc';
  }

  private getSelectedCategory(): TournamentEventCatalogItem | null {
    const selectedId = Number(this.selectedCategoryId());
    if (!selectedId || Number.isNaN(selectedId)) {
      return null;
    }

    return this.categories().find(category => category.id === selectedId) ?? null;
  }
}
