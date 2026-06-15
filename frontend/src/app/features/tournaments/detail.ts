import { CommonModule, DatePipe } from '@angular/common';
import {
  Component,
  OnDestroy,
  OnInit,
  AfterViewInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { PersonService } from '../../data/services/person.service';
import { ProPlayerService } from '../../data/services/pro-player.service';
import { NationalityOption } from '../../data/interfaces/reference-data.model';
import {
  CourtResponse,
  TournamentInscriptionCategoryCount,
  TournamentInscriptionEvent,
  TournamentInscriptionsResponse,
  TournamentEventCatalogItem,
  TournamentEventGender,
  TournamentEventResponse,
  TournamentEventSelection,
  TournamentEventStageSelection,
  TournamentEventsConfigRequest,
  ManualParticipantSource,
  ManualEventInscriptionRequest,
  DrawResponse,
  MatchResponse,
  MatchScheduleTimeType,
  StageResponse,
  TournamentProviderSummary,
  TournamentStatus,
  TournamentResponse,
  getTournamentEventGenderLabel,
  getTournamentStageTypeLabel,
  getTournamentSurfaceCategoryLabel,
  TournamentStageType,
  validateStageSequence,
  isConsolationDisabled,
  getAvailableStageOptions,
  isValidStageType
} from '../../data/interfaces/tournament.model';
import { MemberService } from '../../data/services/member.service';
import { TournamentLiveUpdatesService } from '../../data/services/tournament-live-updates.service';
import { TournamentService } from '../../data/services/tournament.service';
import { ReferenceDataService } from '../../data/services/reference-data.service';
import { getApiErrorMessage } from '../../core/errors/api-error.util';
import { StagesComponent } from './components/stages.component';

type TournamentDetailSection = 'overview' | 'setup' | 'inscriptions' | 'stages';

type DrawGenerationFeedback = {
  status: 'success' | 'error';
  message: string;
};

type ManualPlayerLookupResult = {
  id: string;
  tennisId: string | null;
  firstName: string;
  lastName: string | null;
  nationality: string | null;
  birthDate: string | null;
  gender: string | null;
  rankingPosition?: number | null;
  ageCategory?: string | null;
  clubName?: string | null;
};

type MatchScheduleDraft = {
  courtId: string;
  scheduledAt: string;
  scheduleTimeType: MatchScheduleTimeType;
  cascade: boolean;
};

type TournamentMatchScheduleRow = {
  match: MatchResponse;
  eventLabel: string;
  drawLabel: string;
  firstPlayerName: string;
  secondPlayerName: string;
};

type MatchScheduleSortField = 'event' | 'round' | 'scheduledAt' | 'court';
type MatchScheduleSortDirection = 'asc' | 'desc';

@Component({
  selector: 'app-tournament-detail-page',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe, FormsModule, StagesComponent],
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
            <p class="text-xs font-semibold uppercase tracking-[0.22em] text-primary-600">Torneo</p>
            <div class="mt-3 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <h1 class="text-3xl font-black text-neutral-900 sm:text-4xl">{{ tournament()!.formalName }}</h1>
                <p class="mt-2 text-neutral-600">
                  📍 {{ tournament()!.location }}
                  @if (tournament()!.locationFormattedAddress) {
                    <span class="text-neutral-400 mx-2">|</span>
                    <span class="text-neutral-500 text-sm">{{ tournament()!.locationFormattedAddress }}</span>
                  }
                  @if (hasTournamentMapsLink(tournament()!)) {
                    <a
                      [href]="getTournamentMapsLink(tournament()!)"
                      target="_blank"
                      rel="noopener noreferrer"
                      class="ml-2 text-primary-600 hover:text-primary-800 hover:underline inline-flex items-center gap-0.5 text-sm font-medium"
                    >
                      Ver en mapa ↗
                    </a>
                  }
                </p>
              </div>
              <span class="inline-flex w-fit rounded-full border px-4 py-2 text-sm font-semibold {{ getStatusColorClasses(tournament()!.status) }}">
                Estado: {{ getStatusLabel(tournament()!.status) }}
              </span>
              @if (isProfessionalTournament()) {
                <span class="inline-flex w-fit rounded-full border border-neutral-900 bg-neutral-900 px-4 py-2 text-sm font-bold uppercase tracking-widest text-white">
                  PRO
                </span>
              }
            </div>

            <div class="mt-4 flex flex-wrap gap-2">
              <button
                type="button"
                (click)="exportTournamentPdf()"
                [disabled]="isExportingTournamentPdf()"
                class="inline-flex items-center gap-2 rounded-xl border border-neutral-300 bg-white px-4 py-2 text-sm font-semibold text-neutral-700 transition-colors hover:border-primary-300 hover:bg-primary-50 hover:text-primary-700 disabled:cursor-not-allowed disabled:opacity-60"
              >
                @if (isExportingTournamentPdf()) {
                  <span class="h-4 w-4 animate-spin rounded-full border-2 border-neutral-300 border-t-primary-600"></span>
                  Exportando...
                } @else {
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  Exportar datos (PDF)
                }
              </button>
            </div>

            <div class="mt-6 rounded-2xl border border-neutral-200 bg-neutral-50 p-2">
              <div class="flex flex-wrap gap-2">
                <button
                  type="button"
                  (click)="setActiveSection('overview')"
                  [class]="activeSection() === 'overview' ? 'rounded-xl bg-white px-4 py-2 text-sm font-semibold text-primary-700 shadow-sm' : 'rounded-xl px-4 py-2 text-sm font-semibold text-neutral-600 hover:bg-white/70'"
                >
                  Información general
                </button>

                @if (isCreator()) {
                  <button
                    type="button"
                    (click)="setActiveSection('setup')"
                    [class]="activeSection() === 'setup' ? 'rounded-xl bg-white px-4 py-2 text-sm font-semibold text-primary-700 shadow-sm' : 'rounded-xl px-4 py-2 text-sm font-semibold text-neutral-600 hover:bg-white/70'"
                  >
                    Configuración
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
                  (click)="setActiveSection('stages')"
                  [class]="activeSection() === 'stages' ? 'rounded-xl bg-white px-4 py-2 text-sm font-semibold text-primary-700 shadow-sm' : 'rounded-xl px-4 py-2 text-sm font-semibold text-neutral-600 hover:bg-white/70'"
                >
                  Cuadros
                </button>
              </div>
            </div>
          </header>

          @if (activeSection() === 'overview') {
            <section class="mt-6 rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
              <h2 class="text-xl font-bold text-neutral-900">Información del torneo</h2>
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
                  <p class="text-xs uppercase tracking-widest text-neutral-500">Fechas de juego</p>
                  <p class="mt-1 font-semibold text-neutral-900">
                    {{ tournament()!.playStartDate | date: 'dd/MM/yyyy' }} - {{ tournament()!.playEndDate | date: 'dd/MM/yyyy' }}
                  </p>
                </div>
                <div class="rounded-2xl border border-neutral-200 p-4">
                  <p class="text-xs uppercase tracking-widest text-neutral-500">Periodo de inscripción</p>
                  <p class="mt-1 font-semibold text-neutral-900">
                    {{ tournament()!.inscriptionStartDate | date: 'dd/MM/yyyy' }} - {{ tournament()!.inscriptionEndDate | date: 'dd/MM/yyyy' }}
                  </p>
                </div>
              </div>

              @if (tournament()!.locationLatitude != null && tournament()!.locationLongitude != null) {
                <div class="mt-6">
                  <p class="mb-2 text-xs uppercase tracking-widest text-neutral-500">Ubicación</p>
                  <div
                    id="tournament-map"
                    class="h-64 w-full rounded-2xl border border-neutral-200 overflow-hidden"
                  ></div>
                </div>
              }
            </section>
          }

          @if (activeSection() === 'setup') {
            <section class="mt-6 rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
              @if (isCreator()) {
                <h2 class="text-xl font-bold text-neutral-900">Configuración del torneo</h2>
                <p class="mt-2 text-neutral-600">Prepara las pruebas, pistas e inscripciones antes de poner el torneo en marcha.</p>

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
                            <option [value]="status">{{ getStatusLabel(status) }}</option>
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
                    <p class="mt-2 text-xs text-neutral-500">No hay cambios de estado disponibles para el estado actual.</p>
                    }
                  </div>

                  <div class="mt-8 border-t border-neutral-200 pt-8">
                  <div class="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
                    <div>
                      <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-600">Pruebas del torneo</p>
                      <h3 class="mt-2 text-xl font-bold text-neutral-900">Configura las categorías y modalidades</h3>
                      <p class="mt-2 text-sm text-neutral-600">Selecciona las categorías del catálogo, marca sus modalidades y define el formato del cuadro.</p>
                    </div>
                    <span class="rounded-full bg-white px-3 py-1 text-xs font-semibold uppercase tracking-widest text-neutral-600">
                      {{ selectedEvents().length }} seleccionadas
                    </span>
                  </div>

                  @if (isLoadingEvents()) {
                    <div class="mt-4 rounded-2xl border border-dashed border-neutral-300 bg-white px-4 py-3 text-sm text-neutral-600">
                      Cargando catálogo de categorías...
                    </div>
                  }

                  @if (eventCatalogError()) {
                    <div class="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                      {{ eventCatalogError() }}
                    </div>
                  }

                  <div class="mt-4 grid gap-4 lg:grid-cols-[1fr_1.15fr]">
                    <label class="block">
                      <span class="mb-1 block text-sm font-medium text-neutral-700">Categorías disponibles</span>
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
                        <p class="text-sm text-neutral-500">No hay pruebas seleccionadas todavía.</p>
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
                                    <span class="text-xs font-semibold uppercase tracking-widest text-neutral-500">Modalidades</span>
                                    <span
                                      class="inline-flex h-4 w-4 items-center justify-center rounded-full border border-neutral-300 text-[10px] font-bold text-neutral-500"
                                      title="Por cada modalidad seleccionada se creará una prueba."
                                      aria-label="Información sobre modalidades"
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

                              <div class="mt-4 rounded-2xl border border-dashed border-neutral-200 bg-white p-4">
                                <div class="flex items-center justify-between gap-3">
                                  <div>
                                    <p class="text-xs font-semibold uppercase tracking-widest text-neutral-500">Formato de cuadro</p>
                                    <p class="mt-1 text-sm text-neutral-600">Define el orden y tipo de las cuadros que tendrá esta prueba.</p>
                                  </div>
                                  <button
                                    type="button"
                                    class="rounded-full border border-neutral-300 bg-white px-3 py-1.5 text-xs font-semibold text-neutral-700 transition hover:border-primary-400 hover:text-primary-700"
                                    (click)="addEventStage(event.categoryId)"
                                  >
                                    Añadir cuadro
                                  </button>
                                </div>

                                <div class="mt-4 space-y-3">
                                  @for (stage of event.stages; track $index; let stageIndex = $index) {
                                    <div class="rounded-xl border border-neutral-200 bg-neutral-50 p-3">
                                      <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                                        <div class="flex-1">
                                          <label class="block">
                                            <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Tipo de cuadro {{ stageIndex + 1 }}</span>
                                            <select
                                              class="w-full rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 outline-none transition focus:border-primary-500 focus:bg-white"
                                              [value]="stage.stageType"
                                              (change)="updateEventStageType(event.categoryId, stageIndex, $any($event.target).value)"
                                            >
                                              @for (option of getAvailableStageOptions(event.stages, stageIndex); track option) {
                                                <option [value]="option">{{ getStageLabel(option) }}</option>
                                              }
                                            </select>
                                          </label>
                                        </div>

                                        <div class="flex items-center gap-2">
                                          <button
                                            type="button"
                                            class="rounded-full border border-red-200 bg-red-50 px-3 py-1.5 text-xs font-semibold text-red-700 transition hover:border-red-300 hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-40"
                                            [disabled]="event.stages.length === 1"
                                            (click)="removeEventStage(event.categoryId, stageIndex)"
                                          >
                                            Eliminar
                                          </button>
                                        </div>
                                      </div>
                                    </div>
                                  }
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
                      (click)="saveTournamentEvents()"
                      class="rounded-2xl bg-gradient-to-r from-primary-600 to-accent-600 px-5 py-3 font-semibold text-white shadow-lg shadow-primary-200 transition-all hover:scale-[1.01] hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-60"
                      [disabled]="isSavingEvents() || isLoadingEvents()"
                    >
                      {{ isSavingEvents() ? 'Guardando pruebas...' : 'Guardar pruebas del torneo' }}
                    </button>
                  </div>
                </div>
                </div>
              } @else {
                <div class="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-amber-800">
                  Solo el creador puede acceder a las opciones de configuración del torneo.
                </div>
              }
            </section>
          }

          @if (activeSection() === 'inscriptions') {
            <section class="mt-6 rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
              <h2 class="text-xl font-bold text-neutral-900">Inscripciones</h2>
              <p class="mt-2 text-neutral-600">
                @if (isCreator()) {
                  Administra las inscripciones de tu torneo.
                } @else {
                  Solicita tu inscripción si el torneo está abierto.
                }
              </p>

              @if (isCreator()) {
                <div class="mt-5 rounded-2xl border border-neutral-200 bg-neutral-50 p-4 text-sm text-neutral-700">
                  Desde esta sección puedes revisar inscritos y añadir jugadores a una prueba.
                </div>

                <div class="mt-5 rounded-3xl border border-primary-200 bg-gradient-to-br from-primary-50 to-white p-5 shadow-sm sm:p-6" (keydown.enter)="submitManualPlayer()">
                  <div class="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                    <div>
                      <div class="flex flex-wrap items-center gap-3">
                        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-primary-600">Alta manual</p>
                        <button
                          type="button"
                          class="rounded-full border border-emerald-200 bg-white px-3 py-1.5 text-xs font-semibold text-emerald-700 transition-colors hover:border-emerald-300 hover:bg-emerald-50 hover:text-emerald-800"
                          (click)="toggleManualPlayerPanel()"
                        >
                          {{ isManualPlayerPanelExpanded() ? 'Ocultar' : 'Mostrar' }}
                        </button>
                      </div>
                      <h3 class="mt-2 text-xl font-bold text-neutral-900">Añadir jugador a una prueba</h3>
                      <p class="mt-2 text-sm text-neutral-600">Puedes añadir un jugador existente, crear un jugador manual o seleccionar un profesional cargado.</p>
                    </div>

                    <div class="flex flex-wrap items-center gap-2">
                      <span class="rounded-full bg-white px-3 py-1 text-xs font-semibold uppercase tracking-widest text-neutral-600">
                        Origen: {{ getManualPlayerSourceLabel(manualPlayerSource()) }}
                      </span>
                    </div>
                  </div>

                  @if (isManualPlayerPanelExpanded()) {
                  <div class="mt-5 grid gap-4 lg:grid-cols-2">
                    <label class="block">
                      <span class="mb-1 block text-sm font-medium text-neutral-700">Prueba</span>
                      <select
                        class="w-full rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                        [ngModel]="manualPlayerEventId()"
                        (ngModelChange)="manualPlayerEventId.set($event)"
                        name="manualPlayerEventId"
                      >
                        <option value="">Selecciona prueba</option>
                        @for (event of manualPlayerEventOptions(); track event.eventId) {
                          <option [value]="event.eventId">{{ event.eventName }}</option>
                        }
                      </select>
                    </label>

                    <div class="lg:col-span-2">
                      <div class="mb-2 flex items-end justify-between gap-3">
                        <div>
                          <span class="block text-sm font-medium text-neutral-700">Origen del jugador</span>
                          <p class="mt-1 text-xs text-neutral-500">Elige cómo quieres incorporar al jugador a la prueba.</p>
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

                    @if (manualPlayerSource() !== 'MANUAL') {
                      <div class="lg:col-span-2 rounded-2xl border border-neutral-200 bg-white p-4 shadow-sm">
                        <div class="flex flex-col gap-3 sm:flex-row sm:items-end">
                          <label class="block flex-1">
                            <span class="mb-1 block text-sm font-medium text-neutral-700">Buscar {{ manualPlayerSource() === 'PROFESSIONAL' ? 'jugador profesional' : 'jugador existente' }}</span>
                            <input
                              type="search"
                              class="w-full rounded-xl border border-neutral-300 px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                              [ngModel]="manualPlayerSearchQuery()"
                              (ngModelChange)="onManualPlayerSearchQueryChange($event)"
                              name="manualPlayerSearchQuery"
                              placeholder="Nombre, apellido o licencia"
                              autocomplete="off"
                              (keyup.enter)="searchManualPlayerCandidates()"
                            />
                            <p class="mt-2 text-xs text-neutral-500">La lista se actualiza automáticamente cuando escribes al menos 2 caracteres.</p>
                          </label>

                          <div class="flex flex-wrap gap-2 sm:justify-end">
                            @if (manualPlayerSource() === 'PROFESSIONAL') {
                              <button
                                type="button"
                                class="rounded-2xl border px-4 py-2 text-sm font-semibold transition-colors"
                                [class.border-primary-300]="hasActiveManualPlayerFilters() || isManualPlayerFiltersPanelExpanded()"
                                [class.bg-primary-50]="hasActiveManualPlayerFilters() || isManualPlayerFiltersPanelExpanded()"
                                [class.text-primary-700]="hasActiveManualPlayerFilters() || isManualPlayerFiltersPanelExpanded()"
                                [class.border-neutral-300]="!hasActiveManualPlayerFilters() && !isManualPlayerFiltersPanelExpanded()"
                                [class.bg-white]="!hasActiveManualPlayerFilters() && !isManualPlayerFiltersPanelExpanded()"
                                [class.text-neutral-700]="!hasActiveManualPlayerFilters() && !isManualPlayerFiltersPanelExpanded()"
                                (click)="toggleManualPlayerFiltersPanel()"
                              >
                                Filtros{{ hasActiveManualPlayerFilters() ? ' activos' : '' }}
                              </button>
                            }

                            <button
                              type="button"
                              class="rounded-2xl bg-gradient-to-r from-primary-600 to-accent-600 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-primary-200 transition-all hover:scale-[1.01] hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-60"
                              [disabled]="isSubmittingManualPlayer()"
                              (click)="submitManualPlayer()"
                            >
                              {{ isSubmittingManualPlayer() ? 'Añadiendo...' : 'Añadir jugador' }}
                            </button>
                          </div>
                        </div>

                        @if (manualPlayerSource() === 'PROFESSIONAL' && isManualPlayerFiltersPanelExpanded()) {
                          <div class="mt-4 grid gap-3 rounded-2xl border border-neutral-200 bg-neutral-50 p-4 sm:grid-cols-2">
                            <label class="block">
                              <span class="mb-1 block text-sm font-medium text-neutral-700">Categoría</span>
                              <select
                                class="w-full rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                                [ngModel]="manualPlayerFilterCategory()"
                                (ngModelChange)="onManualPlayerFilterCategoryChange($event)"
                                name="manualPlayerFilterCategory"
                              >
                                <option value="">Todas las categorías</option>
                                @for (category of manualPlayerProfessionalCategoryOptions(); track category) {
                                  <option [value]="category">{{ category }}</option>
                                }
                              </select>
                            </label>

                            <label class="block">
                              <span class="mb-1 block text-sm font-medium text-neutral-700">Género</span>
                              <select
                                class="w-full rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                                [ngModel]="manualPlayerFilterGender()"
                                (ngModelChange)="onManualPlayerFilterGenderChange($event)"
                                name="manualPlayerFilterGender"
                              >
                                <option value="">Todos los géneros</option>
                                <option value="MALE">Masculino</option>
                                <option value="FEMALE">Femenino</option>
                                <option value="MIXED">Mixto</option>
                              </select>
                            </label>
                          </div>
                        }

                        @if (manualPlayerSearchError()) {
                          <div class="mt-3 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                            {{ manualPlayerSearchError() }}
                          </div>
                        }

                        @if (isSearchingPersons()) {
                          <div class="mt-4 rounded-2xl border border-dashed border-primary-200 bg-primary-50 px-4 py-3 text-sm text-primary-700">
                            Buscando coincidencias...
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
                                    @if (person.rankingPosition || person.ageCategory || person.clubName) {
                                      <p class="mt-1 text-xs text-neutral-500">
                                        {{ getManualPlayerMetaLabel(person) }}
                                      </p>
                                    }
                                  </div>
                                  <span class="rounded-full bg-neutral-100 px-3 py-1 text-xs font-semibold text-neutral-600">
                                    {{ manualPlayerSelectedPersonId() === person.id ? 'Seleccionado' : 'Seleccionar' }}
                                  </span>
                                </div>
                              </button>
                            }
                          </div>
                        } @else if (!isSearchingPersons() && (manualPlayerSearchQuery().trim().length >= 2 || hasActiveManualPlayerFilters()) && !manualPlayerSearchError()) {
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
                          <span class="mb-1 block text-sm font-medium text-neutral-700">Modalidad</span>
                          <select
                            class="w-full rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                            [ngModel]="manualPlayerGender()"
                            (ngModelChange)="manualPlayerGender.set($event)"
                            name="manualPlayerGender"
                          >
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
                          <select
                            class="w-full rounded-xl border border-neutral-300 px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                            [ngModel]="manualPlayerNationality()"
                            (ngModelChange)="manualPlayerNationality.set($event)"
                            name="manualPlayerNationality"
                          >
                            <option value="">Selecciona</option>
                            @for (nationality of nationalities(); track nationality.code) {
                              <option [value]="nationality.code">{{ nationality.name }}</option>
                            }
                          </select>
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

                  @if (manualPlayerSource() === 'MANUAL') {
                  <div class="mt-4 flex justify-end">
                    <button
                      type="button"
                      class="rounded-2xl bg-gradient-to-r from-primary-600 to-accent-600 px-5 py-3 font-semibold text-white shadow-lg shadow-primary-200 transition-all hover:scale-[1.01] hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-60"
                      [disabled]="isSubmittingManualPlayer()"
                      (click)="submitManualPlayer()"
                    >
                      {{ isSubmittingManualPlayer() ? 'Añadiendo jugador...' : 'Añadir jugador a la prueba' }}
                    </button>
                  </div>
                  }
                  }
                </div>
              } @else {
                <div class="mt-5 rounded-2xl border border-neutral-200 bg-neutral-50 p-4 text-sm text-neutral-700">
                  Estado actual del torneo: <span class="font-semibold text-neutral-900">{{ getStatusLabel(tournament()!.status) }}</span>
                </div>

                @if (!isProfileComplete()) {
                  <div class="mt-4 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
                    Debes completar tu perfil para inscribirte.
                    <a routerLink="/perfil" class="ml-2 font-semibold underline">Ir a completar perfil</a>
                  </div>
                }

                <div class="mt-4 grid gap-3 sm:grid-cols-2">
                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700">Categoría</span>
                    <select
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [value]="selectedInscriptionCategoryId() ?? ''"
                      (change)="onInscriptionCategoryChange($any($event.target).value)"
                    >
                      <option value="">Selecciona categoría</option>
                      @for (category of inscriptionCategories(); track category.categoryId) {
                        <option [value]="category.categoryId">{{ category.eventCategory }}</option>
                      }
                    </select>
                  </label>

                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700">Modalidad</span>
                    <select
                      class="w-full rounded-lg border border-neutral-300 px-3 py-2 outline-none focus:border-primary-500"
                      [value]="selectedInscriptionGender() ?? ''"
                      (change)="onInscriptionGenderChange($any($event.target).value)"
                    >
                      <option value="">Selecciona modalidad</option>
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
                  {{ isSubmittingInscription() ? 'Tramitando inscripción...' : 'Inscribirme' }}
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

              <div class="mt-6 rounded-3xl border border-neutral-200 bg-white p-5 sm:p-6">
                <div class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                  <div>
                    <div class="flex flex-wrap items-center gap-3">
                      <p class="text-xs font-semibold uppercase tracking-[0.2em] text-primary-600">Listado</p>
                      <button
                        type="button"
                        class="rounded-full border border-emerald-200 bg-white px-3 py-1.5 text-xs font-semibold text-emerald-700 transition-colors hover:border-emerald-300 hover:bg-emerald-50 hover:text-emerald-800"
                        (click)="toggleRegisteredPlayersPanel()"
                      >
                        {{ isRegisteredPlayersPanelExpanded() ? 'Ocultar' : 'Mostrar' }}
                      </button>
                    </div>
                    <h3 class="mt-2 text-xl font-bold text-neutral-900">Jugadores inscritos</h3>
                    <p class="mt-2 text-sm text-neutral-600">Consulta los inscritos del torneo, filtra por prueba y revisa los contadores por categoría y modalidad.</p>
                  </div>

                  <div class="flex flex-col gap-3 sm:flex-row sm:items-end">
                    <label class="block min-w-72">
                      <span class="mb-1 block text-sm font-medium text-neutral-700">Filtrar por prueba</span>
                      <select
                        class="w-full rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                        [disabled]="isLoadingTournamentInscriptions() || !isRegisteredPlayersPanelExpanded()"
                        [value]="selectedTournamentInscriptionEventId() ?? ''"
                        (change)="onTournamentInscriptionEventChange($any($event.target).value)"
                      >
                        <option value="">Todas las pruebas</option>
                        @for (event of tournamentInscriptionEvents(); track event.eventId) {
                          <option [value]="event.eventId">{{ event.eventName }}</option>
                        }
                      </select>
                    </label>
                  </div>
                </div>

                @if (isRegisteredPlayersPanelExpanded()) {
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
                          <span>Prueba / categoría</span>
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
                }
              </div>
            </section>
          }

          @if (activeSection() === 'stages') {
            <section class="mt-6 rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
              <div class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                <div>
                  <h2 class="text-xl font-bold text-neutral-900">Cuadros del torneo</h2>
                  <p class="mt-2 text-neutral-600">Visualiza las pruebas, cuadros y partidos generados.</p>
                </div>
              </div>

              <div class="mt-5 rounded-2xl border border-neutral-200 bg-neutral-50 p-4">
                <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                  <div>
                    <div class="flex items-center gap-3">
                      <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-600">Pistas</p>
                      <button
                        type="button"
                        class="rounded-full border border-neutral-300 bg-white px-3 py-1 text-xs font-semibold text-neutral-700 transition-colors hover:border-primary-300 hover:bg-primary-50 hover:text-primary-700"
                        (click)="toggleCourtsPanel()"
                      >
                        {{ isCourtsPanelExpanded() ? 'Ocultar' : 'Mostrar' }}
                      </button>
                    </div>
                    @if (isCourtsPanelExpanded()) {
                    <div class="mt-2 flex flex-wrap gap-2">
                      @if (isLoadingCourts()) {
                        <span class="rounded-full bg-white px-3 py-1.5 text-xs font-semibold text-neutral-600">Cargando pistas...</span>
                      } @else if (courts().length === 0) {
                        <span class="rounded-full bg-white px-3 py-1.5 text-xs font-semibold text-neutral-600">Sin pistas creadas</span>
                      } @else {
                        @for (court of courts(); track court.id) {
                          <button
                            type="button"
                            class="cursor-pointer rounded-full px-3 py-1.5 text-xs font-semibold ring-1 transition-all duration-150 hover:-translate-y-0.5 hover:shadow-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"
                            [class.bg-neutral-900]="selectedCourtId() === court.id"
                            [class.text-white]="selectedCourtId() === court.id"
                            [class.ring-neutral-900]="selectedCourtId() === court.id"
                            [class.hover:bg-neutral-800]="selectedCourtId() === court.id"
                            [class.bg-white]="selectedCourtId() !== court.id"
                            [class.text-neutral-700]="selectedCourtId() !== court.id"
                            [class.ring-neutral-200]="selectedCourtId() !== court.id"
                            [class.hover:bg-primary-50]="selectedCourtId() !== court.id"
                            [class.hover:text-primary-700]="selectedCourtId() !== court.id"
                            [class.hover:ring-primary-300]="selectedCourtId() !== court.id"
                            (click)="selectCourt(court)"
                          >
                            {{ court.name }}
                          </button>
                        }
                      }
                    </div>
                    }
                  </div>

                  @if (isCreator() && isCourtsPanelExpanded()) {
                    <div class="flex flex-col gap-2 sm:flex-row sm:items-end">
                      <label class="block min-w-60">
                        <span class="mb-1 block text-sm font-medium text-neutral-700">Nueva pista</span>
                        <input
                          type="text"
                          class="w-full rounded-xl border border-neutral-300 px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                          [ngModel]="newCourtName()"
                          (ngModelChange)="newCourtName.set($event)"
                          name="newCourtName"
                          placeholder="Pista 1"
                          (keyup.enter)="createCourt()"
                        />
                      </label>
                      <button
                        type="button"
                        class="rounded-2xl bg-neutral-900 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-neutral-800 disabled:cursor-not-allowed disabled:bg-neutral-300"
                        [disabled]="isCreatingCourt() || newCourtName().trim().length === 0"
                        (click)="createCourt()"
                      >
                        {{ isCreatingCourt() ? 'Creando...' : 'Añadir pista' }}
                      </button>
                    </div>
                  }
                </div>

                @if (isCourtsPanelExpanded() && courtMessage()) {
                  <div class="mt-3 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm text-emerald-700">
                    {{ courtMessage() }}
                  </div>
                }

                @if (isCourtsPanelExpanded() && courtError()) {
                  <div class="mt-3 rounded-xl border border-red-200 bg-red-50 px-4 py-2 text-sm text-red-700">
                    {{ courtError() }}
                  </div>
                }

                @if (isCreator() && isCourtsPanelExpanded() && selectedCourt()) {
                  <div class="mt-4 rounded-2xl border border-neutral-200 bg-white p-4">
                    <div class="flex flex-col gap-3 lg:flex-row lg:items-end">
                      <label class="block flex-1">
                        <span class="mb-1 block text-sm font-medium text-neutral-700">Nombre de la pista seleccionada</span>
                        <input
                          type="text"
                          class="w-full rounded-xl border border-neutral-300 px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                          [ngModel]="selectedCourtName()"
                          (ngModelChange)="selectedCourtName.set($event)"
                          name="selectedCourtName"
                          (keyup.enter)="updateSelectedCourt()"
                        />
                      </label>

                      <div class="flex gap-2">
                        <button
                          type="button"
                          class="rounded-xl border border-neutral-300 px-4 py-2 text-sm font-semibold text-neutral-700 transition-colors hover:bg-neutral-50 disabled:cursor-not-allowed disabled:opacity-60"
                          [disabled]="isUpdatingCourt() || selectedCourtName().trim().length === 0"
                          (click)="updateSelectedCourt()"
                        >
                          {{ isUpdatingCourt() ? 'Guardando...' : 'Guardar nombre' }}
                        </button>
                        <button
                          type="button"
                          class="rounded-xl border border-red-200 bg-red-50 px-4 py-2 text-sm font-semibold text-red-700 transition-colors hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-60"
                          [disabled]="isDeletingCourt()"
                          (click)="deleteSelectedCourt()"
                        >
                          {{ isDeletingCourt() ? 'Eliminando...' : 'Eliminar' }}
                        </button>
                        <button
                          type="button"
                          class="rounded-xl border border-neutral-200 px-4 py-2 text-sm font-semibold text-neutral-600 transition-colors hover:bg-neutral-50"
                          (click)="clearSelectedCourt()"
                        >
                          Cancelar
                        </button>
                      </div>
                    </div>
                  </div>
                }
              </div>

              @if (isCreator()) {
              <div class="mt-5 rounded-2xl border border-neutral-200 bg-white p-4">
                <div class="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
                  <div>
                    <div class="flex items-center gap-3">
                      <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-600">Programación de partidos</p>
                      <button
                        type="button"
                        class="rounded-full border border-neutral-300 bg-white px-3 py-1 text-xs font-semibold text-neutral-700 transition-colors hover:border-primary-300 hover:bg-primary-50 hover:text-primary-700"
                        (click)="toggleMatchSchedulePanel()"
                      >
                        {{ isMatchSchedulePanelExpanded() ? 'Ocultar' : 'Mostrar' }}
                      </button>
                    </div>
                    @if (isMatchSchedulePanelExpanded()) {
                      <p class="mt-1 text-sm text-neutral-600">Asigna pista, hora exacta o no antes de una hora para cada partido generado.</p>
                    }
                  </div>
                  <span class="rounded-full bg-neutral-100 px-3 py-1 text-xs font-semibold text-neutral-600">
                    {{ filteredTournamentMatchScheduleRows().length }} de {{ tournamentMatchScheduleRows().length }} partidos
                  </span>
                </div>

                @if (isMatchSchedulePanelExpanded() && tournamentMatchScheduleRows().length === 0) {
                  <div class="mt-4 rounded-xl border border-dashed border-neutral-300 bg-neutral-50 p-4 text-sm text-neutral-600">
                    Genera cuadros para ver aquí los partidos programables.
                  </div>
                } @else if (isMatchSchedulePanelExpanded()) {
                  <div class="mt-4 grid gap-3 rounded-xl border border-neutral-200 bg-neutral-50 p-4 sm:grid-cols-2 lg:grid-cols-[1.3fr_0.8fr_0.9fr_1fr_0.8fr_auto]">
                    <label class="text-xs font-semibold uppercase tracking-widest text-neutral-600">
                      Prueba
                      <select
                        class="mt-1 w-full rounded-lg border border-neutral-300 bg-white px-3 py-2 text-sm font-normal normal-case tracking-normal text-neutral-800 focus:border-primary-500 focus:outline-none"
                        [ngModel]="matchScheduleEventFilter()"
                        (ngModelChange)="matchScheduleEventFilter.set($event)"
                        name="matchScheduleEventFilter"
                      >
                        <option value="">Todos</option>
                        @for (eventLabel of matchScheduleEventFilterOptions(); track eventLabel) {
                          <option [value]="eventLabel">{{ eventLabel }}</option>
                        }
                      </select>
                    </label>

                    <label class="text-xs font-semibold uppercase tracking-widest text-neutral-600">
                      Ronda
                      <select
                        class="mt-1 w-full rounded-lg border border-neutral-300 bg-white px-3 py-2 text-sm font-normal normal-case tracking-normal text-neutral-800 focus:border-primary-500 focus:outline-none"
                        [ngModel]="matchScheduleRoundFilter()"
                        (ngModelChange)="matchScheduleRoundFilter.set($event)"
                        name="matchScheduleRoundFilter"
                      >
                        <option value="">Todas</option>
                        @for (round of matchScheduleRoundFilterOptions(); track round) {
                          <option [value]="round">Ronda {{ round }}</option>
                        }
                      </select>
                    </label>

                    <label class="text-xs font-semibold uppercase tracking-widest text-neutral-600">
                      Fecha
                      <input
                        type="date"
                        class="mt-1 w-full rounded-lg border border-neutral-300 bg-white px-3 py-2 text-sm font-normal normal-case tracking-normal text-neutral-800 focus:border-primary-500 focus:outline-none"
                        [ngModel]="matchScheduleDateFilter()"
                        (ngModelChange)="matchScheduleDateFilter.set($event)"
                        name="matchScheduleDateFilter"
                      />
                    </label>

                    <label class="text-xs font-semibold uppercase tracking-widest text-neutral-600">
                      Pista
                      <select
                        class="mt-1 w-full rounded-lg border border-neutral-300 bg-white px-3 py-2 text-sm font-normal normal-case tracking-normal text-neutral-800 focus:border-primary-500 focus:outline-none"
                        [ngModel]="matchScheduleCourtFilter()"
                        (ngModelChange)="matchScheduleCourtFilter.set($event)"
                        name="matchScheduleCourtFilter"
                      >
                        <option value="">Todas</option>
                        @for (court of courts(); track court.id) {
                          <option [value]="court.id">{{ court.name }}</option>
                        }
                      </select>
                    </label>

                    <label class="text-xs font-semibold uppercase tracking-widest text-neutral-600">
                      Tipo
                      <select
                        class="mt-1 w-full rounded-lg border border-neutral-300 bg-white px-3 py-2 text-sm font-normal normal-case tracking-normal text-neutral-800 focus:border-primary-500 focus:outline-none"
                        [ngModel]="matchScheduleProfessionalFilter()"
                        (ngModelChange)="matchScheduleProfessionalFilter.set($event)"
                        name="matchScheduleProfessionalFilter"
                      >
                        <option value="">Todos</option>
                        <option value="PRO">PRO</option>
                      </select>
                    </label>

                    <div class="flex items-end">
                      <button
                        type="button"
                        class="w-full rounded-lg border border-neutral-300 bg-white px-3 py-2 text-sm font-semibold text-neutral-700 transition-colors hover:border-primary-300 hover:bg-primary-50 hover:text-primary-700"
                        (click)="clearMatchScheduleFilters()"
                      >
                        Limpiar
                      </button>
                    </div>
                  </div>

                  @if (filteredTournamentMatchScheduleRows().length === 0) {
                    <div class="mt-4 rounded-xl border border-dashed border-neutral-300 bg-neutral-50 p-4 text-sm text-neutral-600">
                      No hay partidos que coincidan con los filtros seleccionados.
                    </div>
                  } @else {
                  <div class="mt-4 overflow-x-auto">
                    <table class="w-full min-w-[920px] text-sm">
                      <thead class="bg-neutral-100">
                        <tr>
                          <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">
                            <button type="button" class="inline-flex items-center gap-1 transition-colors hover:text-primary-700" (click)="setMatchScheduleSort('event')">
                              Prueba <span>{{ getMatchScheduleSortIndicator('event') }}</span>
                            </button>
                          </th>
                          <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">
                            <button type="button" class="inline-flex items-center gap-1 transition-colors hover:text-primary-700" (click)="setMatchScheduleSort('round')">
                              Partido <span>{{ getMatchScheduleSortIndicator('round') }}</span>
                            </button>
                          </th>
                          <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">Tipo</th>
                          <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">
                            <button type="button" class="inline-flex items-center gap-1 transition-colors hover:text-primary-700" (click)="setMatchScheduleSort('scheduledAt')">
                              Inicio <span>{{ getMatchScheduleSortIndicator('scheduledAt') }}</span>
                            </button>
                          </th>
                          <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">
                            <button type="button" class="inline-flex items-center gap-1 transition-colors hover:text-primary-700" (click)="setMatchScheduleSort('court')">
                              Pista <span>{{ getMatchScheduleSortIndicator('court') }}</span>
                            </button>
                          </th>
                          <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">Cascada</th>
                          <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">Acción</th>
                        </tr>
                      </thead>
                      <tbody>
                        @for (row of filteredTournamentMatchScheduleRows(); track row.match.id) {
                          <tr class="border-b border-neutral-200 align-top">
                            <td class="px-3 py-3">
                              <p class="font-medium text-neutral-900">{{ row.eventLabel }}</p>
                              <p class="mt-1 text-xs text-neutral-500">{{ row.drawLabel }}</p>
                            </td>
                            <td class="px-3 py-3">
                              <p class="font-medium text-neutral-900">Ronda {{ row.match.roundNumber }}</p>
                              <p class="mt-1 text-xs text-neutral-600">{{ row.firstPlayerName }} vs {{ row.secondPlayerName }}</p>
                            </td>
                            <td class="px-3 py-3">
                              <select
                                class="w-full rounded-lg border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                                [ngModel]="getMatchScheduleDraft(row.match).scheduleTimeType"
                                (ngModelChange)="updateMatchScheduleType(row.match, $event)"
                                [name]="'scheduleType-' + row.match.id"
                              >
                                <option value="EXACT">A esta hora</option>
                                <option value="NOT_BEFORE">No antes de</option>
                              </select>
                            </td>
                            <td class="px-3 py-3">
                              <input
                                type="datetime-local"
                                class="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                                [ngModel]="getMatchScheduleDraft(row.match).scheduledAt"
                                (ngModelChange)="updateMatchScheduleDate(row.match, $event)"
                                [name]="'scheduledAt-' + row.match.id"
                              />
                            </td>
                            <td class="px-3 py-3">
                              <select
                                class="w-full rounded-lg border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                                [ngModel]="getMatchScheduleDraft(row.match).courtId"
                                (ngModelChange)="updateMatchScheduleCourt(row.match, $event)"
                                [name]="'court-' + row.match.id"
                              >
                                <option value="">Selecciona pista</option>
                                @for (court of courts(); track court.id) {
                                  <option [value]="court.id">{{ court.name }}</option>
                                }
                              </select>
                            </td>
                            <td class="px-3 py-3">
                              <label class="inline-flex items-center gap-2 text-xs font-semibold text-neutral-700">
                                <input
                                  type="checkbox"
                                  class="h-4 w-4 rounded border-neutral-300 text-primary-600 focus:ring-primary-500"
                                  [ngModel]="getMatchScheduleDraft(row.match).cascade"
                                  (ngModelChange)="updateMatchScheduleCascade(row.match, $event)"
                                  [name]="'cascade-' + row.match.id"
                                />
                                Replanificar siguientes
                              </label>
                            </td>
                            <td class="px-3 py-3">
                              <button
                                type="button"
                                class="rounded-xl bg-neutral-900 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-neutral-800 disabled:cursor-not-allowed disabled:bg-neutral-300"
                                [disabled]="!canSaveMatchSchedule(row.match)"
                                (click)="saveMatchSchedule(row.match)"
                              >
                                {{ savingScheduleMatchId() === row.match.id ? 'Guardando...' : 'Guardar' }}
                              </button>
                            </td>
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>
                  }
                }
              </div>
              }

              @if (tournament()?.events && (tournament()!.events!.length > 0)) {
                <div class="mt-5 rounded-2xl border border-neutral-200 bg-white p-4">
                  <div class="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
                    <div>
                      <div class="flex flex-wrap items-center gap-3">
                        <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-600">Pruebas y cuadros</p>
                        <button
                          type="button"
                          class="rounded-full border border-emerald-200 bg-white px-3 py-1.5 text-xs font-semibold text-emerald-700 transition-colors hover:border-emerald-300 hover:bg-emerald-50 hover:text-emerald-800"
                          (click)="toggleEventsDrawsPanel()"
                        >
                          {{ isEventsDrawsPanelExpanded() ? 'Ocultar' : 'Mostrar' }}
                        </button>
                      </div>
                    </div>
                    <div class="flex flex-wrap items-center gap-2">
                      <span class="rounded-full bg-neutral-100 px-3 py-1 text-xs font-semibold text-neutral-600">
                        {{ tournament()!.events!.length }} {{ (tournament()!.events!.length || 0) === 1 ? 'prueba' : 'pruebas' }}
                      </span>
                    </div>
                  </div>

                  @if (isEventsDrawsPanelExpanded()) {
                  <div class="mt-5 space-y-4">
                  @for (event of tournament()!.events!; track event.eventId) {
                    <div class="rounded-2xl border border-neutral-200 bg-neutral-50 p-5">
                      <div class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                        <div>
                          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-neutral-500">Prueba</p>
                          <h4 class="mt-1 text-lg font-semibold text-neutral-900">
                            {{ getCategoryLabel(event.categoryId) }} - {{ getGenderLabelForString(event.gender) }}
                          </h4>
                        </div>
                        <span class="rounded-full bg-white px-3 py-1 text-xs font-semibold text-neutral-600 ring-1 ring-neutral-200">
                          {{ event.stages?.length || 0 }} {{ (event.stages?.length || 0) === 1 ? 'cuadro' : 'cuadros' }}
                        </span>
                      </div>

                      <div class="mt-4 rounded-xl border border-neutral-200 bg-white p-4">
                        <div class="mb-3 flex items-center justify-between gap-3">
                          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-primary-600">Cuadros</p>
                        </div>
                        
                        @if (event.stages && event.stages.length > 0) {
                          <app-stages
                            [stagesInput]="event.stages"
                            [tournamentIdInput]="tournament()!.id"
                            [participantNamesInput]="participantNamesByInscriptionId()"
                            [participantOrderInput]="participantOrderByInscriptionId()"
                            [courtsInput]="courts()"
                            [canManageInput]="isCreator()"
                            [tournamentNameInput]="tournament()!.formalName"
                            [categoryNameInput]="getCategoryLabel(event.categoryId) + ' - ' + getGenderLabelForString(event.gender)"
                            [generatingDrawsForStageIdInput]="generatingDrawsStageId()"
                            [drawGenerationFeedbackInput]="drawGenerationFeedbackByStageId()"
                            (generateDraws)="onGenerateDraws($event, event.eventId!)"
                            (matchSelected)="onMatchSelected($event)"
                            (matchResultSaved)="onMatchResultSaved($event)"
                            (matchScheduleSaved)="onMatchScheduleSaved($event)"
                          ></app-stages>
                        } @else {
                          <div class="rounded-lg border border-neutral-200 bg-neutral-50 p-4 text-sm text-neutral-600">
                            Sin cuadros generados aún
                          </div>
                        }
                      </div>
                    </div>
                  }
                  </div>
                  }
                </div>
              } @else {
                <div class="mt-5 rounded-2xl border border-neutral-200 bg-white p-6 text-center text-neutral-600">
                  No hay pruebas para mostrar
                </div>
              }
            </section>
          }
        }
      </div>
    </section>
  `
})
export class TournamentDetailComponent implements OnInit, OnDestroy, AfterViewInit {
  private readonly route = inject(ActivatedRoute);
  private readonly tournamentService = inject(TournamentService);
  private readonly tournamentLiveUpdatesService = inject(TournamentLiveUpdatesService);
  private readonly memberService = inject(MemberService);
  private readonly authService = inject(AuthService);
  private readonly personService = inject(PersonService);
  private readonly proPlayerService = inject(ProPlayerService);
  private readonly referenceDataService = inject(ReferenceDataService);

  private mapInstance?: any;

  readonly eventGenderOptions: TournamentEventGender[] = ['MALE', 'FEMALE', 'MIXED'];
  readonly stageOptions: TournamentStageType[] = ['SINGLE_ELIMINATION', 'ROUND_ROBIN', 'DOUBLE_ELIMINATION', 'CONSOLATION'];
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
  readonly nationalities = signal<NationalityOption[]>([]);
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
  readonly manualPlayerSearchResults = signal<ManualPlayerLookupResult[]>([]);
  readonly manualPlayerSelectedPersonId = signal<string>('');
  readonly manualPlayerSearchError = signal<string | null>(null);
  readonly manualPlayerFilterGender = signal<string>('');
  readonly manualPlayerFilterCategory = signal<string>('');
  readonly manualPlayerFirstName = signal<string>('');
  readonly manualPlayerLastName = signal<string>('');
  readonly manualPlayerGender = signal<string>('MALE');
  readonly manualPlayerBirthDate = signal<string>('');
  readonly manualPlayerNationality = signal<string>('');
  readonly manualPlayerTennisId = signal<string>('');
  readonly manualPlayerError = signal<string | null>(null);
  readonly manualPlayerSuccess = signal<string | null>(null);
  readonly isSearchingPersons = signal(false);
  readonly isSubmittingManualPlayer = signal(false);
  readonly generatingDrawsStageId = signal<string | null>(null);
  readonly drawGenerationFeedbackByStageId = signal<Record<string, DrawGenerationFeedback>>({});
  readonly courts = signal<CourtResponse[]>([]);
  readonly isLoadingCourts = signal(false);
  readonly isCourtsPanelExpanded = signal(false);
  readonly isMatchSchedulePanelExpanded = signal(false);
  readonly isManualPlayerPanelExpanded = signal(false);
  readonly isManualPlayerFiltersPanelExpanded = signal(false);
  readonly isRegisteredPlayersPanelExpanded = signal(false);
  readonly isEventsDrawsPanelExpanded = signal(false);
  readonly isCreatingCourt = signal(false);
  readonly isUpdatingCourt = signal(false);
  readonly isDeletingCourt = signal(false);
  readonly isExportingTournamentPdf = signal(false);
  readonly newCourtName = signal('');
  readonly selectedCourtId = signal<string | null>(null);
  readonly selectedCourtName = signal('');
  readonly courtMessage = signal<string | null>(null);
  readonly courtError = signal<string | null>(null);
  readonly savingResultMatchIds = signal<Set<string>>(new Set());
  readonly savingResultMatchId = computed(() => Array.from(this.savingResultMatchIds())[0] ?? null);
  readonly savingScheduleMatchId = signal<string | null>(null);
  readonly savingScheduleCascadeMatchId = signal<string | null>(null);
  readonly hasPendingLiveRefresh = signal(false);
  readonly matchScheduleDrafts = signal<Record<string, MatchScheduleDraft>>({});
  readonly matchScheduleEventFilter = signal('');
  readonly matchScheduleRoundFilter = signal('');
  readonly matchScheduleDateFilter = signal('');
  readonly matchScheduleCourtFilter = signal('');
  readonly matchScheduleProfessionalFilter = signal('');
  readonly matchScheduleSortField = signal<MatchScheduleSortField>('event');
  readonly matchScheduleSortDirection = signal<MatchScheduleSortDirection>('asc');
  readonly manualPlayerSourceOptions: Array<{ value: ManualParticipantSource; label: string; description: string }> = [
    {
      value: 'EXISTING_PERSON',
      label: 'Jugador existente',
      description: 'Busca por nombre, apellido o licencia y selecciónalo directamente.'
    },
    {
      value: 'MANUAL',
      label: 'Jugador inventado',
      description: 'Rellena los datos básicos para crear un participante temporal o manual.'
    },
    {
      value: 'PROFESSIONAL',
      label: 'Jugador profesional',
      description: 'Busca en la base de profesionales cargada y selecciona al jugador.'
    }
  ];
  private readonly manualPlayerSearchResultLimit = 10;
  private manualPlayerSearchDebounceHandle: ReturnType<typeof setTimeout> | null = null;
  private manualPlayerSearchRequestId = 0;
  private liveUpdatesSubscription: Subscription | null = null;

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
    const groupedEvents = new Map<number, { eventIdsByGender: Map<TournamentEventGender, string>; genders: Set<TournamentEventGender> }>();
    const events = this.tournament()?.events ?? [];

    events.forEach(event => {
      const normalizedGender = event.gender.toUpperCase() as TournamentEventGender;
      if (!this.eventGenderOptions.includes(normalizedGender)) {
        return;
      }

      const currentEntry = groupedEvents.get(event.categoryId) ?? {
        eventIdsByGender: new Map<TournamentEventGender, string>(),
        genders: new Set<TournamentEventGender>()
      };
      currentEntry.eventIdsByGender.set(normalizedGender, event.eventId);
      currentEntry.genders.add(normalizedGender);
      groupedEvents.set(event.categoryId, currentEntry);
    });

    return Array.from(groupedEvents.entries()).map(([categoryId, entry]) => ({
      categoryId,
      eventCategory: this.getEventLabelById(categoryId),
      eventsByGender: Array.from(entry.eventIdsByGender.entries()).map(([gender, eventId]) => ({ gender, eventId })),
      genders: Array.from(entry.genders),
      stages: []
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

  readonly manualPlayerProfessionalCategoryOptions = computed<string[]>(() =>
    Array.from(new Set(this.manualPlayerEventOptions().map(event => event.category))).sort((a, b) => a.localeCompare(b))
  );

  readonly tournamentInscriptionCategoryCounts = computed<TournamentInscriptionCategoryCount[]>(() =>
    this.tournamentInscriptions()?.categoryCounts ?? []
  );

  readonly tournamentInscriptionPlayers = computed(() => this.tournamentInscriptions()?.inscriptions ?? []);
  readonly isProfessionalTournament = computed(() => {
    if (this.tournament()?.professionalTournament) {
      return true;
    }

    if (this.tournamentInscriptions()?.selectedEventId) {
      return false;
    }

    const players = this.tournamentInscriptionPlayers();
    return players.length > 0 && players.every(player => player.playerSource === 'PROFESSIONAL');
  });

  readonly participantNamesByInscriptionId = computed<Record<string, string>>(() =>
    this.tournamentInscriptionPlayers().reduce<Record<string, string>>((accumulator, player) => {
      accumulator[player.inscriptionId] = [player.firstName, player.lastName].filter(Boolean).join(' ').trim();
      return accumulator;
    }, {})
  );

  readonly participantOrderByInscriptionId = computed<Record<string, number>>(() =>
    this.tournamentInscriptionPlayers().reduce<Record<string, number>>((accumulator, player, index) => {
      accumulator[player.inscriptionId] = index;
      return accumulator;
    }, {})
  );

  readonly hasTournamentInscriptionsResults = computed(() => this.tournamentInscriptionPlayers().length > 0);

  readonly selectedCourt = computed<CourtResponse | null>(() =>
    this.courts().find(court => court.id === this.selectedCourtId()) ?? null
  );

  readonly tournamentMatchScheduleRows = computed<TournamentMatchScheduleRow[]>(() => {
    const rows: TournamentMatchScheduleRow[] = [];

    for (const event of this.tournament()?.events ?? []) {
      const eventLabel = `${this.getCategoryLabel(event.categoryId)} - ${this.getGenderLabelForString(event.gender)}`;

      for (const stage of event.stages ?? []) {
        for (const draw of stage.draws ?? []) {
          for (const match of draw.matches ?? []) {
            rows.push({
              match,
              eventLabel,
              drawLabel: draw.label || stage.description || stage.stageType,
              firstPlayerName: this.getScheduleParticipantName(match.firstInscriptionId),
              secondPlayerName: this.getScheduleParticipantName(match.secondInscriptionId)
            });
          }
        }
      }
    }

    return rows.sort((left, right) =>
      left.eventLabel.localeCompare(right.eventLabel) ||
      (left.match.roundNumber ?? 0) - (right.match.roundNumber ?? 0) ||
      left.drawLabel.localeCompare(right.drawLabel) ||
      left.match.id.localeCompare(right.match.id)
    );
  });

  readonly matchScheduleEventFilterOptions = computed<string[]>(() =>
    Array.from(new Set(this.tournamentMatchScheduleRows().map(row => row.eventLabel)))
      .sort((left, right) => left.localeCompare(right))
  );

  readonly matchScheduleRoundFilterOptions = computed<number[]>(() =>
    Array.from(new Set(this.tournamentMatchScheduleRows().map(row => row.match.roundNumber ?? 0)))
      .filter(round => round > 0)
      .sort((left, right) => left - right)
  );

  readonly filteredTournamentMatchScheduleRows = computed<TournamentMatchScheduleRow[]>(() => {
    const eventFilter = this.matchScheduleEventFilter();
    const roundFilter = this.matchScheduleRoundFilter();
    const dateFilter = this.matchScheduleDateFilter();
    const courtFilter = this.matchScheduleCourtFilter();
    const professionalFilter = this.matchScheduleProfessionalFilter();

    const rows = this.tournamentMatchScheduleRows().filter(row => {
      if (eventFilter && row.eventLabel !== eventFilter) {
        return false;
      }

      if (roundFilter && String(row.match.roundNumber ?? '') !== roundFilter) {
        return false;
      }

      if (dateFilter && this.getSavedMatchScheduleDateValue(row) !== dateFilter) {
        return false;
      }

      if (courtFilter && this.getSavedMatchScheduleCourtId(row) !== courtFilter) {
        return false;
      }

      if (professionalFilter === 'PRO' && !row.match.professionalMatch) {
        return false;
      }

      return true;
    });

    return rows.sort((left, right) => this.compareMatchScheduleRows(left, right));
  });

  readonly getSurfaceLabel = getTournamentSurfaceCategoryLabel;
  readonly getGenderLabel = getTournamentEventGenderLabel;
  readonly getStageLabel = getTournamentStageTypeLabel;

  constructor() {
    this.loadEventCatalog();
    this.loadNationalities();
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
    this.startTournamentLiveUpdates(tournamentId);
  }

  ngAfterViewInit(): void {
    this.tryInitMap();
  }

  ngOnDestroy(): void {
    this.liveUpdatesSubscription?.unsubscribe();
    this.cancelManualPlayerSearch();
    this.mapInstance?.remove();
  }

  setActiveSection(section: TournamentDetailSection): void {
    this.activeSection.set(section);
    this.actionMessage.set(null);
    this.actionError.set(null);

    if (section === 'overview') {
      setTimeout(() => this.tryInitMap(), 0);
    }

    if (section === 'inscriptions' && !this.tournamentInscriptions() && !this.isLoadingTournamentInscriptions()) {
      this.loadTournamentInscriptions();
    }
  }

  exportTournamentPdf(): void {
    const currentTournament = this.tournament();
    if (!currentTournament || this.isExportingTournamentPdf()) {
      return;
    }

    this.isExportingTournamentPdf.set(true);

    this.tournamentService.exportTournamentPdf(currentTournament.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = this.sanitizeFilename(currentTournament.formalName) + '.pdf';
        link.click();
        window.URL.revokeObjectURL(url);
        this.isExportingTournamentPdf.set(false);
        this.actionMessage.set('PDF exportado correctamente.');
      },
      error: (error) => {
        this.isExportingTournamentPdf.set(false);
        this.actionError.set(getApiErrorMessage(error, 'No se pudo exportar el PDF del torneo.'));
      }
    });
  }

  private sanitizeFilename(name: string): string {
    if (!name) return 'torneo';
    return name
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-zA-Z0-9_\- ]/g, '')
      .replace(/\s+/g, '_')
      .toLowerCase();
  }

  onManualPlayerSourceChange(source: ManualParticipantSource): void {
    this.manualPlayerSource.set(source);
    this.manualPlayerError.set(null);
    this.manualPlayerSuccess.set(null);
    this.manualPlayerSearchResults.set([]);
    this.manualPlayerSelectedPersonId.set('');
    this.manualPlayerSearchRequestId += 1;
    this.isSearchingPersons.set(false);
    this.cancelManualPlayerSearch();

    if (source !== 'MANUAL') {
      this.manualPlayerFirstName.set('');
      this.manualPlayerLastName.set('');
      this.manualPlayerGender.set('');
      this.manualPlayerBirthDate.set('');
      this.manualPlayerNationality.set('');
      this.manualPlayerTennisId.set('');

      if (this.manualPlayerSearchQuery().trim().length >= 2) {
        this.scheduleManualPlayerSearch();
      }

      if (source !== 'PROFESSIONAL') {
        this.manualPlayerFilterGender.set('');
        this.manualPlayerFilterCategory.set('');
        this.isManualPlayerFiltersPanelExpanded.set(false);
      }
    } else {
      this.manualPlayerSearchQuery.set('');
      this.manualPlayerFilterGender.set('');
      this.manualPlayerFilterCategory.set('');
      this.isManualPlayerFiltersPanelExpanded.set(false);
    }
  }

  toggleManualPlayerFiltersPanel(): void {
    this.isManualPlayerFiltersPanelExpanded.update(expanded => !expanded);
  }

  onManualPlayerFilterGenderChange(gender: string): void {
    this.manualPlayerFilterGender.set(gender);
    this.onManualPlayerFiltersChange();
  }

  onManualPlayerFilterCategoryChange(category: string): void {
    this.manualPlayerFilterCategory.set(category);
    this.onManualPlayerFiltersChange();
  }

  hasActiveManualPlayerFilters(): boolean {
    return !!this.manualPlayerFilterGender().trim() || !!this.manualPlayerFilterCategory().trim();
  }

  private onManualPlayerFiltersChange(): void {
    this.manualPlayerSearchError.set(null);
    this.manualPlayerSuccess.set(null);
    this.manualPlayerSelectedPersonId.set('');
    this.scheduleManualPlayerSearch();
  }

  toggleCatalogEvent(catalogEvent: TournamentEventCatalogItem, checked: boolean): void {
    this.eventsErrorMessage.set(null);

    if (checked) {
      if (this.isEventSelected(catalogEvent.id)) {
        return;
      }

      this.selectedEvents.update(events => [
        ...events,
        {
          categoryId: catalogEvent.id,
          eventCategory: catalogEvent.category,
          eventsByGender: [],
          genders: [],
          stages: [
            {
              stageType: 'SINGLE_ELIMINATION'
            }
          ]
        }
      ]);
      return;
    }

    this.selectedEvents.update(events => events.filter(event => event.categoryId !== catalogEvent.id));
  }

  toggleEventGender(categoryId: number, gender: TournamentEventGender, checked: boolean): void {
    this.eventsErrorMessage.set(null);

    this.selectedEvents.update(events =>
      events.map(event => {
        if (event.categoryId !== categoryId) {
          return event;
        }

        const nextGenders = checked
          ? Array.from(new Set([...event.genders, gender]))
          : event.genders.filter(currentGender => currentGender !== gender);

        const genderSet = new Set(nextGenders);
        const nextEventsByGender = checked
          ? event.eventsByGender.some(eg => eg.gender === gender)
            ? event.eventsByGender
            : [...event.eventsByGender, { gender, eventId: null }]
          : event.eventsByGender.filter(eg => eg.gender !== gender);

        return {
          ...event,
          genders: nextGenders,
          eventsByGender: nextEventsByGender
        };
      })
    );
  }

  addEventStage(categoryId: number): void {
    this.eventsErrorMessage.set(null);

    this.selectedEvents.update(events =>
      events.map(event => {
        if (event.categoryId !== categoryId) {
          return event;
        }
        const newStageType: TournamentStageType = isConsolationDisabled(event.stages.map(s => s.stageType))
          ? 'SINGLE_ELIMINATION'
          : 'SINGLE_ELIMINATION';
        return {
          ...event,
          stages: [...event.stages, { stageType: newStageType }]
        };
      })
    );
  }

  removeEventStage(categoryId: number, stageIndex: number): void {
    this.eventsErrorMessage.set(null);

    this.selectedEvents.update(events =>
      events.map(event => {
        if (event.categoryId !== categoryId || event.stages.length <= 1) {
          return event;
        }

        const removedType = event.stages[stageIndex].stageType;
        const nextIndex = stageIndex + 1;
        const hasConsolationAfter = removedType === 'SINGLE_ELIMINATION'
          && nextIndex < event.stages.length
          && event.stages[nextIndex].stageType === 'CONSOLATION';

        if (hasConsolationAfter) {
          const confirmed = window.confirm(
            'Al eliminar el cuadro de eliminación simple, el cuadro de consolación también será eliminada. ¿Deseas continuar?'
          );
          if (!confirmed) {
            return event;
          }
          const newStages = event.stages.filter((_, index) => index !== stageIndex && index !== nextIndex);
          return {
            ...event,
            stages: newStages.length > 0 ? newStages : [{ stageType: 'SINGLE_ELIMINATION' }]
          };
        }

        return {
          ...event,
          stages: event.stages.filter((_, index) => index !== stageIndex)
        };
      })
    );
  }

  updateEventStageType(categoryId: number, stageIndex: number, stageType: TournamentStageType): void {
    this.selectedEvents.update(events =>
      events.map(event => {
        if (event.categoryId !== categoryId) {
          return event;
        }

        const newStages = event.stages.map((stage, index) => (index === stageIndex ? { stageType } : stage));
        const stageTypes = newStages.map(s => s.stageType);
        const errors = validateStageSequence(stageTypes);

        if (errors.length > 0) {
          this.eventsErrorMessage.set(errors[0].message);
          return event;
        }

        this.eventsErrorMessage.set(null);
        return {
          ...event,
          stages: newStages
        };
      })
    );
  }

  getAvailableStageOptions(stages: TournamentEventStageSelection[], currentIndex: number): TournamentStageType[] {
    return getAvailableStageOptions(stages.map(s => s.stageType), currentIndex);
  }

  hasEventGender(categoryId: number, gender: TournamentEventGender): boolean {
    return this.selectedEvents()
      .find(event => event.categoryId === categoryId)
      ?.genders?.includes(gender) ?? false;
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
    if (!this.isCreator() || !tournament || !selectedStatus || this.isUpdatingStatus()) {
      return false;
    }

    return this.allowedStatusTransitions().includes(selectedStatus);
  }

  updateTournamentStatus(): void {
    if (!this.isCreator()) {
      this.actionError.set('Solo el administrador del torneo puede cambiar el estado.');
      return;
    }

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
      error: (error) => {
        this.actionError.set(getApiErrorMessage(error, 'No se pudo actualizar el estado del torneo.'));
        this.isUpdatingStatus.set(false);
      }
    });
  }

  saveTournamentEvents(): void {
    if (!this.isCreator()) {
      this.eventsErrorMessage.set('Solo el administrador del torneo puede guardar pruebas.');
      return;
    }

    const currentTournament = this.tournament();
    const selectedEvents = this.selectedEvents();

    if (!currentTournament) {
      return;
    }

    if (selectedEvents.some(event => event.genders.length === 0)) {
      this.eventsErrorMessage.set('Debes seleccionar al menos una modalidad en cada prueba antes de guardar.');
      return;
    }

    if (selectedEvents.some(event => event.stages.length === 0)) {
      this.eventsErrorMessage.set('Debes definir al menos un cuadro en cada prueba antes de guardar.');
      return;
    }

    for (const event of selectedEvents) {
      const stageTypes = event.stages.map(s => s.stageType);
      const errors = validateStageSequence(stageTypes);
      if (errors.length > 0) {
        this.eventsErrorMessage.set(`Prueba "${event.eventCategory}": ${errors[0].message}`);
        return;
      }
    }

    const payload: TournamentEventsConfigRequest = {
      events: selectedEvents.flatMap(event =>
        event.genders.map(gender => {
          const eventEntry = event.eventsByGender.find(eg => eg.gender === gender);
          return {
            id: eventEntry?.eventId ?? null,
            categoryId: event.categoryId,
            gender,
            stages: event.stages.map(stage => stage.stageType)
          };
        })
      )
    };

    this.isSavingEvents.set(true);
    this.eventsErrorMessage.set(null);
    this.eventsSuccessMessage.set(null);

    this.tournamentService.saveTournamentEvents(currentTournament.id, payload).subscribe({
      next: updatedTournament => {
        const updatedEvents = updatedTournament.events ?? [];
        this.tournament.set(updatedTournament);
        this.hydrateSelectedEventsFromTournament(updatedEvents);
        this.initializeInscriptionSelection();
        this.selectedTournamentInscriptionEventId.set(null);
        this.tournamentInscriptions.set(null);
        this.syncManualPlayerEventSelection(updatedEvents);
        this.loadTournamentInscriptions();
        this.isSavingEvents.set(false);
        this.eventsSuccessMessage.set('Pruebas del torneo guardadas correctamente.');
      },
      error: (error) => {
        this.isSavingEvents.set(false);
        this.eventsErrorMessage.set(getApiErrorMessage(error, 'No se pudieron guardar las pruebas del torneo.'));
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
      this.actionError.set('Selecciona categoría y modalidad antes de solicitar la inscripción.');
      return;
    }

    if (!this.canRequestInscription()) {
      this.actionError.set('No cumples los requisitos para inscribirte en esta prueba.');
      return;
    }

    this.isSubmittingInscription.set(true);
    this.actionError.set(null);
    this.actionMessage.set(null);

    this.tournamentService.requestInscription(currentTournament.id, eventId, { categoryId, partnerId: null }).subscribe({
      next: () => {
        this.isSubmittingInscription.set(false);
        this.actionMessage.set('Inscripción realizada correctamente para la prueba seleccionada.');
      },
      error: (error) => {
        this.isSubmittingInscription.set(false);
        this.actionError.set(getApiErrorMessage(error, 'No se pudo completar la inscripción para esta prueba.'));
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
    this.searchManualPlayerCandidates();
  }

  searchManualPlayerCandidates(): void {
    const query = this.manualPlayerSearchQuery().trim();
    const canSearchWithFilters = this.manualPlayerSource() === 'PROFESSIONAL' && this.hasActiveManualPlayerFilters();

    if (query.length < 2 && !canSearchWithFilters) {
      this.manualPlayerSearchResults.set([]);
      this.manualPlayerSearchError.set('Escribe al menos 2 caracteres para buscar un jugador.');
      return;
    }

    const requestId = ++this.manualPlayerSearchRequestId;
    this.isSearchingPersons.set(true);
    this.manualPlayerSearchError.set(null);

    if (this.manualPlayerSource() === 'PROFESSIONAL') {
      this.proPlayerService.searchProPlayers(query, {
        gender: this.manualPlayerFilterGender(),
        category: this.manualPlayerFilterCategory()
      }).subscribe({
        next: players => {
          if (requestId !== this.manualPlayerSearchRequestId) {
            return;
          }

          this.manualPlayerSearchResults.set(players.slice(0, this.manualPlayerSearchResultLimit).map(player => ({
            id: String(player.id),
            tennisId: player.license,
            firstName: player.firstName,
            lastName: player.lastName,
            nationality: 'ESP',
            birthDate: player.birthDate,
            gender: player.gender,
            rankingPosition: player.rankingPosition,
            ageCategory: player.ageCategory,
            clubName: player.clubName
          })));
          this.manualPlayerSelectedPersonId.set('');
          this.isSearchingPersons.set(false);
        },
        error: (error) => {
          if (requestId !== this.manualPlayerSearchRequestId) {
            return;
          }

          this.manualPlayerSearchResults.set([]);
          this.manualPlayerSearchError.set(getApiErrorMessage(error, 'No se pudieron cargar los jugadores profesionales.'));
          this.isSearchingPersons.set(false);
        }
      });
      return;
    }

    this.personService.searchPersons(query).subscribe({
      next: persons => {
        if (requestId !== this.manualPlayerSearchRequestId) {
          return;
        }

        this.manualPlayerSearchResults.set(persons.slice(0, this.manualPlayerSearchResultLimit).map(person => ({
          id: person.id,
          tennisId: person.tennisId,
          firstName: person.firstName,
          lastName: person.lastName,
          nationality: person.nationality,
          birthDate: person.birthDate,
          gender: person.gender
        })));
        this.manualPlayerSelectedPersonId.set('');
        this.isSearchingPersons.set(false);
      },
      error: (error) => {
        if (requestId !== this.manualPlayerSearchRequestId) {
          return;
        }

        this.manualPlayerSearchResults.set([]);
        this.manualPlayerSearchError.set(getApiErrorMessage(error, 'No se pudieron cargar los jugadores existentes.'));
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

  selectExistingPerson(person: ManualPlayerLookupResult): void {
    this.manualPlayerSelectedPersonId.set(person.id);
    this.manualPlayerSearchError.set(null);
    this.manualPlayerSuccess.set(null);
  }

  submitManualPlayer(): void {
    if (!this.isCreator()) {
      this.manualPlayerError.set('Solo el administrador del torneo puede añadir jugadores manualmente.');
      return;
    }

    const currentTournament = this.tournament();
    const eventId = this.manualPlayerEventId().trim() || this.selectedTournamentInscriptionEventId() || '';

    if (!currentTournament) {
      return;
    }

    if (!eventId) {
      this.manualPlayerError.set('Selecciona una prueba para añadir el jugador.');
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
    } else if (playerSource === 'PROFESSIONAL') {
      const selectedProPlayerId = Number(this.manualPlayerSelectedPersonId());
      if (!selectedProPlayerId || Number.isNaN(selectedProPlayerId)) {
        this.manualPlayerError.set('Selecciona un jugador profesional de la lista.');
        return;
      }

      payload.proPlayerId = selectedProPlayerId;
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
        this.manualPlayerSuccess.set('Jugador añadido a la prueba correctamente.');
        this.manualPlayerSearchResults.set([]);
        this.manualPlayerSelectedPersonId.set('');
        this.manualPlayerFirstName.set('');
        this.manualPlayerLastName.set('');
        this.manualPlayerBirthDate.set('');
        this.manualPlayerNationality.set('');
        this.manualPlayerTennisId.set('');
        this.loadTournamentInscriptions();
      },
      error: (error) => {
        this.isSubmittingManualPlayer.set(false);
        this.manualPlayerError.set(getApiErrorMessage(error, 'No se pudo añadir el jugador a la prueba.'));
      }
    });
  }

  createCourt(): void {
    if (!this.isCreator()) {
      this.courtError.set('Solo el administrador del torneo puede crear pistas.');
      return;
    }

    const currentTournament = this.tournament();
    const name = this.newCourtName().trim();

    if (!currentTournament || !name) {
      return;
    }

    this.isCreatingCourt.set(true);
    this.courtError.set(null);
    this.courtMessage.set(null);

    this.tournamentService.createCourt(currentTournament.id, { name }).subscribe({
      next: court => {
        this.courts.update(courts => [...courts, court].sort((left, right) => left.name.localeCompare(right.name)));
        this.newCourtName.set('');
        this.courtMessage.set('Pista creada correctamente.');
        this.isCreatingCourt.set(false);
      },
      error: (error) => {
        this.courtError.set(getApiErrorMessage(error, 'No se pudo crear la pista.'));
        this.isCreatingCourt.set(false);
      }
    });
  }

  toggleCourtsPanel(): void {
    this.isCourtsPanelExpanded.update(expanded => !expanded);
  }

  toggleMatchSchedulePanel(): void {
    this.isMatchSchedulePanelExpanded.update(expanded => !expanded);
  }

  toggleManualPlayerPanel(): void {
    this.isManualPlayerPanelExpanded.update(expanded => !expanded);
  }

  toggleRegisteredPlayersPanel(): void {
    this.isRegisteredPlayersPanelExpanded.update(expanded => !expanded);
  }

  toggleEventsDrawsPanel(): void {
    this.isEventsDrawsPanelExpanded.update(expanded => !expanded);
  }

  clearMatchScheduleFilters(): void {
    this.matchScheduleEventFilter.set('');
    this.matchScheduleRoundFilter.set('');
    this.matchScheduleDateFilter.set('');
    this.matchScheduleCourtFilter.set('');
    this.matchScheduleProfessionalFilter.set('');
  }

  setMatchScheduleSort(field: MatchScheduleSortField): void {
    if (this.matchScheduleSortField() === field) {
      this.matchScheduleSortDirection.update(direction => direction === 'asc' ? 'desc' : 'asc');
      return;
    }

    this.matchScheduleSortField.set(field);
    this.matchScheduleSortDirection.set('asc');
  }

  getMatchScheduleSortIndicator(field: MatchScheduleSortField): string {
    if (this.matchScheduleSortField() !== field) {
      return '';
    }

    return this.matchScheduleSortDirection() === 'asc' ? '↑' : '↓';
  }

  selectCourt(court: CourtResponse): void {
    this.selectedCourtId.set(court.id);
    this.selectedCourtName.set(court.name);
    this.courtError.set(null);
    this.courtMessage.set(null);
  }

  clearSelectedCourt(): void {
    this.selectedCourtId.set(null);
    this.selectedCourtName.set('');
  }

  updateSelectedCourt(): void {
    if (!this.isCreator()) {
      this.courtError.set('Solo el administrador del torneo puede editar pistas.');
      return;
    }

    const currentTournament = this.tournament();
    const selectedCourt = this.selectedCourt();
    const name = this.selectedCourtName().trim();

    if (!currentTournament || !selectedCourt || !name) {
      return;
    }

    this.isUpdatingCourt.set(true);
    this.courtError.set(null);
    this.courtMessage.set(null);

    this.tournamentService.updateCourt(currentTournament.id, selectedCourt.id, { name }).subscribe({
      next: updatedCourt => {
        this.courts.update(courts => courts
          .map(court => court.id === updatedCourt.id ? updatedCourt : court)
          .sort((left, right) => left.name.localeCompare(right.name))
        );
        this.selectedCourtId.set(updatedCourt.id);
        this.selectedCourtName.set(updatedCourt.name);
        this.courtMessage.set('Pista actualizada correctamente.');
        this.isUpdatingCourt.set(false);
      },
      error: error => {
        this.courtError.set(getApiErrorMessage(error, 'No se pudo actualizar la pista.'));
        this.isUpdatingCourt.set(false);
      }
    });
  }

  deleteSelectedCourt(): void {
    if (!this.isCreator()) {
      this.courtError.set('Solo el administrador del torneo puede eliminar pistas.');
      return;
    }

    const currentTournament = this.tournament();
    const selectedCourt = this.selectedCourt();

    if (!currentTournament || !selectedCourt) {
      return;
    }

    if (!window.confirm(`¿Eliminar ${selectedCourt.name}?`)) {
      return;
    }

    this.isDeletingCourt.set(true);
    this.courtError.set(null);
    this.courtMessage.set(null);

    this.tournamentService.deleteCourt(currentTournament.id, selectedCourt.id).subscribe({
      next: () => {
        this.courts.update(courts => courts.filter(court => court.id !== selectedCourt.id));
        this.clearSelectedCourt();
        this.courtMessage.set('Pista eliminada correctamente.');
        this.isDeletingCourt.set(false);
      },
      error: error => {
        this.courtError.set(getApiErrorMessage(error, 'No se pudo eliminar la pista.'));
        this.isDeletingCourt.set(false);
      }
    });
  }

  private loadTournament(tournamentId: string, preserveActiveSection = false): void {
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
        this.loadCourts(tournament.id);
        this.isLoading.set(false);
        if (!preserveActiveSection) {
          this.activeSection.set(this.isCreator() ? 'setup' : 'overview');
        }
        setTimeout(() => this.tryInitMap(), 0);
      },
      error: (error) => {
        this.errorMessage.set(getApiErrorMessage(error, 'No se pudo cargar el detalle del torneo.'));
        this.isLoading.set(false);
      }
    });
  }

  private startTournamentLiveUpdates(tournamentId: string): void {
    this.liveUpdatesSubscription?.unsubscribe();
    this.liveUpdatesSubscription = this.tournamentLiveUpdatesService.watchTournament(tournamentId).subscribe({
      next: event => {
        const currentTournament = this.tournament();
        if (currentTournament && event.tournamentId !== currentTournament.id) {
          return;
        }

        if (this.isLiveUpdateBlockedByLocalSave(event.matchId)) {
          this.hasPendingLiveRefresh.set(true);
          return;
        }

        this.refreshTournamentAfterLiveUpdate(tournamentId);
      }
    });
  }

  private refreshTournamentAfterLiveUpdate(tournamentId: string): void {
    this.hasPendingLiveRefresh.set(false);
    this.actionMessage.set('Hay cambios en el cuadro, sincronizando...');

    this.tournamentService.getTournamentById(tournamentId).subscribe({
      next: tournament => {
        this.tournament.set(tournament);
        this.hydrateSelectedEventsFromTournament(tournament.events ?? []);
        this.initializeInscriptionSelection();
        this.syncManualPlayerEventSelection(tournament.events ?? []);
        this.selectedStatus.set(this.getDefaultStatusSelection(tournament.status));
        this.actionMessage.set('Cuadro actualizado');
      },
      error: error => {
        this.actionError.set(getApiErrorMessage(error, 'No se pudo sincronizar el cuadro actualizado.'));
      }
    });
  }

  private isLiveUpdateBlockedByLocalSave(matchId: string): boolean {
    return this.isSavingResultMatch(matchId) || this.savingScheduleMatchId() === matchId || this.savingScheduleCascadeMatchId() !== null;
  }

  private flushPendingLiveRefresh(): void {
    const currentTournament = this.tournament();
    if (!currentTournament || !this.hasPendingLiveRefresh()) {
      return;
    }

    if (this.savingResultMatchIds().size > 0 || this.savingScheduleMatchId() || this.savingScheduleCascadeMatchId()) {
      return;
    }

    this.refreshTournamentAfterLiveUpdate(currentTournament.id);
  }

  private loadCourts(tournamentId: string): void {
    this.isLoadingCourts.set(true);
    this.courtError.set(null);

    this.tournamentService.getCourts(tournamentId).subscribe({
      next: courts => {
        this.courts.set(courts);
        this.isLoadingCourts.set(false);
      },
      error: (error) => {
        this.courts.set([]);
        this.courtError.set(getApiErrorMessage(error, 'No se pudieron cargar las pistas del torneo.'));
        this.isLoadingCourts.set(false);
      }
    });
  }

  private loadEventCatalog(): void {
    this.isLoadingEvents.set(true);
    this.eventCatalogError.set(null);

    const isOrganizer = this.authService.currentRole === 'ORGANIZER';
    const catalog$ = isOrganizer
      ? this.tournamentService.getEventCatalogAll()
      : this.tournamentService.getEventCatalog();

    catalog$.subscribe({
      next: catalog => {
        this.eventCatalog.set(catalog);
        this.isLoadingEvents.set(false);
      },
      error: (error) => {
        this.eventCatalog.set([]);
        this.eventCatalogError.set(getApiErrorMessage(error, 'No se pudo cargar el catálogo de categorías.'));
        this.isLoadingEvents.set(false);
      }
    });
  }

  private loadNationalities(): void {
    this.referenceDataService.getNationalities().subscribe({
      next: nationalities => {
        this.nationalities.set(nationalities);
      },
      error: () => {
        this.nationalities.set([]);
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

  private hydrateSelectedEventsFromTournament(events: TournamentEventResponse[]): void {
    const groupedEvents = new Map<number, { eventIdsByGender: Map<TournamentEventGender, string>; genders: Set<TournamentEventGender>; stages: TournamentEventSelection['stages'] }>();

    events.forEach(event => {
      const normalizedGender = event.gender.toUpperCase() as TournamentEventGender;
      if (!this.eventGenderOptions.includes(normalizedGender)) {
        return;
      }

      const currentEntry = groupedEvents.get(event.categoryId) ?? {
        eventIdsByGender: new Map<TournamentEventGender, string>(),
        genders: new Set<TournamentEventGender>(),
        stages: event.stages?.length
          ? event.stages
              .slice()
              .sort((left, right) => left.order - right.order)
              .map(stage => {
                const raw = stage.strategyName ?? stage.stageType;
                const stageType: TournamentStageType = isValidStageType(raw) ? raw : 'SINGLE_ELIMINATION';
                return { stageType };
              })
          : [{ stageType: 'SINGLE_ELIMINATION' as TournamentStageType }]
      };
      currentEntry.eventIdsByGender.set(normalizedGender, event.eventId);
      currentEntry.genders.add(normalizedGender);
      groupedEvents.set(event.categoryId, currentEntry);
    });

    const selections: TournamentEventSelection[] = Array.from(groupedEvents.entries()).map(([categoryId, entry]) => ({
      categoryId,
      eventCategory: this.getEventLabelById(categoryId),
      eventsByGender: Array.from(entry.eventIdsByGender.entries()).map(([gender, eventId]) => ({ gender, eventId })),
      genders: Array.from(entry.genders),
      stages: entry.stages
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
        error: (error) => {
          this.tournamentInscriptions.set(null);
          this.tournamentInscriptionsError.set(getApiErrorMessage(error, 'No se pudo cargar el listado de jugadores inscritos.'));
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
        return 'Busca en la base de jugadores y selecciona al jugador correcto sin salir del detalle.';
      case 'MANUAL':
        return 'Introduce los datos básicos del participante directamente en el formulario.';
      case 'PROFESSIONAL':
        return 'Busca en la base de profesionales cargada y selecciona el registro correcto.';
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

  getManualPlayerMetaLabel(person: ManualPlayerLookupResult): string {
    return [
      person.rankingPosition ? `#${person.rankingPosition}` : null,
      person.ageCategory,
      person.clubName
    ].filter(Boolean).join(' · ');
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
    const canSearchWithFilters = this.manualPlayerSource() === 'PROFESSIONAL' && this.hasActiveManualPlayerFilters();

    if ((query.length < 2 && !canSearchWithFilters) || this.manualPlayerSource() === 'MANUAL') {
      this.manualPlayerSearchRequestId += 1;
      this.manualPlayerSearchResults.set([]);
      this.isSearchingPersons.set(false);
      return;
    }

    this.manualPlayerSearchDebounceHandle = setTimeout(() => {
      this.searchManualPlayerCandidates();
    }, 300);
  }

  private cancelManualPlayerSearch(): void {
    if (!this.manualPlayerSearchDebounceHandle) {
      return;
    }

    clearTimeout(this.manualPlayerSearchDebounceHandle);
    this.manualPlayerSearchDebounceHandle = null;
  }

  onGenerateDraws(event: { tournamentId: string; stageId: string }, eventId: string): void {
    if (!this.isCreator()) {
      this.actionError.set('Solo el administrador del torneo puede generar cuadros.');
      return;
    }

    this.generatingDrawsStageId.set(event.stageId);
    this.setDrawGenerationFeedback(event.stageId, null);
    this.actionMessage.set(null);
    this.actionError.set(null);

    this.tournamentService.generateDraws(event.tournamentId, eventId)
      .subscribe({
        next: (tournament) => {
          console.debug('GenerateDraws: backend returned tournament event(s):', tournament.events);
          console.debug('GenerateDraws: current local tournament before replace:', this.tournament());
          const updatedTournament = this.replaceTournamentEvent(this.tournament(), tournament, eventId);
          this.tournament.set(updatedTournament ?? tournament);
          console.debug('GenerateDraws: local tournament after replace:', this.tournament());
          this.clearGeneratingDrawsStage(event.stageId);
          this.setDrawGenerationFeedback(event.stageId, {
            status: 'success',
            message: 'Cuadros generados correctamente.'
          });
          this.actionMessage.set('Cuadros generados correctamente');
        },
        error: (err) => {
          const message = `Error al generar cuadros: ${getApiErrorMessage(err)}`;
          this.clearGeneratingDrawsStage(event.stageId);
          this.setDrawGenerationFeedback(event.stageId, {
            status: 'error',
            message
          });
          this.actionError.set(message);
        }
      });
  }

  private setDrawGenerationFeedback(stageId: string, feedback: DrawGenerationFeedback | null): void {
    const nextFeedback = { ...this.drawGenerationFeedbackByStageId() };

    if (feedback) {
      nextFeedback[stageId] = feedback;
    } else {
      delete nextFeedback[stageId];
    }

    this.drawGenerationFeedbackByStageId.set(nextFeedback);
  }

  private clearGeneratingDrawsStage(stageId: string): void {
    if (this.generatingDrawsStageId() === stageId) {
      this.generatingDrawsStageId.set(null);
    }
  }

  onMatchSelected(matchId: string): void {
    // Placeholder para futuras acciones al seleccionar un match
    console.log('Match selected:', matchId);
  }

  onMatchResultSaved(event: { matchId: string; winnerId: string | null; result: string }): void {
    if (!this.isCreator()) {
      this.actionError.set('Solo el administrador del torneo puede registrar resultados.');
      return;
    }

    const currentTournament = this.tournament();
    if (!currentTournament) {
      return;
    }

    if (this.isSavingResultMatch(event.matchId)) {
      this.actionError.set('Espera a que termine el guardado del resultado en curso.');
      return;
    }

    const previousTournament = this.cloneTournament(currentTournament);
    const optimisticMatch = this.createOptimisticMatchResult(currentTournament, event);
    if (!optimisticMatch) {
      this.actionError.set('No se pudo localizar el partido seleccionado.');
      return;
    }

    const optimisticTournament = this.patchMatchResultInTournament(currentTournament, event.matchId, optimisticMatch);
    if (optimisticTournament) {
      this.tournament.set(optimisticTournament);
    }

    this.markResultMatchAsSaving(event.matchId);
    this.actionError.set(null);
    this.actionMessage.set('Guardando resultado...');
    this.tournamentService.submitMatchResult(currentTournament.id, event.matchId, {
      winnerId: event.winnerId || undefined,
      scoreString: event.result
    }).subscribe({
      next: (updatedMatch) => {
        const updatedTournament = this.patchMatchResultInTournament(this.tournament() ?? currentTournament, event.matchId, updatedMatch);
        if (updatedTournament) {
          this.tournament.set(updatedTournament);
        }
        this.actionMessage.set('Resultado guardado y cuadro actualizado');
        this.unmarkResultMatchAsSaving(event.matchId);
        this.flushPendingLiveRefresh();
      },
      error: (error) => {
        const rolledBackTournament = this.rollbackMatchResultInTournament(this.tournament() ?? currentTournament, previousTournament, event.matchId);
        this.tournament.set(rolledBackTournament ?? previousTournament);
        this.actionError.set(getApiErrorMessage(error, 'No se pudo guardar el resultado del partido.'));
        this.unmarkResultMatchAsSaving(event.matchId);
        this.flushPendingLiveRefresh();
      }
    });
  }

  onMatchScheduleSaved(event: {
    matchId: string;
    courtId: string;
    scheduledAt: string;
    scheduleTimeType: MatchScheduleTimeType;
    cascade?: boolean;
  }): void {
    if (!this.isCreator()) {
      this.actionError.set('Solo el administrador del torneo puede programar partidos.');
      return;
    }

    const currentTournament = this.tournament();
    if (!currentTournament) {
      return;
    }

    this.actionError.set(null);
    this.actionMessage.set(null);
    this.savingScheduleMatchId.set(event.matchId);
    if (event.cascade) {
      this.savingScheduleCascadeMatchId.set(event.matchId);
    }

    this.tournamentService.scheduleMatch(currentTournament.id, event.matchId, {
      courtId: event.courtId,
      scheduledAt: event.scheduledAt,
      scheduleTimeType: event.scheduleTimeType,
      cascade: event.cascade
    }).subscribe({
      next: updatedMatch => {
        if (event.cascade) {
          this.loadTournament(currentTournament.id, true);
        } else {
          const updatedTournament = this.patchSingleMatchInTournament(currentTournament, event.matchId, updatedMatch);
          if (updatedTournament) {
            this.tournament.set(updatedTournament);
          } else {
            this.loadTournament(currentTournament.id, true);
          }
        }
        this.setMatchScheduleDraft(updatedMatch);
        this.actionMessage.set(event.cascade ? 'Programación en cascada guardada.' : 'Programación del partido guardada.');
        this.savingScheduleMatchId.set(null);
        this.savingScheduleCascadeMatchId.set(null);
        this.flushPendingLiveRefresh();
      },
      error: error => {
        this.actionError.set(getApiErrorMessage(error, 'No se pudo guardar la programación del partido.'));
        this.savingScheduleMatchId.set(null);
        this.savingScheduleCascadeMatchId.set(null);
        this.flushPendingLiveRefresh();
      }
    });
  }

  getMatchScheduleDraft(match: MatchResponse): MatchScheduleDraft {
    return this.matchScheduleDrafts()[match.id] ?? this.createMatchScheduleDraft(match);
  }

  updateMatchScheduleType(match: MatchResponse, scheduleTimeType: string): void {
    const nextType: MatchScheduleTimeType = scheduleTimeType === 'NOT_BEFORE' ? 'NOT_BEFORE' : 'EXACT';
    this.updateMatchScheduleDraft(match, { scheduleTimeType: nextType });
  }

  updateMatchScheduleDate(match: MatchResponse, scheduledAt: string): void {
    this.updateMatchScheduleDraft(match, { scheduledAt });
  }

  updateMatchScheduleCourt(match: MatchResponse, courtId: string): void {
    this.updateMatchScheduleDraft(match, { courtId });
  }

  updateMatchScheduleCascade(match: MatchResponse, cascade: boolean): void {
    this.updateMatchScheduleDraft(match, { cascade });
  }

  canSaveMatchSchedule(match: MatchResponse): boolean {
    if (!this.isCreator()) {
      return false;
    }

    const draft = this.getMatchScheduleDraft(match);
    return !!draft.courtId && !!draft.scheduledAt && this.savingScheduleMatchId() !== match.id;
  }

  saveMatchSchedule(match: MatchResponse): void {
    const draft = this.getMatchScheduleDraft(match);
    if (!draft.courtId || !draft.scheduledAt) {
      this.actionError.set('Selecciona pista y hora antes de guardar la programación.');
      return;
    }

    this.onMatchScheduleSaved({
      matchId: match.id,
      courtId: draft.courtId,
      scheduledAt: draft.scheduledAt,
      scheduleTimeType: draft.scheduleTimeType,
      cascade: draft.cascade
    });
  }

  private replaceTournamentEvent(
    currentTournament: TournamentResponse | null,
    updatedTournament: TournamentResponse,
    eventId: string
  ): TournamentResponse | null {
    if (!currentTournament) {
      return updatedTournament;
    }

    const updatedEvent = updatedTournament.events?.find(event => event.eventId === eventId);
    if (!updatedEvent) {
      return updatedTournament;
    }

    const sanitized = this.sanitizeEvent(updatedEvent);

    return {
      ...currentTournament,
      events: (currentTournament.events ?? []).map(event =>
        event.eventId === eventId ? sanitized : event
      )
    };
  }

  private sanitizeEvent(event: TournamentEventResponse): TournamentEventResponse {
    if (!event || !event.stages) return event;

    const stagesById = new Map<string, StageResponse>();

    for (const st of event.stages) {
      const existing = stagesById.get(st.id);
      if (!existing) {
        stagesById.set(st.id, st);
        continue;
      }

      // prefer stage that has more draws or draws with matches
      const existingScore = (existing.draws ?? []).reduce((s: number, d: DrawResponse) => s + ((d.matches?.length ?? 0) > 0 ? 10 : 1), 0);
      const newScore = (st.draws ?? []).reduce((s: number, d: DrawResponse) => s + ((d.matches?.length ?? 0) > 0 ? 10 : 1), 0);
      if (newScore > existingScore) {
        stagesById.set(st.id, st);
      }
    }

    // sanitize draws inside each stage
    const sanitizedStages = Array.from(stagesById.values()).map(stage => {
      if (!stage.draws) return stage;
      const drawsById = new Map<string, DrawResponse>();
      for (const dr of stage.draws) {
        const existing = drawsById.get(dr.id);
        if (!existing) {
          drawsById.set(dr.id, dr);
          continue;
        }
        const existingScore = (existing.matches ?? []).length;
        const newScore = (dr.matches ?? []).length;
        if (newScore > existingScore) drawsById.set(dr.id, dr);
      }
      return { ...stage, draws: Array.from(drawsById.values()) };
    });

    return { ...event, stages: sanitizedStages };
  }

  private patchMatchResultInTournament(
    currentTournament: TournamentResponse,
    matchId: string,
    updatedMatch: MatchResponse
  ): TournamentResponse | null {
    let patched = false;

    const events = (currentTournament.events ?? []).map(event => ({
      ...event,
      stages: (event.stages ?? []).map(stage => ({
        ...stage,
        draws: (stage.draws ?? []).map(draw => {
          const patchedDraw = this.patchDrawMatches(draw, matchId, updatedMatch);
          patched = patched || patchedDraw.patched;
          return patchedDraw.draw;
        })
      }))
    }));

    return patched ? { ...currentTournament, events } : null;
  }

  private patchDrawMatches(
    draw: DrawResponse,
    matchId: string,
    updatedMatch: MatchResponse
  ): { draw: DrawResponse; patched: boolean } {
    const matches = draw.matches ?? [];
    const currentIndex = matches.findIndex(match => match.id === matchId);

    if (currentIndex < 0) {
      return { draw, patched: false };
    }

    const currentMatch = matches[currentIndex];
    const previousWinnerId = currentMatch.winnerId ?? null;
    const nextMatch = this.findNextMatchForSource(matches, currentMatch);

    const patchedMatches = matches.map(match => {
      if (match.id === matchId) {
        return {
          ...match,
          winnerId: updatedMatch.winnerId,
          result: updatedMatch.result
        };
      }

      if (nextMatch && match.id === nextMatch.id) {
        return this.assignWinnerToNextMatchOptimistically(
          match,
          updatedMatch.winnerId ?? null,
          previousWinnerId,
          currentMatch.bracketPosition ?? null,
          matches
        );
      }

      return match;
    });

    // Ensure there are no duplicate matches by id (preserve first occurrence)
    const seen = new Set<string>();
    const uniqueMatches: MatchResponse[] = [];
    for (const m of patchedMatches) {
      if (!m || !m.id) continue;
      if (seen.has(m.id)) continue;
      seen.add(m.id);
      uniqueMatches.push(m);
    }

    return {
      draw: {
        ...draw,
        matches: uniqueMatches
      },
      patched: true
    };
  }

  private createOptimisticMatchResult(
    tournament: TournamentResponse,
    event: { matchId: string; winnerId: string | null; result: string }
  ): MatchResponse | null {
    const match = this.findMatchInTournament(tournament, event.matchId);
    if (!match) {
      return null;
    }

    return {
      ...match,
      winnerId: event.winnerId !== null ? event.winnerId : match.winnerId,
      result: event.result || match.result
    };
  }

  private findMatchInTournament(tournament: TournamentResponse, matchId: string): MatchResponse | null {
    for (const event of tournament.events ?? []) {
      for (const stage of event.stages ?? []) {
        for (const draw of stage.draws ?? []) {
          const match = (draw.matches ?? []).find(currentMatch => currentMatch.id === matchId);
          if (match) {
            return match;
          }
        }
      }
    }

    return null;
  }

  private findNextMatchForSource(matches: MatchResponse[], sourceMatch: MatchResponse): MatchResponse | null {
    if (sourceMatch.bracketPosition == null) {
      return null;
    }

    const nextRoundNumber = (sourceMatch.roundNumber ?? 1) + 1;
    const nextBracketPosition = Math.floor(sourceMatch.bracketPosition / 2);

    return matches.find(match =>
      (match.roundNumber ?? 1) === nextRoundNumber &&
      match.bracketPosition === nextBracketPosition
    ) ?? null;
  }

  private assignWinnerToNextMatchOptimistically(
    nextMatch: MatchResponse,
    winnerId: string | null,
    previousWinnerId: string | null,
    sourceBracketPosition: number | null,
    allMatches: MatchResponse[]
  ): MatchResponse {
    if (!winnerId) {
      return nextMatch;
    }

    if (previousWinnerId && nextMatch.firstInscriptionId === previousWinnerId) {
      return this.hydrateOptimisticProfessionalPoints({
        ...nextMatch,
        firstInscriptionId: winnerId,
        secondInscriptionId: nextMatch.secondInscriptionId === winnerId ? null : nextMatch.secondInscriptionId
      }, allMatches);
    }

    if (previousWinnerId && nextMatch.secondInscriptionId === previousWinnerId) {
      return this.hydrateOptimisticProfessionalPoints({
        ...nextMatch,
        firstInscriptionId: nextMatch.firstInscriptionId === winnerId ? null : nextMatch.firstInscriptionId,
        secondInscriptionId: winnerId
      }, allMatches);
    }

    if (nextMatch.firstInscriptionId === winnerId || nextMatch.secondInscriptionId === winnerId) {
      return nextMatch;
    }

    if (sourceBracketPosition == null) {
      return nextMatch;
    }

    const useFirstSlot = sourceBracketPosition % 2 === 0;
    if (useFirstSlot) {
      if (nextMatch.firstInscriptionId && nextMatch.firstInscriptionId !== winnerId) {
        return nextMatch;
      }

      return this.hydrateOptimisticProfessionalPoints({
        ...nextMatch,
        firstInscriptionId: winnerId,
        secondInscriptionId: nextMatch.secondInscriptionId === winnerId ? null : nextMatch.secondInscriptionId
      }, allMatches);
    }

    if (nextMatch.secondInscriptionId && nextMatch.secondInscriptionId !== winnerId) {
      return nextMatch;
    }

    return this.hydrateOptimisticProfessionalPoints({
      ...nextMatch,
      firstInscriptionId: nextMatch.firstInscriptionId === winnerId ? null : nextMatch.firstInscriptionId,
      secondInscriptionId: winnerId
    }, allMatches);
  }

  private hydrateOptimisticProfessionalPoints(match: MatchResponse, allMatches: MatchResponse[]): MatchResponse {
    if (!match.firstInscriptionId || !match.secondInscriptionId) {
      return {
        ...match,
        professionalMatch: false,
        firstWinPoints: null,
        secondWinPoints: null
      };
    }

    const awardedPointsByInscriptionId = this.getProfessionalAwardedPointsByInscriptionId(allMatches);
    const firstAwardedPoints = awardedPointsByInscriptionId[match.firstInscriptionId];
    const secondAwardedPoints = awardedPointsByInscriptionId[match.secondInscriptionId];

    if (firstAwardedPoints == null || secondAwardedPoints == null) {
      return match;
    }

    return {
      ...match,
      professionalMatch: true,
      firstWinPoints: secondAwardedPoints,
      secondWinPoints: firstAwardedPoints
    };
  }

  private getProfessionalAwardedPointsByInscriptionId(matches: MatchResponse[]): Record<string, number> {
    return matches.reduce<Record<string, number>>((accumulator, match) => {
      if (match.firstInscriptionId && match.secondWinPoints != null) {
        accumulator[match.firstInscriptionId] = match.secondWinPoints;
      }

      if (match.secondInscriptionId && match.firstWinPoints != null) {
        accumulator[match.secondInscriptionId] = match.firstWinPoints;
      }

      return accumulator;
    }, {});
  }

  private cloneTournament(tournament: TournamentResponse): TournamentResponse {
    return JSON.parse(JSON.stringify(tournament)) as TournamentResponse;
  }

  private rollbackMatchResultInTournament(
    currentTournament: TournamentResponse,
    previousTournament: TournamentResponse,
    matchId: string
  ): TournamentResponse | null {
    const previousMatch = this.findMatchInTournament(previousTournament, matchId);
    if (!previousMatch) {
      return null;
    }

    const previousNextMatch = this.findNextMatchInTournament(previousTournament, previousMatch);
    let restored = false;

    const events = (currentTournament.events ?? []).map(event => ({
      ...event,
      stages: (event.stages ?? []).map(stage => ({
        ...stage,
        draws: (stage.draws ?? []).map(draw => ({
          ...draw,
          matches: (draw.matches ?? []).map(match => {
            if (match.id === previousMatch.id) {
              restored = true;
              return previousMatch;
            }

            if (previousNextMatch && match.id === previousNextMatch.id) {
              restored = true;
              return previousNextMatch;
            }

            return match;
          })
        }))
      }))
    }));

    return restored ? { ...currentTournament, events } : null;
  }

  private findNextMatchInTournament(tournament: TournamentResponse, sourceMatch: MatchResponse): MatchResponse | null {
    for (const event of tournament.events ?? []) {
      for (const stage of event.stages ?? []) {
        for (const draw of stage.draws ?? []) {
          const nextMatch = this.findNextMatchForSource(draw.matches ?? [], sourceMatch);
          if (nextMatch) {
            return nextMatch;
          }
        }
      }
    }

    return null;
  }

  private isSavingResultMatch(matchId: string): boolean {
    return this.savingResultMatchIds().has(matchId);
  }

  private markResultMatchAsSaving(matchId: string): void {
    this.savingResultMatchIds.update(matchIds => new Set([...matchIds, matchId]));
  }

  private unmarkResultMatchAsSaving(matchId: string): void {
    this.savingResultMatchIds.update(matchIds => {
      const nextMatchIds = new Set(matchIds);
      nextMatchIds.delete(matchId);
      return nextMatchIds;
    });
  }

  private patchSingleMatchInTournament(
    currentTournament: TournamentResponse,
    matchId: string,
    updatedMatch: MatchResponse
  ): TournamentResponse | null {
    let patched = false;

    const events = (currentTournament.events ?? []).map(event => ({
      ...event,
      stages: (event.stages ?? []).map(stage => ({
        ...stage,
        draws: (stage.draws ?? []).map(draw => ({
          ...draw,
          matches: (draw.matches ?? []).map(match => {
            if (match.id !== matchId) {
              return match;
            }

            patched = true;
            return {
              ...match,
              ...updatedMatch
            };
          })
        }))
      }))
    }));

    return patched ? { ...currentTournament, events } : null;
  }

  private compareMatchScheduleRows(left: TournamentMatchScheduleRow, right: TournamentMatchScheduleRow): number {
    const direction = this.matchScheduleSortDirection() === 'asc' ? 1 : -1;
    let comparison = 0;

    switch (this.matchScheduleSortField()) {
      case 'event':
        comparison = this.compareText(left.eventLabel, right.eventLabel);
        break;
      case 'round':
        comparison = this.compareNumber(left.match.roundNumber ?? 0, right.match.roundNumber ?? 0);
        break;
      case 'scheduledAt':
        comparison = this.compareText(this.getSavedMatchScheduleDateTimeValue(left) || '9999-12-31T23:59', this.getSavedMatchScheduleDateTimeValue(right) || '9999-12-31T23:59');
        break;
      case 'court':
        comparison = this.compareText(this.getSavedMatchScheduleCourtName(left), this.getSavedMatchScheduleCourtName(right));
        break;
    }

    if (comparison !== 0) {
      return comparison * direction;
    }

    return this.compareText(left.eventLabel, right.eventLabel) ||
      this.compareNumber(left.match.roundNumber ?? 0, right.match.roundNumber ?? 0) ||
      this.compareText(this.getSavedMatchScheduleDateTimeValue(left), this.getSavedMatchScheduleDateTimeValue(right)) ||
      this.compareText(this.getSavedMatchScheduleCourtName(left), this.getSavedMatchScheduleCourtName(right)) ||
      this.compareText(left.match.id, right.match.id);
  }

  private getSavedMatchScheduleDateValue(row: TournamentMatchScheduleRow): string {
    return this.getSavedMatchScheduleDateTimeValue(row).slice(0, 10);
  }

  private getSavedMatchScheduleDateTimeValue(row: TournamentMatchScheduleRow): string {
    return this.toDatetimeLocalValue(row.match.scheduledAt);
  }

  private getSavedMatchScheduleCourtId(row: TournamentMatchScheduleRow): string {
    return row.match.courtId ?? '';
  }

  private getSavedMatchScheduleCourtName(row: TournamentMatchScheduleRow): string {
    const courtId = this.getSavedMatchScheduleCourtId(row);
    return this.courts().find(court => court.id === courtId)?.name ?? row.match.court ?? '';
  }

  private compareText(left: string | undefined | null, right: string | undefined | null): number {
    return (left ?? '').localeCompare(right ?? '');
  }

  private compareNumber(left: number, right: number): number {
    return left - right;
  }

  private updateMatchScheduleDraft(match: MatchResponse, patch: Partial<MatchScheduleDraft>): void {
    const currentDraft = this.getMatchScheduleDraft(match);
    this.matchScheduleDrafts.update(drafts => ({
      ...drafts,
      [match.id]: {
        ...currentDraft,
        ...patch
      }
    }));
  }

  private setMatchScheduleDraft(match: MatchResponse): void {
    this.matchScheduleDrafts.update(drafts => ({
      ...drafts,
      [match.id]: this.createMatchScheduleDraft(match)
    }));
  }

  private createMatchScheduleDraft(match: MatchResponse): MatchScheduleDraft {
    return {
      courtId: match.courtId ?? '',
      scheduledAt: this.toDatetimeLocalValue(match.scheduledAt),
      scheduleTimeType: match.scheduleTimeType ?? 'EXACT',
      cascade: false
    };
  }

  private toDatetimeLocalValue(value?: string | null): string {
    return value ? value.slice(0, 16) : '';
  }

  private getScheduleParticipantName(inscriptionId: string | null | undefined): string {
    if (!inscriptionId) {
      return 'Por determinar';
    }

    return this.participantNamesByInscriptionId()[inscriptionId] ?? inscriptionId.substring(0, 8);
  }

  getCategoryLabel(categoryId: number): string {
    return this.getEventLabelById(categoryId);
  }

  getGenderLabelForString(gender: string | TournamentEventGender): string {
    return this.getGenderLabel(gender as TournamentEventGender);
  }

  getStatusLabel(status: TournamentStatus): string {
    const labels: Record<TournamentStatus, string> = {
      DRAFT: 'Borrador',
      OPEN: 'Inscripciones abiertas',
      ACTIVE: 'Activo',
      CLOSED: 'Inscripciones cerradas',
      IN_PROGRESS: 'En juego',
      COMPLETED: 'Finalizado',
      CANCELLED: 'Cancelado'
    };

    return labels[status] ?? status;
  }

  getStatusColorClasses(status: TournamentStatus): string {
    const colors: Record<TournamentStatus, string> = {
      DRAFT: 'border-neutral-200 bg-neutral-100 text-neutral-600',
      OPEN: 'border-blue-200 bg-blue-100 text-blue-700',
      ACTIVE: 'border-green-200 bg-green-100 text-green-700',
      CLOSED: 'border-amber-200 bg-amber-100 text-amber-700',
      IN_PROGRESS: 'border-sky-200 bg-sky-100 text-sky-700',
      COMPLETED: 'border-emerald-200 bg-emerald-100 text-emerald-700',
      CANCELLED: 'border-red-200 bg-red-100 text-red-700'
    };

    return colors[status] ?? 'border-primary-200 bg-primary-50 text-primary-700';
  }

  private tryInitMap(): void {
    if (this.mapInstance) return;

    const t = this.tournament();
    if (!t?.locationLatitude || !t?.locationLongitude) return;

    const el = document.getElementById('tournament-map');
    if (!el) return;

    this.initMap(t, el as HTMLDivElement);
  }

  private async initMap(tournament: TournamentResponse, el: HTMLDivElement): Promise<void> {
    this.mapInstance?.remove();

    await import('leaflet');
    const L = (window as any).L;

    this.mapInstance = L.map(el).setView([tournament.locationLatitude!, tournament.locationLongitude!], 16);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(this.mapInstance);

    L.circleMarker([tournament.locationLatitude!, tournament.locationLongitude!], {
      radius: 8,
      color: '#1a6b4a',
      fillColor: '#22c55e',
      fillOpacity: 0.8,
    })
      .addTo(this.mapInstance)
      .bindPopup(tournament.location);
  }

  hasTournamentMapsLink(tournament: TournamentResponse): boolean {
    return !!tournament.locationPlaceId || (tournament.locationLatitude != null && tournament.locationLongitude != null);
  }

  getTournamentMapsLink(tournament: TournamentResponse): string {
    const query = tournament.locationFormattedAddress || tournament.location;
    if (tournament.locationPlaceId) {
      return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}&query_place_id=${encodeURIComponent(tournament.locationPlaceId)}`;
    }

    return `https://www.google.com/maps/search/?api=1&query=${tournament.locationLatitude},${tournament.locationLongitude}`;
  }
}
