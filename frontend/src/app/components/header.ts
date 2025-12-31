import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AppSettings } from '../shared/constants';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <header class="sticky top-0 z-50 bg-white border-b border-neutral-200 shadow-sm">
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
            <a routerLink="/iniciar-sesion" class="px-4 py-2 text-primary-600 font-medium text-sm hover:text-primary-700 transition-colors">
              Iniciar Sesión
            </a>
            <a routerLink="/registrarse" class="px-4 py-2 sm:px-6 py-2 bg-primary-500 text-white font-medium text-sm rounded-lg hover:bg-primary-600 transition-colors">
              Registrarse
            </a>
          </div>
        </div>
      </div>
    </header>
  `,
  styles: []
})
export class HeaderComponent {
  AppSettings: any = AppSettings;
}
