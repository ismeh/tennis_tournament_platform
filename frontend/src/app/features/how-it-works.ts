import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppSettings } from '../shared/constants';

@Component({
  selector: 'app-how-it-works',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="bg-white">
      <div class="mx-auto max-w-7xl px-4 py-10 sm:px-6 md:py-16 lg:px-8">
        <div class="grid gap-10 lg:grid-cols-[1.05fr_0.95fr] lg:items-center">
          <div class="space-y-6">
            <p class="text-sm font-semibold uppercase tracking-widest text-primary-600">
              Cómo funciona
            </p>
            <div class="space-y-4">
              <h1 class="text-4xl font-bold leading-tight text-neutral-900 md:text-5xl">
                Una forma clara de organizar, jugar y seguir torneos de tenis
              </h1>
              <p class="max-w-2xl text-lg leading-relaxed text-neutral-600">
                {{ AppSettings.PROJECT_NAME }} conecta a clubes, organizadores y jugadores en
                un mismo flujo: publicación del torneo, inscripciones, cuadros, horarios,
                resultados y seguimiento de la competición.
              </p>
            </div>
            <div class="flex flex-col gap-3 sm:flex-row">
              <a
                routerLink="/torneos"
                class="rounded-lg bg-primary-500 px-6 py-3 text-center font-semibold text-white transition-colors hover:bg-primary-600"
              >
                Ver torneos
              </a>
              <a
                routerLink="/torneos/crear"
                class="rounded-lg border-2 border-primary-200 bg-white px-6 py-3 text-center font-semibold text-primary-600 transition-colors hover:border-primary-400 hover:bg-primary-50"
              >
                Crear torneo
              </a>
            </div>
          </div>

          <div class="rounded-2xl border border-neutral-200 bg-neutral-50 p-6 shadow-sm">
            <div class="grid gap-4 sm:grid-cols-2">
              <div class="rounded-xl bg-white p-5">
                <p class="mb-2 text-3xl">1</p>
                <h2 class="font-bold text-neutral-900">Publica</h2>
                <p class="mt-2 text-sm leading-relaxed text-neutral-600">
                  Define nombre, sede, fechas, pistas, plazas y tipo de competición.
                </p>
              </div>
              <div class="rounded-xl bg-white p-5">
                <p class="mb-2 text-3xl">2</p>
                <h2 class="font-bold text-neutral-900">Inscríbete</h2>
                <p class="mt-2 text-sm leading-relaxed text-neutral-600">
                  Los jugadores consultan el calendario y entran en los torneos disponibles.
                </p>
              </div>
              <div class="rounded-xl bg-white p-5">
                <p class="mb-2 text-3xl">3</p>
                <h2 class="font-bold text-neutral-900">Compite</h2>
                <p class="mt-2 text-sm leading-relaxed text-neutral-600">
                  El cuadro, los partidos y los horarios mantienen a todos coordinados.
                </p>
              </div>
              <div class="rounded-xl bg-white p-5">
                <p class="mb-2 text-3xl">4</p>
                <h2 class="font-bold text-neutral-900">Actualiza</h2>
                <p class="mt-2 text-sm leading-relaxed text-neutral-600">
                  Los resultados alimentan la progresión del torneo y el seguimiento en vivo.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="bg-neutral-50 py-10 md:py-16">
      <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div class="mb-10 max-w-3xl space-y-3">
          <p class="text-sm font-semibold uppercase tracking-widest text-primary-600">
            Para creadores de torneos
          </p>
          <h2 class="text-3xl font-bold text-neutral-900">
            Control operativo desde la planificación hasta la final
          </h2>
          <p class="text-neutral-600">
            La plataforma está pensada para reducir trabajo manual y evitar información
            dispersa entre hojas de cálculo, mensajes y llamadas.
          </p>
        </div>

        <div class="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          <article class="rounded-xl border border-neutral-200 bg-white p-6">
            <h3 class="text-lg font-bold text-neutral-900">Configuración del torneo</h3>
            <p class="mt-3 leading-relaxed text-neutral-600">
              Crea torneos con sede, fechas, número de pistas, límite de jugadores y datos
              básicos visibles para los participantes.
            </p>
          </article>
          <article class="rounded-xl border border-neutral-200 bg-white p-6">
            <h3 class="text-lg font-bold text-neutral-900">Gestión de inscritos</h3>
            <p class="mt-3 leading-relaxed text-neutral-600">
              Centraliza participantes, revisa plazas disponibles y prepara la competición con
              una lista clara de jugadores.
            </p>
          </article>
          <article class="rounded-xl border border-neutral-200 bg-white p-6">
            <h3 class="text-lg font-bold text-neutral-900">Cuadros y partidos</h3>
            <p class="mt-3 leading-relaxed text-neutral-600">
              Genera la estructura del torneo, consulta rondas y mantén actualizados los
              enfrentamientos a medida que avanza la competición.
            </p>
          </article>
          <article class="rounded-xl border border-neutral-200 bg-white p-6">
            <h3 class="text-lg font-bold text-neutral-900">Horarios y pistas</h3>
            <p class="mt-3 leading-relaxed text-neutral-600">
              Asigna partidos a franjas horarias y pistas para que jugadores y organización
              trabajen con una referencia común.
            </p>
          </article>
          <article class="rounded-xl border border-neutral-200 bg-white p-6">
            <h3 class="text-lg font-bold text-neutral-900">Resultados centralizados</h3>
            <p class="mt-3 leading-relaxed text-neutral-600">
              Registra marcadores y facilita que el cuadro y la información pública reflejen el
              estado real del torneo.
            </p>
          </article>
          <article class="rounded-xl border border-neutral-200 bg-white p-6">
            <h3 class="text-lg font-bold text-neutral-900">Comunicación más simple</h3>
            <p class="mt-3 leading-relaxed text-neutral-600">
              Reduce dudas repetidas al ofrecer una página de torneo con datos, calendario,
              participantes, partidos y evolución.
            </p>
          </article>
        </div>
      </div>
    </section>

    <section class="bg-white py-10 md:py-16">
      <div class="mx-auto grid max-w-7xl gap-10 px-4 sm:px-6 lg:grid-cols-[0.9fr_1.1fr] lg:px-8">
        <div class="space-y-4">
          <p class="text-sm font-semibold uppercase tracking-widest text-primary-600">
            Para jugadores y usuarios
          </p>
          <h2 class="text-3xl font-bold text-neutral-900">
            Encuentra torneos y sigue tu competición sin perder detalle
          </h2>
          <p class="leading-relaxed text-neutral-600">
            Tanto si compites habitualmente como si buscas torneos locales, puedes consultar
            información clave antes de inscribirte y seguir el progreso una vez empiece el
            torneo.
          </p>
          <a
            routerLink="/register"
            class="inline-flex rounded-lg bg-accent-500 px-6 py-3 font-semibold text-white transition-colors hover:bg-accent-600"
          >
            Crear cuenta
          </a>
        </div>

        <div class="grid gap-5 sm:grid-cols-2">
          <div class="rounded-xl bg-neutral-50 p-6">
            <h3 class="font-bold text-neutral-900">Calendario de torneos</h3>
            <p class="mt-3 text-sm leading-relaxed text-neutral-600">
              Consulta competiciones disponibles, fechas, sedes y plazas para decidir dónde
              participar.
            </p>
          </div>
          <div class="rounded-xl bg-neutral-50 p-6">
            <h3 class="font-bold text-neutral-900">Detalle de cada torneo</h3>
            <p class="mt-3 text-sm leading-relaxed text-neutral-600">
              Revisa participantes, rondas, partidos, pistas y estado general desde una misma
              pantalla.
            </p>
          </div>
          <div class="rounded-xl bg-neutral-50 p-6">
            <h3 class="font-bold text-neutral-900">Seguimiento de partidos</h3>
            <p class="mt-3 text-sm leading-relaxed text-neutral-600">
              Comprueba horarios, rivales y resultados publicados para estar preparado antes de
              cada ronda.
            </p>
          </div>
          <div class="rounded-xl bg-neutral-50 p-6">
            <h3 class="font-bold text-neutral-900">Perfil de jugador</h3>
            <p class="mt-3 text-sm leading-relaxed text-neutral-600">
              Mantén tus datos actualizados para que la organización pueda identificarte y
              gestionar tu participación.
            </p>
          </div>
        </div>
      </div>
    </section>

    <section class="bg-neutral-900 py-10 text-white md:py-14">
      <div class="mx-auto flex max-w-7xl flex-col gap-6 px-4 sm:px-6 md:flex-row md:items-center md:justify-between lg:px-8">
        <div class="max-w-2xl space-y-3">
          <h2 class="text-2xl font-bold md:text-3xl">Empieza por el calendario</h2>
          <p class="text-neutral-300">
            Explora los torneos publicados o crea uno nuevo si eres parte de la organización.
          </p>
        </div>
        <div class="flex flex-col gap-3 sm:flex-row">
          <a
            routerLink="/torneos"
            class="rounded-lg bg-white px-6 py-3 text-center font-semibold text-neutral-900 transition-colors hover:bg-neutral-100"
          >
            Explorar torneos
          </a>
          <a
            routerLink="/torneos/crear"
            class="rounded-lg border border-white/30 px-6 py-3 text-center font-semibold text-white transition-colors hover:bg-white/10"
          >
            Organizar torneo
          </a>
        </div>
      </div>
    </section>
  `,
  styles: []
})
export class HowItWorksComponent {
  AppSettings: typeof AppSettings = AppSettings;
}
