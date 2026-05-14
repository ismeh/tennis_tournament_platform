import { Component, Input, Output, EventEmitter, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DrawResponse } from '../../../data/interfaces/tournament.model';
import { MatchesComponent } from './matches.component';

@Component({
  selector: 'app-draws',
  standalone: true,
  imports: [CommonModule, MatchesComponent],
  template: `
    <div class="space-y-4">
      <h4 class="font-semibold text-neutral-900">Cuadros</h4>
      
      @if (draws().length === 0) {
        <p class="text-sm text-neutral-600">Sin cuadros</p>
      } @else {
        <div class="space-y-3">
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
                  <app-matches [matchesInput]="draw.matches || []" (matchSelected)="onMatchSelected($event)"></app-matches>
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
  `
})
export class DrawsComponent {
  @Input() set drawsInput(value: DrawResponse[]) {
    this._draws.set(value);
  }
  private _draws = signal<DrawResponse[]>([]);
  draws = computed(() => this._draws());

  @Output() matchSelected = new EventEmitter<string>();

  expandedDrawId = signal<string | null>(null);

  toggleDraw(drawId: string) {
    this.expandedDrawId.set(this.expandedDrawId() === drawId ? null : drawId);
  }

  onMatchSelected(matchId: string) {
    this.matchSelected.emit(matchId);
  }
}
