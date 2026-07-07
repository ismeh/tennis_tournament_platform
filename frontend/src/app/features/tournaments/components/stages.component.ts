import { Component, Input, Output, EventEmitter, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CourtResponse, StageResponse, DrawResponse, MatchResponse, MatchScheduleTimeType, MatchStatus, SetScoreResponse, TournamentStatus } from '../../../data/interfaces/tournament.model';
import { DrawsComponent } from './draws.component';

type DrawGenerationFeedback = {
  status: 'success' | 'error';
  message: string;
};

@Component({
  selector: 'app-stages',
  standalone: true,
  imports: [CommonModule, DrawsComponent],
  template: `
    <div class="space-y-4">
      @if (stages().length === 0) {
        <div class="rounded-lg border border-neutral-200 bg-neutral-50 p-6 text-center text-neutral-600">
          No hay fases generadas aún
        </div>
      } @else {
        <div class="space-y-4">
          @for (stage of stages(); track stage.id) {
            <div class="rounded-lg border border-neutral-200 bg-white p-6 shadow-sm">
              <div class="flex items-center justify-between">
                <div>
                  <h3 class="text-lg font-semibold text-neutral-900">{{ getStageTypeLabel(stage.stageType) }}</h3>
                  <p class="mt-1 text-sm text-neutral-600">{{ stage.description }}</p>
                  <p class="mt-2 text-xs text-neutral-500">Fase {{ stage.order }}</p>
                </div>
                @if (canManageInput) {
                  <button
                    type="button"
                    (click)="onGenerateDraws(stage)"
                    [disabled]="isGeneratingDraws(stage.id)"
                    [attr.aria-busy]="isGeneratingDraws(stage.id)"
                    [class]="getGenerateDrawsButtonClass(stage)"
                  >
                    {{ getGenerateDrawsButtonLabel(stage) }}
                  </button>
                }
              </div>

              @if (isGeneratingDraws(stage.id)) {
                <div class="mt-4 rounded-lg border border-primary-200 bg-primary-50 px-4 py-3 text-sm font-medium text-primary-800">
                  Generando cuadros para esta fase...
                </div>
              } @else if (drawGenerationFeedback()[stage.id]) {
                <div
                  class="mt-4 rounded-lg border px-4 py-3 text-sm font-medium"
                  [ngClass]="drawGenerationFeedback()[stage.id].status === 'success'
                    ? 'border-green-200 bg-green-50 text-green-800'
                    : 'border-red-200 bg-red-50 text-red-700'"
                >
                  {{ drawGenerationFeedback()[stage.id].message }}
                </div>
              }

              @if (expandedStageId() === stage.id && (stage.draws || []).length > 0) {
                <div class="mt-4 border-t border-neutral-200 pt-4">
                  <app-draws
                    [drawsInput]="stage.draws || []"
                    [participantNamesInput]="participantNamesInput"
                    [participantOrderInput]="participantOrderInput"
                    [courtsInput]="courtsInput"
                    [canManageInput]="canManageInput"
                    [tournamentStatusInput]="tournamentStatusInput"
                    [tournamentNameInput]="tournamentNameInput"
                    [categoryNameInput]="categoryNameInput"
                    [setsPerMatch]="setsPerMatch"
                    [decisiveTiebreakPoints]="decisiveTiebreakPoints"
                    [gamesPerSet]="gamesPerSet"
                    (matchSelected)="onMatchSelected($event)"
                    (matchResultSaved)="onMatchResultSaved($event)"
                    (matchScheduleSaved)="onMatchScheduleSaved($event)"
                    (playersSwapped)="onPlayersSwapped($event)"
                    (swapScheduleClicked)="swapScheduleClicked.emit($event)"
                  ></app-draws>
                </div>
              }

              @if (expandedStageId() === stage.id && (stage.draws || []).length === 0) {
                <div class="mt-4 border-t border-neutral-200 pt-4 text-sm text-neutral-600">
                  Sin cuadros generados
                </div>
              }

              <button
                (click)="toggleStage(stage.id)"
                class="mt-3 text-sm text-primary-600 hover:text-primary-700"
              >
                {{ expandedStageId() === stage.id ? 'Ocultar' : 'Ver' }} cuadros
              </button>
            </div>
          }
        </div>
      }
    </div>

    @if (confirmModalOpen()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" (click)="cancelRegenerate()">
        <div class="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl" (click)="$event.stopPropagation()">
          <div class="flex items-start gap-4">
            <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-amber-100">
              <svg class="h-5 w-5 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
            </div>
            <div class="flex-1">
              <h3 class="text-lg font-semibold text-neutral-900">Regenerar cuadros</h3>
              <p class="mt-2 text-sm text-neutral-600">
                Se borrarán los resultados de este cuadro y se generarán nuevos emparejamientos.
              </p>
              <p class="mt-1 text-sm font-medium text-red-600">¿Estás seguro de que quieres continuar?</p>
            </div>
          </div>
          <div class="mt-6 flex justify-end gap-3">
            <button
              type="button"
              (click)="cancelRegenerate()"
              class="rounded-xl border border-neutral-300 px-4 py-2 text-sm font-semibold text-neutral-700 transition-colors hover:bg-neutral-50"
            >
              Cancelar
            </button>
            <button
              type="button"
              (click)="confirmRegenerate()"
              class="rounded-xl bg-red-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-red-700"
            >
              Regenerar
            </button>
          </div>
        </div>
      </div>
    }
  `
})
export class StagesComponent {
  @Input() participantNamesInput: Record<string, string> = {};
  @Input() participantOrderInput: Record<string, number> = {};
  @Input() courtsInput: CourtResponse[] = [];
  @Input() canManageInput = false;
  @Input() tournamentStatusInput: TournamentStatus = 'DRAFT';
  @Input() tournamentNameInput = '';
  @Input() categoryNameInput = '';
  @Input() setsPerMatch = 3;
  @Input() decisiveTiebreakPoints = 7;
  @Input() gamesPerSet = 6;

  @Input() set stagesInput(value: StageResponse[]) {
    this._stages.set(value);
    this.tryRestoreExpandedStage();
  }
  private _stages = signal<StageResponse[]>([]);
  stages = computed(() => this._stages());

  @Input() set tournamentIdInput(value: string) {
    this._tournamentId.set(value);
    this.tryRestoreExpandedStage();
  }
  private _tournamentId = signal<string>('');
  tournamentId = computed(() => this._tournamentId());

  private tryRestoreExpandedStage() {
    const value = this._stages();
    if (!this.expandedStageId() && value.length > 0) {
      if (typeof window !== 'undefined' && window.sessionStorage) {
        const expandedStage = value.find(stage => sessionStorage.getItem(`tournament_stage_expanded_${stage.id}`) === 'true');
        if (expandedStage) {
          this.expandedStageId.set(expandedStage.id);
        }
      }
    }
  }

  @Output() generateDraws = new EventEmitter<{ tournamentId: string; stageId: string }>();
  @Output() matchSelected = new EventEmitter<string>();
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
  @Output() swapScheduleClicked = new EventEmitter<MatchResponse>();

  expandedStageId = signal<string | null>(null);

  confirmModalOpen = signal(false);
  confirmModalStage = signal<StageResponse | null>(null);

  @Input() set generatingDrawsForStageIdInput(value: string | null) {
    this._generatingDrawsForStageId.set(value);
  }
  private _generatingDrawsForStageId = signal<string | null>(null);
  generatingDrawsForStageId = computed(() => this._generatingDrawsForStageId());

  @Input() set drawGenerationFeedbackInput(value: Record<string, DrawGenerationFeedback>) {
    this._drawGenerationFeedback.set(value);
  }
  private _drawGenerationFeedback = signal<Record<string, DrawGenerationFeedback>>({});
  drawGenerationFeedback = computed(() => this._drawGenerationFeedback());

  toggleStage(stageId: string) {
    const prevExpandedId = this.expandedStageId();
    const nextVal = prevExpandedId === stageId ? null : stageId;
    this.expandedStageId.set(nextVal);

    if (typeof window !== 'undefined' && window.sessionStorage) {
      if (prevExpandedId) {
        sessionStorage.removeItem(`tournament_stage_expanded_${prevExpandedId}`);
      }
      if (nextVal) {
        sessionStorage.setItem(`tournament_stage_expanded_${nextVal}`, 'true');
      }
    }
  }

  onGenerateDraws(stage: StageResponse) {
    if (!this.canManageInput) {
      return;
    }

    const hasExistingDraws = (stage.draws || []).length > 0;
    if (hasExistingDraws) {
      this.confirmModalStage.set(stage);
      this.confirmModalOpen.set(true);
      return;
    }

    this.doGenerateDraws(stage);
  }

  cancelRegenerate() {
    this.confirmModalOpen.set(false);
    this.confirmModalStage.set(null);
  }

  confirmRegenerate() {
    const stage = this.confirmModalStage();
    this.confirmModalOpen.set(false);
    this.confirmModalStage.set(null);
    if (stage) {
      this.doGenerateDraws(stage);
    }
  }

  private doGenerateDraws(stage: StageResponse) {
    const prevExpandedId = this.expandedStageId();
    this.expandedStageId.set(stage.id);

    if (typeof window !== 'undefined' && window.sessionStorage) {
      if (prevExpandedId) {
        sessionStorage.removeItem(`tournament_stage_expanded_${prevExpandedId}`);
      }
      sessionStorage.setItem(`tournament_stage_expanded_${stage.id}`, 'true');
    }

    this.generateDraws.emit({
      tournamentId: this.tournamentId(),
      stageId: stage.id
    });
  }

  onMatchSelected(matchId: string) {
    this.matchSelected.emit(matchId);
  }

  onMatchResultSaved(event: { matchId: string; winnerId: string | null; result: string; sets?: SetScoreResponse[] | null; notes?: string | null; status: MatchStatus; keepOpen?: boolean }) {
    if (!this.canManageInput) {
      return;
    }

    this.matchResultSaved.emit(event);
  }

  onMatchScheduleSaved(event: {
    matchId: string;
    courtId: string;
    scheduledAt: string;
    scheduleTimeType: MatchScheduleTimeType;
  }) {
    if (!this.canManageInput) {
      return;
    }

    this.matchScheduleSaved.emit(event);
  }

  onPlayersSwapped(event: {
    matchId1: string;
    slot1: 'first' | 'second';
    matchId2: string;
    slot2: 'first' | 'second';
  }) {
    if (!this.canManageInput) {
      return;
    }

    this.playersSwapped.emit(event);
  }

  isGeneratingDraws(stageId: string): boolean {
    return this.generatingDrawsForStageId() === stageId;
  }

  getGenerateDrawsButtonLabel(stage: StageResponse): string {
    if (this.isGeneratingDraws(stage.id)) {
      return 'Generando cuadros...';
    }

    const feedback = this.drawGenerationFeedback()[stage.id];
    if (feedback?.status === 'error') {
      return 'Reintentar generar';
    }

    if ((stage.draws || []).length > 0 || feedback?.status === 'success') {
      return 'Regenerar cuadros';
    }

    return 'Generar cuadros';
  }

  getGenerateDrawsButtonClass(stage: StageResponse): string {
    const baseClass = 'rounded-lg px-4 py-2 text-white disabled:cursor-wait disabled:bg-neutral-400';
    const feedback = this.drawGenerationFeedback()[stage.id];

    if (feedback?.status === 'error') {
      return `${baseClass} bg-red-600 hover:bg-red-700`;
    }

    if ((stage.draws || []).length > 0 || feedback?.status === 'success') {
      return `${baseClass} bg-green-700 hover:bg-green-800`;
    }

    return `${baseClass} bg-primary-600 hover:bg-primary-700`;
  }

  getStageTypeLabel(stageType: string): string {
    const labels: Record<string, string> = {
      MAIN: 'Cuadro Principal',
      SINGLE_ELIMINATION: 'Eliminatoria simple',
      ROUND_ROBIN: 'Liga',
      DOUBLE_ELIMINATION: 'Doble eliminación',
      CONSOLATION: 'Consolación'
    };

    return labels[stageType] ?? stageType;
  }
}
