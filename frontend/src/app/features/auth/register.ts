import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
		<section class="mx-auto max-w-md px-4 py-10 sm:py-16">
			<div class="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
				<h1 class="text-2xl font-bold text-neutral-900">Crear cuenta</h1>
				<p class="mt-2 text-sm text-neutral-600">Registrate para empezar a usar la plataforma.</p>

				<form class="mt-6 space-y-4" [formGroup]="form" (ngSubmit)="submit()">
					<div>
						<label class="mb-1 block text-sm font-medium text-neutral-700" for="name">Nombre</label>
						<input
							id="name"
							type="text"
							formControlName="name"
							class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
							placeholder="Nombre y apellidos"
						/>
					</div>

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
						<label class="mb-1 block text-sm font-medium text-neutral-700" for="password">Contrasena</label>
						<input
							id="password"
							type="password"
							formControlName="password"
							class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
							placeholder="Minimo 6 caracteres"
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
						{{ isSubmitting() ? 'Creando cuenta...' : 'Registrarme' }}
					</button>
				</form>

				<p class="mt-6 text-sm text-neutral-600">
					Ya tienes cuenta?
					<a routerLink="/iniciar" class="font-medium text-primary-600 hover:text-primary-700">Inicia sesion</a>
				</p>
			</div>
		</section>
	`
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  submit(): void {
    if (this.form.invalid || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    this.authService.register(this.form.getRawValue()).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.router.navigateByUrl('/');
      },
      error: () => {
        this.isSubmitting.set(false);
        this.errorMessage.set('No se pudo registrar la cuenta. Prueba con otro email.');
      }
    });
  }
}
