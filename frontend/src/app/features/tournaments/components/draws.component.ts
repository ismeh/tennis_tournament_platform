import { Component, Input, Output, EventEmitter, signal, computed, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DrawResponse, MatchResponse } from '../../../data/interfaces/tournament.model';
import { MatchesComponent } from './matches.component';
import { BracketComponent } from './bracket.component';
import { MatchDetailModalComponent } from './match-detail-modal.component';

type DrawViewMode = 'tree' | 'list';

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
          @for (draw of draws(); track draw.id) {
            <div class="rounded-md border border-neutral-200 bg-white p-4">
              <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <p class="font-medium text-neutral-900">{{ draw.label }}</p>
                  <p class="text-xs text-neutral-500">Tipo: {{ draw.drawType }}</p>
                  <p class="text-xs text-neutral-500">{{ (draw.matches || []).length }} enfrentamientos</p>
                </div>
                <button
                  type="button"
                  (click)="toggleDrawView(draw.id)"
                  class="inline-flex items-center justify-center rounded-lg border border-primary-200 px-3 py-2 text-sm font-medium text-primary-700 transition-colors hover:bg-primary-50"
                  [attr.aria-label]="getDrawViewToggleLabel(draw.id)"
                >
                  {{ getDrawViewToggleLabel(draw.id) }}
                </button>
              </div>

              @if ((draw.matches || []).length > 0) {
                <div class="mt-3 border-t border-neutral-200 pt-3">
                  @if (getDrawViewMode(draw.id) === 'tree') {
                    <app-bracket
                      [drawsInput]="[draw]"
                      [participantNamesInput]="participantNamesInput"
                      [participantOrderInput]="participantOrderInput"
                      [showTitleInput]="false"
                      [showDrawCardInput]="false"
                      (matchSelected)="onMatchSelected($event)"
                    ></app-bracket>
                  } @else {
                    <app-matches
                      [matchesInput]="draw.matches || []"
                      [participantNamesInput]="participantNamesInput"
                      [participantOrderInput]="participantOrderInput"
                      (matchSelected)="onMatchSelected($event)"
                    ></app-matches>
                  }
                </div>
              }

              @if ((draw.matches || []).length === 0) {
                <div class="mt-3 border-t border-neutral-200 pt-3 text-sm text-neutral-600">
                  Sin enfrentamientos
                </div>
              }
            </div>
          }
        </div>
      }
    </div>

    <!-- Match Detail Modal -->
    <app-match-detail-modal 
      #matchModal 
      [matchInput]="selectedMatch()"
      [participantNamesInput]="participantNamesInput"
      (saveResult)="onSaveMatchResult($event)"
      (close)="onModalClose()"
    ></app-match-detail-modal>
  `
})
export class DrawsComponent {
  @Input() participantNamesInput: Record<string, string> = {};
  @Input() participantOrderInput: Record<string, number> = {};

  @Input() set drawsInput(value: DrawResponse[]) {
    this._draws.set(value);
  }
  private _draws = signal<DrawResponse[]>([]);
  draws = computed(() => this._draws());

  @ViewChild('matchModal') matchModal!: MatchDetailModalComponent;

  @Output() matchSelected = new EventEmitter<string>();
  @Output() matchResultSaved = new EventEmitter<{ matchId: string; winnerId: string; result: string }>();

  drawViewModes = signal<Record<string, DrawViewMode>>({});
  selectedMatch = signal<MatchResponse | null>(null);

  toggleDrawView(drawId: string) {
    this.drawViewModes.update(viewModes => ({
      ...viewModes,
      [drawId]: this.getDrawViewMode(drawId) === 'tree' ? 'list' : 'tree'
    }));
  }

  getDrawViewMode(drawId: string): DrawViewMode {
    return this.drawViewModes()[drawId] ?? 'tree';
  }

  getDrawViewToggleLabel(drawId: string): string {
    return this.getDrawViewMode(drawId) === 'tree' ? 'Ver listado' : 'Ver árbol';
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

  onSaveMatchResult(event: { matchId: string; winnerId: string; result: string }) {
    this.matchResultSaved.emit(event);
    this.selectedMatch.set(null);
  }
}
