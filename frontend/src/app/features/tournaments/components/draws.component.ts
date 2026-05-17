import { Component, Input, Output, EventEmitter, signal, computed, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DrawResponse, MatchResponse } from '../../../data/interfaces/tournament.model';
import { MatchesComponent } from './matches.component';
import { BracketComponent } from './bracket.component';
import { MatchDetailModalComponent } from './match-detail-modal.component';

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
        <!-- Bracket Visualization -->
        <app-bracket [drawsInput]="draws()" (matchSelected)="onMatchSelected($event)"></app-bracket>
        
        <!-- Legacy matches list (optional, can be hidden) -->
        <div class="space-y-3 border-t border-neutral-200 pt-4">
          <p class="text-xs font-semibold text-neutral-600 uppercase">Vista alternativa: Lista de enfrentamientos</p>
          @for (draw of draws(); track draw.id) {
            <div class="rounded-md border border-neutral-200 bg-neutral-50 p-4">
              <div class="flex items-center justify-between">
                <div>
                  <p class="font-medium text-neutral-900">{{ draw.label }}</p>
                  <p class="text-xs text-neutral-500">Tipo: {{ draw.drawType }}</p>
                  <p class="text-xs text-neutral-500">{{ (draw.matches || []).length }} enfrentamientos</p>
                </div>
                <button
                  (click)="toggleDraw(draw.id)"
                  class="rounded px-3 py-1 text-sm text-primary-600 hover:bg-primary-50 hover:text-primary-700"
                >
                  {{ expandedDrawId() === draw.id ? '−' : '+' }}
                </button>
              </div>

              @if (expandedDrawId() === draw.id && (draw.matches || []).length > 0) {
                <div class="mt-3 border-t border-neutral-200 pt-3">
                  <app-matches [matchesInput]="draw.matches || []" (matchSelected)="onRowMatchSelected($event)"></app-matches>
                </div>
              }

              @if (expandedDrawId() === draw.id && (draw.matches || []).length === 0) {
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
      (saveResult)="onSaveMatchResult($event)"
      (close)="onModalClose()"
    ></app-match-detail-modal>
  `
})
export class DrawsComponent {
  @Input() set drawsInput(value: DrawResponse[]) {
    this._draws.set(value);
  }
  private _draws = signal<DrawResponse[]>([]);
  draws = computed(() => this._draws());

  @ViewChild('matchModal') matchModal!: MatchDetailModalComponent;

  @Output() matchSelected = new EventEmitter<string>();
  @Output() matchResultSaved = new EventEmitter<{ matchId: string; winnerId: string; result: string }>();

  expandedDrawId = signal<string | null>(null);
  selectedMatch = signal<MatchResponse | null>(null);

  toggleDraw(drawId: string) {
    this.expandedDrawId.set(this.expandedDrawId() === drawId ? null : drawId);
  }

  onMatchSelected(match: MatchResponse) {
    this.selectedMatch.set(match);
    if (this.matchModal) {
      this.matchModal.open();
    }
  }

  onRowMatchSelected(matchId: string) {
    this.matchSelected.emit(matchId);
  }

  onModalClose() {
    this.selectedMatch.set(null);
  }

  onSaveMatchResult(event: { matchId: string; winnerId: string; result: string }) {
    this.matchResultSaved.emit(event);
    this.selectedMatch.set(null);
  }
}
