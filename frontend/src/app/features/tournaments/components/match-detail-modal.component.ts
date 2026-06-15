import { Component, EventEmitter, HostListener, Input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CourtResponse, MatchResponse, MatchScheduleTimeType } from '../../../data/interfaces/tournament.model';

@Component({
  selector: 'app-match-detail-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (isOpen()) {
      <div class="fixed inset-0 z-[2147483647] flex items-center justify-center bg-black bg-opacity-50 p-4">
        <div class="w-full max-w-2xl rounded-lg bg-white shadow-lg">
          <!-- Header -->
          <div class="border-b border-neutral-200 bg-neutral-50 px-6 py-4">
            <div class="flex items-center justify-between">
              <h3 class="text-lg font-semibold text-neutral-900">Detalles del partido</h3>
              <button
                (click)="onClose()"
                class="text-neutral-500 hover:text-neutral-700"
              >
                ✕
              </button>
            </div>
          </div>

          <!-- Content -->
          <div class="space-y-4 px-6 py-4">
            @if (match()) {
              <div class="space-y-3">
                <!-- Round Info -->
                <div>
                  <p class="text-xs font-semibold text-neutral-600">RONDA</p>
                  <p class="text-sm text-neutral-900">{{ match()?.roundNumber }}</p>
                </div>

                <!-- Participants -->
                <div class="space-y-2">
                  <p class="text-xs font-semibold text-neutral-600">JUGADORES</p>
                  <div class="space-y-2">
                    @if (match()?.firstInscriptionId) {
                      <div class="rounded bg-neutral-50 px-3 py-2">
                        <div class="flex items-center justify-between gap-3">
                          <p class="truncate text-sm font-medium text-neutral-900">{{ getParticipantName(match()?.firstInscriptionId) }}</p>
                          @if (match()?.professionalMatch) {
                            <span [class]="getWinPointsClasses(match(), match()?.firstInscriptionId)">
                              {{ getWinPointsLabel(match()?.firstWinPoints) }}
                            </span>
                          }
                        </div>
                      </div>
                    } @else {
                      <div class="rounded bg-neutral-50 px-3 py-2">
                        <p class="text-sm italic text-neutral-500">Por determinar</p>
                      </div>
                    }
                    <div class="text-center text-xs text-neutral-500">vs</div>
                    @if (match()?.secondInscriptionId) {
                      <div class="rounded bg-neutral-50 px-3 py-2">
                        <div class="flex items-center justify-between gap-3">
                          <p class="truncate text-sm font-medium text-neutral-900">{{ getParticipantName(match()?.secondInscriptionId) }}</p>
                          @if (match()?.professionalMatch) {
                            <span [class]="getWinPointsClasses(match(), match()?.secondInscriptionId)">
                              {{ getWinPointsLabel(match()?.secondWinPoints) }}
                            </span>
                          }
                        </div>
                      </div>
                    } @else {
                      <div class="rounded bg-neutral-50 px-3 py-2">
                        <p class="text-sm italic text-neutral-500">Por determinar</p>
                      </div>
                    }
                  </div>
                </div>

                <!-- Schedule Info -->
                <div class="border-t border-neutral-200 pt-4">
                  <p class="mb-3 text-xs font-semibold text-neutral-600">PROGRAMACIÓN</p>
                  @if (canManageInput) {
                    <div class="grid gap-3 sm:grid-cols-3">
                      <label class="block">
                        <span class="text-xs font-semibold text-neutral-600">Tipo</span>
                        <select
                          [(ngModel)]="selectedScheduleTimeType"
                          class="mt-1 w-full rounded border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none"
                        >
                          <option value="EXACT">A esta hora</option>
                          <option value="NOT_BEFORE">No antes de</option>
                        </select>
                      </label>

                      <label class="block">
                        <span class="text-xs font-semibold text-neutral-600">Inicio</span>
                        <input
                          type="datetime-local"
                          [(ngModel)]="scheduledAtInput"
                          class="mt-1 w-full rounded border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none"
                        />
                      </label>

                      <label class="block">
                        <span class="text-xs font-semibold text-neutral-600">Pista</span>
                        <select
                          [(ngModel)]="selectedCourtId"
                          class="mt-1 w-full rounded border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none"
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

                    <button
                      type="button"
                      (click)="onSaveSchedule()"
                      [disabled]="!isScheduleValid()"
                      class="mt-3 rounded bg-neutral-900 px-4 py-2 text-sm font-medium text-white hover:bg-neutral-800 disabled:cursor-not-allowed disabled:bg-neutral-300"
                    >
                      Guardar programación
                    </button>
                  } @else {
                    <div class="grid gap-3 sm:grid-cols-3">
                      <div class="rounded bg-neutral-50 px-3 py-2">
                        <p class="text-xs font-semibold text-neutral-600">Tipo</p>
                        <p class="mt-1 text-sm text-neutral-900">{{ getScheduleTypeLabel(match()?.scheduleTimeType) }}</p>
                      </div>
                      <div class="rounded bg-neutral-50 px-3 py-2">
                        <p class="text-xs font-semibold text-neutral-600">Inicio</p>
                        <p class="mt-1 text-sm text-neutral-900">{{ match()?.scheduledAt || 'Por definir' }}</p>
                      </div>
                      <div class="rounded bg-neutral-50 px-3 py-2">
                        <p class="text-xs font-semibold text-neutral-600">Pista</p>
                        <p class="mt-1 text-sm text-neutral-900">{{ match()?.court || 'Por definir' }}</p>
                      </div>
                    </div>
                  }
                </div>

                <!-- Result Entry Section -->
                <div class="border-t border-neutral-200 pt-4">
                  <p class="mb-3 text-xs font-semibold text-neutral-600">REGISTRO DE RESULTADO</p>
                  @if (canManageInput) {
                    <div class="space-y-3">
                    <!-- Winner Selection -->
                    <div>
                      <label class="text-xs font-semibold text-neutral-600">Ganador</label>
                      <select
                        [(ngModel)]="selectedWinnerId"
                        class="mt-1 w-full rounded border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none"
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

                    <!-- Match Result -->
                    <div>
                      <label class="text-xs font-semibold text-neutral-600">Resultado (ej: 6-4, 6-3)</label>
                      <input
                        type="text"
                        [(ngModel)]="matchResult"
                        placeholder="Ej: 6-4 7-5"
                        class="mt-1 w-full rounded border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none"
                      />
                    </div>

                    @if (validationMessage()) {
                      <p class="text-xs font-medium text-red-600">{{ validationMessage() }}</p>
                    }
                    </div>
                  } @else {
                    <div class="rounded bg-neutral-50 px-3 py-2">
                      <p class="text-xs font-semibold text-neutral-600">Resultado</p>
                      <p class="mt-1 text-sm text-neutral-900">{{ match()?.result || 'Pendiente' }}</p>
                    </div>
                  }
                </div>
              </div>
            }
          </div>

          <!-- Footer -->
          <div class="border-t border-neutral-200 bg-neutral-50 px-6 py-4">
            <div class="flex gap-2">
              <button
                (click)="onClose()"
                class="flex-1 rounded border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50"
              >
                Cancelar
              </button>
              @if (canManageInput) {
                <button
                  (click)="onSave()"
                  [disabled]="!isFormValid()"
                  class="flex-1 rounded bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 disabled:cursor-not-allowed disabled:bg-neutral-300"
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

  @Input() set matchInput(value: MatchResponse | null) {
    if (value) {
      this.match.set(value);
      this.selectedWinnerId = value.winnerId || '';
      this.matchResult = value.result || '';
      this.selectedCourtId = value.courtId || '';
      this.scheduledAtInput = this.toDatetimeLocalValue(value.scheduledAt);
      this.selectedScheduleTimeType = value.scheduleTimeType || 'EXACT';
      this.validationMessage.set(null);
      this.scheduleValidationMessage.set(null);
    }
  }
  match = signal<MatchResponse | null>(null);
  isOpen = signal(false);
  validationMessage = signal<string | null>(null);
  scheduleValidationMessage = signal<string | null>(null);

  selectedWinnerId = '';
  matchResult = '';
  selectedCourtId = '';
  scheduledAtInput = '';
  selectedScheduleTimeType: MatchScheduleTimeType = 'EXACT';

  @Output() close = new EventEmitter<void>();
  @Output() saveResult = new EventEmitter<{ matchId: string; winnerId: string | null; result: string }>();
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
      this.validationMessage.set('Selecciona un ganador o registra un resultado antes de guardar.');
      return;
    }

    this.saveResult.emit({
      matchId: this.match()!.id,
      winnerId: this.selectedWinnerId || null,
      result: this.matchResult
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

  private toDatetimeLocalValue(value?: string | null): string {
    if (!value) {
      return '';
    }

    return value.slice(0, 16);
  }
}
