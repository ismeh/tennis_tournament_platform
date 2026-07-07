import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AppSettings } from '../../shared/constants';
import { ClubService } from './club.service';
import { ClubResponse } from '../interfaces/club.model';

describe('ClubService', () => {
  let service: ClubService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ClubService
      ]
    });

    service = TestBed.inject(ClubService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should search clubs via GET with query parameter', () => {
    const mockClubs: ClubResponse[] = [
      { id: '1', name: 'Real Club de Tenis' },
      { id: '2', name: 'Club Tenis Chamartín' }
    ];

    service.searchClubs('Tenis').subscribe(res => {
      expect(res).toEqual(mockClubs);
    });

    const req = httpMock.expectOne(`${AppSettings.API_URL}/clubs?q=Tenis`);
    expect(req.request.method).toBe('GET');
    req.flush(mockClubs);
  });
});
