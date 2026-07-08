import { Component, Input, Output, EventEmitter, signal, computed, ElementRef, HostListener, ViewChild, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CourtResponse, DrawResponse, MatchResponse, MatchScheduleTimeType, MatchStatus, SetScoreResponse, TournamentStatus } from '../../../data/interfaces/tournament.model';
import { MatchDetailModalComponent } from './match-detail-modal.component';
import { MatchesComponent } from './matches.component';
import { BracketExportService } from '../services/bracket-export.service';

@Component({
  selector: 'app-bracket',
  standalone: true,
  imports: [CommonModule, MatchDetailModalComponent, MatchesComponent],
  template: `
    <div class="bracket-root space-y-4" #fullscreenRoot>
      <div class="flex items-center justify-between flex-wrap gap-2">
        @if (showTitleInput) {
          <h5 class="text-sm font-medium text-neutral-900">Cuadro de partidos</h5>
        }
        <!-- TOGGLE VISTA MÓVIL/ESCRITORIO -->
        <div class="inline-flex rounded-lg border border-neutral-200 bg-neutral-50 p-1 md:hidden">
          <button
            type="button"
            (click)="mobileViewMode.set('bracket')"
            [class]="mobileViewMode() === 'bracket' ? 'rounded-md bg-white px-2.5 py-1 text-xs font-semibold text-primary-700 shadow-sm' : 'rounded-md px-2.5 py-1 text-xs font-semibold text-neutral-600'"
          >
            Cuadro
          </button>
          <button
            type="button"
            (click)="mobileViewMode.set('list')"
            [class]="mobileViewMode() === 'list' ? 'rounded-md bg-white px-2.5 py-1 text-xs font-semibold text-primary-700 shadow-sm' : 'rounded-md px-2.5 py-1 text-xs font-semibold text-neutral-600'"
          >
            Lista
          </button>
        </div>
      </div>

      @if (draws().length === 0) {
        <p class="text-xs text-neutral-600">Sin cuadros</p>
      } @else if (mobileViewMode() === 'list') {
        <!-- VISTA LISTA RESPONSIVA (Móvil) -->
        <div class="space-y-6 md:hidden">
          @for (draw of draws(); track draw.id) {
            <div class="rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
              <h6 class="font-bold text-neutral-900 border-b border-neutral-100 pb-2 mb-3">{{ draw.label }}</h6>
              <app-matches
                [matchesInput]="draw.matches || []"
                [participantNamesInput]="participantNamesInput"
                [participantOrderInput]="participantOrderInput"
                [showSwapButton]="false"
                (matchSelected)="onMatchClicked($event)"
              ></app-matches>
            </div>
          }
        </div>
      } @else if (isDoubleElimination()) {
        <div class="space-y-4" [class.hidden]="mobileViewMode() === 'list'">
          @if (getWinnersDraw(); as winnersDraw) {
            <div [ngClass]="showDrawCardInput ? 'rounded-md border border-neutral-200 bg-white p-4' : ''">
              @if (showDrawCardInput) {
                <p class="mb-3 font-medium text-neutral-900">{{ winnersDraw.label }} — Cuadro de Ganadores</p>
              }
              @if (winnersDraw.matches && winnersDraw.matches.length > 0) {
                @let rounds = getRounds(winnersDraw.matches);
                <div class="mb-3 flex flex-col gap-2 rounded-lg border border-slate-200 bg-slate-50 p-2 sm:flex-row sm:items-center sm:justify-between">
                  <div class="flex items-center gap-1.5">
                    <button type="button" class="inline-flex h-8 min-w-8 items-center justify-center rounded-md border border-slate-300 bg-white text-sm font-extrabold text-slate-900 transition-colors hover:border-blue-500 hover:bg-blue-50 hover:text-blue-700 disabled:cursor-not-allowed disabled:opacity-45" (click)="zoomOut()" [disabled]="zoomLevel() <= minZoom">
                      -
                    </button>
                    <span class="min-w-14 text-center text-xs font-extrabold text-slate-600">{{ getZoomLabel() }}</span>
                    <button type="button" class="inline-flex h-8 min-w-8 items-center justify-center rounded-md border border-slate-300 bg-white text-sm font-extrabold text-slate-900 transition-colors hover:border-blue-500 hover:bg-blue-50 hover:text-blue-700 disabled:cursor-not-allowed disabled:opacity-45" (click)="zoomIn()" [disabled]="zoomLevel() >= maxZoom">
                      +
                    </button>
                    <button type="button" class="inline-flex h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-sm font-extrabold text-slate-900 transition-colors hover:border-blue-500 hover:bg-blue-50 hover:text-blue-700" (click)="resetZoom()">
                      Restablecer
                    </button>
                  </div>
                  <div class="flex items-center gap-2">
                    <button type="button" class="inline-flex h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-sm font-extrabold text-slate-900 transition-colors hover:border-blue-500 hover:bg-blue-50 hover:text-blue-700" (click)="exportBracketPdf()" [disabled]="isExportingPdf()">
                      {{ isExportingPdf() ? 'Exportando...' : 'Exportar PDF' }}
                    </button>
                    <button type="button" class="inline-flex h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-sm font-extrabold text-slate-900 transition-colors hover:border-blue-500 hover:bg-blue-50 hover:text-blue-700" (click)="toggleFullscreen()">
                      {{ isFullscreen() ? 'Salir pantalla completa' : 'Pantalla completa' }}
                    </button>
                  </div>
                </div>
                <div class="bracket-shell">
                  <div class="bracket-scroll">
                    <div
                      class="bracket-zoom-surface"
                      [style.width.px]="getScaledBoardWidth(winnersDraw.matches)"
                      [style.height.px]="getScaledBoardHeight(rounds)"
                    >
                      <div
                        class="bracket-board"
                        [style.min-width.px]="getMinWidth(winnersDraw.matches)"
                        [style.transform]="getZoomTransform()"
                      >
                        @for (round of rounds; track round.roundNumber; let roundIndex = $index) {
                          <section class="bracket-round">
                            <div class="bracket-round-header">
                              <div>
                                <p class="bracket-round-title">{{ getDoubleEliminationRoundLabel(round.roundNumber, rounds.length, 'WINNERS') }}</p>
                                <p class="bracket-round-subtitle">Ronda {{ round.roundNumber }}</p>
                              </div>
                              <span class="bracket-round-count">{{ round.matches.length }}</span>
                            </div>

                            <div class="bracket-round-matches" [style.height.px]="getBracketBodyHeight(rounds)">
                              @for (match of round.matches; track match.id; let matchIndex = $index) {
                                <button
                                  type="button"
                                  (click)="onMatchClicked(match)"
                                  class="bracket-match"
                                  [class.bracket-match-complete]="!!match.winnerId"
                                  [style.top.px]="getMatchTop(roundIndex, matchIndex)"
                                >
                                  @if (roundIndex > 0) {
                                    <span class="bracket-input-line"></span>
                                  }

                                  @if (!isLastRound(roundIndex, rounds.length)) {
                                    <span class="bracket-output-line"></span>
                                    @if (shouldShowConnectorRail(matchIndex, round.matches.length)) {
                                      <span class="bracket-connector-rail" [style.height.px]="getConnectorHeight(roundIndex)"></span>
                                    }
                                  }

                                  <span class="bracket-match-meta">
                                    <span class="bracket-match-number">P{{ getMatchNumber(match, winnersDraw.matches || []) }}</span>
                                    @if (match.winnerId) {
                                      <span class="bracket-status bracket-status-complete">Finalizado</span>
                                    } @else if (match.result) {
                                      <span class="bracket-status bracket-status-in-progress">En curso</span>
                                    } @else {
                                      <span class="bracket-status bracket-status-pending">Pendiente</span>
                                    }
                                  </span>
                                  <div
                                    class="bracket-player"
                                    [class.bracket-player-winner]="isWinner(match, match.firstInscriptionId)"
                                    [class.bracket-player-empty]="!match.firstInscriptionId && !isByeSlot(match, match.firstInscriptionId, match.secondInscriptionId)"
                                    [class.bracket-player-bye]="isByeSlot(match, match.firstInscriptionId, match.secondInscriptionId)"
                                    [class.bracket-player-draggable]="canDragSlot(match, 'first')"
                                    [class.bracket-player-drag-over]="isDragOver(match.id, 'first')"
                                    [draggable]="canDragSlot(match, 'first')"
                                    (dragstart)="onSlotDragStart($event, match, 'first')"
                                    (dragover)="onSlotDragOver($event, match, 'first')"
                                    (dragleave)="onSlotDragLeave(match.id, 'first')"
                                    (drop)="onSlotDrop($event, match, 'first')"
                                    (dragend)="onSlotDragEnd()"
                                  >
                                    <span class="bracket-player-name">{{ getMatchSlotLabel(match, match.firstInscriptionId, match.secondInscriptionId) }}</span>
                                    @if (match.firstInscriptionId && (match.firstWinPoints != null || match.firstPlayerPoints)) {
                                      <span
                                        class="bracket-points"
                                        [class.bracket-points-winner]="isWinner(match, match.firstInscriptionId)"
                                        [class.bracket-points-loser]="isLoser(match, match.firstInscriptionId)"
                                      >
                                        {{ getWinPointsLabel(match.firstWinPoints) }}
                                      </span>
                                    }
                                    @if (isWinner(match, match.firstInscriptionId)) {
                                      <span class="bracket-winner-mark">G</span>
                                    }
                                  </div>

                                  <div
                                    class="bracket-player"
                                    [class.bracket-player-winner]="isWinner(match, match.secondInscriptionId)"
                                    [class.bracket-player-empty]="!match.secondInscriptionId && !isByeSlot(match, match.secondInscriptionId, match.firstInscriptionId)"
                                    [class.bracket-player-bye]="isByeSlot(match, match.secondInscriptionId, match.firstInscriptionId)"
                                    [class.bracket-player-draggable]="canDragSlot(match, 'second')"
                                    [class.bracket-player-drag-over]="isDragOver(match.id, 'second')"
                                    [draggable]="canDragSlot(match, 'second')"
                                    (dragstart)="onSlotDragStart($event, match, 'second')"
                                    (dragover)="onSlotDragOver($event, match, 'second')"
                                    (dragleave)="onSlotDragLeave(match.id, 'second')"
                                    (drop)="onSlotDrop($event, match, 'second')"
                                    (dragend)="onSlotDragEnd()"
                                  >
                                    <span class="bracket-player-name">{{ getMatchSlotLabel(match, match.secondInscriptionId, match.firstInscriptionId) }}</span>
                                    @if (match.secondInscriptionId && (match.secondWinPoints != null || match.secondPlayerPoints)) {
                                      <span
                                        class="bracket-points"
                                        [class.bracket-points-winner]="isWinner(match, match.secondInscriptionId)"
                                        [class.bracket-points-loser]="isLoser(match, match.secondInscriptionId)"
                                      >
                                        {{ getWinPointsLabel(match.secondWinPoints) }}
                                      </span>
                                    }
                                    @if (isWinner(match, match.secondInscriptionId)) {
                                      <span class="bracket-winner-mark">G</span>
                                    }
                                  </div>

                                  @if (match.result) {
                                    <span class="bracket-result">
                                      {{ match.result }}
                                      @if (match.status === 'IN_PROGRESS' && match.firstPlayerPoints && match.secondPlayerPoints) {
                                        <span class="ml-1 text-primary-600 font-extrabold text-[10px]">
                                          ({{ match.firstPlayerPoints }}-{{ match.secondPlayerPoints }})
                                        </span>
                                      }
                                    </span>
                                  }
                                </button>
                              }
                            </div>
                          </section>
                        }
                      </div>
                    </div>
                  </div>
                </div>
              } @else {
                <p class="text-xs text-neutral-600">Sin partidos generados</p>
              }
            </div>
          }

          <div class="flex items-center gap-3 py-2">
            <div class="h-px flex-1 bg-gradient-to-r from-transparent via-amber-400 to-transparent"></div>
            <span class="rounded-full border border-amber-300 bg-amber-50 px-4 py-1.5 text-sm font-bold text-amber-700 shadow-sm">
              Gran Final
            </span>
            <div class="h-px flex-1 bg-gradient-to-r from-transparent via-amber-400 to-transparent"></div>
          </div>

          @if (getLosersDraw(); as losersDraw) {
            <div [ngClass]="showDrawCardInput ? 'rounded-md border border-neutral-200 bg-white p-4' : ''">
              @if (showDrawCardInput) {
                <p class="mb-3 font-medium text-neutral-900">{{ losersDraw.label }} — Cuadro de Perdedores</p>
              }
              @if (losersDraw.matches && losersDraw.matches.length > 0) {
                @let rounds = getRounds(losersDraw.matches);
                <div class="mb-3 flex flex-col gap-2 rounded-lg border border-slate-200 bg-slate-50 p-2 sm:flex-row sm:items-center sm:justify-between">
                  <div class="flex items-center gap-1.5">
                    <button type="button" class="inline-flex h-8 min-w-8 items-center justify-center rounded-md border border-slate-300 bg-white text-sm font-extrabold text-slate-900 transition-colors hover:border-blue-500 hover:bg-blue-50 hover:text-blue-700 disabled:cursor-not-allowed disabled:opacity-45" (click)="zoomOut()" [disabled]="zoomLevel() <= minZoom">
                      -
                    </button>
                    <span class="min-w-14 text-center text-xs font-extrabold text-slate-600">{{ getZoomLabel() }}</span>
                    <button type="button" class="inline-flex h-8 min-w-8 items-center justify-center rounded-md border border-slate-300 bg-white text-sm font-extrabold text-slate-900 transition-colors hover:border-blue-500 hover:bg-blue-50 hover:text-blue-700 disabled:cursor-not-allowed disabled:opacity-45" (click)="zoomIn()" [disabled]="zoomLevel() >= maxZoom">
                      +
                    </button>
                    <button type="button" class="inline-flex h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-sm font-extrabold text-slate-900 transition-colors hover:border-blue-500 hover:bg-blue-50 hover:text-blue-700" (click)="resetZoom()">
                      Restablecer
                    </button>
                  </div>
                  <div class="flex items-center gap-2">
                    <button type="button" class="inline-flex h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-sm font-extrabold text-slate-900 transition-colors hover:border-blue-500 hover:bg-blue-50 hover:text-blue-700" (click)="exportBracketPdf()" [disabled]="isExportingPdf()">
                      {{ isExportingPdf() ? 'Exportando...' : 'Exportar PDF' }}
                    </button>
                    <button type="button" class="inline-flex h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-sm font-extrabold text-slate-900 transition-colors hover:border-blue-500 hover:bg-blue-50 hover:text-blue-700" (click)="toggleFullscreen()">
                      {{ isFullscreen() ? 'Salir pantalla completa' : 'Pantalla completa' }}
                    </button>
                  </div>
                </div>
                <div class="bracket-shell">
                  <div class="bracket-scroll">
                    <div
                      class="bracket-zoom-surface"
                      [style.width.px]="getScaledBoardWidth(losersDraw.matches)"
                      [style.height.px]="getScaledBoardHeight(rounds)"
                    >
                      <div
                        class="bracket-board"
                        [style.min-width.px]="getMinWidth(losersDraw.matches)"
                        [style.transform]="getZoomTransform()"
                      >
                        @for (round of rounds; track round.roundNumber; let roundIndex = $index) {
                          <section class="bracket-round">
                            <div class="bracket-round-header">
                              <div>
                                <p class="bracket-round-title">{{ getDoubleEliminationRoundLabel(round.roundNumber, rounds.length, 'LOSERS') }}</p>
                                <p class="bracket-round-subtitle">Ronda {{ round.roundNumber }}</p>
                              </div>
                              <span class="bracket-round-count">{{ round.matches.length }}</span>
                            </div>

                            <div class="bracket-round-matches" [style.height.px]="getBracketBodyHeight(rounds)">
                              @for (match of round.matches; track match.id; let matchIndex = $index) {
                                <button
                                  type="button"
                                  (click)="onMatchClicked(match)"
                                  class="bracket-match"
                                  [class.bracket-match-complete]="!!match.winnerId"
                                  [style.top.px]="getMatchTop(roundIndex, matchIndex)"
                                >
                                  @if (roundIndex > 0) {
                                    <span class="bracket-input-line"></span>
                                  }

                                  @if (!isLastRound(roundIndex, rounds.length)) {
                                    <span class="bracket-output-line"></span>
                                    @if (shouldShowConnectorRail(matchIndex, round.matches.length)) {
                                      <span class="bracket-connector-rail" [style.height.px]="getConnectorHeight(roundIndex)"></span>
                                    }
                                  }

                                  <span class="bracket-match-meta">
                                    <span class="bracket-match-number">P{{ getMatchNumber(match, losersDraw.matches || []) }}</span>
                                    @if (match.winnerId) {
                                      <span class="bracket-status bracket-status-complete">Finalizado</span>
                                    } @else if (match.result) {
                                      <span class="bracket-status bracket-status-in-progress">En curso</span>
                                    } @else {
                                      <span class="bracket-status bracket-status-pending">Pendiente</span>
                                    }
                                  </span>
                                  <div
                                    class="bracket-player"
                                    [class.bracket-player-winner]="isWinner(match, match.firstInscriptionId)"
                                    [class.bracket-player-empty]="!match.firstInscriptionId && !isByeSlot(match, match.firstInscriptionId, match.secondInscriptionId)"
                                    [class.bracket-player-bye]="isByeSlot(match, match.firstInscriptionId, match.secondInscriptionId)"
                                    [class.bracket-player-draggable]="canDragSlot(match, 'first')"
                                    [class.bracket-player-drag-over]="isDragOver(match.id, 'first')"
                                    [draggable]="canDragSlot(match, 'first')"
                                    (dragstart)="onSlotDragStart($event, match, 'first')"
                                    (dragover)="onSlotDragOver($event, match, 'first')"
                                    (dragleave)="onSlotDragLeave(match.id, 'first')"
                                    (drop)="onSlotDrop($event, match, 'first')"
                                    (dragend)="onSlotDragEnd()"
                                  >
                                    <span class="bracket-player-name">{{ getMatchSlotLabel(match, match.firstInscriptionId, match.secondInscriptionId) }}</span>
                                    @if (match.firstInscriptionId && (match.firstWinPoints != null || match.firstPlayerPoints)) {
                                      <span
                                        class="bracket-points"
                                        [class.bracket-points-winner]="isWinner(match, match.firstInscriptionId)"
                                        [class.bracket-points-loser]="isLoser(match, match.firstInscriptionId)"
                                      >
                                        {{ getWinPointsLabel(match.firstWinPoints) }}
                                      </span>
                                    }
                                    @if (isWinner(match, match.firstInscriptionId)) {
                                      <span class="bracket-winner-mark">G</span>
                                    }
                                  </div>

                                  <div
                                    class="bracket-player"
                                    [class.bracket-player-winner]="isWinner(match, match.secondInscriptionId)"
                                    [class.bracket-player-empty]="!match.secondInscriptionId && !isByeSlot(match, match.secondInscriptionId, match.firstInscriptionId)"
                                    [class.bracket-player-bye]="isByeSlot(match, match.secondInscriptionId, match.firstInscriptionId)"
                                    [class.bracket-player-draggable]="canDragSlot(match, 'second')"
                                    [class.bracket-player-drag-over]="isDragOver(match.id, 'second')"
                                    [draggable]="canDragSlot(match, 'second')"
                                    (dragstart)="onSlotDragStart($event, match, 'second')"
                                    (dragover)="onSlotDragOver($event, match, 'second')"
                                    (dragleave)="onSlotDragLeave(match.id, 'second')"
                                    (drop)="onSlotDrop($event, match, 'second')"
                                    (dragend)="onSlotDragEnd()"
                                  >
                                    <span class="bracket-player-name">{{ getMatchSlotLabel(match, match.secondInscriptionId, match.firstInscriptionId) }}</span>
                                    @if (match.secondInscriptionId && (match.secondWinPoints != null || match.secondPlayerPoints)) {
                                      <span
                                        class="bracket-points"
                                        [class.bracket-points-winner]="isWinner(match, match.secondInscriptionId)"
                                        [class.bracket-points-loser]="isLoser(match, match.secondInscriptionId)"
                                      >
                                        {{ getWinPointsLabel(match.secondWinPoints) }}
                                      </span>
                                    }
                                    @if (isWinner(match, match.secondInscriptionId)) {
                                      <span class="bracket-winner-mark">G</span>
                                    }
                                  </div>

                                  @if (match.result) {
                                    <span class="bracket-result">
                                      {{ match.result }}
                                      @if (match.status === 'IN_PROGRESS' && match.firstPlayerPoints && match.secondPlayerPoints) {
                                        <span class="ml-1 text-primary-600 font-extrabold text-[10px]">
                                          ({{ match.firstPlayerPoints }}-{{ match.secondPlayerPoints }})
                                        </span>
                                      }
                                    </span>
                                  }
                                </button>
                              }
                            </div>
                          </section>
                        }
                      </div>
                    </div>
                  </div>
                </div>
              } @else {
                <p class="text-xs text-neutral-600">Sin partidos generados</p>
              }
            </div>
          }
        </div>
      } @else {
        <div class="space-y-4">
          @for (draw of draws(); track draw.id) {
            <div [ngClass]="showDrawCardInput ? 'rounded-md border border-neutral-200 bg-white p-4' : ''">
              @if (showDrawCardInput) {
                <p class="mb-3 font-medium text-neutral-900">{{ draw.label }}</p>
              }

              @if (draw.matches && draw.matches.length > 0) {
                @let rounds = getRounds(draw.matches);
                <div class="mb-3 flex flex-col gap-2 rounded-lg border border-slate-200 bg-slate-50 p-2 sm:flex-row sm:items-center sm:justify-between">
                  <div class="flex items-center gap-1.5">
                    <button type="button" class="inline-flex h-8 min-w-8 items-center justify-center rounded-md border border-slate-300 bg-white text-sm font-extrabold text-slate-900 transition-colors hover:border-blue-500 hover:bg-blue-50 hover:text-blue-700 disabled:cursor-not-allowed disabled:opacity-45" (click)="zoomOut()" [disabled]="zoomLevel() <= minZoom">
                      -
                    </button>
                    <span class="min-w-14 text-center text-xs font-extrabold text-slate-600">{{ getZoomLabel() }}</span>
                    <button type="button" class="inline-flex h-8 min-w-8 items-center justify-center rounded-md border border-slate-300 bg-white text-sm font-extrabold text-slate-900 transition-colors hover:border-blue-500 hover:bg-blue-50 hover:text-blue-700 disabled:cursor-not-allowed disabled:opacity-45" (click)="zoomIn()" [disabled]="zoomLevel() >= maxZoom">
                      +
                    </button>
                    <button type="button" class="inline-flex h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-sm font-extrabold text-slate-900 transition-colors hover:border-blue-500 hover:bg-blue-50 hover:text-blue-700" (click)="resetZoom()">
                      Restablecer
                    </button>
                  </div>
                  <div class="flex items-center gap-2">
                    <button type="button" class="inline-flex h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-sm font-extrabold text-slate-900 transition-colors hover:border-blue-500 hover:bg-blue-50 hover:text-blue-700" (click)="exportBracketPdf()" [disabled]="isExportingPdf()">
                      {{ isExportingPdf() ? 'Exportando...' : 'Exportar PDF' }}
                    </button>
                    <button type="button" class="inline-flex h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-sm font-extrabold text-slate-900 transition-colors hover:border-blue-500 hover:bg-blue-50 hover:text-blue-700" (click)="toggleFullscreen()">
                      {{ isFullscreen() ? 'Salir pantalla completa' : 'Pantalla completa' }}
                    </button>
                  </div>
                </div>
                <div class="bracket-shell">
                  <div class="bracket-scroll">
                    <div
                      class="bracket-zoom-surface"
                      [style.width.px]="getScaledBoardWidth(draw.matches)"
                      [style.height.px]="getScaledBoardHeight(rounds)"
                    >
                      <div
                        class="bracket-board"
                        [style.min-width.px]="getMinWidth(draw.matches)"
                        [style.transform]="getZoomTransform()"
                      >
                        @for (round of rounds; track round.roundNumber; let roundIndex = $index) {
                          <section class="bracket-round">
                            <div class="bracket-round-header">
                              <div>
                                <p class="bracket-round-title">{{ getRoundLabel(round.roundNumber, rounds.length) }}</p>
                                <p class="bracket-round-subtitle">Ronda {{ round.roundNumber }}</p>
                              </div>
                              <span class="bracket-round-count">{{ round.matches.length }}</span>
                            </div>

                            <div class="bracket-round-matches" [style.height.px]="getBracketBodyHeight(rounds)">
                              @for (match of round.matches; track match.id; let matchIndex = $index) {
                                <button
                                  type="button"
                                  (click)="onMatchClicked(match)"
                                  class="bracket-match"
                                  [class.bracket-match-complete]="!!match.winnerId"
                                  [style.top.px]="getMatchTop(roundIndex, matchIndex)"
                                >
                                  @if (roundIndex > 0) {
                                    <span class="bracket-input-line"></span>
                                  }

                                  @if (!isLastRound(roundIndex, rounds.length)) {
                                    <span class="bracket-output-line"></span>
                                    @if (shouldShowConnectorRail(matchIndex, round.matches.length)) {
                                      <span class="bracket-connector-rail" [style.height.px]="getConnectorHeight(roundIndex)"></span>
                                    }
                                  }

                                  <span class="bracket-match-meta">
                                    <span class="bracket-match-number">P{{ getMatchNumber(match, draw.matches || []) }}</span>
                                    @if (match.winnerId) {
                                      <span class="bracket-status bracket-status-complete">Finalizado</span>
                                    } @else if (match.result) {
                                      <span class="bracket-status bracket-status-in-progress">En curso</span>
                                    } @else {
                                      <span class="bracket-status bracket-status-pending">Pendiente</span>
                                    }
                                  </span>
                                  <div
                                    class="bracket-player"
                                    [class.bracket-player-winner]="isWinner(match, match.firstInscriptionId)"
                                    [class.bracket-player-empty]="!match.firstInscriptionId && !isByeSlot(match, match.firstInscriptionId, match.secondInscriptionId)"
                                    [class.bracket-player-bye]="isByeSlot(match, match.firstInscriptionId, match.secondInscriptionId)"
                                    [class.bracket-player-draggable]="canDragSlot(match, 'first')"
                                    [class.bracket-player-drag-over]="isDragOver(match.id, 'first')"
                                    [draggable]="canDragSlot(match, 'first')"
                                    (dragstart)="onSlotDragStart($event, match, 'first')"
                                    (dragover)="onSlotDragOver($event, match, 'first')"
                                    (dragleave)="onSlotDragLeave(match.id, 'first')"
                                    (drop)="onSlotDrop($event, match, 'first')"
                                    (dragend)="onSlotDragEnd()"
                                  >
                                    <span class="bracket-player-name">{{ getMatchSlotLabel(match, match.firstInscriptionId, match.secondInscriptionId) }}</span>
                                    @if (match.firstInscriptionId && (match.firstWinPoints != null || match.firstPlayerPoints)) {
                                      <span
                                        class="bracket-points"
                                        [class.bracket-points-winner]="isWinner(match, match.firstInscriptionId)"
                                        [class.bracket-points-loser]="isLoser(match, match.firstInscriptionId)"
                                      >
                                        {{ getWinPointsLabel(match.firstWinPoints) }}
                                      </span>
                                    }
                                    @if (isWinner(match, match.firstInscriptionId)) {
                                      <span class="bracket-winner-mark">G</span>
                                    }
                                  </div>

                                  <div
                                    class="bracket-player"
                                    [class.bracket-player-winner]="isWinner(match, match.secondInscriptionId)"
                                    [class.bracket-player-empty]="!match.secondInscriptionId && !isByeSlot(match, match.secondInscriptionId, match.firstInscriptionId)"
                                    [class.bracket-player-bye]="isByeSlot(match, match.secondInscriptionId, match.firstInscriptionId)"
                                    [class.bracket-player-draggable]="canDragSlot(match, 'second')"
                                    [class.bracket-player-drag-over]="isDragOver(match.id, 'second')"
                                    [draggable]="canDragSlot(match, 'second')"
                                    (dragstart)="onSlotDragStart($event, match, 'second')"
                                    (dragover)="onSlotDragOver($event, match, 'second')"
                                    (dragleave)="onSlotDragLeave(match.id, 'second')"
                                    (drop)="onSlotDrop($event, match, 'second')"
                                    (dragend)="onSlotDragEnd()"
                                  >
                                    <span class="bracket-player-name">{{ getMatchSlotLabel(match, match.secondInscriptionId, match.firstInscriptionId) }}</span>
                                    @if (match.secondInscriptionId && (match.secondWinPoints != null || match.secondPlayerPoints)) {
                                      <span
                                        class="bracket-points"
                                        [class.bracket-points-winner]="isWinner(match, match.secondInscriptionId)"
                                        [class.bracket-points-loser]="isLoser(match, match.secondInscriptionId)"
                                      >
                                        {{ getWinPointsLabel(match.secondWinPoints) }}
                                      </span>
                                    }
                                    @if (isWinner(match, match.secondInscriptionId)) {
                                      <span class="bracket-winner-mark">G</span>
                                    }
                                  </div>

                                  @if (match.result) {
                                    <span class="bracket-result">
                                      {{ match.result }}
                                      @if (match.status === 'IN_PROGRESS' && match.firstPlayerPoints && match.secondPlayerPoints) {
                                        <span class="ml-1 text-primary-600 font-extrabold text-[10px]">
                                          ({{ match.firstPlayerPoints }}-{{ match.secondPlayerPoints }})
                                        </span>
                                      }
                                    </span>
                                  }
                                </button>
                              }
                            </div>
                          </section>
                        }
                      </div>
                    </div>
                  </div>
                </div>
              } @else {
                <p class="text-xs text-neutral-600">Sin partidos generados</p>
              }
            </div>
          }
        </div>
      }

      <app-match-detail-modal
        #matchModal
        [matchInput]="selectedMatch()"
        [participantNamesInput]="participantNamesInput"
        [courtsInput]="courtsInput"
        [canManageInput]="canManageInput"
        [setsPerMatch]="setsPerMatch"
        [decisiveTiebreakPoints]="decisiveTiebreakPoints"
        [gamesPerSet]="gamesPerSet"
        (saveResult)="onSaveMatchResult($event)"
        (saveSchedule)="onSaveMatchSchedule($event)"
        (close)="onModalClose()"
      ></app-match-detail-modal>
    </div>
  `,
  styles: [`
    .bracket-shell {
      border: 1px solid rgb(226 232 240);
      border-radius: 0.5rem;
      background: linear-gradient(180deg, rgb(248 250 252), rgb(255 255 255));
    }

    .bracket-root:fullscreen {
      overflow: auto;
      background: rgb(255 255 255);
      padding: 1rem;
    }

    .bracket-scroll {
      overflow-x: auto;
      overflow-y: hidden;
      padding: 1rem 1.25rem 1.25rem;
      scrollbar-width: thin;
      -webkit-overflow-scrolling: touch;
      position: relative;
      /* Indicadores de scroll gradientes en los extremos */
      background-image: 
        linear-gradient(to right, white, white),
        linear-gradient(to right, white, white),
        linear-gradient(to right, rgba(0, 0, 0, 0.08), rgba(0, 0, 0, 0)),
        linear-gradient(to left, rgba(0, 0, 0, 0.08), rgba(0, 0, 0, 0));
      background-position: left center, right center, left center, right center;
      background-repeat: no-repeat;
      background-color: white;
      background-size: 20px 100%, 20px 100%, 10px 100%, 10px 100%;
      background-attachment: local, local, scroll, scroll;
    }

    .bracket-zoom-surface {
      position: relative;
    }

    .bracket-board {
      display: flex;
      gap: 1rem;
      align-items: flex-start;
      transform-origin: top left;
    }

    @media (min-width: 768px) {
      .bracket-board {
        gap: 3.5rem;
      }
    }

    .bracket-round {
      width: 14rem;
      flex: 0 0 14rem;
    }

    @media (min-width: 768px) {
      .bracket-round {
        width: 19rem;
        flex: 0 0 19rem;
      }
    }

    .bracket-round-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.625rem;
      margin-bottom: 0.875rem;
      min-height: 2.25rem;
      border-bottom: 1px solid rgb(226 232 240);
      padding-bottom: 0.625rem;
    }

    .bracket-round-title {
      color: rgb(15 23 42);
      font-size: 0.875rem;
      font-weight: 800;
      line-height: 1.1;
    }

    .bracket-round-subtitle {
      margin-top: 0.125rem;
      color: rgb(100 116 139);
      font-size: 0.6875rem;
      font-weight: 700;
      text-transform: uppercase;
    }

    .bracket-round-count {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 1.75rem;
      height: 1.5rem;
      border-radius: 9999px;
      background: rgb(241 245 249);
      color: rgb(71 85 105);
      font-size: 0.6875rem;
      font-weight: 800;
    }

    .bracket-round-matches {
      position: relative;
    }

    .bracket-match {
      position: absolute;
      left: 0;
      display: flex;
      width: 100%;
      flex-direction: column;
      gap: 0.25rem;
      border: 1px solid rgb(203 213 225);
      border-left: 3px solid rgb(100 116 139);
      border-radius: 0.5rem;
      background: rgb(255 255 255);
      padding: 0.35rem;
      text-align: left;
      box-shadow: 0 8px 18px rgb(15 23 42 / 0.06);
      transition: border-color 160ms ease, box-shadow 160ms ease, transform 160ms ease, background 160ms ease;
      z-index: 2;
    }

    @media (min-width: 768px) {
      .bracket-match {
        gap: 0.375rem;
        padding: 0.625rem;
      }
    }

    .bracket-match:hover,
    .bracket-match:focus-visible {
      border-color: rgb(59 130 246);
      background: rgb(248 250 252);
      box-shadow: 0 12px 26px rgb(37 99 235 / 0.14);
      outline: none;
      transform: translateY(-2px);
    }

    .bracket-match-complete {
      border-left-color: rgb(22 163 74);
    }

    .bracket-match-meta {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.5rem;
      color: rgb(71 85 105);
      font-size: 0.6875rem;
      font-weight: 600;
    }

    .bracket-match-number {
      color: rgb(30 41 59);
      font-weight: 800;
      letter-spacing: 0;
    }

    .bracket-status {
      flex: 0 0 auto;
      border-radius: 9999px;
      padding: 0.125rem 0.375rem;
      font-size: 0.625rem;
      font-weight: 700;
    }

    .bracket-status-complete {
      background: rgb(220 252 231);
      color: rgb(22 101 52);
    }

    .bracket-status-in-progress {
      background: rgb(219 234 254);
      color: rgb(30 64 175);
    }

    .bracket-status-pending {
      background: rgb(254 249 195);
      color: rgb(133 77 14);
    }

    .bracket-player {
      display: grid;
      grid-template-columns: minmax(0, 1fr) auto auto;
      align-items: center;
      gap: 0.25rem;
      min-height: 2.5rem;
      border: 1px solid rgb(226 232 240);
      border-radius: 0.375rem;
      background: rgb(248 250 252);
      padding: 0.4rem 0.5rem;
      color: rgb(15 23 42);
      font-size: 0.75rem;
      font-weight: 700;
      line-height: 1.3;
    }

    @media (min-width: 768px) {
      .bracket-player {
        gap: 0.5rem;
        min-height: 3.75rem;
        padding: 0.875rem 0.75rem;
        font-size: 0.875rem;
        line-height: 1.5;
      }
    }

    .bracket-player-draggable {
      cursor: grab;
    }

    .bracket-player-drag-over {
      border-color: rgb(96 165 250);
      background: rgb(239 246 255);
      box-shadow: inset 0 0 0 1px rgb(147 197 253);
    }

    .bracket-player-name {
      display: block;
      min-width: 0;
      overflow: visible;
      text-overflow: ellipsis;
      white-space: nowrap;
      line-height: 1.5;
      padding-block: 0.125rem;
    }

    .bracket-player-empty {
      color: rgb(148 163 184);
      font-style: italic;
      font-weight: 600;
    }

    .bracket-player-bye {
      border-style: dashed;
      color: rgb(100 116 139);
      font-style: italic;
      font-weight: 700;
    }

    .bracket-player-winner {
      border-color: rgb(134 239 172);
      background: rgb(240 253 244);
      color: rgb(20 83 45);
    }

    .bracket-points {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 2.5rem;
      height: 1.375rem;
      flex: 0 0 auto;
      border-radius: 9999px;
      background: rgb(241 245 249);
      color: rgb(100 116 139);
      font-size: 0.6875rem;
      font-weight: 800;
      white-space: nowrap;
    }

    .bracket-points-winner {
      background: rgb(220 252 231);
      color: rgb(21 128 61);
      box-shadow: inset 0 0 0 1px rgb(134 239 172);
    }

    .bracket-points-loser {
      background: rgb(248 250 252);
      color: rgb(148 163 184);
      text-decoration: line-through;
      text-decoration-thickness: 2px;
    }

    .bracket-winner-mark {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 1.5rem;
      height: 1.5rem;
      flex: 0 0 auto;
      border-radius: 9999px;
      background: rgb(22 163 74);
      color: white;
      font-size: 0.75rem;
      font-weight: 800;
    }

    .bracket-result {
      align-self: flex-start;
      border-radius: 0.375rem;
      background: rgb(239 246 255);
      padding: 0.1875rem 0.4375rem;
      color: rgb(30 64 175);
      font-size: 0.6875rem;
      font-weight: 700;
    }

    .bracket-input-line,
    .bracket-output-line,
    .bracket-connector-rail {
      position: absolute;
      pointer-events: none;
      background: rgb(148 163 184);
      z-index: 1;
      display: none;
    }

    @media (min-width: 768px) {
      .bracket-input-line,
      .bracket-output-line,
      .bracket-connector-rail {
        display: block;
      }
    }

    .bracket-input-line {
      top: 50%;
      left: -1.75rem;
      width: 1.75rem;
      height: 2px;
    }

    .bracket-output-line {
      top: 50%;
      right: -1.75rem;
      width: 1.75rem;
      height: 2px;
    }

    .bracket-connector-rail {
      top: 50%;
      right: -1.75rem;
      width: 2px;
    }
  `]
})
export class BracketComponent implements OnDestroy {
  private static readonly LOCKED_TOURNAMENT_STATUSES: TournamentStatus[] = ['IN_PROGRESS', 'COMPLETED', 'CANCELLED'];
  private readonly matchHeight = 170;
  private readonly slotPitch = 200;
  readonly minZoom = 0.6;
  readonly maxZoom = 1.6;
  private readonly zoomStep = 0.1;

  private readonly bracketExportService = inject(BracketExportService);

  @Input() participantNamesInput: Record<string, string> = {};
  @Input() participantOrderInput: Record<string, number> = {};
  @Input() courtsInput: CourtResponse[] = [];
  @Input() showTitleInput = true;
  @Input() showDrawCardInput = true;
  @Input() canManageInput = false;
  @Input() tournamentStatusInput: TournamentStatus = 'DRAFT';
  @Input() tournamentNameInput = '';
  @Input() categoryNameInput = '';
  @Input() setsPerMatch = 3;
  @Input() decisiveTiebreakPoints = 7;
  @Input() gamesPerSet = 6;

  @Input() set drawsInput(value: DrawResponse[]) {
    this._draws.set(value);
    const sel = this.selectedMatch();
    if (sel) {
      for (const draw of value) {
        const updated = (draw.matches || []).find(m => m.id === sel.id);
        if (updated) {
          this.selectedMatch.set(updated);
          break;
        }
      }
    }
  }
  private _draws = signal<DrawResponse[]>([]);
  draws = computed(() => this._draws());
  zoomLevel = signal(1);
  isFullscreen = signal(false);
  isExportingPdf = signal(false);
  mobileViewMode = signal<'bracket' | 'list'>('bracket');
  private wakeLock: WakeLockSentinel | null = null;
  selectedMatch = signal<MatchResponse | null>(null);

  @ViewChild('fullscreenRoot') fullscreenRoot?: ElementRef<HTMLElement>;
  @ViewChild('matchModal') matchModal?: MatchDetailModalComponent;
  @ViewChild('bracketBoard') bracketBoard?: ElementRef<HTMLElement>;

  @Output() matchSelected = new EventEmitter<MatchResponse>();
  @Output() matchResultSaved = new EventEmitter<{ matchId: string; winnerId: string | null; result: string; sets?: SetScoreResponse[] | null; notes?: string | null; status: MatchStatus; keepOpen?: boolean }>();
  @Output() matchScheduleSaved = new EventEmitter<{
    matchId: string;
    courtId: string;
    scheduledAt: string;
    scheduleTimeType: MatchScheduleTimeType;
  }>();
  @Output() playersSwapped = new EventEmitter<{
    matchId1: string;
    slot1: 'first' | 'second';
    matchId2: string;
    slot2: 'first' | 'second';
  }>();

  private readonly draggedSlot = signal<{
    matchId: string;
    slot: 'first' | 'second';
    inscriptionId: string;
  } | null>(null);
  private readonly dragOverKey = signal<string | null>(null);

  isDoubleElimination = computed(() => {
    const draws = this.draws();
    if (draws.length !== 2) {
      return false;
    }
    return (
      (draws[0].drawType === 'ELIMINATION' && draws[1].drawType === 'DOUBLE_ELIMINATION') ||
      (draws[0].drawType === 'DOUBLE_ELIMINATION' && draws[1].drawType === 'ELIMINATION')
    );
  });

  getWinnersDraw = computed(() => {
    if (!this.isDoubleElimination()) {
      return null;
    }
    return this.draws().find(d => d.drawType === 'ELIMINATION') ?? null;
  });

  getLosersDraw = computed(() => {
    if (!this.isDoubleElimination()) {
      return null;
    }
    return this.draws().find(d => d.drawType === 'DOUBLE_ELIMINATION') ?? null;
  });

  @HostListener('document:fullscreenchange')
  onFullscreenChange(): void {
    const isNowFullscreen = document.fullscreenElement === this.fullscreenRoot?.nativeElement;
    this.isFullscreen.set(isNowFullscreen);

    if (isNowFullscreen) {
      this.requestWakeLock();
    } else {
      this.releaseWakeLock();
    }
  }

  @HostListener('document:visibilitychange')
  onVisibilityChange(): void {
    if (document.visibilityState === 'visible' && this.isFullscreen()) {
      this.reEnterFullscreen();
    }
  }

  private async reEnterFullscreen(): Promise<void> {
    const fullscreenElement = this.fullscreenRoot?.nativeElement;
    if (!fullscreenElement || document.fullscreenElement === fullscreenElement) {
      return;
    }

    try {
      await fullscreenElement.requestFullscreen();
    } catch {
      // Ignore errors when re-entering fullscreen
    }
  }

  private async requestWakeLock(): Promise<void> {
    if (!('wakeLock' in navigator)) {
      return;
    }

    try {
      this.wakeLock = await navigator.wakeLock.request('screen');
      this.wakeLock.addEventListener('release', () => {
        this.wakeLock = null;
      });
    } catch {
      // Wake lock not supported or denied
    }
  }

  private releaseWakeLock(): void {
    if (this.wakeLock) {
      this.wakeLock.release().catch(() => undefined);
      this.wakeLock = null;
    }
  }

  onMatchClicked(match: MatchResponse) {
    this.selectedMatch.set(match);
    this.matchModal?.open();
    this.matchSelected.emit(match);
  }

  onModalClose(): void {
    this.selectedMatch.set(null);
  }

  onSaveMatchResult(event: { matchId: string; winnerId: string | null; result: string; sets?: SetScoreResponse[] | null; notes?: string | null; status: MatchStatus; keepOpen?: boolean }): void {
    if (!this.canManageInput) {
      return;
    }

    this.matchResultSaved.emit(event);
    if (!event.keepOpen) {
      this.selectedMatch.set(null);
    }
  }

  onSaveMatchSchedule(event: {
    matchId: string;
    courtId: string;
    scheduledAt: string;
    scheduleTimeType: MatchScheduleTimeType;
  }): void {
    if (!this.canManageInput) {
      return;
    }

    this.matchScheduleSaved.emit(event);
  }

  getRounds(matches: MatchResponse[] | undefined): Array<{ roundNumber: number; matches: MatchResponse[] }> {
    if (!matches) {
      return [];
    }

    const roundMap = new Map<number, MatchResponse[]>();
    matches.forEach((match) => {
      const roundNum = match.roundNumber || 1;
      if (!roundMap.has(roundNum)) {
        roundMap.set(roundNum, []);
      }
      roundMap.get(roundNum)!.push(match);
    });

    return Array.from(roundMap.entries())
      .sort((a, b) => a[0] - b[0])
      .map(([roundNumber, roundMatches]) => ({
        roundNumber,
        matches: this.sortRoundMatches(roundMatches)
      }));
  }

  getMinWidth(matches: MatchResponse[] | undefined): number {
    if (!matches) {
      return 400;
    }
    const rounds = this.getRounds(matches);
    const isMobile = typeof window !== 'undefined' && window.innerWidth < 768;
    const colWidth = isMobile ? 224 : 304; // 14rem vs 19rem
    const gapWidth = isMobile ? 16 : 56;    // 1rem vs 3.5rem
    return Math.max(isMobile ? 280 : 360, rounds.length * colWidth + Math.max(0, rounds.length - 1) * gapWidth);
  }

  getScaledBoardWidth(matches: MatchResponse[] | undefined): number {
    return this.getMinWidth(matches) * this.zoomLevel();
  }

  getScaledBoardHeight(rounds: Array<{ roundNumber: number; matches: MatchResponse[] }>): number {
    return this.getBracketContentHeight(rounds) * this.zoomLevel();
  }

  getBracketContentHeight(rounds: Array<{ roundNumber: number; matches: MatchResponse[] }>): number {
    return 56 + this.getBracketBodyHeight(rounds);
  }

  getRoundLabel(roundNumber: number, totalRounds: number): string {
    if (roundNumber === totalRounds) {
      return 'Final';
    }

    if (roundNumber === totalRounds - 1) {
      return 'Semifinales';
    }

    if (roundNumber === totalRounds - 2) {
      return 'Cuartos';
    }

    return `Ronda ${roundNumber}`;
  }

  getDoubleEliminationRoundLabel(
    roundNumber: number,
    totalRounds: number,
    bracket: 'WINNERS' | 'LOSERS'
  ): string {
    if (bracket === 'WINNERS') {
      if (roundNumber === totalRounds) {
        return 'Gran Final';
      }
      if (roundNumber === totalRounds - 1) {
        return 'Semifinal Ganadores';
      }
      if (roundNumber === totalRounds - 2) {
        return 'Cuartos Ganadores';
      }
      return `Ronda Ganadores ${roundNumber}`;
    }

    return `Ronda Perdedores ${roundNumber}`;
  }

  getBracketBodyHeight(rounds: Array<{ roundNumber: number; matches: MatchResponse[] }>): number {
    const firstRoundMatchCount = rounds[0]?.matches.length ?? 1;
    const isMobile = typeof window !== 'undefined' && window.innerWidth < 768;
    const currentSlotPitch = isMobile ? 130 : this.slotPitch;
    const currentMatchHeight = isMobile ? 110 : this.matchHeight;
    return Math.max(isMobile ? 240 : 360, (firstRoundMatchCount - 1) * currentSlotPitch + currentMatchHeight + 24);
  }

  getMatchTop(roundIndex: number, matchIndex: number): number {
    const roundSpan = 2 ** roundIndex;
    const isMobile = typeof window !== 'undefined' && window.innerWidth < 768;
    const currentSlotPitch = isMobile ? 130 : this.slotPitch;
    return (matchIndex * roundSpan + (roundSpan - 1) / 2) * currentSlotPitch;
  }

  getRoundTopPadding(roundIndex: number): number {
    return roundIndex === 0 ? 0 : Math.min(156, 36 * roundIndex);
  }

  getRoundGap(roundIndex: number): number {
    return 18 + roundIndex * 64;
  }

  getConnectorHeight(roundIndex: number): number {
    const isMobile = typeof window !== 'undefined' && window.innerWidth < 768;
    const currentSlotPitch = isMobile ? 130 : this.slotPitch;
    return currentSlotPitch * (2 ** roundIndex);
  }

  isLastRound(roundIndex: number, totalRounds: number): boolean {
    return roundIndex === totalRounds - 1;
  }

  shouldShowConnectorRail(matchIndex: number, roundMatchCount: number): boolean {
    return matchIndex % 2 === 0 && matchIndex + 1 < roundMatchCount;
  }

  getMatchNumber(match: MatchResponse, allMatches: MatchResponse[]): number {
    const matchesInRound = this.sortRoundMatches(allMatches.filter((m) => m.roundNumber === match.roundNumber));
    return (matchesInRound.indexOf(match) + 1) || 1;
  }

  isWinner(match: MatchResponse, inscriptionId: string | null | undefined): boolean {
    return !!inscriptionId && match.winnerId === inscriptionId;
  }

  isLoser(match: MatchResponse, inscriptionId: string | null | undefined): boolean {
    return !!inscriptionId && !!match.winnerId && match.winnerId !== inscriptionId;
  }

  getMatchSlotLabel(
    match: MatchResponse,
    inscriptionId: string | null | undefined,
    opponentInscriptionId: string | null | undefined
  ): string {
    if (this.isByeSlot(match, inscriptionId, opponentInscriptionId)) {
      return 'Bye';
    }

    return this.getParticipantName(inscriptionId);
  }

  isByeSlot(
    match: MatchResponse,
    inscriptionId: string | null | undefined,
    opponentInscriptionId: string | null | undefined
  ): boolean {
    return !inscriptionId && !!opponentInscriptionId && (match.roundNumber ?? 1) === 1;
  }

  getParticipantName(inscriptionId: string | null | undefined): string {
    if (!inscriptionId) {
      return 'Por definir';
    }

    return this.sanitizeParticipantName(this.participantNamesInput[inscriptionId]) ?? inscriptionId.substring(0, 8);
  }

  getWinPointsLabel(points: number | null | undefined): string {
    return points == null ? '+0 pts' : `+${points} pts`;
  }

  canDragSlot(match: MatchResponse, slot: 'first' | 'second'): boolean {
    if (!this.canManageInput || this.isTournamentStatusLockedForReorganization()) {
      return false;
    }

    if ((match.roundNumber ?? 1) !== 1) {
      return false;
    }

    return !!this.getSlotInscriptionId(match, slot);
  }

  isDragOver(matchId: string, slot: 'first' | 'second'): boolean {
    return this.dragOverKey() === this.getSlotKey(matchId, slot);
  }

  onSlotDragStart(event: DragEvent, match: MatchResponse, slot: 'first' | 'second'): void {
    const inscriptionId = this.getSlotInscriptionId(match, slot);
    if (!inscriptionId || !this.canDragSlot(match, slot)) {
      event.preventDefault();
      return;
    }

    event.stopPropagation();
    this.draggedSlot.set({
      matchId: match.id,
      slot,
      inscriptionId
    });

    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
      event.dataTransfer.setData('text/plain', `${match.id}:${slot}`);
    }
  }

  onSlotDragOver(event: DragEvent, match: MatchResponse, slot: 'first' | 'second'): void {
    if (!this.canDropOnSlot(match, slot)) {
      return;
    }

    event.preventDefault();
    event.stopPropagation();
    this.dragOverKey.set(this.getSlotKey(match.id, slot));
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
  }

  onSlotDragLeave(matchId: string, slot: 'first' | 'second'): void {
    if (this.dragOverKey() === this.getSlotKey(matchId, slot)) {
      this.dragOverKey.set(null);
    }
  }

  onSlotDrop(event: DragEvent, match: MatchResponse, slot: 'first' | 'second'): void {
    event.preventDefault();
    event.stopPropagation();

    const source = this.draggedSlot();
    if (!source || !this.canDropOnSlot(match, slot)) {
      this.clearDragState();
      return;
    }

    this.playersSwapped.emit({
      matchId1: source.matchId,
      slot1: source.slot,
      matchId2: match.id,
      slot2: slot
    });

    this.clearDragState();
  }

  onSlotDragEnd(): void {
    this.clearDragState();
  }

  getZoomLabel(): string {
    return `${Math.round(this.zoomLevel() * 100)}%`;
  }

  getZoomTransform(): string {
    return `scale(${this.zoomLevel()})`;
  }

  zoomIn(): void {
    this.setZoom(this.zoomLevel() + this.zoomStep);
  }

  zoomOut(): void {
    this.setZoom(this.zoomLevel() - this.zoomStep);
  }

  resetZoom(): void {
    this.zoomLevel.set(1);
  }

  toggleFullscreen(): void {
    const fullscreenElement = this.fullscreenRoot?.nativeElement;
    if (!fullscreenElement) {
      return;
    }

    if (document.fullscreenElement === fullscreenElement) {
      document.exitFullscreen().catch(() => undefined);
      this.releaseWakeLock();
      return;
    }

    fullscreenElement.requestFullscreen().catch(() => undefined);
  }

  async exportBracketPdf(): Promise<void> {
    const bracketElement = this.fullscreenRoot?.nativeElement;
    if (!bracketElement || this.isExportingPdf()) {
      return;
    }

    this.isExportingPdf.set(true);

    try {
      const firstDraw = this.draws()[0];
      const drawLabel = firstDraw?.label ?? '';
      await this.bracketExportService.exportBracket(
        bracketElement,
        this.tournamentNameInput,
        this.categoryNameInput,
        drawLabel
      );
    } catch (error) {
      console.error('Error exporting bracket PDF:', error);
    } finally {
      this.isExportingPdf.set(false);
    }
  }

  private setZoom(value: number): void {
    const clampedValue = Math.min(this.maxZoom, Math.max(this.minZoom, value));
    this.zoomLevel.set(Math.round(clampedValue * 10) / 10);
  }

  private sanitizeParticipantName(name: string | undefined): string | null {
    const sanitizedName = name
      ?.replace(/\bnull\b/gi, '')
      .replace(/\bundefined\b/gi, '')
      .replace(/\s+/g, ' ')
      .trim();

    return sanitizedName || null;
  }

  private sortRoundMatches(matches: MatchResponse[]): MatchResponse[] {
    return [...matches].sort((left, right) =>
      this.compareNumbers(left.bracketPosition, right.bracketPosition) ||
      this.compareStrings(left.id, right.id)
    );
  }

  private compareNumbers(left: number | null | undefined, right: number | null | undefined): number {
    return (left ?? Number.MAX_SAFE_INTEGER) - (right ?? Number.MAX_SAFE_INTEGER);
  }

  private compareStrings(left: string | null | undefined, right: string | null | undefined): number {
    return (left ?? '').localeCompare(right ?? '');
  }

  private canDropOnSlot(match: MatchResponse, slot: 'first' | 'second'): boolean {
    if (this.isTournamentStatusLockedForReorganization()) {
      return false;
    }

    const source = this.draggedSlot();
    if (!source) {
      return false;
    }

    if ((match.roundNumber ?? 1) !== 1) {
      return false;
    }

    return source.matchId !== match.id || source.slot !== slot;
  }

  private isTournamentStatusLockedForReorganization(): boolean {
    return BracketComponent.LOCKED_TOURNAMENT_STATUSES.includes(this.tournamentStatusInput);
  }

  private getSlotInscriptionId(match: MatchResponse, slot: 'first' | 'second'): string | null {
    return slot === 'first' ? (match.firstInscriptionId ?? null) : (match.secondInscriptionId ?? null);
  }

  private getSlotKey(matchId: string, slot: 'first' | 'second'): string {
    return `${matchId}:${slot}`;
  }

  private clearDragState(): void {
    this.draggedSlot.set(null);
    this.dragOverKey.set(null);
  }

  ngOnDestroy(): void {
    this.releaseWakeLock();
  }

}
