import { Component, Input, Output, EventEmitter, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DrawResponse, MatchResponse } from '../../../data/interfaces/tournament.model';

@Component({
  selector: 'app-bracket',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-4">
      <h5 class="text-sm font-medium text-neutral-900">Árbol de Enfrentamientos</h5>
      
      @if (draws().length === 0) {
        <p class="text-xs text-neutral-600">Sin cuadros</p>
      } @else {
        <div class="space-y-4">
          @for (draw of draws(); track draw.id) {
            <div class="rounded-md border border-neutral-200 bg-white p-4">
              <p class="mb-3 font-medium text-neutral-900">{{ draw.label }}</p>
              
              @if (draw.matches && draw.matches.length > 0) {
                <div class="overflow-x-auto">
                  <div class="flex gap-6 pb-4" [style.min-width.px]="getMinWidth(draw.matches)">
                    @for (round of getRounds(draw.matches); track round.roundNumber) {
                      <div class="flex flex-col gap-3">
                        <p class="text-xs font-semibold text-neutral-600">Ronda {{ round.roundNumber }}</p>
                        
                        @for (match of round.matches; track match.id) {
                          <div
                            (click)="onMatchClicked(match)"
                            class="w-48 cursor-pointer rounded-md border-2 border-neutral-200 bg-neutral-50 p-3 transition-all hover:border-primary-400 hover:bg-primary-50"
                          >
                            <div class="text-xs text-neutral-600">Match #{{ getMatchNumber(match, draw.matches || []) }}</div>
                            
                            @if (match.firstInscriptionId) {
                              <div class="mt-1 truncate text-sm font-medium text-neutral-900">
                                {{ getParticipantName(match.firstInscriptionId) }}
                              </div>
                            } @else {
                              <div class="mt-1 text-sm italic text-neutral-400">Pendiente asignación</div>
                            }
                            
                            <div class="my-1 border-b border-neutral-200"></div>
                            
                            @if (match.secondInscriptionId) {
                              <div class="truncate text-sm font-medium text-neutral-900">
                                {{ getParticipantName(match.secondInscriptionId) }}
                              </div>
                            } @else {
                              <div class="text-sm italic text-neutral-400">Pendiente asignación</div>
                            }
                            
                            @if (match.result) {
                              <div class="mt-2 rounded bg-green-100 px-2 py-1 text-xs text-green-800">
                                {{ match.result }}
                              </div>
                            } @else {
                              <div class="mt-2 rounded bg-yellow-100 px-2 py-1 text-xs text-yellow-800">
                                Pendiente
                              </div>
                            }
                          </div>
                        }
                      </div>
                    }
                  </div>
                </div>
              } @else {
                <p class="text-xs text-neutral-600">Sin enfrentamientos generados</p>
              }
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: []
})
export class BracketComponent {
  @Input() set drawsInput(value: DrawResponse[]) {
    this._draws.set(value);
  }
  private _draws = signal<DrawResponse[]>([]);
  draws = computed(() => this._draws());

  @Output() matchSelected = new EventEmitter<MatchResponse>();

  onMatchClicked(match: MatchResponse) {
    this.matchSelected.emit(match);
  }

  getRounds(matches: MatchResponse[] | undefined): Array<{ roundNumber: number; matches: MatchResponse[] }> {
    if (!matches) {
      return [];
    }

    const roundMap = new Map<number, MatchResponse[]>();
    matches.forEach((match) => {
      const roundNum = match.roundNumber || 1;
      if (!roundMap.has(roundNum)) {
        roundMap.set(roundNum, []);
      }
      roundMap.get(roundNum)!.push(match);
    });

    // Sort by round number
    const sortedRounds = Array.from(roundMap.entries())
      .sort((a, b) => a[0] - b[0])
      .map(([roundNumber, roundMatches]) => ({
        roundNumber,
        matches: roundMatches
      }));

    return sortedRounds;
  }

  getMinWidth(matches: MatchResponse[] | undefined): number {
    if (!matches) {
      return 400;
    }
    const rounds = this.getRounds(matches);
    return Math.max(400, rounds.length * 250);
  }

  getMatchNumber(match: MatchResponse, allMatches: MatchResponse[]): number {
    const matchesInRound = allMatches.filter((m) => m.roundNumber === match.roundNumber);
    return (matchesInRound.indexOf(match) + 1) || 1;
  }

  getParticipantName(inscriptionId: string | undefined): string {
    // TODO: Fetch participant name from service
    // For now, return inscriptionId as placeholder
    return inscriptionId ? inscriptionId.substring(0, 8) : 'Participante';
  }
}
