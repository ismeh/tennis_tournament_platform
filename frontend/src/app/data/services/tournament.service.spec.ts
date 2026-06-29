import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TournamentService } from './tournament.service';
import { AppSettings } from '../../shared/constants';
import { TournamentCalendarFilters } from '../interfaces/tournament.model';

describe('TournamentService', () => {
  let service: TournamentService;
  let httpMock: HttpTestingController;
  const originalApiUrl = AppSettings.API_URL;
  let apiUrl: string;

  beforeEach(() => {
    apiUrl = AppSettings.API_URL;
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TournamentService],
    });
    service = TestBed.inject(TournamentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    AppSettings.API_URL = originalApiUrl;
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getTournaments', () => {
    it('should GET tournaments', () => {
      service.getTournaments().subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  describe('getUmpireTournaments', () => {
    it('should GET umpire tournaments', () => {
      service.getUmpireTournaments().subscribe();
      const req = httpMock.expectOne(`${apiUrl}/umpires/me/tournaments`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  describe('getPublishedTournamentCalendar', () => {
    it('should GET calendar with no filters', () => {
      service.getPublishedTournamentCalendar().subscribe();
      const req = httpMock.expectOne(`${apiUrl}/calendar/tournaments`);
      expect(req.request.method).toBe('GET');
      req.flush({ content: [], totalElements: 0, totalPages: 0 });
    });

    it('should GET calendar with all filters', () => {
      const filters: TournamentCalendarFilters = {
        from: '2026-01-01',
        to: '2026-12-31',
        surface: 'CLAY',
        location: 'Madrid',
        name: 'Open',
        professionalTournament: true,
        status: 'OPEN',
        page: 0,
        size: 10,
      };
      service.getPublishedTournamentCalendar(filters).subscribe();
      const req = httpMock.expectOne(r =>
        r.url === `${apiUrl}/calendar/tournaments` &&
        r.params.get('from') === '2026-01-01' &&
        r.params.get('to') === '2026-12-31' &&
        r.params.get('surface') === 'CLAY' &&
        r.params.get('location') === 'Madrid' &&
        r.params.get('name') === 'Open' &&
        r.params.get('professionalTournament') === 'true' &&
        r.params.get('status') === 'OPEN' &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '10'
      );
      expect(req.request.method).toBe('GET');
      req.flush({ content: [], totalElements: 0, totalPages: 0 });
    });

    it('should handle location with whitespace only', () => {
      service.getPublishedTournamentCalendar({ location: '   ' }).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/calendar/tournaments`);
      expect(req.request.params.has('location')).toBeFalse();
      req.flush({ content: [], totalElements: 0, totalPages: 0 });
    });

    it('should handle name with whitespace only', () => {
      service.getPublishedTournamentCalendar({ name: '   ' }).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/calendar/tournaments`);
      expect(req.request.params.has('name')).toBeFalse();
      req.flush({ content: [], totalElements: 0, totalPages: 0 });
    });

    it('should handle professionalTournament as false', () => {
      service.getPublishedTournamentCalendar({ professionalTournament: false }).subscribe();
      const req = httpMock.expectOne(r =>
        r.url === `${apiUrl}/calendar/tournaments` &&
        r.params.get('professionalTournament') === 'false'
      );
      req.flush({ content: [], totalElements: 0, totalPages: 0 });
    });

    it('should handle professionalTournament as null', () => {
      service.getPublishedTournamentCalendar({ professionalTournament: null }).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/calendar/tournaments`);
      expect(req.request.params.has('professionalTournament')).toBeFalse();
      req.flush({ content: [], totalElements: 0, totalPages: 0 });
    });
  });

  describe('getMyMatchCalendar', () => {
    it('should GET my matches', () => {
      service.getMyMatchCalendar().subscribe();
      const req = httpMock.expectOne(`${apiUrl}/calendar/my-matches`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  describe('getMyTournamentCalendar', () => {
    it('should GET my tournaments', () => {
      service.getMyTournamentCalendar().subscribe();
      const req = httpMock.expectOne(`${apiUrl}/calendar/my-tournaments`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  describe('getTournamentById', () => {
    it('should GET tournament by id', () => {
      service.getTournamentById('t1').subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1`);
      expect(req.request.method).toBe('GET');
      req.flush({});
    });
  });

  describe('createTournament', () => {
    it('should POST tournament', () => {
      service.createTournament({} as any).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments`);
      expect(req.request.method).toBe('POST');
      req.flush({});
    });
  });

  describe('getCourts', () => {
    it('should GET courts', () => {
      service.getCourts('t1').subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/courts`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  describe('createCourt', () => {
    it('should POST court', () => {
      service.createCourt('t1', {} as any).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/courts`);
      expect(req.request.method).toBe('POST');
      req.flush({});
    });
  });

  describe('updateCourt', () => {
    it('should PATCH court', () => {
      service.updateCourt('t1', 'c1', {} as any).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/courts/c1`);
      expect(req.request.method).toBe('PATCH');
      req.flush({});
    });
  });

  describe('deleteCourt', () => {
    it('should DELETE court', () => {
      service.deleteCourt('t1', 'c1').subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/courts/c1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('getEventCatalog', () => {
    it('should GET event catalog', () => {
      service.getEventCatalog().subscribe();
      const req = httpMock.expectOne(`${apiUrl}/age-categories`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  describe('getEventCatalogAll', () => {
    it('should GET all event catalog', () => {
      service.getEventCatalogAll().subscribe();
      const req = httpMock.expectOne(`${apiUrl}/age-categories/all`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  describe('saveTournamentEvents', () => {
    it('should POST events', () => {
      service.saveTournamentEvents('t1', {} as any).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/events`);
      expect(req.request.method).toBe('POST');
      req.flush({});
    });
  });

  describe('updateTournamentStatus', () => {
    it('should PATCH status', () => {
      service.updateTournamentStatus('t1', {} as any).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/status`);
      expect(req.request.method).toBe('PATCH');
      req.flush({});
    });
  });

  describe('updateTournamentGeneralInfo', () => {
    it('should PATCH general info', () => {
      service.updateTournamentGeneralInfo('t1', {} as any).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/general-info`);
      expect(req.request.method).toBe('PATCH');
      req.flush({});
    });
  });

  describe('requestInscription', () => {
    it('should POST inscription', () => {
      service.requestInscription('t1', 'e1', {} as any).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/events/e1/inscriptions`);
      expect(req.request.method).toBe('POST');
      req.flush({});
    });
  });

  describe('addManualInscription', () => {
    it('should POST manual inscription', () => {
      service.addManualInscription('t1', 'e1', {} as any).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/events/e1/manual-inscriptions`);
      expect(req.request.method).toBe('POST');
      req.flush({});
    });
  });

  describe('getTournamentInscriptions', () => {
    it('should GET inscriptions without eventId', () => {
      service.getTournamentInscriptions('t1').subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/inscriptions`);
      expect(req.request.method).toBe('GET');
      req.flush({});
    });

    it('should GET inscriptions with eventId', () => {
      service.getTournamentInscriptions('t1', 'e1').subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/inscriptions?eventId=e1`);
      expect(req.request.method).toBe('GET');
      req.flush({});
    });
  });

  describe('generateDraws', () => {
    it('should POST generate draws', () => {
      service.generateDraws('t1', 'e1').subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/events/e1/generate-draws`);
      expect(req.request.method).toBe('POST');
      req.flush({});
    });
  });

  describe('submitMatchResult', () => {
    it('should POST match result', () => {
      service.submitMatchResult('t1', 'm1', { scoreString: '6-3 6-4', winnerId: 'p1' }).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/matches/m1/result`);
      expect(req.request.method).toBe('POST');
      req.flush({});
    });
  });

  describe('scheduleMatch', () => {
    it('should PATCH match schedule', () => {
      service.scheduleMatch('t1', 'm1', {} as any).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/matches/m1/schedule`);
      expect(req.request.method).toBe('PATCH');
      req.flush({});
    });
  });

  describe('exportTournamentPdf', () => {
    it('should GET PDF export', () => {
      service.exportTournamentPdf('t1').subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/export/pdf`);
      expect(req.request.method).toBe('GET');
      req.flush(new Blob());
    });
  });

  describe('updateParticipantsPoints', () => {
    it('should PATCH participants points', () => {
      service.updateParticipantsPoints('t1', []).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/participants/points`);
      expect(req.request.method).toBe('PATCH');
      req.flush(null);
    });
  });

  describe('getScheduleConfig', () => {
    it('should GET schedule config', () => {
      service.getScheduleConfig('t1').subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/schedule-config`);
      expect(req.request.method).toBe('GET');
      req.flush({});
    });
  });

  describe('saveScheduleConfig', () => {
    it('should PUT schedule config', () => {
      service.saveScheduleConfig('t1', {} as any).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/schedule-config`);
      expect(req.request.method).toBe('PUT');
      req.flush({});
    });
  });

  describe('searchUmpires', () => {
    it('should GET umpires without query', () => {
      service.searchUmpires().subscribe();
      const req = httpMock.expectOne(`${apiUrl}/umpires`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });

    it('should GET umpires with query', () => {
      service.searchUmpires('john').subscribe();
      const req = httpMock.expectOne(`${apiUrl}/umpires?query=john`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  describe('getTournamentUmpires', () => {
    it('should GET tournament umpires', () => {
      service.getTournamentUmpires('t1').subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/umpires`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  describe('addTournamentUmpire', () => {
    it('should POST umpire', () => {
      service.addTournamentUmpire('t1', {} as any).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/umpires`);
      expect(req.request.method).toBe('POST');
      req.flush({});
    });
  });

  describe('removeTournamentUmpire', () => {
    it('should DELETE umpire', () => {
      service.removeTournamentUmpire('t1', 'u1').subscribe();
      const req = httpMock.expectOne(`${apiUrl}/tournaments/t1/umpires/u1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
