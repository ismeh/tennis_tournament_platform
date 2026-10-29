import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { CustomCategoryService } from '../../data/services/custom-category.service';
import { TournamentEventCatalogItem } from '../../data/interfaces/tournament.model';

@Component({
  selector: 'app-manage-categories',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="relative overflow-hidden bg-gradient-to-br from-neutral-50 via-white to-primary-50/60 py-10 sm:py-14 min-h-screen">
      <div class="absolute inset-0 -z-10 opacity-60">
        <div class="absolute left-0 top-20 h-72 w-72 rounded-full bg-primary-200 blur-3xl"></div>
        <div class="absolute bottom-0 right-0 h-80 w-80 rounded-full bg-accent-200 blur-3xl"></div>
      </div>

      <div class="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8">
        <div class="rounded-3xl border border-neutral-200 bg-white p-6 shadow-2xl shadow-neutral-200 sm:p-8">
          <div class="flex items-start justify-between gap-4">
            <div>
              <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-600">Gestión</p>
              <h1 class="mt-2 text-2xl font-bold text-neutral-900">Mis Categorías Personalizadas</h1>
              <p class="mt-2 text-sm text-neutral-500">
                Crea categorías propias que aparecerán al crear tus torneos.
              </p>
            </div>
            <a routerLink="/torneos/crear" class="rounded-full border border-neutral-200 px-4 py-2 text-sm font-medium text-neutral-600 transition-colors hover:border-primary-300 hover:text-primary-700">
              Volver
            </a>
          </div>

          <!-- Formulario para crear/editar -->
          <div class="mt-8 rounded-2xl border border-neutral-100 bg-neutral-50 p-4">
            <div class="flex gap-3">
              <input
                type="text"
                [(ngModel)]="newCategoryName"
                (keyup.enter)="createCategory()"
                placeholder="Nombre de la nueva categoría"
                class="flex-1 rounded-xl border border-neutral-300 bg-white px-4 py-3 text-sm outline-none transition focus:border-primary-500 focus:ring-2 focus:ring-primary-200"
                [disabled]="isSubmitting()"
              />
              <button
                type="button"
                (click)="createCategory()"
                [disabled]="!newCategoryName.trim() || isSubmitting()"
                class="whitespace-nowrap rounded-xl bg-primary-500 px-5 py-3 text-sm font-semibold text-white transition-colors hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                @if (isSubmitting()) {
                  <span class="inline-block h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"></span>
                } @else {
                  Crear
                }
              </button>
            </div>
            @if (errorMessage()) {
              <p class="mt-2 text-xs text-red-600">{{ errorMessage() }}</p>
            }
          </div>

          <!-- Lista de categorías -->
          <div class="mt-6">
            @if (isLoading()) {
              <div class="flex justify-center py-8">
                <span class="inline-block h-8 w-8 animate-spin rounded-full border-4 border-primary-500 border-t-transparent"></span>
              </div>
            } @else if (categories().length === 0) {
              <div class="rounded-2xl border border-dashed border-neutral-300 py-10 text-center">
                <p class="text-sm text-neutral-500">No tienes categorías personalizadas todavía.</p>
                <p class="mt-1 text-xs text-neutral-400">Crea una categoría arriba para que aparezca en tus torneos.</p>
              </div>
            } @else {
              <ul class="divide-y divide-neutral-100">
                @for (cat of categories(); track cat.id) {
                  <li class="flex items-center justify-between gap-3 py-3">
                    @if (editingId() === cat.id) {
                      <input
                        type="text"
                        [(ngModel)]="editingName"
                        (keyup.enter)="saveEdit(cat)"
                        (keyup.escape)="cancelEdit()"
                        class="flex-1 rounded-xl border border-primary-300 bg-white px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary-200"
                        autofocus
                      />
                      <button
                        type="button"
                        (click)="saveEdit(cat)"
                        class="rounded-lg bg-primary-500 px-3 py-1.5 text-xs font-semibold text-white hover:bg-primary-600"
                      >
                        Guardar
                      </button>
                      <button
                        type="button"
                        (click)="cancelEdit()"
                        class="rounded-lg border border-neutral-200 px-3 py-1.5 text-xs font-medium text-neutral-600 hover:bg-neutral-50"
                      >
                        Cancelar
                      </button>
                    } @else {
                      <span class="flex-1 text-sm font-medium text-neutral-800">{{ cat.category }}</span>
                      <button
                        type="button"
                        (click)="startEdit(cat)"
                        class="rounded-lg border border-neutral-200 px-3 py-1.5 text-xs font-medium text-neutral-600 hover:bg-neutral-50 hover:text-primary-600"
                      >
                        Editar
                      </button>
                      <button
                        type="button"
                        (click)="confirmDelete(cat)"
                        class="rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50"
                      >
                        Eliminar
                      </button>
                    }
                  </li>
                }
              </ul>
            }
          </div>

          @if (successMessage()) {
            <div class="mt-4 rounded-xl border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">
              {{ successMessage() }}
            </div>
          }
        </div>
      </div>
    </section>
  `
})
export class ManageCategoriesComponent implements OnInit {
  private readonly customCategoryService = inject(CustomCategoryService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly categories = signal<TournamentEventCatalogItem[]>([]);
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly editingId = signal<number | null>(null);

  newCategoryName = '';
  editingName = '';

  ngOnInit(): void {
    if (this.authService.currentRole !== 'ORGANIZER') {
      this.router.navigateByUrl('/torneos');
      return;
    }
    this.loadCategories();
  }

  loadCategories(): void {
    this.isLoading.set(true);
    this.customCategoryService.getMyCategories().subscribe({
      next: cats => {
        this.categories.set(cats);
        this.isLoading.set(false);
      },
      error: () => {
        this.categories.set([]);
        this.isLoading.set(false);
      }
    });
  }

  createCategory(): void {
    const name = this.newCategoryName.trim();
    if (!name) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.customCategoryService.createCategory(name).subscribe({
      next: created => {
        this.categories.update(cats => [...cats, created]);
        this.newCategoryName = '';
        this.isSubmitting.set(false);
        this.successMessage.set('Categoría creada correctamente.');
        setTimeout(() => this.successMessage.set(null), 3000);
      },
      error: (error) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(error?.error?.message || 'No se pudo crear la categoría.');
      }
    });
  }

  startEdit(cat: TournamentEventCatalogItem): void {
    this.editingId.set(cat.id);
    this.editingName = cat.category;
  }

  cancelEdit(): void {
    this.editingId.set(null);
    this.editingName = '';
  }

  saveEdit(cat: TournamentEventCatalogItem): void {
    const name = this.editingName.trim();
    if (!name) return;

    this.isSubmitting.set(true);
    this.customCategoryService.updateCategory(cat.id, name).subscribe({
      next: updated => {
        this.categories.update(cats =>
          cats.map(c => c.id === updated.id ? updated : c)
        );
        this.cancelEdit();
        this.isSubmitting.set(false);
        this.successMessage.set('Categoría actualizada.');
        setTimeout(() => this.successMessage.set(null), 3000);
      },
      error: (error) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(error?.error?.message || 'No se pudo actualizar la categoría.');
      }
    });
  }

  confirmDelete(cat: TournamentEventCatalogItem): void {
    if (!confirm(`¿Eliminar la categoría "${cat.category}"?`)) return;

    this.customCategoryService.deleteCategory(cat.id).subscribe({
      next: () => {
        this.categories.update(cats => cats.filter(c => c.id !== cat.id));
        this.successMessage.set('Categoría eliminada.');
        setTimeout(() => this.successMessage.set(null), 3000);
      },
      error: () => {
        this.errorMessage.set('No se pudo eliminar la categoría.');
      }
    });
  }
}
