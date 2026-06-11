import { Component, Input, Output, EventEmitter, signal, computed, ElementRef, HostListener, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CourtResponse, DrawResponse, MatchResponse, MatchScheduleTimeType } from '../../../data/interfaces/tournament.model';
import { MatchDetailModalComponent } from './match-detail-modal.component';

@Component({
  selector: 'app-bracket',
  standalone: true,
  imports: [CommonModule, MatchDetailModalComponent],
  template: `
    <div class="bracket-root space-y-4" #fullscreenRoot>
      @if (showTitleInput) {
        <h5 class="text-sm font-medium text-neutral-900">Cuadro de partidos</h5>
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
                <div class="mb-3 flex flex-col gap-2 rounded-lg border border-slate-200 bg-slate-50 p-2 sm:flex-row sm:items-center sm:justify-between">
                  <div class="flex items-center gap-1.5">
                    <button type="button" class="inline-flex h-8 min-w-8 items-center justify-center rounded-md border border-slate-300 bg-white text-sm font-extrabold text-slate-900 transition-colors hover:border-blue-500 hover:bg-blue-50 hover:text-blue-700 disabled:cursor-not-allowed disabled:opacity-45" (click)="zoomOut()" [disabled]="zoomLevel() <= minZoom">
                      -
                    </button>
                    <span class="min-w-14 text-center text-xs font-extrabold text-slate-600">{{ getZoomLabel() }}</span>
                    <button type="button" class="inline-flex h-8 min-w-8 items-center justify-center rounded-md border border-slate-300 bg-white text-sm font-extrabold text-slate-900 transition-colors hover:border-blue-500 hover:bg-blue-50 hover:text-blue-700 disabled:cursor-not-allowed disabled:opacity-45" (click)="zoomIn()" [disabled]="zoomLevel() >= maxZoom">
                      +
                    </button>
                    <button type="button" class="inline-flex h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-sm font-extrabold text-slate-900 transition-colors hover:border-blue-500 hover:bg-blue-50 hover:text-blue-700" (click)="resetZoom()">
                      Restablecer
                    </button>
                  </div>
                  <button type="button" class="inline-flex h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-sm font-extrabold text-slate-900 transition-colors hover:border-blue-500 hover:bg-blue-50 hover:text-blue-700" (click)="toggleFullscreen()">
                    {{ isFullscreen() ? 'Salir pantalla completa' : 'Pantalla completa' }}
                  </button>
                </div>
                <div class="bracket-shell">
                  <div class="bracket-scroll">
                    <div
                      class="bracket-zoom-surface"
                      [style.width.px]="getScaledBoardWidth(draw.matches)"
                      [style.height.px]="getScaledBoardHeight(rounds)"
                    >
                      <div
                        class="bracket-board"
                        [style.min-width.px]="getMinWidth(draw.matches)"
                        [style.transform]="getZoomTransform()"
                      >
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
                                    [class.bracket-player-empty]="!match.firstInscriptionId && !isByeSlot(match, match.firstInscriptionId, match.secondInscriptionId)"
                                    [class.bracket-player-bye]="isByeSlot(match, match.firstInscriptionId, match.secondInscriptionId)"
                                  >
                                    <span class="truncate">{{ getMatchSlotLabel(match, match.firstInscriptionId, match.secondInscriptionId) }}</span>
                                    @if (match.professionalMatch && match.firstInscriptionId) {
                                      <span
                                        class="bracket-points"
                                        [class.bracket-points-winner]="isWinner(match, match.firstInscriptionId)"
                                        [class.bracket-points-loser]="isLoser(match, match.firstInscriptionId)"
                                      >
                                        {{ getWinPointsLabel(match.firstWinPoints) }}
                                      </span>
                                    }
                                    @if (isWinner(match, match.firstInscriptionId)) {
                                      <span class="bracket-winner-mark">G</span>
                                    }
                                  </span>

                                  <span
                                    class="bracket-player"
                                    [class.bracket-player-winner]="isWinner(match, match.secondInscriptionId)"
                                    [class.bracket-player-empty]="!match.secondInscriptionId && !isByeSlot(match, match.secondInscriptionId, match.firstInscriptionId)"
                                    [class.bracket-player-bye]="isByeSlot(match, match.secondInscriptionId, match.firstInscriptionId)"
                                  >
                                    <span class="truncate">{{ getMatchSlotLabel(match, match.secondInscriptionId, match.firstInscriptionId) }}</span>
                                    @if (match.professionalMatch && match.secondInscriptionId) {
                                      <span
                                        class="bracket-points"
                                        [class.bracket-points-winner]="isWinner(match, match.secondInscriptionId)"
                                        [class.bracket-points-loser]="isLoser(match, match.secondInscriptionId)"
                                      >
                                        {{ getWinPointsLabel(match.secondWinPoints) }}
                                      </span>
                                    }
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
                </div>
              } @else {
                <p class="text-xs text-neutral-600">Sin partidos generados</p>
              }
            </div>
          }
        </div>
      }

      <app-match-detail-modal
        #matchModal
        [matchInput]="selectedMatch()"
        [participantNamesInput]="participantNamesInput"
        [courtsInput]="courtsInput"
        (saveResult)="onSaveMatchResult($event)"
        (saveSchedule)="onSaveMatchSchedule($event)"
        (close)="onModalClose()"
      ></app-match-detail-modal>
    </div>
  `,
  styles: [`
    .bracket-shell {
      border: 1px solid rgb(226 232 240);
      border-radius: 0.5rem;
      background: linear-gradient(180deg, rgb(248 250 252), rgb(255 255 255));
    }

    .bracket-root:fullscreen {
      overflow: auto;
      background: rgb(255 255 255);
      padding: 1rem;
    }

    .bracket-scroll {
      overflow: auto;
      padding: 1rem 1.25rem 1.25rem;
    }

    .bracket-zoom-surface {
      position: relative;
    }

    .bracket-board {
      display: flex;
      gap: 3.5rem;
      align-items: flex-start;
      transform-origin: top left;
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

    .bracket-player-bye {
      border-style: dashed;
      color: rgb(100 116 139);
      font-style: italic;
      font-weight: 700;
    }

    .bracket-player-winner {
      border-color: rgb(134 239 172);
      background: rgb(240 253 244);
      color: rgb(20 83 45);
    }

    .bracket-points {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 3rem;
      height: 1.375rem;
      flex: 0 0 auto;
      border-radius: 9999px;
      background: rgb(241 245 249);
      color: rgb(100 116 139);
      font-size: 0.6875rem;
      font-weight: 800;
      white-space: nowrap;
    }

    .bracket-points-winner {
      background: rgb(220 252 231);
      color: rgb(21 128 61);
      box-shadow: inset 0 0 0 1px rgb(134 239 172);
    }

    .bracket-points-loser {
      background: rgb(248 250 252);
      color: rgb(148 163 184);
      text-decoration: line-through;
      text-decoration-thickness: 2px;
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
  private readonly matchHeight = 136;
  private readonly slotPitch = 170;
  readonly minZoom = 0.6;
  readonly maxZoom = 1.6;
  private readonly zoomStep = 0.1;

  @Input() participantNamesInput: Record<string, string> = {};
  @Input() participantOrderInput: Record<string, number> = {};
  @Input() courtsInput: CourtResponse[] = [];
  @Input() showTitleInput = true;
  @Input() showDrawCardInput = true;

  @Input() set drawsInput(value: DrawResponse[]) {
    this._draws.set(value);
  }
  private _draws = signal<DrawResponse[]>([]);
  draws = computed(() => this._draws());
  zoomLevel = signal(1);
  isFullscreen = signal(false);
  selectedMatch = signal<MatchResponse | null>(null);

  @ViewChild('fullscreenRoot') fullscreenRoot?: ElementRef<HTMLElement>;
  @ViewChild('matchModal') matchModal?: MatchDetailModalComponent;

  @Output() matchSelected = new EventEmitter<MatchResponse>();
  @Output() matchResultSaved = new EventEmitter<{ matchId: string; winnerId: string; result: string }>();
  @Output() matchScheduleSaved = new EventEmitter<{
    matchId: string;
    courtId: string;
    scheduledAt: string;
    scheduleTimeType: MatchScheduleTimeType;
  }>();

  @HostListener('document:fullscreenchange')
  onFullscreenChange(): void {
    this.isFullscreen.set(document.fullscreenElement === this.fullscreenRoot?.nativeElement);
  }

  onMatchClicked(match: MatchResponse) {
    this.selectedMatch.set(match);
    this.matchModal?.open();
    this.matchSelected.emit(match);
  }

  onModalClose(): void {
    this.selectedMatch.set(null);
  }

  onSaveMatchResult(event: { matchId: string; winnerId: string; result: string }): void {
    this.matchResultSaved.emit(event);
    this.selectedMatch.set(null);
  }

  onSaveMatchSchedule(event: {
    matchId: string;
    courtId: string;
    scheduledAt: string;
    scheduleTimeType: MatchScheduleTimeType;
  }): void {
    this.matchScheduleSaved.emit(event);
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
        matches: roundMatches
      }));
  }

  getMinWidth(matches: MatchResponse[] | undefined): number {
    if (!matches) {
      return 400;
    }
    const rounds = this.getRounds(matches);
    return Math.max(360, rounds.length * 300);
  }

  getScaledBoardWidth(matches: MatchResponse[] | undefined): number {
    return this.getMinWidth(matches) * this.zoomLevel();
  }

  getScaledBoardHeight(rounds: Array<{ roundNumber: number; matches: MatchResponse[] }>): number {
    return this.getBracketContentHeight(rounds) * this.zoomLevel();
  }

  getBracketContentHeight(rounds: Array<{ roundNumber: number; matches: MatchResponse[] }>): number {
    return 56 + this.getBracketBodyHeight(rounds);
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
    const matchesInRound = allMatches.filter((m) => m.roundNumber === match.roundNumber);
    return (matchesInRound.indexOf(match) + 1) || 1;
  }

  isWinner(match: MatchResponse, inscriptionId: string | null | undefined): boolean {
    return !!inscriptionId && match.winnerId === inscriptionId;
  }

  isLoser(match: MatchResponse, inscriptionId: string | null | undefined): boolean {
    return !!inscriptionId && !!match.winnerId && match.winnerId !== inscriptionId;
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

  isByeSlot(
    match: MatchResponse,
    inscriptionId: string | null | undefined,
    opponentInscriptionId: string | null | undefined
  ): boolean {
    return !inscriptionId && !!opponentInscriptionId && (match.roundNumber ?? 1) === 1;
  }

  getParticipantName(inscriptionId: string | null | undefined): string {
    if (!inscriptionId) {
      return 'Por definir';
    }

    return this.sanitizeParticipantName(this.participantNamesInput[inscriptionId]) ?? inscriptionId.substring(0, 8);
  }

  getWinPointsLabel(points: number | null | undefined): string {
    return points == null ? '+0 pts' : `+${points} pts`;
  }

  getZoomLabel(): string {
    return `${Math.round(this.zoomLevel() * 100)}%`;
  }

  getZoomTransform(): string {
    return `scale(${this.zoomLevel()})`;
  }

  zoomIn(): void {
    this.setZoom(this.zoomLevel() + this.zoomStep);
  }

  zoomOut(): void {
    this.setZoom(this.zoomLevel() - this.zoomStep);
  }

  resetZoom(): void {
    this.zoomLevel.set(1);
  }

  toggleFullscreen(): void {
    const fullscreenElement = this.fullscreenRoot?.nativeElement;
    if (!fullscreenElement) {
      return;
    }

    if (document.fullscreenElement === fullscreenElement) {
      document.exitFullscreen().catch(() => undefined);
      return;
    }

    fullscreenElement.requestFullscreen().catch(() => undefined);
  }

  private setZoom(value: number): void {
    const clampedValue = Math.min(this.maxZoom, Math.max(this.minZoom, value));
    this.zoomLevel.set(Math.round(clampedValue * 10) / 10);
  }

  private sanitizeParticipantName(name: string | undefined): string | null {
    const sanitizedName = name
      ?.replace(/\bnull\b/gi, '')
      .replace(/\bundefined\b/gi, '')
      .replace(/\s+/g, ' ')
      .trim();

    return sanitizedName || null;
  }

}
