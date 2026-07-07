import { TestBed } from '@angular/core/testing';
import { BracketComponent } from './bracket.component';
import { DrawResponse, MatchResponse } from '../../../data/interfaces/tournament.model';
import { BracketExportService } from '../services/bracket-export.service';

describe('BracketComponent', () => {
  let component: BracketComponent;
  let bracketExportServiceSpy: jasmine.SpyObj<BracketExportService>;

  const baseMatch: Partial<MatchResponse> = {
    id: 'm1',
    roundNumber: 1,
    bracketPosition: 1,
    firstInscriptionId: 'p1',
    secondInscriptionId: 'p2',
    winnerId: null,
    result: null,
    professionalMatch: false,
    firstWinPoints: null,
    secondWinPoints: null,
    scheduledAt: null,
    courtId: null,
    status: 'PENDING',
    scheduleTimeType: null,
  };

  function makeMatch(overrides: Partial<MatchResponse>): MatchResponse {
    return { ...baseMatch, ...overrides } as MatchResponse;
  }

  function makeDraw(overrides: Partial<DrawResponse>): DrawResponse {
    return {
      id: 'd1',
      stageId: 's1',
      label: 'Draw',
      drawType: 'ELIMINATION',
      matches: [],
      ...overrides,
    } as DrawResponse;
  }

  beforeEach(() => {
    bracketExportServiceSpy = jasmine.createSpyObj('BracketExportService', ['exportBracket']);

    TestBed.configureTestingModule({
      imports: [BracketComponent],
      providers: [
        { provide: BracketExportService, useValue: bracketExportServiceSpy },
      ],
    });

    component = TestBed.createComponent(BracketComponent).componentInstance;
  });

  describe('getRounds', () => {
    it('should return empty array for undefined', () => {
      expect(component.getRounds(undefined)).toEqual([]);
    });

    it('should group matches by round', () => {
      const matches = [
        makeMatch({ id: 'm1', roundNumber: 1 }),
        makeMatch({ id: 'm2', roundNumber: 1 }),
        makeMatch({ id: 'm3', roundNumber: 2 }),
      ];
      const rounds = component.getRounds(matches);
      expect(rounds.length).toBe(2);
      expect(rounds[0].roundNumber).toBe(1);
      expect(rounds[0].matches.length).toBe(2);
      expect(rounds[1].roundNumber).toBe(2);
    });

    it('should default roundNumber to 1', () => {
      const matches = [makeMatch({ id: 'm1', roundNumber: undefined })];
      const rounds = component.getRounds(matches);
      expect(rounds[0].roundNumber).toBe(1);
    });

    it('should sort rounds by roundNumber', () => {
      const matches = [
        makeMatch({ id: 'm3', roundNumber: 3 }),
        makeMatch({ id: 'm1', roundNumber: 1 }),
        makeMatch({ id: 'm2', roundNumber: 2 }),
      ];
      const rounds = component.getRounds(matches);
      expect(rounds.map(r => r.roundNumber)).toEqual([1, 2, 3]);
    });
  });

  describe('getMinWidth', () => {
    it('should return 400 for undefined', () => {
      expect(component.getMinWidth(undefined)).toBe(400);
    });

    it('should enforce minimum 360', () => {
      const matches = [makeMatch({ roundNumber: 1 })];
      expect(component.getMinWidth(matches)).toBe(360);
    });

    it('should calculate width based on rounds', () => {
      const matches = [
        makeMatch({ roundNumber: 1 }),
        makeMatch({ roundNumber: 2 }),
      ];
      const width = component.getMinWidth(matches);
      expect(width).toBeGreaterThanOrEqual(360);
    });
  });

  describe('getRoundLabel', () => {
    it('should return Final', () => {
      expect(component.getRoundLabel(4, 4)).toBe('Final');
    });

    it('should return Semifinales', () => {
      expect(component.getRoundLabel(3, 4)).toBe('Semifinales');
    });

    it('should return Cuartos', () => {
      expect(component.getRoundLabel(2, 4)).toBe('Cuartos');
    });

    it('should return Ronda N', () => {
      expect(component.getRoundLabel(1, 5)).toBe('Ronda 1');
    });
  });

  describe('getDoubleEliminationRoundLabel', () => {
    it('should return Gran Final for winners', () => {
      expect(component.getDoubleEliminationRoundLabel(4, 4, 'WINNERS')).toBe('Gran Final');
    });

    it('should return Semifinal Ganadores', () => {
      expect(component.getDoubleEliminationRoundLabel(3, 4, 'WINNERS')).toBe('Semifinal Ganadores');
    });

    it('should return Cuartos Ganadores', () => {
      expect(component.getDoubleEliminationRoundLabel(2, 4, 'WINNERS')).toBe('Cuartos Ganadores');
    });

    it('should return Ronda Ganadores N', () => {
      expect(component.getDoubleEliminationRoundLabel(1, 5, 'WINNERS')).toBe('Ronda Ganadores 1');
    });

    it('should return Ronda Perdedores N', () => {
      expect(component.getDoubleEliminationRoundLabel(1, 3, 'LOSERS')).toBe('Ronda Perdedores 1');
    });
  });

  describe('getBracketBodyHeight', () => {
    it('should return 360 minimum for empty', () => {
      expect(component.getBracketBodyHeight([])).toBe(360);
    });

    it('should calculate height based on first round matches', () => {
      const rounds = [{ roundNumber: 1, matches: [makeMatch({}), makeMatch({}), makeMatch({}), makeMatch({})] }];
      expect(component.getBracketBodyHeight(rounds)).toBeGreaterThan(360);
    });
  });

  describe('getMatchTop', () => {
    it('should calculate position for first round', () => {
      expect(component.getMatchTop(0, 0)).toBe(0);
      expect(component.getMatchTop(0, 1)).toBe(200);
    });

    it('should calculate position for second round', () => {
      expect(component.getMatchTop(1, 0)).toBe(100);
    });
  });

  describe('getConnectorHeight', () => {
    it('should calculate connector height', () => {
      expect(component.getConnectorHeight(0)).toBe(200);
      expect(component.getConnectorHeight(1)).toBe(400);
    });
  });

  describe('isLastRound', () => {
    it('should return true for last round', () => {
      expect(component.isLastRound(2, 3)).toBeTrue();
    });

    it('should return false for non-last round', () => {
      expect(component.isLastRound(0, 3)).toBeFalse();
    });
  });

  describe('shouldShowConnectorRail', () => {
    it('should return true for even index with more matches', () => {
      expect(component.shouldShowConnectorRail(0, 4)).toBeTrue();
    });

    it('should return false for odd index', () => {
      expect(component.shouldShowConnectorRail(1, 4)).toBeFalse();
    });

    it('should return false when last match', () => {
      expect(component.shouldShowConnectorRail(2, 3)).toBeFalse();
    });
  });

  describe('isWinner', () => {
    it('should return true when inscriptionId matches winnerId', () => {
      const match = makeMatch({ winnerId: 'p1' });
      expect(component.isWinner(match, 'p1')).toBeTrue();
    });

    it('should return false when inscriptionId is null', () => {
      const match = makeMatch({ winnerId: 'p1' });
      expect(component.isWinner(match, null)).toBeFalse();
    });

    it('should return false when inscriptionId does not match', () => {
      const match = makeMatch({ winnerId: 'p1' });
      expect(component.isWinner(match, 'p2')).toBeFalse();
    });
  });

  describe('isLoser', () => {
    it('should return true when there is a winner and it is not this player', () => {
      const match = makeMatch({ winnerId: 'p1' });
      expect(component.isLoser(match, 'p2')).toBeTrue();
    });

    it('should return false when no winner', () => {
      const match = makeMatch({ winnerId: null });
      expect(component.isLoser(match, 'p1')).toBeFalse();
    });

    it('should return false when player is winner', () => {
      const match = makeMatch({ winnerId: 'p1' });
      expect(component.isLoser(match, 'p1')).toBeFalse();
    });

    it('should return false when inscriptionId is null', () => {
      const match = makeMatch({ winnerId: 'p1' });
      expect(component.isLoser(match, null)).toBeFalse();
    });
  });

  describe('isByeSlot', () => {
    it('should return true for bye in round 1', () => {
      const match = makeMatch({ roundNumber: 1 });
      expect(component.isByeSlot(match, undefined, 'p1')).toBeTrue();
    });

    it('should return false when inscriptionId exists', () => {
      const match = makeMatch({ roundNumber: 1 });
      expect(component.isByeSlot(match, 'p1', 'p2')).toBeFalse();
    });

    it('should return false in round > 1', () => {
      const match = makeMatch({ roundNumber: 2 });
      expect(component.isByeSlot(match, undefined, 'p1')).toBeFalse();
    });

    it('should return false when both undefined', () => {
      const match = makeMatch({ roundNumber: 1 });
      expect(component.isByeSlot(match, undefined, undefined)).toBeFalse();
    });
  });

  describe('getMatchSlotLabel', () => {
    it('should return Bye for bye slot', () => {
      const match = makeMatch({ roundNumber: 1 });
      expect(component.getMatchSlotLabel(match, undefined, 'p1')).toBe('Bye');
    });

    it('should return participant name', () => {
      component.participantNamesInput = { p1: 'Player 1' };
      const match = makeMatch({});
      expect(component.getMatchSlotLabel(match, 'p1', 'p2')).toBe('Player 1');
    });
  });

  describe('getParticipantName', () => {
    it('should return Por definir for null', () => {
      expect(component.getParticipantName(null)).toBe('Por definir');
    });

    it('should return name from input', () => {
      component.participantNamesInput = { p1: 'John Doe' };
      expect(component.getParticipantName('p1')).toBe('John Doe');
    });

    it('should return truncated id when name missing', () => {
      expect(component.getParticipantName('longId')).toBe('longId');
    });
  });

  describe('getWinPointsLabel', () => {
    it('should return +0 pts for null', () => {
      expect(component.getWinPointsLabel(null)).toBe('+0 pts');
    });

    it('should return +N pts', () => {
      expect(component.getWinPointsLabel(10)).toBe('+10 pts');
    });
  });

  describe('zoom', () => {
    it('zoomIn should increase level', () => {
      component.zoomLevel.set(1);
      component.zoomIn();
      expect(component.zoomLevel()).toBe(1.1);
    });

    it('zoomOut should decrease level', () => {
      component.zoomLevel.set(1);
      component.zoomOut();
      expect(component.zoomLevel()).toBe(0.9);
    });

    it('resetZoom should set to 1', () => {
      component.zoomLevel.set(1.5);
      component.resetZoom();
      expect(component.zoomLevel()).toBe(1);
    });

    it('should clamp to min', () => {
      component.zoomLevel.set(0.6);
      component.zoomOut();
      expect(component.zoomLevel()).toBe(0.6);
    });

    it('should clamp to max', () => {
      component.zoomLevel.set(1.6);
      component.zoomIn();
      expect(component.zoomLevel()).toBe(1.6);
    });
  });

  describe('getZoomLabel', () => {
    it('should return zoom percentage', () => {
      component.zoomLevel.set(1);
      expect(component.getZoomLabel()).toBe('100%');
    });
  });

  describe('getZoomTransform', () => {
    it('should return scale transform', () => {
      component.zoomLevel.set(1.2);
      expect(component.getZoomTransform()).toBe('scale(1.2)');
    });
  });

  describe('isDoubleElimination', () => {
    it('should be false for empty draws', () => {
      component.drawsInput = [];
      expect(component.isDoubleElimination()).toBeFalse();
    });

    it('should be true for ELIMINATION + DOUBLE_ELIMINATION', () => {
      component.drawsInput = [
        makeDraw({ id: 'd1', drawType: 'ELIMINATION' }),
        makeDraw({ id: 'd2', drawType: 'DOUBLE_ELIMINATION' }),
      ];
      expect(component.isDoubleElimination()).toBeTrue();
    });

    it('should be false for two ELIMINATION draws', () => {
      component.drawsInput = [
        makeDraw({ id: 'd1', drawType: 'ELIMINATION' }),
        makeDraw({ id: 'd2', drawType: 'ELIMINATION' }),
      ];
      expect(component.isDoubleElimination()).toBeFalse();
    });

    it('should be false for single draw', () => {
      component.drawsInput = [makeDraw({ id: 'd1', drawType: 'ELIMINATION' })];
      expect(component.isDoubleElimination()).toBeFalse();
    });

    it('should be true for DOUBLE_ELIMINATION + ELIMINATION (reversed)', () => {
      component.drawsInput = [
        makeDraw({ id: 'd1', drawType: 'DOUBLE_ELIMINATION' }),
        makeDraw({ id: 'd2', drawType: 'ELIMINATION' }),
      ];
      expect(component.isDoubleElimination()).toBeTrue();
    });
  });

  describe('getWinnersDraw', () => {
    it('should return null when not double elimination', () => {
      component.drawsInput = [makeDraw({ id: 'd1', drawType: 'ELIMINATION' })];
      expect(component.getWinnersDraw()).toBeNull();
    });

    it('should return the ELIMINATION draw', () => {
      component.drawsInput = [
        makeDraw({ id: 'd1', drawType: 'DOUBLE_ELIMINATION' }),
        makeDraw({ id: 'd2', drawType: 'ELIMINATION' }),
      ];
      expect(component.getWinnersDraw()?.id).toBe('d2');
    });
  });

  describe('getLosersDraw', () => {
    it('should return null when not double elimination', () => {
      component.drawsInput = [makeDraw({ id: 'd1', drawType: 'ELIMINATION' })];
      expect(component.getLosersDraw()).toBeNull();
    });

    it('should return the DOUBLE_ELIMINATION draw', () => {
      component.drawsInput = [
        makeDraw({ id: 'd1', drawType: 'ELIMINATION' }),
        makeDraw({ id: 'd2', drawType: 'DOUBLE_ELIMINATION' }),
      ];
      expect(component.getLosersDraw()?.id).toBe('d2');
    });
  });

  describe('onSaveMatchResult', () => {
    it('should emit when canManage is true', () => {
      spyOn(component.matchResultSaved, 'emit');
      component.canManageInput = true;
      const event = { matchId: 'm1', winnerId: 'p1', result: '2-0', status: 'COMPLETED' as const };
      component.onSaveMatchResult(event);
      expect(component.matchResultSaved.emit).toHaveBeenCalledWith(event);
      expect(component.selectedMatch()).toBeNull();
    });

    it('should not emit when canManage is false', () => {
      spyOn(component.matchResultSaved, 'emit');
      component.canManageInput = false;
      component.onSaveMatchResult({ matchId: 'm1', winnerId: 'p1', result: '2-0', status: 'COMPLETED' as const });
      expect(component.matchResultSaved.emit).not.toHaveBeenCalled();
    });
  });

  describe('onSaveMatchSchedule', () => {
    it('should emit when canManage is true', () => {
      spyOn(component.matchScheduleSaved, 'emit');
      component.canManageInput = true;
      const event = { matchId: 'm1', courtId: 'c1', scheduledAt: '2025-01-01', scheduleTimeType: 'EXACT' as const };
      component.onSaveMatchSchedule(event);
      expect(component.matchScheduleSaved.emit).toHaveBeenCalledWith(event);
    });

    it('should not emit when canManage is false', () => {
      spyOn(component.matchScheduleSaved, 'emit');
      component.canManageInput = false;
      component.onSaveMatchSchedule({ matchId: 'm1', courtId: 'c1', scheduledAt: '2025-01-01', scheduleTimeType: 'EXACT' as const });
      expect(component.matchScheduleSaved.emit).not.toHaveBeenCalled();
    });
  });

  describe('onModalClose', () => {
    it('should clear selected match', () => {
      component.selectedMatch.set(makeMatch({}));
      component.onModalClose();
      expect(component.selectedMatch()).toBeNull();
    });
  });

  describe('onMatchClicked', () => {
    it('should set selected match and emit', () => {
      spyOn(component.matchSelected, 'emit');
      const match = makeMatch({});
      component.onMatchClicked(match);
      expect(component.selectedMatch()).toBe(match);
      expect(component.matchSelected.emit).toHaveBeenCalledWith(match);
    });
  });

  describe('getMatchNumber', () => {
    it('should return match position in round', () => {
      const matches = [
        makeMatch({ id: 'm1', roundNumber: 1, bracketPosition: 1 }),
        makeMatch({ id: 'm2', roundNumber: 1, bracketPosition: 2 }),
      ];
      expect(component.getMatchNumber(matches[1], matches)).toBe(2);
    });
  });

  describe('getScaledBoardWidth', () => {
    it('should scale with zoom', () => {
      component.zoomLevel.set(2);
      const matches = [makeMatch({ roundNumber: 1 })];
      const width = component.getScaledBoardWidth(matches);
      expect(width).toBe(component.getMinWidth(matches) * 2);
    });
  });

  describe('getScaledBoardHeight', () => {
    it('should scale with zoom', () => {
      component.zoomLevel.set(1.5);
      const rounds = [{ roundNumber: 1, matches: [makeMatch({})] }];
      const height = component.getScaledBoardHeight(rounds);
      expect(height).toBe(component.getBracketContentHeight(rounds) * 1.5);
    });
  });

  describe('getBracketContentHeight', () => {
    it('should return 56 + body height', () => {
      const rounds = [{ roundNumber: 1, matches: [makeMatch({})] }];
      expect(component.getBracketContentHeight(rounds)).toBe(56 + component.getBracketBodyHeight(rounds));
    });
  });

  describe('getRoundTopPadding', () => {
    it('should return 0 for first round', () => {
      expect(component.getRoundTopPadding(0)).toBe(0);
    });

    it('should calculate padding for later rounds', () => {
      expect(component.getRoundTopPadding(2)).toBe(Math.min(156, 36 * 2));
    });
  });

  describe('getRoundGap', () => {
    it('should calculate gap', () => {
      expect(component.getRoundGap(0)).toBe(18);
      expect(component.getRoundGap(1)).toBe(82);
    });
  });

  describe('sanitizeParticipantName', () => {
    it('should remove null text', () => {
      const result = (component as any).sanitizeParticipantName('Player null');
      expect(result).toBe('Player');
    });

    it('should remove undefined text', () => {
      const result = (component as any).sanitizeParticipantName('Player undefined');
      expect(result).toBe('Player');
    });

    it('should trim whitespace', () => {
      const result = (component as any).sanitizeParticipantName('  Player  ');
      expect(result).toBe('Player');
    });

    it('should return null for empty result', () => {
      const result = (component as any).sanitizeParticipantName(undefined);
      expect(result).toBeNull();
    });

    it('should normalize multiple spaces', () => {
      const result = (component as any).sanitizeParticipantName('Player   Name');
      expect(result).toBe('Player Name');
    });
  });

  describe('compareNumbers', () => {
    it('should compare two numbers', () => {
      const cmp = (component as any).compareNumbers.bind(component);
      expect(cmp(1, 2)).toBeLessThan(0);
      expect(cmp(2, 1)).toBeGreaterThan(0);
      expect(cmp(1, 1)).toBe(0);
    });

    it('should handle null/undefined', () => {
      const cmp = (component as any).compareNumbers.bind(component);
      expect(cmp(null, 1)).toBeGreaterThan(0);
      expect(cmp(1, null)).toBeLessThan(0);
      expect(cmp(null, null)).toBe(0);
      expect(cmp(undefined, 5)).toBeGreaterThan(0);
      expect(cmp(5, undefined)).toBeLessThan(0);
    });
  });

  describe('compareStrings', () => {
    it('should compare two strings', () => {
      const cmp = (component as any).compareStrings.bind(component);
      expect(cmp('a', 'b')).toBeLessThan(0);
      expect(cmp('b', 'a')).toBeGreaterThan(0);
      expect(cmp('a', 'a')).toBe(0);
    });

    it('should handle null/undefined', () => {
      const cmp = (component as any).compareStrings.bind(component);
      expect(cmp(null, 'a')).toBeLessThan(0);
      expect(cmp('a', null)).toBeGreaterThan(0);
    });
  });

  describe('toggleFullscreen', () => {
    it('should do nothing if no fullscreenRoot', () => {
      component.fullscreenRoot = undefined;
      expect(() => component.toggleFullscreen()).not.toThrow();
    });
  });

  describe('exportBracketPdf', () => {
    it('should do nothing if no bracketElement', async () => {
      component.fullscreenRoot = undefined;
      await component.exportBracketPdf();
      expect(bracketExportServiceSpy.exportBracket).not.toHaveBeenCalled();
    });

    it('should do nothing if already exporting', async () => {
      component.isExportingPdf.set(true);
      await component.exportBracketPdf();
      expect(bracketExportServiceSpy.exportBracket).not.toHaveBeenCalled();
    });
  });

  describe('onFullscreenChange', () => {
    it('should set fullscreen to false when no fullscreenElement', () => {
      component.isFullscreen.set(true);
      Object.defineProperty(document, 'fullscreenElement', { value: null, configurable: true });
      component.onFullscreenChange();
      expect(component.isFullscreen()).toBeFalse();
    });
  });

  describe('requestWakeLock', () => {
    it('should do nothing if wakeLock API is not available', async () => {
      const originalWakeLock = navigator.wakeLock;
      Object.defineProperty(navigator, 'wakeLock', {
        value: undefined,
        configurable: true,
        writable: true
      });
      await (component as any).requestWakeLock();
      expect((component as any).wakeLock).toBeNull();
      Object.defineProperty(navigator, 'wakeLock', {
        value: originalWakeLock,
        configurable: true,
        writable: true
      });
    });

    it('should request screen lock when API is available', async () => {
      const originalWakeLock = navigator.wakeLock;
      const mockSentinel = { addEventListener: jasmine.createSpy('addEventListener'), release: Promise.resolve.bind(Promise) };
      const mockWakeLock = {
        request: jasmine.createSpy('request').and.returnValue(Promise.resolve(mockSentinel))
      };
      Object.defineProperty(navigator, 'wakeLock', {
        value: mockWakeLock,
        configurable: true,
        writable: true
      });
      await (component as any).requestWakeLock();
      expect(mockWakeLock.request).toHaveBeenCalledWith('screen');
      expect((component as any).wakeLock).toBe(mockSentinel);
      Object.defineProperty(navigator, 'wakeLock', {
        value: originalWakeLock,
        configurable: true,
        writable: true
      });
    });
  });

  describe('releaseWakeLock', () => {
    it('should release and nullify wakeLock', async () => {
      const mockRelease = jasmine.createSpy('release').and.returnValue(Promise.resolve());
      (component as any).wakeLock = { release: mockRelease };
      (component as any).releaseWakeLock();
      expect(mockRelease).toHaveBeenCalled();
      expect((component as any).wakeLock).toBeNull();
    });

    it('should do nothing if no wakeLock', () => {
      (component as any).wakeLock = null;
      expect(() => (component as any).releaseWakeLock()).not.toThrow();
    });
  });
});
