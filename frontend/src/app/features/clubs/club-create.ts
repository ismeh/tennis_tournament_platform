import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { getApiErrorMessage } from '../../core/errors/api-error.util';
import { ClubService } from '../../data/services/club.service';

@Component({
  selector: 'app-club-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="mx-auto max-w-3xl px-4 py-10 sm:py-14">
      <a routerLink="/clubes" class="text-sm font-semibold text-primary-700 hover:underline"
        >← Volver a clubes</a
      >
      <div class="mt-4 rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
        <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-600">Nuevo club</p>
        <h1 class="mt-2 text-3xl font-black text-neutral-950">Crea la página de tu club</h1>
        <p class="mt-2 text-sm text-neutral-600">
          Serás el propietario del club y podrás personalizar su página, gestionar miembros,
          permisos y ranking.
        </p>

        <form class="mt-8 space-y-6" [formGroup]="form" (ngSubmit)="submit()">
          <div class="grid gap-4 sm:grid-cols-2">
            <label class="block sm:col-span-2">
              <span class="mb-1 block text-sm font-medium text-neutral-700">Nombre del club *</span>
              <input
                formControlName="name"
                type="text"
                class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
              />
            </label>
            <label class="block">
              <span class="mb-1 block text-sm font-medium text-neutral-700">Dirección pública</span>
              <input
                formControlName="slug"
                type="text"
                placeholder="mi-club-de-tenis"
                class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
              />
              <span class="mt-1 block text-xs text-neutral-500"
                >Se generará automáticamente si lo dejas vacío.</span
              >
            </label>
            <label class="block">
              <span class="mb-1 block text-sm font-medium text-neutral-700">Eslogan</span>
              <input
                formControlName="tagline"
                type="text"
                class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
              />
            </label>
            <label class="block sm:col-span-2">
              <span class="mb-1 block text-sm font-medium text-neutral-700">Descripción</span>
              <textarea
                formControlName="description"
                rows="4"
                class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
              ></textarea>
            </label>
            <label class="block">
              <span class="mb-1 block text-sm font-medium text-neutral-700">Ciudad</span>
              <input
                formControlName="city"
                type="text"
                class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
              />
            </label>
            <label class="block">
              <span class="mb-1 block text-sm font-medium text-neutral-700">País</span>
              <input
                formControlName="country"
                type="text"
                class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
              />
            </label>
            <label class="block">
              <span class="mb-1 block text-sm font-medium text-neutral-700">Color principal</span>
              <div class="flex items-center gap-2">
                <span
                  class="h-10 w-10 shrink-0 rounded border border-neutral-300"
                  [style.background]="form.controls.primaryColor.value"
                ></span>
                <input
                  formControlName="primaryColor"
                  type="text"
                  class="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-primary-500"
                />
              </div>
            </label>
            <label class="block">
              <span class="mb-1 block text-sm font-medium text-neutral-700">Color secundario</span>
              <div class="flex items-center gap-2">
                <span
                  class="h-10 w-10 shrink-0 rounded border border-neutral-300"
                  [style.background]="form.controls.secondaryColor.value"
                ></span>
                <input
                  formControlName="secondaryColor"
                  type="text"
                  class="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-primary-500"
                />
              </div>
            </label>
            <label class="block">
              <span class="mb-1 block text-sm font-medium text-neutral-700">Logo (URL)</span>
              <input
                formControlName="logoUrl"
                type="url"
                class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
              />
            </label>
            <label class="block">
              <span class="mb-1 block text-sm font-medium text-neutral-700">Portada (URL)</span>
              <input
                formControlName="bannerUrl"
                type="url"
                class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
              />
            </label>
          </div>

          @if (errorMessage()) {
            <p class="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {{ errorMessage() }}
            </p>
          }

          <div class="flex flex-wrap gap-3">
            <button
              type="submit"
              class="rounded-xl bg-primary-500 px-5 py-2.5 text-sm font-bold text-white transition hover:bg-primary-600 disabled:opacity-60"
              [disabled]="form.invalid || isSubmitting()"
            >
              {{ isSubmitting() ? 'Creando club...' : 'Crear club' }}
            </button>
            <a
              routerLink="/clubes"
              class="rounded-xl border border-neutral-300 px-5 py-2.5 text-sm font-semibold text-neutral-700 hover:bg-neutral-50"
              >Cancelar</a
            >
          </div>
        </form>
      </div>
    </section>
  `,
})
export class ClubCreateComponent {
  private readonly fb = inject(FormBuilder);
  private readonly clubService = inject(ClubService);
  private readonly router = inject(Router);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    slug: [''],
    tagline: [''],
    description: [''],
    city: [''],
    country: [''],
    primaryColor: ['#16a34a'],
    secondaryColor: ['#0284c7'],
    logoUrl: [''],
    bannerUrl: [''],
  });

  submit(): void {
    if (this.form.invalid || this.isSubmitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.clubService
      .createClub({
        name: value.name,
        slug: value.slug || null,
        tagline: value.tagline || null,
        description: value.description || null,
        city: value.city || null,
        country: value.country || null,
        primaryColor: value.primaryColor,
        secondaryColor: value.secondaryColor,
        logoUrl: value.logoUrl || null,
        bannerUrl: value.bannerUrl || null,
        publicPageEnabled: true,
      })
      .subscribe({
        next: (club) => {
          this.isSubmitting.set(false);
          this.router.navigate(['/clubes', club.id, 'gestion']);
        },
        error: (error) => {
          this.isSubmitting.set(false);
          this.errorMessage.set(getApiErrorMessage(error, 'No se pudo crear el club.'));
        },
      });
  }
}
