import { Component, Input, Output, EventEmitter, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StageResponse, DrawResponse } from '../../../data/interfaces/tournament.model';
import { DrawsComponent } from './draws.component';

@Component({
  selector: 'app-stages',
  standalone: true,
  imports: [CommonModule, DrawsComponent],
  template: `
    <div class="mt-8 space-y-6">
      <h2 class="text-2xl font-bold text-neutral-900">Fases del Evento</h2>
      
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
                  <h3 class="text-lg font-semibold text-neutral-900">{{ stage.stageType }}</h3>
                  <p class="mt-1 text-sm text-neutral-600">{{ stage.description }}</p>
                  <p class="mt-2 text-xs text-neutral-500">Fase {{ stage.order }}</p>
                </div>
                <button
                  (click)="onGenerateDraws(stage)"
                  [disabled]="generatingDrawsForStageId() === stage.id"
                  class="rounded-lg bg-primary-600 px-4 py-2 text-white hover:bg-primary-700 disabled:bg-neutral-400"
                >
                  {{ generatingDrawsForStageId() === stage.id ? 'Generando...' : 'Generar Cuadros' }}
                </button>
              </div>

              @if (expandedStageId() === stage.id && (stage.draws || []).length > 0) {
                <div class="mt-4 border-t border-neutral-200 pt-4">
                  <app-draws [drawsInput]="stage.draws || []" (matchSelected)="onMatchSelected($event)"></app-draws>
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
  `
})
export class StagesComponent {
  @Input() set stagesInput(value: StageResponse[]) {
    this._stages.set(value);
  }
  private _stages = signal<StageResponse[]>([]);
  stages = computed(() => this._stages());

  @Input() set tournamentIdInput(value: string) {
    this._tournamentId.set(value);
  }
  private _tournamentId = signal<string>('');
  tournamentId = computed(() => this._tournamentId());

  @Output() generateDraws = new EventEmitter<{ tournamentId: string; stageId: string }>();
  @Output() matchSelected = new EventEmitter<string>();

  expandedStageId = signal<string | null>(null);
  generatingDrawsForStageId = signal<string | null>(null);

  toggleStage(stageId: string) {
    this.expandedStageId.set(this.expandedStageId() === stageId ? null : stageId);
  }

  onGenerateDraws(stage: StageResponse) {
    this.generatingDrawsForStageId.set(stage.id);
    this.generateDraws.emit({
      tournamentId: this.tournamentId(),
      stageId: stage.id
    });
  }

  onMatchSelected(matchId: string) {
    this.matchSelected.emit(matchId);
  }
}
