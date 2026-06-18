import { Component, inject, OnInit, OnDestroy, PLATFORM_ID, NgZone, ChangeDetectorRef } from '@angular/core';import { CommonModule, isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { AppSettings } from '../shared/constants';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="relative overflow-hidden bg-white">
      <div class="absolute inset-0 bg-gradient-to-br from-primary-50 via-white to-accent-50"></div>
      <div class="absolute top-0 right-0 -z-10 w-96 h-96 bg-primary-200 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob"></div>
      <div class="absolute bottom-0 left-0 -z-10 w-96 h-96 bg-accent-200 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob" style="animation-delay: 2s"></div>

      <div class="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 md:py-20">
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          <div class="space-y-8">
            <div class="space-y-4">
              <p class="text-primary-600 font-semibold text-sm uppercase tracking-widest">Tu próximo partido empieza aquí</p>
              <h1 class="text-4xl md:text-5xl lg:text-6xl font-bold text-neutral-900 leading-tight">
                Encuentra y compite en torneos de <span class="bg-gradient-to-r from-primary-600 to-accent-600 bg-clip-text text-transparent">tenis</span>
              </h1>
              <p class="text-lg text-neutral-600 leading-relaxed max-w-lg">
                {{ settings.PROJECT_NAME }} te permite descubrir los mejores torneos, inscribirte rápidamente y seguir todos los resultados de tus partidos al instante. Tu próxima victoria está a un paso de distancia.
              </p>
            </div>

            <div class="flex flex-col sm:flex-row gap-4">
              @if ((role$ | async) === 'ORGANIZER') {
                <a routerLink="/tournaments/create" class="px-8 py-4 bg-gradient-to-r from-primary-500 to-primary-600 text-white font-semibold rounded-lg hover:shadow-lg hover:from-primary-600 hover:to-primary-700 transition-all transform hover:scale-105 text-center">
                  Crear mi torneo
                </a>
              } @else {
                <a routerLink="/torneos" class="px-8 py-4 bg-gradient-to-r from-primary-500 to-primary-600 text-white font-semibold rounded-lg hover:shadow-lg hover:from-primary-600 hover:to-primary-700 transition-all transform hover:scale-105 text-center">
                  Explorar torneos
                </a>
              }
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
                <span class="text-neutral-700 font-medium">Inscripción digital inmediata</span>
              </div>
              <div class="flex items-center gap-2">
                <div class="flex items-center justify-center w-8 h-8 rounded-full bg-primary-100">
                  <svg class="w-5 h-5 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                  </svg>
                </div>
                <span class="text-neutral-700 font-medium">Resultados en vivo</span>
              </div>
            </div>
          </div>

          <div class="relative hidden lg:block"
               (mouseenter)="onCarouselHover(true)"
               (mouseleave)="onCarouselHover(false)">
            <div class="relative w-full h-96 bg-gradient-to-br from-primary-100 to-accent-100 rounded-2xl overflow-hidden">
              @for (img of carouselImages; track img.id; let i = $index) {
                <div class="absolute inset-0 transition-opacity duration-500"
                     [class.opacity-0]="i !== currentSlide"
                     [class.opacity-100]="i === currentSlide">
                  <img [src]="img.src" [alt]="img.alt" class="w-full h-full object-cover" />
                </div>
              }

              <button (click)="prevSlide()"
                      class="absolute left-3 top-1/2 -translate-y-1/2 w-10 h-10 bg-white/40 hover:bg-white/70 rounded-full flex items-center justify-center shadow-md transition-all z-10">
                <svg class="w-5 h-5 text-neutral-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
                </svg>
              </button>
              <button (click)="nextSlide()"
                      class="absolute right-3 top-1/2 -translate-y-1/2 w-10 h-10 bg-white/40 hover:bg-white/70 rounded-full flex items-center justify-center shadow-md transition-all z-10">
                <svg class="w-5 h-5 text-neutral-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                </svg>
              </button>

              <div class="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-1.5 z-10">
                @for (img of carouselImages; track img.id; let i = $index) {
                  <button (click)="goToSlide(i)"
                          class="h-1.5 rounded-full transition-all duration-300"
                          [class.bg-white]="i === currentSlide"
                          [class.bg-white/40]="i !== currentSlide"
                          [class.w-6]="i === currentSlide"
                          [class.w-1.5]="i !== currentSlide">
                  </button>
                }
              </div>

              <div class="absolute bottom-0 left-0 right-0 h-1.5 bg-black/30 z-10">
                <div class="h-full bg-white/30 transition-none"
                     [style.width.%]="progress">
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="py-8 md:py-20 bg-neutral-50">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-16 space-y-4">
          <p class="text-primary-600 font-semibold text-sm uppercase tracking-widest">Para jugadores</p>
          <h2 class="text-3xl md:text-4xl font-bold text-neutral-900">
            Todo lo que necesitas para saltar a la pista
          </h2>
          <p class="text-neutral-600 text-lg max-w-2xl mx-auto">
            Disfruta de la mejor experiencia competitiva sin preocuparte por el papeleo
          </p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          <div class="bg-white rounded-xl p-8 border border-neutral-200 hover:border-primary-300 hover:shadow-lg transition-all group">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center mb-4 group-hover:bg-primary-200 transition-colors">
              <svg class="w-6 h-6 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd"/>
              </svg>
            </div>
            <h3 class="text-lg font-bold text-neutral-900 mb-2">Buscador inteligente</h3>
            <p class="text-neutral-600">
              Encuentra competiciones cerca de ti filtrando por tus superficies favoritas, fechas y ubicaciones exactas.
            </p>
          </div>

          <div class="bg-white rounded-xl p-8 border border-neutral-200 hover:border-primary-300 hover:shadow-lg transition-all group">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center mb-4 group-hover:bg-primary-200 transition-colors">
              <svg class="w-6 h-6 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-11a1 1 0 10-2 0v2H7a1 1 0 100 2h2v2a1 1 0 102 0v-2h2a1 1 0 100-2h-2V7z" clip-rule="evenodd"/>
              </svg>
            </div>
            <h3 class="text-lg font-bold text-neutral-900 mb-2">Inscripción en segundos</h3>
            <p class="text-neutral-600">
              Crea tu perfil con tus datos de juego una sola vez y apúntate a cualquier cuadro abierto al instante.
            </p>
          </div>

          <div class="bg-white rounded-xl p-8 border border-neutral-200 hover:border-primary-300 hover:shadow-lg transition-all group">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center mb-4 group-hover:bg-primary-200 transition-colors">
              <svg class="w-6 h-6 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                <path d="M2 11a1 1 0 011-1h2a1 1 0 011 1v5a1 1 0 01-1 1H3a1 1 0 01-1-1v-5zM8 7a1 1 0 011-1h2a1 1 0 011 1v9a1 1 0 01-1 1H9a1 1 0 01-1-1V7zM14 4a1 1 0 011-1h2a1 1 0 011 1v12a1 1 0 01-1 1h-2a1 1 0 01-1-1V4z"/>
              </svg>
            </div>
            <h3 class="text-lg font-bold text-neutral-900 mb-2">Resultados en directo</h3>
            <p class="text-neutral-600">
              Sigue la progresión del torneo minuto a minuto. Mira quién avanza y cuándo te toca volver a la pista.
            </p>
          </div>

          <div class="bg-white rounded-xl p-8 border border-neutral-200 hover:border-primary-300 hover:shadow-lg transition-all group">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center mb-4 group-hover:bg-primary-200 transition-colors">
              <svg class="w-6 h-6 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M5 4a2 2 0 012-2h6a2 2 0 012 2v14l-5-2.5L5 18V4z" clip-rule="evenodd"/>
              </svg>
            </div>
            <h3 class="text-lg font-bold text-neutral-900 mb-2">Rankings Actualizados</h3>
            <p class="text-neutral-600">
              Mide tu nivel competitivo, consulta las tablas de clasificación y descubre tu posición frente a otros jugadores.
            </p>
          </div>

          <div class="bg-white rounded-xl p-8 border border-neutral-200 hover:border-primary-300 hover:shadow-lg transition-all group">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center mb-4 group-hover:bg-primary-200 transition-colors">
              <svg class="w-6 h-6 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M4 4a2 2 0 00-2 2v4a1 1 0 001 1h12a1 1 0 001-1V6a2 2 0 00-2-2H4zm12 12H4c-1.1 0-2-.9-2-2v-4a1 1 0 00-1-1H1a1 1 0 001 1v4a4 4 0 004 4h12a1 1 0 001-1v-4a1 1 0 00-1-1h-1a1 1 0 001 1v4z" clip-rule="evenodd"/>
              </svg>
            </div>
            <h3 class="text-lg font-bold text-neutral-900 mb-2">Formatos emocionantes</h3>
            <p class="text-neutral-600">
              Participa tanto en eliminaciones directas tradicionales como en dinámicas fases de grupos.
            </p>
          </div>

          <div class="bg-white rounded-xl p-8 border border-neutral-200 hover:border-primary-300 hover:shadow-lg transition-all group">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center mb-4 group-hover:bg-primary-200 transition-colors">
              <svg class="w-6 h-6 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M12.316 3.051a1 1 0 01.633 1.265l-4 12a1 1 0 11-1.898-.632l4-12a1 1 0 011.265-.633zM5.707 6.293a1 1 0 010 1.414L3.414 10l2.293 2.293a1 1 0 11-1.414 1.414l-3-3a1 1 0 010-1.414l3-3a1 1 0 011.414 0zm8.586 0a1 1 0 011.414 0l3 3a1 1 0 010 1.414l-3 3a1 1 0 11-1.414-1.414L16.586 10l-2.293-2.293a1 1 0 010-1.414z" clip-rule="evenodd"/>
              </svg>
            </div>
            <h3 class="text-lg font-bold text-neutral-900 mb-2">Torneos de consolación</h3>
            <p class="text-neutral-600">
              ¿Un mal día en primera ronda? No pasa nada. El sistema te asigna automáticamente un puesto en el cuadro secundario para seguir jugando.
            </p>
          </div>
        </div>
      </div>
    </section>

    <section class="py-8 md:py-20 bg-white">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-16 space-y-4">
          <p class="text-primary-600 font-semibold text-sm uppercase tracking-widest">Para organizadores</p>
          <h2 class="text-3xl md:text-4xl font-bold text-neutral-900">
            Gestiona tu club y tus torneos sin dolores de cabeza
          </h2>
          <p class="text-neutral-600 text-lg max-w-2xl mx-auto">
            Todas las herramientas necesarias para diseñar, publicar y arbitrar competiciones impecables
          </p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          <div class="bg-white rounded-xl p-8 border border-neutral-200 hover:border-primary-300 hover:shadow-lg transition-all group">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center mb-4 group-hover:bg-primary-200 transition-colors">
              <svg class="w-6 h-6 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd"/>
              </svg>
            </div>
            <h3 class="text-lg font-bold text-neutral-900 mb-2">Creación guiada</h3>
            <p class="text-neutral-600">
              Configura las fechas de inscripción, límites de jugadores y publica la convocatoria del club en pocos pasos.
            </p>
          </div>

          <div class="bg-white rounded-xl p-8 border border-neutral-200 hover:border-primary-300 hover:shadow-lg transition-all group">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center mb-4 group-hover:bg-primary-200 transition-colors">
              <svg class="w-6 h-6 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M4 4a2 2 0 00-2 2v4a1 1 0 001 1h12a1 1 0 001-1V6a2 2 0 00-2-2H4zm12 12H4c-1.1 0-2-.9-2-2v-4a1 1 0 00-1-1H1a1 1 0 001 1v4a4 4 0 004 4h12a1 1 0 001-1v-4a1 1 0 00-1-1h-1a1 1 0 001 1v4z" clip-rule="evenodd"/>
              </svg>
            </div>
            <h3 class="text-lg font-bold text-neutral-900 mb-2">Cuadros automáticos</h3>
            <p class="text-neutral-600">
              Olvídate del papel y los sorteos complejos. El sistema genera los emparejamientos y las fases de juego al instante.
            </p>
          </div>

          <div class="bg-white rounded-xl p-8 border border-neutral-200 hover:border-primary-300 hover:shadow-lg transition-all group">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center mb-4 group-hover:bg-primary-200 transition-colors">
              <svg class="w-6 h-6 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clip-rule="evenodd"/>
              </svg>
            </div>
            <h3 class="text-lg font-bold text-neutral-900 mb-2">Control de pistas y horarios</h3>
            <p class="text-neutral-600">
              Asigna partidos a tus canchas libres y define los horarios del torneo sin solapamientos ni confusiones.
            </p>
          </div>

          <div class="bg-white rounded-xl p-8 border border-neutral-200 hover:border-primary-300 hover:shadow-lg transition-all group">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center mb-4 group-hover:bg-primary-200 transition-colors">
              <svg class="w-6 h-6 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M5 4a2 2 0 012-2h6a2 2 0 012 2v14l-5-2.5L5 18V4z" clip-rule="evenodd"/>
              </svg>
            </div>
            <h3 class="text-lg font-bold text-neutral-900 mb-2">Listas oficiales y RFET</h3>
            <p class="text-neutral-600">
              Sincroniza y equilibra la competición usando las clasificaciones oficiales de la federación.
            </p>
          </div>

          <div class="bg-white rounded-xl p-8 border border-neutral-200 hover:border-primary-300 hover:shadow-lg transition-all group">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center mb-4 group-hover:bg-primary-200 transition-colors">
              <svg class="w-6 h-6 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                <path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v1h8v-1zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-1a4 4 0 00-4-4h-2v1h2a3 3 0 013 3v1h3z"/>
              </svg>
            </div>
            <h3 class="text-lg font-bold text-neutral-900 mb-2">Inscripciones manuales</h3>
            <p class="text-neutral-600">
              Suma jugadores cómodamente desde tu panel de administrador para añadir participantes invitados de última hora.
            </p>
          </div>

          <div class="bg-white rounded-xl p-8 border border-neutral-200 hover:border-primary-300 hover:shadow-lg transition-all group">
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center mb-4 group-hover:bg-primary-200 transition-colors">
              <svg class="w-6 h-6 text-primary-600" fill="currentColor" viewBox="0 0 20 20">
                <path d="M2 11a1 1 0 011-1h2a1 1 0 011 1v5a1 1 0 01-1 1H3a1 1 0 01-1-1v-5zM8 7a1 1 0 011-1h2a1 1 0 011 1v9a1 1 0 01-1 1H9a1 1 0 01-1-1V7zM14 4a1 1 0 011-1h2a1 1 0 011 1v12a1 1 0 01-1 1h-2a1 1 0 01-1-1V4z"/>
              </svg>
            </div>
            <h3 class="text-lg font-bold text-neutral-900 mb-2">Informes listos para imprimir</h3>
            <p class="text-neutral-600">
              Genera resúmenes claros en PDF del estado y desarrollo de tus competiciones con un solo botón.
            </p>
          </div>
        </div>
      </div>
    </section>

    <section class="py-8 md:py-20 bg-neutral-50">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-16 space-y-4">
          <p class="text-primary-600 font-semibold text-sm uppercase tracking-widest">Tipos de torneos</p>
          <h2 class="text-3xl md:text-4xl font-bold text-neutral-900">
            Encuentra tu estilo de juego
          </h2>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div class="relative overflow-hidden rounded-2xl bg-gradient-to-br from-primary-50 to-primary-100 p-8 border-2 border-primary-200 hover:shadow-lg transition-shadow">
            <div class="absolute top-0 right-0 w-32 h-32 bg-primary-200 rounded-full -mr-16 -mt-16 opacity-30"></div>
            <div class="relative z-10">
              <div class="w-12 h-12 bg-primary-500 rounded-lg flex items-center justify-center text-white font-bold text-xl mb-4">
                🏆
              </div>
              <h3 class="text-2xl font-bold text-neutral-900 mb-3">Torneos Competitivos</h3>
              <p class="text-neutral-700 mb-6 leading-relaxed">
                Competiciones oficiales alineadas con rankings profesionales. Organización estricta de cabezas de serie según méritos y licencias federativas.
              </p>
              <ul class="space-y-2 text-neutral-700">
                <li class="flex items-center gap-2">
                  <span class="w-2 h-2 bg-primary-500 rounded-full"></span>
                  Integración de ránkings oficiales
                </li>
                <li class="flex items-center gap-2">
                  <span class="w-2 h-2 bg-primary-500 rounded-full"></span>
                  Sorteo automático de cabezas de serie
                </li>
                <li class="flex items-center gap-2">
                  <span class="w-2 h-2 bg-primary-500 rounded-full"></span>
                  Jugadores con licencia verificada
                </li>
              </ul>
            </div>
          </div>

          <div class="relative overflow-hidden rounded-2xl bg-gradient-to-br from-accent-50 to-accent-100 p-8 border-2 border-accent-200 hover:shadow-lg transition-shadow">
            <div class="absolute top-0 right-0 w-32 h-32 bg-accent-200 rounded-full -mr-16 -mt-16 opacity-30"></div>
            <div class="relative z-10">
              <div class="w-12 h-12 bg-accent-500 rounded-lg flex items-center justify-center text-white font-bold text-xl mb-4">
                🎯
              </div>
              <h3 class="text-2xl font-bold text-neutral-900 mb-3">Torneos Aficionados</h3>
              <p class="text-neutral-700 mb-6 leading-relaxed">
                Perfectos para ligas locales, eventos sociales y clubes comunitarios con divisiones de edad flexibles y personalizadas.
              </p>
              <ul class="space-y-2 text-neutral-700">
                <li class="flex items-center gap-2">
                  <span class="w-2 h-2 bg-accent-500 rounded-full"></span>
                  Clasificaciones internas del club
                </li>
                <li class="flex items-center gap-2">
                  <span class="w-2 h-2 bg-accent-500 rounded-full"></span>
                  Categorías adaptadas a cada nivel
                </li>
                <li class="flex items-center gap-2">
                  <span class="w-2 h-2 bg-accent-500 rounded-full"></span>
                  Ambiente social e impulsado por la comunidad
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="py-8 md:py-20 bg-gradient-to-r from-primary-600 to-accent-600 relative overflow-hidden">
      <div class="absolute inset-0 opacity-10">
        <div class="absolute inset-0 bg-pattern"></div>
      </div>
      <div class="relative z-10 max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-8">
        <h2 class="text-3xl md:text-4xl font-bold text-white">
          ¿Listo para competir?
        </h2>
        <p class="text-xl text-white text-opacity-90 max-w-2xl mx-auto">
          Únete a miles de tenistas y clubes que ya organizan y juegan sus partidos en {{ settings.PROJECT_NAME }}.
        </p>
        <div class="flex flex-col sm:flex-row gap-4 justify-center">
          <a routerLink="/register" class="px-8 py-4 bg-white text-primary-600 font-semibold rounded-lg hover:bg-neutral-50 transition-all transform hover:scale-105 text-center">
            Comienza gratis
          </a>
          <a routerLink="/contact" class="px-8 py-4 bg-white bg-opacity-20 text-white font-semibold rounded-lg border-2 border-white border-opacity-30 hover:bg-opacity-30 transition-all text-center">
            Solicitar una demostración
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
export class HomeComponent implements OnInit, OnDestroy {
  readonly settings = AppSettings;
  private readonly authService = inject(AuthService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly zone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);
  readonly role$ = this.authService.role$;

  carouselImages = [
    { id: 1, src: 'https://images.unsplash.com/photo-1554068865-24cecd4e34b8?w=800&q=80', alt: 'Pista de tenis' },
    { id: 2, src: 'https://images.unsplash.com/photo-1622279457486-62dcc4a431d6?w=800&q=80', alt: 'Jugador de tenis' },
    { id: 3, src: 'https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=800&q=80', alt: 'Torneo de tenis' },
    { id: 4, src: 'https://images.unsplash.com/photo-1587280501635-68a0e82cd5ff?w=800&q=80', alt: 'Raqueta y pelota' },
  ];

  currentSlide = 0;
  progress = 0;
  isPaused = false;
  private tickId: ReturnType<typeof setInterval> | null = null;
  private readonly DURATION = 3500;
  private readonly TICK = 30;

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.startAutoplay();
    }
  }

  ngOnDestroy(): void {
    this.stopAutoplay();
  }

  onCarouselHover(hovering: boolean): void {
    this.isPaused = hovering;
    if (hovering) {
      this.stopAutoplay();
    } else {
      this.startAutoplay();
    }
  }

  nextSlide(): void {
    this.currentSlide = (this.currentSlide + 1) % this.carouselImages.length;
    this.progress = 0;
  }

  prevSlide(): void {
    this.currentSlide = (this.currentSlide - 1 + this.carouselImages.length) % this.carouselImages.length;
    this.progress = 0;
  }

  goToSlide(index: number): void {
    this.currentSlide = index;
    this.progress = 0;
  }

  private startAutoplay(): void {
    this.stopAutoplay();
    this.zone.runOutsideAngular(() => {
      this.tickId = setInterval(() => {
        if (this.isPaused) return;
        this.zone.run(() => {
          this.progress += (this.TICK / this.DURATION) * 100;
          if (this.progress >= 100) {
            this.nextSlide();
          }
          this.cdr.markForCheck(); // ← fuerza re-render
        });
      }, this.TICK);
    });
  }

  private stopAutoplay(): void {
    if (this.tickId) {
      clearInterval(this.tickId);
      this.tickId = null;
    }
  }
}