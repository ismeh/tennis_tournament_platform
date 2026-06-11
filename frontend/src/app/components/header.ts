import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { AppSettings } from '../shared/constants';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <header class="fixed inset-x-0 top-0 z-50 border-b border-neutral-200 bg-white shadow-sm backdrop-blur supports-[backdrop-filter]:bg-white/95">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-16">
          <!-- Logo -->
          <a routerLink="/" class="flex items-center gap-2 group">
            <div class="w-10 h-10 bg-gradient-to-br from-primary-500 to-accent-500 rounded-lg flex items-center justify-center text-white font-bold text-lg group-hover:shadow-lg transition-shadow">
              🎾
            </div>
            <span class="text-xl font-bold bg-gradient-to-r from-primary-600 to-accent-600 bg-clip-text text-transparent hidden sm:inline">
              {{ AppSettings.PROJECT_NAME }}
            </span>
          </a>

          <!-- Navigation Links -->
          <nav class="hidden md:flex items-center gap-1">
            <a routerLink="/" routerLinkActive="text-primary-600 bg-primary-50" 
               [routerLinkActiveOptions]="{ exact: true }"
               class="px-4 py-2 rounded-lg text-neutral-600 hover:text-primary-600 hover:bg-primary-50 transition-colors font-medium text-sm">
              Inicio
            </a>
            <a routerLink="/torneos" routerLinkActive="text-primary-600 bg-primary-50"
               class="px-4 py-2 rounded-lg text-neutral-600 hover:text-primary-600 hover:bg-primary-50 transition-colors font-medium text-sm">
              Torneos
            </a>
            <a routerLink="/calendario" routerLinkActive="text-primary-600 bg-primary-50"
               class="px-4 py-2 rounded-lg text-neutral-600 hover:text-primary-600 hover:bg-primary-50 transition-colors font-medium text-sm">
              Calendario
            </a>
            <a routerLink="/ranking" routerLinkActive="text-primary-600 bg-primary-50"
               class="px-4 py-2 rounded-lg text-neutral-600 hover:text-primary-600 hover:bg-primary-50 transition-colors font-medium text-sm">
              Ranking
            </a>
            <a routerLink="/como-funciona" routerLinkActive="text-primary-600 bg-primary-50"
               class="px-4 py-2 rounded-lg text-neutral-600 hover:text-primary-600 hover:bg-primary-50 transition-colors font-medium text-sm">
              Cómo Funciona
            </a>
            <a routerLink="/contacto" routerLinkActive="text-primary-600 bg-primary-50"
               class="px-4 py-2 rounded-lg text-neutral-600 hover:text-primary-600 hover:bg-primary-50 transition-colors font-medium text-sm">
              Contacto
            </a>
          </nav>

          <!-- Auth Buttons -->
          <div class="flex items-center gap-2 sm:gap-3">
            @if (isLoggedIn$ | async) {
              <div class="flex items-center gap-2 sm:gap-3">
                <div class="flex h-9 w-9 items-center justify-center rounded-full bg-primary-500 text-sm font-semibold text-white">
                  {{ getUserInitial(displayName$ | async) }}
                </div>
                <span class="text-sm font-medium text-neutral-700">{{ resolveDisplayName(displayName$ | async) }}</span>
                <a
                  routerLink="/perfil"
                  class="px-3 py-2 text-sm font-medium text-neutral-600 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
                >
                  Mi perfil
                </a>
                <button
                  type="button"
                  (click)="onLogout()"
                  class="px-3 py-2 text-sm font-medium text-neutral-600 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
                >
                  Cerrar sesión
                </button>
              </div>
            } @else {
              <a
                routerLink="/login"
                [queryParams]="loginQueryParams()"
                class="px-4 py-2 text-primary-600 font-medium text-sm hover:text-primary-700 transition-colors"
              >
                Iniciar Sesión
              </a>
              <a routerLink="/register" class="px-4 py-2 sm:px-6 bg-primary-500 text-white font-medium text-sm rounded-lg hover:bg-primary-600 transition-colors">
                Registrarse
              </a>
            }
          </div>
        </div>
      </div>
    </header>
  `,
  styles: []
})
export class HeaderComponent {
  AppSettings: typeof AppSettings = AppSettings;
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly isLoggedIn$ = this.authService.isLoggedIn$;
  readonly displayName$ = this.authService.displayName$;

  getUserInitial(displayName: string | null): string {
    const name = displayName ?? this.authService.getCurrentUserEmail()?.split('@')[0] ?? null;
    if (!name) {
      return '?';
    }

    return name.charAt(0).toUpperCase();
  }

  resolveDisplayName(displayName: string | null): string {
    if (displayName) {
      return displayName;
    }

    const email = this.authService.getCurrentUserEmail();
    if (email) {
      return email.split('@')[0];
    }

    return 'Player';
  }

  loginQueryParams(): { returnUrl: string } {
    return { returnUrl: this.resolveLoginReturnUrl() };
  }

  onLogout(): void {
    this.authService.logout().subscribe(() => {
      this.router.navigateByUrl('/');
    });
  }

  private resolveLoginReturnUrl(): string {
    const currentUrl = this.router.url || '/torneos';
    const path = currentUrl.split(/[?#]/)[0];

    if (path === '/login' || path === '/register' || path === '/confirmar-email') {
      return '/torneos';
    }

    return currentUrl;
  }
}
