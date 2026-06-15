import { Injectable } from '@angular/core';
import html2canvas from 'html2canvas';
import { jsPDF } from 'jspdf';

@Injectable({
  providedIn: 'root'
})
export class BracketExportService {

  async exportBracket(
    bracketElement: HTMLElement,
    tournamentName: string,
    categoryName: string,
    stageName: string
  ): Promise<void> {
    const scrollContainer = bracketElement.querySelector('.bracket-scroll') as HTMLElement | null;
    const zoomSurface = bracketElement.querySelector('.bracket-zoom-surface') as HTMLElement | null;
    const bracketBoard = bracketElement.querySelector('.bracket-board') as HTMLElement | null;

    if (!scrollContainer || !zoomSurface || !bracketBoard) {
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

      const canvas = await html2canvas(bracketBoard, {
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
      const pdf = new jsPDF({
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