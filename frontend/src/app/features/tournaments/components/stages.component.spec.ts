import { TestBed } from '@angular/core/testing';
import { StagesComponent } from './stages.component';
import { StageResponse } from '../../../data/interfaces/tournament.model';

describe('StagesComponent', () => {
  let component: StagesComponent;

  function createStage(overrides: Partial<StageResponse> = {}): StageResponse {
    return {
      id: 's1',
      eventId: 'e1',
      stageType: 'SINGLE_ELIMINATION',
      order: 1,
      strategyName: null,
      description: 'Phase 1',
      draws: [],
      ...overrides
    } as StageResponse;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [StagesComponent]
    });

    const fixture = TestBed.createComponent(StagesComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('getStageTypeLabel', () => {
    it('returns correct labels for known types', () => {
      expect(component.getStageTypeLabel('MAIN')).toBe('Cuadro Principal');
      expect(component.getStageTypeLabel('SINGLE_ELIMINATION')).toBe('Eliminatoria simple');
      expect(component.getStageTypeLabel('ROUND_ROBIN')).toBe('Liga');
      expect(component.getStageTypeLabel('DOUBLE_ELIMINATION')).toBe('Doble eliminación');
      expect(component.getStageTypeLabel('CONSOLATION')).toBe('Consolación');
    });

    it('returns raw value for unknown type', () => {
      expect(component.getStageTypeLabel('CUSTOM')).toBe('CUSTOM');
    });
  });

  describe('toggleStage', () => {
    it('expands a collapsed stage', () => {
      component.toggleStage('stage-1');
      expect(component.expandedStageId()).toBe('stage-1');
    });

    it('collapses an expanded stage', () => {
      component.toggleStage('stage-1');
      component.toggleStage('stage-1');
      expect(component.expandedStageId()).toBeNull();
    });

    it('switches to a different stage', () => {
      component.toggleStage('stage-1');
      component.toggleStage('stage-2');
      expect(component.expandedStageId()).toBe('stage-2');
    });
  });

  describe('sessionStorage persistence', () => {
    beforeEach(() => {
      if (typeof window !== 'undefined' && window.sessionStorage) {
        sessionStorage.clear();
      }
    });

    afterEach(() => {
      if (typeof window !== 'undefined' && window.sessionStorage) {
        sessionStorage.clear();
      }
    });

    it('saves expanded stage ID to sessionStorage on toggleStage', () => {
      component.tournamentIdInput = 't1';
      component.toggleStage('stage-1');
      expect(sessionStorage.getItem('tournament_stage_expanded_stage-1')).toBe('true');

      component.toggleStage('stage-1');
      expect(sessionStorage.getItem('tournament_stage_expanded_stage-1')).toBeNull();
    });

    it('restores expanded stage ID from sessionStorage when stages are input', () => {
      component.tournamentIdInput = 't1';
      sessionStorage.setItem('tournament_stage_expanded_stage-2', 'true');

      component.stagesInput = [
        createStage({ id: 'stage-1' }),
        createStage({ id: 'stage-2' })
      ];

      expect(component.expandedStageId()).toBe('stage-2');
    });
  });

  describe('getGenerateDrawsButtonLabel', () => {
    it('returns Generar cuadros when no draws and no feedback', () => {
      const stage = createStage({ draws: [] });
      expect(component.getGenerateDrawsButtonLabel(stage)).toBe('Generar cuadros');
    });

    it('returns Regenerar cuadros when draws exist', () => {
      const stage = createStage({ draws: [{ id: 'd1' }] } as any);
      expect(component.getGenerateDrawsButtonLabel(stage)).toBe('Regenerar cuadros');
    });

    it('returns Reintentar generar on error feedback', () => {
      component.drawGenerationFeedbackInput = { 's1': { status: 'error', message: 'Failed' } };
      const stage = createStage({ draws: [] });
      expect(component.getGenerateDrawsButtonLabel(stage)).toBe('Reintentar generar');
    });

    it('returns Generando cuadros... when generating', () => {
      component.generatingDrawsForStageIdInput = 's1';
      const stage = createStage({ draws: [] });
      expect(component.getGenerateDrawsButtonLabel(stage)).toBe('Generando cuadros...');
    });
  });

  describe('getGenerateDrawsButtonClass', () => {
    it('returns primary class when no draws and no feedback', () => {
      const stage = createStage({ draws: [] });
      const cls = component.getGenerateDrawsButtonClass(stage);
      expect(cls).toContain('bg-primary-600');
    });

    it('returns green class when draws exist', () => {
      const stage = createStage({ draws: [{ id: 'd1' }] } as any);
      const cls = component.getGenerateDrawsButtonClass(stage);
      expect(cls).toContain('bg-green-700');
    });

    it('returns red class on error feedback', () => {
      component.drawGenerationFeedbackInput = { 's1': { status: 'error', message: 'Failed' } };
      const stage = createStage({ draws: [] });
      const cls = component.getGenerateDrawsButtonClass(stage);
      expect(cls).toContain('bg-red-600');
    });
  });

  describe('isGeneratingDraws', () => {
    it('returns true for the generating stage', () => {
      component.generatingDrawsForStageIdInput = 's1';
      expect(component.isGeneratingDraws('s1')).toBe(true);
      expect(component.isGeneratingDraws('s2')).toBe(false);
    });
  });

  describe('onGenerateDraws', () => {
    it('emits generateDraws event when canManage is true', () => {
      spyOn(component.generateDraws, 'emit');
      component.canManageInput = true;
      component.tournamentIdInput = 't1';
      const stage = createStage({ id: 's1' });
      component.onGenerateDraws(stage);
      expect(component.generateDraws.emit).toHaveBeenCalledWith({ tournamentId: 't1', stageId: 's1' });
    });

    it('does not emit when canManage is false', () => {
      spyOn(component.generateDraws, 'emit');
      component.canManageInput = false;
      const stage = createStage({ id: 's1' });
      component.onGenerateDraws(stage);
      expect(component.generateDraws.emit).not.toHaveBeenCalled();
    });
  });

  describe('onMatchResultSaved', () => {
    it('emits when canManage is true', () => {
      spyOn(component.matchResultSaved, 'emit');
      component.canManageInput = true;
      const event = { matchId: 'm1', winnerId: 'w1', result: '6-3', status: 'COMPLETED' as const };
      component.onMatchResultSaved(event);
      expect(component.matchResultSaved.emit).toHaveBeenCalledWith(event);
    });

    it('does not emit when canManage is false', () => {
      spyOn(component.matchResultSaved, 'emit');
      component.canManageInput = false;
      component.onMatchResultSaved({ matchId: 'm1', winnerId: 'w1', result: '6-3', status: 'COMPLETED' });
      expect(component.matchResultSaved.emit).not.toHaveBeenCalled();
    });
  });

  describe('onMatchScheduleSaved', () => {
    it('emits when canManage is true', () => {
      spyOn(component.matchScheduleSaved, 'emit');
      component.canManageInput = true;
      const event = { matchId: 'm1', courtId: 'c1', scheduledAt: '2026-06-01T10:00', scheduleTimeType: 'EXACT' as const };
      component.onMatchScheduleSaved(event);
      expect(component.matchScheduleSaved.emit).toHaveBeenCalledWith(event);
    });

    it('does not emit when canManage is false', () => {
      spyOn(component.matchScheduleSaved, 'emit');
      component.canManageInput = false;
      component.onMatchScheduleSaved({ matchId: 'm1', courtId: 'c1', scheduledAt: '2026-06-01T10:00', scheduleTimeType: 'EXACT' });
      expect(component.matchScheduleSaved.emit).not.toHaveBeenCalled();
    });
  });

  describe('onMatchSelected', () => {
    it('emits matchSelected event', () => {
      spyOn(component.matchSelected, 'emit');
      component.onMatchSelected('m1');
      expect(component.matchSelected.emit).toHaveBeenCalledWith('m1');
    });
  });
});
