import { TestBed } from '@angular/core/testing';
import { BracketExportService } from './bracket-export.service';
import { DrawResponse } from '../../../data/interfaces/tournament.model';

describe('BracketExportService', () => {
  let service: BracketExportService;
  let saveSpy: jasmine.Spy;
  let textSpy: jasmine.Spy;
  let html2canvasSpy: jasmine.Spy;

  function createMockPdf(): any {
    const mock: any = {
      save: jasmine.createSpy('save'),
      text: jasmine.createSpy('text'),
      addImage: jasmine.createSpy('addImage'),
      setFont: jasmine.createSpy('setFont'),
      setFontSize: jasmine.createSpy('setFontSize'),
      internal: {
        pageSize: {
          getWidth: jasmine.createSpy('getWidth').and.returnValue(297),
          getHeight: jasmine.createSpy('getHeight').and.returnValue(210),
        },
      },
    };
    mock.text.and.returnValue(mock);
    mock.setFont.and.returnValue(mock);
    mock.setFontSize.and.returnValue(mock);
    mock.addImage.and.returnValue(mock);
    return mock;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [BracketExportService] });
    service = TestBed.inject(BracketExportService);

    const mockPdf = createMockPdf();
    saveSpy = mockPdf.save;
    textSpy = mockPdf.text;

    (service as any).jsPdfClass = function () { return mockPdf; };

    const mockCanvas = document.createElement('canvas');
    mockCanvas.width = 800;
    mockCanvas.height = 600;
    (mockCanvas as any).toDataURL = jasmine.createSpy('toDataURL').and.returnValue('data:image/png;base64,mock');
    html2canvasSpy = spyOn(service as any, 'html2canvasFn').and.returnValue(Promise.resolve(mockCanvas));
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('exportBracket', () => {
    function buildShell(): HTMLElement {
      const shell = document.createElement('div');
      shell.className = 'bracket-shell';
      const scroll = document.createElement('div');
      scroll.className = 'bracket-scroll';
      const zoom = document.createElement('div');
      zoom.className = 'bracket-zoom-surface';
      const board = document.createElement('div');
      board.className = 'bracket-board';
      zoom.appendChild(board);
      scroll.appendChild(zoom);
      shell.appendChild(scroll);
      return shell;
    }

    function buildMultiShell(): HTMLElement {
      const shell = document.createElement('div');
      shell.className = 'bracket-shell';
      for (let i = 0; i < 2; i++) {
        const scroll = document.createElement('div');
        scroll.className = 'bracket-scroll';
        const zoom = document.createElement('div');
        zoom.className = 'bracket-zoom-surface';
        const board = document.createElement('div');
        board.className = 'bracket-board';
        zoom.appendChild(board);
        scroll.appendChild(zoom);
        shell.appendChild(scroll);
      }
      return shell;
    }

    it('throws when no bracket boards found', async () => {
      const el = document.createElement('div');
      await expectAsync(service.exportBracket(el as HTMLElement, 'T', 'C', 'S'))
        .toBeRejectedWithError('Bracket DOM structure not found');
    });

    it('exports single board', async () => {
      const shell = buildShell();
      await service.exportBracket(shell, 'Open de Primavera', 'Masculino A', 'Fase Final');
      expect(html2canvasSpy).toHaveBeenCalled();
      expect(saveSpy).toHaveBeenCalledWith('open_de_primavera_masculino_a_fase_final.pdf');
    });

    it('exports multiple boards', async () => {
      const shell = buildMultiShell();
      await service.exportBracket(shell, 'Torneo Especial', 'Femenino B', 'Cuadro Principal');
      expect(html2canvasSpy).toHaveBeenCalled();
      expect(saveSpy).toHaveBeenCalledWith('torneo_especial_femenino_b_cuadro_principal.pdf');
    });
  });

  describe('exportRoundRobinTable', () => {
    it('exports with winners', async () => {
      const draw = {
        id: '1', label: 'RR A', stageId: '1',
        drawType: 'ROUND_ROBIN',
        matches: [
          { id: '1', firstInscriptionId: 'p1', secondInscriptionId: 'p2', winnerId: 'p1', roundNumber: 1 },
          { id: '2', firstInscriptionId: 'p3', secondInscriptionId: 'p4', winnerId: 'p3', roundNumber: 1 },
          { id: '3', firstInscriptionId: 'p1', secondInscriptionId: 'p3', winnerId: null, roundNumber: 2 },
        ] as any,
      } as DrawResponse;
      await service.exportRoundRobinTable(draw, { 'p1': 'Player 1', 'p2': 'Player 2', 'p3': 'Player 3', 'p4': 'Player 4' }, 'Torneo Otono', 'Mixto');
      expect(textSpy).toHaveBeenCalled();
      expect(saveSpy).toHaveBeenCalledWith('torneo_otono_mixto_rr_a.pdf');
    });

    it('handles empty matches', async () => {
      const draw = { id: '1', label: 'Vacio', stageId: '1', drawType: 'ROUND_ROBIN', matches: [] } as DrawResponse;
      await service.exportRoundRobinTable(draw, {}, 'Torneo Otono', 'Mixto');
      expect(textSpy).toHaveBeenCalled();
      expect(saveSpy).toHaveBeenCalledWith('torneo_otono_mixto_vacio.pdf');
    });

    it('handles unknown player IDs', async () => {
      const draw = {
        id: '1', label: 'RR', stageId: '1', drawType: 'ROUND_ROBIN',
        matches: [{ id: '1', firstInscriptionId: 'p1', secondInscriptionId: 'p2', winnerId: 'p1', roundNumber: 1 }] as any,
      } as DrawResponse;
      await service.exportRoundRobinTable(draw, {}, 'Torneo', 'Categoria');
      expect(saveSpy).toHaveBeenCalledWith('torneo_categoria_rr.pdf');
    });

    it('sorts by wins and losses', async () => {
      const draw = {
        id: '1', label: 'RR', stageId: '1', drawType: 'ROUND_ROBIN',
        matches: [
          { id: '1', firstInscriptionId: 'p1', secondInscriptionId: 'p2', winnerId: 'p1', roundNumber: 1 },
          { id: '2', firstInscriptionId: 'p1', secondInscriptionId: 'p3', winnerId: 'p1', roundNumber: 1 },
          { id: '3', firstInscriptionId: 'p2', secondInscriptionId: 'p3', winnerId: 'p2', roundNumber: 1 },
        ] as any,
      } as DrawResponse;
      await service.exportRoundRobinTable(draw, { 'p1': 'P1', 'p2': 'P2', 'p3': 'P3' }, 'Torneo', 'Categoria');
      expect(saveSpy).toHaveBeenCalledWith('torneo_categoria_rr.pdf');
    });
  });
});
