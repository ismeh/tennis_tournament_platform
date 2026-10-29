import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { getApiErrorMessage } from '../../core/errors/api-error.util';

@Component({
	selector: 'app-confirm-email-page',
	standalone: true,
	imports: [CommonModule, RouterLink],
	template: `
		<section class="mx-auto max-w-md px-4 py-10 sm:py-16">
			<div class="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
				<h1 class="text-2xl font-bold text-neutral-900">Confirmar email</h1>

				@if (isLoading()) {
					<p class="mt-4 text-sm text-neutral-600">Confirmando tu email...</p>
				} @else {
					<p class="mt-4 text-sm" [class.text-green-700]="isSuccess()" [class.text-red-600]="!isSuccess()">
						{{ message() }}
					</p>

					<a
						routerLink="/login"
						class="mt-6 inline-flex w-full justify-center rounded-lg bg-primary-500 px-4 py-2 font-medium text-white transition-colors hover:bg-primary-600"
					>
						Ir a iniciar sesión
					</a>
				}
			</div>
		</section>
	`
})
export class ConfirmEmailComponent {
	private readonly route = inject(ActivatedRoute);
	private readonly authService = inject(AuthService);

	readonly isLoading = signal(true);
	readonly isSuccess = signal(false);
	readonly message = signal('No se pudo confirmar el email.');

	constructor() {
		const token = this.route.snapshot.queryParamMap.get('token');
		if (!token) {
			this.isLoading.set(false);
			this.message.set('El enlace de confirmación no es válido.');
			return;
		}

		this.authService.confirmEmail(token).subscribe({
			next: (response) => {
				this.isLoading.set(false);
				this.isSuccess.set(true);
				this.message.set(response.message);
			},
			error: (error) => {
				this.isLoading.set(false);
				this.message.set(getApiErrorMessage(error, 'El enlace de confirmación no es válido o ha caducado.'));
			}
		});
	}
}
