import { isPlatformBrowser } from '@angular/common';
import { Injectable, NgZone, PLATFORM_ID, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AppSettings } from '../../shared/constants';
import { TournamentUpdateEvent } from '../interfaces/tournament.model';

@Injectable({
  providedIn: 'root'
})
export class TournamentLiveUpdatesService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly ngZone = inject(NgZone);

  watchTournament(tournamentId: string): Observable<TournamentUpdateEvent> {
    return new Observable<TournamentUpdateEvent>(observer => {
      if (!isPlatformBrowser(this.platformId) || typeof EventSource === 'undefined') {
        observer.complete();
        return undefined;
      }

      const eventSource = new EventSource(this.getUpdatesUrl(tournamentId));
      const handleMatchResultUpdated = (event: MessageEvent<string>) => this.emitEvent(event, observer);
      const handleMatchScheduleUpdated = (event: MessageEvent<string>) => this.emitEvent(event, observer);

      eventSource.addEventListener('match-result-updated', handleMatchResultUpdated);
      eventSource.addEventListener('match-schedule-updated', handleMatchScheduleUpdated);

      return () => {
        eventSource.removeEventListener('match-result-updated', handleMatchResultUpdated);
        eventSource.removeEventListener('match-schedule-updated', handleMatchScheduleUpdated);
        eventSource.close();
      };
    });
  }

  private getUpdatesUrl(tournamentId: string): string {
    return `${AppSettings.API_URL}/tournaments/${encodeURIComponent(tournamentId)}/updates`;
  }

  private emitEvent(event: MessageEvent<string>, observer: { next: (value: TournamentUpdateEvent) => void }): void {
    try {
      const payload = JSON.parse(event.data) as TournamentUpdateEvent;
      this.ngZone.run(() => observer.next(payload));
    } catch {
      return;
    }
  }
}
