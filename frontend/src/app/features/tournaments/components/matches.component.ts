import { Component, Input, Output, EventEmitter, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatchResponse, MatchStatus, MATCH_STATUS_LABELS } from '../../../data/interfaces/tournament.model';

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
                      <p class="flex items-center gap-2 truncate font-medium text-neutral-900">
                        <span class="truncate" [class.bracket-player-bye]="isByeSlot(match, match.firstInscriptionId, match.secondInscriptionId)">{{ getMatchSlotLabel(match, match.firstInscriptionId, match.secondInscriptionId) }}</span>
                        @if (match.professionalMatch && match.firstInscriptionId) {
                          <span [class]="getWinPointsClasses(match, match.firstInscriptionId)">
                            {{ getWinPointsLabel(match.firstWinPoints) }}
                          </span>
                        }
                      </p>
                      <p class="mt-1 flex items-center gap-2 truncate text-neutral-600">
                        <span class="truncate" [class.bracket-player-bye]="isByeSlot(match, match.secondInscriptionId, match.firstInscriptionId)">{{ getMatchSlotLabel(match, match.secondInscriptionId, match.firstInscriptionId) }}</span>
                        @if (match.professionalMatch && match.secondInscriptionId) {
                          <span [class]="getWinPointsClasses(match, match.secondInscriptionId)">
                            {{ getWinPointsLabel(match.secondWinPoints) }}
                          </span>
                        }
                      </p>
                    </div>
                  </td>
                  <td class="px-3 py-2">
                    @if (match.status === 'WALKOVER') {
                      <span class="inline-block rounded bg-orange-100 px-2 py-1 text-xs text-orange-800">
                        Walkover
                      </span>
                    } @else if (match.status === 'RETIRED') {
                      <span class="inline-block rounded bg-amber-100 px-2 py-1 text-xs text-amber-800">
                        Retirada
                      </span>
                    } @else if (match.status === 'CANCELLED') {
                      <span class="inline-block rounded bg-red-100 px-2 py-1 text-xs text-red-800">
                        Cancelado
                      </span>
                    } @else if (match.status === 'SUSPENDED') {
                      <span class="inline-block rounded bg-purple-100 px-2 py-1 text-xs text-purple-800">
                        Suspendido
                      </span>
                    } @else if (match.status === 'IN_PROGRESS') {
                      <span class="inline-block rounded bg-blue-100 px-2 py-1 text-xs text-blue-800">
                        En juego{{ match.result ? ': ' + match.result : '' }}
                        @if (match.firstPlayerPoints && match.secondPlayerPoints) {
                          <span class="font-extrabold text-[10px] text-blue-900 ml-1">
                            ({{ match.firstPlayerPoints }}-{{ match.secondPlayerPoints }})
                          </span>
                        }
                      </span>
                    } @else if (match.winnerId) {
                      <span class="inline-block rounded bg-green-100 px-2 py-1 text-xs text-green-800">
                        Jugado: {{ match.result }}
                      </span>
                    } @else if (match.result) {
                      <span class="inline-block rounded bg-blue-100 px-2 py-1 text-xs text-blue-800">
                        En juego: {{ match.result }}
                        @if (match.firstPlayerPoints && match.secondPlayerPoints) {
                          <span class="font-extrabold text-[10px] text-blue-900 ml-1">
                            ({{ match.firstPlayerPoints }}-{{ match.secondPlayerPoints }})
                          </span>
                        }
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
  `,
  styles: [`
    .bracket-player-bye {
      border-style: dashed;
      color: rgb(100 116 139);
      font-style: italic;
      font-weight: 700;
    }
  `]
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
      return 'Por determinar';
    }

    return this.participantNamesInput[inscriptionId] ?? inscriptionId.substring(0, 8);
  }

  isByeSlot(
    match: MatchResponse,
    inscriptionId: string | null | undefined,
    opponentInscriptionId: string | null | undefined
  ): boolean {
    return !inscriptionId && !!opponentInscriptionId && (match.roundNumber ?? 1) === 1;
  }

  getMatchSlotLabel(
    match: MatchResponse,
    inscriptionId: string | null | undefined,
    opponentInscriptionId: string | null | undefined
  ): string {
    if (this.isByeSlot(match, inscriptionId, opponentInscriptionId)) {
      return 'Bye';
    }
    return this.getParticipantName(inscriptionId);
  }

  getSchedulePrefix(scheduleTimeType: string | null | undefined): string {
    return scheduleTimeType === 'NOT_BEFORE' ? 'No antes de' : 'A las';
  }

  getWinPointsLabel(points: number | null | undefined): string {
    return points == null ? '+0 pts' : `+${points} pts`;
  }

  getWinPointsClasses(match: MatchResponse, inscriptionId: string | null | undefined): string {
    const baseClasses = 'shrink-0 rounded-full px-2 py-0.5 text-[0.65rem] font-bold';

    if (this.isWinner(match, inscriptionId)) {
      return `${baseClasses} bg-green-100 text-green-700 ring-1 ring-green-200`;
    }

    if (this.isLoser(match, inscriptionId)) {
      return `${baseClasses} bg-neutral-100 text-neutral-400 line-through decoration-2`;
    }

    return `${baseClasses} bg-neutral-100 text-neutral-500`;
  }

  private isWinner(match: MatchResponse, inscriptionId: string | null | undefined): boolean {
    return !!inscriptionId && match.winnerId === inscriptionId;
  }

  private isLoser(match: MatchResponse, inscriptionId: string | null | undefined): boolean {
    return !!inscriptionId && !!match.winnerId && match.winnerId !== inscriptionId;
  }

  private compareMatches(left: MatchResponse, right: MatchResponse, leftIndex: number, rightIndex: number): number {
    const leftTime = left.scheduledAt ?? '9999-12-31T23:59';
    const rightTime = right.scheduledAt ?? '9999-12-31T23:59';
    return (
      this.compareStrings(leftTime, rightTime) ||
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
