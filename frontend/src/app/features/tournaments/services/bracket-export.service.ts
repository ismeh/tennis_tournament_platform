import { Injectable } from '@angular/core';
import html2canvas from 'html2canvas';
import { jsPDF } from 'jspdf';
import { DrawResponse, MatchResponse } from '../../../data/interfaces/tournament.model';

@Injectable({
  providedIn: 'root'
})
export class BracketExportService {
  html2canvasFn = html2canvas;
  jsPdfClass = jsPDF;

  async exportBracket(
    bracketElement: HTMLElement,
    tournamentName: string,
    categoryName: string,
    stageName: string
  ): Promise<void> {
    const bracketBoards = bracketElement.querySelectorAll('.bracket-board');

    if (bracketBoards.length === 0) {
      throw new Error('Bracket DOM structure not found');
    }

    if (bracketBoards.length === 1) {
      await this.exportSingleBoard(bracketBoards[0] as HTMLElement, tournamentName, categoryName, stageName);
      return;
    }

    await this.exportMultipleBoards(bracketElement, bracketBoards as NodeListOf<HTMLElement>, tournamentName, categoryName, stageName);
  }

  private async exportMultipleBoards(
    bracketElement: HTMLElement,
    boardList: NodeListOf<HTMLElement>,
    tournamentName: string,
    categoryName: string,
    stageName: string
  ): Promise<void> {
    const scrollContainers = bracketElement.querySelectorAll('.bracket-scroll') as NodeListOf<HTMLElement>;
    const zoomSurfaces = bracketElement.querySelectorAll('.bracket-zoom-surface') as NodeListOf<HTMLElement>;

    const saved: Array<{
      scrollOverflow: string;
      scrollPadding: string;
      scrollWidth: string;
      scrollHeight: string;
      surfaceWidth: string;
      surfaceHeight: string;
      boardTransform: string;
      boardMinWidth: string;
    }> = [];

    try {
      for (let i = 0; i < boardList.length; i++) {
        const scrollContainer = scrollContainers[i];
        const zoomSurface = zoomSurfaces[i];
        const board = boardList[i];

        saved.push({
          scrollOverflow: scrollContainer.style.overflow,
          scrollPadding: scrollContainer.style.padding,
          scrollWidth: scrollContainer.style.width,
          scrollHeight: scrollContainer.style.height,
          surfaceWidth: zoomSurface.style.width,
          surfaceHeight: zoomSurface.style.height,
          boardTransform: board.style.transform,
          boardMinWidth: board.style.minWidth,
        });

        scrollContainer.style.overflow = 'visible';
        scrollContainer.style.padding = '0';
        scrollContainer.style.width = 'max-content';
        scrollContainer.style.height = 'auto';
        zoomSurface.style.width = 'max-content';
        zoomSurface.style.height = 'auto';
        board.style.transform = 'none';
        board.style.minWidth = '0';
      }

      await new Promise(r => setTimeout(r, 200));

      let totalHeight = 0;
      let maxWidth = 0;
      const boardSizes: Array<{ width: number; height: number }> = [];

      for (let i = 0; i < boardList.length; i++) {
        const board = boardList[i];
        const width = board.scrollWidth + 80;
        const height = board.scrollHeight + 40;
        boardSizes.push({ width, height });
        totalHeight += height;
        maxWidth = Math.max(maxWidth, width);
      }

      const canvas = document.createElement('canvas');
      const scale = 2;
      canvas.width = maxWidth * scale;
      canvas.height = totalHeight * scale;
      const ctx = canvas.getContext('2d');
      if (!ctx) throw new Error('Could not create canvas');

      ctx.fillStyle = '#ffffff';
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      let yOffset = 0;
      for (let i = 0; i < boardList.length; i++) {
        const board = boardList[i];
        const { width, height } = boardSizes[i];

        const boardCanvas = await this.html2canvasFn(board, {
          scale,
          useCORS: true,
          allowTaint: true,
          backgroundColor: '#ffffff',
          logging: false,
          width,
          height,
          windowWidth: width,
          windowHeight: height,
        });

        ctx.drawImage(boardCanvas, 0, yOffset * scale, canvas.width, boardCanvas.height);
        yOffset += height;

        if (i < boardList.length - 1) {
          ctx.save();
          ctx.strokeStyle = '#d97706';
          ctx.lineWidth = 2;
          ctx.setLineDash([6, 4]);
          ctx.beginPath();
          ctx.moveTo(40, yOffset * scale - 20);
          ctx.lineTo(canvas.width - 40, yOffset * scale - 20);
          ctx.stroke();
          ctx.restore();

          ctx.fillStyle = '#92400e';
          ctx.font = `bold ${14 * scale}px sans-serif`;
          ctx.textAlign = 'center';
          ctx.fillText('--- Gran Final ---', canvas.width / 2, yOffset * scale - 10);
          yOffset += 30;
        }
      }

      const imgWidth = canvas.width;
      const imgHeight = canvas.height;
      const { widthMm, heightMm } = this.calculatePdfSize(imgWidth, imgHeight);
      const orientation = widthMm > heightMm ? 'l' : 'p';
      const pdf = new this.jsPdfClass({ orientation, unit: 'mm', format: [widthMm, heightMm] });

      const imgData = canvas.toDataURL('image/png');
      pdf.addImage(imgData, 'PNG', 0, 0, widthMm, heightMm);

      const filename = this.buildFilename(tournamentName, categoryName, stageName);
      pdf.save(filename);
    } finally {
      for (let i = 0; i < boardList.length; i++) {
        if (saved[i]) {
          const s = saved[i];
          (scrollContainers[i]).style.overflow = s.scrollOverflow;
          (scrollContainers[i]).style.padding = s.scrollPadding;
          (scrollContainers[i]).style.width = s.scrollWidth;
          (scrollContainers[i]).style.height = s.scrollHeight;
          (zoomSurfaces[i]).style.width = s.surfaceWidth;
          (zoomSurfaces[i]).style.height = s.surfaceHeight;
          boardList[i].style.transform = s.boardTransform;
          boardList[i].style.minWidth = s.boardMinWidth;
        }
      }
    }
  }

  private async exportSingleBoard(
    bracketBoard: HTMLElement,
    tournamentName: string,
    categoryName: string,
    stageName: string
  ): Promise<void> {
    const scrollContainer = bracketBoard.closest('.bracket-shell')?.querySelector('.bracket-scroll') as HTMLElement | null;
    const zoomSurface = scrollContainer?.querySelector('.bracket-zoom-surface') as HTMLElement | null;

    if (!scrollContainer || !zoomSurface) {
      throw new Error('Bracket DOM structure not found');
    }

    const saved = {
      scrollOverflow: scrollContainer.style.overflow,
      scrollPadding: scrollContainer.style.padding,
      scrollWidth: scrollContainer.style.width,
      scrollHeight: scrollContainer.style.height,
      surfaceWidth: zoomSurface.style.width,
      surfaceHeight: zoomSurface.style.height,
      boardTransform: bracketBoard.style.transform,
      boardMinWidth: bracketBoard.style.minWidth,
    };

    try {
      scrollContainer.style.overflow = 'visible';
      scrollContainer.style.padding = '0';
      scrollContainer.style.width = 'max-content';
      scrollContainer.style.height = 'auto';
      zoomSurface.style.width = 'max-content';
      zoomSurface.style.height = 'auto';
      bracketBoard.style.transform = 'none';
      bracketBoard.style.minWidth = '0';

      await new Promise(r => setTimeout(r, 150));

      const contentWidth = bracketBoard.scrollWidth + 80;
      const contentHeight = bracketBoard.scrollHeight + 80;

      const canvas = await this.html2canvasFn(bracketBoard, {
        scale: 2,
        useCORS: true,
        allowTaint: true,
        backgroundColor: '#ffffff',
        logging: false,
        width: contentWidth,
        height: contentHeight,
        windowWidth: contentWidth,
        windowHeight: contentHeight,
      });

      const imgWidth = canvas.width;
      const imgHeight = canvas.height;

      const { widthMm, heightMm } = this.calculatePdfSize(imgWidth, imgHeight);

      const orientation = widthMm > heightMm ? 'l' : 'p';
      const pdf = new this.jsPdfClass({
        orientation,
        unit: 'mm',
        format: [widthMm, heightMm]
      });

      const imgData = canvas.toDataURL('image/png');
      pdf.addImage(imgData, 'PNG', 0, 0, widthMm, heightMm);

      const filename = this.buildFilename(tournamentName, categoryName, stageName);
      pdf.save(filename);
    } finally {
      scrollContainer.style.overflow = saved.scrollOverflow;
      scrollContainer.style.padding = saved.scrollPadding;
      scrollContainer.style.width = saved.scrollWidth;
      scrollContainer.style.height = saved.scrollHeight;
      zoomSurface.style.width = saved.surfaceWidth;
      zoomSurface.style.height = saved.surfaceHeight;
      bracketBoard.style.transform = saved.boardTransform;
      bracketBoard.style.minWidth = saved.boardMinWidth;
    }
  }

  async exportRoundRobinTable(
    draw: DrawResponse,
    participantNames: Record<string, string>,
    tournamentName: string,
    categoryName: string
  ): Promise<void> {
    const pdf = new this.jsPdfClass({ orientation: 'landscape', unit: 'mm', format: 'a4' });

    const pageWidth = pdf.internal.pageSize.getWidth();
    const pageHeight = pdf.internal.pageSize.getHeight();
    const margin = 15;
    const usableWidth = pageWidth - 2 * margin;

    pdf.setFontSize(14);
    pdf.setFont('helvetica', 'bold');
    pdf.text(`${tournamentName} - ${categoryName}`, margin, margin);

    pdf.setFontSize(11);
    pdf.setFont('helvetica', 'normal');
    pdf.text(`Cuadro: ${draw.label}`, margin, margin + 8);

    const matches = draw.matches || [];
    const playerIds = new Set<string>();
    matches.forEach(m => {
      if (m.firstInscriptionId) playerIds.add(m.firstInscriptionId);
      if (m.secondInscriptionId) playerIds.add(m.secondInscriptionId);
    });

    const players = Array.from(playerIds).map(id => ({
      id,
      name: participantNames[id] ?? id.substring(0, 8)
    }));

    const standings = new Map<string, { wins: number; losses: number }>();
    players.forEach(p => standings.set(p.id, { wins: 0, losses: 0 }));

    matches.forEach(m => {
      if (m.winnerId && m.firstInscriptionId && m.secondInscriptionId) {
        const winner = m.winnerId;
        const loser = winner === m.firstInscriptionId ? m.secondInscriptionId : m.firstInscriptionId;
        const w = standings.get(winner);
        const l = standings.get(loser);
        if (w) w.wins++;
        if (l) l.losses++;
      }
    });

    const sortedPlayers = [...players].sort((a, b) => {
      const sa = standings.get(a.id) ?? { wins: 0, losses: 0 };
      const sb = standings.get(b.id) ?? { wins: 0, losses: 0 };
      return sb.wins - sa.wins || sa.losses - sb.losses;
    });

    const colWidth = usableWidth / (players.length + 1);
    const rowHeight = 7;
    let y = margin + 18;

    pdf.setFontSize(9);
    pdf.setFont('helvetica', 'bold');

    pdf.text('Jugador', margin, y);
    sortedPlayers.forEach((p, i) => {
      pdf.text(p.name, margin + colWidth * (i + 1), y);
    });
    y += rowHeight;

    pdf.setFont('helvetica', 'normal');
    sortedPlayers.forEach((rowPlayer, ri) => {
      pdf.text(rowPlayer.name, margin, y);
      sortedPlayers.forEach((colPlayer, ci) => {
        if (ri === ci) {
          pdf.text('-', margin + colWidth * (ci + 1), y);
        } else {
          const match = matches.find(m =>
            (m.firstInscriptionId === rowPlayer.id && m.secondInscriptionId === colPlayer.id) ||
            (m.firstInscriptionId === colPlayer.id && m.secondInscriptionId === rowPlayer.id)
          );
          if (match && match.winnerId) {
            pdf.text(match.winnerId === rowPlayer.id ? '1-0' : '0-1', margin + colWidth * (ci + 1), y);
          } else {
            pdf.text('-', margin + colWidth * (ci + 1), y);
          }
        }
      });
      y += rowHeight;
    });

    y += 5;
    pdf.setFont('helvetica', 'bold');
    pdf.text('Tabla de posiciones:', margin, y);
    y += rowHeight;
    pdf.setFont('helvetica', 'normal');
    sortedPlayers.forEach((p, i) => {
      const s = standings.get(p.id) ?? { wins: 0, losses: 0 };
      pdf.text(`${i + 1}. ${p.name} - ${s.wins}V / ${s.losses}D`, margin, y);
      y += rowHeight;
    });

    const filename = this.buildFilename(tournamentName, categoryName, draw.label);
    pdf.save(filename);
  }

  private calculatePdfSize(imgWidthPx: number, imgHeightPx: number): { widthMm: number; heightMm: number } {
    const PX_TO_MM = 0.264583;
    let widthMm = Math.round(imgWidthPx * PX_TO_MM);
    let heightMm = Math.round(imgHeightPx * PX_TO_MM);

    const MAX_WIDTH_MM = 1500;

    if (widthMm > MAX_WIDTH_MM) {
      const ratio = MAX_WIDTH_MM / widthMm;
      widthMm = MAX_WIDTH_MM;
      heightMm = Math.round(heightMm * ratio);
    }

    return { widthMm, heightMm };
  }

  private buildFilename(tournamentName: string, categoryName: string, stageName: string): string {
    const sanitize = (str: string) =>
      str
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[^a-zA-Z0-9_\- ]/g, '')
        .replace(/\s+/g, '_')
        .toLowerCase();

    const parts = [tournamentName, categoryName, stageName]
      .filter(Boolean)
      .map(sanitize);

    return (parts.length > 0 ? parts.join('_') : 'cuadro') + '.pdf';
  }
}