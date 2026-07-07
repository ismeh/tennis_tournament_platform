import { TestBed } from '@angular/core/testing';
import { MatchesComponent } from './matches.component';
import { MatchResponse } from '../../../data/interfaces/tournament.model';

describe('MatchesComponent', () => {
  let component: MatchesComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MatchesComponent]
    });

    const fixture = TestBed.createComponent(MatchesComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('getParticipantName', () => {
    it('returns name from participantNamesInput when available', () => {
      component.participantNamesInput = { 'ins-1': 'Carlos Alcaraz' };
      expect(component.getParticipantName('ins-1')).toBe('Carlos Alcaraz');
    });

    it('returns truncated inscriptionId when name not available', () => {
      expect(component.getParticipantName('ins-12345678-long')).toBe('ins-1234');
    });

    it('returns placeholder for null inscriptionId', () => {
      expect(component.getParticipantName(null)).toBe('Por determinar');
    });

    it('returns placeholder for undefined inscriptionId', () => {
      expect(component.getParticipantName(undefined)).toBe('Por determinar');
    });
  });

  describe('isByeSlot', () => {
    it('returns true when inscriptionId is null and opponent exists in round 1', () => {
      const match = { roundNumber: 1 } as MatchResponse;
      expect(component.isByeSlot(match, null, 'opponent-id')).toBe(true);
    });

    it('returns false when inscriptionId is present', () => {
      const match = { roundNumber: 1 } as MatchResponse;
      expect(component.isByeSlot(match, 'player-id', 'opponent-id')).toBe(false);
    });

    it('returns false when opponent is also null', () => {
      const match = { roundNumber: 1 } as MatchResponse;
      expect(component.isByeSlot(match, null, null)).toBe(false);
    });

    it('returns false when round is not 1', () => {
      const match = { roundNumber: 2 } as MatchResponse;
      expect(component.isByeSlot(match, null, 'opponent-id')).toBe(false);
    });
  });

  describe('getMatchSlotLabel', () => {
    it('returns Bye for bye slot', () => {
      const match = { roundNumber: 1 } as MatchResponse;
      expect(component.getMatchSlotLabel(match, null, 'opponent')).toBe('Bye');
    });

    it('returns participant name for non-bye slot', () => {
      component.participantNamesInput = { 'ins-1': 'Carlos' };
      const match = { roundNumber: 1 } as MatchResponse;
      expect(component.getMatchSlotLabel(match, 'ins-1', 'opponent')).toBe('Carlos');
    });
  });

  describe('getSchedulePrefix', () => {
    it('returns No antes de for NOT_BEFORE', () => {
      expect(component.getSchedulePrefix('NOT_BEFORE')).toBe('No antes de');
    });

    it('returns A las for other values', () => {
      expect(component.getSchedulePrefix('EXACT')).toBe('A las');
      expect(component.getSchedulePrefix(null)).toBe('A las');
      expect(component.getSchedulePrefix(undefined)).toBe('A las');
    });
  });

  describe('getWinPointsLabel', () => {
    it('returns +0 pts when points is null', () => {
      expect(component.getWinPointsLabel(null)).toBe('+0 pts');
      expect(component.getWinPointsLabel(undefined)).toBe('+0 pts');
    });

    it('returns formatted points when present', () => {
      expect(component.getWinPointsLabel(5)).toBe('+5 pts');
      expect(component.getWinPointsLabel(0)).toBe('+0 pts');
    });
  });

  describe('getWinPointsClasses', () => {
    it('returns green classes for winner', () => {
      const match = { winnerId: 'ins-1' } as MatchResponse;
      const classes = component.getWinPointsClasses(match, 'ins-1');
      expect(classes).toContain('bg-green-100');
      expect(classes).toContain('text-green-700');
    });

    it('returns neutral strikethrough classes for loser', () => {
      const match = { winnerId: 'ins-2' } as MatchResponse;
      const classes = component.getWinPointsClasses(match, 'ins-1');
      expect(classes).toContain('bg-neutral-100');
      expect(classes).toContain('line-through');
    });

    it('returns neutral classes for undecided', () => {
      const match = { winnerId: null } as MatchResponse;
      const classes = component.getWinPointsClasses(match, 'ins-1');
      expect(classes).toContain('bg-neutral-100');
      expect(classes).toContain('text-neutral-500');
    });
  });

  describe('sortedMatches', () => {
    it('sorts matches by scheduled time, then round, then position', () => {
      component.matchesInput = [
        { id: 'm2', roundNumber: 2, firstInscriptionId: 'a', secondInscriptionId: 'b', scheduledAt: '2026-06-01T10:00' } as MatchResponse,
        { id: 'm1', roundNumber: 1, firstInscriptionId: 'a', secondInscriptionId: 'b', scheduledAt: '2026-06-01T09:00' } as MatchResponse,
        { id: 'm3', roundNumber: 1, firstInscriptionId: 'a', secondInscriptionId: 'b', scheduledAt: null } as MatchResponse,
      ];

      const sorted = component.sortedMatches();
      expect(sorted[0].id).toBe('m1');
      expect(sorted[1].id).toBe('m2');
      expect(sorted[2].id).toBe('m3');
    });
  });

  describe('getMatchNumber', () => {
    it('returns position within the round', () => {
      component.participantNamesInput = {};
      component.matchesInput = [
        { id: 'm1', roundNumber: 1, firstInscriptionId: 'a', secondInscriptionId: 'b' } as MatchResponse,
        { id: 'm2', roundNumber: 1, firstInscriptionId: 'c', secondInscriptionId: 'd' } as MatchResponse,
        { id: 'm3', roundNumber: 2, firstInscriptionId: 'a', secondInscriptionId: 'c' } as MatchResponse,
      ];

      expect(component.getMatchNumber(component.sortedMatches()[0])).toBe(1);
      expect(component.getMatchNumber(component.sortedMatches()[1])).toBe(2);
    });
  });

  describe('onMatchClicked', () => {
    it('emits matchSelected event', () => {
      spyOn(component.matchSelected, 'emit');
      const match = { id: 'm1' } as MatchResponse;
      component.onMatchClicked(match);
      expect(component.matchSelected.emit).toHaveBeenCalledWith(match);
    });
  });

  describe('searchQuery and filteredMatches', () => {
    beforeEach(() => {
      component.participantNamesInput = {
        'ins-1': 'Carlos Alcaraz',
        'ins-2': 'Rafael Nadal',
        'ins-3': 'Novak Djokovic',
        'ins-4': 'Roger Federer'
      };
      component.matchesInput = [
        { id: 'm1', roundNumber: 1, firstInscriptionId: 'ins-1', secondInscriptionId: 'ins-2' } as MatchResponse,
        { id: 'm2', roundNumber: 1, firstInscriptionId: 'ins-3', secondInscriptionId: 'ins-4' } as MatchResponse,
        { id: 'm3', roundNumber: 2, firstInscriptionId: 'ins-1', secondInscriptionId: 'ins-3' } as MatchResponse,
      ];
    });

    it('returns all matches when search query is empty', () => {
      component.searchQuery.set('');
      expect(component.filteredMatches().length).toBe(3);
    });

    it('filters matches by first player name', () => {
      component.searchQuery.set('carlos');
      expect(component.filteredMatches().length).toBe(2);
      expect(component.filteredMatches().every(m =>
        m.firstInscriptionId === 'ins-1' || m.secondInscriptionId === 'ins-1'
      )).toBeTrue();
    });

    it('filters matches by second player name', () => {
      component.searchQuery.set('federer');
      expect(component.filteredMatches().length).toBe(1);
      expect(component.filteredMatches()[0].id).toBe('m2');
    });

    it('returns empty when no matches found', () => {
      component.searchQuery.set('nonexistent');
      expect(component.filteredMatches().length).toBe(0);
    });

    it('is case insensitive', () => {
      component.searchQuery.set('CARLOS');
      expect(component.filteredMatches().length).toBe(2);
    });

    it('trims whitespace from query', () => {
      component.searchQuery.set('  carlos  ');
      expect(component.filteredMatches().length).toBe(2);
    });
  });
});
