import { Injectable, inject } from '@angular/core';
import { OfflineRecord, OfflineStorageService } from '../../core/offline/offline-storage.service';
import { TournamentInscriptionsResponse, TournamentResponse } from '../interfaces/tournament.model';

/**
 * Persists read-only tournament snapshots so the detail view can be opened without network.
 *
 * Writes are keyed by tournament id; inscriptions are keyed by tournament and event so the
 * default list and each event filter keep their own last known version.
 */
@Injectable({
  providedIn: 'root',
})
export class TournamentOfflineService {
  private static readonly TOURNAMENT_PREFIX = 'tournament:';
  private static readonly INSCRIPTIONS_PREFIX = 'inscriptions:';
  private static readonly ALL_EVENTS = 'all';

  private readonly storage = inject(OfflineStorageService);

  saveTournament(tournament: TournamentResponse): Promise<void> {
    return this.storage.write(this.tournamentKey(tournament.id), tournament);
  }

  readTournament(tournamentId: string): Promise<OfflineRecord<TournamentResponse> | null> {
    return this.storage.read<TournamentResponse>(this.tournamentKey(tournamentId));
  }

  saveInscriptions(
    tournamentId: string,
    eventId: string | undefined,
    inscriptions: TournamentInscriptionsResponse,
  ): Promise<void> {
    return this.storage.write(this.inscriptionsKey(tournamentId, eventId), inscriptions);
  }

  readInscriptions(
    tournamentId: string,
    eventId: string | undefined,
  ): Promise<OfflineRecord<TournamentInscriptionsResponse> | null> {
    return this.storage.read<TournamentInscriptionsResponse>(
      this.inscriptionsKey(tournamentId, eventId),
    );
  }

  private tournamentKey(tournamentId: string): string {
    return `${TournamentOfflineService.TOURNAMENT_PREFIX}${tournamentId}`;
  }

  private inscriptionsKey(tournamentId: string, eventId: string | undefined): string {
    return `${TournamentOfflineService.INSCRIPTIONS_PREFIX}${tournamentId}:${eventId ?? TournamentOfflineService.ALL_EVENTS}`;
  }
}
