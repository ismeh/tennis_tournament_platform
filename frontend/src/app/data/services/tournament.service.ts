import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppSettings } from '../../shared/constants';
import {
  EventInscriptionRequest,
  EventInscriptionResponse,
  ManualEventInscriptionRequest,
  TournamentCreateRequest,
  TournamentEventCatalogItem,
  TournamentEventsConfigRequest,
  TournamentInscriptionsResponse,
  TournamentStatusUpdateRequest,
  TournamentResponse
} from '../interfaces/tournament.model';

@Injectable({
  providedIn: 'root'
})
export class TournamentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${AppSettings.API_URL}/tournaments`;
  private readonly eventCatalogUrl = `${AppSettings.API_URL}/age-categories`;

  getTournaments(): Observable<TournamentResponse[]> {
    return this.http.get<TournamentResponse[]>(this.apiUrl);
  }

  getTournamentById(id: string): Observable<TournamentResponse> {
    return this.http.get<TournamentResponse>(`${this.apiUrl}/${id}`);
  }

  createTournament(payload: TournamentCreateRequest): Observable<TournamentResponse> {
    return this.http.post<TournamentResponse>(this.apiUrl, payload);
  }

  getEventCatalog(): Observable<TournamentEventCatalogItem[]> {
    return this.http.get<TournamentEventCatalogItem[]>(this.eventCatalogUrl);
  }

  saveTournamentEvents(tournamentId: string, payload: TournamentEventsConfigRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${tournamentId}/events`, payload);
  }

  updateTournamentStatus(tournamentId: string, payload: TournamentStatusUpdateRequest): Observable<TournamentResponse> {
    return this.http.patch<TournamentResponse>(`${this.apiUrl}/${tournamentId}/status`, payload);
  }

  requestInscription(tournamentId: string, eventId: string, payload: EventInscriptionRequest): Observable<EventInscriptionResponse> {
    return this.http.post<EventInscriptionResponse>(`${this.apiUrl}/${tournamentId}/events/${eventId}/inscriptions`, payload);
  }

  addManualInscription(tournamentId: string, eventId: string, payload: ManualEventInscriptionRequest): Observable<EventInscriptionResponse> {
    return this.http.post<EventInscriptionResponse>(`${this.apiUrl}/${tournamentId}/events/${eventId}/manual-inscriptions`, payload);
  }

  getTournamentInscriptions(tournamentId: string, eventId?: string): Observable<TournamentInscriptionsResponse> {
    const query = eventId ? `?eventId=${encodeURIComponent(eventId)}` : '';
    return this.http.get<TournamentInscriptionsResponse>(`${this.apiUrl}/${tournamentId}/inscriptions${query}`);
  }
}
