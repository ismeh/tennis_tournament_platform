import { Component, EventEmitter, HostListener, Input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CourtResponse, MatchResponse, MatchScheduleTimeType, MatchStatus, SetScoreResponse } from '../../../data/interfaces/tournament.model';

@Component({
  selector: 'app-match-detail-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (isOpen()) {
      <div class="fixed inset-0 z-[2147483647] flex items-center justify-center bg-black bg-opacity-50 p-4">
        <div class="w-full max-w-2xl rounded-3xl bg-white shadow-2xl overflow-hidden border border-neutral-100 text-neutral-900">
          <!-- Header -->
          <div class="border-b border-neutral-100 bg-neutral-50/50 px-6 py-4 flex items-center justify-between">
            <div>
              <span class="text-xs font-semibold uppercase tracking-[0.2em] text-primary-600">Ronda {{ match()?.roundNumber }}</span>
              <h3 class="text-lg font-extrabold text-neutral-950">Detalles del partido</h3>
            </div>
            <button
              (click)="onClose()"
              class="rounded-full p-2 text-neutral-400 transition hover:bg-neutral-100 hover:text-neutral-700"
            >
              ✕
            </button>
          </div>

          <!-- Content -->
          <div class="max-h-[70vh] overflow-y-auto px-6 py-4 space-y-6">
            @if (match()) {
              <div class="space-y-5">
                <!-- Participants Summary -->
                <div class="space-y-3">
                  <div class="flex items-center gap-3">
                    <div class="flex-1 rounded-2xl border border-neutral-100 bg-neutral-50/40 p-4">
                      <div class="flex items-center justify-between gap-3">
                        <p class="truncate text-sm font-bold text-neutral-800">
                          {{ getParticipantName(match()?.firstInscriptionId) }}
                        </p>
                        @if (match()?.professionalMatch && match()?.firstInscriptionId) {
                          <span [class]="getWinPointsClasses(match(), match()?.firstInscriptionId)">
                            {{ getWinPointsLabel(match()?.firstWinPoints) }}
                          </span>
                        }
                      </div>
                    </div>

                    <span class="text-xs font-bold text-neutral-400 uppercase tracking-widest shrink-0">VS</span>

                    <div class="flex-1 rounded-2xl border border-neutral-100 bg-neutral-50/40 p-4">
                      <div class="flex items-center justify-between gap-3">
                        <p class="truncate text-sm font-bold text-neutral-800">
                          {{ getParticipantName(match()?.secondInscriptionId) }}
                        </p>
                        @if (match()?.professionalMatch && match()?.secondInscriptionId) {
                          <span [class]="getWinPointsClasses(match(), match()?.secondInscriptionId)">
                            {{ getWinPointsLabel(match()?.secondWinPoints) }}
                          </span>
                        }
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Schedule Info Section -->
                <div class="border-t border-neutral-100 pt-4">
                  <p class="mb-3 text-xs font-bold text-neutral-500 uppercase tracking-widest">Programación del Partido</p>
                  @if (canManageInput) {
                    <div class="grid gap-3 sm:grid-cols-3">
                      <label class="block">
                        <span class="text-xs font-semibold text-neutral-600">Tipo hora</span>
                        <select
                          [(ngModel)]="selectedScheduleTimeType"
                          class="mt-1 w-full rounded-2xl border border-neutral-300 bg-white px-3 py-2.5 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                        >
                          <option value="EXACT">A esta hora</option>
                          <option value="NOT_BEFORE">No antes de</option>
                        </select>
                      </label>

                      <label class="block">
                        <span class="text-xs font-semibold text-neutral-600">Fecha y Hora</span>
                        <input
                          type="datetime-local"
                          [(ngModel)]="scheduledAtInput"
                          class="mt-1 w-full rounded-2xl border border-neutral-300 bg-white px-3 py-2.5 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                        />
                      </label>

                      <label class="block">
                        <span class="text-xs font-semibold text-neutral-600">Pista</span>
                        <select
                          [(ngModel)]="selectedCourtId"
                          class="mt-1 w-full rounded-2xl border border-neutral-300 bg-white px-3 py-2.5 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                        >
                          <option value="">Selecciona pista</option>
                          @for (court of activeCourts(); track court.id) {
                            <option [value]="court.id">{{ court.name }}</option>
                          }
                        </select>
                      </label>
                    </div>

                    @if (activeCourts().length === 0) {
                      <p class="mt-2 text-xs text-amber-700">Crea una pista en el torneo antes de programar partidos.</p>
                    }

                    @if (scheduleValidationMessage()) {
                      <p class="mt-2 text-xs font-medium text-red-600">{{ scheduleValidationMessage() }}</p>
                    }

                    <div class="flex justify-end mt-3">
                      <button
                        type="button"
                        (click)="onSaveSchedule()"
                        [disabled]="!isScheduleValid()"
                        class="rounded-2xl bg-neutral-900 px-4 py-2 text-xs font-bold text-white hover:bg-neutral-800 disabled:cursor-not-allowed disabled:bg-neutral-300"
                      >
                        Guardar programación
                      </button>
                    </div>
                  } @else {
                    <div class="grid gap-3 sm:grid-cols-3">
                      <div class="rounded-2xl border border-neutral-100 bg-neutral-50/30 p-3">
                        <p class="text-xs font-semibold text-neutral-500">Programación</p>
                        <p class="mt-0.5 text-sm font-semibold text-neutral-800">{{ getScheduleTypeLabel(match()?.scheduleTimeType) }}</p>
                      </div>
                      <div class="rounded-2xl border border-neutral-100 bg-neutral-50/30 p-3">
                        <p class="text-xs font-semibold text-neutral-500">Inicio</p>
                        <p class="mt-0.5 text-sm font-semibold text-neutral-800">{{ match()?.scheduledAt || 'Por definir' }}</p>
                      </div>
                      <div class="rounded-2xl border border-neutral-100 bg-neutral-50/30 p-3">
                        <p class="text-xs font-semibold text-neutral-500">Pista</p>
                        <p class="mt-0.5 text-sm font-semibold text-neutral-800">{{ match()?.court || 'Por definir' }}</p>
                      </div>
                    </div>
                  }
                </div>

                <!-- Result Entry Section -->
                <div class="border-t border-neutral-100 pt-4 space-y-4">
                  <p class="text-xs font-bold text-neutral-500 uppercase tracking-widest">Registro de Resultado</p>

                  @if (canManageInput) {
                    <div class="grid gap-4 sm:grid-cols-2">
                      <div>
                        <label class="text-xs font-semibold text-neutral-600">Estado del partido</label>
                        <select
                          [(ngModel)]="selectedStatus"
                          (ngModelChange)="onStatusChanged()"
                          class="mt-1 w-full rounded-2xl border border-neutral-300 bg-white px-3 py-2.5 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                        >
                          <option value="PENDING">Pendiente</option>
                          <option value="IN_PROGRESS">En juego</option>
                          <option value="COMPLETED">Jugado</option>
                          <option value="WALKOVER">Walkover</option>
                          <option value="RETIRED">Retirada</option>
                          <option value="CANCELLED">Cancelado</option>
                          <option value="SUSPENDED">Suspendido</option>
                        </select>
                      </div>

                      <div>
                        <label class="text-xs font-semibold text-neutral-600">Ganador del partido</label>
                        <select
                          [(ngModel)]="selectedWinnerId"
                          (ngModelChange)="onWinnerChanged()"
                          class="mt-1 w-full rounded-2xl border border-neutral-300 bg-white px-3 py-2.5 text-sm text-neutral-800 focus:border-primary-500 focus:outline-none"
                        >
                          <option value="">Selecciona ganador</option>
                          @if (match()?.firstInscriptionId) {
                            <option [value]="match()!.firstInscriptionId">
                              {{ getParticipantName(match()?.firstInscriptionId) }}
                            </option>
                          }
                          @if (match()?.secondInscriptionId) {
                            <option [value]="match()!.secondInscriptionId">
                              {{ getParticipantName(match()?.secondInscriptionId) }}
                            </option>
                          }
                        </select>
                      </div>
                    </div>

                    <!-- Scoring Tabs -->
                    <div class="flex rounded-2xl bg-neutral-100 p-1">
                      <button
                        type="button"
                        (click)="scoringMode.set('interactive')"
                        [class]="scoringMode() === 'interactive' ? 'bg-white font-bold text-neutral-900 shadow-sm' : 'text-neutral-500 hover:text-neutral-900'"
                        class="flex-1 py-2 rounded-xl text-center text-xs transition focus:outline-none"
                      >
                        Marcador Interactivo (Punto a Punto)
                      </button>
                      <button
                        type="button"
                        (click)="scoringMode.set('manual')"
                        [class]="scoringMode() === 'manual' ? 'bg-white font-bold text-neutral-900 shadow-sm' : 'text-neutral-500 hover:text-neutral-900'"
                        class="flex-1 py-2 rounded-xl text-center text-xs transition focus:outline-none"
                      >
                        Resultado por Set (Manual)
                      </button>
                    </div>

                    <!-- Interactive Scoring Mode (LIGHT MODE) -->
                    @if (scoringMode() === 'interactive') {
                      <div class="space-y-4 rounded-3xl bg-gradient-to-br from-neutral-50 to-white p-5 text-neutral-800 border border-neutral-200/70 shadow-sm">
                        <!-- Scoreboard Header -->
                        <div class="grid grid-cols-12 gap-2 text-center text-[10px] font-bold uppercase tracking-wider text-neutral-400 border-b border-neutral-100 pb-3">
                          <span class="col-span-5 text-left">Jugador</span>
                          <span class="col-span-1">S1</span>
                          <span class="col-span-1">S2</span>
                          <span class="col-span-1">S3</span>
                          @if (setsPerMatch === 5) {
                            <span class="col-span-1">S4</span>
                            <span class="col-span-1">S5</span>
                          }
                          <span class="col-span-2 text-primary-600">Puntos</span>
                          <span class="col-span-3 text-right">Controles</span>
                        </div>

                        <!-- Player 1 Score Row -->
                        <div class="grid grid-cols-12 gap-2 items-center text-xs font-bold py-2 border-b border-neutral-100/50">
                          <span class="col-span-5 truncate text-left text-neutral-900 font-extrabold">{{ getParticipantName(match()?.firstInscriptionId) }}</span>
                          @for (s of [0,1,2,3,4]; track $index) {
                            @if ($index < setsPerMatch) {
                              <span class="col-span-1 text-center" [class.text-primary-600]="$index === currentSetIndex()" [class.text-neutral-400]="$index !== currentSetIndex()">
                                {{ getInteractiveSetGames(1, $index) }}
                              </span>
                            }
                          }
                          <span class="col-span-2 text-center text-sm font-black text-primary-600 bg-primary-50 rounded-xl py-1 px-1.5 min-h-[2rem] flex items-center justify-center border border-primary-100">
                            {{ getPointsDisplay(1) }}
                          </span>
                          <!-- Player 1 Controls -->
                          <div class="col-span-3 flex justify-end items-center gap-1.5">
                            <button
                              type="button"
                              (click)="decrementPoint(1)"
                              title="Restar punto"
                              class="rounded-xl border border-neutral-200 bg-white p-2 text-neutral-600 transition hover:bg-neutral-50 hover:text-neutral-900 active:scale-[0.93] flex items-center justify-center shrink-0 w-8 h-8 font-black text-sm"
                            >
                              -
                            </button>
                            <button
                              type="button"
                              (click)="incrementPoint(1)"
                              title="Sumar punto"
                              class="rounded-xl bg-primary-600 p-2 text-white font-black transition hover:bg-primary-500 active:scale-[0.93] flex items-center justify-center shrink-0 w-8 h-8 text-sm"
                            >
                              +
                            </button>
                          </div>
                        </div>

                        <!-- Player 2 Score Row -->
                        <div class="grid grid-cols-12 gap-2 items-center text-xs font-bold py-2 border-b border-neutral-100/50">
                          <span class="col-span-5 truncate text-left text-neutral-900 font-extrabold">{{ getParticipantName(match()?.secondInscriptionId) }}</span>
                          @for (s of [0,1,2,3,4]; track $index) {
                            @if ($index < setsPerMatch) {
                              <span class="col-span-1 text-center" [class.text-primary-600]="$index === currentSetIndex()" [class.text-neutral-400]="$index !== currentSetIndex()">
                                {{ getInteractiveSetGames(2, $index) }}
                              </span>
                            }
                          }
                          <span class="col-span-2 text-center text-sm font-black text-primary-600 bg-primary-50 rounded-xl py-1 px-1.5 min-h-[2rem] flex items-center justify-center border border-primary-100">
                            {{ getPointsDisplay(2) }}
                          </span>
                          <!-- Player 2 Controls -->
                          <div class="col-span-3 flex justify-end items-center gap-1.5">
                            <button
                              type="button"
                              (click)="decrementPoint(2)"
                              title="Restar punto"
                              class="rounded-xl border border-neutral-200 bg-white p-2 text-neutral-600 transition hover:bg-neutral-50 hover:text-neutral-900 active:scale-[0.93] flex items-center justify-center shrink-0 w-8 h-8 font-black text-sm"
                            >
                              -
                            </button>
                            <button
                              type="button"
                              (click)="incrementPoint(2)"
                              title="Sumar punto"
                              class="rounded-xl bg-primary-600 p-2 text-white font-black transition hover:bg-primary-500 active:scale-[0.93] flex items-center justify-center shrink-0 w-8 h-8 text-sm"
                            >
                              +
                            </button>
                          </div>
                        </div>

                        <!-- Warnings & Info -->
                        @if (pendingGamesDecrementPlayer()) {
                          <div class="flex justify-center items-center text-[10px] text-red-600 font-extrabold bg-red-50 rounded-xl py-2 px-3 border border-red-100 transition animate-pulse">
                            Pulsa otra vez "-" para disminuir los juegos de {{ getParticipantLastName(pendingGamesDecrementPlayer() === 1 ? match()?.firstInscriptionId : match()?.secondInscriptionId) }}
                          </div>
                        }

                        @if (isTiebreak()) {
                          <div class="flex justify-center items-center text-[10px] text-amber-600 font-extrabold bg-amber-50 rounded-xl py-2 px-3 border border-amber-100 animate-pulse uppercase tracking-wider">
                            ¡TIEBREAK a {{ isDecisiveSet() ? decisiveTiebreakPoints : 7 }} puntos!
                          </div>
                        }
                      </div>
                    }

                    <!-- Manual Scoring Mode -->
                    @if (scoringMode() === 'manual') {
                      <div class="rounded-2xl border border-neutral-100 bg-neutral-50/50 p-4 space-y-4">
                        <p class="text-xs font-bold text-neutral-500 uppercase tracking-widest">Introducir juegos por set</p>
                        @for (set of manualSets(); track $index) {
                          <div class="flex items-center justify-between gap-4 py-1.5 border-b border-neutral-200/50 last:border-0">
                            <span class="w-16 text-xs font-extrabold text-neutral-500">SET {{ $index + 1 }}</span>
                            <div class="flex items-center gap-3 flex-1">
                              <!-- Player 1 games input -->
                              <div class="flex items-center gap-1.5">
                                <span class="text-[10px] text-neutral-400 uppercase font-bold w-12 truncate text-right">P1</span>
                                <input
                                  type="number"
                                  min="0"
                                  max="7"
                                  placeholder="0"
                                  [(ngModel)]="set.p1Games"
                                  (ngModelChange)="onManualScoreChanged()"
                                  class="w-14 rounded-xl border border-neutral-300 bg-white px-2 py-1.5 text-center text-sm font-semibold focus:border-primary-500 focus:outline-none"
                                />
                              </div>
                              <span class="text-neutral-400">-</span>
                              <!-- Player 2 games input -->
                              <div class="flex items-center gap-1.5">
                                <input
                                  type="number"
                                  min="0"
                                  max="7"
                                  placeholder="0"
                                  [(ngModel)]="set.p2Games"
                                  (ngModelChange)="onManualScoreChanged()"
                                  class="w-14 rounded-xl border border-neutral-300 bg-white px-2 py-1.5 text-center text-sm font-semibold focus:border-primary-500 focus:outline-none"
                                />
                                <span class="text-[10px] text-neutral-400 uppercase font-bold w-12 truncate text-left">P2</span>
                              </div>
                            </div>

                            <!-- Tiebreak score input (shown if 7-6 or 6-7) -->
                            @if ((set.p1Games === 7 && set.p2Games === 6) || (set.p1Games === 6 && set.p2Games === 7)) {
                              <div class="flex items-center gap-1.5 border-l border-neutral-200 pl-3">
                                <span class="text-[10px] font-bold text-neutral-500 uppercase">Tiebreak</span>
                                <input
                                  type="number"
                                  min="0"
                                  placeholder="0"
                                  [(ngModel)]="set.p1Tiebreak"
                                  (ngModelChange)="onManualScoreChanged()"
                                  class="w-10 rounded-lg border border-neutral-300 bg-white p-1 text-center text-xs focus:border-primary-500 focus:outline-none"
                                />
                                <span class="text-neutral-400">:</span>
                                <input
                                  type="number"
                                  min="0"
                                  placeholder="0"
                                  [(ngModel)]="set.p2Tiebreak"
                                  (ngModelChange)="onManualScoreChanged()"
                                  class="w-10 rounded-lg border border-neutral-300 bg-white p-1 text-center text-xs focus:border-primary-500 focus:outline-none"
                                />
                              </div>
                            }
                          </div>
                        }
                      </div>
                    }

                    <!-- Calculated Result string preview & notes -->
                    <div class="space-y-3">
                      <div>
                        <span class="text-xs font-semibold text-neutral-500">Preview del resultado:</span>
                        <div class="mt-1 font-bold text-neutral-800 bg-neutral-100 rounded-xl px-4 py-2.5 text-sm">
                          {{ matchResult || 'Ningún set registrado' }}
                        </div>
                      </div>

                      <div>
                        <label class="text-xs font-semibold text-neutral-600">Notas o comentarios del partido</label>
                        <textarea
                          [(ngModel)]="notesInput"
                          (ngModelChange)="onNotesChanged()"
                          placeholder="Notas o comentarios sobre el partido (ej. walkover por lesión, partido suspendido por lluvia, etc.)"
                          class="mt-1 w-full rounded-2xl border border-neutral-300 bg-white px-4 py-2.5 text-sm focus:border-primary-500 focus:outline-none"
                          rows="2"
                        ></textarea>
                      </div>
                    </div>

                    @if (validationMessage()) {
                      <p class="text-xs font-semibold text-red-600 bg-red-50 p-3 rounded-2xl border border-red-100">
                        {{ validationMessage() }}
                      </p>
                    }
                  } @else {
                    <!-- Read-only view for regular players -->
                    <div class="space-y-3">
                      <div class="rounded-2xl border border-neutral-100 bg-neutral-50/50 p-4">
                        <p class="text-xs font-semibold text-neutral-500 uppercase tracking-widest">Resultado</p>
                        <p class="mt-1.5 text-lg font-bold text-neutral-900">{{ match()?.result || 'Pendiente' }}</p>
                        
                        @if (match()?.sets && match()!.sets!.length > 0) {
                          <div class="mt-3 flex gap-2">
                            @for (set of match()!.sets!; track set.setNumber) {
                              <span class="rounded bg-white px-2.5 py-1 text-xs font-semibold border border-neutral-100 text-neutral-700 shadow-sm">
                                Set {{ set.setNumber }}: {{ set.firstPlayerGames }}-{{ set.secondPlayerGames }}
                                @if (set.firstPlayerTiebreak != null || set.secondPlayerTiebreak != null) {
                                  @let minTb = getMinTiebreak(set);
                                  <span class="text-[10px] text-neutral-400 font-normal">({{ minTb }})</span>
                                }
                              </span>
                            }
                          </div>
                        }
                      </div>

                      @if (match()?.notes) {
                        <div class="rounded-2xl border border-neutral-100 bg-neutral-50/50 p-4">
                          <p class="text-xs font-semibold text-neutral-500 uppercase tracking-widest">Notas del partido</p>
                          <p class="mt-1.5 text-sm text-neutral-700 whitespace-pre-line">{{ match()?.notes }}</p>
                        </div>
                      }
                    </div>
                  }
                </div>
              </div>
            }
          </div>

          <!-- Footer -->
          <div class="border-t border-neutral-100 bg-neutral-50/50 px-6 py-4">
            <div class="flex gap-3">
              <button
                (click)="onClose()"
                class="flex-1 rounded-2xl border border-neutral-300 bg-white px-5 py-3 font-semibold text-neutral-700 hover:bg-neutral-50 transition active:scale-[0.99]"
              >
                Cerrar
              </button>
              @if (canManageInput && scoringMode() === 'manual') {
                <button
                  (click)="onSave()"
                  [disabled]="!isFormValid()"
                  class="flex-1 rounded-2xl bg-primary-600 hover:bg-primary-500 px-5 py-3 font-semibold text-white transition active:scale-[0.99] disabled:cursor-not-allowed disabled:bg-neutral-200 disabled:text-neutral-400"
                >
                  Guardar resultado
                </button>
              }
            </div>
          </div>
        </div>
      </div>
    }
  `,
  styles: []
})
export class MatchDetailModalComponent {
  @Input() participantNamesInput: Record<string, string> = {};
  @Input() courtsInput: CourtResponse[] = [];
  @Input() canManageInput = false;
  @Input() setsPerMatch = 3;
  @Input() decisiveTiebreakPoints = 7;

  @Input() set matchInput(value: MatchResponse | null) {
    if (value) {
      this.match.set(value);
      this.selectedWinnerId = value.winnerId || '';
      this.matchResult = value.result || '';
      this.selectedCourtId = value.courtId || '';
      this.scheduledAtInput = this.toDatetimeLocalValue(value.scheduledAt);
      this.selectedScheduleTimeType = value.scheduleTimeType || 'EXACT';
      this.selectedStatus = value.status || 'PENDING';
      this.notesInput = value.notes || '';
      this.validationMessage.set(null);
      this.scheduleValidationMessage.set(null);

      // Populate sets for manual entry and interactive scoring
      const initialSets = value.sets || [];
      const numSets = this.setsPerMatch;
      const populatedManualSets = [];
      for (let i = 0; i < numSets; i++) {
        const existingSet = initialSets.find(s => s.setNumber === i + 1);
        populatedManualSets.push({
          p1Games: existingSet ? existingSet.firstPlayerGames : null,
          p2Games: existingSet ? existingSet.secondPlayerGames : null,
          p1Tiebreak: existingSet ? (existingSet.firstPlayerTiebreak ?? null) : null,
          p2Tiebreak: existingSet ? (existingSet.secondPlayerTiebreak ?? null) : null
        });
      }
      this.manualSets.set(populatedManualSets);

      // Reset interactive engine
      this.resetInteractiveScoreState();
      if (initialSets.length > 0) {
        const populatedInteractiveSets = initialSets.map(s => ({
          p1Games: s.firstPlayerGames,
          p2Games: s.secondPlayerGames,
          p1Tiebreak: s.firstPlayerTiebreak ?? undefined,
          p2Tiebreak: s.secondPlayerTiebreak ?? undefined
        }));
        this.interactiveSets.set(populatedInteractiveSets);
        // Find last in-progress/completed set index
        this.currentSetIndex.set(Math.min(populatedInteractiveSets.length - 1, this.setsPerMatch - 1));

        // Restore current points
        const p1Str = value.firstPlayerPoints || '0';
        const p2Str = value.secondPlayerPoints || '0';
        if (this.isTiebreak()) {
          this.tbPointsP1.set(parseInt(p1Str, 10) || 0);
          this.tbPointsP2.set(parseInt(p2Str, 10) || 0);
        } else {
          const p1Idx = ['0', '15', '30', '40', 'Ad'].indexOf(p1Str);
          this.gamePointsP1.set(p1Idx >= 0 ? p1Idx : 0);
          const p2Idx = ['0', '15', '30', '40', 'Ad'].indexOf(p2Str);
          this.gamePointsP2.set(p2Idx >= 0 ? p2Idx : 0);
        }
      }
    }
  }

  match = signal<MatchResponse | null>(null);
  isOpen = signal(false);
  validationMessage = signal<string | null>(null);
  scheduleValidationMessage = signal<string | null>(null);

  scoringMode = signal<'interactive' | 'manual'>('interactive');
  manualSets = signal<Array<{ p1Games: number | null; p2Games: number | null; p1Tiebreak: number | null; p2Tiebreak: number | null }>>([]);
  
  // Interactive engine state
  interactiveSets = signal<Array<{ p1Games: number; p2Games: number; p1Tiebreak?: number; p2Tiebreak?: number }>>([{ p1Games: 0, p2Games: 0 }]);
  currentSetIndex = signal<number>(0);
  gamePointsP1 = signal<number>(0); // 0=0, 1=15, 2=30, 3=40, 4=Ad
  gamePointsP2 = signal<number>(0);
  tbPointsP1 = signal<number>(0);
  tbPointsP2 = signal<number>(0);

  selectedWinnerId = '';
  matchResult = '';
  notesInput = '';
  selectedCourtId = '';
  scheduledAtInput = '';
  selectedScheduleTimeType: MatchScheduleTimeType = 'EXACT';
  selectedStatus: MatchStatus = 'PENDING';

  pendingGamesDecrementPlayer = signal<1 | 2 | null>(null);

  @Output() close = new EventEmitter<void>();
  @Output() saveResult = new EventEmitter<{
    matchId: string;
    winnerId: string | null;
    result: string;
    sets: SetScoreResponse[];
    notes: string;
    firstPlayerPoints: string | null;
    secondPlayerPoints: string | null;
    status: MatchStatus;
    keepOpen?: boolean;
  }>();
  @Output() saveSchedule = new EventEmitter<{
    matchId: string;
    courtId: string;
    scheduledAt: string;
    scheduleTimeType: MatchScheduleTimeType;
  }>();

  @HostListener('document:keydown.escape', ['$event'])
  onEscapeKeyDown(event: Event): void {
    if (!this.isOpen()) {
      return;
    }

    event.preventDefault();
    this.onClose();
  }

  @HostListener('document:keydown.enter', ['$event'])
  onEnterKeyDown(event: Event): void {
    const keyboardEvent = event as KeyboardEvent;
    if (!this.isOpen() || !this.canManageInput || keyboardEvent.isComposing) {
      return;
    }

    // Do not submit forms on enter if interactive scoring is active (since user might be editing scores)
    if (this.scoringMode() === 'interactive') {
      return;
    }

    event.preventDefault();
    this.onSave();
  }

  open() {
    this.isOpen.set(true);
  }

  onClose() {
    this.isOpen.set(false);
    this.close.emit();
  }

  onSave() {
    if (!this.canManageInput) {
      return;
    }

    if (!this.isFormValid()) {
      this.validationMessage.set('Selecciona un ganador y registra un resultado antes de guardar.');
      return;
    }

    // Prepare sets payload
    let finalSets: SetScoreResponse[] = [];
    if (this.scoringMode() === 'interactive') {
      finalSets = this.interactiveSets()
        .map((s, idx) => ({
          setNumber: idx + 1,
          firstPlayerGames: s.p1Games,
          secondPlayerGames: s.p2Games,
          firstPlayerTiebreak: s.p1Tiebreak ?? null,
          secondPlayerTiebreak: s.p2Tiebreak ?? null
        }));
    } else {
      finalSets = this.manualSets()
        .filter(s => s.p1Games != null && s.p2Games != null)
        .map((s, idx) => ({
          setNumber: idx + 1,
          firstPlayerGames: s.p1Games!,
          secondPlayerGames: s.p2Games!,
          firstPlayerTiebreak: s.p1Tiebreak,
          secondPlayerTiebreak: s.p2Tiebreak
        }));
    }

    this.saveResult.emit({
      matchId: this.match()!.id,
      winnerId: this.selectedWinnerId || null,
      result: this.matchResult,
      sets: finalSets,
      notes: this.notesInput,
      firstPlayerPoints: this.scoringMode() === 'interactive' ? this.getPointsDisplay(1) : null,
      secondPlayerPoints: this.scoringMode() === 'interactive' ? this.getPointsDisplay(2) : null,
      status: this.selectedStatus,
      keepOpen: false
    });
    this.validationMessage.set(null);
    this.isOpen.set(false);
  }

  onSaveSchedule() {
    if (!this.canManageInput) {
      return;
    }

    if (!this.isScheduleValid()) {
      this.scheduleValidationMessage.set('Selecciona tipo, fecha, hora y pista antes de guardar.');
      return;
    }

    this.saveSchedule.emit({
      matchId: this.match()!.id,
      courtId: this.selectedCourtId,
      scheduledAt: this.scheduledAtInput,
      scheduleTimeType: this.selectedScheduleTimeType
    });
    this.scheduleValidationMessage.set(null);
  }

  activeCourts(): CourtResponse[] {
    return this.courtsInput.filter(court => court.active);
  }

  isByeSlot(
    inscriptionId: string | null | undefined,
    opponentInscriptionId: string | null | undefined
  ): boolean {
    const m = this.match();
    return !inscriptionId && !!opponentInscriptionId && (m?.roundNumber ?? 1) === 1;
  }

  getParticipantName(inscriptionId: string | null | undefined): string {
    if (!inscriptionId) {
      return 'Participante';
    }

    return this.participantNamesInput[inscriptionId] ?? inscriptionId.substring(0, 8);
  }

  getWinPointsLabel(points: number | null | undefined): string {
    return points == null ? '+0 pts' : `+${points} pts`;
  }

  getWinPointsClasses(match: MatchResponse | null, inscriptionId: string | null | undefined): string {
    const baseClasses = 'shrink-0 rounded-full px-2 py-0.5 text-xs font-bold';

    if (this.isWinner(match, inscriptionId)) {
      return `${baseClasses} bg-green-100 text-green-700 ring-1 ring-green-200`;
    }

    if (this.isLoser(match, inscriptionId)) {
      return `${baseClasses} bg-neutral-100 text-neutral-400 line-through decoration-2`;
    }

    return `${baseClasses} bg-neutral-100 text-neutral-500`;
  }

  private isWinner(match: MatchResponse | null, inscriptionId: string | null | undefined): boolean {
    return !!match && !!inscriptionId && match.winnerId === inscriptionId;
  }

  private isLoser(match: MatchResponse | null, inscriptionId: string | null | undefined): boolean {
    return !!match && !!inscriptionId && !!match.winnerId && match.winnerId !== inscriptionId;
  }

  isFormValid(): boolean {
    return !!this.match() && (!!this.selectedWinnerId || this.matchResult.trim().length > 0);
  }

  isScheduleValid(): boolean {
    return !!this.match() && !!this.selectedCourtId && this.scheduledAtInput.trim().length > 0;
  }

  getScheduleTypeLabel(value: MatchScheduleTimeType | null | undefined): string {
    return value === 'NOT_BEFORE' ? 'No antes de' : 'A esta hora';
  }

  getMinTiebreak(set: SetScoreResponse): number {
    return Math.min(set.firstPlayerTiebreak ?? 0, set.secondPlayerTiebreak ?? 0);
  }

  private toDatetimeLocalValue(value?: string | null): string {
    if (!value) {
      return '';
    }

    return value.slice(0, 16);
  }

  // TENNIS INTERACTIVE ENGINE
  getInteractiveSetGames(player: 1 | 2, setIdx: number): string {
    const sets = this.interactiveSets();
    if (setIdx >= sets.length) {
      return '-';
    }
    const set = sets[setIdx];
    const games = player === 1 ? set.p1Games : set.p2Games;
    const oppGames = player === 1 ? set.p2Games : set.p1Games;
    const tb = player === 1 ? set.p1Tiebreak : set.p2Tiebreak;
    const oppTb = player === 1 ? set.p2Tiebreak : set.p1Tiebreak;

    if (games === 7 && oppGames === 6 && tb != null && oppTb != null) {
      return `7(${oppTb})`;
    }
    if (games === 6 && oppGames === 7 && tb != null && oppTb != null) {
      return `6(${tb})`;
    }
    return games.toString();
  }

  getPointsDisplay(player: 1 | 2): string {
    if (this.isTiebreak()) {
      return (player === 1 ? this.tbPointsP1() : this.tbPointsP2()).toString();
    }
    const pVal = player === 1 ? this.gamePointsP1() : this.gamePointsP2();
    return ['0', '15', '30', '40', 'Ad'][pVal] || '0';
  }

  isTiebreak(): boolean {
    const sets = this.interactiveSets();
    const idx = this.currentSetIndex();
    if (idx >= sets.length) return false;
    return sets[idx].p1Games === 6 && sets[idx].p2Games === 6;
  }

  isDecisiveSet(): boolean {
    return this.currentSetIndex() === this.setsPerMatch - 1;
  }

  getParticipantLastName(inscriptionId: string | null | undefined): string {
    const fullName = this.getParticipantName(inscriptionId);
    if (!fullName) return 'Jugador';
    const parts = fullName.split(' ');
    return parts[parts.length - 1] || fullName;
  }

  decrementPoint(player: 1 | 2): void {
    const currentMatch = this.match();
    if (!currentMatch) return;

    if (this.selectedStatus === 'PENDING') {
      this.selectedStatus = 'IN_PROGRESS';
    }

    const isTb = this.isTiebreak();
    const isZeroPoints = isTb 
      ? (player === 1 ? this.tbPointsP1() === 0 : this.tbPointsP2() === 0)
      : (player === 1 ? this.gamePointsP1() === 0 : this.gamePointsP2() === 0);

    if (isZeroPoints) {
      if (this.pendingGamesDecrementPlayer() === player) {
        // Decrease games or set
        let sets = [...this.interactiveSets()];
        let idx = this.currentSetIndex();
        if (idx >= 0 && idx < sets.length) {
          const currentSet = { ...sets[idx] };
          const isZeroGames = currentSet.p1Games === 0 && currentSet.p2Games === 0;

          if (isZeroGames && idx > 0) {
            // Remove current set, go back to previous set
            sets.pop();
            this.interactiveSets.set(sets);
            this.currentSetIndex.set(idx - 1);
            this.resetGamePoints();
            this.selectedWinnerId = '';
            this.selectedStatus = 'IN_PROGRESS';
            this.matchResult = this.formatInteractiveResultString(sets);
            this.pendingGamesDecrementPlayer.set(null);
            this.autoSaveInteractiveScore();
            return;
          }

          if (player === 1) {
            currentSet.p1Games = Math.max(0, currentSet.p1Games - 1);
            currentSet.p1Tiebreak = undefined;
            currentSet.p2Tiebreak = undefined;
          } else {
            currentSet.p2Games = Math.max(0, currentSet.p2Games - 1);
            currentSet.p1Tiebreak = undefined;
            currentSet.p2Tiebreak = undefined;
          }
          
          sets[idx] = currentSet;
          this.interactiveSets.set(sets);
          this.resetGamePoints();
          this.selectedWinnerId = '';
          this.selectedStatus = 'IN_PROGRESS';
          this.matchResult = this.formatInteractiveResultString(sets);
        }
        this.pendingGamesDecrementPlayer.set(null);
        this.autoSaveInteractiveScore();
      } else {
        this.pendingGamesDecrementPlayer.set(player);
        setTimeout(() => {
          if (this.pendingGamesDecrementPlayer() === player) {
            this.pendingGamesDecrementPlayer.set(null);
          }
        }, 3000);
      }
      return;
    }

    // Regular decrement of points
    if (isTb) {
      if (player === 1) {
        this.tbPointsP1.update(p => Math.max(0, p - 1));
      } else {
        this.tbPointsP2.update(p => Math.max(0, p - 1));
      }
    } else {
      if (player === 1) {
        const p1 = this.gamePointsP1();
        if (p1 === 4) { // Ad
          this.gamePointsP1.set(3); // 40
        } else {
          this.gamePointsP1.update(p => Math.max(0, p - 1));
        }
      } else {
        const p2 = this.gamePointsP2();
        if (p2 === 4) { // Ad
          this.gamePointsP2.set(3); // 40
        } else {
          this.gamePointsP2.update(p => Math.max(0, p - 1));
        }
      }
    }

    this.pendingGamesDecrementPlayer.set(null);
    this.autoSaveInteractiveScore();
  }

  incrementPoint(player: 1 | 2): void {
    const currentMatch = this.match();
    if (!currentMatch) return;

    // Set status to IN_PROGRESS when score starts updating
    if (this.selectedStatus === 'PENDING') {
      this.selectedStatus = 'IN_PROGRESS';
    }

    let setIndex = this.currentSetIndex();
    let sets = [...this.interactiveSets()];

    if (sets.length === 0) {
      sets = [{ p1Games: 0, p2Games: 0 }];
      setIndex = 0;
    }

    const currentSet = { ...sets[setIndex] };
    const isTb = currentSet.p1Games === 6 && currentSet.p2Games === 6;

    if (isTb) {
      if (player === 1) {
        this.tbPointsP1.update(p => p + 1);
      } else {
        this.tbPointsP2.update(p => p + 1);
      }

      const p1 = this.tbPointsP1();
      const p2 = this.tbPointsP2();
      const targetPoints = this.isDecisiveSet() ? this.decisiveTiebreakPoints : 7;

      if (p1 >= targetPoints && p1 - p2 >= 2) {
        currentSet.p1Games = 7;
        currentSet.p2Games = 6;
        currentSet.p1Tiebreak = p1;
        currentSet.p2Tiebreak = p2;
        sets[setIndex] = currentSet;
        this.interactiveSets.set(sets);
        this.resetGamePoints();
        this.checkSetWin(sets, setIndex, 1);
      } else if (p2 >= targetPoints && p2 - p1 >= 2) {
        currentSet.p1Games = 6;
        currentSet.p2Games = 7;
        currentSet.p1Tiebreak = p1;
        currentSet.p2Tiebreak = p2;
        sets[setIndex] = currentSet;
        this.interactiveSets.set(sets);
        this.resetGamePoints();
        this.checkSetWin(sets, setIndex, 2);
      }
    } else {
      let p1 = this.gamePointsP1();
      let p2 = this.gamePointsP2();

      if (player === 1) {
        if (p1 === 3) {
          if (p2 === 4) {
            p2 = 3;
          } else if (p2 === 3) {
            p1 = 4;
          } else {
            this.winGame(sets, setIndex, 1);
            return;
          }
        } else if (p1 === 4) {
          this.winGame(sets, setIndex, 1);
          return;
        } else {
          p1++;
        }
      } else {
        if (p2 === 3) {
          if (p1 === 4) {
            p1 = 3;
          } else if (p1 === 3) {
            p2 = 4;
          } else {
            this.winGame(sets, setIndex, 2);
            return;
          }
        } else if (p2 === 4) {
          this.winGame(sets, setIndex, 2);
          return;
        } else {
          p2++;
        }
      }

      this.gamePointsP1.set(p1);
      this.gamePointsP2.set(p2);
    }

    // Auto-save changes immediately point-by-point
    this.autoSaveInteractiveScore();
  }

  resetGamePoints(): void {
    this.gamePointsP1.set(0);
    this.gamePointsP2.set(0);
    this.tbPointsP1.set(0);
    this.tbPointsP2.set(0);
  }

  winGame(sets: any[], setIndex: number, player: 1 | 2): void {
    const currentSet = { ...sets[setIndex] };
    if (player === 1) {
      currentSet.p1Games++;
    } else {
      currentSet.p2Games++;
    }
    sets[setIndex] = currentSet;
    this.interactiveSets.set(sets);
    this.resetGamePoints();

    const g1 = currentSet.p1Games;
    const g2 = currentSet.p2Games;

    if (g1 === 6 && g2 === 6) {
      this.autoSaveInteractiveScore();
      return;
    }

    if (g1 >= 6 && g1 - g2 >= 2) {
      this.checkSetWin(sets, setIndex, 1);
    } else if (g2 >= 6 && g2 - g1 >= 2) {
      this.checkSetWin(sets, setIndex, 2);
    }

    this.autoSaveInteractiveScore();
  }

  checkSetWin(sets: any[], setIndex: number, setWinner: 1 | 2): void {
    let p1SetsWon = 0;
    let p2SetsWon = 0;

    sets.forEach(s => {
      if (s.p1Games > s.p2Games) p1SetsWon++;
      if (s.p2Games > s.p1Games) p2SetsWon++;
    });

    const setsToWin = Math.ceil(this.setsPerMatch / 2);

    if (p1SetsWon >= setsToWin) {
      this.selectedWinnerId = this.match()!.firstInscriptionId || '';
      this.selectedStatus = 'COMPLETED';
    } else if (p2SetsWon >= setsToWin) {
      this.selectedWinnerId = this.match()!.secondInscriptionId || '';
      this.selectedStatus = 'COMPLETED';
    } else {
      if (setIndex === sets.length - 1 && sets.length < this.setsPerMatch) {
        sets.push({ p1Games: 0, p2Games: 0 });
        this.interactiveSets.set(sets);
        this.currentSetIndex.set(setIndex + 1);
      }
    }
    this.matchResult = this.formatInteractiveResultString(sets);
  }

  formatInteractiveResultString(setsArray: any[]): string {
    return setsArray
      .filter(s => s.p1Games != null && s.p2Games != null)
      .map(s => {
        let str = `${s.p1Games}-${s.p2Games}`;
        if (s.p1Games === 7 && s.p2Games === 6 && s.p2Tiebreak != null) {
          str += `(${s.p2Tiebreak})`;
        } else if (s.p1Games === 6 && s.p2Games === 7 && s.p1Tiebreak != null) {
          str += `(${s.p1Tiebreak})`;
        }
        return str;
      })
      .join(' ');
  }

  resetInteractiveScoreState(): void {
    this.resetGamePoints();
    this.interactiveSets.set([{ p1Games: 0, p2Games: 0 }]);
    this.currentSetIndex.set(0);
  }

  // AUTO-SAVE LOGIC POINT-BY-POINT
  autoSaveInteractiveScore(): void {
    if (!this.match()) return;
    const finalSets = this.interactiveSets()
      .map((s, idx) => ({
        setNumber: idx + 1,
        firstPlayerGames: s.p1Games,
        secondPlayerGames: s.p2Games,
        firstPlayerTiebreak: s.p1Tiebreak ?? null,
        secondPlayerTiebreak: s.p2Tiebreak ?? null
      }));

    this.saveResult.emit({
      matchId: this.match()!.id,
      winnerId: this.selectedWinnerId || null,
      result: this.matchResult,
      sets: finalSets,
      notes: this.notesInput,
      firstPlayerPoints: this.getPointsDisplay(1),
      secondPlayerPoints: this.getPointsDisplay(2),
      status: this.selectedStatus,
      keepOpen: true
    });
  }

  onStatusChanged(): void {
    if (this.scoringMode() === 'interactive') {
      this.autoSaveInteractiveScore();
    } else {
      this.autoSaveManualScore();
    }
  }

  onWinnerChanged(): void {
    if (this.scoringMode() === 'interactive') {
      this.autoSaveInteractiveScore();
    } else {
      this.autoSaveManualScore();
    }
  }

  onNotesChanged(): void {
    if (this.scoringMode() === 'interactive') {
      this.autoSaveInteractiveScore();
    } else {
      this.autoSaveManualScore();
    }
  }

  // MANUAL SCORING FLOW
  onManualScoreChanged(): void {
    const sets = this.manualSets();
    let p1SetsWon = 0;
    let p2SetsWon = 0;

    sets.forEach(s => {
      if (s.p1Games != null && s.p2Games != null) {
        if (s.p1Games > s.p2Games) p1SetsWon++;
        if (s.p2Games > s.p1Games) p2SetsWon++;
      }
    });

    const setsToWin = Math.ceil(this.setsPerMatch / 2);

    if (p1SetsWon >= setsToWin) {
      this.selectedWinnerId = this.match()?.firstInscriptionId || '';
      this.selectedStatus = 'COMPLETED';
    } else if (p2SetsWon >= setsToWin) {
      this.selectedWinnerId = this.match()?.secondInscriptionId || '';
      this.selectedStatus = 'COMPLETED';
    } else {
      this.selectedWinnerId = '';
      this.selectedStatus = 'PENDING';
    }

    this.matchResult = this.formatManualResultString();
    this.autoSaveManualScore();
  }

  autoSaveManualScore(): void {
    if (!this.match()) return;
    const finalSets = this.manualSets()
      .filter(s => s.p1Games != null && s.p2Games != null)
      .map((s, idx) => ({
        setNumber: idx + 1,
        firstPlayerGames: s.p1Games!,
        secondPlayerGames: s.p2Games!,
        firstPlayerTiebreak: s.p1Tiebreak,
        secondPlayerTiebreak: s.p2Tiebreak
      }));

    this.saveResult.emit({
      matchId: this.match()!.id,
      winnerId: this.selectedWinnerId || null,
      result: this.matchResult,
      sets: finalSets,
      notes: this.notesInput,
      firstPlayerPoints: null,
      secondPlayerPoints: null,
      status: this.selectedStatus,
      keepOpen: true
    });
  }

  formatManualResultString(): string {
    return this.manualSets()
      .filter(s => s.p1Games != null && s.p2Games != null)
      .map(s => {
        let str = `${s.p1Games}-${s.p2Games}`;
        if (s.p1Games === 7 && s.p2Games === 6 && s.p2Tiebreak != null) {
          str += `(${s.p2Tiebreak})`;
        } else if (s.p1Games === 6 && s.p2Games === 7 && s.p1Tiebreak != null) {
          str += `(${s.p1Tiebreak})`;
        }
        return str;
      })
      .join(' ');
  }
}
