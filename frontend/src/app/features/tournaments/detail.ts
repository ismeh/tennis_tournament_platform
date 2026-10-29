import { CommonModule, DatePipe } from '@angular/common';
import {
  Component,
  OnDestroy,
  OnInit,
  AfterViewInit,
  HostListener,
  computed,
  inject,
  signal,
  ViewChild,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { PersonService } from '../../data/services/person.service';
import { ProPlayerService } from '../../data/services/pro-player.service';
import { NationalityOption } from '../../data/interfaces/reference-data.model';
import {
  CourtResponse,
  TournamentInscriptionCategoryCount,
  TournamentInscriptionEvent,
  TournamentInscriptionPlayer,
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
  MatchStatus,
  SetScoreResponse,
  StageResponse,
  TournamentProviderSummary,
  TournamentStatus,
  TournamentResponse,
  TournamentGeneralInfoUpdateRequest,
  TournamentSurfaceCategory,
  getTournamentEventGenderLabel,
  getTournamentStageTypeLabel,
  getTournamentSurfaceCategoryLabel,
  TournamentStageType,
  validateStageSequence,
  isConsolationDisabled,
  getAvailableStageOptions,
  isValidStageType,
  ScheduleConfigResponse,
  ScheduleTimeSlot,
  TournamentUmpireResponse,
  TournamentUmpireSearchResponse
} from '../../data/interfaces/tournament.model';
import { MemberService } from '../../data/services/member.service';
import { TournamentLiveUpdatesService } from '../../data/services/tournament-live-updates.service';
import { TournamentService } from '../../data/services/tournament.service';
import { InvitationService } from '../../core/services/invitation.service';
import { ReferenceDataService } from '../../data/services/reference-data.service';
import { ClubAutocompleteComponent } from '../../components/club-autocomplete';
import { LocationInputComponent } from '../../components/location-input';
import { getApiErrorMessage } from '../../core/errors/api-error.util';
import { StagesComponent } from './components/stages.component';
import { MatchDetailModalComponent } from './components/match-detail-modal.component';

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
  imports: [CommonModule, RouterLink, DatePipe, FormsModule, ReactiveFormsModule, StagesComponent, LocationInputComponent, ClubAutocompleteComponent, MatchDetailModalComponent],
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
                <div class="flex items-center gap-3">
                  <h1 class="text-3xl font-black text-neutral-900 sm:text-4xl">{{ tournament()!.formalName }}</h1>
                  @if (canEditGeneralInfo() && !isEditingGeneralInfo()) {
                    <button
                      type="button"
                      (click)="startEditGeneralInfo()"
                      class="inline-flex items-center justify-center rounded-full p-2 text-neutral-400 transition-colors hover:bg-primary-50 hover:text-primary-600"
                      title="Editar información general del torneo"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
                      </svg>
                    </button>
                  }
                </div>
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
              <div class="relative">
                <span
                  (click)="toggleHeaderStatusDropdown(); $event.stopPropagation()"
                  [class]="(isTournamentAdmin() && allowedStatusTransitions().length > 0)
                    ? 'inline-flex w-fit cursor-pointer items-center gap-1.5 rounded-full border px-4 py-2 text-sm font-semibold transition-opacity hover:opacity-80 ' + getStatusColorClasses(tournament()!.status)
                    : 'inline-flex w-fit rounded-full border px-4 py-2 text-sm font-semibold ' + getStatusColorClasses(tournament()!.status)"
                >
                  Estado: {{ getStatusLabel(tournament()!.status) }}
                  @if (isTournamentAdmin() && allowedStatusTransitions().length > 0) {
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
                    </svg>
                  }
                </span>
                @if (showHeaderStatusDropdown()) {
                  <div
                    class="absolute left-0 z-30 mt-2 w-56 rounded-xl border border-neutral-200 bg-white py-1 shadow-lg"
                    (keydown)="onHeaderStatusDropdownKeydown($event)"
                    (click)="$event.stopPropagation()"
                  >
                    <p class="px-3 pt-2 pb-1 text-xs font-semibold uppercase tracking-widest text-neutral-400">Cambiar a</p>
                    @for (status of allowedStatusTransitions(); track status) {
                      <button
                        type="button"
                        (click)="updateHeaderStatus(status)"
                        [disabled]="isUpdatingStatus()"
                        class="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-neutral-700 transition-colors hover:bg-neutral-50 disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        <span class="inline-block h-2.5 w-2.5 rounded-full {{ getStatusColorClasses(status) }}"></span>
                        {{ getStatusLabel(status) }}
                      </button>
                    }
                    @if (allowedStatusTransitions().length === 0) {
                      <p class="px-3 py-2 text-xs text-neutral-500">No hay cambios disponibles.</p>
                    }
                  </div>
                }
              </div>
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

                @if (isTournamentAdmin()) {
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
              <div class="flex items-center justify-between">
                <h2 class="text-xl font-bold text-neutral-900">Información del torneo</h2>
                @if (canEditGeneralInfo() && !isEditingGeneralInfo()) {
                  <button
                    type="button"
                    (click)="startEditGeneralInfo()"
                    class="inline-flex items-center gap-2 rounded-xl border border-neutral-300 bg-white px-4 py-2 text-sm font-semibold text-neutral-700 transition-colors hover:border-primary-300 hover:bg-primary-50 hover:text-primary-700"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
                    </svg>
                    Editar
                  </button>
                }
              </div>

              @if (isEditingGeneralInfo()) {
                <form class="mt-5 space-y-5" [formGroup]="generalInfoForm" (ngSubmit)="saveGeneralInfo()">
                  <div class="grid gap-4 sm:grid-cols-2">
                    <label class="block">
                      <span class="mb-1 block text-sm font-medium text-neutral-700">Nombre formal</span>
                      <input
                        type="text"
                        formControlName="formalName"
                        class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white"
                      />
                    </label>
                    <label class="block">
                      <span class="mb-1 block text-sm font-medium text-neutral-700">Superficie</span>
                      <select
                        formControlName="surfaceCategory"
                        class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white"
                      >
                        @for (surface of surfaceOptions; track surface) {
                          <option [value]="surface">{{ getSurfaceLabel(surface) }}</option>
                        }
                      </select>
                    </label>
                  </div>

                  <div class="grid gap-4 sm:grid-cols-2">
                    <label class="block">
                      <span class="mb-1 block text-sm font-medium text-neutral-700">Inicio del torneo</span>
                      <input type="date" formControlName="playStartDate" class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white" />
                    </label>
                    <label class="block">
                      <span class="mb-1 block text-sm font-medium text-neutral-700">Hora de inicio</span>
                      <input type="time" formControlName="tournamentStartTime" class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white" />
                    </label>
                  </div>

                  <div class="grid gap-4 sm:grid-cols-2">
                    <label class="block">
                      <span class="mb-1 block text-sm font-medium text-neutral-700">Fin del torneo</span>
                      <input type="date" formControlName="playEndDate" class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white" />
                    </label>
                    <label class="block">
                      <span class="mb-1 block text-sm font-medium text-neutral-700">Plazas</span>
                      <input type="number" formControlName="maxPlayers" min="2" step="1" class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white" />
                    </label>
                  </div>

                  <div class="grid gap-4 sm:grid-cols-2">
                    <label class="block">
                      <span class="mb-1 block text-sm font-medium text-neutral-700">Inicio de inscripciones</span>
                      <input type="date" formControlName="inscriptionStartDate" class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white" />
                    </label>
                    <label class="block">
                      <span class="mb-1 block text-sm font-medium text-neutral-700">Cierre de inscripciones</span>
                      <input type="date" formControlName="inscriptionEndDate" class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white" />
                    </label>
                  </div>

                  <label class="block">
                    <span class="mb-1 block text-sm font-medium text-neutral-700">Ubicación</span>
                    <app-location-input
                      formControlName="location"
                      placeholder="Club de Tenis Principal"
                      (locationSelected)="onEditLocationSelected($event)"
                    ></app-location-input>
                  </label>

                  <div class="grid gap-4 sm:grid-cols-2">
                    <label class="block">
                      <span class="mb-1 block text-sm font-medium text-neutral-700">Formato de partido</span>
                      <select
                        formControlName="setsPerMatch"
                        class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white"
                      >
                        <option [value]="1">1 set (al primer set)</option>
                        <option [value]="2">2 sets (al mejor de 2)</option>
                        <option [value]="3">Al mejor de 3 sets</option>
                        <option [value]="5">Al mejor de 5 sets</option>
                      </select>
                    </label>

                    <label class="block">
                      <span class="mb-1 block text-sm font-medium text-neutral-700">Tiebreak set decisivo</span>
                      <select
                        formControlName="decisiveTiebreakPoints"
                        class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white"
                      >
                        <option [value]="7">Tiebreak normal (a 7 puntos)</option>
                        <option [value]="10">Super Tiebreak (a 10 puntos)</option>
                      </select>
                    </label>
                  </div>

                  @if (generalInfoForm.value.setsPerMatch === 1 || generalInfoForm.value.setsPerMatch === 2) {
                    <div class="grid gap-4 sm:grid-cols-2">
                      <label class="block">
                        <span class="mb-1 block text-sm font-medium text-neutral-700">Juegos por set</span>
                        <select
                          formControlName="gamesPerSet"
                          class="w-full rounded-2xl border border-neutral-300 bg-neutral-50 px-4 py-3 outline-none transition focus:border-primary-500 focus:bg-white"
                        >
                          <option [value]="6">Estándar (6 juegos)</option>
                          <option [value]="5">5 juegos</option>
                          <option [value]="4">4 juegos</option>
                        </select>
                      </label>
                    </div>
                  }

                  @if (generalInfoError()) {
                    <div class="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                      {{ generalInfoError() }}
                    </div>
                  }

                  <div class="flex flex-col gap-3 sm:flex-row sm:justify-end">
                    <button
                      type="button"
                      (click)="cancelEditGeneralInfo()"
                      class="rounded-2xl border border-neutral-300 px-5 py-3 font-semibold text-neutral-700 transition-colors hover:border-neutral-400 hover:bg-neutral-50"
                    >
                      Cancelar
                    </button>
                    <button
                      type="submit"
                      class="rounded-2xl bg-gradient-to-r from-primary-600 to-accent-600 px-5 py-3 font-semibold text-white shadow-lg shadow-primary-200 transition-all hover:scale-[1.01] hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-60"
                      [disabled]="generalInfoForm.invalid || isSavingGeneralInfo()"
                    >
                      {{ isSavingGeneralInfo() ? 'Guardando...' : 'Guardar cambios' }}
                    </button>
                  </div>
                </form>
              } @else {
                <div class="mt-5 rounded-2xl border border-neutral-200 bg-neutral-50/30 overflow-hidden divide-y divide-neutral-200">
                  <div class="grid sm:grid-cols-2 divide-y sm:divide-y-0 sm:divide-x divide-neutral-200">
                    <div class="p-4">
                      <p class="text-xs uppercase tracking-widest text-neutral-500 font-medium">Superficie</p>
                      <p class="mt-1 font-semibold text-neutral-900">{{ getSurfaceLabel(tournament()!.surfaceCategory) }}</p>
                    </div>
                    <div class="p-4">
                      <p class="text-xs uppercase tracking-widest text-neutral-500 font-medium">Plazas</p>
                      <p class="mt-1 font-semibold text-neutral-900">{{ tournament()!.maxPlayers }} jugadores</p>
                    </div>
                  </div>
                  <div class="grid sm:grid-cols-2 divide-y sm:divide-y-0 sm:divide-x divide-neutral-200">
                    <div class="p-4">
                      <p class="text-xs uppercase tracking-widest text-neutral-500 font-medium">Fechas de juego</p>
                      <p class="mt-1 font-semibold text-neutral-900">
                        {{ tournament()!.playStartDate | date: 'dd/MM/yyyy' }} - {{ tournament()!.playEndDate | date: 'dd/MM/yyyy' }}
                      </p>
                    </div>
                    <div class="p-4">
                      <p class="text-xs uppercase tracking-widest text-neutral-500 font-medium">Periodo de inscripción</p>
                      <p class="mt-1 font-semibold text-neutral-900">
                        {{ tournament()!.inscriptionStartDate | date: 'dd/MM/yyyy' }} - {{ tournament()!.inscriptionEndDate | date: 'dd/MM/yyyy' }}
                      </p>
                    </div>
                  </div>
                  <div class="grid sm:grid-cols-2 divide-y sm:divide-y-0 sm:divide-x divide-neutral-200">
                    <div class="p-4">
                      <p class="text-xs uppercase tracking-widest text-neutral-500 font-medium">Formato de partido</p>
                      <p class="mt-1 font-semibold text-neutral-900">
                        @if (tournament()!.setsPerMatch === 1 && (tournament()!.gamesPerSet ?? 6) !== 6) {
                          A {{ tournament()!.gamesPerSet }} juegos
                        } @else if (tournament()!.setsPerMatch === 1) {
                          A 1 set
                        } @else if (tournament()!.setsPerMatch === 2 && (tournament()!.gamesPerSet ?? 6) !== 6) {
                          A 2 sets ({{ tournament()!.gamesPerSet }} juegos)
                        } @else if (tournament()!.setsPerMatch === 2) {
                          A 2 sets (mejor de 2)
                        } @else {
                          Al mejor de {{ tournament()!.setsPerMatch ?? 3 }} sets
                        }
                      </p>
                    </div>
                    <div class="p-4">
                      <p class="text-xs uppercase tracking-widest text-neutral-500 font-medium">Tiebreak set decisivo</p>
                      <p class="mt-1 font-semibold text-neutral-900">A {{ tournament()!.decisiveTiebreakPoints ?? 7 }} puntos</p>
                    </div>
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
              }
            </section>
          }

          @if (activeSection() === 'setup') {
            <section class="mt-6 rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm sm:p-8">
              @if (isTournamentAdmin()) {
                <h2 class="text-xl font-bold text-neutral-900">Configuración del torneo</h2>
                <p class="mt-2 text-neutral-600">Prepara las pruebas, pistas e inscripciones antes de poner el torneo en marcha.</p>

                <div class="mt-6 rounded-3xl border border-neutral-200 bg-neutral-50 p-5 sm:p-6">
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
                    <div class="block">
                      <span class="mb-1 block text-sm font-medium text-neutral-700">Categorías disponibles</span>
                      <div class="max-h-64 space-y-2 overflow-y-auto rounded-2xl border border-neutral-300 bg-white p-3">
                        @for (cat of eventCatalog(); track cat.id) {
                          <div class="flex items-center justify-between gap-3 rounded-xl bg-neutral-50 px-3 py-2 hover:bg-primary-50">
                            <span class="text-sm font-medium text-neutral-800">{{ cat.category }}</span>
                            <button
                              type="button"
                              class="rounded-full bg-primary-500 p-1.5 text-white transition hover:bg-primary-600 disabled:opacity-40 disabled:cursor-not-allowed"
                              [disabled]="isAddCategoryDisabled(cat.id)"
                              (click)="addCatalogCategory(cat)"
                              title="Añadir esta categoría"
                            >
                              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4" />
                              </svg>
                            </button>
                          </div>
                        }
                      </div>
                    </div>

                    <div class="rounded-2xl border border-dashed border-neutral-300 bg-white p-4">
                      @if (selectedEvents().length === 0) {
                        <p class="text-sm text-neutral-500">No hay pruebas seleccionadas todavía.</p>
                      } @else {
                        <div class="space-y-3">
                          @for (event of selectedEvents(); track event.uniqueId) {
                            <div class="rounded-2xl border border-neutral-200 bg-neutral-50 p-4">
                              <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                                <div>
                                  <p class="text-sm font-bold text-neutral-900">{{ event.eventCategory }}</p>
                                </div>
                                <div class="flex items-center gap-3">
                                  <label class="block">
                                    <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Modalidad</span>
                                    <select
                                      class="rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 outline-none transition focus:border-primary-500"
                                      [ngModel]="event.genders[0]"
                                      (ngModelChange)="changeEventGender(event.uniqueId!, $event)"
                                    >
                                      @for (gender of eventGenderOptions; track gender) {
                                        <option [value]="gender" [disabled]="isGenderOptionDisabled(event.categoryId, gender, event.uniqueId)">
                                          {{ getGenderLabel(gender) }}
                                        </option>
                                      }
                                    </select>
                                  </label>

                                  <button
                                    type="button"
                                    class="mt-5 rounded-full border border-red-200 bg-red-50 p-2 text-red-600 transition hover:border-red-300 hover:bg-red-100"
                                    (click)="removeSelectedEvent(event.uniqueId!)"
                                    title="Eliminar esta prueba"
                                  >
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                                      <path stroke-linecap="round" stroke-linejoin="round" d="M20 12H4" />
                                    </svg>
                                  </button>
                                </div>
                              </div>

                                  <div class="mt-4 rounded-2xl border border-dashed border-neutral-200 bg-white p-4">
                                    <div class="flex items-center justify-between gap-3">
                                      <div>
                                        <button
                                          type="button"
                                          class="rounded-full border border-neutral-300 bg-white px-3 py-1.5 text-xs font-semibold text-neutral-700 transition hover:border-primary-400 hover:text-primary-700"
                                          (click)="addEventStage(event.uniqueId!)"
                                        >
                                          Añadir cuadro
                                        </button>
                                      </div>
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
                                               [ngModel]="stage.stageType"
                                               (ngModelChange)="updateEventStageType(event.uniqueId!, stageIndex, $event)"
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
                                            (click)="removeEventStage(event.uniqueId!, stageIndex)"
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

                <div class="mt-8 border-t border-neutral-200 pt-8">
                  <div class="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
                    <div>
                      <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-600">Horarios del torneo</p>
                      <h3 class="mt-2 text-xl font-bold text-neutral-900">Configura los horarios de juego</h3>
                      <p class="mt-2 text-sm text-neutral-600">Define las franjas horarias en las que se pueden planificar partidos y la duración esperada de cada partido.</p>
                    </div>
                  </div>

                  <div class="mt-4 rounded-3xl border border-neutral-200 bg-neutral-50 p-5 sm:p-6">
                    <div class="rounded-2xl border border-neutral-200 bg-white p-4">
                      <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-600">Duración de los partidos</p>
                      <div class="mt-3 flex flex-col gap-3 sm:flex-row sm:items-end">
                        <label class="block min-w-56">
                          <span class="mb-1 block text-sm font-medium text-neutral-700">Duración esperada (minutos)</span>
                          <input
                            type="number"
                            min="15"
                            max="480"
                            step="15"
                            class="w-full rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                            [ngModel]="scheduleConfigDraft().matchDurationMinutes"
                            (ngModelChange)="updateScheduleDuration($event)"
                          />
                        </label>
                      </div>
                    </div>

                    <div class="mt-6 rounded-2xl border border-neutral-200 bg-white p-4">
                      <div class="flex items-center justify-between">
                        <div>
                          <p class="text-sm font-semibold uppercase tracking-[0.2em] text-primary-600">Franjas horarias</p>
                          <p class="mt-1 text-sm text-neutral-600">Define los tramos horarios en los que se permiten planificar partidos.</p>
                        </div>
                        <button
                          type="button"
                          class="rounded-full border border-neutral-300 bg-white px-3 py-1.5 text-xs font-semibold text-neutral-700 transition hover:border-primary-400 hover:text-primary-700"
                          (click)="addScheduleTimeSlot()"
                        >
                          Añadir franja
                        </button>
                      </div>

                      @if (scheduleOverlapWarning()) {
                        <div class="mt-3 rounded-xl border border-amber-200 bg-amber-50 p-3">
                          <div class="flex items-start gap-2">
                            <span class="mt-0.5 text-amber-600">
                              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
                              </svg>
                            </span>
                            <div class="flex-1">
                              <p class="text-sm font-medium text-amber-800">{{ scheduleOverlapWarning() }}</p>
                              <div class="mt-2 flex gap-2">
                                <button
                                  type="button"
                                  class="rounded-lg bg-amber-100 px-3 py-1.5 text-xs font-semibold text-amber-800 transition hover:bg-amber-200"
                                  (click)="mergeOverlappingSlots()"
                                >
                                  Unir franjas solapadas
                                </button>
                                <button
                                  type="button"
                                  class="rounded-lg bg-white px-3 py-1.5 text-xs font-semibold text-amber-700 ring-1 ring-amber-300 transition hover:bg-amber-50"
                                  (click)="clearOverlapWarning()"
                                >
                                  Editar manualmente
                                </button>
                              </div>
                            </div>
                          </div>
                        </div>
                      }

                      <div class="mt-3 space-y-3">
                        @for (slot of scheduleConfigDraft().timeSlots; track $index; let slotIndex = $index) {
                          <div class="flex flex-col gap-2 rounded-xl border border-neutral-200 bg-neutral-50 p-3 sm:flex-row sm:items-center">
                            <div class="flex flex-1 items-center gap-2">
                              <label class="block">
                                <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Inicio</span>
                                <input
                                  type="time"
                                  class="w-full rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                                  [ngModel]="slot.startTime"
                                  (ngModelChange)="updateScheduleTimeSlot(slotIndex, 'startTime', $event)"
                                />
                              </label>
                              <span class="mt-5 text-neutral-400">-</span>
                              <label class="block">
                                <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Fin</span>
                                <input
                                  type="time"
                                  class="w-full rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                                  [ngModel]="slot.endTime"
                                  (ngModelChange)="updateScheduleTimeSlot(slotIndex, 'endTime', $event)"
                                />
                              </label>
                            </div>
                            <button
                              type="button"
                              class="self-end rounded-full border border-red-200 bg-red-50 px-3 py-1.5 text-xs font-semibold text-red-700 transition hover:border-red-300 hover:bg-red-100 sm:self-center"
                              (click)="removeScheduleTimeSlot(slotIndex)"
                            >
                              Eliminar
                            </button>
                          </div>
                        }
                        @if (scheduleConfigDraft().timeSlots.length === 0) {
                          <p class="rounded-xl border border-dashed border-neutral-300 bg-white p-3 text-center text-sm text-neutral-500">
                            No hay franjas horarias definidas. Añade al menos una franja para permitir la planificación de partidos.
                          </p>
                        }
                      </div>
                    </div>

                    @if (scheduleConfigSuccess()) {
                      <div class="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                        {{ scheduleConfigSuccess() }}
                      </div>
                    }

                    @if (scheduleConfigError()) {
                      <div class="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                        {{ scheduleConfigError() }}
                      </div>
                    }

                    <div class="mt-4 flex flex-col gap-3 sm:flex-row sm:justify-end">
                      <button
                        type="button"
                        (click)="saveScheduleConfig()"
                        class="rounded-2xl bg-gradient-to-r from-primary-600 to-accent-600 px-5 py-3 font-semibold text-white shadow-lg shadow-primary-200 transition-all hover:scale-[1.01] hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-60"
                        [disabled]="isSavingScheduleConfig()"
                      >
                        {{ isSavingScheduleConfig() ? 'Guardando horarios...' : 'Guardar horarios' }}
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
                @if (isTournamentAdmin()) {
                  Administra las inscripciones de tu torneo.
                } @else {
                  Solicita tu inscripción si el torneo está abierto.
                }
              </p>

              @if (isTournamentAdmin()) {
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
                      <h3 class="mt-2 text-xl font-bold text-neutral-900">Añadir jugador al torneo</h3>
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

                        <label class="block">
                          <span class="mb-1 block text-sm font-medium text-neutral-700">Club</span>
                          <input
                            type="text"
                            class="w-full rounded-xl border border-neutral-300 px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                            [ngModel]="manualPlayerClub()"
                            (ngModelChange)="manualPlayerClub.set($event)"
                            name="manualPlayerClub"
                            placeholder="Nombre del club"
                          />
                        </label>

                        <label class="block">
                          <span class="mb-1 block text-sm font-medium text-neutral-700">Puntos</span>
                          <input
                            type="number"
                            class="w-full rounded-xl border border-neutral-300 px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                            [ngModel]="manualPlayerPoints()"
                            (ngModelChange)="manualPlayerPoints.set($event)"
                            name="manualPlayerPoints"
                            placeholder="0"
                            min="0"
                          />
                        </label>

                        <label class="block">
                          <span class="mb-1 block text-sm font-medium text-neutral-700">Siglas</span>
                          <select
                            class="w-full rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                            [ngModel]="manualPlayerEntryStatus()"
                            (ngModelChange)="manualPlayerEntryStatus.set($event)"
                            name="manualPlayerEntryStatus"
                          >
                            <option value="">Selecciona</option>
                            <option value="DIRECT_ACCEPTANCE">DA - Aceptación directa</option>
                            <option value="WILDCARD">WC - Wildcard</option>
                            <option value="QUALIFIER">Q - Clasificado</option>
                            <option value="LUCKY_LOSER">LL - Perdedor afortunado</option>
                          </select>
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
                @if (!isLoggedIn()) {
                  <div class="mt-4 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
                    Registrate y completa tu perfil para poder inscribirte.
                    <a routerLink="/register" class="ml-2 font-semibold underline">Ir a registrarme</a>
                  </div>
                } @else if (!isProfileComplete()) {
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

                <div class="mt-4 flex items-center gap-3 flex-wrap">
                  <button
                    type="button"
                    class="rounded-2xl bg-gradient-to-r from-primary-600 to-accent-600 px-5 py-3 font-semibold text-white shadow-lg shadow-primary-200 transition-all hover:scale-[1.01] hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-60"
                    [disabled]="!canRequestInscription() || isSubmittingInscription()"
                    (click)="requestInscription()"
                  >
                    {{ isSubmittingInscription() ? 'Tramitando inscripción...' : 'Inscribirme' }}
                  </button>

                  @if (tournament()?.status === 'OPEN') {
                    <span class="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-3 py-1.5 text-xs font-semibold text-emerald-700">
                      <span class="h-2 w-2 rounded-full bg-emerald-500 animate-pulse"></span>
                      Inscripciones abiertas
                    </span>
                  } @else {
                    <span class="inline-flex items-center gap-1.5 rounded-full bg-red-50 px-3 py-1.5 text-xs font-semibold text-red-700">
                      <span class="h-2 w-2 rounded-full bg-red-500"></span>
                      Inscripciones cerradas
                    </span>
                  }
                </div>
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
                      @if (isTournamentAdmin() && (hasPointsChanges() || hasDetailChanges())) {
                        <div class="mt-4 flex items-center gap-3 rounded-xl border border-primary-200 bg-primary-50 px-4 py-2.5">
                          <span class="text-sm text-primary-700">Hay cambios sin guardar</span>
                          <button type="button" (click)="saveAllDetails()" [disabled]="isSavingDetails()" class="rounded-lg bg-primary-600 px-4 py-1.5 text-sm font-semibold text-white hover:bg-primary-700 disabled:opacity-50">Guardar</button>
                          <button type="button" (click)="resetAllChanges()" class="rounded-lg border border-neutral-300 bg-white px-4 py-1.5 text-sm font-semibold text-neutral-700 hover:bg-neutral-50">Descartar</button>
                        </div>
                      }

                      <div class="mt-4 rounded-2xl border border-neutral-200 bg-white overflow-hidden">
                        <div class="overflow-x-auto">
                          <div class="min-w-[768px]">
                            <div class="grid grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)_minmax(0,1fr)_minmax(0,0.8fr)_minmax(0,0.5fr)_minmax(0,0.5fr)] gap-3 bg-neutral-50 px-4 py-2 text-xs font-semibold uppercase tracking-[0.15em] text-neutral-500 rounded-t-2xl">
                              <span>Nombre</span>
                              <span>Prueba</span>
                              <span title="Club al que pertenece el jugador" class="cursor-help">Club</span>
                              <span title="DA = Aceptación Directa · WC = Wildcard · Q = Clasificado · LL = Perdedor Afortunado" class="cursor-help">Tipo Acceso</span>
                              <span class="text-right">Puntos</span>
                              <span class="text-center">Pago</span>
                            </div>

                            <div class="divide-y divide-neutral-100 bg-white rounded-b-2xl">
                              @for (player of tournamentInscriptionPlayers(); track player.inscriptionId + player.firstName + player.lastName) {
                                <div class="grid grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)_minmax(0,1fr)_minmax(0,0.8fr)_minmax(0,0.5fr)_minmax(0,0.5fr)] items-center gap-3 px-4 py-2 text-sm last:rounded-b-2xl">
                                  <div class="min-w-0">
                                    @if (isTournamentAdmin() && player.playerSource === 'MANUAL') {
                                      <div class="flex flex-col gap-1">
                                        <input
                                          type="text"
                                          class="w-full rounded-lg border border-neutral-300 bg-white px-2 py-1 text-xs focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                                          [ngModel]="getEditedFirstName(player)"
                                          (ngModelChange)="setEditedFirstName(player, $event)"
                                          placeholder="Nombre"
                                        />
                                        <input
                                          type="text"
                                          class="w-full rounded-lg border border-neutral-300 bg-white px-2 py-1 text-xs focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                                          [ngModel]="getEditedLastName(player)"
                                          (ngModelChange)="setEditedLastName(player, $event)"
                                          placeholder="Apellido"
                                        />
                                        <div class="mt-1 flex items-center gap-1.5 flex-wrap">
                                          <button
                                            type="button"
                                            (click)="generateInvitation(player)"
                                            [disabled]="isGeneratingInvitation(player.inscriptionId)"
                                            class="inline-flex items-center gap-1 rounded bg-neutral-100 hover:bg-neutral-200 text-[10px] font-semibold text-neutral-700 px-2 py-0.5 transition-colors disabled:opacity-50"
                                            [title]="'Generar enlace de invitación para que el jugador se registre en la plataforma'"
                                          >
                                            @if (isGeneratingInvitation(player.inscriptionId)) {
                                              <span>Generando...</span>
                                            } @else if (getInvitationUrl(player.inscriptionId)) {
                                              <svg class="h-3 w-3 text-emerald-600 animate-bounce" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                                              </svg>
                                              <span class="text-emerald-700">¡Copiado! Recopiar</span>
                                            } @else {
                                              <svg class="h-3 w-3 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                                              </svg>
                                              <span>Invitar</span>
                                            }
                                          </button>
                                        </div>
                                      </div>
                                    } @else {
                                      <p class="truncate font-semibold text-neutral-900">{{ player.firstName }} {{ player.lastName }}</p>
                                      <p class="truncate text-xs text-neutral-500">{{ getInscriptionGenderLabel(player.gender) }} · {{ getPlayerSourceLabel(player.playerSource) }}</p>
                                    }
                                  </div>
                                  <div class="min-w-0">
                                    @if (isTournamentAdmin()) {
                                      <select
                                        class="w-full rounded-lg border border-neutral-300 bg-white px-2 py-1 text-xs focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                                        [ngModel]="getEditedEventId(player)"
                                        (ngModelChange)="setEditedEventId(player, $event)"
                                      >
                                        @for (event of manualPlayerEventOptions(); track event.eventId) {
                                          <option [value]="event.eventId">{{ event.eventName }}</option>
                                        }
                                      </select>
                                    } @else {
                                      <p class="truncate text-xs text-neutral-700">{{ player.eventName }}</p>
                                      <p class="truncate text-xs text-neutral-400">{{ player.category }}</p>
                                    }
                                  </div>
                                  @if (isTournamentAdmin()) {
                                    <div class="min-w-0">
                                      <app-club-autocomplete
                                        placeholder="Club..."
                                        [ngModel]="getEditedClub(player)"
                                        (ngModelChange)="setEditedClub(player, $event)"
                                        (clubSelected)="onClubSelected(player, $event)"
                                      ></app-club-autocomplete>
                                    </div>
                                    <div class="min-w-0">
                                      <select
                                        class="w-full rounded-lg border border-neutral-300 bg-white px-2 py-1 text-xs focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                                        [ngModel]="getEditedEntryStatus(player)"
                                        (ngModelChange)="setEditedEntryStatus(player, $event)"
                                      >
                                        <option value="">-</option>
                                        <option value="DIRECT_ACCEPTANCE">DA</option>
                                        <option value="WILDCARD">WC</option>
                                        <option value="QUALIFIER">Q</option>
                                        <option value="LUCKY_LOSER">LL</option>
                                      </select>
                                    </div>
                                    <div class="flex items-center justify-end">
                                      <input type="number" min="0" class="w-20 rounded-lg border border-neutral-300 bg-white px-2 py-1 text-right text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500" [ngModel]="getEditedPoints(player)" (ngModelChange)="setEditedPoints(player, $event)" />
                                    </div>
                                  } @else {
                                    <div class="min-w-0">
                                      <p class="truncate text-xs text-neutral-700">{{ player.club || '-' }}</p>
                                    </div>
                                    <div class="min-w-0">
                                      <span class="inline-flex rounded-full border border-neutral-200 bg-neutral-50 px-2 py-0.5 text-[11px] font-semibold text-neutral-600">{{ getEntryStatusLabel(player.entryStatus) }}</span>
                                    </div>
                                    <div class="text-right">
                                      @if (player.points != null) {
                                        <span class="font-semibold text-neutral-900">{{ player.points }}</span>
                                      } @else {
                                        <span class="text-neutral-400">-</span>
                                      }
                                    </div>
                                  }
                                  <div class="text-center">
                                    @if (isTournamentAdmin()) {
                                      <select
                                        class="w-full rounded-lg border border-neutral-300 bg-white px-1 py-0.5 text-[11px] focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                                        [ngModel]="getEditedPaymentStatus(player)"
                                        (ngModelChange)="setEditedPaymentStatus(player, $event)"
                                      >
                                        <option value="PENDING">Pendiente</option>
                                        <option value="PAID">Pagado</option>
                                      </select>
                                    } @else {
                                      @if (player.paymentStatus === 'PAID') {
                                        <span class="inline-flex items-center rounded-full bg-emerald-100 px-2 py-0.5 text-[11px] font-semibold text-emerald-700">Pagado</span>
                                      } @else if (player.paymentStatus === 'PENDING') {
                                        <span class="inline-flex items-center rounded-full bg-amber-100 px-2 py-0.5 text-[11px] font-semibold text-amber-700">Pendiente</span>
                                      } @else {
                                        <span class="inline-flex items-center rounded-full bg-neutral-100 px-2 py-0.5 text-[11px] font-semibold text-neutral-500">-</span>
                                      }
                                    }
                                  </div>
                                </div>
                              }
                            </div>
                          </div>
                        </div>
                      </div>
                    }
                  }
                }
              </div>

              @if (isTournamentAdmin()) {
                <div class="mt-8 border-t border-neutral-200 pt-8">
                  <div class="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
                    <div>
                      <p class="text-xs font-semibold uppercase tracking-[0.2em] text-primary-600">Árbitros</p>
                      <h3 class="mt-2 text-xl font-bold text-neutral-900">Habilitar árbitros</h3>
                      <p class="mt-2 text-sm text-neutral-600">Asigna árbitros al torneo para que puedan registrar resultados de partidos.</p>
                    </div>
                  </div>

                  <div class="mt-4 rounded-3xl border border-neutral-200 bg-neutral-50 p-5 sm:p-6">
                    <div class="flex flex-col gap-3 sm:flex-row sm:items-end">
                      <label class="block flex-1">
                        <span class="mb-1 block text-sm font-medium text-neutral-700">Buscar árbitro</span>
                        <input
                          type="search"
                          class="w-full rounded-xl border border-neutral-300 px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                          [ngModel]="umpireSearchQuery()"
                          (ngModelChange)="onUmpireSearchQueryChange($event)"
                          name="umpireSearchQuery"
                          placeholder="Nombre, apellido o email exacto"
                          autocomplete="off"
                        />
                        <p class="mt-2 text-xs text-neutral-500">Escribe al menos 2 caracteres para buscar, o el email exacto del árbitro.</p>
                      </label>
                      <label class="block sm:w-48">
                        <span class="mb-1 block text-sm font-medium text-neutral-700">Rol</span>
                        <select
                          class="w-full rounded-xl border border-neutral-300 px-3 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                          [ngModel]="umpireSearchRoleFilter()"
                          (ngModelChange)="onUmpireRoleFilterChange($event)"
                          name="umpireRoleFilter"
                        >
                          <option value="ALL">Todos (Árbitros y Organizadores)</option>
                          <option value="UMPIRE">Solo Árbitros</option>
                          <option value="ORGANIZER">Solo Organizadores</option>
                        </select>
                      </label>
                    </div>

                    @if (isSearchingUmpires()) {
                      <div class="mt-4 rounded-2xl border border-dashed border-primary-200 bg-primary-50 px-4 py-3 text-sm text-primary-700">
                        Buscando árbitros...
                      </div>
                    }

                    @if (umpireSearchResults().length > 0) {
                      <div class="mt-4">
                        <p class="text-sm font-medium text-neutral-700">{{ umpireSearchResults().length }} resultado(s) encontrado(s)</p>
                        <div class="mt-3 grid gap-3">
                          @for (umpire of umpireSearchResults(); track umpire.id) {
                            <div class="flex items-center justify-between rounded-2xl border border-neutral-200 bg-white px-4 py-3 transition-all hover:-translate-y-0.5 hover:shadow-sm">
                              <div>
                                <p class="font-semibold text-neutral-900">{{ umpire.firstName }} {{ umpire.lastName }}</p>
                                <p class="mt-1 text-xs text-neutral-500">{{ umpire.email }}</p>
                              </div>
                              <button
                                type="button"
                                class="rounded-full bg-primary-500 px-4 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-primary-600 disabled:opacity-50"
                                [disabled]="isAddingUmpire()"
                                (click)="addUmpire(umpire)"
                              >
                                {{ isAddingUmpire() ? 'Añadiendo...' : 'Habilitar' }}
                              </button>
                            </div>
                          }
                        </div>
                      </div>
                    } @else if (!isSearchingUmpires() && umpireSearchQuery().trim().length >= 2 && !umpireSearchError()) {
                      <div class="mt-4 rounded-2xl border border-dashed border-neutral-300 bg-neutral-50 px-4 py-3 text-sm text-neutral-600">
                        No se han encontrado usuarios con ese criterio. Prueba con otro nombre, apellido o email exacto.
                      </div>
                    }

                    @if (umpireSearchError()) {
                      <div class="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                        {{ umpireSearchError() }}
                      </div>
                    }
                  </div>

                  <div class="mt-6">
                    <div class="flex items-center justify-between">
                      <p class="text-sm font-semibold text-neutral-700">Árbitros asignados</p>
                      <span class="rounded-full bg-white px-3 py-1 text-xs font-semibold uppercase tracking-widest text-neutral-600">
                        {{ tournamentUmpires().length }} asignado(s)
                      </span>
                    </div>

                    @if (isLoadingTournamentUmpires()) {
                      <div class="mt-3 rounded-2xl border border-neutral-200 bg-neutral-50 p-4 text-sm text-neutral-600">
                        Cargando árbitros...
                      </div>
                    } @else if (tournamentUmpires().length === 0) {
                      <div class="mt-3 rounded-2xl border border-dashed border-neutral-300 bg-neutral-50 px-4 py-3 text-sm text-neutral-600">
                        No hay árbitros asignados a este torneo.
                      </div>
                    } @else {
                      <div class="mt-3 grid gap-3">
                        @for (umpire of tournamentUmpires(); track umpire.id) {
                          <div class="flex items-center justify-between rounded-2xl border border-neutral-200 bg-white px-4 py-3">
                            <div>
                              <p class="font-semibold text-neutral-900">{{ getUmpireDisplayName(umpire) }}</p>
                              <p class="mt-1 text-xs text-neutral-500">Asignado el {{ umpire.assignedAt | date:'dd/MM/yyyy HH:mm' }}</p>
                            </div>
                            <button
                              type="button"
                              class="rounded-full border border-red-200 bg-red-50 px-3 py-1.5 text-xs font-semibold text-red-700 transition-colors hover:border-red-300 hover:bg-red-100"
                              [disabled]="isRemovingUmpireId() === umpire.umpireId"
                              (click)="removeUmpire(umpire)"
                            >
                              {{ isRemovingUmpireId() === umpire.umpireId ? 'Eliminando...' : 'Eliminar' }}
                            </button>
                          </div>
                        }
                      </div>
                    }
                  </div>

                  @if (umpireSuccessMessage()) {
                    <div class="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                      {{ umpireSuccessMessage() }}
                    </div>
                  }

                  @if (umpireErrorMessage()) {
                    <div class="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                      {{ umpireErrorMessage() }}
                    </div>
                  }
                </div>
              }
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

                  @if (isTournamentAdmin() && isCourtsPanelExpanded()) {
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

                @if (isTournamentAdmin() && isCourtsPanelExpanded() && selectedCourt()) {
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

              @if (isTournamentAdmin()) {
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
                  <!-- VISTA ESCRITORIO: todos los filtros en línea -->
                  <div class="mt-4 hidden md:grid gap-3 rounded-xl border border-neutral-200 bg-neutral-50 p-4 md:grid-cols-2 lg:grid-cols-[1.5fr_1.2fr_0.8fr_0.9fr_1fr_0.8fr_auto]">
                    <label class="text-xs font-semibold uppercase tracking-widest text-neutral-600">
                      Jugador
                      <input
                        type="text"
                        class="mt-1 w-full rounded-lg border border-neutral-300 bg-white px-3 py-2 text-sm font-normal normal-case tracking-normal text-neutral-800 focus:border-primary-500 focus:outline-none"
                        [ngModel]="matchSchedulePlayerFilter()"
                        (ngModelChange)="matchSchedulePlayerFilter.set($event)"
                        name="matchSchedulePlayerFilter"
                        placeholder="Buscar jugador..."
                      />
                    </label>

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

                  <!-- VISTA MÓVIL: solo jugador + botón de filtros drawer -->
                  <div class="mt-4 flex gap-3 md:hidden items-center">
                    <div class="relative flex-1 font-sans">
                      <input
                        type="text"
                        class="w-full rounded-lg border border-neutral-300 bg-white pl-3 pr-10 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                        [ngModel]="matchSchedulePlayerFilter()"
                        (ngModelChange)="matchSchedulePlayerFilter.set($event)"
                        name="m-matchSchedulePlayerFilter"
                        placeholder="Buscar jugador..."
                      />
                      @if (matchSchedulePlayerFilter()) {
                        <button
                          type="button"
                          (click)="matchSchedulePlayerFilter.set('')"
                          class="absolute inset-y-0 right-0 flex items-center pr-3 text-neutral-400 hover:text-neutral-600"
                        >
                          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                          </svg>
                        </button>
                      }
                    </div>
                    <button
                      type="button"
                      (click)="matchScheduleFilterPanelOpen.set(true)"
                      class="relative inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-neutral-900 text-white shadow-sm transition hover:bg-neutral-800"
                      title="Filtros de partidos"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <line x1="4" y1="21" x2="4" y2="14"></line>
                        <line x1="4" y1="10" x2="4" y2="3"></line>
                        <line x1="12" y1="21" x2="12" y2="12"></line>
                        <line x1="12" y1="8" x2="12" y2="3"></line>
                        <line x1="20" y1="21" x2="20" y2="16"></line>
                        <line x1="20" y1="12" x2="20" y2="3"></line>
                        <line x1="1" y1="14" x2="7" y2="14"></line>
                        <line x1="9" y1="8" x2="15" y2="8"></line>
                        <line x1="17" y1="16" x2="23" y2="16"></line>
                      </svg>
                      @if (activeMatchScheduleFilterCount() > 0) {
                        <span class="absolute -right-1 -top-1 flex h-5 w-5 items-center justify-center rounded-full bg-primary-600 text-[10px] font-bold text-white">
                          {{ activeMatchScheduleFilterCount() }}
                        </span>
                      }
                    </button>
                  </div>

                  @if (actionError()) {
                    <div class="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                      {{ actionError() }}
                    </div>
                  }

                  @if (filteredTournamentMatchScheduleRows().length === 0) {
                    <div class="mt-4 rounded-xl border border-dashed border-neutral-300 bg-neutral-50 p-4 text-sm text-neutral-600">
                      No hay partidos que coincidan con los filtros seleccionados.
                    </div>
                  } @else {
                  <!-- VISTA ESCRITORIO: tabla 5 columnas -->
                  <div class="mt-4 hidden overflow-x-auto md:block">
                    <table class="w-full text-sm">
                      <thead class="bg-neutral-100">
                        <tr>
                          <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">
                            <button type="button" class="inline-flex items-center gap-1 transition-colors hover:text-primary-700" (click)="setMatchScheduleSort('event')">
                              Prueba / Partido <span>{{ getMatchScheduleSortIndicator('event') }}</span>
                            </button>
                          </th>
                          <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">
                            <button type="button" class="inline-flex items-center gap-1 transition-colors hover:text-primary-700" (click)="setMatchScheduleSort('scheduledAt')">
                              Tipo / Inicio <span>{{ getMatchScheduleSortIndicator('scheduledAt') }}</span>
                            </button>
                          </th>
                          <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">
                            <button type="button" class="inline-flex items-center gap-1 transition-colors hover:text-primary-700" (click)="setMatchScheduleSort('court')">
                              Pista <span>{{ getMatchScheduleSortIndicator('court') }}</span>
                            </button>
                          </th>
                          <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">Cascada</th>
                          <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">Acciones</th>
                        </tr>
                      </thead>
                      <tbody>
                        @for (row of filteredTournamentMatchScheduleRows(); track row.match.id) {
                          <tr class="border-b border-neutral-200 align-top hover:bg-neutral-50/80 cursor-pointer transition-colors" (click)="onMatchSelected(row.match)">
                            <td class="px-3 py-3" title="Hacer clic para ver detalles o registrar resultados">
                              <p class="font-medium text-neutral-900">{{ row.eventLabel }}</p>
                              <p class="mt-0.5 text-xs text-neutral-500">{{ row.drawLabel }}</p>
                              <p class="mt-1 text-xs font-semibold text-primary-700">R{{ row.match.roundNumber }}: {{ row.firstPlayerName }} vs {{ row.secondPlayerName }}</p>
                            </td>
                            <td class="px-3 py-3" (click)="$event.stopPropagation()">
                              <select
                                class="mb-1.5 w-full rounded-lg border border-neutral-300 bg-white px-2 py-1.5 text-xs text-neutral-800 focus:border-primary-500 focus:outline-none"
                                [ngModel]="getMatchScheduleDraft(row.match).scheduleTimeType"
                                (ngModelChange)="updateMatchScheduleType(row.match, $event)"
                                [name]="'scheduleType-' + row.match.id"
                              >
                                <option value="EXACT">A esta hora</option>
                                <option value="NOT_BEFORE">No antes de</option>
                              </select>
                              <input
                                type="datetime-local"
                                class="w-full rounded-lg border border-neutral-300 px-2 py-1.5 text-xs text-neutral-800 focus:border-primary-500 focus:outline-none"
                                [ngModel]="getMatchScheduleDraft(row.match).scheduledAt"
                                (ngModelChange)="updateMatchScheduleDate(row.match, $event)"
                                [name]="'scheduledAt-' + row.match.id"
                              />
                            </td>
                            <td class="px-3 py-3" (click)="$event.stopPropagation()">
                              <select
                                class="w-full rounded-lg border border-neutral-300 bg-white px-2 py-1.5 text-xs text-neutral-800 focus:border-primary-500 focus:outline-none"
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
                            <td class="px-3 py-3" (click)="$event.stopPropagation()">
                              <label class="inline-flex items-center gap-1.5 text-xs font-semibold text-neutral-700 cursor-pointer">
                                <input
                                  type="checkbox"
                                  class="h-4 w-4 rounded border-neutral-300 text-primary-600 focus:ring-primary-500"
                                  [ngModel]="getMatchScheduleDraft(row.match).cascade"
                                  (ngModelChange)="updateMatchScheduleCascade(row.match, $event)"
                                  [name]="'cascade-' + row.match.id"
                                />
                                Sí
                              </label>
                            </td>
                            <td class="px-3 py-3 whitespace-nowrap" (click)="$event.stopPropagation()">
                              <div class="flex items-center gap-1.5">
                                <button
                                  type="button"
                                  class="rounded-lg bg-neutral-900 px-3 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-neutral-800 disabled:cursor-not-allowed disabled:bg-neutral-300"
                                  [disabled]="!canSaveMatchSchedule(row.match)"
                                  (click)="saveMatchSchedule(row.match)"
                                >
                                  {{ savingScheduleMatchId() === row.match.id ? '...' : 'Guardar' }}
                                </button>
                                <button
                                  type="button"
                                  class="rounded-lg border border-amber-300 bg-amber-50 px-3 py-1.5 text-xs font-semibold text-amber-700 transition-colors hover:bg-amber-100 disabled:cursor-not-allowed disabled:bg-neutral-200 disabled:text-neutral-400"
                                  [disabled]="!isTournamentAdmin() || tournament()?.status === 'COMPLETED' || tournament()?.status === 'CANCELLED'"
                                  (click)="openSwapScheduleModal(row.match)"
                                >
                                  Intercambiar
                                </button>
                              </div>
                            </td>
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>

                  <!-- VISTA MÓVIL: tarjetas expandibles -->
                  <div class="mt-4 space-y-2 md:hidden">
                    @for (row of filteredTournamentMatchScheduleRows(); track row.match.id) {
                      <div class="rounded-xl border border-neutral-200 bg-white shadow-sm overflow-hidden">
                        <!-- Cabecera de tarjeta (siempre visible) -->
                        <button
                          type="button"
                          class="flex w-full items-start justify-between gap-3 px-4 py-3 text-left"
                          (click)="toggleScheduleCard(row.match.id)"
                        >
                          <div class="min-w-0 flex-1">
                            <p class="text-xs font-semibold uppercase tracking-wide text-primary-600">{{ row.eventLabel }}</p>
                            <p class="mt-0.5 text-sm font-semibold text-neutral-900">{{ row.firstPlayerName }} vs {{ row.secondPlayerName }}</p>
                            <p class="mt-0.5 text-xs text-neutral-500">{{ row.drawLabel }} · Ronda {{ row.match.roundNumber }}</p>
                            <div class="mt-1 flex flex-wrap gap-2 items-center">
                              @if (getMatchScheduleDraft(row.match).scheduledAt) {
                                <span class="text-xs font-medium text-neutral-600">📅 {{ getMatchScheduleDraft(row.match).scheduledAt | date:'dd/MM HH:mm' }}</span>
                              }
                              @if (row.match.result) {
                                <span class="rounded bg-green-50 px-2 py-0.5 text-[10px] font-bold text-green-700 ring-1 ring-green-200">
                                  ✅ {{ row.match.result }}
                                </span>
                              } @else {
                                <span class="rounded px-2 py-0.5 text-[10px] font-bold"
                                  [class.bg-neutral-100]="row.match.status === 'PENDING'"
                                  [class.text-neutral-600]="row.match.status === 'PENDING'"
                                  [class.bg-sky-50]="row.match.status === 'IN_PROGRESS'"
                                  [class.text-sky-700]="row.match.status === 'IN_PROGRESS'"
                                  [class.ring-1]="true"
                                  [class.ring-neutral-200]="row.match.status === 'PENDING'"
                                  [class.ring-sky-200]="row.match.status === 'IN_PROGRESS'"
                                >
                                  {{ row.match.status === 'PENDING' ? 'Pendiente' : (row.match.status === 'IN_PROGRESS' ? 'En juego' : row.match.status) }}
                                </span>
                              }
                            </div>
                          </div>
                          <span class="mt-1 shrink-0 text-neutral-400 text-sm">{{ isScheduleCardOpen(row.match.id) ? '▲' : '▼' }}</span>
                        </button>

                        <!-- Formulario expandible -->
                        @if (isScheduleCardOpen(row.match.id)) {
                          <div class="border-t border-neutral-100 px-4 py-3 space-y-3 bg-neutral-50" (click)="$event.stopPropagation()">
                            <div class="grid grid-cols-2 gap-2">
                              <div>
                                <label class="mb-1 block text-[10px] font-semibold uppercase tracking-wide text-neutral-500">Tipo</label>
                                <select
                                  class="w-full rounded-lg border border-neutral-300 bg-white px-2 py-2 text-xs text-neutral-800 focus:border-primary-500 focus:outline-none"
                                  [ngModel]="getMatchScheduleDraft(row.match).scheduleTimeType"
                                  (ngModelChange)="updateMatchScheduleType(row.match, $event)"
                                  [name]="'m-scheduleType-' + row.match.id"
                                >
                                  <option value="EXACT">A esta hora</option>
                                  <option value="NOT_BEFORE">No antes de</option>
                                </select>
                              </div>
                              <div>
                                <label class="mb-1 block text-[10px] font-semibold uppercase tracking-wide text-neutral-500">Pista</label>
                                <select
                                  class="w-full rounded-lg border border-neutral-300 bg-white px-2 py-2 text-xs text-neutral-800 focus:border-primary-500 focus:outline-none"
                                  [ngModel]="getMatchScheduleDraft(row.match).courtId"
                                  (ngModelChange)="updateMatchScheduleCourt(row.match, $event)"
                                  [name]="'m-court-' + row.match.id"
                                >
                                  <option value="">Selecciona</option>
                                  @for (court of courts(); track court.id) {
                                    <option [value]="court.id">{{ court.name }}</option>
                                  }
                                </select>
                              </div>
                            </div>
                            <div>
                              <label class="mb-1 block text-[10px] font-semibold uppercase tracking-wide text-neutral-500">Fecha y hora de inicio</label>
                              <input
                                type="datetime-local"
                                class="w-full rounded-lg border border-neutral-300 px-2 py-2 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                                [ngModel]="getMatchScheduleDraft(row.match).scheduledAt"
                                (ngModelChange)="updateMatchScheduleDate(row.match, $event)"
                                [name]="'m-scheduledAt-' + row.match.id"
                              />
                            </div>
                            <label class="inline-flex items-center gap-2 text-xs font-semibold text-neutral-700 cursor-pointer">
                              <input
                                type="checkbox"
                                class="h-4 w-4 rounded border-neutral-300 text-primary-600 focus:ring-primary-500"
                                [ngModel]="getMatchScheduleDraft(row.match).cascade"
                                (ngModelChange)="updateMatchScheduleCascade(row.match, $event)"
                                [name]="'m-cascade-' + row.match.id"
                              />
                              Replanificar siguientes automáticamente
                            </label>
                            <div class="flex flex-col gap-2 pt-1">
                              <div class="flex gap-2">
                                <button
                                  type="button"
                                  class="flex-1 rounded-xl bg-neutral-900 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-neutral-800 disabled:cursor-not-allowed disabled:bg-neutral-300"
                                  [disabled]="!canSaveMatchSchedule(row.match)"
                                  (click)="saveMatchSchedule(row.match)"
                                >
                                  {{ savingScheduleMatchId() === row.match.id ? 'Guardando...' : 'Guardar' }}
                                </button>
                                <button
                                  type="button"
                                  class="flex-1 rounded-xl border border-amber-300 bg-amber-50 px-4 py-2 text-sm font-semibold text-amber-700 transition-colors hover:bg-amber-100 disabled:cursor-not-allowed disabled:bg-neutral-200 disabled:text-neutral-400"
                                  [disabled]="!isTournamentAdmin() || tournament()?.status === 'COMPLETED' || tournament()?.status === 'CANCELLED'"
                                  (click)="openSwapScheduleModal(row.match)"
                                >
                                  Intercambiar
                                </button>
                              </div>
                              <button
                                type="button"
                                class="w-full rounded-xl border border-neutral-300 bg-white px-4 py-2 text-sm font-semibold text-neutral-700 transition-colors hover:bg-neutral-50"
                                (click)="onMatchSelected(row.match)"
                              >
                                ✏️ Registrar / Editar resultado
                              </button>
                            </div>
                          </div>
                        }
                      </div>
                    }
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
                  <div class="mt-4 divide-y divide-neutral-100">
                  @for (event of tournament()!.events!; track event.eventId) {
                    <div class="py-6 first:pt-2 last:pb-0">
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

                      <div class="mt-4">
                        @if (event.stages && event.stages.length > 0) {
                          <app-stages
                            [stagesInput]="event.stages"
                            [tournamentIdInput]="tournament()!.id"
                            [participantNamesInput]="participantNamesByInscriptionId()"
                            [participantOrderInput]="participantOrderByInscriptionId()"
                            [courtsInput]="courts()"
                            [canManageInput]="canManageMatches()"
                            [tournamentStatusInput]="tournament()!.status"
                            [tournamentNameInput]="tournament()!.formalName"
                            [categoryNameInput]="getCategoryLabel(event.categoryId) + ' - ' + getGenderLabelForString(event.gender)"
                            [generatingDrawsForStageIdInput]="generatingDrawsStageId()"
                            [drawGenerationFeedbackInput]="drawGenerationFeedbackByStageId()"
                            [setsPerMatch]="tournament()!.setsPerMatch ?? 3"
                            [decisiveTiebreakPoints]="tournament()!.decisiveTiebreakPoints ?? 7"
                            [gamesPerSet]="tournament()!.gamesPerSet ?? 6"
                            (generateDraws)="onGenerateDraws($event, event.eventId!)"
                            (matchSelected)="onMatchSelected($event)"
                            (matchResultSaved)="onMatchResultSaved($event)"
                            (matchScheduleSaved)="onMatchScheduleSaved($event)"
                            (playersSwapped)="onPlayersSwapped($event)"
                            (swapScheduleClicked)="openSwapScheduleModal($event)"
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

    @if (swapModalOpen()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" (click)="closeSwapModal()">
        <div class="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl" (click)="$event.stopPropagation()">
          <div class="flex items-center justify-between">
            <h3 class="text-lg font-semibold text-neutral-900">Intercambiar programación</h3>
            <button (click)="closeSwapModal()" class="text-neutral-400 hover:text-neutral-600">&times;</button>
          </div>
          @if (swapModalError()) {
            <p class="mt-2 text-sm text-red-600">{{ swapModalError() }}</p>
          }
          <p class="mt-2 text-sm text-neutral-600">
            Partido seleccionado: <strong>{{ swapModalSourceLabel() }}</strong>
          </p>
          <input
            type="search"
            [ngModel]="swapModalSearch()"
            (ngModelChange)="swapModalSearch.set($event)"
            placeholder="Buscar por nombre o categoría..."
            class="mt-4 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none"
          />
          <div class="mt-4 max-h-72 overflow-y-auto">
            @for (row of swapModalFilteredMatches(); track row.match.id) {
              <div
                class="flex cursor-pointer items-center justify-between rounded-lg border border-neutral-200 px-3 py-2 hover:bg-primary-50"
                (click)="selectSwapTarget(row)"
              >
                <div>
                  <p class="text-sm font-medium text-neutral-900">{{ row.firstPlayerName }} vs {{ row.secondPlayerName }}</p>
                  <p class="text-xs text-neutral-500">{{ row.eventLabel }} - {{ row.drawLabel }}</p>
                </div>
                <span class="text-xs text-neutral-400">Ronda {{ row.match.roundNumber }}</span>
              </div>
            } @empty {
              <p class="text-sm text-neutral-500">No se encontraron partidos.</p>
            }
          </div>
        </div>
      </div>
    }

    <app-match-detail-modal
      #matchModal
      [matchInput]="selectedMatch()"
      [participantNamesInput]="participantNamesByInscriptionId()"
      [courtsInput]="courts()"
      [canManageInput]="canManageMatches()"
      [setsPerMatch]="tournament()?.setsPerMatch ?? 3"
      [decisiveTiebreakPoints]="tournament()?.decisiveTiebreakPoints ?? 7"
      [gamesPerSet]="tournament()?.gamesPerSet ?? 6"
      (saveResult)="onMatchResultSaved($event)"
      (saveSchedule)="onMatchScheduleSaved($event)"
      (close)="onModalClose()"
    ></app-match-detail-modal>

    @if (matchScheduleFilterPanelOpen()) {
      <div class="fixed inset-0 z-50 flex justify-end font-sans">
        <div class="absolute inset-0 bg-black/40" (click)="matchScheduleFilterPanelOpen.set(false)"></div>
        <div class="relative flex h-full w-full max-w-md flex-col bg-white shadow-xl">
          <div class="flex items-center justify-between border-b border-neutral-200 px-6 py-4">
            <h2 class="text-lg font-bold text-neutral-950">Filtros de partidos</h2>
            <button
              type="button"
              (click)="matchScheduleFilterPanelOpen.set(false)"
              class="flex h-9 w-9 items-center justify-center rounded-lg text-neutral-500 transition hover:bg-neutral-100 hover:text-neutral-900"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>

          <div class="flex-1 overflow-y-auto px-6 py-5">
            <form (ngSubmit)="$event.preventDefault()" class="space-y-5">
              <label class="block">
                <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Prueba</span>
                <select
                  class="h-11 w-full rounded-lg border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition focus:border-primary-500"
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

              <label class="block">
                <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Ronda</span>
                <select
                  class="h-11 w-full rounded-lg border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition focus:border-primary-500"
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

              <label class="block">
                <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Fecha</span>
                <input
                  type="date"
                  class="h-11 w-full rounded-lg border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition focus:border-primary-500"
                  [ngModel]="matchScheduleDateFilter()"
                  (ngModelChange)="matchScheduleDateFilter.set($event)"
                  name="matchScheduleDateFilter"
                />
              </label>

              <label class="block">
                <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Pista</span>
                <select
                  class="h-11 w-full rounded-lg border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition focus:border-primary-500"
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

              <label class="block">
                <span class="mb-1 block text-xs font-semibold uppercase tracking-widest text-neutral-500">Tipo</span>
                <select
                  class="h-11 w-full rounded-lg border border-neutral-300 bg-white px-3 text-sm text-neutral-900 outline-none transition focus:border-primary-500"
                  [ngModel]="matchScheduleProfessionalFilter()"
                  (ngModelChange)="matchScheduleProfessionalFilter.set($event)"
                  name="matchScheduleProfessionalFilter"
                >
                  <option value="">Todos</option>
                  <option value="PRO">PRO</option>
                </select>
              </label>
            </form>
          </div>

          <div class="flex gap-3 border-t border-neutral-200 px-6 py-4">
            <button
              type="button"
              (click)="clearMatchScheduleFilters()"
              class="h-11 flex-1 rounded-lg border border-neutral-300 bg-white px-4 text-sm font-semibold text-neutral-700 transition hover:bg-neutral-50"
            >
              Limpiar
            </button>
            <button
              type="button"
              (click)="matchScheduleFilterPanelOpen.set(false)"
              class="h-11 flex-1 rounded-lg bg-primary-600 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700"
            >
              Aplicar
            </button>
          </div>
        </div>
      </div>
    }
  `
})
export class TournamentDetailComponent implements OnInit, OnDestroy, AfterViewInit {
  private readonly route = inject(ActivatedRoute);
  private readonly tournamentService = inject(TournamentService);
  private readonly invitationService = inject(InvitationService);
  private readonly tournamentLiveUpdatesService = inject(TournamentLiveUpdatesService);
  private readonly memberService = inject(MemberService);
  private readonly authService = inject(AuthService);
  private readonly personService = inject(PersonService);
  private readonly proPlayerService = inject(ProPlayerService);
  private readonly referenceDataService = inject(ReferenceDataService);
  private readonly fb = inject(FormBuilder);

  readonly isGeneratingInvitationMap = signal<Record<string, boolean>>({});
  readonly invitationUrlMap = signal<Record<string, string>>({});

  @HostListener('document:click')
  onDocumentClick(): void {
    this.showHeaderStatusDropdown.set(false);
  }

  private mapInstance?: any;

  readonly eventGenderOptions: TournamentEventGender[] = ['MALE', 'FEMALE', 'MIXED'];
  readonly stageOptions: TournamentStageType[] = ['SINGLE_ELIMINATION', 'ROUND_ROBIN', 'DOUBLE_ELIMINATION', 'CONSOLATION'];
  readonly surfaceOptions: TournamentSurfaceCategory[] = ['CLAY', 'HARD', 'GRASS', 'CARPET'];
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
  readonly isUpdatingStatus = signal(false);
  readonly showHeaderStatusDropdown = signal(false);
  readonly isProfileComplete = signal(false);
  readonly isLoggedIn = computed(() => this.authService.currentRole !== null);
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
  readonly manualPlayerBirthDate = signal<string>('');
  readonly manualPlayerNationality = signal<string>('');
  readonly manualPlayerTennisId = signal<string>('');
  readonly manualPlayerClub = signal<string>('');
  readonly manualPlayerPoints = signal<string>('');
  readonly manualPlayerEntryStatus = signal<string>('');
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
  readonly openScheduleCardIds = signal<Set<string>>(new Set());
  readonly matchScheduleDrafts = signal<Record<string, MatchScheduleDraft>>({});
  readonly matchScheduleEventFilter = signal('');
  readonly matchScheduleRoundFilter = signal('');
  readonly matchScheduleDateFilter = signal('');
  readonly matchScheduleCourtFilter = signal('');
  readonly matchScheduleProfessionalFilter = signal('');
  readonly matchSchedulePlayerFilter = signal('');
  readonly matchScheduleFilterPanelOpen = signal(false);
  readonly activeMatchScheduleFilterCount = computed(() => {
    let count = 0;
    if (this.matchScheduleEventFilter()) count++;
    if (this.matchScheduleRoundFilter()) count++;
    if (this.matchScheduleDateFilter()) count++;
    if (this.matchScheduleCourtFilter()) count++;
    if (this.matchScheduleProfessionalFilter()) count++;
    return count;
  });
  readonly matchScheduleSortField = signal<MatchScheduleSortField>('scheduledAt');
  readonly matchScheduleSortDirection = signal<MatchScheduleSortDirection>('asc');

  readonly selectedMatch = signal<MatchResponse | null>(null);
  @ViewChild('matchModal') matchModal!: MatchDetailModalComponent;

  readonly swapModalOpen = signal(false);
  readonly swapModalSourceMatch = signal<MatchResponse | null>(null);
  readonly swapModalSourceLabel = signal('');
  readonly swapModalSearch = signal('');
  readonly swapModalError = signal<string | null>(null);
  readonly swapModalCandidateMatches = signal<TournamentMatchScheduleRow[]>([]);

  readonly swapModalFilteredMatches = computed(() => {
    const query = this.swapModalSearch().toLowerCase().trim();
    const sourceId = this.swapModalSourceMatch()?.id;
    return this.swapModalCandidateMatches().filter(row => {
      if (row.match.id === sourceId) return false;
      if (!query) return true;
      return (
        row.firstPlayerName.toLowerCase().includes(query) ||
        row.secondPlayerName.toLowerCase().includes(query) ||
        row.eventLabel.toLowerCase().includes(query) ||
        row.drawLabel.toLowerCase().includes(query)
      );
    });
  });

  readonly scheduleConfigDraft = signal<{ timeSlots: Array<{ startTime: string; endTime: string }>; matchDurationMinutes: number }>({
    timeSlots: [],
    matchDurationMinutes: 60
  });
  readonly isSavingScheduleConfig = signal(false);
  readonly scheduleConfigSuccess = signal<string | null>(null);
  readonly scheduleConfigError = signal<string | null>(null);
  readonly scheduleOverlapWarning = signal<string | null>(null);

  readonly editedPointsMap = signal<Record<string, number | null>>({});
  readonly editedClubMap = signal<Record<string, string>>({});
  readonly editedEntryStatusMap = signal<Record<string, string>>({});
  readonly editedPaymentStatusMap = signal<Record<string, string>>({});
  readonly editedEventIdMap = signal<Record<string, string>>({});
  readonly editedFirstNameMap = signal<Record<string, string>>({});
  readonly editedLastNameMap = signal<Record<string, string>>({});
  readonly isSavingPoints = signal(false);
  readonly isSavingDetails = signal(false);
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

  readonly umpireSearchQuery = signal<string>('');
  readonly umpireSearchResults = signal<TournamentUmpireSearchResponse[]>([]);
  readonly isSearchingUmpires = signal(false);
  readonly umpireSearchError = signal<string | null>(null);
  readonly isAddingUmpire = signal(false);
  readonly umpireSearchRoleFilter = signal<string>('ALL');
  readonly isRemovingUmpireId = signal<string | null>(null);
  readonly tournamentUmpires = signal<TournamentUmpireResponse[]>([]);
  readonly isLoadingTournamentUmpires = signal(false);
  readonly umpireSuccessMessage = signal<string | null>(null);
  readonly umpireErrorMessage = signal<string | null>(null);
  private umpireSearchDebounceHandle: ReturnType<typeof setTimeout> | null = null;
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

  readonly canEditGeneralInfo = computed(() => {
    const tournament = this.tournament();
    if (!tournament || !this.isTournamentAdmin()) {
      return false;
    }
    return tournament.status === 'DRAFT' || tournament.status === 'OPEN';
  });

  readonly isAssignedUmpire = computed(() => {
    const memberId = this.currentMemberId();
    if (!memberId) return false;
    return this.tournamentUmpires().some(u => u.umpireId === memberId);
  });

  readonly isTournamentAdmin = computed(() => this.isCreator() || this.isAssignedUmpire());
  readonly canManageMatches = computed(() => this.isTournamentAdmin());

  readonly isEditingGeneralInfo = signal(false);
  readonly isSavingGeneralInfo = signal(false);
  readonly generalInfoError = signal<string | null>(null);

  readonly generalInfoForm = this.fb.group({
    formalName: ['', [Validators.required, Validators.minLength(3)]],
    playStartDate: ['', Validators.required],
    playEndDate: ['', Validators.required],
    tournamentStartTime: ['09:00', Validators.required],
    inscriptionStartDate: ['', Validators.required],
    inscriptionEndDate: ['', Validators.required],
    surfaceCategory: ['CLAY' as TournamentSurfaceCategory, Validators.required],
    maxPlayers: [32, [Validators.required, Validators.min(2)]],
    location: ['', [Validators.required, Validators.minLength(3)]],
    locationLatitude: [null as number | null],
    locationLongitude: [null as number | null],
    locationPlaceId: [null as string | null],
    locationFormattedAddress: [null as string | null],
    setsPerMatch: [3, Validators.required],
    decisiveTiebreakPoints: [7, Validators.required],
    gamesPerSet: [6, Validators.required]
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

  private static readonly ALL_STATUSES: TournamentStatus[] = [
    'DRAFT', 'OPEN', 'CLOSED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'
  ];

  readonly allowedStatusTransitions = computed<TournamentStatus[]>(() => {
    const currentTournament = this.tournament();
    if (!currentTournament) {
      return [];
    }

    return TournamentDetailComponent.ALL_STATUSES.filter(s => s !== currentTournament.status);
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
    const playerFilter = this.matchSchedulePlayerFilter().toLowerCase().trim();

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

      if (playerFilter) {
        const first = row.firstPlayerName.toLowerCase();
        const second = row.secondPlayerName.toLowerCase();
        if (!first.includes(playerFilter) && !second.includes(playerFilter)) {
          return false;
        }
      }

      return true;
    });

    return rows.sort((left, right) => this.compareMatchScheduleRows(left, right));
  });

  readonly getSurfaceLabel = getTournamentSurfaceCategoryLabel;
  readonly getGenderLabel = getTournamentEventGenderLabel;
  readonly getStageLabel = getTournamentStageTypeLabel;

  getGamesPerSetLabel(gamesPerSet: number | null | undefined): string {
    const value = gamesPerSet ?? 6;
    const labels: Record<number, string> = {
      4: '4 juegos',
      5: '5 juegos',
      6: '6 juegos (estándar)'
    };
    return labels[value] ?? `${value} juegos`;
  }

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

    if (typeof window !== 'undefined' && window.sessionStorage) {
      const savedCourts = sessionStorage.getItem(`tournament_detail_courts_expanded_${tournamentId}`);
      if (savedCourts !== null) this.isCourtsPanelExpanded.set(savedCourts === 'true');

      const savedSchedule = sessionStorage.getItem(`tournament_detail_match_schedule_expanded_${tournamentId}`);
      if (savedSchedule !== null) this.isMatchSchedulePanelExpanded.set(savedSchedule === 'true');

      const savedManualPlayer = sessionStorage.getItem(`tournament_detail_manual_player_expanded_${tournamentId}`);
      if (savedManualPlayer !== null) this.isManualPlayerPanelExpanded.set(savedManualPlayer === 'true');

      const savedManualPlayerFilters = sessionStorage.getItem(`tournament_detail_manual_player_filters_expanded_${tournamentId}`);
      if (savedManualPlayerFilters !== null) this.isManualPlayerFiltersPanelExpanded.set(savedManualPlayerFilters === 'true');

      const savedRegistered = sessionStorage.getItem(`tournament_detail_registered_players_expanded_${tournamentId}`);
      if (savedRegistered !== null) this.isRegisteredPlayersPanelExpanded.set(savedRegistered === 'true');

      const savedEventsDraws = sessionStorage.getItem(`tournament_detail_events_draws_expanded_${tournamentId}`);
      if (savedEventsDraws !== null) this.isEventsDrawsPanelExpanded.set(savedEventsDraws === 'true');
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

    const tournamentId = this.tournament()?.id || this.route.snapshot.paramMap.get('id');
    if (tournamentId && typeof window !== 'undefined' && window.sessionStorage) {
      sessionStorage.setItem(`tournament_detail_section_${tournamentId}`, section);
    }

    if (section === 'overview') {
      setTimeout(() => this.tryInitMap(), 0);
    }

    if (section === 'inscriptions' && !this.tournamentInscriptions() && !this.isLoadingTournamentInscriptions()) {
      this.loadTournamentInscriptions();
    }
  }

  startEditGeneralInfo(): void {
    const tournament = this.tournament();
    if (!tournament) {
      return;
    }

    this.generalInfoForm.patchValue({
      formalName: tournament.formalName,
      playStartDate: tournament.playStartDate,
      playEndDate: tournament.playEndDate,
      tournamentStartTime: tournament.tournamentStartTime ?? '09:00',
      inscriptionStartDate: tournament.inscriptionStartDate,
      inscriptionEndDate: tournament.inscriptionEndDate,
      surfaceCategory: tournament.surfaceCategory,
      maxPlayers: tournament.maxPlayers,
      location: tournament.location,
      locationLatitude: tournament.locationLatitude ?? null,
      locationLongitude: tournament.locationLongitude ?? null,
      locationPlaceId: tournament.locationPlaceId ?? null,
      locationFormattedAddress: tournament.locationFormattedAddress ?? null,
      setsPerMatch: tournament.setsPerMatch ?? 3,
      decisiveTiebreakPoints: tournament.decisiveTiebreakPoints ?? 7,
      gamesPerSet: tournament.gamesPerSet ?? 6
    });

    this.generalInfoError.set(null);
    this.isEditingGeneralInfo.set(true);
  }

  cancelEditGeneralInfo(): void {
    this.isEditingGeneralInfo.set(false);
    this.generalInfoError.set(null);
    this.generalInfoForm.reset();
  }

  onEditLocationSelected(location: { name: string; latitude?: number | null; longitude?: number | null; placeId?: string | null; formattedAddress?: string | null }): void {
    this.generalInfoForm.patchValue({
      location: location.name,
      locationLatitude: location.latitude ?? null,
      locationLongitude: location.longitude ?? null,
      locationPlaceId: location.placeId ?? null,
      locationFormattedAddress: location.formattedAddress ?? null
    });
  }

  saveGeneralInfo(): void {
    if (!this.isTournamentAdmin() || this.generalInfoForm.invalid || this.isSavingGeneralInfo()) {
      return;
    }

    const currentTournament = this.tournament();
    if (!currentTournament) {
      return;
    }

    const formValue = this.generalInfoForm.getRawValue();

    const payload: TournamentGeneralInfoUpdateRequest = {};

    if (formValue.formalName !== currentTournament.formalName) {
      payload.formalName = formValue.formalName!;
    }
    if (formValue.playStartDate !== currentTournament.playStartDate) {
      payload.playStartDate = formValue.playStartDate!;
    }
    if (formValue.playEndDate !== currentTournament.playEndDate) {
      payload.playEndDate = formValue.playEndDate!;
    }
    if (formValue.tournamentStartTime !== (currentTournament.tournamentStartTime ?? '09:00')) {
      payload.tournamentStartTime = formValue.tournamentStartTime!;
    }
    if (formValue.inscriptionStartDate !== currentTournament.inscriptionStartDate) {
      payload.inscriptionStartDate = formValue.inscriptionStartDate!;
    }
    if (formValue.inscriptionEndDate !== currentTournament.inscriptionEndDate) {
      payload.inscriptionEndDate = formValue.inscriptionEndDate!;
    }
    if (formValue.surfaceCategory !== currentTournament.surfaceCategory) {
      payload.surfaceCategory = formValue.surfaceCategory!;
    }
    if (formValue.maxPlayers !== currentTournament.maxPlayers) {
      payload.maxPlayers = formValue.maxPlayers!;
    }
    if (formValue.location !== currentTournament.location) {
      payload.location = formValue.location!;
    }
    if (formValue.locationLatitude !== (currentTournament.locationLatitude ?? null)) {
      payload.locationLatitude = formValue.locationLatitude;
    }
    if (formValue.locationLongitude !== (currentTournament.locationLongitude ?? null)) {
      payload.locationLongitude = formValue.locationLongitude;
    }
    if (formValue.locationPlaceId !== (currentTournament.locationPlaceId ?? null)) {
      payload.locationPlaceId = formValue.locationPlaceId;
    }
    if (formValue.locationFormattedAddress !== (currentTournament.locationFormattedAddress ?? null)) {
      payload.locationFormattedAddress = formValue.locationFormattedAddress;
    }
    if (formValue.setsPerMatch !== (currentTournament.setsPerMatch ?? 3)) {
      payload.setsPerMatch = Number(formValue.setsPerMatch);
    }
    if (formValue.decisiveTiebreakPoints !== (currentTournament.decisiveTiebreakPoints ?? 7)) {
      payload.decisiveTiebreakPoints = Number(formValue.decisiveTiebreakPoints);
    }
    if (formValue.gamesPerSet !== (currentTournament.gamesPerSet ?? 6)) {
      payload.gamesPerSet = Number(formValue.gamesPerSet);
    }

    if (Object.keys(payload).length === 0) {
      this.isEditingGeneralInfo.set(false);
      return;
    }

    this.isSavingGeneralInfo.set(true);
    this.generalInfoError.set(null);

    this.tournamentService.updateTournamentGeneralInfo(currentTournament.id, payload).subscribe({
      next: (updatedTournament) => {
        this.tournament.set(updatedTournament);
        this.isEditingGeneralInfo.set(false);
        this.isSavingGeneralInfo.set(false);
        this.actionMessage.set('Información general del torneo actualizada correctamente.');
        setTimeout(() => this.tryInitMap(), 0);
      },
      error: (error) => {
        this.generalInfoError.set(getApiErrorMessage(error, 'No se pudo actualizar la información del torneo.'));
        this.isSavingGeneralInfo.set(false);
      }
    });
  }

  exportTournamentPdf(): void {
    const currentTournament = this.tournament();
    if (!currentTournament || this.isExportingTournamentPdf()) {
      return;
    }

    if (!this.isLoggedIn()) {
      this.actionError.set('Debe registrarse para imprimir el pdf del torneo.');
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
      this.manualPlayerBirthDate.set('');
      this.manualPlayerNationality.set('');
      this.manualPlayerTennisId.set('');
      this.manualPlayerClub.set('');
      this.manualPlayerPoints.set('');
      this.manualPlayerEntryStatus.set('');

      if (this.manualPlayerSearchQuery().trim().length >= 2) {
        this.scheduleManualPlayerSearch();
      }

      if (source !== 'PROFESSIONAL') {
        this.manualPlayerFilterGender.set('');
        this.manualPlayerFilterCategory.set('');
        this.setManualPlayerFiltersPanelExpanded(false);
      }
    } else {
      this.manualPlayerSearchQuery.set('');
      this.manualPlayerFilterGender.set('');
      this.manualPlayerFilterCategory.set('');
      this.setManualPlayerFiltersPanelExpanded(false);
    }
  }

  private setManualPlayerFiltersPanelExpanded(expanded: boolean): void {
    this.isManualPlayerFiltersPanelExpanded.set(expanded);
    const tournamentId = this.tournament()?.id || this.route.snapshot.paramMap.get('id');
    if (tournamentId && typeof window !== 'undefined' && window.sessionStorage) {
      sessionStorage.setItem(`tournament_detail_manual_player_filters_expanded_${tournamentId}`, String(expanded));
    }
  }

  toggleManualPlayerFiltersPanel(): void {
    this.setManualPlayerFiltersPanelExpanded(!this.isManualPlayerFiltersPanelExpanded());
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

  addCatalogCategory(catalogEvent: TournamentEventCatalogItem): void {
    this.eventsErrorMessage.set(null);

    const existingSelections = this.selectedEvents().filter(event => event.categoryId === catalogEvent.id);
    if (existingSelections.length >= 3) {
      this.eventsErrorMessage.set('No se puede añadir una categoría más de 3 veces.');
      return;
    }

    const chosenGenders = existingSelections.flatMap(event => event.genders);
    const defaultGender = this.eventGenderOptions.find(gender => !chosenGenders.includes(gender));

    if (!defaultGender) {
      this.eventsErrorMessage.set('Todas las modalidades para esta categoría ya han sido añadidas.');
      return;
    }

    const uniqueId = 'sel-' + Math.random().toString(36).substring(2, 9);

    this.selectedEvents.update(events => [
      ...events,
      {
        uniqueId,
        categoryId: catalogEvent.id,
        eventCategory: catalogEvent.category,
        eventsByGender: [{ gender: defaultGender, eventId: null }],
        genders: [defaultGender],
        stages: [
          {
            stageType: 'SINGLE_ELIMINATION'
          }
        ]
      }
    ]);
  }

  changeEventGender(uniqueId: string, gender: TournamentEventGender): void {
    this.eventsErrorMessage.set(null);

    this.selectedEvents.update(events =>
      events.map(event => {
        if (event.uniqueId !== uniqueId) {
          return event;
        }

        const isDuplicate = events.some(other =>
          other.categoryId === event.categoryId &&
          other.uniqueId !== uniqueId &&
          other.genders.includes(gender)
        );

        if (isDuplicate) {
          this.eventsErrorMessage.set(`La modalidad ${this.getGenderLabel(gender)} ya está añadida para esta categoría.`);
          return event;
        }

        const hasGenderEntry = event.eventsByGender.some(eg => eg.gender === gender);
        const nextEventsByGender = hasGenderEntry
          ? event.eventsByGender
          : [...event.eventsByGender, { gender, eventId: null }];

        return {
          ...event,
          genders: [gender],
          eventsByGender: nextEventsByGender
        };
      })
    );
  }

  removeSelectedEvent(uniqueId: string): void {
    this.eventsErrorMessage.set(null);
    this.selectedEvents.update(events => events.filter(event => event.uniqueId !== uniqueId));
  }

  addEventStage(uniqueId: string): void {
    this.eventsErrorMessage.set(null);

    this.selectedEvents.update(events =>
      events.map(event => {
        if (event.uniqueId !== uniqueId) {
          return event;
        }
        const newStageType: TournamentStageType = isConsolationDisabled(event.stages.map(s => s.stageType))
          ? 'SINGLE_ELIMINATION'
          : 'CONSOLATION';
        return {
          ...event,
          stages: [...event.stages, { stageType: newStageType }]
        };
      })
    );
  }

  removeEventStage(uniqueId: string, stageIndex: number): void {
    this.eventsErrorMessage.set(null);

    this.selectedEvents.update(events =>
      events.map(event => {
        if (event.uniqueId !== uniqueId || event.stages.length <= 1) {
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

  updateEventStageType(uniqueId: string, stageIndex: number, stageType: TournamentStageType): void {
    this.selectedEvents.update(events =>
      events.map(event => {
        if (event.uniqueId !== uniqueId) {
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

  isGenderOptionDisabled(categoryId: number, gender: TournamentEventGender, currentUniqueId?: string): boolean {
    return this.selectedEvents().some(event =>
      event.categoryId === categoryId &&
      event.uniqueId !== currentUniqueId &&
      event.genders.includes(gender)
    );
  }

  isAddCategoryDisabled(categoryId: number): boolean {
    const count = this.selectedEvents().filter(event => event.categoryId === categoryId).length;
    if (count >= 3) {
      return true;
    }
    const chosenGenders = this.selectedEvents()
      .filter(event => event.categoryId === categoryId)
      .flatMap(event => event.genders);
    return chosenGenders.length >= 3;
  }

  isEventSelected(categoryId: number): boolean {
    return this.selectedEvents().some(event => event.categoryId === categoryId);
  }

  getEventLabelById(categoryId: number): string {
    return this.eventCatalog().find(event => event.id === categoryId)?.category ?? String(categoryId);
  }

  toggleHeaderStatusDropdown(): void {
    if (!this.isTournamentAdmin() || this.allowedStatusTransitions().length === 0) {
      return;
    }
    this.showHeaderStatusDropdown.update(open => !open);
  }

  closeHeaderStatusDropdown(): void {
    this.showHeaderStatusDropdown.set(false);
  }

  onHeaderStatusDropdownKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.closeHeaderStatusDropdown();
    }
  }

  updateHeaderStatus(nextStatus: TournamentStatus): void {
    if (!this.isTournamentAdmin()) {
      this.actionError.set('Solo el administrador del torneo puede cambiar el estado.');
      return;
    }

    const currentTournament = this.tournament();
    if (!currentTournament || !nextStatus) {
      return;
    }

    this.isUpdatingStatus.set(true);
    this.actionError.set(null);
    this.actionMessage.set(null);
    this.showHeaderStatusDropdown.set(false);

    this.tournamentService.updateTournamentStatus(currentTournament.id, { status: nextStatus }).subscribe({
      next: updatedTournament => {
        this.tournament.set(updatedTournament);
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
    if (!this.isTournamentAdmin()) {
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
    if (!this.isTournamentAdmin()) {
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
      payload.birthDate = this.manualPlayerBirthDate().trim() || null;
      payload.nationality = this.manualPlayerNationality().trim() || null;
      payload.tennisId = this.manualPlayerTennisId().trim() || null;
      payload.club = this.manualPlayerClub().trim() || null;
      payload.points = this.manualPlayerPoints() ? Number(this.manualPlayerPoints()) : null;
      payload.entryStatus = this.manualPlayerEntryStatus().trim() || null;
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
        this.manualPlayerClub.set('');
        this.manualPlayerPoints.set('');
        this.manualPlayerEntryStatus.set('');
        this.loadTournamentInscriptions();
      },
      error: (error) => {
        this.isSubmittingManualPlayer.set(false);
        this.manualPlayerError.set(getApiErrorMessage(error, 'No se pudo añadir el jugador a la prueba.'));
      }
    });
  }

  createCourt(): void {
    if (!this.isTournamentAdmin()) {
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
    this.isCourtsPanelExpanded.update(expanded => {
      const nextVal = !expanded;
      const tournamentId = this.tournament()?.id || this.route.snapshot.paramMap.get('id');
      if (tournamentId && typeof window !== 'undefined' && window.sessionStorage) {
        sessionStorage.setItem(`tournament_detail_courts_expanded_${tournamentId}`, String(nextVal));
      }
      return nextVal;
    });
  }

  toggleMatchSchedulePanel(): void {
    this.isMatchSchedulePanelExpanded.update(expanded => {
      const nextVal = !expanded;
      const tournamentId = this.tournament()?.id || this.route.snapshot.paramMap.get('id');
      if (tournamentId && typeof window !== 'undefined' && window.sessionStorage) {
        sessionStorage.setItem(`tournament_detail_match_schedule_expanded_${tournamentId}`, String(nextVal));
      }
      return nextVal;
    });
  }

  toggleManualPlayerPanel(): void {
    this.isManualPlayerPanelExpanded.update(expanded => {
      const nextVal = !expanded;
      const tournamentId = this.tournament()?.id || this.route.snapshot.paramMap.get('id');
      if (tournamentId && typeof window !== 'undefined' && window.sessionStorage) {
        sessionStorage.setItem(`tournament_detail_manual_player_expanded_${tournamentId}`, String(nextVal));
      }
      return nextVal;
    });
  }

  toggleRegisteredPlayersPanel(): void {
    this.isRegisteredPlayersPanelExpanded.update(expanded => {
      const nextVal = !expanded;
      const tournamentId = this.tournament()?.id || this.route.snapshot.paramMap.get('id');
      if (tournamentId && typeof window !== 'undefined' && window.sessionStorage) {
        sessionStorage.setItem(`tournament_detail_registered_players_expanded_${tournamentId}`, String(nextVal));
      }
      return nextVal;
    });
  }

  onUmpireSearchQueryChange(query: string): void {
    this.umpireSearchQuery.set(query);
    this.umpireSearchError.set(null);
    this.umpireSuccessMessage.set(null);

    if (this.umpireSearchDebounceHandle) {
      clearTimeout(this.umpireSearchDebounceHandle);
    }

    if (query.trim().length < 2) {
      this.umpireSearchResults.set([]);
      return;
    }

    this.umpireSearchDebounceHandle = setTimeout(() => {
      this.searchUmpires(query);
    }, 300);
  }

  onUmpireRoleFilterChange(role: string): void {
    this.umpireSearchRoleFilter.set(role);
    const query = this.umpireSearchQuery();
    if (query.trim().length >= 2) {
      if (this.umpireSearchDebounceHandle) {
        clearTimeout(this.umpireSearchDebounceHandle);
      }
      this.umpireSearchDebounceHandle = setTimeout(() => {
        this.searchUmpires(query);
      }, 150);
    }
  }

  private searchUmpires(query: string): void {
    this.isSearchingUmpires.set(true);
    this.umpireSearchError.set(null);

    const roleFilter = this.umpireSearchRoleFilter();
    const roles = roleFilter === 'ALL' ? ['UMPIRE', 'ORGANIZER'] : [roleFilter];

    this.tournamentService.searchUmpires(query, roles).subscribe({
      next: results => {
        const assignedIds = new Set(this.tournamentUmpires().map(u => u.umpireId));
        this.umpireSearchResults.set(results.filter(r => !assignedIds.has(r.id)));
        this.isSearchingUmpires.set(false);
      },
      error: error => {
        this.umpireSearchError.set(getApiErrorMessage(error, 'No se pudieron buscar árbitros.'));
        this.isSearchingUmpires.set(false);
      }
    });
  }

  addUmpire(umpire: TournamentUmpireSearchResponse): void {
    const tournamentId = this.tournament()?.id;
    if (!tournamentId) return;

    this.isAddingUmpire.set(true);
    this.umpireErrorMessage.set(null);
    this.umpireSuccessMessage.set(null);

    this.tournamentService.addTournamentUmpire(tournamentId, { id: umpire.id }).subscribe({
      next: () => {
        this.umpireSuccessMessage.set(`Árbitro ${umpire.firstName || ''} ${umpire.lastName || ''} habilitado correctamente.`);
        this.isAddingUmpire.set(false);
        this.umpireSearchQuery.set('');
        this.umpireSearchResults.set([]);
        this.loadTournamentUmpires();
      },
      error: error => {
        this.umpireErrorMessage.set(getApiErrorMessage(error, 'No se pudo habilitar el árbitro.'));
        this.isAddingUmpire.set(false);
      }
    });
  }

  removeUmpire(umpire: TournamentUmpireResponse): void {
    const tournamentId = this.tournament()?.id;
    if (!tournamentId) return;

    if (!window.confirm('¿Eliminar este árbitro del torneo?')) return;

    this.isRemovingUmpireId.set(umpire.umpireId);
    this.umpireErrorMessage.set(null);
    this.umpireSuccessMessage.set(null);

    this.tournamentService.removeTournamentUmpire(tournamentId, umpire.umpireId).subscribe({
      next: () => {
        this.umpireSuccessMessage.set('Árbitro eliminado correctamente.');
        this.isRemovingUmpireId.set(null);
        this.loadTournamentUmpires();
      },
      error: error => {
        this.umpireErrorMessage.set(getApiErrorMessage(error, 'No se pudo eliminar el árbitro.'));
        this.isRemovingUmpireId.set(null);
      }
    });
  }

  getUmpireDisplayName(umpire: TournamentUmpireResponse): string {
    const parts = [umpire.umpireFirstName, umpire.umpireLastName].filter(Boolean);
    if (parts.length > 0) {
      return parts.join(' ');
    }
    return umpire.umpireEmail || umpire.umpireId;
  }

  private loadTournamentUmpires(): void {
    const tournamentId = this.tournament()?.id;
    if (!tournamentId) return;

    this.isLoadingTournamentUmpires.set(true);
    this.tournamentService.getTournamentUmpires(tournamentId).subscribe({
      next: umpires => {
        this.tournamentUmpires.set(umpires);
        this.isLoadingTournamentUmpires.set(false);
      },
      error: () => {
        this.tournamentUmpires.set([]);
        this.isLoadingTournamentUmpires.set(false);
      }
    });
  }

  getEditedPoints(player: TournamentInscriptionPlayer): number | null {
    const map = this.editedPointsMap();
    if (player.inscriptionId in map) {
      return map[player.inscriptionId];
    }
    return player.points ?? null;
  }

  setEditedPoints(player: TournamentInscriptionPlayer, value: number | null): void {
    this.editedPointsMap.update(map => ({ ...map, [player.inscriptionId]: value }));
  }

  hasPointsChanges(): boolean {
    const map = this.editedPointsMap();
    const players = this.tournamentInscriptionPlayers();
    return players.some(p => {
      const edited = map[p.inscriptionId];
      const original = p.points ?? null;
      return edited !== undefined && edited !== original;
    });
  }

  saveAllPoints(): void {
    const tournamentId = this.tournament()?.id;
    if (!tournamentId) return;

    const map = this.editedPointsMap();
    const players = this.tournamentInscriptionPlayers();
    const updates = players
      .filter(p => {
        const edited = map[p.inscriptionId];
        const original = p.points ?? null;
        return edited !== undefined && edited !== original;
      })
      .map(p => ({
        participantId: p.participantId,
        points: map[p.inscriptionId] ?? null,
        seed: null
      }));

    if (updates.length === 0) return;

    this.isSavingPoints.set(true);
    this.tournamentService.updateParticipantsPoints(tournamentId, updates).subscribe({
      next: () => {
        this.tournamentService.getTournamentInscriptions(tournamentId).subscribe({
          next: (inscriptions) => {
            this.tournamentInscriptions.set(inscriptions);
            this.editedPointsMap.set({});
            this.isSavingPoints.set(false);
          },
          error: () => this.isSavingPoints.set(false)
        });
      },
      error: () => this.isSavingPoints.set(false)
    });
  }

  resetPointsChanges(): void {
    this.editedPointsMap.set({});
  }

  getEditedClub(player: TournamentInscriptionPlayer): string {
    const map = this.editedClubMap();
    if (player.inscriptionId in map) {
      return map[player.inscriptionId];
    }
    return player.club ?? '';
  }

  setEditedClub(player: TournamentInscriptionPlayer, value: string): void {
    this.editedClubMap.update(map => ({ ...map, [player.inscriptionId]: value }));
  }

  onClubSelected(player: TournamentInscriptionPlayer, event: { clubId: string | null; clubName: string }): void {
    this.setEditedClub(player, event.clubName);
  }

  getEditedEntryStatus(player: TournamentInscriptionPlayer): string {
    const map = this.editedEntryStatusMap();
    if (player.inscriptionId in map) {
      return map[player.inscriptionId];
    }
    return player.entryStatus ?? '';
  }

  setEditedEntryStatus(player: TournamentInscriptionPlayer, value: string): void {
    this.editedEntryStatusMap.update(map => ({ ...map, [player.inscriptionId]: value }));
  }

  getEditedPaymentStatus(player: TournamentInscriptionPlayer): string {
    const map = this.editedPaymentStatusMap();
    if (player.inscriptionId in map) {
      return map[player.inscriptionId];
    }
    return player.paymentStatus ?? '';
  }

  setEditedPaymentStatus(player: TournamentInscriptionPlayer, value: string): void {
    this.editedPaymentStatusMap.update(map => ({ ...map, [player.inscriptionId]: value }));
  }

  getEditedEventId(player: TournamentInscriptionPlayer): string {
    const map = this.editedEventIdMap();
    if (player.inscriptionId in map) {
      return map[player.inscriptionId];
    }
    return player.eventId ?? '';
  }

  setEditedEventId(player: TournamentInscriptionPlayer, value: string): void {
    this.editedEventIdMap.update(map => ({ ...map, [player.inscriptionId]: value }));
  }

  getEditedFirstName(player: TournamentInscriptionPlayer): string {
    const map = this.editedFirstNameMap();
    if (player.inscriptionId in map) {
      return map[player.inscriptionId];
    }
    return player.firstName ?? '';
  }

  setEditedFirstName(player: TournamentInscriptionPlayer, value: string): void {
    this.editedFirstNameMap.update(map => ({ ...map, [player.inscriptionId]: value }));
  }

  getEditedLastName(player: TournamentInscriptionPlayer): string {
    const map = this.editedLastNameMap();
    if (player.inscriptionId in map) {
      return map[player.inscriptionId];
    }
    return player.lastName ?? '';
  }

  setEditedLastName(player: TournamentInscriptionPlayer, value: string): void {
    this.editedLastNameMap.update(map => ({ ...map, [player.inscriptionId]: value }));
  }

  getEntryStatusLabel(status: string | null | undefined): string {
    if (!status) return '-';
    const labels: Record<string, string> = {
      'DIRECT_ACCEPTANCE': 'DA',
      'WILDCARD': 'WC',
      'QUALIFIER': 'Q',
      'LUCKY_LOSER': 'LL'
    };
    return labels[status] ?? status;
  }

  hasDetailChanges(): boolean {
    const clubMap = this.editedClubMap();
    const entryStatusMap = this.editedEntryStatusMap();
    const paymentStatusMap = this.editedPaymentStatusMap();
    const eventIdMap = this.editedEventIdMap();
    const firstNameMap = this.editedFirstNameMap();
    const lastNameMap = this.editedLastNameMap();
    const players = this.tournamentInscriptionPlayers();
    return players.some(p => {
      const editedClub = clubMap[p.inscriptionId];
      const clubChanged = editedClub !== undefined && editedClub !== (p.club ?? '');

      const editedStatus = entryStatusMap[p.inscriptionId];
      const statusChanged = editedStatus !== undefined && editedStatus !== (p.entryStatus ?? '');

      const editedPayment = paymentStatusMap[p.inscriptionId];
      const paymentChanged = editedPayment !== undefined && editedPayment !== (p.paymentStatus ?? '');

      const editedEventId = eventIdMap[p.inscriptionId];
      const eventChanged = editedEventId !== undefined && editedEventId !== (p.eventId ?? '');

      const editedFirstName = firstNameMap[p.inscriptionId];
      const firstNameChanged = editedFirstName !== undefined && editedFirstName !== (p.firstName ?? '');

      const editedLastName = lastNameMap[p.inscriptionId];
      const lastNameChanged = editedLastName !== undefined && editedLastName !== (p.lastName ?? '');

      return clubChanged || statusChanged || paymentChanged || eventChanged || firstNameChanged || lastNameChanged;
    });
  }

  saveAllDetails(): void {
    const tournamentId = this.tournament()?.id;
    if (!tournamentId) return;

    const clubMap = this.editedClubMap();
    const entryStatusMap = this.editedEntryStatusMap();
    const paymentStatusMap = this.editedPaymentStatusMap();
    const eventIdMap = this.editedEventIdMap();
    const firstNameMap = this.editedFirstNameMap();
    const lastNameMap = this.editedLastNameMap();
    const players = this.tournamentInscriptionPlayers();

    const detailUpdates = players.filter(p => {
      const clubEdited = clubMap[p.inscriptionId];
      const clubChanged = clubEdited !== undefined && clubEdited !== (p.club ?? '');
      const statusEdited = entryStatusMap[p.inscriptionId];
      const statusChanged = statusEdited !== undefined && statusEdited !== (p.entryStatus ?? '');
      const paymentEdited = paymentStatusMap[p.inscriptionId];
      const paymentChanged = paymentEdited !== undefined && paymentEdited !== (p.paymentStatus ?? '');
      const eventEdited = eventIdMap[p.inscriptionId];
      const eventChanged = eventEdited !== undefined && eventEdited !== (p.eventId ?? '');
      const firstNameEdited = firstNameMap[p.inscriptionId];
      const firstNameChanged = firstNameEdited !== undefined && firstNameEdited !== (p.firstName ?? '');
      const lastNameEdited = lastNameMap[p.inscriptionId];
      const lastNameChanged = lastNameEdited !== undefined && lastNameEdited !== (p.lastName ?? '');
      return clubChanged || statusChanged || paymentChanged || eventChanged || firstNameChanged || lastNameChanged;
    }).map(p => ({
      inscriptionId: p.inscriptionId,
      participantId: p.participantId,
      clubName: clubMap[p.inscriptionId] !== undefined ? (clubMap[p.inscriptionId] || null) : null,
      entryStatus: entryStatusMap[p.inscriptionId] !== undefined ? (entryStatusMap[p.inscriptionId] || null) : null,
      paymentStatus: paymentStatusMap[p.inscriptionId] !== undefined ? (paymentStatusMap[p.inscriptionId] || null) : null,
      eventId: eventIdMap[p.inscriptionId] !== undefined ? (eventIdMap[p.inscriptionId] || null) : null,
      firstName: firstNameMap[p.inscriptionId] !== undefined ? (firstNameMap[p.inscriptionId] || null) : null,
      lastName: lastNameMap[p.inscriptionId] !== undefined ? (lastNameMap[p.inscriptionId] || null) : null
    }));

    const pointUpdates = players.filter(p => {
      const edited = this.editedPointsMap()[p.inscriptionId];
      const original = p.points ?? null;
      return edited !== undefined && edited !== original;
    }).map(p => ({
      participantId: p.participantId,
      points: this.editedPointsMap()[p.inscriptionId] ?? null,
      seed: null
    }));

    if (detailUpdates.length === 0 && pointUpdates.length === 0) return;

    const hasNewClub = detailUpdates.some(u => u.clubName != null && u.clubName.trim().length > 0);
    this.isSavingDetails.set(true);
    this.actionMessage.set(null);
    this.actionError.set(null);

    const reloadAndFinish = (successMsg: string) => {
      this.tournamentService.getTournamentInscriptions(tournamentId).subscribe({
        next: (inscriptions) => {
          this.tournamentInscriptions.set(inscriptions);
          this.editedPointsMap.set({});
          this.editedClubMap.set({});
          this.editedEntryStatusMap.set({});
          this.editedPaymentStatusMap.set({});
          this.editedEventIdMap.set({});
          this.editedFirstNameMap.set({});
          this.editedLastNameMap.set({});
          this.isSavingDetails.set(false);
          this.actionMessage.set(successMsg);
        },
        error: () => {
          this.isSavingDetails.set(false);
          this.actionError.set('Los datos se guardaron pero no se pudo actualizar la vista.');
        }
      });
    };

    const handleError = () => {
      this.isSavingDetails.set(false);
      this.actionError.set('No se pudieron guardar los cambios. Inténtalo de nuevo.');
    };

    const detailCount = detailUpdates.length;
    const pointCount = pointUpdates.length;

    const sendDetailUpdates = () => {
      if (detailCount === 0) {
        const msg = pointCount > 0 ? 'Puntos actualizados.' : 'Cambios guardados.';
        reloadAndFinish(msg);
        return;
      }
      let completed = 0;
      let hasError = false;
      detailUpdates.forEach(update => {
        this.tournamentService.updateParticipantDetails(tournamentId, update).subscribe({
          next: () => {
            completed++;
            if (completed === detailCount && !hasError) {
              const clubInfo = hasNewClub ? ' Club registrado.' : '';
              const msg = pointCount > 0
                ? `Detalles y puntos actualizados.${clubInfo}`
                : `Cambios guardados.${clubInfo}`;
              if (pointCount > 0) {
                this.tournamentService.updateParticipantsPoints(tournamentId, pointUpdates).subscribe({
                  next: () => reloadAndFinish(msg),
                  error: () => {
                    reloadAndFinish(`Detalles guardados${clubInfo}. No se pudieron actualizar los puntos.`);
                  }
                });
              } else {
                reloadAndFinish(msg);
              }
            }
          },
          error: () => {
            hasError = true;
            completed++;
            if (completed === detailCount) {
              handleError();
            }
          }
        });
      });
    };

    if (pointCount > 0 && detailCount === 0) {
      this.tournamentService.updateParticipantsPoints(tournamentId, pointUpdates).subscribe({
        next: () => reloadAndFinish('Puntos actualizados.'),
        error: () => handleError()
      });
    } else {
      sendDetailUpdates();
    }
  }

  resetAllChanges(): void {
    this.editedPointsMap.set({});
    this.editedClubMap.set({});
    this.editedEntryStatusMap.set({});
    this.editedPaymentStatusMap.set({});
    this.editedEventIdMap.set({});
    this.editedFirstNameMap.set({});
    this.editedLastNameMap.set({});
  }

  isGeneratingInvitation(inscriptionId: string): boolean {
    return !!this.isGeneratingInvitationMap()[inscriptionId];
  }

  getInvitationUrl(inscriptionId: string): string | null {
    return this.invitationUrlMap()[inscriptionId] || null;
  }

  generateInvitation(player: TournamentInscriptionPlayer): void {
    const tournamentId = this.tournament()?.id;
    if (!tournamentId || !player.participantId) return;

    this.isGeneratingInvitationMap.update(map => ({ ...map, [player.inscriptionId]: true }));
    this.invitationService.generateInvitation(tournamentId, player.participantId).subscribe({
      next: (res) => {
        this.invitationUrlMap.update(map => ({ ...map, [player.inscriptionId]: res.invitationUrl }));
        this.isGeneratingInvitationMap.update(map => ({ ...map, [player.inscriptionId]: false }));
        if (typeof navigator !== 'undefined' && navigator.clipboard) {
          navigator.clipboard.writeText(res.invitationUrl).then(() => {
            this.actionMessage.set(`Enlace de invitación copiado para ${player.firstName} ${player.lastName}`);
            setTimeout(() => this.actionMessage.set(null), 4000);
          }).catch(() => {
            this.actionMessage.set(`Enlace de invitación generado para ${player.firstName} ${player.lastName}`);
            setTimeout(() => this.actionMessage.set(null), 4000);
          });
        } else {
          this.actionMessage.set(`Enlace de invitación generado para ${player.firstName} ${player.lastName}`);
          setTimeout(() => this.actionMessage.set(null), 4000);
        }
      },
      error: (err) => {
        this.isGeneratingInvitationMap.update(map => ({ ...map, [player.inscriptionId]: false }));
        this.actionError.set(getApiErrorMessage(err, 'No se pudo generar la invitación.'));
        setTimeout(() => this.actionError.set(null), 4000);
      }
    });
  }

  toggleEventsDrawsPanel(): void {
    this.isEventsDrawsPanelExpanded.update(expanded => {
      const nextVal = !expanded;
      const tournamentId = this.tournament()?.id || this.route.snapshot.paramMap.get('id');
      if (tournamentId && typeof window !== 'undefined' && window.sessionStorage) {
        sessionStorage.setItem(`tournament_detail_events_draws_expanded_${tournamentId}`, String(nextVal));
      }
      return nextVal;
    });
  }

  clearMatchScheduleFilters(): void {
    this.matchScheduleEventFilter.set('');
    this.matchScheduleRoundFilter.set('');
    this.matchScheduleDateFilter.set('');
    this.matchScheduleCourtFilter.set('');
    this.matchScheduleProfessionalFilter.set('');
    this.matchSchedulePlayerFilter.set('');
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
    if (!this.isTournamentAdmin()) {
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
    if (!this.isTournamentAdmin()) {
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

  private loadTournament(tournamentId: string, preserveActiveSection = false, silent = false): void {
    if (!silent) {
      this.isLoading.set(true);
    }
    this.errorMessage.set(null);

    this.tournamentService.getTournamentById(tournamentId).subscribe({
      next: tournament => {
        this.tournament.set(tournament);
        this.hydrateSelectedEventsFromTournament(tournament.events ?? []);
        this.initializeInscriptionSelection();
        this.syncManualPlayerEventSelection(tournament.events ?? []);
        this.loadTournamentInscriptions();
        this.loadCourts(tournament.id);
        this.loadScheduleConfig(tournamentId);
        this.loadTournamentUmpires();
        if (!silent) {
          this.isLoading.set(false);
        }
        if (!preserveActiveSection) {
          let restored = false;
          if (typeof window !== 'undefined' && window.sessionStorage) {
            const savedSection = sessionStorage.getItem(`tournament_detail_section_${tournamentId}`);
            if (savedSection && ['overview', 'setup', 'inscriptions', 'stages'].includes(savedSection)) {
              this.activeSection.set(savedSection as TournamentDetailSection);
              restored = true;
            }
          }
          if (!restored) {
            this.activeSection.set(this.isTournamentAdmin() ? 'setup' : 'overview');
          }
        }
        setTimeout(() => this.tryInitMap(), 0);
      },
      error: (error) => {
        this.errorMessage.set(getApiErrorMessage(error, 'No se pudo cargar el detalle del torneo.'));
        if (!silent) {
          this.isLoading.set(false);
        }
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

  private loadScheduleConfig(tournamentId: string): void {
    this.scheduleConfigSuccess.set(null);
    this.scheduleConfigError.set(null);
    this.scheduleOverlapWarning.set(null);

    this.tournamentService.getScheduleConfig(tournamentId).subscribe({
      next: config => {
        const slots = config.timeSlots.length > 0
          ? config.timeSlots.map(slot => ({ startTime: slot.startTime, endTime: slot.endTime }))
          : [
            { startTime: '08:00', endTime: '13:00' },
            { startTime: '16:00', endTime: '20:00' }
          ];
        this.scheduleConfigDraft.set({
          timeSlots: slots,
          matchDurationMinutes: config.matchDurationMinutes || 60
        });
      },
      error: () => {
        this.scheduleConfigDraft.set({
          timeSlots: [
            { startTime: '08:00', endTime: '13:00' },
            { startTime: '16:00', endTime: '20:00' }
          ],
          matchDurationMinutes: 60
        });
      }
    });
  }

  addScheduleTimeSlot(): void {
    const current = this.scheduleConfigDraft();
    const lastSlot = current.timeSlots[current.timeSlots.length - 1];
    const newStartTime = lastSlot ? lastSlot.endTime : '08:00';
    const newEndTime = this.addHoursToTime(newStartTime, 1);

    this.scheduleConfigDraft.set({
      ...current,
      timeSlots: [...current.timeSlots, { startTime: newStartTime, endTime: newEndTime }]
    });
    this.checkOverlap();
  }

  removeScheduleTimeSlot(index: number): void {
    const current = this.scheduleConfigDraft();
    this.scheduleConfigDraft.set({
      ...current,
      timeSlots: current.timeSlots.filter((_, i) => i !== index)
    });
    this.scheduleOverlapWarning.set(null);
  }

  updateScheduleTimeSlot(index: number, field: 'startTime' | 'endTime', value: string): void {
    const current = this.scheduleConfigDraft();
    const updated = current.timeSlots.map((slot, i) =>
      i === index ? { ...slot, [field]: value } : slot
    );
    this.scheduleConfigDraft.set({ ...current, timeSlots: updated });
    this.checkOverlap();
  }

  updateScheduleDuration(minutes: number): void {
    const current = this.scheduleConfigDraft();
    this.scheduleConfigDraft.set({ ...current, matchDurationMinutes: minutes });
  }

  saveScheduleConfig(): void {
    const tournament = this.tournament();
    if (!tournament) return;

    const draft = this.scheduleConfigDraft();
    if (draft.timeSlots.length === 0) {
      this.scheduleConfigError.set('Debe añadir al menos una franja horaria.');
      return;
    }

    const sorted = [...draft.timeSlots].sort((a, b) => a.startTime.localeCompare(b.startTime));
    for (let i = 0; i < sorted.length - 1; i++) {
      if (sorted[i].endTime > sorted[i + 1].startTime) {
        this.scheduleOverlapWarning.set(
          `Las franjas ${sorted[i].startTime}-${sorted[i].endTime} y ${sorted[i + 1].startTime}-${sorted[i + 1].endTime} se solapan.`
        );
        return;
      }
    }

    this.isSavingScheduleConfig.set(true);
    this.scheduleConfigError.set(null);
    this.scheduleConfigSuccess.set(null);

    this.tournamentService.saveScheduleConfig(tournament.id, {
      timeSlots: sorted.map(slot => ({ startTime: slot.startTime, endTime: slot.endTime })),
      matchDurationMinutes: draft.matchDurationMinutes
    }).subscribe({
      next: () => {
        this.scheduleConfigSuccess.set('Horarios guardados correctamente.');
        this.isSavingScheduleConfig.set(false);
        this.scheduleOverlapWarning.set(null);
      },
      error: (error) => {
        this.scheduleConfigError.set(getApiErrorMessage(error, 'No se pudieron guardar los horarios.'));
        this.isSavingScheduleConfig.set(false);
      }
    });
  }

  mergeOverlappingSlots(): void {
    const current = this.scheduleConfigDraft();
    const sorted = [...current.timeSlots].sort((a, b) => a.startTime.localeCompare(b.startTime));
    const merged: Array<{ startTime: string; endTime: string }> = [];

    for (const slot of sorted) {
      if (merged.length === 0) {
        merged.push({ ...slot });
      } else {
        const last = merged[merged.length - 1];
        if (slot.startTime <= last.endTime) {
          last.endTime = last.endTime > slot.endTime ? last.endTime : slot.endTime;
        } else {
          merged.push({ ...slot });
        }
      }
    }

    this.scheduleConfigDraft.set({ ...current, timeSlots: merged });
    this.scheduleOverlapWarning.set(null);
  }

  clearOverlapWarning(): void {
    this.scheduleOverlapWarning.set(null);
  }

  private checkOverlap(): void {
    const current = this.scheduleConfigDraft();
    const sorted = [...current.timeSlots].sort((a, b) => a.startTime.localeCompare(b.startTime));

    for (let i = 0; i < sorted.length - 1; i++) {
      if (sorted[i].endTime > sorted[i + 1].startTime) {
        this.scheduleOverlapWarning.set(
          `Las franjas ${sorted[i].startTime}-${sorted[i].endTime} y ${sorted[i + 1].startTime}-${sorted[i + 1].endTime} se solapan. Puedes unirlas automáticamente o editarlas manualmente.`
        );
        return;
      }
    }
    this.scheduleOverlapWarning.set(null);
  }

  private addHoursToTime(timeStr: string, hours: number): string {
    const [h, m] = timeStr.split(':').map(Number);
    const totalMinutes = h * 60 + m + hours * 60;
    const newH = Math.floor(totalMinutes / 60) % 24;
    const newM = totalMinutes % 60;
    return `${String(newH).padStart(2, '0')}:${String(newM).padStart(2, '0')}`;
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
    const selections: TournamentEventSelection[] = events
      .map(event => {
        const normalizedGender = event.gender.toUpperCase() as TournamentEventGender;
        if (!this.eventGenderOptions.includes(normalizedGender)) {
          return null;
        }

        const stages = event.stages?.length
          ? event.stages
            .slice()
            .sort((left, right) => left.order - right.order)
            .map(stage => {
              const raw = stage.strategyName ?? stage.stageType;
              const stageType: TournamentStageType = isValidStageType(raw) ? raw : 'SINGLE_ELIMINATION';
              return { stageType };
            })
          : [{ stageType: 'SINGLE_ELIMINATION' as TournamentStageType }];

        const uniqueId = 'sel-' + Math.random().toString(36).substring(2, 9);

        const selection: TournamentEventSelection = {
          uniqueId,
          categoryId: event.categoryId,
          eventCategory: this.getEventLabelById(event.categoryId),
          eventsByGender: [{ gender: normalizedGender, eventId: event.eventId }],
          genders: [normalizedGender],
          stages
        };
        return selection;
      })
      .filter((item): item is TournamentEventSelection => item !== null);

    this.selectedEvents.set(selections);
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
    if (!this.isTournamentAdmin()) {
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

  onMatchSelected(matchOrId: MatchResponse | string): void {
    let matchToSelect: MatchResponse | null = null;
    if (typeof matchOrId === 'string') {
      const tournamentVal = this.tournament();
      if (tournamentVal && tournamentVal.events) {
        for (const ev of tournamentVal.events) {
          if (ev.stages) {
            for (const st of ev.stages) {
              if (st.draws) {
                for (const dr of st.draws) {
                  if (dr.matches) {
                    const found = dr.matches.find(m => m.id === matchOrId);
                    if (found) {
                      matchToSelect = found;
                      break;
                    }
                  }
                }
              }
              if (matchToSelect) break;
            }
          }
          if (matchToSelect) break;
        }
      }
    } else {
      matchToSelect = matchOrId;
    }

    if (matchToSelect) {
      this.selectedMatch.set(matchToSelect);
      if (this.matchModal) {
        this.matchModal.open();
      }
    }
  }

  onModalClose(): void {
    this.selectedMatch.set(null);
  }

  onMatchResultSaved(event: {
    matchId: string;
    winnerId: string | null;
    result: string;
    sets?: SetScoreResponse[] | null;
    notes?: string | null;
    firstPlayerPoints?: string | null;
    secondPlayerPoints?: string | null;
    status: MatchStatus;
    keepOpen?: boolean;
  }): void {
    if (!this.isTournamentAdmin()) {
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
      scoreString: event.result,
      sets: event.sets,
      notes: event.notes,
      firstPlayerPoints: event.firstPlayerPoints,
      secondPlayerPoints: event.secondPlayerPoints,
      status: event.status
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
    if (!this.isTournamentAdmin()) {
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

  onPlayersSwapped(event: {
    matchId1: string;
    slot1: 'first' | 'second';
    matchId2: string;
    slot2: 'first' | 'second';
  }): void {
    if (!this.isTournamentAdmin()) {
      this.actionError.set('Solo el administrador del torneo puede reorganizar jugadores.');
      return;
    }

    const currentTournament = this.tournament();
    if (!currentTournament) {
      return;
    }

    const previousTournament = this.cloneTournament(currentTournament);
    const optimisticTournament = this.patchMatchPlayersInTournament(currentTournament, event);
    if (optimisticTournament) {
      this.tournament.set(optimisticTournament);
    }

    this.actionError.set(null);
    this.actionMessage.set('Reorganizando jugadores...');

    this.tournamentService.reorganizeMatchPlayers(currentTournament.id, event).subscribe({
      next: () => {
        this.actionMessage.set('Jugadores reorganizados correctamente.');
      },
      error: (error) => {
        this.tournament.set(previousTournament);
        this.actionError.set(getApiErrorMessage(error, 'No se pudieron reorganizar los jugadores del cuadro.'));
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
    if (!this.isTournamentAdmin()) {
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

  toggleScheduleCard(matchId: string): void {
    const current = new Set(this.openScheduleCardIds());
    if (current.has(matchId)) {
      current.delete(matchId);
    } else {
      current.add(matchId);
    }
    this.openScheduleCardIds.set(current);
  }

  isScheduleCardOpen(matchId: string): boolean {
    return this.openScheduleCardIds().has(matchId);
  }

  openSwapScheduleModal(match: MatchResponse): void {
    if (!this.isTournamentAdmin()) {
      return;
    }

    const row = this.tournamentMatchScheduleRows().find(r => r.match.id === match.id);
    if (!row) return;

    const label = `${row.firstPlayerName} vs ${row.secondPlayerName}`;
    this.swapModalSourceMatch.set(match);
    this.swapModalSourceLabel.set(label);
    this.swapModalSearch.set('');
    this.swapModalError.set(null);
    this.swapModalCandidateMatches.set(this.tournamentMatchScheduleRows());
    this.swapModalOpen.set(true);
  }

  closeSwapModal(): void {
    this.swapModalOpen.set(false);
    this.swapModalSourceMatch.set(null);
    this.swapModalError.set(null);
  }

  selectSwapTarget(targetRow: TournamentMatchScheduleRow): void {
    const source = this.swapModalSourceMatch();
    if (!source || !targetRow) return;

    const tournamentId = this.tournament()?.id;
    if (!tournamentId) return;

    this.swapModalError.set(null);
    this.tournamentService.swapMatchSchedules(tournamentId, source.id, targetRow.match.id).subscribe({
      next: () => {
        this.closeSwapModal();
        this.actionMessage.set('Programación intercambiada correctamente.');

        const currentTournament = this.tournament();
        if (currentTournament) {
          const updatedTournament = this.patchMatchSchedulesInTournament(currentTournament, source.id, targetRow.match.id);
          if (updatedTournament) {
            this.tournament.set(updatedTournament);
            const m1 = this.findMatchInTournament(updatedTournament, source.id);
            const m2 = this.findMatchInTournament(updatedTournament, targetRow.match.id);
            if (m1) this.setMatchScheduleDraft(m1);
            if (m2) this.setMatchScheduleDraft(m2);
          }
        }

        this.loadTournament(tournamentId, true, true);
      },
      error: (error) => {
        this.swapModalError.set(getApiErrorMessage(error, 'No se pudo intercambiar la programación.'));
      }
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
      if (newScore >= existingScore) {
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
        if (newScore >= existingScore) drawsById.set(dr.id, dr);
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
          ...updatedMatch
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
    event: {
      matchId: string;
      winnerId: string | null;
      result: string;
      sets?: SetScoreResponse[] | null;
      notes?: string | null;
      firstPlayerPoints?: string | null;
      secondPlayerPoints?: string | null;
      status: MatchStatus;
    }
  ): MatchResponse | null {
    const match = this.findMatchInTournament(tournament, event.matchId);
    if (!match) {
      return null;
    }

    return {
      ...match,
      winnerId: event.winnerId !== null ? event.winnerId : match.winnerId,
      result: event.result || match.result,
      sets: event.sets || match.sets,
      notes: event.notes !== undefined ? event.notes : match.notes,
      firstPlayerPoints: event.firstPlayerPoints !== undefined ? event.firstPlayerPoints : match.firstPlayerPoints,
      secondPlayerPoints: event.secondPlayerPoints !== undefined ? event.secondPlayerPoints : match.secondPlayerPoints,
      status: event.status
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

  private patchMatchPlayersInTournament(
    currentTournament: TournamentResponse,
    event: {
      matchId1: string;
      slot1: 'first' | 'second';
      matchId2: string;
      slot2: 'first' | 'second';
    }
  ): TournamentResponse | null {
    const clonedTournament = this.cloneTournament(currentTournament);
    const sourceMatch = this.findMatchInTournament(clonedTournament, event.matchId1);
    const targetMatch = this.findMatchInTournament(clonedTournament, event.matchId2);

    if (!sourceMatch || !targetMatch) {
      return null;
    }

    if (sourceMatch.id === targetMatch.id && event.slot1 === event.slot2) {
      return null;
    }

    const sourceValue = event.slot1 === 'first'
      ? (sourceMatch.firstInscriptionId ?? null)
      : (sourceMatch.secondInscriptionId ?? null);
    const targetValue = event.slot2 === 'first'
      ? (targetMatch.firstInscriptionId ?? null)
      : (targetMatch.secondInscriptionId ?? null);

    if (sourceMatch.id === targetMatch.id) {
      if (event.slot1 === 'first') {
        sourceMatch.firstInscriptionId = targetValue;
      } else {
        sourceMatch.secondInscriptionId = targetValue;
      }

      if (event.slot2 === 'first') {
        sourceMatch.firstInscriptionId = sourceValue;
      } else {
        sourceMatch.secondInscriptionId = sourceValue;
      }

      Object.assign(sourceMatch, this.resolveOptimisticMatchState(sourceMatch));
      return clonedTournament;
    }

    if (event.slot1 === 'first') {
      sourceMatch.firstInscriptionId = targetValue;
    } else {
      sourceMatch.secondInscriptionId = targetValue;
    }

    if (event.slot2 === 'first') {
      targetMatch.firstInscriptionId = sourceValue;
    } else {
      targetMatch.secondInscriptionId = sourceValue;
    }

    Object.assign(sourceMatch, this.resolveOptimisticMatchState(sourceMatch));
    Object.assign(targetMatch, this.resolveOptimisticMatchState(targetMatch));

    return clonedTournament;
  }

  private patchMatchSchedulesInTournament(
    currentTournament: TournamentResponse,
    matchId1: string,
    matchId2: string
  ): TournamentResponse | null {
    const clonedTournament = this.cloneTournament(currentTournament);
    const match1 = this.findMatchInTournament(clonedTournament, matchId1);
    const match2 = this.findMatchInTournament(clonedTournament, matchId2);

    if (!match1 || !match2) {
      return null;
    }

    const tempScheduledAt = match1.scheduledAt;
    const tempScheduleTimeType = match1.scheduleTimeType;
    const tempCourtId = match1.courtId;
    const tempCourt = match1.court;

    match1.scheduledAt = match2.scheduledAt;
    match1.scheduleTimeType = match2.scheduleTimeType;
    match1.courtId = match2.courtId;
    match1.court = match2.court;

    match2.scheduledAt = tempScheduledAt;
    match2.scheduleTimeType = tempScheduleTimeType;
    match2.courtId = tempCourtId;
    match2.court = tempCourt;

    return clonedTournament;
  }

  private resolveOptimisticMatchState(match: MatchResponse): MatchResponse {
    if ((match.roundNumber ?? 1) !== 1) {
      return match;
    }

    const firstEmpty = !match.firstInscriptionId;
    const secondEmpty = !match.secondInscriptionId;

    if (firstEmpty !== secondEmpty) {
      const winnerId = !firstEmpty ? match.firstInscriptionId : match.secondInscriptionId;
      return {
        ...match,
        winnerId,
        result: 'Bye',
        status: 'COMPLETED'
      };
    }

    return {
      ...match,
      winnerId: null,
      result: null,
      status: 'PENDING'
    };
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
