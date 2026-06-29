import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { AppSettings } from '../../shared/constants';
import { PersonService } from './person.service';

describe('PersonService', () => {
  let service: PersonService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), PersonService]
    });

    service = TestBed.inject(PersonService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should search persons with query param', () => {
    service.searchPersons(' Carlos ').subscribe(response => {
      expect(response.length).toBe(1);
      expect(response[0].firstName).toBe('Carlos');
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/persons?query=Carlos`);
    expect(request.request.method).toBe('GET');

    request.flush([{ id: '1', firstName: 'Carlos', lastName: 'Alcaraz' }]);
  });

  it('should search persons without query when empty', () => {
    service.searchPersons('').subscribe(response => {
      expect(response.length).toBe(0);
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/persons`);
    expect(request.request.method).toBe('GET');

    request.flush([]);
  });

  it('should search persons without query when whitespace only', () => {
    service.searchPersons('   ').subscribe(response => {
      expect(response.length).toBe(0);
    });

    const request = httpMock.expectOne(`${AppSettings.API_URL}/persons`);
    expect(request.request.method).toBe('GET');

    request.flush([]);
  });
});
