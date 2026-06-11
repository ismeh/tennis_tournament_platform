import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { getApiErrorMessage } from '../../core/errors/api-error.util';

const EMAIL_PATTERN = /^(?=.{1,254}$)(?=.{1,64}@)(?!.*\.\.)[A-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?\.)+[A-Z]{2,63}$/i;

@Component({
	selector: 'app-login-page',
	standalone: true,
	imports: [CommonModule, ReactiveFormsModule, RouterLink],
	template: `
		<section class="mx-auto max-w-md px-4 py-10 sm:py-16">
			<div class="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
				<h1 class="text-2xl font-bold text-neutral-900">Iniciar sesión</h1>
				<p class="mt-2 text-sm text-neutral-600">Accede para gestionar torneos y partidos.</p>

				<form class="mt-6 space-y-4" [formGroup]="form" (ngSubmit)="submit()">
					<div>
						<label class="mb-1 block text-sm font-medium text-neutral-700" for="email">Email</label>
						<input
							id="email"
							type="email"
							formControlName="email"
							class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
							placeholder="tu@email.com"
						/>
					</div>

					<div>
						<label class="mb-1 block text-sm font-medium text-neutral-700" for="password">Contraseña</label>
						<input
							id="password"
							type="password"
							formControlName="password"
							class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
							placeholder="********"
						/>
					</div>

					@if (errorMessage()) {
						<p class="text-sm text-red-600">{{ errorMessage() }}</p>
					}

					<button
						type="submit"
						class="w-full rounded-lg bg-primary-500 px-4 py-2 font-medium text-white transition-colors hover:bg-primary-600 disabled:opacity-60"
						[disabled]="form.invalid || isSubmitting()"
					>
						{{ isSubmitting() ? 'Entrando...' : 'Iniciar sesión' }}
					</button>
				</form>

				<p class="mt-6 text-sm text-neutral-600">
					¿Aún no tienes cuenta?
					<a routerLink="/register" class="font-medium text-primary-600 hover:text-primary-700">Regístrate</a>
				</p>
			</div>
		</section>
	`
})
export class LoginComponent {
	private readonly fb = inject(FormBuilder);
	private readonly authService = inject(AuthService);
	private readonly router = inject(Router);
	private readonly route = inject(ActivatedRoute);

	readonly isSubmitting = signal(false);
	readonly errorMessage = signal<string | null>(null);

	readonly form = this.fb.nonNullable.group({
		email: ['', [Validators.required, Validators.pattern(EMAIL_PATTERN)]],
		password: ['', [Validators.required, Validators.minLength(6)]]
	});

	submit(): void {
		if (this.form.invalid || this.isSubmitting()) {
			return;
		}

		this.isSubmitting.set(true);
		this.errorMessage.set(null);

		this.authService.login(this.form.getRawValue()).subscribe({
			next: () => {
				this.isSubmitting.set(false);
				this.router.navigateByUrl(this.getRedirectUrl());
			},
			error: (error) => {
				this.isSubmitting.set(false);
				this.errorMessage.set(getApiErrorMessage(error, 'No se pudo iniciar sesión. Revisa tus credenciales.'));
			}
		});
	}

	private getRedirectUrl(): string {
		return this.normalizeRedirectUrl(this.route.snapshot.queryParamMap.get('returnUrl')) ?? '/torneos';
	}

	private normalizeRedirectUrl(returnUrl: string | null): string | null {
		const redirectUrl = returnUrl?.trim();
		if (!redirectUrl || !redirectUrl.startsWith('/') || redirectUrl.startsWith('//')) {
			return null;
		}

		const path = redirectUrl.split(/[?#]/)[0];
		if (path === '/login' || path === '/register' || path === '/confirmar-email') {
			return null;
		}

		return redirectUrl;
	}
}
