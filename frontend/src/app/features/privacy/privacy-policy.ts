import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService, LegalDocument } from '../../core/auth/auth.service';

@Component({
  selector: 'app-privacy-policy',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="bg-white">
      <div class="mx-auto max-w-4xl px-4 py-10 sm:px-6 md:py-16 lg:px-8">
        <div class="mb-8">
          <a routerLink="/" class="text-sm font-medium text-primary-600 hover:text-primary-700">&larr; Volver al inicio</a>
        </div>

        @if (isLoading()) {
          <div class="flex justify-center py-12">
            <div class="h-8 w-8 animate-spin rounded-full border-4 border-primary-500 border-t-transparent"></div>
          </div>
        } @else if (error()) {
          <div class="rounded-lg border border-red-200 bg-red-50 p-4 text-red-700">
            {{ error() }}
          </div>
        } @else if (document()) {
          <div [innerHTML]="document()!.contentSnapshot"></div>
        }
      </div>
    </section>
  `,
  styles: []
})
export class PrivacyPolicyComponent implements OnInit {
  private readonly authService = inject(AuthService);

  readonly document = signal<LegalDocument | null>(null);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.loadDocument();
  }

  private loadDocument(): void {
    this.isLoading.set(true);
    this.authService.getLegalDocument('PRIVACY_POLICY').subscribe({
      next: (doc) => {
        this.document.set(doc);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('No se pudo cargar la política de privacidad.');
        this.isLoading.set(false);
      }
    });
  }
}
