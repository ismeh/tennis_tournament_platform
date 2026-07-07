import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { AppSettings } from '../../shared/constants';
import { RankingService } from './ranking.service';

describe('RankingService', () => {
  let service: RankingService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), RankingService]
    });

    service = TestBed.inject(RankingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should request tournament ranking with filters', () => {
    service.getTournamentRanking('tournament-id', { gender: 'FEMALE', categoryId: 7, page: 0, size: 10 }).subscribe(response => {
      expect(response.items[0].victories).toBe(3);
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/rankings/tournaments/tournament-id?gender=FEMALE&categoryId=7&page=0&size=10`);
    expect(request.request.method).toBe('GET');
    request.flush({
      items: [{ position: 1, participantId: 'participant-id', firstName: 'Jessica', victories: 3 }],
      page: 0,
      size: 10,
      totalItems: 1,
      totalPages: 1,
      sortBy: 'victories',
      sortDirection: 'desc'
    });
  });
});
