import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="bg-white">
      <div class="mx-auto max-w-4xl px-4 py-10 sm:px-6 md:py-16 lg:px-8">
        <div class="mb-8">
          <a routerLink="/" class="text-sm font-medium text-primary-600 hover:text-primary-700">&larr; Volver al inicio</a>
        </div>

        <h1 class="text-3xl font-bold text-neutral-900 md:text-4xl">Contacto</h1>
        <p class="mt-2 text-sm text-neutral-500">Estamos encantados de escucharte</p>

        <div class="mt-8 space-y-8 leading-relaxed text-neutral-700">
          <div>
            <h2 class="mb-3 text-xl font-bold text-neutral-900">¿Tienes alguna pregunta?</h2>
            <p>
              Si tienes alguna duda, sugerencia o simplemente quieres ponerte en contacto con nosotros,
              no dudes en escribirnos. Haremos lo posible por responderte lo antes posible.
            </p>
          </div>

          <div class="rounded-xl border border-neutral-200 bg-neutral-50 p-6">
            <h2 class="mb-4 text-xl font-bold text-neutral-900">Datos de contacto</h2>
            <ul class="space-y-3 text-neutral-700">
              <li class="flex items-center gap-3">
                <span class="text-lg">👤</span>
                <span><strong>Nombre:</strong> Ismael</span>
              </li>
              <li class="flex items-center gap-3">
                <span class="text-lg">✉️</span>
                <span>
                  <strong>Email:</strong>
                  <a href="mailto:gadeismael+puntomatch@hotmail.com" class="font-medium text-primary-600 hover:text-primary-700">
                    gadeismael+puntomatch&#64;hotmail.com
                  </a>
                </span>
              </li>
              <li class="flex items-center gap-3">
                <span class="text-lg">🐙</span>
                <span>
                  <strong>GitHub:</strong>
                  <a href="https://github.com/ismeh" target="_blank" rel="noopener noreferrer" class="font-medium text-primary-600 hover:text-primary-700">
                    github.com/ismeh
                  </a>
                </span>
              </li>
            </ul>
          </div>

          <div>
            <h2 class="mb-3 text-xl font-bold text-neutral-900">¿Cómo contactarnos?</h2>
            <p>
              La forma más rápida de contactarnos es a través de nuestro correo electrónico.
              También puedes abrir un issue en nuestro repositorio de GitHub si encuentras un
              bug o tienes una propuesta de mejora.
            </p>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: []
})
export class ContactComponent {}
