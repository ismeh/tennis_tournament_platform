import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-placeholder',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="min-h-[calc(100vh-320px)] flex items-center justify-center bg-gradient-to-br from-neutral-50 to-neutral-100">
      <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-6">
        <div class="w-20 h-20 mx-auto bg-gradient-to-br from-primary-100 to-accent-100 rounded-2xl flex items-center justify-center text-4xl">
          🏗️
        </div>
        <h1 class="text-3xl md:text-4xl font-bold text-neutral-900">
          {{ pageTitle }}
        </h1>
        <p class="text-lg text-neutral-600 leading-relaxed max-w-lg mx-auto">
          ¡Esta página está en construcción! Continúa desarrollando esta función y estaremos encantados de ayudarte a desarrollarla.
        </p>
        <div class="flex flex-col sm:flex-row gap-4 justify-center pt-4">
          <a routerLink="/" class="px-6 py-3 bg-primary-500 text-white font-semibold rounded-lg hover:bg-primary-600 transition-colors">
            Volver a Inicio
          </a>
          <a routerLink="/contact" class="px-6 py-3 bg-white text-primary-600 font-semibold rounded-lg border-2 border-primary-200 hover:border-primary-400 hover:bg-primary-50 transition-colors">
            Enviar Comentarios
          </a>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class PlaceholderComponent implements OnInit {
  pageTitle = 'Próximamente';

  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    this.route.data.subscribe(data => {
      this.pageTitle = data['title'] || 'Próximamente';
    });
  }
}
