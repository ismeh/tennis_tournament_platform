import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, AccountExportData } from '../../core/auth/auth.service';
import { getApiErrorMessage } from '../../core/errors/api-error.util';

@Component({
  selector: 'app-account-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="mx-auto max-w-2xl px-4 py-10 sm:py-16">
      <h1 class="text-2xl font-bold text-neutral-900">Ajustes de cuenta</h1>
      <p class="mt-2 text-sm text-neutral-600">
        Gestiona tu cuenta y datos personales conforme al RGPD.
      </p>

      <!-- Export Section -->
      <div class="mt-8 rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
        <h2 class="text-lg font-semibold text-neutral-900">Exportar mis datos</h2>
        <p class="mt-1 text-sm text-neutral-600">
          Descarga una copia de todos tus datos personales almacenados en la plataforma (Art. 20 RGPD).
        </p>

        @if (exportData()) {
          <div class="mt-4 rounded-lg border border-neutral-200 bg-neutral-50 p-4">
            <h3 class="text-sm font-medium text-neutral-700">Datos de la cuenta</h3>
            <dl class="mt-2 grid grid-cols-2 gap-2 text-sm">
              <dt class="text-neutral-500">Email:</dt>
              <dd class="text-neutral-900">{{ exportData()!.account.email }}</dd>
              <dt class="text-neutral-500">Rol:</dt>
              <dd class="text-neutral-900">{{ exportData()!.account.role }}</dd>
              <dt class="text-neutral-500">Nivel:</dt>
              <dd class="text-neutral-900">{{ exportData()!.account.tier }}</dd>
              <dt class="text-neutral-500">Registro:</dt>
              <dd class="text-neutral-900">{{ exportData()!.account.registeredAt | date:'dd/MM/yyyy' }}</dd>
            </dl>

            @if (exportData()!.person) {
              <h3 class="mt-4 text-sm font-medium text-neutral-700">Datos personales</h3>
              <dl class="mt-2 grid grid-cols-2 gap-2 text-sm">
                <dt class="text-neutral-500">Nombre:</dt>
                <dd class="text-neutral-900">{{ exportData()!.person!.firstName }}</dd>
                @if (exportData()!.person!.lastName) {
                  <dt class="text-neutral-500">Apellido:</dt>
                  <dd class="text-neutral-900">{{ exportData()!.person!.lastName }}</dd>
                }
                @if (exportData()!.person!.nationality) {
                  <dt class="text-neutral-500">Nacionalidad:</dt>
                  <dd class="text-neutral-900">{{ exportData()!.person!.nationality }}</dd>
                }
                @if (exportData()!.person!.birthDate) {
                  <dt class="text-neutral-500">Fecha nacimiento:</dt>
                  <dd class="text-neutral-900">{{ exportData()!.person!.birthDate | date:'dd/MM/yyyy' }}</dd>
                }
              </dl>
            }
          </div>
          <button
            type="button"
            (click)="downloadExport()"
            class="mt-4 rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 transition-colors hover:bg-neutral-50"
          >
            Descargar JSON
          </button>
        } @else {
          <button
            type="button"
            (click)="loadExportData()"
            [disabled]="isLoadingExport()"
            class="mt-4 rounded-lg bg-primary-500 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-primary-600 disabled:opacity-60"
          >
            {{ isLoadingExport() ? 'Cargando...' : 'Ver mis datos' }}
          </button>
        }

        @if (exportError()) {
          <p class="mt-2 text-sm text-red-600">{{ exportError() }}</p>
        }
      </div>

      <!-- Delete Account Section -->
      <div class="mt-8 rounded-2xl border border-red-200 bg-white p-6 shadow-sm sm:p-8">
        <h2 class="text-lg font-semibold text-red-900">Eliminar mi cuenta</h2>
        <p class="mt-1 text-sm text-neutral-600">
          Solicita la baja de tu cuenta y la anonimización de tus datos personales (Art. 17 RGPD).
          Esta acción es irreversible.
        </p>

        <div class="mt-4 rounded-lg border border-amber-200 bg-amber-50 p-4">
          <p class="text-sm text-amber-800">
            <strong>Consecuencias de la baja:</strong>
          </p>
          <ul class="mt-2 list-inside list-disc text-sm text-amber-700">
            <li>Tus datos personales serán anonimizados</li>
            <li>Tu email será reemplazado por un identificador anónimo</li>
            <li>Los torneos que creaste serán transferidos al administrador</li>
            <li>No podrás recuperar tu cuenta</li>
          </ul>
        </div>

        <form [formGroup]="deleteForm" (ngSubmit)="deleteAccount()" class="mt-4">
          <div>
            <label class="mb-1 block text-sm font-medium text-neutral-700" for="password">
              Confirma tu contraseña *
            </label>
            <input
              id="password"
              type="password"
              formControlName="password"
              class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-red-500"
              placeholder="Introduce tu contraseña para confirmar"
            />
          </div>

          @if (deleteError()) {
            <p class="mt-2 text-sm text-red-600">{{ deleteError() }}</p>
          }

          @if (deleteSuccess()) {
            <p class="mt-2 text-sm text-emerald-700">{{ deleteSuccess() }}</p>
          }

          <button
            type="submit"
            [disabled]="deleteForm.invalid || isDeleting()"
            class="mt-4 rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-red-700 disabled:opacity-60"
          >
            {{ isDeleting() ? 'Eliminando...' : 'Eliminar mi cuenta' }}
          </button>
        </form>
      </div>
    </section>
  `
})
export class AccountSettingsComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  readonly exportData = signal<AccountExportData | null>(null);
  readonly isLoadingExport = signal(false);
  readonly exportError = signal<string | null>(null);

  readonly isDeleting = signal(false);
  readonly deleteError = signal<string | null>(null);
  readonly deleteSuccess = signal<string | null>(null);

  readonly deleteForm = this.fb.nonNullable.group({
    password: ['', [Validators.required]]
  });

  ngOnInit(): void {}

  loadExportData(): void {
    this.isLoadingExport.set(true);
    this.exportError.set(null);

    this.authService.exportAccountData().subscribe({
      next: data => {
        this.exportData.set(data);
        this.isLoadingExport.set(false);
      },
      error: error => {
        this.exportError.set(getApiErrorMessage(error, 'No se pudieron cargar tus datos.'));
        this.isLoadingExport.set(false);
      }
    });
  }

  downloadExport(): void {
    const data = this.exportData();
    if (!data) return;

    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `datos-cuenta-${new Date().toISOString().split('T')[0]}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }

  deleteAccount(): void {
    if (this.deleteForm.invalid || this.isDeleting()) return;

    this.isDeleting.set(true);
    this.deleteError.set(null);
    this.deleteSuccess.set(null);

    const password = this.deleteForm.get('password')!.value;

    this.authService.deleteAccount(password).subscribe({
      next: response => {
        this.deleteSuccess.set(response.message);
        this.isDeleting.set(false);
        this.authService.logout().subscribe(() => {
          this.router.navigateByUrl('/');
        });
      },
      error: error => {
        this.deleteError.set(getApiErrorMessage(error, 'No se pudo eliminar la cuenta. Verifica tu contraseña.'));
        this.isDeleting.set(false);
      }
    });
  }
}
