import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { AppSettings } from '../../shared/constants';
import { ProPlayerService } from './pro-player.service';

describe('ProPlayerService', () => {
  let service: ProPlayerService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), ProPlayerService]
    });

    service = TestBed.inject(ProPlayerService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should search professional players through the backend endpoint', () => {
    service.searchProPlayers(' Carlos ').subscribe(response => {
      expect(response.length).toBe(1);
      expect(response[0].id).toBe(42);
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/pro-players?query=Carlos`);
    expect(request.request.method).toBe('GET');

    request.flush([
      {
        id: 42,
        license: 'RFET-42',
        fullName: 'ALCARAZ, CARLOS',
        firstName: 'CARLOS',
        lastName: 'ALCARAZ',
        rankingPosition: 1,
        ageCategory: 'Absoluta',
        clubName: 'Club Central',
        birthDate: '2003-05-05',
        gender: 'MALE'
      }
    ]);
  });
});
