import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppSettings } from '../shared/constants';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink],
  template: `
    <footer class="bg-neutral-900 text-neutral-50 border-t border-neutral-800">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div class="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
          <!-- Brand -->
          <div>
            <div class="flex items-center gap-2 mb-4">
              <div class="w-10 h-10 bg-gradient-to-br from-primary-500 to-accent-500 rounded-lg flex items-center justify-center text-white font-bold text-lg">
                🎾
              </div>
              <span class="text-lg font-bold">{{ AppSettings.projectName }}</span>
            </div>
            <p class="text-neutral-400 text-sm leading-relaxed">
              La plataforma definitiva para organizar y gestionar torneos de tenis fácilmente.
            </p>
          </div>

          <!-- Producto -->
          <div>
            <h3 class="font-semibold text-white mb-4">Producto</h3>
            <ul class="space-y-2 text-sm">
              <li><a routerLink="/tournaments" class="text-neutral-400 hover:text-primary-400 transition-colors">Explorar Torneos</a></li>
              <li><a routerLink="/how-it-works" class="text-neutral-400 hover:text-primary-400 transition-colors">Cómo Funciona</a></li>
              <li><a href="#" class="text-neutral-400 hover:text-primary-400 transition-colors">Precios</a></li>
              <li><a href="#" class="text-neutral-400 hover:text-primary-400 transition-colors">Características</a></li>
            </ul>
          </div>

          <!-- Empresa -->
          <div>
            <h3 class="font-semibold text-white mb-4">Empresa</h3>
            <ul class="space-y-2 text-sm">
              <li><a href="#" class="text-neutral-400 hover:text-primary-400 transition-colors">Sobre Nosotros</a></li>
              <li><a routerLink="/contact" class="text-neutral-400 hover:text-primary-400 transition-colors">Contacto</a></li>
              <li><a href="#" class="text-neutral-400 hover:text-primary-400 transition-colors">Blog</a></li>
              <li><a href="#" class="text-neutral-400 hover:text-primary-400 transition-colors">Empleos</a></li>
            </ul>
          </div>

          <!-- Legal -->
          <div>
            <h3 class="font-semibold text-white mb-4">Legal</h3>
            <ul class="space-y-2 text-sm">
              <li><a href="#" class="text-neutral-400 hover:text-primary-400 transition-colors">Política de Privacidad</a></li>
              <li><a href="#" class="text-neutral-400 hover:text-primary-400 transition-colors">Términos de Servicio</a></li>
              <li><a href="#" class="text-neutral-400 hover:text-primary-400 transition-colors">Política de Cookies</a></li>
              <li><a href="#" class="text-neutral-400 hover:text-primary-400 transition-colors">RGPD</a></li>
            </ul>
          </div>
        </div>

        <!-- Divisor -->
        <div class="border-t border-neutral-800"></div>

        <!-- Sección Inferior -->
        <div class="flex flex-col md:flex-row justify-between items-center pt-8 text-sm text-neutral-400">
          <p>&copy; 2025 {{ AppSettings.projectName }}. Todos los derechos reservados.</p>
          <div class="flex gap-4 mt-4 md:mt-0">
            <a href="#" class="hover:text-primary-400 transition-colors">Twitter</a>
            <a href="#" class="hover:text-primary-400 transition-colors">LinkedIn</a>
            <a href="#" class="hover:text-primary-400 transition-colors">Instagram</a>
          </div>
        </div>
      </div>
    </footer>
  `,
  styles: []
})
export class FooterComponent {
  AppSettings: any = AppSettings;
}
