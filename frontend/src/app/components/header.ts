import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { AppSettings } from '../shared/constants';
import { alpha3ToAlpha2 } from '../shared/country-flag.util';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <header class="fixed inset-x-0 top-0 z-50 border-b border-neutral-200 bg-white shadow-sm backdrop-blur supports-[backdrop-filter]:bg-white/95">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-16">
          <!-- Logo -->
          <a routerLink="/" (click)="closeMobileMenu()" class="flex items-center gap-2 group">
            <img src="/logo_sin_texto_resized.svg" alt="Tennis Platform Logo" class="w-10 h-10 group-hover:shadow-lg transition-shadow">
            <span class="text-xl font-bold bg-gradient-to-r from-primary-600 to-accent-600 bg-clip-text text-transparent hidden sm:inline">
              {{ AppSettings.PROJECT_NAME }}
            </span>
          </a>

          <!-- Desktop Navigation Links -->
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

          <!-- Desktop Auth Buttons -->
          <div class="hidden md:flex items-center gap-2 sm:gap-3">
            @if (isLoggedIn$ | async) {
              @if ((role$ | async) === 'ORGANIZER') {
                <a
                  routerLink="/torneos/crear"
                  class="inline-flex whitespace-nowrap rounded-lg bg-primary-500 px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-primary-600 sm:px-4"
                >
                  Crear torneo
                </a>
              } @else if ((role$ | async) === 'UMPIRE') {
                <a
                  routerLink="/torneos"
                  class="inline-flex whitespace-nowrap rounded-lg bg-primary-500 px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-primary-600 sm:px-4"
                >
                  Gestionar resultados
                </a>
              } @else {
                <a
                  routerLink="/torneos"
                  class="inline-flex whitespace-nowrap rounded-lg bg-primary-500 px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-primary-600 sm:px-4"
                >
                  Inscribirse
                </a>
              }
              <div class="relative flex items-center gap-0.5 border border-neutral-200 bg-white rounded-full pl-2 pr-1 py-1 hover:bg-neutral-50 transition-colors">
                <a
                  routerLink="/perfil"
                  class="flex items-center gap-2 text-sm font-medium text-neutral-700 hover:text-primary-600 focus:outline-none"
                  title="Ver Perfil"
                >
                  <div class="flex h-8 w-8 items-center justify-center rounded-full bg-primary-500 text-xs font-semibold text-white">
                    {{ getUserInitial(displayName$ | async) }}
                  </div>
                  @if (countryCode(); as cc) {
                    <span class="fi fi-{{ cc }}"></span>
                  }
                  <span class="max-w-[120px] truncate">
                    {{ resolveDisplayName(displayName$ | async) }}
                  </span>
                </a>
                <button
                  type="button"
                  (click)="toggleProfileMenu()"
                  class="flex items-center justify-center rounded-full p-1.5 text-neutral-500 hover:bg-neutral-200 hover:text-neutral-700 focus:outline-none transition-colors"
                  title="Abrir menú"
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    class="h-4 w-4 transition-transform duration-200"
                    [class.rotate-180]="isProfileMenuOpen()"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <polyline points="6 9 12 15 18 9"></polyline>
                  </svg>
                </button>
                @if (isProfileMenuOpen()) {
                  <div class="absolute right-0 top-12 z-50 w-44 overflow-hidden rounded-lg border border-neutral-200 bg-white py-1 shadow-lg">
                    <a
                      routerLink="/perfil"
                      class="block px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-primary-50 hover:text-primary-700"
                      (click)="closeProfileMenu()"
                    >
                      Perfil
                    </a>
                    <a
                      routerLink="/ajustes-cuenta"
                      class="block px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-primary-50 hover:text-primary-700"
                      (click)="closeProfileMenu()"
                    >
                      Ajustes de cuenta
                    </a>
                    @if ((role$ | async) === 'ORGANIZER') {
                      <a
                        routerLink="/mis-categorias"
                        class="block px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-primary-50 hover:text-primary-700"
                        (click)="closeProfileMenu()"
                      >
                        Mis Categorías
                      </a>
                    }
                    <button
                      type="button"
                      (click)="onLogout()"
                      class="block w-full px-4 py-2 text-left text-sm font-medium text-neutral-700 hover:bg-primary-50 hover:text-primary-700"
                    >
                      Cerrar sesión
                    </button>
                  </div>
                }
              </div>
            } @else {
              <a
                routerLink="/login"
                [queryParams]="loginQueryParams()"
                class="px-3 py-2 text-sm font-medium text-primary-600 transition-colors hover:text-primary-700 sm:px-4"
              >
                Iniciar Sesión
              </a>
              <a
                routerLink="/register"
                class="inline-flex whitespace-nowrap rounded-lg bg-primary-500 px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-primary-600 sm:px-4"
              >
                Comienza
              </a>
            }
          </div>

          <!-- Mobile Hamburger Button -->
          <button
            type="button"
            (click)="toggleMobileMenu()"
            class="md:hidden inline-flex items-center justify-center rounded-lg p-2 text-neutral-600 transition-colors hover:bg-primary-50 hover:text-primary-600 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"
            [attr.aria-expanded]="isMobileMenuOpen()"
            aria-label="Abrir menú de navegación"
          >
            @if (isMobileMenuOpen()) {
              <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            } @else {
              <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
              </svg>
            }
          </button>
        </div>
      </div>

      <!-- Mobile Menu Panel -->
      @if (isMobileMenuOpen()) {
        <div class="md:hidden border-t border-neutral-200 bg-white shadow-lg max-h-[calc(100vh-4rem)] overflow-y-auto">
          <nav class="max-w-7xl mx-auto px-4 py-3 space-y-1">
            <a routerLink="/" routerLinkActive="text-primary-600 bg-primary-50"
               [routerLinkActiveOptions]="{ exact: true }"
               (click)="closeMobileMenu()"
               class="block px-4 py-2.5 rounded-lg text-neutral-600 hover:text-primary-600 hover:bg-primary-50 transition-colors font-medium text-sm">
              Inicio
            </a>
            <a routerLink="/torneos" routerLinkActive="text-primary-600 bg-primary-50"
               (click)="closeMobileMenu()"
               class="block px-4 py-2.5 rounded-lg text-neutral-600 hover:text-primary-600 hover:bg-primary-50 transition-colors font-medium text-sm">
              Torneos
            </a>
            <a routerLink="/ranking" routerLinkActive="text-primary-600 bg-primary-50"
               (click)="closeMobileMenu()"
               class="block px-4 py-2.5 rounded-lg text-neutral-600 hover:text-primary-600 hover:bg-primary-50 transition-colors font-medium text-sm">
              Ranking
            </a>
            <a routerLink="/como-funciona" routerLinkActive="text-primary-600 bg-primary-50"
               (click)="closeMobileMenu()"
               class="block px-4 py-2.5 rounded-lg text-neutral-600 hover:text-primary-600 hover:bg-primary-50 transition-colors font-medium text-sm">
              Cómo Funciona
            </a>
            <a routerLink="/contacto" routerLinkActive="text-primary-600 bg-primary-50"
               (click)="closeMobileMenu()"
               class="block px-4 py-2.5 rounded-lg text-neutral-600 hover:text-primary-600 hover:bg-primary-50 transition-colors font-medium text-sm">
              Contacto
            </a>
          </nav>

          <!-- Mobile Auth Section -->
          <div class="border-t border-neutral-200 px-4 py-3">
            @if (isLoggedIn$ | async) {
              <a
                routerLink="/perfil"
                (click)="closeMobileMenu()"
                class="flex items-center gap-3 mb-3 px-2 py-1.5 rounded-lg hover:bg-neutral-50 transition-colors"
              >
                <div class="flex h-9 w-9 items-center justify-center rounded-full bg-primary-500 text-sm font-semibold text-white">
                  {{ getUserInitial(displayName$ | async) }}
                </div>
                <div class="flex flex-col">
                  <span class="text-sm font-medium text-neutral-800">
                    {{ resolveDisplayName(displayName$ | async) }}
                  </span>
                  @if (countryCode(); as cc) {
                    <span class="text-xs text-neutral-500">
                      <span class="fi fi-{{ cc }}"></span>
                    </span>
                  }
                </div>
              </a>
              @if ((role$ | async) === 'ORGANIZER') {
                <a
                  routerLink="/torneos/crear"
                  (click)="closeMobileMenu()"
                  class="block w-full text-center rounded-lg bg-primary-500 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-600 mb-2"
                >
                  Crear torneo
                </a>
              } @else if ((role$ | async) === 'UMPIRE') {
                <a
                  routerLink="/torneos"
                  (click)="closeMobileMenu()"
                  class="block w-full text-center rounded-lg bg-primary-500 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-600 mb-2"
                >
                  Gestionar resultados
                </a>
              } @else {
                <a
                  routerLink="/torneos"
                  (click)="closeMobileMenu()"
                  class="block w-full text-center rounded-lg bg-primary-500 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-600 mb-2"
                >
                  Inscribirse
                </a>
              }

              <!-- Perfil accordion -->
              <div class="space-y-1">
                <button
                  type="button"
                  (click)="toggleMobileProfile()"
                  class="flex w-full items-center justify-between px-4 py-2.5 rounded-lg text-sm font-medium text-neutral-600 hover:bg-primary-50 hover:text-primary-700 transition-colors"
                >
                  <span>Perfil</span>
                  <svg class="h-4 w-4 text-neutral-400 transition-transform duration-200"
                       [class.rotate-180]="isMobileProfileOpen()">
                    <path fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
                          d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                  </svg>
                </button>
                @if (isMobileProfileOpen()) {
                  <div class="ml-4 space-y-1">
                    <a
                      routerLink="/perfil"
                      (click)="closeMobileMenu()"
                      class="block px-4 py-2 rounded-lg text-sm font-medium text-neutral-500 hover:bg-primary-50 hover:text-primary-700 transition-colors"
                    >
                      Ver perfil
                    </a>
                    <a
                      routerLink="/ajustes-cuenta"
                      (click)="closeMobileMenu()"
                      class="block px-4 py-2 rounded-lg text-sm font-medium text-neutral-500 hover:bg-primary-50 hover:text-primary-700 transition-colors"
                    >
                      Ajustes de cuenta
                    </a>
                    @if ((role$ | async) === 'ORGANIZER') {
                      <a
                        routerLink="/mis-categorias"
                        (click)="closeMobileMenu()"
                        class="block px-4 py-2 rounded-lg text-sm font-medium text-neutral-500 hover:bg-primary-50 hover:text-primary-700 transition-colors"
                      >
                        Mis Categorías
                      </a>
                    }
                    <button
                      type="button"
                      (click)="onLogout(); closeMobileMenu()"
                      class="block w-full px-4 py-2 text-left rounded-lg text-sm font-medium text-neutral-500 hover:bg-primary-50 hover:text-primary-700 transition-colors"
                    >
                      Cerrar sesión
                    </button>
                  </div>
                }
              </div>
            } @else {
              <div class="space-y-2">
                <a
                  routerLink="/login"
                  [queryParams]="loginQueryParams()"
                  (click)="closeMobileMenu()"
                  class="block w-full text-center rounded-lg border border-primary-500 px-4 py-2.5 text-sm font-semibold text-primary-600 transition-colors hover:bg-primary-50"
                >
                  Iniciar Sesión
                </a>
                <a
                  routerLink="/register"
                  (click)="closeMobileMenu()"
                  class="block w-full text-center rounded-lg bg-primary-500 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-600"
                >
                  Comienza
                </a>
              </div>
            }
          </div>
        </div>
      }
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
  readonly role$ = this.authService.role$;
  readonly nationality$ = this.authService.nationality$;
  readonly isProfileMenuOpen = signal(false);
  readonly isMobileMenuOpen = signal(false);
  readonly isMobileProfileOpen = signal(false);

  readonly countryCode = signal<string | null>(null);

  constructor() {
    this.nationality$.subscribe(n => this.countryCode.set(alpha3ToAlpha2(n)));
  }

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
    this.closeProfileMenu();
    this.closeMobileMenu();
    this.authService.logout().subscribe(() => {
      this.router.navigateByUrl('/');
    });
  }

  toggleProfileMenu(): void {
    this.isProfileMenuOpen.update(isOpen => !isOpen);
  }

  closeProfileMenu(): void {
    this.isProfileMenuOpen.set(false);
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update(isOpen => !isOpen);
    this.closeProfileMenu();
    this.isMobileProfileOpen.set(false);
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen.set(false);
    this.isMobileProfileOpen.set(false);
  }

  toggleMobileProfile(): void {
    this.isMobileProfileOpen.update(isOpen => !isOpen);
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
