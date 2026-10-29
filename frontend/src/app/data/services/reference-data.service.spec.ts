import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AppSettings } from '../../shared/constants';
import { ReferenceDataService } from './reference-data.service';

describe('ReferenceDataService', () => {
  let service: ReferenceDataService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), ReferenceDataService]
    });

    service = TestBed.inject(ReferenceDataService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load nationalities from refs endpoint', () => {
    service.getNationalities().subscribe(response => {
      expect(response).toEqual([
        {
          code: 'ESP',
          name: 'España'
        }
      ]);
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/refs/nationalities`);
    expect(request.request.method).toBe('GET');
    request.flush([
      {
        code: 'ESP',
        name: 'España'
      }
    ]);
  });
});
