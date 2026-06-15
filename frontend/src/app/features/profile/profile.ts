import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MemberService } from '../../data/services/member.service';
import { AuthService } from '../../core/auth/auth.service';
import { getApiErrorMessage } from '../../core/errors/api-error.util';
import { NationalityOption } from '../../data/interfaces/reference-data.model';
import { ReferenceDataService } from '../../data/services/reference-data.service';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="mx-auto max-w-2xl px-4 py-10 sm:py-16">
      <div class="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
        <h1 class="text-2xl font-bold text-neutral-900">Completar perfil</h1>
        <p class="mt-2 text-sm text-neutral-600">
          Para poder inscribirte en eventos debes completar, al menos, nombre, genero y fecha de nacimiento.
        </p>

        <div class="mt-4 flex items-center gap-2 rounded-lg bg-primary-50 px-4 py-3">
          <span class="text-sm font-medium text-neutral-700">Rol:</span>
          <span class="inline-flex items-center gap-1 rounded-full bg-primary-100 px-3 py-1 text-sm font-semibold text-primary-700">
            {{ roleLabel() }}
          </span>
        </div>

        <form class="mt-6 grid gap-4" [formGroup]="form" (ngSubmit)="submit()">
          <div>
            <label class="mb-1 block text-sm font-medium text-neutral-700" for="firstName">Nombre *</label>
            <input id="firstName" type="text" formControlName="firstName" class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500" />
          </div>

          <div>
            <label class="mb-1 block text-sm font-medium text-neutral-700" for="lastName">Apellido</label>
            <input id="lastName" type="text" formControlName="lastName" class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500" />
          </div>

          <div class="grid gap-4 sm:grid-cols-2">
            <div>
              <label class="mb-1 block text-sm font-medium text-neutral-700" for="gender">Genero *</label>
              <select id="gender" formControlName="gender" class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500">
                <option value="">Selecciona</option>
                <option value="MALE">Masculino</option>
                <option value="FEMALE">Femenino</option>
                <option value="MIXED">Prefiero no decirlo</option>
              </select>
            </div>
            <div>
              <label class="mb-1 block text-sm font-medium text-neutral-700" for="birthDate">Fecha de nacimiento *</label>
              <input id="birthDate" type="date" formControlName="birthDate" class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500" />
            </div>
          </div>

          <div class="grid gap-4 sm:grid-cols-2">
            <div>
              <label class="mb-1 block text-sm font-medium text-neutral-700" for="nationality">País de nacionalidad</label>
              <select id="nationality" formControlName="nationality" class="w-full rounded-lg border border-neutral-300 bg-white px-3 py-2 outline-none focus:border-primary-500">
                <option value="">Selecciona</option>
                @for (nationality of nationalities(); track nationality.code) {
                  <option [value]="nationality.code">{{ nationality.name }}</option>
                }
              </select>
            </div>
            <div>
              <label class="mb-1 block text-sm font-medium text-neutral-700" for="federationLicense">Licencia federativa</label>
              <input id="federationLicense" type="text" formControlName="federationLicense" class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500" />
            </div>
          </div>

          @if (errorMessage()) {
            <p class="text-sm text-red-600">{{ errorMessage() }}</p>
          }

          @if (successMessage()) {
            <p class="text-sm text-emerald-700">{{ successMessage() }}</p>
          }

          <div class="mt-2 flex flex-wrap gap-3">
            <button type="submit" class="rounded-lg bg-primary-500 px-4 py-2 font-medium text-white transition-colors hover:bg-primary-600 disabled:opacity-60" [disabled]="form.invalid || isSubmitting()">
              {{ isSubmitting() ? 'Guardando...' : 'Guardar perfil' }}
            </button>
            <a routerLink="/torneos" class="rounded-lg border border-neutral-300 px-4 py-2 font-medium text-neutral-700 transition-colors hover:bg-neutral-50">Volver a torneos</a>
          </div>
        </form>
      </div>
    </section>
  `
})
export class ProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly memberService = inject(MemberService);
  private readonly authService = inject(AuthService);
  private readonly referenceDataService = inject(ReferenceDataService);
  private readonly router = inject(Router);

  readonly isSubmitting = signal(false);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly nationalities = signal<NationalityOption[]>([]);
  readonly roleLabel = signal('');

  readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: [''],
    gender: ['MALE', [Validators.required]],
    birthDate: ['', [Validators.required]],
    nationality: [''],
    federationLicense: ['']
  });

  ngOnInit(): void {
    this.referenceDataService.getNationalities().subscribe({
      next: nationalities => {
        this.nationalities.set(nationalities);
      },
      error: () => {
        this.nationalities.set([]);
      }
    });

    this.memberService.getMyProfile().subscribe({
      next: profile => {
        this.form.patchValue({
          firstName: profile.firstName ?? '',
          lastName: profile.lastName ?? '',
          gender: profile.gender ?? '',
          birthDate: profile.birthDate ?? '',
          nationality: profile.nationality ?? '',
          federationLicense: profile.federationLicense ?? ''
        });
        this.roleLabel.set(profile.role === 'ORGANIZER' ? 'Organizador' : 'Jugador');
        this.isLoading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(getApiErrorMessage(error, 'No se pudo cargar tu perfil actual.'));
        this.isLoading.set(false);
      }
    });
  }

  submit(): void {
    if (this.form.invalid || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const raw = this.form.getRawValue();
    this.memberService.updateMyProfile({
      firstName: raw.firstName,
      lastName: raw.lastName || null,
      gender: raw.gender,
      birthDate: raw.birthDate,
      nationality: raw.nationality || null,
      federationLicense: raw.federationLicense || null
    }).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.successMessage.set('Perfil actualizado correctamente.');
        const raw = this.form.getRawValue();
        const fullName = (raw.firstName || '').trim() + (raw.lastName ? ' ' + raw.lastName.trim() : '');
        this.authService.setDisplayName(fullName || null);
        this.authService.setNationality(raw.nationality || null);
        this.router.navigateByUrl('/torneos');
      },
      error: (error) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(getApiErrorMessage(error, 'No se pudo guardar el perfil. Revisa los datos e inténtalo de nuevo.'));
      }
    });
  }
}