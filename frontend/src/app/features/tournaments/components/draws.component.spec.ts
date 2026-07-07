import { TestBed } from '@angular/core/testing';
import { DrawsComponent } from './draws.component';
import { DrawResponse, MatchResponse } from '../../../data/interfaces/tournament.model';

describe('DrawsComponent', () => {
  let component: DrawsComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [DrawsComponent]
    });

    const fixture = TestBed.createComponent(DrawsComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('getDrawTypeLabel', () => {
    it('returns correct labels', () => {
      expect(component.getDrawTypeLabel('ELIMINATION')).toBe('Eliminatoria');
      expect(component.getDrawTypeLabel('CONSOLATION')).toBe('Consolacion');
      expect(component.getDrawTypeLabel('ROUND_ROBIN')).toBe('Liga');
      expect(component.getDrawTypeLabel('DOUBLE_ELIMINATION')).toBe('Doble eliminacion');
    });

    it('returns raw value for unknown type', () => {
      expect(component.getDrawTypeLabel('CUSTOM')).toBe('CUSTOM');
    });
  });

  describe('getGroupName', () => {
    it('converts group index to letter', () => {
      expect(component.getGroupName(0)).toBe('A');
      expect(component.getGroupName(1)).toBe('B');
      expect(component.getGroupName(25)).toBe('Z');
    });
  });

  describe('getTotalMatchCount', () => {
    it('counts matches across draws', () => {
      const draws = [
        { id: 'd1', matches: [{ id: 'm1' }, { id: 'm2' }] },
        { id: 'd2', matches: [{ id: 'm3' }] }
      ] as any;
      expect(component.getTotalMatchCount(draws)).toBe(3);
    });

    it('returns 0 for empty matches', () => {
      const draws = [{ id: 'd1', matches: [] }] as any;
      expect(component.getTotalMatchCount(draws)).toBe(0);
    });

    it('handles draws with no matches property', () => {
      const draws = [{ id: 'd1' }] as any;
      expect(component.getTotalMatchCount(draws)).toBe(0);
    });
  });

  describe('getAllMatches', () => {
    it('flattens matches from multiple draws', () => {
      const draws = [
        { id: 'd1', matches: [{ id: 'm1' }, { id: 'm2' }] },
        { id: 'd2', matches: [{ id: 'm3' }] }
      ] as any;
      const result = component.getAllMatches(draws);
      expect(result.length).toBe(3);
      expect(result.map((m: any) => m.id)).toEqual(['m1', 'm2', 'm3']);
    });
  });

  describe('isRoundRobinDraw', () => {
    it('returns true for ROUND_ROBIN', () => {
      expect(component.isRoundRobinDraw({ drawType: 'ROUND_ROBIN' } as any)).toBe(true);
    });

    it('returns false for other types', () => {
      expect(component.isRoundRobinDraw({ drawType: 'ELIMINATION' } as any)).toBe(false);
    });
  });

  describe('getDrawViewMode', () => {
    it('defaults to tree for elimination draw', () => {
      component.drawsInput = [{ id: 'd1', drawType: 'ELIMINATION', label: 'Main', matches: [] }] as any;
      expect(component.getDrawViewMode('d1')).toBe('tree');
    });

    it('defaults to list for round robin draw', () => {
      component.drawsInput = [{ id: 'd1', drawType: 'ROUND_ROBIN', label: 'Group', matches: [] }] as any;
      expect(component.getDrawViewMode('d1')).toBe('list');
    });

    it('defaults to tree for double elimination display item', () => {
      component.drawsInput = [
        { id: 'd1', drawType: 'ELIMINATION', label: 'Winners', matches: [] },
        { id: 'd2', drawType: 'DOUBLE_ELIMINATION', label: 'Losers', matches: [] }
      ] as any;
      expect(component.getDrawViewMode('de-d1')).toBe('tree');
    });

    it('returns stored mode after toggle', () => {
      component.drawsInput = [{ id: 'd1', drawType: 'ELIMINATION', label: 'Main', matches: [] }] as any;
      component.toggleDrawView('d1');
      expect(component.getDrawViewMode('d1')).toBe('list');
    });
  });

  describe('getDrawViewToggleLabel', () => {
    it('returns Ver listado when in tree mode', () => {
      component.drawsInput = [{ id: 'd1', drawType: 'ELIMINATION', label: 'Main', matches: [] }] as any;
      expect(component.getDrawViewToggleLabel('d1')).toBe('Ver listado');
    });

    it('returns Ver arbol when in list mode', () => {
      component.drawsInput = [{ id: 'd1', drawType: 'ROUND_ROBIN', label: 'Group', matches: [] }] as any;
      expect(component.getDrawViewToggleLabel('d1')).toBe('Ver arbol');
    });
  });

  describe('toggleDrawView', () => {
    it('toggles from tree to list', () => {
      component.drawsInput = [{ id: 'd1', drawType: 'ELIMINATION', label: 'Main', matches: [] }] as any;
      component.toggleDrawView('d1');
      expect(component.getDrawViewMode('d1')).toBe('list');
    });

    it('toggles back to tree', () => {
      component.drawsInput = [{ id: 'd1', drawType: 'ELIMINATION', label: 'Main', matches: [] }] as any;
      component.toggleDrawView('d1');
      component.toggleDrawView('d1');
      expect(component.getDrawViewMode('d1')).toBe('tree');
    });
  });

  describe('buildDisplayItems', () => {
    it('groups elimination + double elimination draws into one item', () => {
      component.drawsInput = [
        { id: 'd1', drawType: 'ELIMINATION', label: 'Winners', matches: [{ id: 'm1' }] },
        { id: 'd2', drawType: 'DOUBLE_ELIMINATION', label: 'Losers', matches: [{ id: 'm2' }] }
      ] as any;

      const items = component.displayItems();
      expect(items.length).toBe(1);
      expect(items[0].isDoubleElimination).toBe(true);
      expect(items[0].draws.length).toBe(2);
    });

    it('creates separate items for non-double-elimination draws', () => {
      component.drawsInput = [
        { id: 'd1', drawType: 'ROUND_ROBIN', label: 'Group A', matches: [] },
        { id: 'd2', drawType: 'ROUND_ROBIN', label: 'Group B', matches: [] }
      ] as any;

      const items = component.displayItems();
      expect(items.length).toBe(2);
      expect(items[0].isDoubleElimination).toBe(false);
    });
  });

  describe('onMatchSelected', () => {
    it('sets selectedMatch signal', () => {
      const match = { id: 'm1' } as MatchResponse;
      component.onMatchSelected(match);
      expect(component.selectedMatch()).toBe(match);
    });
  });

  describe('onModalClose', () => {
    it('clears selectedMatch signal', () => {
      component.selectedMatch.set({ id: 'm1' } as MatchResponse);
      component.onModalClose();
      expect(component.selectedMatch()).toBeNull();
    });
  });

  describe('onSaveMatchResult', () => {
    it('emits when canManageInput is true', () => {
      spyOn(component.matchResultSaved, 'emit');
      component.canManageInput = true;
      const event = { matchId: 'm1', winnerId: 'w1', result: '6-3', status: 'COMPLETED' as const };
      component.onSaveMatchResult(event);
      expect(component.matchResultSaved.emit).toHaveBeenCalledWith(event);
    });

    it('does not emit when canManageInput is false', () => {
      spyOn(component.matchResultSaved, 'emit');
      component.canManageInput = false;
      component.onSaveMatchResult({ matchId: 'm1', winnerId: 'w1', result: '6-3', status: 'COMPLETED' });
      expect(component.matchResultSaved.emit).not.toHaveBeenCalled();
    });

    it('clears selectedMatch after emitting', () => {
      spyOn(component.matchResultSaved, 'emit');
      component.canManageInput = true;
      component.selectedMatch.set({ id: 'm1' } as MatchResponse);
      component.onSaveMatchResult({ matchId: 'm1', winnerId: 'w1', result: '6-3', status: 'COMPLETED' });
      expect(component.selectedMatch()).toBeNull();
    });
  });

  describe('onSaveMatchSchedule', () => {
    it('emits when canManageInput is true', () => {
      spyOn(component.matchScheduleSaved, 'emit');
      component.canManageInput = true;
      const event = { matchId: 'm1', courtId: 'c1', scheduledAt: '2026-06-01T10:00', scheduleTimeType: 'EXACT' as const };
      component.onSaveMatchSchedule(event);
      expect(component.matchScheduleSaved.emit).toHaveBeenCalledWith(event);
    });

    it('does not emit when canManageInput is false', () => {
      spyOn(component.matchScheduleSaved, 'emit');
      component.canManageInput = false;
      component.onSaveMatchSchedule({ matchId: 'm1', courtId: 'c1', scheduledAt: '2026-06-01T10:00', scheduleTimeType: 'EXACT' });
      expect(component.matchScheduleSaved.emit).not.toHaveBeenCalled();
    });
  });

  describe('eventNameInput', () => {
    it('should have empty string default', () => {
      expect(component.eventNameInput).toBe('');
    });

    it('should accept event name input', () => {
      component.eventNameInput = 'Absoluto Masculino';
      expect(component.eventNameInput).toBe('Absoluto Masculino');
    });
  });
});
