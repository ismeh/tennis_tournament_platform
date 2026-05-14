import { Component, Input, Output, EventEmitter, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatchResponse } from '../../../data/interfaces/tournament.model';

@Component({
  selector: 'app-matches',
  standalone: true,
  imports: [CommonModule, DatePipe],
  template: `
    <div class="space-y-2">
      <h5 class="text-sm font-medium text-neutral-900">Enfrentamientos</h5>
      
      @if (matches().length === 0) {
        <p class="text-xs text-neutral-600">Sin enfrentamientos</p>
      } @else {
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead class="bg-neutral-100">
              <tr>
                <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">Ronda</th>
                <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">Estado</th>
                <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">Cancha</th>
                <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">Hora</th>
              </tr>
            </thead>
            <tbody>
              @for (match of matches(); track match.id) {
                <tr class="border-b border-neutral-200 hover:bg-neutral-50">
                  <td class="px-3 py-2">Ronda {{ match.roundNumber }}</td>
                  <td class="px-3 py-2">
                    @if (match.result) {
                      <span class="inline-block rounded bg-green-100 px-2 py-1 text-xs text-green-800">
                        Jugado: {{ match.result }}
                      </span>
                    } @else {
                      <span class="inline-block rounded bg-yellow-100 px-2 py-1 text-xs text-yellow-800">
                        Pendiente
                      </span>
                    }
                  </td>
                  <td class="px-3 py-2">{{ match.court || '—' }}</td>
                  <td class="px-3 py-2">{{ match.scheduledAt ? (match.scheduledAt | date : 'short') : '—' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `
})
export class MatchesComponent {
  @Input() set matchesInput(value: MatchResponse[]) {
    this._matches.set(value);
  }
  private _matches = signal<MatchResponse[]>([]);
  matches = computed(() => this._matches());

  @Output() matchSelected = new EventEmitter<string>();
}
