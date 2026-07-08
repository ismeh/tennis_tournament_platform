import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MemberService } from '../../data/services/member.service';
import { AuthService } from '../../core/auth/auth.service';
import { TournamentService } from '../../data/services/tournament.service';
import { PlayerInscriptionResponse } from '../../data/interfaces/tournament.model';
import { getApiErrorMessage } from '../../core/errors/api-error.util';
import { NationalityOption } from '../../data/interfaces/reference-data.model';
import { ReferenceDataService } from '../../data/services/reference-data.service';
import { ClubAutocompleteComponent } from '../../components/club-autocomplete';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, ClubAutocompleteComponent],
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

          <div>
            <label class="mb-1 block text-sm font-medium text-neutral-700">Club</label>
            <app-club-autocomplete
              placeholder="Buscar club..."
              formControlName="clubName"
            ></app-club-autocomplete>
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

      <!-- Inscriptions Section -->
      <div class="mt-8 rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
        <h2 class="text-xl font-bold text-neutral-900">Mis inscripciones</h2>
        <p class="mt-1 text-sm text-neutral-600">
          Torneos y categorías a los que te has apuntado.
        </p>

        @if (isLoadingInscriptions()) {
          <div class="mt-6 flex justify-center py-6">
            <div class="h-6 w-6 animate-spin rounded-full border-2 border-primary-500 border-t-transparent"></div>
          </div>
        } @else if (inscriptionsError()) {
          <div class="mt-6 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            {{ inscriptionsError() }}
          </div>
        } @else if (inscriptions().length === 0) {
          <div class="mt-6 rounded-xl border border-dashed border-neutral-300 bg-neutral-50 p-6 text-center">
            <p class="text-sm font-medium text-neutral-900">Aún no te has inscrito en ningún torneo</p>
            <p class="mt-1 text-xs text-neutral-500">Explora el calendario de torneos y apúntate para competir.</p>
            <a routerLink="/torneos" class="mt-4 inline-flex items-center rounded-lg bg-primary-500 px-4 py-2 text-xs font-semibold text-white transition-colors hover:bg-primary-600">
              Explorar torneos
            </a>
          </div>
        } @else {
          <div class="mt-6 divide-y divide-neutral-100 overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm">
            @for (ins of inscriptions(); track ins.eventId + ins.tournamentId) {
              <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 p-5 hover:bg-neutral-50 transition-colors">
                <div class="min-w-0">
                  <a [routerLink]="['/torneos', ins.tournamentId]" class="font-bold text-neutral-900 hover:text-primary-600 transition-colors text-base truncate block">
                    {{ ins.tournamentName }}
                  </a>
                  <p class="mt-1 text-xs font-semibold uppercase tracking-wider text-neutral-500">
                    Prueba: {{ ins.eventName }} · {{ ins.categoryName || 'General' }}
                  </p>
                  <p class="mt-1 text-xs text-neutral-500">
                    Fechas: {{ ins.playStartDate | date:'dd/MM/yyyy' }} - {{ ins.playEndDate | date:'dd/MM/yyyy' }}
                  </p>
                </div>

                <div class="flex flex-wrap items-center gap-2 sm:justify-end">
                  <span class="rounded-full bg-neutral-100 border border-neutral-200 px-2.5 py-1 text-xs font-medium text-neutral-700">
                    {{ getEntryStatusLabel(ins.entryStatus) }}
                  </span>

                  <span class="inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium border"
                        [class]="ins.paymentStatus === 'PAID' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-amber-50 text-amber-700 border-amber-200'">
                    <span class="mr-1.5 h-1.5 w-1.5 rounded-full" [class]="ins.paymentStatus === 'PAID' ? 'bg-emerald-500' : 'bg-amber-500 animate-pulse'"></span>
                    {{ getPaymentStatusLabel(ins.paymentStatus) }}
                  </span>
                </div>
              </div>
            }
          </div>
        }
      </div>
    </section>
  `
})
export class ProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly memberService = inject(MemberService);
  private readonly authService = inject(AuthService);
  private readonly referenceDataService = inject(ReferenceDataService);
  private readonly tournamentService = inject(TournamentService);
  private readonly router = inject(Router);

  readonly isSubmitting = signal(false);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly nationalities = signal<NationalityOption[]>([]);
  readonly roleLabel = signal('');

  readonly inscriptions = signal<PlayerInscriptionResponse[]>([]);
  readonly isLoadingInscriptions = signal(false);
  readonly inscriptionsError = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: [''],
    gender: ['MALE', [Validators.required]],
    birthDate: ['', [Validators.required]],
    nationality: [''],
    federationLicense: [''],
    clubName: ['']
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

    this.loadInscriptions();

    this.memberService.getMyProfile().subscribe({
      next: profile => {
        this.form.patchValue({
          firstName: profile.firstName ?? '',
          lastName: profile.lastName ?? '',
          gender: profile.gender ?? '',
          birthDate: profile.birthDate ?? '',
          nationality: profile.nationality ?? '',
          federationLicense: profile.federationLicense ?? '',
          clubName: profile.clubName ?? ''
        });
        this.roleLabel.set(
          profile.role === 'ORGANIZER' ? 'Organizador' :
          profile.role === 'UMPIRE' ? 'Árbitro' :
          'Jugador'
        );
        this.isLoading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(getApiErrorMessage(error, 'No se pudo cargar tu perfil actual.'));
        this.isLoading.set(false);
      }
    });
  }

  loadInscriptions(): void {
    this.isLoadingInscriptions.set(true);
    this.inscriptionsError.set(null);
    this.tournamentService.getMyInscriptions().subscribe({
      next: (data) => {
        this.inscriptions.set(data);
        this.isLoadingInscriptions.set(false);
      },
      error: (error) => {
        this.inscriptionsError.set(getApiErrorMessage(error, 'No se pudieron cargar tus inscripciones.'));
        this.isLoadingInscriptions.set(false);
      }
    });
  }

  getEntryStatusLabel(status: string | null): string {
    if (!status) return 'DA';
    switch (status) {
      case 'DIRECT_ACCEPTANCE': return 'DA (Aceptación Directa)';
      case 'WILDCARD': return 'WC (Invitación)';
      case 'QUALIFIER': return 'Q (Clasificado)';
      case 'LUCKY_LOSER': return 'LL (Perdedor Afortunado)';
      default: return status;
    }
  }

  getPaymentStatusLabel(status: string | null): string {
    if (!status) return 'Pendiente';
    switch (status) {
      case 'PAID': return 'Pagado';
      case 'PENDING': return 'Pendiente';
      default: return status;
    }
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
      federationLicense: raw.federationLicense || null,
      clubName: raw.clubName || null
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
