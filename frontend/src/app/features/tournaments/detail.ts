import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';
import { PersonService } from '../../data/services/person.service';
import {
  TournamentInscriptionCategoryCount,
  TournamentInscriptionEvent,
  TournamentInscriptionsResponse,
  TournamentEventCategoryGender,
  TournamentEventCatalogItem,
  TournamentEventGender,
  TournamentEventSelection,
  TournamentEventsConfigRequest,
  ManualParticipantSource,
  ManualEventInscriptionRequest,
  TournamentProviderSummary,
  TournamentStatus,
  TournamentResponse,
  getTournamentEventGenderLabel,
  getTournamentSurfaceCategoryLabel
} from '../../data/interfaces/tournament.model';
import { PersonSearchResponse } from '../../data/interfaces/person.model';
import { MemberService } from '../../data/services/member.service';
import { TournamentService } from '../../data/services/tournament.service';

type TournamentDetailSection = 'overview' | 'setup' | 'inscriptions' | 'registeredPlayers';

@Component({
  selector: 'app-tournament-detail-page',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe, FormsModule],
  template: `
    <section class="relative overflow-hidden bg-gradient-to-b from-neutral-50 via-white to-white py-10 sm:py-14">
      <div class="mx-auto max-w-6xl px-4 sm:px-6 lg:px-8">
        <a routerLink="/torneos" class="inline-flex items-center text-sm font-semibold text-primary-700 hover:text-primary-800">
          <- Volver al listado
        </a>

        @if (isLoading()) {
          <div class="mt-5 rounded-2xl border border-neutral-200 bg-white p-6 text-neutral-600">Cargando torneo...</div>
        } @else if (errorMessage()) {
          <div class="mt-5 rounded-2xl border border-red-200 bg-red-50 p-6 text-red-700">{{ errorMessage() }}</div>
        } @else if (tournament()) {
          <header class="mt-5 rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
            <p class="text-xs font-semibold uppercase tracking-[0.22em] text-primary-600">Detalle de torneo</p>
            <div class="mt-3 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <h1 class="text-3xl font-black text-neutral-900 sm:text-4xl">{{ tournament()!.formalName }}</h1>
                <p class="mt-2 text-neutral-600">{{ tournament()!.location }}</p>
              </div>
              <span class="inline-flex w-fit rounded-full border border-primary-200 bg-primary-50 px-4 py-2 text-sm font-semibold text-primary-700">
                Estado: {{ tournament()!.status }}
              </span>
            </div>

            <div class="mt-6 rounded-2xl border border-neutral-200 bg-neutral-50 p-2">
              <div class="flex flex-wrap gap-2">
                <button
                  type="button"
                  (click)="setActiveSection('overview')"
                  [class]="activeSection() === 'overview' ? 'rounded-xl bg-white px-4 py-2 text-sm font-semibold text-primary-700 shadow-sm' : 'rounded-xl px-4 py-2 text-sm font-semibold text-neutral-600 hover:bg-white/70'"
                >
                  Informacion general
                </button>

                @if (isCreator()) {
                  <button
                    type="button"
                    (click)="setActiveSection('setup')"
                    [class]="activeSection() === 'setup' ? 'rounded-xl bg-white px-4 py-2 text-sm font-semibold text-primary-700 shadow-sm' : 'rounded-xl px-4 py-2 text-sm font-semibold text-neutral-600 hover:bg-white/70'"
                  >
                    Finalizar configuracion
                  </button>
                }

                <button
                  type="button"
                  (click)="setActiveSection('inscriptions')"
                  [class]="activeSection() === 'inscriptions' ? 'rounded-xl bg-white px-4 py-2 text-sm font-semibold text-primary-700 shadow-sm' : 'rounded-xl px-4 py-2 text-sm font-semibold text-neutral-600 hover:bg-white/70'"
                >
                  Inscripciones
                </button>

                <button
                  type="button"
                  (click)="setActiveSection('registeredPlayers')"
                  [class]="activeSection() === 'registeredPlayers' ? 'rounded-xl bg-white px-4 py-2 text-sm font-semibold text-primary-700 shadow-sm' : 'rounded-xl px-4 py-2 text-sm font-semibold text-neutral-600 hover:bg-white/70'"
                >
                  Jugadores inscritos
                </button>
              </div>
            </div>
          </header>

          @if (activeSection() === 'overview') {
            <section class="mt-6 rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
              <h2 class="text-xl font-bold text-neutral-900">Informacion del torneo</h2>
              <div class="mt-5 grid gap-4 sm:grid-cols-2">
                <div class="rounded-2xl border border-neutral-200 p-4">
                  <p class="text-xs uppercase tracking-widest text-neutral-500">Superficie</p>
                  <p class="mt-1 font-semibold text-neutral-900">{{ getSurfaceLabel(tournament()!.surfaceCategory) }}</p>
                </div>
                <div class="rounded-2xl border border-neutral-200 p-4">
                  <p class="text-xs uppercase tracking-widest text-neutral-500">Capacidad</p>
                  <p class="mt-1 font-semibold text-neutral-900">{{ tournament()!.maxPlayers }} jugadores</p>
                </div>
                <div class="rounded-2xl border border-neutral-200 p-4">
                  <p class="text-xs uppercase tracking-widest text-neutral-500">Periodo de juego</p>
                  <p class="mt-1 font-semibold text-neutral-900">
                    {{ tournament()!.playStartDate | date: 'dd/MM/yyyy' }} - {{ tournament()!.playEndDate | date: 'dd/MM/yyyy' }}
                  </p>
                </div>
                <div class="rounded-2xl border border-neutral-200 p-4">
                  <p class="text-xs uppercase tracking-widest text-neutral-500">Periodo de inscripcion</p>
                  <p class="mt-1 font-semibold text-neutral-900">
                    {{ tournament()!.inscriptionStartDate | date: 'dd/MM/yyyy' }} - {{ tournament()!.inscriptionEndDate | date: 'dd/MM/yyyy' }}
                  </p>
                </div>
              </div>
            </section>
          }

          @if (activeSection() === 'setup') {
            <section class="mt-6 rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
              @if (isCreator()) {
                <h2 class="text-xl font-bold text-neutral-900">Panel de configuracion del creador</h2>
                <p class="mt-2 text-neutral-600">Gestiona el torneo antes de abrir o durante el proceso de inscripcion.</p>

                <div class="mt-6 rounded-3xl border border-neutral-200 bg-neutral-50 p-5 sm:p-6">
                  <div class="rounded-2xl border border-neutral-200 bg-white p-4">
                    <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-600">Estado del torneo</p>
                    <div class="mt-3 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                      <label class="block min-w-56">
                        <span class="mb-1 block text-sm font-medium text-neutral-700">Nuevo estado</span>
                        <select
                          class="w-full rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                          [disabled]="isUpdatingStatus() || allowedStatusTransitions().length === 0"
                          [value]="selectedStatus() ?? ''"
                          (change)="onSelectedStatusChange($any($event.target).value)"
                        >
                          @for (status of allowedStatusTransitions(); track status) {
                            <option [value]="status">{{ status }}</option>
                          }
                        </select>
                      </label>

                      <button
                        type="button"
                        (click)="updateTournamentStatus()"
                        class="rounded-2xl bg-white px-5 py-3 font-semibold text-neutral-800 ring-1 ring-neutral-300 transition-colors hover:bg-neutral-50 disabled:cursor-not-allowed disabled:opacity-60"
                        [disabled]="!canUpdateStatus()"
                      >
                        {{ isUpdatingStatus() ? 'Actualizando estado...' : 'Actualizar estado' }}
                      </button>
                    </div>
                    @if (allowedStatusTransitions().length === 0) {
                      <p class="mt-2 text-xs text-neutral-500">No hay transiciones de estado disponibles para el estado actual.</p>
                    }
                  </div>

                  <div class="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
                    <div>
                      <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-600">Eventos del torneo</p>
                      <h3 class="mt-2 text-xl font-bold text-neutral-900">Configura la lista de eventos aquí mismo</h3>
                      <p class="mt-2 text-sm text-neutral-600">Selecciona varios eventos del catálogo, marca uno o varios géneros y guarda la configuración sin salir del detalle.</p>
                    </div>
                    <span class="rounded-full bg-white px-3 py-1 text-xs font-semibold uppercase tracking-widest text-neutral-600">
                      {{ selectedEvents().length }} seleccionados
                    </span>
                  </div>

                  @if (isLoadingEvents()) {
                    <div class="mt-4 rounded-2xl border border-dashed border-neutral-300 bg-white px-4 py-3 text-sm text-neutral-600">
                      Cargando catálogo de eventos...
                    </div>
                  }

                  @if (eventCatalogError()) {
                    <div class="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                      {{ eventCatalogError() }}
                    </div>
                  }

                  <div class="mt-4 grid gap-4 lg:grid-cols-[1fr_1.15fr]">
                    <label class="block">
                      <span class="mb-1 block text-sm font-medium text-neutral-700">Catálogo disponible</span>
                      <div class="max-h-64 space-y-2 overflow-y-auto rounded-2xl border border-neutral-300 bg-white p-3">
                        @for (event of eventCatalog(); track event.id) {
                          <label class="flex cursor-pointer items-center gap-3 rounded-xl bg-neutral-50 px-3 py-2 hover:bg-primary-50">
                            <input
                              type="checkbox"
                              class="h-4 w-4 rounded border-neutral-300 text-primary-600 focus:ring-primary-500"
                              [checked]="isEventSelected(event.id)"
                              (change)="toggleCatalogEvent(event, $any($event.target).checked)"
                            />
                            <span class="text-sm text-neutral-800">{{ event.category }}</span>
                          </label>
                        }
                      </div>
                    </label>

                    <div class="rounded-2xl border border-dashed border-neutral-300 bg-white p-4">
                      @if (selectedEvents().length === 0) {
                        <p class="text-sm text-neutral-500">No hay eventos seleccionados todavía.</p>
                      } @else {
                        <div class="space-y-3">
                          @for (event of selectedEvents(); track event.categoryId) {
                            <div class="rounded-2xl border border-neutral-200 bg-neutral-50 p-4">
                              <div class="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
                                <div>
                                  <p class="text-sm font-semibold text-neutral-900">{{ getEventLabelById(event.categoryId) }}</p>
                                </div>
                                <div class="min-w-52">
                                  <div class="mb-2 flex items-center gap-2">
                                    <span class="text-xs font-semibold uppercase tracking-widest text-neutral-500">Géneros</span>
                                    <span
                                      class="inline-flex h-4 w-4 items-center justify-center rounded-full border border-neutral-300 text-[10px] font-bold text-neutral-500"
                                      title="Por cada género seleccionado se creará un evento extra."
                                      aria-label="Información sobre géneros"
                                    >
                                      i
                                    </span>
                                  </div>
                                  <div class="flex flex-wrap gap-2">
                                    @for (gender of eventGenderOptions; track gender) {
                                      <label class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-neutral-300 bg-white px-3 py-1.5 text-xs font-medium text-neutral-700 hover:border-primary-400">
                                        <input
                                          type="checkbox"
                                          class="h-3.5 w-3.5 rounded border-neutral-300 text-primary-600 focus:ring-primary-500"
                                          [checked]="hasEventGender(event.categoryId, gender)"
                                          (change)="toggleEventGender(event.categoryId, gender, $any($event.target).checked)"
                                        />
                                        <span>{{ getGenderLabel(gender) }}</span>
                                      </label>
                                    }
                                  </div>
                                </div>
                              </div>
                            </div>
                          }
                        </div>
                      }
                    </div>
                  </div>

                  @if (eventsSuccessMessage()) {
                    <div class="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                      {{ eventsSuccessMessage() }}
                    </div>
                  }

                  @if (eventsErrorMessage()) {
                    <div class="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                      {{ eventsErrorMessage() }}
                    </div>
                  }

                  <div class="mt-4 flex flex-col gap-3 sm:flex-row sm:justify-end">
                    <button
                      type="button"
                      (click)="clearSelectedEvents()"
                      class="rounded-2xl border border-neutral-300 px-5 py-3 font-semibold text-neutral-700 transition-colors hover:border-neutral-400 hover:bg-white"
                    >
                      Limpiar eventos
                    </button>
                    <button
                      type="button"
                      (click)="saveTournamentEvents()"
                      class="rounded-2xl bg-gradient-to-r from-primary-600 to-accent-600 px-5 py-3 font-semibold text-white shadow-lg shadow-primary-200 transition-all hover:scale-[1.01] hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-60"
                      [disabled]="isSavingEvents() || isLoadingEvents() || selectedEvents().length === 0"
                    >
                      {{ isSavingEvents() ? 'Guardando eventos...' : 'Guardar eventos del torneo' }}
                    </button>
                  </div>
                </div>

                <div class="mt-5 grid gap-3">
                  <div class="rounded-2xl border border-primary-100 bg-primary-50 p-4 text-sm text-neutral-700">
                    Verifica fechas de juego e inscripcion para evitar solapes.
                  </div>
                  <div class="rounded-2xl border border-accent-100 bg-accent-50 p-4 text-sm text-neutral-700">
                    Comprueba capacidad y ubicacion antes de publicar.
                  </div>
                  <div class="rounded-2xl border border-neutral-200 bg-white p-4 text-sm text-neutral-700">
                    Siguiente paso sugerido: abrir inscripciones cuando el estado sea OPEN.
                  </div>
                </div>
              } @else {
                <div class="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-amber-800">
                  Solo el creador puede acceder a las opciones de configuracion del torneo.
                </div>
              }
            </section>
          }

          @if (activeSection() === 'inscriptions') {
            <section class="mt-6 rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
              <h2 class="text-xl font-bold text-neutral-900">Inscripciones</h2>
              <p class="mt-2 text-neutral-600">
                @if (isCreator()) {
                  Administra el proceso de inscripciones de tu torneo.
                } @else {
                  Solicita tu inscripcion si el torneo esta abierto.
                }
              </p>

              @if (isCreator()) {
                <div class="mt-5 rounded-2xl border border-neutral-200 bg-neutral-50 p-4 text-sm text-neutral-700">
                  Desde esta seccion podras revisar participantes inscritos y su estado conforme se habilite el flujo completo.
                </div>

                <div class="mt-5 rounded-3xl border border-primary-200 bg-gradient-to-br from-primary-50 to-white p-5 shadow-sm sm:p-6">
                  <div class="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                    <div>
                      <p class="text-xs font-semibold uppercase tracking-[0.2em] text-primary-600">Alta manual</p>
                      <h3 class="mt-2 text-xl font-bold text-neutral-900">Añadir jugador al evento</h3>
                      <p class="mt-2 text-sm text-neutral-600">Puedes añadir un jugador existente en la BBDD, uno inventado o dejar preparado el flujo para profesionales.</p>
                    </div>

                    <span class="rounded-full bg-white px-3 py-1 text-xs font-semibold uppercase tracking-widest text-neutral-600">
                      Origen: {{ getManualPlayerSourceLabel(manualPlayerSource()) }}
                    </span>
                  </div>

                  <div class="mt-5 grid gap-4 lg:grid-cols-2">
                    <label class="block">
                      <span class="mb-1 block text-sm font-medium text-neutral-700">Evento</span>
                      <select
                        class="w-full rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                        [ngModel]="manualPlayerEventId()"
                        (ngModelChange)="manualPlayerEventId.set($event)"
                        name="manualPlayerEventId"
                      >
                        <option value="">Selecciona evento</option>
                        @for (event of manualPlayerEventOptions(); track event.eventId) {
                          <option [value]="event.eventId">{{ event.eventName }}</option>
                        }
                      </select>
                    </label>

                    <div class="lg:col-span-2">
                      <div class="mb-2 flex items-end justify-between gap-3">
                        <div>
                          <span class="block text-sm font-medium text-neutral-700">Origen del jugador</span>
                          <p class="mt-1 text-xs text-neutral-500">Elige cómo quieres incorporar al jugador al evento.</p>
                        </div>
                        <span class="rounded-full bg-white px-3 py-1 text-xs font-semibold uppercase tracking-widest text-neutral-600">
                          {{ getManualPlayerSourceLabel(manualPlayerSource()) }}
                        </span>
                      </div>

                      <div class="grid gap-3 md:grid-cols-3">
                        @for (option of manualPlayerSourceOptions; track option.value) {
                          <button
                            type="button"
                            class="rounded-2xl border p-4 text-left transition-all"
                            [class.border-primary-300]="manualPlayerSource() === option.value"
                            [class.bg-primary-50]="manualPlayerSource() === option.value"
                            [class.shadow-sm]="manualPlayerSource() === option.value"
                            [class.border-neutral-200]="manualPlayerSource() !== option.value"
                            [class.bg-white]="manualPlayerSource() !== option.value"
                            (click)="onManualPlayerSourceChange(option.value)"
                          >
                            <div class="flex items-center justify-between gap-3">
                              <p class="font-semibold text-neutral-900">{{ option.label }}</p>
                              @if (manualPlayerSource() === option.value) {
                                <span class="rounded-full bg-white px-2.5 py-1 text-[10px] font-bold uppercase tracking-widest text-primary-700 ring-1 ring-primary-200">Activo</span>
                              }
                            </div>
                            <p class="mt-2 text-sm text-neutral-600">{{ option.description }}</p>
                          </button>
                        }
                      </div>

                      <p class="mt-3 text-xs text-neutral-500">{{ getManualPlayerSourceDescription(manualPlayerSource()) }}</p>
                    </div>

                    @if (manualPlayerSource() === 'EXISTING_PERSON') {
                      <div class="lg:col-span-2 rounded-2xl border border-neutral-200 bg-white p-4 shadow-sm">
                        <div class="flex flex-col gap-3 sm:flex-row sm:items-end">
                          <label class="block flex-1">
                            <span class="mb-1 block text-sm font-medium text-neutral-700">Buscar jugador existente</span>
                            <input
                              type="search"
                              class="w-full rounded-xl border border-neutral-300 px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                              [ngModel]="manualPlayerSearchQuery()"
                              (ngModelChange)="onManualPlayerSearchQueryChange($event)"
                              name="manualPlayerSearchQuery"
                              placeholder="Nombre, apellido o licencia"
                              autocomplete="off"
                              (keyup.enter)="searchExistingPersons()"
                            />
                            <p class="mt-2 text-xs text-neutral-500">La lista se actualiza automáticamente cuando escribes al menos 2 caracteres.</p>
                          </label>

                          <div class="flex gap-2">
                            <button
                              type="button"
                              class="rounded-2xl border border-primary-200 bg-primary-50 px-4 py-2 text-sm font-semibold text-primary-700 transition-colors hover:bg-primary-100 disabled:cursor-not-allowed disabled:opacity-60"
                              [disabled]="isSearchingPersons() || manualPlayerSearchQuery().trim().length < 2"
                              (click)="searchExistingPersons()"
                            >
                              {{ isSearchingPersons() ? 'Buscando...' : 'Buscar' }}
                            </button>

                            <button
                              type="button"
                              class="rounded-2xl border border-neutral-300 bg-white px-4 py-2 text-sm font-semibold text-neutral-700 transition-colors hover:bg-neutral-50 disabled:cursor-not-allowed disabled:opacity-60"
                              [disabled]="isSearchingPersons() && !manualPlayerSearchQuery().trim()"
                              (click)="onManualPlayerSearchQueryChange('')"
                            >
                              Limpiar
                            </button>
                          </div>
                        </div>

                        @if (manualPlayerSearchError()) {
                          <div class="mt-3 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                            {{ manualPlayerSearchError() }}
                          </div>
                        }

                        @if (isSearchingPersons()) {
                          <div class="mt-4 rounded-2xl border border-dashed border-primary-200 bg-primary-50 px-4 py-3 text-sm text-primary-700">
                            Buscando coincidencias en la BBDD...
                          </div>
                        }

                        @if (manualPlayerSearchResults().length > 0) {
                          <div class="mt-4 flex items-center justify-between gap-3">
                            <p class="text-sm font-medium text-neutral-700">{{ manualPlayerSearchResults().length }} resultado(s) encontrado(s)</p>
                            @if (manualPlayerSelectedPersonId()) {
                              <span class="rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700">Seleccionado: {{ getSelectedManualPlayerLabel() }}</span>
                            }
                          </div>

                          <div class="mt-3 grid gap-3">
                            @for (person of manualPlayerSearchResults(); track person.id) {
                              <button
                                type="button"
                                class="rounded-2xl border px-4 py-3 text-left transition-all hover:-translate-y-0.5 hover:shadow-sm"
                                [class.border-primary-300]="manualPlayerSelectedPersonId() === person.id"
                                [class.bg-primary-50]="manualPlayerSelectedPersonId() === person.id"
                                [class.border-neutral-200]="manualPlayerSelectedPersonId() !== person.id"
                                [class.bg-white]="manualPlayerSelectedPersonId() !== person.id"
                                (click)="selectExistingPerson(person)"
                              >
                                <div class="flex items-center justify-between gap-3">
                                  <div>
                                    <p class="font-semibold text-neutral-900">{{ person.firstName }} {{ person.lastName }}</p>
                                    <p class="mt-1 text-xs text-neutral-500">
                                      {{ person.tennisId || 'Sin licencia' }} · {{ person.gender || 'Sin género' }} · {{ person.nationality || 'Sin nacionalidad' }}
                                    </p>
                                  </div>
                                  <span class="rounded-full bg-neutral-100 px-3 py-1 text-xs font-semibold text-neutral-600">
                                    {{ manualPlayerSelectedPersonId() === person.id ? 'Seleccionado' : 'Seleccionar' }}
                                  </span>
                                </div>
                              </button>
                            }
                          </div>
                        } @else if (!isSearchingPersons() && manualPlayerSearchQuery().trim().length >= 2 && !manualPlayerSearchError()) {
                          <div class="mt-4 rounded-2xl border border-dashed border-neutral-300 bg-neutral-50 px-4 py-3 text-sm text-neutral-600">
                            No se han encontrado jugadores con ese criterio. Prueba con otro nombre, apellido o licencia.
                          </div>
                        }

                        @if (manualPlayerSelectedPersonId()) {
                          <p class="mt-3 text-sm font-medium text-emerald-700">
                            Jugador seleccionado: {{ getSelectedManualPlayerLabel() }}
                          </p>
                        }
                      </div>
                    } @else {
                      <div class="grid gap-4 lg:col-span-2 sm:grid-cols-2">
                        <label class="block">
                          <span class="mb-1 block text-sm font-medium text-neutral-700">Nombre</span>
                          <input
                            type="text"
                            class="w-full rounded-xl border border-neutral-300 px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                            [ngModel]="manualPlayerFirstName()"
                            (ngModelChange)="manualPlayerFirstName.set($event)"
                            name="manualPlayerFirstName"
                            placeholder="Nombre"
                          />
                        </label>

                        <label class="block">
                          <span class="mb-1 block text-sm font-medium text-neutral-700">Apellido</span>
                          <input
                            type="text"
                            class="w-full rounded-xl border border-neutral-300 px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                            [ngModel]="manualPlayerLastName()"
                            (ngModelChange)="manualPlayerLastName.set($event)"
                            name="manualPlayerLastName"
                            placeholder="Apellido"
                          />
                        </label>

                        <label class="block">
                          <span class="mb-1 block text-sm font-medium text-neutral-700">Genero</span>
                          <select
                            class="w-full rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                            [ngModel]="manualPlayerGender()"
                            (ngModelChange)="manualPlayerGender.set($event)"
                            name="manualPlayerGender"
                          >
                            <option value="">Selecciona genero</option>
                            <option value="MALE">Masculino</option>
                            <option value="FEMALE">Femenino</option>
                            <option value="MIXED">Mixto</option>
                          </select>
                        </label>

                        <label class="block">
                          <span class="mb-1 block text-sm font-medium text-neutral-700">Fecha de nacimiento</span>
                          <input
                            type="date"
                            class="w-full rounded-xl border border-neutral-300 px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                            [ngModel]="manualPlayerBirthDate()"
                            (ngModelChange)="manualPlayerBirthDate.set($event)"
                            name="manualPlayerBirthDate"
                          />
                        </label>

                        <label class="block">
                          <span class="mb-1 block text-sm font-medium text-neutral-700">Nacionalidad</span>
                          <input
                            type="text"
                            class="w-full rounded-xl border border-neutral-300 px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                            [ngModel]="manualPlayerNationality()"
                            (ngModelChange)="manualPlayerNationality.set($event)"
                            name="manualPlayerNationality"
                            placeholder="ESP"
                          />
                        </label>

                        <label class="block">
                          <span class="mb-1 block text-sm font-medium text-neutral-700">Licencia / ID</span>
                          <input
                            type="text"
                            class="w-full rounded-xl border border-neutral-300 px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                            [ngModel]="manualPlayerTennisId()"
                            (ngModelChange)="manualPlayerTennisId.set($event)"
                            name="manualPlayerTennisId"
                            placeholder="LIC-123"
                          />
                        </label>
                      </div>
                    }
                  </div>

                  @if (manualPlayerError()) {
                    <div class="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                      {{ manualPlayerError() }}
                    </div>
                  }

                  @if (manualPlayerSuccess()) {
                    <div class="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                      {{ manualPlayerSuccess() }}
                    </div>
                  }

                  <div class="mt-4 flex justify-end">
                    <button
                      type="button"
                      class="rounded-2xl bg-gradient-to-r from-primary-600 to-accent-600 px-5 py-3 font-semibold text-white shadow-lg shadow-primary-200 transition-all hover:scale-[1.01] hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-60"
                      [disabled]="isSubmittingManualPlayer()"
                      (click)="submitManualPlayer()"
                    >
                      {{ isSubmittingManualPlayer() ? 'Añadiendo jugador...' : 'Añadir jugador al evento' }}
                    </button>
                  </div>
                </div>
              } @else {
                <div class="mt-5 rounded-2xl border border-neutral-200 bg-neutral-50 p-4 text-sm text-neutral-700">
                  Estado actual del torneo: <span class="font-semibold text-neutral-900">{{ tournament()!.status }}</span>
                </div>

                @if (!isProfileComplete()) {
                  <div class="mt-4 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
                    Debes completar tu perfil para inscribirte.
                    <a routerLink="/perfil" class="ml-2 font-semibold underline">Ir a completar perfil</a>
                  </div>
                }

                <div class="mt-4 grid gap-3 sm:grid-cols-2">
                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700">Categoria</span>
                    <select
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [value]="selectedInscriptionCategoryId() ?? ''"
                      (change)="onInscriptionCategoryChange($any($event.target).value)"
                    >
                      <option value="">Selecciona categoria</option>
                      @for (category of inscriptionCategories(); track category.categoryId) {
                        <option [value]="category.categoryId">{{ category.eventCategory }}</option>
                      }
                    </select>
                  </label>

                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700">Genero del evento</span>
                    <select
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [value]="selectedInscriptionGender() ?? ''"
                      (change)="onInscriptionGenderChange($any($event.target).value)"
                    >
                      <option value="">Selecciona genero</option>
                      @for (gender of inscriptionGenderOptions(); track gender) {
                        <option [value]="gender">{{ getGenderLabel(gender) }}</option>
                      }
                    </select>
                  </label>
                </div>

                <button
                  type="button"
                  class="mt-4 rounded-2xl bg-gradient-to-r from-primary-600 to-accent-600 px-5 py-3 font-semibold text-white shadow-lg shadow-primary-200 transition-all hover:scale-[1.01] hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-60"
                  [disabled]="!canRequestInscription() || isSubmittingInscription()"
                  (click)="requestInscription()"
                >
                  {{ isSubmittingInscription() ? 'Tramitando inscripcion...' : 'Realizar inscripcion' }}
                </button>
              }

              @if (actionMessage()) {
                <div class="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                  {{ actionMessage() }}
                </div>
              }

              @if (actionError()) {
                <div class="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                  {{ actionError() }}
                </div>
              }
            </section>
          }

          @if (activeSection() === 'registeredPlayers') {
            <section class="mt-6 rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
              <div class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                <div>
                  <h2 class="text-xl font-bold text-neutral-900">Jugadores inscritos</h2>
                  <p class="mt-2 text-neutral-600">Consulta los inscritos del torneo, filtra por evento y revisa los contadores por categoria y genero.</p>
                </div>

                <label class="block min-w-72">
                  <span class="mb-1 block text-sm font-medium text-neutral-700">Filtrar por evento</span>
                  <select
                    class="w-full rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                    [disabled]="isLoadingTournamentInscriptions()"
                    [value]="selectedTournamentInscriptionEventId() ?? ''"
                    (change)="onTournamentInscriptionEventChange($any($event.target).value)"
                  >
                    <option value="">Todos los eventos</option>
                    @for (event of tournamentInscriptionEvents(); track event.eventId) {
                      <option [value]="event.eventId">{{ event.eventName }}</option>
                    }
                  </select>
                </label>
              </div>

              @if (isCreator()) {
                <div class="mt-5 rounded-2xl border border-dashed border-neutral-300 bg-neutral-50 p-4 text-sm text-neutral-600">
                  Las altas manuales se realizan desde la pestaña Inscripciones, donde el admin puede elegir el origen del jugador y añadirlo al evento correspondiente.
                </div>
              }

              @if (isLoadingTournamentInscriptions()) {
                <div class="mt-5 rounded-2xl border border-neutral-200 bg-neutral-50 p-4 text-sm text-neutral-600">
                  Cargando jugadores inscritos...
                </div>
              } @else if (tournamentInscriptionsError()) {
                <div class="mt-5 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
                  {{ tournamentInscriptionsError() }}
                </div>
              } @else {
                <div class="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                  @for (counter of tournamentInscriptionCategoryCounts(); track counter.categoryId) {
                    <article class="rounded-2xl border border-neutral-200 bg-neutral-50 p-5">
                      <div class="flex items-start justify-between gap-3">
                        <div>
                          <p class="text-xs font-semibold uppercase tracking-[0.2em] text-primary-600">Categoria</p>
                          <h3 class="mt-2 text-lg font-bold text-neutral-900">{{ counter.category }}</h3>
                        </div>
                        <div class="rounded-2xl bg-white px-3 py-2 text-right shadow-sm">
                          <p class="text-xs uppercase tracking-widest text-neutral-500">Total</p>
                          <p class="text-2xl font-black text-neutral-900">{{ counter.totalPlayers }}</p>
                        </div>
                      </div>

                      <div class="mt-4 flex flex-wrap gap-2">
                        @for (genderCount of counter.genders; track genderCount.gender) {
                          <span class="inline-flex rounded-full border border-neutral-200 bg-white px-3 py-1.5 text-xs font-semibold text-neutral-700">
                            {{ getInscriptionGenderLabel(genderCount.gender) }}: {{ genderCount.totalPlayers }}
                          </span>
                        }
                      </div>
                    </article>
                  }
                </div>

                @if (!hasTournamentInscriptionsResults()) {
                  <div class="mt-5 rounded-2xl border border-dashed border-neutral-300 bg-neutral-50 p-6 text-sm text-neutral-600">
                    No hay jugadores inscritos para el filtro seleccionado.
                  </div>
                } @else {
                  <div class="mt-6 overflow-hidden rounded-3xl border border-neutral-200">
                    <div class="grid grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)] gap-4 bg-neutral-50 px-5 py-3 text-xs font-semibold uppercase tracking-[0.18em] text-neutral-500">
                      <span>Nombre y apellidos</span>
                      <span>Evento / categoria</span>
                    </div>

                    <div class="divide-y divide-neutral-200 bg-white">
                      @for (player of tournamentInscriptionPlayers(); track player.inscriptionId + player.firstName + player.lastName) {
                        <div class="grid grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)] gap-4 px-5 py-4 text-sm text-neutral-700">
                          <div>
                            <p class="font-semibold text-neutral-900">{{ player.firstName }} {{ player.lastName }}</p>
                            <p class="mt-1 text-xs text-neutral-500">
                              {{ getInscriptionGenderLabel(player.gender) }} · {{ getPlayerSourceLabel(player.playerSource) }}
                            </p>
                          </div>
                          <div>
                            <p class="font-medium text-neutral-900">{{ player.eventName }}</p>
                            <p class="mt-1 text-xs text-neutral-500">{{ player.category }}</p>
                          </div>
                        </div>
                      }
                    </div>
                  </div>
                }
              }
            </section>
          }
        }
      </div>
    </section>
  `
})
export class TournamentDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly tournamentService = inject(TournamentService);
  private readonly memberService = inject(MemberService);
  private readonly authService = inject(AuthService);
  private readonly personService = inject(PersonService);

  readonly eventGenderOptions: TournamentEventGender[] = ['MALE', 'FEMALE', 'MIXED'];
  readonly tournament = signal<TournamentResponse | null>(null);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly actionMessage = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly isSubmittingInscription = signal(false);
  readonly activeSection = signal<TournamentDetailSection>('overview');
  readonly currentMemberId = signal<string | null>(null);
  readonly isLoadingEvents = signal(true);
  readonly isSavingEvents = signal(false);
  readonly eventCatalog = signal<TournamentEventCatalogItem[]>([]);
  readonly selectedEvents = signal<TournamentEventSelection[]>([]);
  readonly eventsSuccessMessage = signal<string | null>(null);
  readonly eventsErrorMessage = signal<string | null>(null);
  readonly eventCatalogError = signal<string | null>(null);
  readonly selectedStatus = signal<TournamentStatus | null>(null);
  readonly isUpdatingStatus = signal(false);
  readonly isProfileComplete = signal(false);
  readonly selectedInscriptionCategoryId = signal<number | null>(null);
  readonly selectedInscriptionGender = signal<TournamentEventGender | null>(null);
  readonly tournamentInscriptions = signal<TournamentInscriptionsResponse | null>(null);
  readonly isLoadingTournamentInscriptions = signal(false);
  readonly tournamentInscriptionsError = signal<string | null>(null);
  readonly selectedTournamentInscriptionEventId = signal<string | null>(null);
  readonly manualPlayerEventOptions = computed<TournamentInscriptionEvent[]>(() =>
    (this.tournament()?.events ?? [])
      .filter(event => !!event.eventId)
      .map(event => {
        const normalizedGender = event.gender.toUpperCase() as TournamentEventGender;
        const eventCategory = this.getEventLabelById(event.categoryId);
        const eventGender = this.eventGenderOptions.includes(normalizedGender) ? normalizedGender : event.gender;

        return {
          eventId: event.eventId as string,
          categoryId: event.categoryId,
          category: eventCategory,
          eventName: `${eventCategory} - ${this.getGenderLabel(eventGender as TournamentEventGender)}`,
          eventGender
        };
      })
  );
  readonly manualPlayerSource = signal<ManualParticipantSource>('EXISTING_PERSON');
  readonly manualPlayerEventId = signal<string>('');
  readonly manualPlayerSearchQuery = signal<string>('');
  readonly manualPlayerSearchResults = signal<PersonSearchResponse[]>([]);
  readonly manualPlayerSelectedPersonId = signal<string>('');
  readonly manualPlayerSearchError = signal<string | null>(null);
  readonly manualPlayerFirstName = signal<string>('');
  readonly manualPlayerLastName = signal<string>('');
  readonly manualPlayerGender = signal<string>('');
  readonly manualPlayerBirthDate = signal<string>('');
  readonly manualPlayerNationality = signal<string>('');
  readonly manualPlayerTennisId = signal<string>('');
  readonly manualPlayerError = signal<string | null>(null);
  readonly manualPlayerSuccess = signal<string | null>(null);
  readonly isSearchingPersons = signal(false);
  readonly isSubmittingManualPlayer = signal(false);
  readonly manualPlayerSourceOptions: Array<{ value: ManualParticipantSource; label: string; description: string }> = [
    {
      value: 'EXISTING_PERSON',
      label: 'Jugador existente',
      description: 'Busca por nombre, apellido o licencia y selecciónalo directamente de la BBDD.'
    },
    {
      value: 'MANUAL',
      label: 'Jugador inventado',
      description: 'Rellena los datos básicos para crear un participante temporal o manual.'
    },
    {
      value: 'PROFESSIONAL',
      label: 'Jugador profesional',
      description: 'Deja preparado el flujo para enlazarlo a la futura tabla de profesionales.'
    }
  ];
  private manualPlayerSearchDebounceHandle: ReturnType<typeof setTimeout> | null = null;

  readonly isCreator = computed(() => {
    const tournament = this.tournament();
    const memberId = this.currentMemberId();
    const providerOrganisationId = this.getProviderOrganisationId(tournament?.providerOrganisationId);

    if (!providerOrganisationId || !memberId) {
      return false;
    }

    return providerOrganisationId === memberId;
  });

  readonly canRequestInscription = computed(() => {
    const currentTournament = this.tournament();
    if (!currentTournament) {
      return false;
    }

    if (!this.isProfileComplete()) {
      return false;
    }

    return currentTournament.status === 'OPEN' && !!this.selectedEventId();
  });

  readonly inscriptionCategories = computed<TournamentEventSelection[]>(() => {
    const groupedEvents = new Map<number, Set<TournamentEventGender>>();
    const events = this.tournament()?.events ?? [];

    events.forEach(event => {
      const normalizedGender = event.gender.toUpperCase() as TournamentEventGender;
      if (!this.eventGenderOptions.includes(normalizedGender)) {
        return;
      }

      const currentGenders = groupedEvents.get(event.categoryId) ?? new Set<TournamentEventGender>();
      currentGenders.add(normalizedGender);
      groupedEvents.set(event.categoryId, currentGenders);
    });

    return Array.from(groupedEvents.entries()).map(([categoryId, genders]) => ({
      categoryId,
      eventCategory: this.getEventLabelById(categoryId),
      genders: Array.from(genders)
    }));
  });

  readonly inscriptionGenderOptions = computed<TournamentEventGender[]>(() => {
    const selectedCategoryId = this.selectedInscriptionCategoryId();
    if (!selectedCategoryId) {
      return [];
    }

    return this.inscriptionCategories().find(event => event.categoryId === selectedCategoryId)?.genders ?? [];
  });

  readonly selectedEventId = computed<string | null>(() => {
    const selectedCategoryId = this.selectedInscriptionCategoryId();
    const selectedGender = this.selectedInscriptionGender();
    const events = this.tournament()?.events ?? [];

    if (!selectedCategoryId || !selectedGender) {
      return null;
    }

    const matchedEvent = events.find(event => event.categoryId === selectedCategoryId && event.gender.toUpperCase() === selectedGender);
    return matchedEvent?.eventId ?? null;
  });

  readonly allowedStatusTransitions = computed<TournamentStatus[]>(() => {
    const currentTournament = this.tournament();
    if (!currentTournament) {
      return [];
    }

    const transitionsByStatus: Record<TournamentStatus, TournamentStatus[]> = {
      DRAFT: ['OPEN', 'CANCELLED'],
      OPEN: ['CLOSED', 'CANCELLED'],
      ACTIVE: ['CLOSED', 'CANCELLED'],
      CLOSED: ['IN_PROGRESS', 'CANCELLED'],
      IN_PROGRESS: ['COMPLETED', 'CANCELLED'],
      COMPLETED: [],
      CANCELLED: []
    };

    return transitionsByStatus[currentTournament.status] ?? [];
  });

  readonly tournamentInscriptionEvents = computed<TournamentInscriptionEvent[]>(() => this.tournamentInscriptions()?.events ?? []);

  readonly tournamentInscriptionCategoryCounts = computed<TournamentInscriptionCategoryCount[]>(() =>
    this.tournamentInscriptions()?.categoryCounts ?? []
  );

  readonly tournamentInscriptionPlayers = computed(() => this.tournamentInscriptions()?.inscriptions ?? []);

  readonly hasTournamentInscriptionsResults = computed(() => this.tournamentInscriptionPlayers().length > 0);

  readonly getSurfaceLabel = getTournamentSurfaceCategoryLabel;
  readonly getGenderLabel = getTournamentEventGenderLabel;

  constructor() {
    this.loadEventCatalog();
  }

  ngOnInit(): void {
    const tournamentId = this.route.snapshot.paramMap.get('id');
    if (!tournamentId) {
      this.errorMessage.set('No se encontro el identificador del torneo.');
      this.isLoading.set(false);
      return;
    }

    this.resolveCurrentMemberId();
    this.loadTournament(tournamentId);
  }

  setActiveSection(section: TournamentDetailSection): void {
    this.activeSection.set(section);
    this.actionMessage.set(null);
    this.actionError.set(null);

    if (section === 'registeredPlayers' && !this.tournamentInscriptions() && !this.isLoadingTournamentInscriptions()) {
      this.loadTournamentInscriptions();
    }
  }

  onManualPlayerSourceChange(source: ManualParticipantSource): void {
    this.manualPlayerSource.set(source);
    this.manualPlayerError.set(null);
    this.manualPlayerSuccess.set(null);
    this.manualPlayerSearchResults.set([]);
    this.manualPlayerSelectedPersonId.set('');
    this.cancelManualPlayerSearch();

    if (source === 'EXISTING_PERSON') {
      this.manualPlayerFirstName.set('');
      this.manualPlayerLastName.set('');
      this.manualPlayerGender.set('');
      this.manualPlayerBirthDate.set('');
      this.manualPlayerNationality.set('');
      this.manualPlayerTennisId.set('');

      if (this.manualPlayerSearchQuery().trim().length >= 2) {
        this.scheduleManualPlayerSearch();
      }
    } else {
      this.manualPlayerSearchQuery.set('');
    }
  }

  toggleCatalogEvent(catalogEvent: TournamentEventCatalogItem, checked: boolean): void {
    if (checked) {
      if (this.isEventSelected(catalogEvent.id)) {
        return;
      }

      this.selectedEvents.update(events => [
        ...events,
        {
          categoryId: catalogEvent.id,
          eventCategory: catalogEvent.category,
          genders: []
        }
      ]);
      return;
    }

    this.selectedEvents.update(events => events.filter(event => event.categoryId !== catalogEvent.id));
  }

  toggleEventGender(categoryId: number, gender: TournamentEventGender, checked: boolean): void {
    this.selectedEvents.update(events =>
      events.map(event =>
        event.categoryId === categoryId
          ? {
            ...event,
            genders: checked
              ? Array.from(new Set([...event.genders, gender]))
              : event.genders.filter(currentGender => currentGender !== gender)
          }
          : event
      )
    );
  }

  hasEventGender(categoryId: number, gender: TournamentEventGender): boolean {
    return this.selectedEvents()
      .find(event => event.categoryId === categoryId)
      ?.genders.includes(gender) ?? false;
  }

  clearSelectedEvents(): void {
    this.selectedEvents.set([]);
    this.eventsSuccessMessage.set(null);
    this.eventsErrorMessage.set(null);
  }

  isEventSelected(categoryId: number): boolean {
    return this.selectedEvents().some(event => event.categoryId === categoryId);
  }

  getEventLabelById(categoryId: number): string {
    return this.eventCatalog().find(event => event.id === categoryId)?.category ?? String(categoryId);
  }

  onSelectedStatusChange(nextStatus: string): void {
    this.selectedStatus.set(nextStatus as TournamentStatus);
  }

  canUpdateStatus(): boolean {
    const tournament = this.tournament();
    const selectedStatus = this.selectedStatus();
    if (!tournament || !selectedStatus || this.isUpdatingStatus()) {
      return false;
    }

    return this.allowedStatusTransitions().includes(selectedStatus);
  }

  updateTournamentStatus(): void {
    const currentTournament = this.tournament();
    const nextStatus = this.selectedStatus();

    if (!currentTournament || !nextStatus) {
      return;
    }

    if (!this.allowedStatusTransitions().includes(nextStatus)) {
      this.actionError.set('No se puede realizar la transición de estado seleccionada.');
      return;
    }

    this.isUpdatingStatus.set(true);
    this.actionError.set(null);
    this.actionMessage.set(null);

    this.tournamentService.updateTournamentStatus(currentTournament.id, { status: nextStatus }).subscribe({
      next: updatedTournament => {
        this.tournament.set(updatedTournament);
        this.selectedStatus.set(this.allowedStatusTransitions()[0] ?? null);
        this.actionMessage.set('Estado del torneo actualizado correctamente.');
        this.isUpdatingStatus.set(false);
      },
      error: () => {
        this.actionError.set('No se pudo actualizar el estado del torneo.');
        this.isUpdatingStatus.set(false);
      }
    });
  }

  saveTournamentEvents(): void {
    const currentTournament = this.tournament();
    const selectedEvents = this.selectedEvents();

    if (!currentTournament) {
      return;
    }

    if (selectedEvents.length === 0) {
      this.eventsErrorMessage.set('Selecciona al menos un evento antes de guardar.');
      return;
    }

    if (selectedEvents.some(event => event.genders.length === 0)) {
      this.eventsErrorMessage.set('Debes seleccionar al menos un género en cada evento antes de guardar.');
      return;
    }

    const payload: TournamentEventsConfigRequest = {
      events: selectedEvents.flatMap(event =>
        event.genders.map(gender => ({
          categoryId: event.categoryId,
          gender
        }))
      )
    };

    this.isSavingEvents.set(true);
    this.eventsErrorMessage.set(null);
    this.eventsSuccessMessage.set(null);

    this.tournamentService.saveTournamentEvents(currentTournament.id, payload).subscribe({
      next: () => {
        this.isSavingEvents.set(false);
        this.eventsSuccessMessage.set('Eventos del torneo guardados correctamente.');
      },
      error: () => {
        this.isSavingEvents.set(false);
        this.eventsErrorMessage.set('No se pudieron guardar los eventos del torneo.');
      }
    });
  }

  requestInscription(): void {
    const currentTournament = this.tournament();
    const eventId = this.selectedEventId();
    const categoryId = this.selectedInscriptionCategoryId();

    if (!currentTournament) {
      return;
    }

    if (!eventId || !categoryId) {
      this.actionError.set('Selecciona categoria y genero antes de solicitar la inscripcion.');
      return;
    }

    if (!this.canRequestInscription()) {
      this.actionError.set('No cumples los requisitos para inscribirte en este evento.');
      return;
    }

    this.isSubmittingInscription.set(true);
    this.actionError.set(null);
    this.actionMessage.set(null);

    this.tournamentService.requestInscription(currentTournament.id, eventId, { categoryId, partnerId: null }).subscribe({
      next: () => {
        this.isSubmittingInscription.set(false);
        this.actionMessage.set('Inscripcion realizada correctamente para el evento seleccionado.');
      },
      error: () => {
        this.isSubmittingInscription.set(false);
        this.actionError.set('No se pudo completar la inscripcion para este evento.');
      }
    });
  }

  onInscriptionCategoryChange(rawCategoryId: string): void {
    if (!rawCategoryId) {
      this.selectedInscriptionCategoryId.set(null);
      this.selectedInscriptionGender.set(null);
      return;
    }

    const categoryId = Number(rawCategoryId);
    this.selectedInscriptionCategoryId.set(Number.isNaN(categoryId) ? null : categoryId);
    this.selectedInscriptionGender.set(null);
  }

  onInscriptionGenderChange(rawGender: string): void {
    if (!rawGender) {
      this.selectedInscriptionGender.set(null);
      return;
    }

    this.selectedInscriptionGender.set(rawGender as TournamentEventGender);
  }

  onTournamentInscriptionEventChange(eventId: string): void {
    this.selectedTournamentInscriptionEventId.set(eventId || null);
    this.loadTournamentInscriptions();
  }

  searchExistingPersons(): void {
    const query = this.manualPlayerSearchQuery().trim();
    if (query.length < 2) {
      this.manualPlayerSearchResults.set([]);
      this.manualPlayerSearchError.set('Escribe al menos 2 caracteres para buscar un jugador.');
      return;
    }

    this.isSearchingPersons.set(true);
    this.manualPlayerSearchError.set(null);

    this.personService.searchPersons(query).subscribe({
      next: persons => {
        this.manualPlayerSearchResults.set(persons);
        this.manualPlayerSelectedPersonId.set('');
        this.isSearchingPersons.set(false);
      },
      error: () => {
        this.manualPlayerSearchResults.set([]);
        this.manualPlayerSearchError.set('No se pudieron cargar los jugadores existentes.');
        this.isSearchingPersons.set(false);
      }
    });
  }

  onManualPlayerSearchQueryChange(query: string): void {
    this.manualPlayerSearchQuery.set(query);
    this.manualPlayerSearchError.set(null);
    this.manualPlayerSuccess.set(null);
    this.manualPlayerSelectedPersonId.set('');

    this.scheduleManualPlayerSearch();
  }

  selectExistingPerson(person: PersonSearchResponse): void {
    this.manualPlayerSelectedPersonId.set(person.id);
    this.manualPlayerSearchError.set(null);
    this.manualPlayerSuccess.set(null);
  }

  submitManualPlayer(): void {
    const currentTournament = this.tournament();
    const eventId = this.manualPlayerEventId().trim() || this.selectedTournamentInscriptionEventId() || '';

    if (!currentTournament) {
      return;
    }

    if (!eventId) {
      this.manualPlayerError.set('Selecciona un evento para añadir el jugador.');
      return;
    }

    const playerSource = this.manualPlayerSource();
    const payload: ManualEventInscriptionRequest = {
      playerSource
    };

    if (playerSource === 'EXISTING_PERSON') {
      const selectedPersonId = this.manualPlayerSelectedPersonId();
      if (!selectedPersonId) {
        this.manualPlayerError.set('Selecciona un jugador existente de la lista.');
        return;
      }

      payload.personId = selectedPersonId;
    } else {
      payload.firstName = this.manualPlayerFirstName().trim();
      payload.lastName = this.manualPlayerLastName().trim() || null;
      payload.gender = this.manualPlayerGender().trim() || null;
      payload.birthDate = this.manualPlayerBirthDate().trim() || null;
      payload.nationality = this.manualPlayerNationality().trim() || null;
      payload.tennisId = this.manualPlayerTennisId().trim() || null;
    }

    this.isSubmittingManualPlayer.set(true);
    this.manualPlayerError.set(null);
    this.manualPlayerSuccess.set(null);

    this.tournamentService.addManualInscription(currentTournament.id, eventId, payload).subscribe({
      next: () => {
        this.isSubmittingManualPlayer.set(false);
        this.manualPlayerSuccess.set('Jugador añadido al evento correctamente.');
        this.manualPlayerSearchResults.set([]);
        this.manualPlayerSelectedPersonId.set('');
        this.manualPlayerFirstName.set('');
        this.manualPlayerLastName.set('');
        this.manualPlayerGender.set('');
        this.manualPlayerBirthDate.set('');
        this.manualPlayerNationality.set('');
        this.manualPlayerTennisId.set('');
        this.loadTournamentInscriptions();
      },
      error: () => {
        this.isSubmittingManualPlayer.set(false);
        this.manualPlayerError.set('No se pudo añadir el jugador al evento.');
      }
    });
  }

  private loadTournament(tournamentId: string): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.tournamentService.getTournamentById(tournamentId).subscribe({
      next: tournament => {
        this.tournament.set(tournament);
        this.hydrateSelectedEventsFromTournament(tournament.events ?? []);
        this.initializeInscriptionSelection();
        this.syncManualPlayerEventSelection(tournament.events ?? []);
        this.selectedStatus.set(this.getDefaultStatusSelection(tournament.status));
        this.loadTournamentInscriptions();
        this.isLoading.set(false);
        this.activeSection.set(this.isCreator() ? 'setup' : 'overview');
      },
      error: () => {
        this.errorMessage.set('No se pudo cargar el detalle del torneo.');
        this.isLoading.set(false);
      }
    });
  }

  private loadEventCatalog(): void {
    this.isLoadingEvents.set(true);
    this.eventCatalogError.set(null);

    this.tournamentService.getEventCatalog().subscribe({
      next: catalog => {
        this.eventCatalog.set(catalog);
        this.isLoadingEvents.set(false);
      },
      error: () => {
        this.eventCatalog.set([]);
        this.eventCatalogError.set('No se pudo cargar el catálogo de eventos desde backend.');
        this.isLoadingEvents.set(false);
      }
    });
  }

  private resolveCurrentMemberId(): void {
    const email = this.authService.getCurrentUserEmail();
    if (!email) {
      return;
    }

    this.memberService.getMemberByEmail(email).subscribe({
      next: member => {
        this.currentMemberId.set(member.id);
      },
      error: () => {
        this.currentMemberId.set(null);
      }
    });

    this.memberService.getMyProfile().subscribe({
      next: profile => {
        const complete = !!profile.firstName && !!profile.gender && !!profile.birthDate;
        this.isProfileComplete.set(complete);
      },
      error: () => {
        this.isProfileComplete.set(false);
      }
    });
  }

  private hydrateSelectedEventsFromTournament(events: TournamentEventCategoryGender[]): void {
    const groupedEvents = new Map<number, Set<TournamentEventGender>>();

    events.forEach(event => {
      const normalizedGender = event.gender.toUpperCase() as TournamentEventGender;
      if (!this.eventGenderOptions.includes(normalizedGender)) {
        return;
      }

      const currentGenders = groupedEvents.get(event.categoryId) ?? new Set<TournamentEventGender>();
      currentGenders.add(normalizedGender);
      groupedEvents.set(event.categoryId, currentGenders);
    });

    const selections: TournamentEventSelection[] = Array.from(groupedEvents.entries()).map(([categoryId, genders]) => ({
      categoryId,
      eventCategory: this.getEventLabelById(categoryId),
      genders: Array.from(genders)
    }));

    this.selectedEvents.set(selections);
  }

  private getDefaultStatusSelection(currentStatus: TournamentStatus): TournamentStatus | null {
    const transitionsByStatus: Record<TournamentStatus, TournamentStatus[]> = {
      DRAFT: ['OPEN', 'CANCELLED'],
      OPEN: ['CLOSED', 'CANCELLED'],
      ACTIVE: ['CLOSED', 'CANCELLED'],
      CLOSED: ['IN_PROGRESS', 'CANCELLED'],
      IN_PROGRESS: ['COMPLETED', 'CANCELLED'],
      COMPLETED: [],
      CANCELLED: []
    };

    return transitionsByStatus[currentStatus]?.[0] ?? null;
  }

  private getProviderOrganisationId(providerOrganisation: string | TournamentProviderSummary | null | undefined): string | null {
    if (!providerOrganisation) {
      return null;
    }

    if (typeof providerOrganisation === 'string') {
      return providerOrganisation;
    }

    return providerOrganisation.id ?? null;
  }

  private initializeInscriptionSelection(): void {
    const firstCategory = this.inscriptionCategories()[0];
    if (!firstCategory) {
      this.selectedInscriptionCategoryId.set(null);
      this.selectedInscriptionGender.set(null);
      return;
    }

    this.selectedInscriptionCategoryId.set(firstCategory.categoryId);
    this.selectedInscriptionGender.set(firstCategory.genders[0] ?? null);
  }

  private loadTournamentInscriptions(): void {
    const currentTournament = this.tournament();
    if (!currentTournament) {
      return;
    }

    this.isLoadingTournamentInscriptions.set(true);
    this.tournamentInscriptionsError.set(null);

    this.tournamentService
      .getTournamentInscriptions(currentTournament.id, this.selectedTournamentInscriptionEventId() ?? undefined)
      .subscribe({
        next: response => {
          this.tournamentInscriptions.set(response);
          this.isLoadingTournamentInscriptions.set(false);
        },
        error: () => {
          this.tournamentInscriptions.set(null);
          this.tournamentInscriptionsError.set('No se pudo cargar el listado de jugadores inscritos.');
          this.isLoadingTournamentInscriptions.set(false);
        }
      });
  }

  protected getInscriptionGenderLabel(gender: string): string {
    const normalizedGender = gender?.toUpperCase();

    if (normalizedGender === 'UNKNOWN') {
      return 'Sin especificar';
    }

    if (normalizedGender === 'MALE' || normalizedGender === 'FEMALE' || normalizedGender === 'MIXED') {
      return this.getGenderLabel(normalizedGender);
    }

    return gender;
  }

  getManualPlayerSourceLabel(source?: ManualParticipantSource | string | null): string {
    switch (source) {
      case 'MANUAL':
        return 'Jugador inventado';
      case 'PROFESSIONAL':
        return 'Jugador profesional';
      case 'EXISTING_PERSON':
        return 'Jugador existente';
      default:
        return 'Jugador';
    }
  }

  getManualPlayerSourceDescription(source?: ManualParticipantSource | string | null): string {
    switch (source) {
      case 'EXISTING_PERSON':
        return 'Busca en la BBDD y selecciona al jugador correcto sin salir del detalle.';
      case 'MANUAL':
        return 'Introduce los datos básicos del participante directamente en el formulario.';
      case 'PROFESSIONAL':
        return 'Prepara la inscripción para enlazarla con la futura tabla de profesionales.';
      default:
        return 'Selecciona la forma en la que quieres incorporar al jugador.';
    }
  }

  getPlayerSourceLabel(source?: string | null): string {
    return this.getManualPlayerSourceLabel(source);
  }

  getSelectedManualPlayerLabel(): string {
    const selected = this.manualPlayerSearchResults().find(person => person.id === this.manualPlayerSelectedPersonId());
    if (!selected) {
      return this.manualPlayerSelectedPersonId();
    }

    return `${selected.firstName} ${selected.lastName ?? ''}`.trim();
  }

  private syncManualPlayerEventSelection(events: Array<{ eventId?: string | null }>): void {
    if (this.manualPlayerEventId() && events.some(event => event.eventId === this.manualPlayerEventId())) {
      return;
    }

    const nextEventId = this.selectedTournamentInscriptionEventId() ?? events[0]?.eventId ?? '';
    this.manualPlayerEventId.set(nextEventId);
  }

  private scheduleManualPlayerSearch(): void {
    this.cancelManualPlayerSearch();

    const query = this.manualPlayerSearchQuery().trim();
    if (query.length < 2 || this.manualPlayerSource() !== 'EXISTING_PERSON') {
      this.manualPlayerSearchResults.set([]);
      return;
    }

    this.manualPlayerSearchDebounceHandle = setTimeout(() => {
      this.searchExistingPersons();
    }, 300);
  }

  private cancelManualPlayerSearch(): void {
    if (!this.manualPlayerSearchDebounceHandle) {
      return;
    }

    clearTimeout(this.manualPlayerSearchDebounceHandle);
    this.manualPlayerSearchDebounceHandle = null;
  }
}
