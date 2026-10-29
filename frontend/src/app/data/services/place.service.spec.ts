import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PlaceService, PlaceSuggestion } from './place.service';
import { AppSettings } from '../../shared/constants';

describe('PlaceService', () => {
  let service: PlaceService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PlaceService]
    });
    service = TestBed.inject(PlaceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should query place search with params', () => {
    const dummySuggestions: PlaceSuggestion[] = [
      {
        placeId: 'id1',
        name: 'Club de Tenis',
        formattedAddress: 'Calle de Tenis, 1',
        latitude: 40.0,
        longitude: -3.0,
        mapsUrl: 'http://maps'
      }
    ];

    service.search('Club', 40.0, -3.0).subscribe(suggestions => {
      expect(suggestions).toEqual(dummySuggestions);
    });

    const req = httpMock.expectOne(req => req.url === `${AppSettings.API_URL}/places/search`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('query')).toBe('Club');
    expect(req.request.params.get('lat')).toBe('40');
    expect(req.request.params.get('lng')).toBe('-3');

    req.flush(dummySuggestions);
  });
});
