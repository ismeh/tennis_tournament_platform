import { Component, Input, Output, EventEmitter, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DrawResponse, MatchResponse } from '../../../data/interfaces/tournament.model';

@Component({
  selector: 'app-bracket',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-4">
      @if (showTitleInput) {
        <h5 class="text-sm font-medium text-neutral-900">Árbol de Enfrentamientos</h5>
      }
      
      @if (draws().length === 0) {
        <p class="text-xs text-neutral-600">Sin cuadros</p>
      } @else {
        <div class="space-y-4">
          @for (draw of draws(); track draw.id) {
            <div [ngClass]="showDrawCardInput ? 'rounded-md border border-neutral-200 bg-white p-4' : ''">
              @if (showDrawCardInput) {
                <p class="mb-3 font-medium text-neutral-900">{{ draw.label }}</p>
              }

              @if (draw.matches && draw.matches.length > 0) {
                @let rounds = getRounds(draw.matches);
                <div class="bracket-shell">
                  <div class="bracket-scroll">
                    <div class="bracket-board" [style.min-width.px]="getMinWidth(draw.matches)">
                      @for (round of rounds; track round.roundNumber; let roundIndex = $index) {
                        <section class="bracket-round">
                          <div class="bracket-round-header">
                            <div>
                              <p class="bracket-round-title">{{ getRoundLabel(round.roundNumber, rounds.length) }}</p>
                              <p class="bracket-round-subtitle">Ronda {{ round.roundNumber }}</p>
                            </div>
                            <span class="bracket-round-count">{{ round.matches.length }}</span>
                          </div>

                          <div class="bracket-round-matches" [style.height.px]="getBracketBodyHeight(rounds)">
                            @for (match of round.matches; track match.id; let matchIndex = $index) {
                              <button
                                type="button"
                                (click)="onMatchClicked(match)"
                                class="bracket-match"
                                [class.bracket-match-complete]="!!match.winnerId"
                                [style.top.px]="getMatchTop(roundIndex, matchIndex)"
                              >
                                @if (roundIndex > 0) {
                                  <span class="bracket-input-line"></span>
                                }

                                @if (!isLastRound(roundIndex, rounds.length)) {
                                  <span class="bracket-output-line"></span>
                                  @if (shouldShowConnectorRail(matchIndex, round.matches.length)) {
                                    <span class="bracket-connector-rail" [style.height.px]="getConnectorHeight(roundIndex)"></span>
                                  }
                                }

                                <span class="bracket-match-meta">
                                  <span class="bracket-match-number">P{{ getMatchNumber(match, draw.matches || []) }}</span>
                                  @if (match.result) {
                                    <span class="bracket-status bracket-status-complete">Finalizado</span>
                                  } @else {
                                    <span class="bracket-status bracket-status-pending">Pendiente</span>
                                  }
                                </span>

                                <span
                                  class="bracket-player"
                                  [class.bracket-player-winner]="isWinner(match, match.firstInscriptionId)"
                                  [class.bracket-player-empty]="!match.firstInscriptionId"
                                >
                                  <span class="truncate">{{ getParticipantName(match.firstInscriptionId) }}</span>
                                  @if (isWinner(match, match.firstInscriptionId)) {
                                    <span class="bracket-winner-mark">G</span>
                                  }
                                </span>

                                <span
                                  class="bracket-player"
                                  [class.bracket-player-winner]="isWinner(match, match.secondInscriptionId)"
                                  [class.bracket-player-empty]="!match.secondInscriptionId"
                                >
                                  <span class="truncate">{{ getParticipantName(match.secondInscriptionId) }}</span>
                                  @if (isWinner(match, match.secondInscriptionId)) {
                                    <span class="bracket-winner-mark">G</span>
                                  }
                                </span>

                                @if (match.result) {
                                  <span class="bracket-result">{{ match.result }}</span>
                                }
                              </button>
                            }
                          </div>
                        </section>
                      }
                    </div>
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
  styles: [`
    .bracket-shell {
      border: 1px solid rgb(226 232 240);
      border-radius: 0.5rem;
      background: linear-gradient(180deg, rgb(248 250 252), rgb(255 255 255));
    }

    .bracket-scroll {
      overflow-x: auto;
      padding: 1rem 1.25rem 1.25rem;
    }

    .bracket-board {
      display: flex;
      gap: 3.5rem;
      align-items: flex-start;
    }

    .bracket-round {
      width: 15.75rem;
      flex: 0 0 15.75rem;
    }

    .bracket-round-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.625rem;
      margin-bottom: 0.875rem;
      min-height: 2.25rem;
      border-bottom: 1px solid rgb(226 232 240);
      padding-bottom: 0.625rem;
    }

    .bracket-round-title {
      color: rgb(15 23 42);
      font-size: 0.875rem;
      font-weight: 800;
      line-height: 1.1;
    }

    .bracket-round-subtitle {
      margin-top: 0.125rem;
      color: rgb(100 116 139);
      font-size: 0.6875rem;
      font-weight: 700;
      text-transform: uppercase;
    }

    .bracket-round-count {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 1.75rem;
      height: 1.5rem;
      border-radius: 9999px;
      background: rgb(241 245 249);
      color: rgb(71 85 105);
      font-size: 0.6875rem;
      font-weight: 800;
    }

    .bracket-round-matches {
      position: relative;
    }

    .bracket-match {
      position: absolute;
      left: 0;
      display: flex;
      width: 100%;
      min-height: 7.25rem;
      flex-direction: column;
      gap: 0.375rem;
      border: 1px solid rgb(203 213 225);
      border-left: 3px solid rgb(100 116 139);
      border-radius: 0.5rem;
      background: rgb(255 255 255);
      padding: 0.625rem;
      text-align: left;
      box-shadow: 0 8px 18px rgb(15 23 42 / 0.06);
      transition: border-color 160ms ease, box-shadow 160ms ease, transform 160ms ease, background 160ms ease;
      transform: translateY(-50%);
      z-index: 2;
    }

    .bracket-match:hover,
    .bracket-match:focus-visible {
      border-color: rgb(59 130 246);
      background: rgb(248 250 252);
      box-shadow: 0 12px 26px rgb(37 99 235 / 0.14);
      outline: none;
      transform: translateY(calc(-50% - 1px));
    }

    .bracket-match-complete {
      border-left-color: rgb(22 163 74);
    }

    .bracket-match-meta {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.5rem;
      color: rgb(71 85 105);
      font-size: 0.6875rem;
      font-weight: 600;
    }

    .bracket-match-number {
      color: rgb(30 41 59);
      font-weight: 800;
      letter-spacing: 0;
    }

    .bracket-status {
      flex: 0 0 auto;
      border-radius: 9999px;
      padding: 0.125rem 0.375rem;
      font-size: 0.625rem;
      font-weight: 700;
    }

    .bracket-status-complete {
      background: rgb(220 252 231);
      color: rgb(22 101 52);
    }

    .bracket-status-pending {
      background: rgb(254 249 195);
      color: rgb(133 77 14);
    }

    .bracket-player {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.5rem;
      min-height: 2rem;
      border: 1px solid rgb(226 232 240);
      border-radius: 0.375rem;
      background: rgb(248 250 252);
      padding: 0.375rem 0.5rem;
      color: rgb(15 23 42);
      font-size: 0.8125rem;
      font-weight: 700;
      line-height: 1.1;
    }

    .bracket-player-empty {
      color: rgb(148 163 184);
      font-style: italic;
      font-weight: 600;
    }

    .bracket-player-winner {
      border-color: rgb(134 239 172);
      background: rgb(240 253 244);
      color: rgb(20 83 45);
    }

    .bracket-winner-mark {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 1.375rem;
      height: 1.375rem;
      flex: 0 0 auto;
      border-radius: 9999px;
      background: rgb(22 163 74);
      color: white;
      font-size: 0.6875rem;
      font-weight: 800;
    }

    .bracket-result {
      align-self: flex-start;
      border-radius: 0.375rem;
      background: rgb(239 246 255);
      padding: 0.1875rem 0.4375rem;
      color: rgb(30 64 175);
      font-size: 0.6875rem;
      font-weight: 700;
    }

    .bracket-input-line,
    .bracket-output-line,
    .bracket-connector-rail {
      position: absolute;
      pointer-events: none;
      background: rgb(148 163 184);
      z-index: 1;
    }

    .bracket-input-line {
      top: 50%;
      left: -1.75rem;
      width: 1.75rem;
      height: 2px;
    }

    .bracket-output-line {
      top: 50%;
      right: -1.75rem;
      width: 1.75rem;
      height: 2px;
    }

    .bracket-connector-rail {
      top: 50%;
      right: -1.75rem;
      width: 2px;
    }
  `]
})
export class BracketComponent {
  private readonly matchHeight = 116;
  private readonly slotPitch = 150;

  @Input() participantNamesInput: Record<string, string> = {};
  @Input() participantOrderInput: Record<string, number> = {};
  @Input() showTitleInput = true;
  @Input() showDrawCardInput = true;

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

    return Array.from(roundMap.entries())
      .sort((a, b) => a[0] - b[0])
      .map(([roundNumber, roundMatches]) => ({
        roundNumber,
        matches: this.sortRoundMatches(roundMatches)
      }));
  }

  getMinWidth(matches: MatchResponse[] | undefined): number {
    if (!matches) {
      return 400;
    }
    const rounds = this.getRounds(matches);
    return Math.max(360, rounds.length * 300);
  }

  getRoundLabel(roundNumber: number, totalRounds: number): string {
    if (roundNumber === totalRounds) {
      return 'Final';
    }

    if (roundNumber === totalRounds - 1) {
      return 'Semifinales';
    }

    if (roundNumber === totalRounds - 2) {
      return 'Cuartos';
    }

    return `Ronda ${roundNumber}`;
  }

  getBracketBodyHeight(rounds: Array<{ roundNumber: number; matches: MatchResponse[] }>): number {
    const firstRoundMatchCount = rounds[0]?.matches.length ?? 1;
    return Math.max(320, this.matchHeight + Math.max(0, firstRoundMatchCount - 1) * this.slotPitch);
  }

  getMatchTop(roundIndex: number, matchIndex: number): number {
    const roundSpan = 2 ** roundIndex;
    return this.matchHeight / 2 + (matchIndex * roundSpan + (roundSpan - 1) / 2) * this.slotPitch;
  }

  getRoundTopPadding(roundIndex: number): number {
    return roundIndex === 0 ? 0 : Math.min(156, 36 * roundIndex);
  }

  getRoundGap(roundIndex: number): number {
    return 18 + roundIndex * 64;
  }

  getConnectorHeight(roundIndex: number): number {
    return this.slotPitch * (2 ** roundIndex);
  }

  isLastRound(roundIndex: number, totalRounds: number): boolean {
    return roundIndex === totalRounds - 1;
  }

  shouldShowConnectorRail(matchIndex: number, roundMatchCount: number): boolean {
    return matchIndex % 2 === 0 && matchIndex + 1 < roundMatchCount;
  }

  getMatchNumber(match: MatchResponse, allMatches: MatchResponse[]): number {
    const matchesInRound = this.sortRoundMatches(allMatches.filter((m) => m.roundNumber === match.roundNumber));
    return (matchesInRound.indexOf(match) + 1) || 1;
  }

  isWinner(match: MatchResponse, inscriptionId: string | undefined): boolean {
    return !!inscriptionId && match.winnerId === inscriptionId;
  }

  getParticipantName(inscriptionId: string | undefined): string {
    if (!inscriptionId) {
      return 'Por definir';
    }

    return this.sanitizeParticipantName(this.participantNamesInput[inscriptionId]) ?? inscriptionId.substring(0, 8);
  }

  private sanitizeParticipantName(name: string | undefined): string | null {
    const sanitizedName = name
      ?.replace(/\bnull\b/gi, '')
      .replace(/\bundefined\b/gi, '')
      .replace(/\s+/g, ' ')
      .trim();

    return sanitizedName || null;
  }

  private sortRoundMatches(matches: MatchResponse[]): MatchResponse[] {
    return [...matches].sort((left, right) =>
      this.compareNumbers(this.getMatchSeedOrder(left), this.getMatchSeedOrder(right)) ||
      this.compareStrings(this.getParticipantName(left.firstInscriptionId), this.getParticipantName(right.firstInscriptionId)) ||
      this.compareStrings(this.getParticipantName(left.secondInscriptionId), this.getParticipantName(right.secondInscriptionId)) ||
      this.compareStrings(left.id, right.id)
    );
  }

  private getMatchSeedOrder(match: MatchResponse): number {
    const firstOrder = this.getParticipantOrder(match.firstInscriptionId);
    const secondOrder = this.getParticipantOrder(match.secondInscriptionId);
    return Math.min(firstOrder, secondOrder);
  }

  private getParticipantOrder(inscriptionId: string | undefined): number {
    if (!inscriptionId) {
      return Number.MAX_SAFE_INTEGER;
    }

    return this.participantOrderInput[inscriptionId] ?? Number.MAX_SAFE_INTEGER;
  }

  private compareNumbers(left: number, right: number): number {
    return left - right;
  }

  private compareStrings(left: string | undefined, right: string | undefined): number {
    return (left ?? '').localeCompare(right ?? '');
  }
}
