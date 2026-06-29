import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { RankingComponent } from './ranking';
import { RankingService } from '../../data/services/ranking.service';
import { TournamentService } from '../../data/services/tournament.service';
import { ProfessionalRankingResponse, TournamentRankingResponse, RankingTournamentResponse } from '../../data/interfaces/ranking.model';
import { TournamentEventCatalogItem } from '../../data/interfaces/tournament.model';

describe('RankingComponent', () => {
  let component: RankingComponent;
  let rankingServiceSpy: jasmine.SpyObj<RankingService>;
  let tournamentServiceSpy: jasmine.SpyObj<TournamentService>;

  const mockProfessionalRows: ProfessionalRankingResponse[] = [
    { position: 1, playerId: 1, license: 'L001', fullName: 'Player One', firstName: 'Player', lastName: 'One', gender: 'MALE', category: 'A', clubName: 'Club A', birthDate: '2000-01-01', points: 100 },
    { position: 2, playerId: 2, license: null, fullName: 'Player Two', firstName: 'Player', lastName: 'Two', gender: 'FEMALE', category: 'B', clubName: null, birthDate: null, points: 50 },
  ];

  const mockTournamentRows: TournamentRankingResponse[] = [
    { position: 1, participantId: 'p1', license: 'L001', firstName: 'Player', lastName: 'One', gender: 'MALE', victories: 5 },
    { position: 2, participantId: 'p2', license: null, firstName: 'Player', lastName: null, gender: 'FEMALE', victories: 3 },
  ];

  const mockTournaments: RankingTournamentResponse[] = [
    { id: 't1', formalName: 'Tournament 1', playStartDate: '2026-01-01', playEndDate: '2026-01-10', inscriptionStartDate: '2025-12-01', inscriptionEndDate: '2025-12-31', surfaceCategory: 'CLAY', maxPlayers: 32, location: 'Madrid', status: 'OPEN' },
  ];

  const mockCategories: TournamentEventCatalogItem[] = [
    { id: 1, category: 'A', description: 'Category A', custom: false },
    { id: 2, category: 'B', description: 'Category B', custom: false },
  ];

  beforeEach(async () => {
    rankingServiceSpy = jasmine.createSpyObj('RankingService', [
      'getProfessionalRanking',
      'getTournamentRanking',
      'getRankingTournaments',
    ]);

    rankingServiceSpy.getProfessionalRanking.and.returnValue(of({
      items: mockProfessionalRows,
      page: 0,
      size: 10,
      totalItems: 2,
      totalPages: 1,
      sortBy: 'position',
      sortDirection: 'asc',
    }));

    rankingServiceSpy.getTournamentRanking.and.returnValue(of({
      items: mockTournamentRows,
      page: 0,
      size: 10,
      totalItems: 2,
      totalPages: 1,
      sortBy: 'position',
      sortDirection: 'asc',
    }));

    rankingServiceSpy.getRankingTournaments.and.returnValue(of(mockTournaments));

    tournamentServiceSpy = jasmine.createSpyObj('TournamentService', ['getEventCatalog']);
    tournamentServiceSpy.getEventCatalog.and.returnValue(of(mockCategories));

    await TestBed.configureTestingModule({
      imports: [RankingComponent],
      providers: [
        { provide: RankingService, useValue: rankingServiceSpy },
        { provide: TournamentService, useValue: tournamentServiceSpy },
        provideRouter([{ path: '**', component: RankingComponent }]),
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(RankingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('setMode', () => {
    it('should not change if same mode', () => {
      component.setMode('professionals');
      expect(component.mode()).toBe('professionals');
    });

    it('should switch to tournament mode', () => {
      component.setMode('tournament');
      expect(component.mode()).toBe('tournament');
    });

    it('should reset gender if switching from MIXED to professionals', () => {
      component.mode.set('tournament');
      component.selectedGender.set('MIXED');
      component.setMode('professionals');
      expect(component.selectedGender()).toBeNull();
    });
  });

  describe('applyFilters', () => {
    it('should load professional ranking', () => {
      component.mode.set('professionals');
      component.applyFilters();
      expect(rankingServiceSpy.getProfessionalRanking).toHaveBeenCalled();
    });

    it('should load tournament ranking', () => {
      component.mode.set('tournament');
      component.selectedTournamentId.set('t1');
      component.applyFilters();
      expect(rankingServiceSpy.getTournamentRanking).toHaveBeenCalled();
    });
  });

  describe('resetFilters', () => {
    it('should reset all filters', () => {
      component.selectedGender.set('MALE');
      component.selectedCategoryId.set('1');
      component.resetFilters();
      expect(component.selectedGender()).toBeNull();
      expect(component.selectedCategoryId()).toBe('');
    });
  });

  describe('onGenderChange', () => {
    it('should set gender', () => {
      component.onGenderChange('MALE');
      expect(component.selectedGender()).toBe('MALE');
    });

    it('should set null for empty', () => {
      component.onGenderChange('');
      expect(component.selectedGender()).toBeNull();
    });
  });

  describe('onCategoryChange', () => {
    it('should set category', () => {
      component.onCategoryChange('1');
      expect(component.selectedCategoryId()).toBe('1');
    });
  });

  describe('onTournamentChange', () => {
    it('should set tournament id', () => {
      component.onTournamentChange('t1');
      expect(component.selectedTournamentId()).toBe('t1');
    });
  });

  describe('onPageSizeChange', () => {
    it('should set page size', () => {
      rankingServiceSpy.getProfessionalRanking.and.returnValue(of({
        items: mockProfessionalRows,
        page: 0,
        size: 25,
        totalItems: 2,
        totalPages: 1,
        sortBy: 'position',
        sortDirection: 'asc',
      }));
      component.onPageSizeChange('25');
      expect(component.pageSize()).toBe(25);
    });

    it('should default to 10 for NaN', () => {
      rankingServiceSpy.getProfessionalRanking.and.returnValue(of({
        items: mockProfessionalRows,
        page: 0,
        size: 10,
        totalItems: 2,
        totalPages: 1,
        sortBy: 'position',
        sortDirection: 'asc',
      }));
      component.onPageSizeChange('abc');
      expect(component.pageSize()).toBe(10);
    });
  });

  describe('goToPreviousPage', () => {
    it('should not go before page 0', () => {
      component.page.set(0);
      component.goToPreviousPage();
      expect(component.page()).toBe(0);
    });

    it('should go to previous page', () => {
      component.page.set(2);
      rankingServiceSpy.getProfessionalRanking.and.returnValue(of({
        items: mockProfessionalRows,
        page: 1,
        size: 10,
        totalItems: 20,
        totalPages: 3,
        sortBy: 'position',
        sortDirection: 'asc',
      }));
      component.goToPreviousPage();
      expect(component.page()).toBe(1);
    });
  });

  describe('goToNextPage', () => {
    it('should not go past last page', () => {
      component.totalPages.set(1);
      component.page.set(0);
      component.goToNextPage();
      expect(component.page()).toBe(0);
    });

    it('should go to next page', () => {
      component.totalPages.set(3);
      component.page.set(0);
      rankingServiceSpy.getProfessionalRanking.and.returnValue(of({
        items: mockProfessionalRows,
        page: 1,
        size: 10,
        totalItems: 20,
        totalPages: 3,
        sortBy: 'position',
        sortDirection: 'asc',
      }));
      component.goToNextPage();
      expect(component.page()).toBe(1);
    });
  });

  describe('setSort', () => {
    it('should not sort by invalid field', () => {
      component.setSort('invalidField');
      expect(component.sortBy()).toBe('position');
    });

    it('should sort by new field', () => {
      component.setSort('name');
      expect(component.sortBy()).toBe('name');
    });

    it('should toggle direction for same field', () => {
      component.sortBy.set('name');
      component.sortDirection.set('asc');
      component.setSort('name');
      expect(component.sortDirection()).toBe('desc');
    });
  });

  describe('getSortIndicator', () => {
    it('should return empty for different field', () => {
      component.sortBy.set('position');
      expect(component.getSortIndicator('name')).toBe('');
    });

    it('should return up arrow for asc', () => {
      component.sortBy.set('position');
      component.sortDirection.set('asc');
      expect(component.getSortIndicator('position')).toBe('↑');
    });

    it('should return down arrow for desc', () => {
      component.sortBy.set('position');
      component.sortDirection.set('desc');
      expect(component.getSortIndicator('position')).toBe('↓');
    });
  });

  describe('getGenderLabel', () => {
    it('should return dash for null', () => {
      expect(component.getGenderLabel(null)).toBe('-');
    });

    it('should return label for MALE', () => {
      const label = component.getGenderLabel('MALE');
      expect(label).toBeTruthy();
    });
  });

  describe('getTournamentPlayerName', () => {
    it('should join first and last name', () => {
      const name = component.getTournamentPlayerName(mockTournamentRows[0]);
      expect(name).toBe('Player One');
    });

    it('should handle missing last name', () => {
      const name = component.getTournamentPlayerName(mockTournamentRows[1]);
      expect(name).toBe('Player');
    });

    it('should return default for empty names', () => {
      const name = component.getTournamentPlayerName({ firstName: '', lastName: '' } as any);
      expect(name).toBe('Jugador sin nombre');
    });
  });

  describe('formatNumber', () => {
    it('should format a number', () => {
      const formatted = component.formatNumber(1000);
      expect(formatted).not.toBe('-');
      expect(formatted).toContain('1');
    });

    it('should return dash for null', () => {
      expect(component.formatNumber(null)).toBe('-');
    });
  });

  describe('computed properties', () => {
    it('should return correct ranking title for professionals', () => {
      component.mode.set('professionals');
      expect(component.rankingTitle()).toBe('Ranking profesional');
    });

    it('should return correct ranking title for tournament', () => {
      component.mode.set('tournament');
      expect(component.rankingTitle()).toBe('Ranking del torneo');
    });

    it('should return row count based on mode', () => {
      component.mode.set('professionals');
      expect(component.rowCount()).toBe(2);
    });

    it('should return gender options based on mode', () => {
      component.mode.set('professionals');
      expect(component.genderOptions()).toEqual(['MALE', 'FEMALE']);
      component.mode.set('tournament');
      expect(component.genderOptions()).toEqual(['MALE', 'FEMALE', 'MIXED']);
    });

    it('should compute page range label', () => {
      component.totalItems.set(2);
      component.page.set(0);
      component.pageSize.set(10);
      expect(component.pageRangeLabel()).toBe('1-2');
    });

    it('should compute page range for empty results', () => {
      component.totalItems.set(0);
      expect(component.pageRangeLabel()).toBe('0-0');
    });

    it('should compute currentPageLabel', () => {
      component.totalItems.set(10);
      component.page.set(0);
      expect(component.currentPageLabel()).toBe(1);
    });

    it('should compute currentPageLabel for empty', () => {
      component.totalItems.set(0);
      expect(component.currentPageLabel()).toBe(0);
    });
  });
});
