import { TestBed } from '@angular/core/testing';
import { OfflineStorageService } from '../../core/offline/offline-storage.service';
import { TournamentInscriptionsResponse, TournamentResponse } from '../interfaces/tournament.model';
import { TournamentOfflineService } from './tournament-offline.service';

describe('TournamentOfflineService', () => {
  let service: TournamentOfflineService;
  let storage: OfflineStorageService;

  const tournament = { id: 'tournament-1', formalName: 'Open de prueba' } as TournamentResponse;
  const inscriptions = { players: [] } as unknown as TournamentInscriptionsResponse;

  beforeEach(async () => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TournamentOfflineService);
    storage = TestBed.inject(OfflineStorageService);
    await storage.clear();
  });

  afterEach(async () => {
    await storage.clear();
  });

  it('should persist and restore the tournament snapshot', async () => {
    await service.saveTournament(tournament);

    const record = await service.readTournament('tournament-1');

    expect(record?.data.formalName).toBe('Open de prueba');
    expect(record?.cachedAt).toBeTruthy();
  });

  it('should keep separate inscription snapshots per event', async () => {
    await service.saveInscriptions('tournament-1', undefined, inscriptions);
    await service.saveInscriptions('tournament-1', 'event-1', inscriptions);

    expect(await service.readInscriptions('tournament-1', undefined)).not.toBeNull();
    expect(await service.readInscriptions('tournament-1', 'event-1')).not.toBeNull();
    expect(await service.readInscriptions('tournament-1', 'event-2')).toBeNull();
  });
});
