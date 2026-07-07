import { TestBed } from '@angular/core/testing';
import { MatchDetailModalComponent } from './match-detail-modal.component';
import { CourtResponse, MatchResponse, MatchStatus } from '../../../data/interfaces/tournament.model';

function createMatch(overrides: Record<string, any> = {}): MatchResponse {
  return {
    id: 'match-1',
    firstInscriptionId: 'p1',
    secondInscriptionId: 'p2',
    roundNumber: 1,
    scheduledAt: '2026-05-01T10:00',
    court: 'Pista 1',
    result: '',
    professionalMatch: false,
    firstWinPoints: null,
    secondWinPoints: null,
    winnerId: null,
    status: 'PENDING' as MatchStatus,
    ...overrides,
  };
}

function createCourt(overrides: Record<string, any> = {}): CourtResponse {
  return {
    id: 'court-1',
    tournamentId: 't1',
    name: 'Pista Central',
    active: true,
    ...overrides,
  };
}

describe('MatchDetailModalComponent', () => {
  let component: MatchDetailModalComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatchDetailModalComponent],
      providers: [],
    }).compileComponents();

    const fixture = TestBed.createComponent(MatchDetailModalComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('initial state', () => {
    it('should start with modal closed', () => {
      expect(component.isOpen()).toBeFalse();
    });

    it('should start with interactive scoring mode', () => {
      expect(component.scoringMode()).toBe('interactive');
    });

    it('should have empty match initially', () => {
      expect(component.match()).toBeNull();
    });
  });

  describe('onClose', () => {
    it('should close the modal', () => {
      component.isOpen.set(true);
      component.onClose();
      expect(component.isOpen()).toBeFalse();
    });

    it('should emit close event', () => {
      spyOn(component.close, 'emit');
      component.onClose();
      expect(component.close.emit).toHaveBeenCalled();
    });
  });

  describe('getParticipantName', () => {
    it('should return name from participantNamesInput', () => {
      component.participantNamesInput = { 'p1': 'Carlos Garcia' };
      expect(component.getParticipantName('p1')).toBe('Carlos Garcia');
    });

    it('should return truncated id when name not found', () => {
      component.participantNamesInput = {};
      expect(component.getParticipantName('long-uuid-here')).toBe('long-uui');
    });

    it('should return default for null inscriptionId', () => {
      expect(component.getParticipantName(null)).toBe('Participante');
    });

    it('should return default for undefined inscriptionId', () => {
      expect(component.getParticipantName(undefined)).toBe('Participante');
    });
  });

  describe('getWinPointsLabel', () => {
    it('should return +0 pts for null', () => {
      expect(component.getWinPointsLabel(null)).toBe('+0 pts');
    });

    it('should return +0 pts for undefined', () => {
      expect(component.getWinPointsLabel(undefined)).toBe('+0 pts');
    });

    it('should return formatted points', () => {
      expect(component.getWinPointsLabel(5)).toBe('+5 pts');
    });
  });

  describe('getScheduleTypeLabel', () => {
    it('should return No antes de for NOT_BEFORE', () => {
      expect(component.getScheduleTypeLabel('NOT_BEFORE')).toBe('No antes de');
    });

    it('should return A esta hora for EXACT', () => {
      expect(component.getScheduleTypeLabel('EXACT')).toBe('A esta hora');
    });

    it('should return A esta hora for null', () => {
      expect(component.getScheduleTypeLabel(null)).toBe('A esta hora');
    });

    it('should return A esta hora for undefined', () => {
      expect(component.getScheduleTypeLabel(undefined)).toBe('A esta hora');
    });
  });

  describe('isFormValid', () => {
    it('should return false when no match', () => {
      component.match.set(null);
      expect(component.isFormValid()).toBeFalse();
    });

    it('should return true when winner selected', () => {
      component.match.set(createMatch());
      component.selectedWinnerId = 'p1';
      expect(component.isFormValid()).toBeTrue();
    });

    it('should return true when result entered', () => {
      component.match.set(createMatch());
      component.matchResult = '6-4 7-5';
      expect(component.isFormValid()).toBeTrue();
    });

    it('should return false when no winner and no result', () => {
      component.match.set(createMatch());
      component.selectedWinnerId = '';
      component.matchResult = '';
      expect(component.isFormValid()).toBeFalse();
    });
  });

  describe('isScheduleValid', () => {
    it('should return false when no match', () => {
      component.match.set(null);
      expect(component.isScheduleValid()).toBeFalse();
    });

    it('should return true when all fields filled', () => {
      component.match.set(createMatch());
      component.selectedCourtId = 'court-1';
      component.scheduledAtInput = '2026-05-01T10:00';
      expect(component.isScheduleValid()).toBeTrue();
    });

    it('should return false when no court', () => {
      component.match.set(createMatch());
      component.selectedCourtId = '';
      component.scheduledAtInput = '2026-05-01T10:00';
      expect(component.isScheduleValid()).toBeFalse();
    });

    it('should return false when no date', () => {
      component.match.set(createMatch());
      component.selectedCourtId = 'court-1';
      component.scheduledAtInput = '';
      expect(component.isScheduleValid()).toBeFalse();
    });
  });

  describe('activeCourts', () => {
    it('should return only active courts', () => {
      component.courtsInput = [
        createCourt({ id: 'c1', active: true }),
        createCourt({ id: 'c2', active: false }),
        createCourt({ id: 'c3', active: true }),
      ];
      expect(component.activeCourts().length).toBe(2);
    });

    it('should return empty when no active courts', () => {
      component.courtsInput = [createCourt({ active: false })];
      expect(component.activeCourts().length).toBe(0);
    });
  });

  describe('onSave', () => {
    it('should not emit if canManageInput is false', () => {
      spyOn(component.saveResult, 'emit');
      component.canManageInput = false;
      component.onSave();
      expect(component.saveResult.emit).not.toHaveBeenCalled();
    });

    it('should show validation message if form invalid', () => {
      component.canManageInput = true;
      component.match.set(createMatch());
      component.selectedWinnerId = '';
      component.matchResult = '';
      component.onSave();
      expect(component.validationMessage()).toBeTruthy();
    });
  });

  describe('onSaveSchedule', () => {
    it('should not emit if canManageInput is false', () => {
      spyOn(component.saveSchedule, 'emit');
      component.canManageInput = false;
      component.onSaveSchedule();
      expect(component.saveSchedule.emit).not.toHaveBeenCalled();
    });

    it('should show validation message if schedule invalid', () => {
      component.canManageInput = true;
      component.match.set(createMatch());
      component.selectedCourtId = '';
      component.scheduledAtInput = '';
      component.onSaveSchedule();
      expect(component.scheduleValidationMessage()).toBeTruthy();
    });
  });

  describe('resetGamePoints', () => {
    it('should reset all point signals to 0', () => {
      component.gamePointsP1.set(3);
      component.gamePointsP2.set(4);
      component.tbPointsP1.set(5);
      component.tbPointsP2.set(6);
      component.resetGamePoints();
      expect(component.gamePointsP1()).toBe(0);
      expect(component.gamePointsP2()).toBe(0);
      expect(component.tbPointsP1()).toBe(0);
      expect(component.tbPointsP2()).toBe(0);
    });
  });

  describe('isTiebreak', () => {
    it('should return false when sets empty', () => {
      component.interactiveSets.set([]);
      expect(component.isTiebreak()).toBeFalse();
    });

    it('should return true when both at 6 games', () => {
      component.interactiveSets.set([{ p1Games: 6, p2Games: 6 }]);
      component.currentSetIndex.set(0);
      expect(component.isTiebreak()).toBeTrue();
    });

    it('should return false when not tied at 6', () => {
      component.interactiveSets.set([{ p1Games: 4, p2Games: 3 }]);
      component.currentSetIndex.set(0);
      expect(component.isTiebreak()).toBeFalse();
    });
  });

  describe('isDecisiveSet', () => {
    it('should return true when current set is last', () => {
      component.setsPerMatch = 3;
      component.currentSetIndex.set(2);
      expect(component.isDecisiveSet()).toBeTrue();
    });

    it('should return false when not last set', () => {
      component.setsPerMatch = 3;
      component.currentSetIndex.set(1);
      expect(component.isDecisiveSet()).toBeFalse();
    });
  });

  describe('getPointsDisplay', () => {
    it('should display tiebreak points', () => {
      component.interactiveSets.set([{ p1Games: 6, p2Games: 6 }]);
      component.currentSetIndex.set(0);
      component.tbPointsP1.set(5);
      expect(component.getPointsDisplay(1)).toBe('5');
    });

    it('should display 0 points', () => {
      component.interactiveSets.set([{ p1Games: 3, p2Games: 2 }]);
      component.currentSetIndex.set(0);
      component.gamePointsP1.set(0);
      expect(component.getPointsDisplay(1)).toBe('0');
    });

    it('should display 15 points', () => {
      component.interactiveSets.set([{ p1Games: 3, p2Games: 2 }]);
      component.currentSetIndex.set(0);
      component.gamePointsP1.set(1);
      expect(component.getPointsDisplay(1)).toBe('15');
    });

    it('should display 30 points', () => {
      component.interactiveSets.set([{ p1Games: 3, p2Games: 2 }]);
      component.currentSetIndex.set(0);
      component.gamePointsP1.set(2);
      expect(component.getPointsDisplay(1)).toBe('30');
    });

    it('should display 40 points', () => {
      component.interactiveSets.set([{ p1Games: 3, p2Games: 2 }]);
      component.currentSetIndex.set(0);
      component.gamePointsP1.set(3);
      expect(component.getPointsDisplay(1)).toBe('40');
    });

    it('should display Ad points', () => {
      component.interactiveSets.set([{ p1Games: 3, p2Games: 2 }]);
      component.currentSetIndex.set(0);
      component.gamePointsP1.set(4);
      expect(component.getPointsDisplay(1)).toBe('Ad');
    });
  });

  describe('formatInteractiveResultString', () => {
    it('should format simple set', () => {
      const result = component.formatInteractiveResultString([{ p1Games: 6, p2Games: 4 }]);
      expect(result).toBe('6-4');
    });

    it('should format tiebreak with loser tiebreak points', () => {
      const result = component.formatInteractiveResultString([{ p1Games: 7, p2Games: 6, p2Tiebreak: 4 }]);
      expect(result).toBe('7-6(4)');
    });

    it('should format tiebreak with winner tiebreak points', () => {
      const result = component.formatInteractiveResultString([{ p1Games: 6, p2Games: 7, p1Tiebreak: 3 }]);
      expect(result).toBe('6-7(3)');
    });

    it('should format multiple sets', () => {
      const result = component.formatInteractiveResultString([
        { p1Games: 6, p2Games: 4 },
        { p1Games: 3, p2Games: 6 },
        { p1Games: 7, p2Games: 5 },
      ]);
      expect(result).toBe('6-4 3-6 7-5');
    });

    it('should filter sets with null games', () => {
      const result = component.formatInteractiveResultString([
        { p1Games: 6, p2Games: 4 },
        { p1Games: null, p2Games: null },
      ]);
      expect(result).toBe('6-4');
    });
  });

  describe('getInteractiveSetGames', () => {
    it('should return - for out of range set', () => {
      component.interactiveSets.set([{ p1Games: 6, p2Games: 4 }]);
      expect(component.getInteractiveSetGames(1, 5)).toBe('-');
    });

    it('should return games for player 1', () => {
      component.interactiveSets.set([{ p1Games: 6, p2Games: 4 }]);
      expect(component.getInteractiveSetGames(1, 0)).toBe('6');
    });

    it('should return games for player 2', () => {
      component.interactiveSets.set([{ p1Games: 6, p2Games: 4 }]);
      expect(component.getInteractiveSetGames(2, 0)).toBe('4');
    });

    it('should return tiebreak score for winner', () => {
      component.interactiveSets.set([{ p1Games: 7, p2Games: 6, p1Tiebreak: 7, p2Tiebreak: 4 }]);
      expect(component.getInteractiveSetGames(1, 0)).toBe('7(4)');
    });

    it('should return tiebreak score for loser', () => {
      component.interactiveSets.set([{ p1Games: 6, p2Games: 7, p1Tiebreak: 3, p2Tiebreak: 7 }]);
      expect(component.getInteractiveSetGames(2, 0)).toBe('7(3)');
    });
  });

  describe('onStatusChanged', () => {
    it('should auto-save in interactive mode', () => {
      spyOn(component.saveResult, 'emit');
      component.match.set(createMatch());
      component.scoringMode.set('interactive');
      component.selectedStatus = 'COMPLETED';
      component.onStatusChanged();
      expect(component.saveResult.emit).toHaveBeenCalled();
    });

    it('should not auto-save in manual mode', () => {
      spyOn(component.saveResult, 'emit');
      component.match.set(createMatch());
      component.scoringMode.set('manual');
      component.selectedStatus = 'COMPLETED';
      component.onStatusChanged();
      expect(component.saveResult.emit).not.toHaveBeenCalled();
    });
  });

  describe('onWinnerChanged', () => {
    it('should auto-save in interactive mode', () => {
      spyOn(component.saveResult, 'emit');
      component.match.set(createMatch());
      component.scoringMode.set('interactive');
      component.selectedWinnerId = 'p1';
      component.onWinnerChanged();
      expect(component.saveResult.emit).toHaveBeenCalled();
    });
  });

  describe('onNotesChanged', () => {
    it('should auto-save in interactive mode', () => {
      spyOn(component.saveResult, 'emit');
      component.match.set(createMatch());
      component.scoringMode.set('interactive');
      component.notesInput = 'Test note';
      component.onNotesChanged();
      expect(component.saveResult.emit).toHaveBeenCalled();
    });
  });

  describe('getMinTiebreak', () => {
    it('should return minimum of tiebreak points', () => {
      const set = { setNumber: 1, firstPlayerGames: 7, secondPlayerGames: 6, firstPlayerTiebreak: 4, secondPlayerTiebreak: 7 };
      expect(component.getMinTiebreak(set)).toBe(4);
    });

    it('should handle null tiebreak points', () => {
      const set = { setNumber: 1, firstPlayerGames: 6, secondPlayerGames: 4, firstPlayerTiebreak: null, secondPlayerTiebreak: null };
      expect(component.getMinTiebreak(set)).toBe(0);
    });
  });

  describe('getParticipantLastName', () => {
    it('should return last part of name', () => {
      component.participantNamesInput = { 'p1': 'Carlos Garcia' };
      expect(component.getParticipantLastName('p1')).toBe('Garcia');
    });

    it('should return full name if single word', () => {
      component.participantNamesInput = { 'p1': 'Carlos' };
      expect(component.getParticipantLastName('p1')).toBe('Carlos');
    });

    it('should return Participante for null', () => {
      expect(component.getParticipantLastName(null)).toBe('Participante');
    });
  });

  describe('key down event listeners', () => {
    it('should close on Escape keydown if open', () => {
      spyOn(component, 'onClose');
      component.isOpen.set(true);
      const event = new KeyboardEvent('keydown', { key: 'Escape' });
      component.onEscapeKeyDown(event);
      expect(component.onClose).toHaveBeenCalled();
    });

    it('should not close on Escape keydown if closed', () => {
      spyOn(component, 'onClose');
      component.isOpen.set(false);
      const event = new KeyboardEvent('keydown', { key: 'Escape' });
      component.onEscapeKeyDown(event);
      expect(component.onClose).not.toHaveBeenCalled();
    });

    it('should save on Enter keydown in manual mode if open and form is valid', () => {
      spyOn(component, 'onSave');
      component.isOpen.set(true);
      component.canManageInput = true;
      component.scoringMode.set('manual');
      const event = new KeyboardEvent('keydown', { key: 'Enter' });
      component.onEnterKeyDown(event);
      expect(component.onSave).toHaveBeenCalled();
    });
  });

  describe('isByeSlot', () => {
    it('should detect bye slots', () => {
      component.match.set(createMatch({ roundNumber: 1 }));
      expect(component.isByeSlot(null, 'p2')).toBeTrue();
      expect(component.isByeSlot('p1', 'p2')).toBeFalse();
    });
  });

  describe('save schedule', () => {
    it('should emit saveSchedule when schedule is valid', () => {
      spyOn(component.saveSchedule, 'emit');
      component.canManageInput = true;
      component.match.set(createMatch());
      component.selectedCourtId = 'court-123';
      component.scheduledAtInput = '2026-05-01T10:00';
      component.selectedScheduleTimeType = 'EXACT';

      component.onSaveSchedule();
      expect(component.saveSchedule.emit).toHaveBeenCalledWith({
        matchId: 'match-1',
        courtId: 'court-123',
        scheduledAt: '2026-05-01T10:00',
        scheduleTimeType: 'EXACT'
      });
    });
  });

  describe('interactive scoring points logic', () => {
    it('should increment and decrement game points correctly for Player 1', () => {
      component.match.set(createMatch({ firstInscriptionId: 'p1', secondInscriptionId: 'p2' }));
      component.scoringMode.set('interactive');
      component.resetInteractiveScoreState();

      // starts at 0 (0)
      expect(component.getPointsDisplay(1)).toBe('0');

      component.incrementPoint(1); // 15
      expect(component.getPointsDisplay(1)).toBe('15');

      component.incrementPoint(1); // 30
      expect(component.getPointsDisplay(1)).toBe('30');

      component.decrementPoint(1); // 15
      expect(component.getPointsDisplay(1)).toBe('15');
    });

    it('should handle deuce and Ad advantage', () => {
      component.match.set(createMatch({ firstInscriptionId: 'p1', secondInscriptionId: 'p2' }));
      component.scoringMode.set('interactive');
      component.resetInteractiveScoreState();

      // P1: 40, P2: 40
      component.gamePointsP1.set(3);
      component.gamePointsP2.set(3);

      component.incrementPoint(1); // P1 gets Ad
      expect(component.getPointsDisplay(1)).toBe('Ad');
      expect(component.getPointsDisplay(2)).toBe('40');

      component.incrementPoint(2); // back to deuce (40-40)
      expect(component.getPointsDisplay(1)).toBe('40');
      expect(component.getPointsDisplay(2)).toBe('40');
    });

    it('should win game and advance set or match on winGame/checkSetWin', () => {
      component.match.set(createMatch({ firstInscriptionId: 'p1', secondInscriptionId: 'p2' }));
      component.scoringMode.set('interactive');
      component.setsPerMatch = 3;
      component.resetInteractiveScoreState();

      // Set games to 5-4 for P1
      component.interactiveSets.set([{ p1Games: 5, p2Games: 4 }]);
      component.gamePointsP1.set(3); // P1 at 40 points

      // P1 wins the game, thus winning the set 6-4
      component.incrementPoint(1);
      expect(component.interactiveSets()[0].p1Games).toBe(6);
      expect(component.interactiveSets()[0].p2Games).toBe(4);

      // It should push a second set because the match is not over yet (best of 3 sets needs 2 sets won)
      expect(component.interactiveSets().length).toBe(2);
      expect(component.currentSetIndex()).toBe(1);

      // Now set second set to 5-0 for P1
      component.interactiveSets.set([
        { p1Games: 6, p2Games: 4 },
        { p1Games: 5, p2Games: 0 }
      ]);
      component.currentSetIndex.set(1);
      component.gamePointsP1.set(3); // 40 points

      // P1 wins the game, thus winning the set 6-0 and the match 2-0 sets
      component.incrementPoint(1);
      expect(component.selectedWinnerId).toBe('p1');
      expect(component.selectedStatus).toBe('COMPLETED');
      expect(component.matchResult).toBe('6-4 6-0');
    });

    it('should remove current set and go back when decrementing at 0-0 in second set', () => {
      component.match.set(createMatch({ firstInscriptionId: 'p1', secondInscriptionId: 'p2' }));
      component.scoringMode.set('interactive');
      component.setsPerMatch = 3;
      
      // We are at 0-0 in second set, after first set ended 6-4
      component.interactiveSets.set([
        { p1Games: 6, p2Games: 4 },
        { p1Games: 0, p2Games: 0 }
      ]);
      component.currentSetIndex.set(1);
      component.resetGamePoints();

      // Decrementing point for P1 when points are 0 should prompt confirmation
      component.decrementPoint(1);
      expect(component.pendingGamesDecrementPlayer()).toBe(1);

      // Decrementing again should remove second set and go back to first set
      component.decrementPoint(1);
      expect(component.interactiveSets().length).toBe(1);
      expect(component.currentSetIndex()).toBe(0);
      expect(component.matchResult).toBe('6-4');
    });
  });

  describe('getWinPointsClasses', () => {
    it('should return green classes for winner', () => {
      const match = createMatch({ winnerId: 'p1' });
      const classes = component.getWinPointsClasses(match, 'p1');
      expect(classes).toContain('bg-green-100');
    });

    it('should return line-through classes for loser', () => {
      const match = createMatch({ winnerId: 'p2' });
      const classes = component.getWinPointsClasses(match, 'p1');
      expect(classes).toContain('line-through');
    });

    it('should return default classes otherwise', () => {
      const match = createMatch({ winnerId: null });
      const classes = component.getWinPointsClasses(match, 'p1');
      expect(classes).toContain('bg-neutral-100');
      expect(classes).not.toContain('line-through');
    });
  });

  describe('getInteractiveSetGames branches', () => {
    it('should return - for out of bound index', () => {
      component.interactiveSets.set([{ p1Games: 6, p2Games: 4 }]);
      expect(component.getInteractiveSetGames(1, 1)).toBe('-');
    });

    it('should return games with tiebreak for player 1 winning tiebreak', () => {
      component.interactiveSets.set([{ p1Games: 7, p2Games: 6, p1Tiebreak: 7, p2Tiebreak: 5 }]);
      expect(component.getInteractiveSetGames(1, 0)).toBe('7(5)');
    });

    it('should return games with tiebreak for player 1 losing tiebreak', () => {
      component.interactiveSets.set([{ p1Games: 6, p2Games: 7, p1Tiebreak: 4, p2Tiebreak: 7 }]);
      expect(component.getInteractiveSetGames(1, 0)).toBe('6(4)');
    });
  });

  describe('onManualScoreChanged branch decisions', () => {
    it('should decide winner when P1 wins required sets', () => {
      component.match.set(createMatch({ firstInscriptionId: 'p1', secondInscriptionId: 'p2' }));
      component.setsPerMatch = 3;
      component.manualSets.set([
        { p1Games: 6, p2Games: 4, p1Tiebreak: null, p2Tiebreak: null },
        { p1Games: 6, p2Games: 2, p1Tiebreak: null, p2Tiebreak: null }
      ]);
      component.onManualScoreChanged();
      expect(component.selectedWinnerId).toBe('p1');
      expect(component.selectedStatus).toBe('COMPLETED');
    });

    it('should decide winner when P2 wins required sets', () => {
      component.match.set(createMatch({ firstInscriptionId: 'p1', secondInscriptionId: 'p2' }));
      component.setsPerMatch = 3;
      component.manualSets.set([
        { p1Games: 4, p2Games: 6, p1Tiebreak: null, p2Tiebreak: null },
        { p1Games: 2, p2Games: 6, p1Tiebreak: null, p2Tiebreak: null }
      ]);
      component.onManualScoreChanged();
      expect(component.selectedWinnerId).toBe('p2');
      expect(component.selectedStatus).toBe('COMPLETED');
    });

    it('should keep pending status when no player has won enough sets', () => {
      component.match.set(createMatch({ firstInscriptionId: 'p1', secondInscriptionId: 'p2' }));
      component.setsPerMatch = 3;
      component.manualSets.set([
        { p1Games: 6, p2Games: 4, p1Tiebreak: null, p2Tiebreak: null },
        { p1Games: 3, p2Games: 6, p1Tiebreak: null, p2Tiebreak: null }
      ]);
      component.onManualScoreChanged();
      expect(component.selectedWinnerId).toBe('');
      expect(component.selectedStatus).toBe('PENDING');
    });
  });
});
