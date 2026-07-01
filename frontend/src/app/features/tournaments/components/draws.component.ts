import { Component, Input, Output, EventEmitter, signal, computed, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CourtResponse, DrawResponse, MatchResponse, MatchScheduleTimeType, MatchStatus, SetScoreResponse } from '../../../data/interfaces/tournament.model';
import { MatchesComponent } from './matches.component';
import { BracketComponent } from './bracket.component';
import { MatchDetailModalComponent } from './match-detail-modal.component';

type DrawViewMode = 'tree' | 'list';

interface DrawDisplayItem {
  key: string;
  label: string;
  isDoubleElimination: boolean;
  draws: DrawResponse[];
}

@Component({
  selector: 'app-draws',
  standalone: true,
  imports: [CommonModule, MatchesComponent, BracketComponent, MatchDetailModalComponent],
  template: `
    <div class="space-y-4">
      <h4 class="font-semibold text-neutral-900">Cuadros</h4>

      @if (draws().length === 0) {
        <p class="text-sm text-neutral-600">Sin cuadros</p>
      } @else {
        <div class="space-y-3">
          @for (item of displayItems(); track item.key) {
            <div class="rounded-md border border-neutral-200 bg-white p-4">
              <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <p class="font-medium text-neutral-900">{{ item.label }}</p>
                  @if (item.isDoubleElimination) {
                    <p class="text-xs text-neutral-500">Formato: Doble eliminacion</p>
                    <p class="text-xs text-neutral-500">{{ getTotalMatchCount(item.draws) }} partidos</p>
                  } @else {
                    @let draw = item.draws[0];
                    <p class="text-xs text-neutral-500">Formato: {{ getDrawTypeLabel(draw.drawType) }}</p>
                    @if (draw.groupIndex != null) {
                      <p class="text-xs text-neutral-500">Grupo {{ getGroupName(draw.groupIndex) }}</p>
                    }
                    <p class="text-xs text-neutral-500">{{ (draw.matches || []).length }} partidos</p>
                  }
                </div>
                <button
                  type="button"
                  (click)="toggleDrawView(item.key)"
                  class="inline-flex items-center justify-center rounded-lg border border-primary-200 px-3 py-2 text-sm font-medium text-primary-700 transition-colors hover:bg-primary-50"
                  [attr.aria-label]="getDrawViewToggleLabel(item.key)"
                >
                  {{ getDrawViewToggleLabel(item.key) }}
                </button>
              </div>

              @if (getTotalMatchCount(item.draws) > 0) {
                <div class="mt-3 border-t border-neutral-200 pt-3">
                  @if (getDrawViewMode(item.key) === 'tree') {
                    <app-bracket
                      [drawsInput]="item.draws"
                      [participantNamesInput]="participantNamesInput"
                      [participantOrderInput]="participantOrderInput"
                      [courtsInput]="courtsInput"
                      [canManageInput]="canManageInput"
                      [showTitleInput]="false"
                      [showDrawCardInput]="false"
                      [tournamentNameInput]="tournamentNameInput"
                      [categoryNameInput]="categoryNameInput"
                      [setsPerMatch]="setsPerMatch"
                      [decisiveTiebreakPoints]="decisiveTiebreakPoints"
                      (matchSelected)="matchSelected.emit($event.id)"
                      (matchResultSaved)="onSaveMatchResult($event)"
                      (matchScheduleSaved)="onSaveMatchSchedule($event)"
                    ></app-bracket>
                  } @else {
                    <app-matches
                      [matchesInput]="getAllMatches(item.draws)"
                      [participantNamesInput]="participantNamesInput"
                      [participantOrderInput]="participantOrderInput"
                      (matchSelected)="onMatchSelected($event)"
                    ></app-matches>
                  }
                </div>
              }

              @if (getTotalMatchCount(item.draws) === 0) {
                <div class="mt-3 border-t border-neutral-200 pt-3 text-sm text-neutral-600">
                  Sin partidos
                </div>
              }
            </div>
          }
        </div>
      }
    </div>

    <app-match-detail-modal
      #matchModal
      [matchInput]="selectedMatch()"
      [participantNamesInput]="participantNamesInput"
      [courtsInput]="courtsInput"
      [canManageInput]="canManageInput"
      [setsPerMatch]="setsPerMatch"
      [decisiveTiebreakPoints]="decisiveTiebreakPoints"
      (saveResult)="onSaveMatchResult($event)"
      (saveSchedule)="onSaveMatchSchedule($event)"
      (close)="onModalClose()"
    ></app-match-detail-modal>
  `
})
export class DrawsComponent {
  @Input() participantNamesInput: Record<string, string> = {};
  @Input() participantOrderInput: Record<string, number> = {};
  @Input() courtsInput: CourtResponse[] = [];
  @Input() canManageInput = false;
  @Input() tournamentNameInput = '';
  @Input() categoryNameInput = '';
  @Input() setsPerMatch = 3;
  @Input() decisiveTiebreakPoints = 7;

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

  displayItems = computed(() => this.buildDisplayItems(this._draws()));

  @ViewChild('matchModal') matchModal!: MatchDetailModalComponent;

  @Output() matchSelected = new EventEmitter<string>();
  @Output() matchResultSaved = new EventEmitter<{ matchId: string; winnerId: string | null; result: string; sets?: SetScoreResponse[] | null; notes?: string | null; status: MatchStatus; keepOpen?: boolean }>();
  @Output() matchScheduleSaved = new EventEmitter<{
    matchId: string;
    courtId: string;
    scheduledAt: string;
    scheduleTimeType: MatchScheduleTimeType;
  }>();

  drawViewModes = signal<Record<string, DrawViewMode>>({});
  selectedMatch = signal<MatchResponse | null>(null);

  private buildDisplayItems(draws: DrawResponse[]): DrawDisplayItem[] {
    const eliminationDraw = draws.find(d => d.drawType === 'ELIMINATION');
    const losersDraw = draws.find(d => d.drawType === 'DOUBLE_ELIMINATION');

    if (eliminationDraw && losersDraw) {
      return [{
        key: `de-${eliminationDraw.id}`,
        label: 'Doble Eliminacion',
        isDoubleElimination: true,
        draws: [eliminationDraw, losersDraw]
      }];
    }

    return draws.map(draw => ({
      key: draw.id,
      label: draw.label,
      isDoubleElimination: false,
      draws: [draw]
    }));
  }

  toggleDrawView(drawId: string) {
    this.drawViewModes.update(viewModes => ({
      ...viewModes,
      [drawId]: this.getDrawViewMode(drawId) === 'tree' ? 'list' : 'tree'
    }));
  }

  getDrawViewMode(drawId: string): DrawViewMode {
    const stored = this.drawViewModes()[drawId];
    if (stored) {
      return stored;
    }
    const item = this.displayItems().find(i => i.key === drawId);
    if (item && item.isDoubleElimination) {
      return 'tree';
    }
    const draw = this._draws().find(d => d.id === drawId);
    if (draw && this.isRoundRobinDraw(draw)) {
      return 'list';
    }
    return 'tree';
  }

  isRoundRobinDraw(draw: DrawResponse): boolean {
    return draw.drawType === 'ROUND_ROBIN';
  }

  getDrawViewToggleLabel(drawId: string): string {
    return this.getDrawViewMode(drawId) === 'tree' ? 'Ver listado' : 'Ver arbol';
  }

  getDrawTypeLabel(drawType: string): string {
    const labels: Record<string, string> = {
      ELIMINATION: 'Eliminatoria',
      CONSOLATION: 'Consolacion',
      ROUND_ROBIN: 'Liga',
      DOUBLE_ELIMINATION: 'Doble eliminacion'
    };

    return labels[drawType] ?? drawType;
  }

  getGroupName(groupIndex: number): string {
    return String.fromCharCode(65 + groupIndex);
  }

  getTotalMatchCount(draws: DrawResponse[]): number {
    return draws.reduce((sum, d) => sum + (d.matches?.length ?? 0), 0);
  }

  getAllMatches(draws: DrawResponse[]): MatchResponse[] {
    return draws.flatMap(d => d.matches ?? []);
  }

  onMatchSelected(match: MatchResponse) {
    this.selectedMatch.set(match);
    if (this.matchModal) {
      this.matchModal.open();
    }
  }

  onModalClose() {
    this.selectedMatch.set(null);
  }

  onSaveMatchResult(event: { matchId: string; winnerId: string | null; result: string; sets?: SetScoreResponse[] | null; notes?: string | null; status: MatchStatus; keepOpen?: boolean }) {
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
  }) {
    if (!this.canManageInput) {
      return;
    }

    this.matchScheduleSaved.emit(event);
  }
}
