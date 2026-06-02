import { Component, Input, Output, EventEmitter, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatchResponse } from '../../../data/interfaces/tournament.model';

@Component({
  selector: 'app-matches',
  standalone: true,
  imports: [CommonModule, DatePipe],
  template: `
    <div class="space-y-2">
      <h5 class="text-sm font-medium text-neutral-900">Partidos</h5>
      
      @if (matches().length === 0) {
        <p class="text-xs text-neutral-600">Sin partidos</p>
      } @else {
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead class="bg-neutral-100">
              <tr>
                <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">Ronda</th>
                <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">Partido</th>
                <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">Jugadores</th>
                <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">Estado</th>
                <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">Pista</th>
                <th class="px-3 py-2 text-left text-xs font-semibold text-neutral-700">Hora</th>
              </tr>
            </thead>
            <tbody>
              @for (match of sortedMatches(); track match.id) {
                <tr
                  (click)="onMatchClicked(match)"
                  class="cursor-pointer border-b border-neutral-200 hover:bg-primary-50"
                >
                  <td class="px-3 py-2">Ronda {{ match.roundNumber }}</td>
                  <td class="px-3 py-2">#{{ getMatchNumber(match) }}</td>
                  <td class="px-3 py-2">
                    <div class="max-w-64">
                      <p class="truncate font-medium text-neutral-900">{{ getParticipantName(match.firstInscriptionId) }}</p>
                      <p class="truncate text-neutral-600">{{ getParticipantName(match.secondInscriptionId) }}</p>
                    </div>
                  </td>
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
                  <td class="px-3 py-2">
                    @if (match.scheduledAt) {
                      <span>{{ getSchedulePrefix(match.scheduleTimeType) }} {{ match.scheduledAt | date : 'short' }}</span>
                    } @else {
                      <span>—</span>
                    }
                  </td>
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
  @Input() participantNamesInput: Record<string, string> = {};
  @Input() participantOrderInput: Record<string, number> = {};

  @Input() set matchesInput(value: MatchResponse[]) {
    this._matches.set(value);
  }
  private _matches = signal<MatchResponse[]>([]);
  matches = computed(() => this._matches());
  sortedMatches = computed(() =>
    this.matches()
      .map((match, index) => ({ match, index }))
      .sort((left, right) => this.compareMatches(left.match, right.match, left.index, right.index))
      .map(entry => entry.match)
  );

  @Output() matchSelected = new EventEmitter<MatchResponse>();

  onMatchClicked(match: MatchResponse): void {
    this.matchSelected.emit(match);
  }

  getMatchNumber(match: MatchResponse): number {
    const matchesInRound = this.sortedMatches().filter(candidate => candidate.roundNumber === match.roundNumber);
    return matchesInRound.indexOf(match) + 1;
  }

  getParticipantName(inscriptionId: string | null | undefined): string {
    if (!inscriptionId) {
      return 'Bye';
    }

    return this.participantNamesInput[inscriptionId] ?? inscriptionId.substring(0, 8);
  }

  getSchedulePrefix(scheduleTimeType: string | null | undefined): string {
    return scheduleTimeType === 'NOT_BEFORE' ? 'No antes de' : 'A las';
  }

  private compareMatches(left: MatchResponse, right: MatchResponse, leftIndex: number, rightIndex: number): number {
    return (
      this.compareNumbers(left.roundNumber, right.roundNumber) ||
      this.compareNumbers(this.getMatchSeedOrder(left), this.getMatchSeedOrder(right)) ||
      this.compareStrings(this.getParticipantName(left.firstInscriptionId), this.getParticipantName(right.firstInscriptionId)) ||
      this.compareStrings(this.getParticipantName(left.secondInscriptionId), this.getParticipantName(right.secondInscriptionId)) ||
      this.compareStrings(left.id, right.id) ||
      leftIndex - rightIndex
    );
  }

  private compareNumbers(left: number | undefined, right: number | undefined): number {
    return (left ?? Number.MAX_SAFE_INTEGER) - (right ?? Number.MAX_SAFE_INTEGER);
  }

  private compareStrings(left: string | null | undefined, right: string | null | undefined): number {
    return (left ?? '').localeCompare(right ?? '');
  }

  private getMatchSeedOrder(match: MatchResponse): number {
    const firstOrder = this.getParticipantOrder(match.firstInscriptionId);
    const secondOrder = this.getParticipantOrder(match.secondInscriptionId);
    return Math.min(firstOrder, secondOrder);
  }

  private getParticipantOrder(inscriptionId: string | null | undefined): number {
    if (!inscriptionId) {
      return Number.MAX_SAFE_INTEGER;
    }

    return this.participantOrderInput[inscriptionId] ?? Number.MAX_SAFE_INTEGER;
  }
}
