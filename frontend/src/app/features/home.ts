import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AppSettings } from '../shared/constants';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <!-- Hero Section -->
    <section class="relative overflow-hidden bg-white">
      <div class="absolute inset-0 bg-gradient-to-br from-primary-50 via-white to-accent-50"></div>
      <div class="absolute top-0 right-0 -z-10 w-96 h-96 bg-primary-200 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob"></div>
      <div class="absolute bottom-0 left-0 -z-10 w-96 h-96 bg-accent-200 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob" style="animation-delay: 2s"></div>

      <div class="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 md:py-20">
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          <!-- Left Content -->
          <div class="space-y-8">
            <div class="space-y-4">
              <p class="text-primary-600 font-semibold text-sm uppercase tracking-widest">Bienvenido al futuro del tenis</p>
              <h1 class="text-4xl md:text-5xl lg:text-6xl font-bold text-neutral-900 leading-tight">
                Organiza torneos de tenis con <span class="bg-gradient-to-r from-primary-600 to-accent-600 bg-clip-text text-transparent">Facilidad</span>
              </h1>
              <p class="text-lg text-neutral-600 leading-relaxed max-w-lg">
                {{ AppSettings.PROJECT_NAME }} es la plataforma todo en uno para gestionar torneos de tenis competitivos y aficionados. Desde el registro hasta los resultados en vivo, te tenemos cubierto.
              </p>
            </div>

            <div class="flex flex-col sm:flex-row gap-4">
              <a routerLink="/torneos" class="px-8 py-4 bg-gradient-to-r from-primary-500 to-primary-600 text-white font-semibold rounded-lg hover:shadow-lg hover:from-primary-600 hover:to-primary-700 transition-all transform hover:scale-105 text-center">
                Crear torneo
              </a>
              <a routerLink="/como-funciona" class="px-8 py-4 bg-white text-primary-600 font-semibold rounded-lg border-2 border-primary-200 hover:border-primary-400 hover:bg-primary-50 transition-all text-center">
                Cómo funciona
              </a>
            </div>

            <div class="flex flex-col sm:flex-row gap-6 pt-4">
              <div class="flex items-center gap-2">
                <div class="flex items-center justify-center w-8 h-8 rounded-full bg-primary-100">
                  <svg class="w-5 h-5 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                  </svg>
                </div>
                <span class="text-neutral-700 font-medium">Generación automática de brackets</span>
              </div>
              <div class="flex items-center gap-2">
                <div class="flex items-center justify-center w-8 h-8 rounded-full bg-primary-100">
                  <svg class="w-5 h-5 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                  </svg>
                </div>
                <span class="text-neutral-700 font-medium">Resultados en tiempo real</span>
              </div>
            </div>
          </div>

          <!-- Right Image -->
          <div class="relative hidden lg:block">
            <div class="relative w-full h-96 bg-gradient-to-br from-primary-100 to-accent-100 rounded-2xl overflow-hidden">
              <div class="absolute inset-0 flex items-center justify-center">
                <div class="text-center space-y-4 px-8">
                  <svg class="w-32 h-32 mx-auto text-primary-400 opacity-60" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M20.822 18.096c-3.439.794-6.64 1.7-8.822 0-2.182 1.7-5.385.936-8.822 0M4.33 6.75h15.34M2.75 9.75h18.5M4.33 12.75h15.34M2.75 16.25h18.5"/>
                  </svg>
                  <p class="text-primary-700 font-semibold">Organiza tu torneo hoy</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="py-8 md:py-20 bg-neutral-50">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-16 space-y-4">
          <p class="text-primary-600 font-semibold text-sm uppercase tracking-widest">Características</p>
          <h2 class="text-3xl md:text-4xl font-bold text-neutral-900">
            Todo lo que necesitas para ejecutar grandes torneos
          </h2>
          <p class="text-neutral-600 text-lg max-w-2xl mx-auto">
            Herramientas integrales diseñadas específicamente para la gestión de torneos de tenis
          </p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          <!-- Feature 1 -->
          <div class="bg-white rounded-xl p-8 border border-neutral-200 hover:border-primary-300 hover:shadow-lg transition-all group">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center mb-4 group-hover:bg-primary-200 transition-colors">
              <svg class="w-6 h-6 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                <path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v1h8v-1zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-1a4 4 0 00-4-4h-2v1h2a3 3 0 013 3v1h3z"/>
              </svg>
            </div>
            <h3 class="text-lg font-bold text-neutral-900 mb-2">Gestión de usuarios</h3>
            <p class="text-neutral-600">
              Organiza jugadores y organizadores de torneos. Registro sencillo y gestión de perfiles para todos.
            </p>
          </div>

          <!-- Feature 2 -->
          <div class="bg-white rounded-xl p-8 border border-neutral-200 hover:border-primary-300 hover:shadow-lg transition-all group">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center mb-4 group-hover:bg-primary-200 transition-colors">
              <svg class="w-6 h-6 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M4 4a2 2 0 00-2 2v4a1 1 0 001 1h12a1 1 0 001-1V6a2 2 0 00-2-2H4zm12 12H4c-1.1 0-2-.9-2-2v-4a1 1 0 00-1-1H1a1 1 0 001 1v4a4 4 0 004 4h12a1 1 0 001-1v-4a1 1 0 00-1-1h-1a1 1 0 001 1v4z" clip-rule="evenodd"/>
              </svg>
            </div>
            <h3 class="text-lg font-bold text-neutral-900 mb-2">Generación de brackets</h3>
            <p class="text-neutral-600">
              Creación automática de brackets de torneo con categorías (alevín, infantil, cadete, juvenil, adulto).
            </p>
          </div>

          <!-- Feature 3 -->
          <div class="bg-white rounded-xl p-8 border border-neutral-200 hover:border-primary-300 hover:shadow-lg transition-all group">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center mb-4 group-hover:bg-primary-200 transition-colors">
              <svg class="w-6 h-6 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clip-rule="evenodd"/>
              </svg>
            </div>
            <h3 class="text-lg font-bold text-neutral-900 mb-2">Gestión de horarios</h3>
            <p class="text-neutral-600">
              Asigna partidos a franjas horarias y canchas. Gestiona horarios con actualizaciones en tiempo real.
            </p>
          </div>

          <!-- Feature 4 -->
          <div class="bg-white rounded-xl p-8 border border-neutral-200 hover:border-primary-300 hover:shadow-lg transition-all group">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center mb-4 group-hover:bg-primary-200 transition-colors">
              <svg class="w-6 h-6 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M5 4a2 2 0 012-2h6a2 2 0 012 2v14l-5-2.5L5 18V4z" clip-rule="evenodd"/>
              </svg>
            </div>
            <h3 class="text-lg font-bold text-neutral-900 mb-2">Integración RFET</h3>
            <p class="text-neutral-600">
              Conecta con los rankings oficiales de la RFET para torneos competitivos y seeding automático.
            </p>
          </div>

          <!-- Feature 5 -->
          <div class="bg-white rounded-xl p-8 border border-neutral-200 hover:border-primary-300 hover:shadow-lg transition-all group">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center mb-4 group-hover:bg-primary-200 transition-colors">
              <svg class="w-6 h-6 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                <path d="M2 11a1 1 0 011-1h2a1 1 0 011 1v5a1 1 0 01-1 1H3a1 1 0 01-1-1v-5zM8 7a1 1 0 011-1h2a1 1 0 011 1v9a1 1 0 01-1 1H9a1 1 0 01-1-1V7zM14 4a1 1 0 011-1h2a1 1 0 011 1v12a1 1 0 01-1 1h-2a1 1 0 01-1-1V4z"/>
              </svg>
            </div>
            <h3 class="text-lg font-bold text-neutral-900 mb-2">Resultados en vivo</h3>
            <p class="text-neutral-600">
              Actualizaciones de resultados en tiempo real y progresión de brackets. Ve al instante quién avanza.
            </p>
          </div>

          <!-- Feature 6 -->
          <div class="bg-white rounded-xl p-8 border border-neutral-200 hover:border-primary-300 hover:shadow-lg transition-all group">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center mb-4 group-hover:bg-primary-200 transition-colors">
              <svg class="w-6 h-6 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M12.316 3.051a1 1 0 01.633 1.265l-4 12a1 1 0 11-1.898-.632l4-12a1 1 0 011.265-.633zM5.707 6.293a1 1 0 010 1.414L3.414 10l2.293 2.293a1 1 0 11-1.414 1.414l-3-3a1 1 0 010-1.414l3-3a1 1 0 011.414 0zm8.586 0a1 1 0 011.414 0l3 3a1 1 0 010 1.414l-3 3a1 1 0 11-1.414-1.414L16.586 10l-2.293-2.293a1 1 0 010-1.414z" clip-rule="evenodd"/>
              </svg>
            </div>
            <h3 class="text-lg font-bold text-neutral-900 mb-2">Torneos de consolación</h3>
            <p class="text-neutral-600">
              Crea brackets secundarios para eliminaciones en la primera ronda. Mantén a todos comprometidos.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- Tournament Types Section -->
    <section class="py-8 md:py-20 bg-white">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-16 space-y-4">
          <p class="text-primary-600 font-semibold text-sm uppercase tracking-widest">Tipos de torneos</p>
          <h2 class="text-3xl md:text-4xl font-bold text-neutral-900">
            Elige tu tipo de torneo
          </h2>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
          <!-- Competitive -->
          <div class="relative overflow-hidden rounded-2xl bg-gradient-to-br from-primary-50 to-primary-100 p-8 border-2 border-primary-200 hover:shadow-lg transition-shadow">
            <div class="absolute top-0 right-0 w-32 h-32 bg-primary-200 rounded-full -mr-16 -mt-16 opacity-30"></div>
            <div class="relative z-10">
              <div class="w-12 h-12 bg-primary-500 rounded-lg flex items-center justify-center text-white font-bold text-xl mb-4">
                🏆
              </div>
              <h3 class="text-2xl font-bold text-neutral-900 mb-3">Competitivo</h3>
              <p class="text-neutral-700 mb-6 leading-relaxed">
                Torneos profesionales con integración de rankings de la RFET. Seeding automático basado en rankings oficiales.
              </p>
              <ul class="space-y-2 text-neutral-700">
                <li class="flex items-center gap-2">
                  <span class="w-2 h-2 bg-primary-500 rounded-full"></span>
                  Integración de rankings RFET
                </li>
                <li class="flex items-center gap-2">
                  <span class="w-2 h-2 bg-primary-500 rounded-full"></span>
                  Seeding automático
                </li>
                <li class="flex items-center gap-2">
                  <span class="w-2 h-2 bg-primary-500 rounded-full"></span>
                  Jugadores verificados
                </li>
              </ul>
            </div>
          </div>

          <!-- Amateur -->
          <div class="relative overflow-hidden rounded-2xl bg-gradient-to-br from-accent-50 to-accent-100 p-8 border-2 border-accent-200 hover:shadow-lg transition-shadow">
            <div class="absolute top-0 right-0 w-32 h-32 bg-accent-200 rounded-full -mr-16 -mt-16 opacity-30"></div>
            <div class="relative z-10">
              <div class="w-12 h-12 bg-accent-500 rounded-lg flex items-center justify-center text-white font-bold text-xl mb-4">
                🎯
              </div>
              <h3 class="text-2xl font-bold text-neutral-900 mb-3">Aficionado</h3>
              <p class="text-neutral-700 mb-6 leading-relaxed">
                Torneos locales y comunitarios con sistemas de ranking internos configurables.
              </p>
              <ul class="space-y-2 text-neutral-700">
                <li class="flex items-center gap-2">
                  <span class="w-2 h-2 bg-accent-500 rounded-full"></span>
                  Rankings personalizados
                </li>
                <li class="flex items-center gap-2">
                  <span class="w-2 h-2 bg-accent-500 rounded-full"></span>
                  Categorías flexibles
                </li>
                <li class="flex items-center gap-2">
                  <span class="w-2 h-2 bg-accent-500 rounded-full"></span>
                  Impulsado por la comunidad
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="py-8 md:py-20 bg-gradient-to-r from-primary-600 to-accent-600 relative overflow-hidden">
      <div class="absolute inset-0 opacity-10">
        <div class="absolute inset-0 bg-pattern"></div>
      </div>
      <div class="relative z-10 max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-8">
        <h2 class="text-3xl md:text-4xl font-bold text-white">
          ¿Listo para transformar tus torneos?
        </h2>
        <p class="text-xl text-white text-opacity-90 max-w-2xl mx-auto">
          Únete a miles de organizadores de torneos y jugadores que usan {{ AppSettings.PROJECT_NAME }} hoy.
        </p>
        <div class="flex flex-col sm:flex-row gap-4 justify-center">
          <a routerLink="/register" class="px-8 py-4 bg-white text-primary-600 font-semibold rounded-lg hover:bg-neutral-50 transition-all transform hover:scale-105 text-center">
            Comienza gratis
          </a>
          <a routerLink="/contact" class="px-8 py-4 bg-white bg-opacity-20 text-white font-semibold rounded-lg border-2 border-white border-opacity-30 hover:bg-opacity-30 transition-all text-center">
            Programar una demostración
          </a>
        </div>
      </div>
    </section>
  `,
  styles: [`
    @keyframes blob {
      0%, 100% {
        transform: translate(0, 0) scale(1);
      }
      33% {
        transform: translate(30px, -50px) scale(1.1);
      }
      66% {
        transform: translate(-20px, 20px) scale(0.9);
      }
    }

    :host ::ng-deep .animate-blob {
      animation: blob 7s infinite;
    }
  `]
})
export class HomeComponent {
  AppSettings: typeof AppSettings = AppSettings;
}

