import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { UserRole } from '../../core/auth/auth.model';
import { getApiErrorMessage } from '../../core/errors/api-error.util';

const EMAIL_PATTERN = /^(?=.{1,254}$)(?=.{1,64}@)(?!.*\.\.)[A-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?\.)+[A-Z]{2,63}$/i;

@Component({
	selector: 'app-register-page',
	standalone: true,
	imports: [CommonModule, ReactiveFormsModule, RouterLink],
	template: `
		<section class="mx-auto max-w-md px-4 py-10 sm:py-16">
			<div class="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
				<h1 class="text-2xl font-bold text-neutral-900">Crear cuenta</h1>
				<p class="mt-2 text-sm text-neutral-600">Regístrate para empezar a usar la plataforma.</p>

				<form class="mt-6 space-y-4" [formGroup]="form" (ngSubmit)="submit()">
					<div>
						<label class="mb-1 block text-sm font-medium text-neutral-700">Tipo de cuenta</label>
						<div class="grid grid-cols-3 gap-3">
							<button type="button"
								class="rounded-lg border-2 px-4 py-3 text-center transition-all"
								[class.border-primary-500]="form.get('role')?.value === 'PLAYER'"
								[class.border-neutral-200]="form.get('role')?.value !== 'PLAYER'"
								[class.bg-primary-50]="form.get('role')?.value === 'PLAYER'"
								(click)="form.patchValue({ role: 'PLAYER' })">
								<span class="block text-lg mb-1">🎾</span>
								<span class="block text-sm font-medium">Jugador</span>
								<span class="block text-xs text-neutral-500">Inscribirte en torneos</span>
							</button>
							<button type="button"
								class="rounded-lg border-2 px-4 py-3 text-center transition-all"
								[class.border-primary-500]="form.get('role')?.value === 'ORGANIZER'"
								[class.border-neutral-200]="form.get('role')?.value !== 'ORGANIZER'"
								[class.bg-primary-50]="form.get('role')?.value === 'ORGANIZER'"
								(click)="form.patchValue({ role: 'ORGANIZER' })">
								<span class="block text-lg mb-1">🏆</span>
								<span class="block text-sm font-medium">Organizador</span>
								<span class="block text-xs text-neutral-500">Crear y gestionar torneos</span>
							</button>
							<button type="button"
								class="rounded-lg border-2 px-4 py-3 text-center transition-all"
								[class.border-primary-500]="form.get('role')?.value === 'UMPIRE'"
								[class.border-neutral-200]="form.get('role')?.value !== 'UMPIRE'"
								[class.bg-primary-50]="form.get('role')?.value === 'UMPIRE'"
								(click)="form.patchValue({ role: 'UMPIRE' })">
								<span class="block text-lg mb-1">⚖️</span>
								<span class="block text-sm font-medium">Anotador</span>
								<span class="block text-xs text-neutral-500">Registrar resultados</span>
							</button>
						</div>
					</div>

					<div>
						<label class="mb-1 block text-sm font-medium text-neutral-700" for="email">Email</label>
						<input
							id="email"
							type="email"
							formControlName="email"
							class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
							placeholder="tu&#64;email.com"
						/>
					</div>

					<div>
						<label class="mb-1 block text-sm font-medium text-neutral-700" for="password">Contraseña</label>
						<input
							id="password"
							type="password"
							formControlName="password"
							class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
							placeholder="Mínimo 6 caracteres"
						/>
					</div>

					<div class="flex items-start gap-3">
						<input
							id="privacyAccepted"
							type="checkbox"
							formControlName="privacyPolicyAccepted"
							class="mt-1 h-4 w-4 rounded border-neutral-300 text-primary-500 focus:ring-primary-500"
						/>
						<label for="privacyAccepted" class="text-sm text-neutral-600">
							Acepto la
							<a href="/politica-privacidad" target="_blank" class="font-medium text-primary-600 hover:text-primary-700">política de privacidad</a>
							y los
							<a href="/terminos-condiciones" target="_blank" class="font-medium text-primary-600 hover:text-primary-700">términos y condiciones</a>.
						</label>
					</div>

					@if (errorMessage()) {
						<p class="text-sm text-red-600">{{ errorMessage() }}</p>
					}

					@if (successMessage()) {
						<p class="rounded-lg border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-700">{{ successMessage() }}</p>
					}

					<button
						type="submit"
						class="w-full rounded-lg bg-primary-500 px-4 py-2 font-medium text-white transition-colors hover:bg-primary-600 disabled:opacity-60"
						[disabled]="form.invalid || isSubmitting()"
					>
						{{ isSubmitting() ? 'Creando cuenta...' : 'Registrarme' }}
					</button>
				</form>

				<p class="mt-6 text-sm text-neutral-600">
					¿Ya tienes cuenta?
					<a routerLink="/login" class="font-medium text-primary-600 hover:text-primary-700">Inicia sesión</a>
				</p>
			</div>
		</section>
	`
})
export class RegisterComponent {
	private readonly fb = inject(FormBuilder);
	private readonly authService = inject(AuthService);

	readonly isSubmitting = signal(false);
	readonly errorMessage = signal<string | null>(null);
	readonly successMessage = signal<string | null>(null);

	readonly form = this.fb.nonNullable.group({
		role: ['PLAYER' as UserRole, Validators.required],
		email: ['', [Validators.required, Validators.pattern(EMAIL_PATTERN)]],
		password: ['', [Validators.required, Validators.minLength(6)]],
		privacyPolicyAccepted: [false, [Validators.requiredTrue]]
	});

	submit(): void {
		if (this.form.invalid || this.isSubmitting()) {
			return;
		}

		this.isSubmitting.set(true);
		this.errorMessage.set(null);
		this.successMessage.set(null);

		const raw = this.form.getRawValue();
		this.authService.register({
			email: raw.email,
			password: raw.password,
			role: raw.role,
			privacyPolicyAccepted: raw.privacyPolicyAccepted
		}).subscribe({
			next: (response) => {
				this.isSubmitting.set(false);
				this.successMessage.set(response.message);
			},
			error: (error) => {
				this.isSubmitting.set(false);
				this.errorMessage.set(getApiErrorMessage(error, 'No se pudo registrar la cuenta. Revisa los datos e inténtalo de nuevo.'));
			}
		});
	}
}
