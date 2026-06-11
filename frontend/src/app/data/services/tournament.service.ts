import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppSettings } from '../../shared/constants';
import {
  CourtCreateRequest,
  CourtResponse,
  CourtUpdateRequest,
  EventInscriptionRequest,
  EventInscriptionResponse,
  ManualEventInscriptionRequest,
  MatchScheduleRequest,
  MatchResponse,
  PlayerMatchCalendarResponse,
  TournamentCalendarFilters,
  TournamentCalendarResponse,
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

  getTournaments(): Observable<TournamentResponse[]> {
    return this.http.get<TournamentResponse[]>(this.apiUrl);
  }

  getPublishedTournamentCalendar(filters: TournamentCalendarFilters = {}): Observable<TournamentCalendarResponse[]> {
    return this.http.get<TournamentCalendarResponse[]>(this.calendarTournamentsUrl, {
      params: this.toCalendarParams(filters)
    });
  }

  getMyMatchCalendar(filters: TournamentCalendarFilters = {}): Observable<PlayerMatchCalendarResponse[]> {
    return this.http.get<PlayerMatchCalendarResponse[]>(this.myMatchesCalendarUrl, {
      params: this.toCalendarParams({
        from: filters.from,
        to: filters.to
      })
    });
  }

  getTournamentById(id: string): Observable<TournamentResponse> {
    return this.http.get<TournamentResponse>(`${this.apiUrl}/${id}`);
  }

  createTournament(payload: TournamentCreateRequest): Observable<TournamentResponse> {
    return this.http.post<TournamentResponse>(this.apiUrl, payload);
  }

  getCourts(tournamentId: string): Observable<CourtResponse[]> {
    return this.http.get<CourtResponse[]>(`${this.apiUrl}/${tournamentId}/courts`);
  }

  createCourt(tournamentId: string, payload: CourtCreateRequest): Observable<CourtResponse> {
    return this.http.post<CourtResponse>(`${this.apiUrl}/${tournamentId}/courts`, payload);
  }

  updateCourt(tournamentId: string, courtId: string, payload: CourtUpdateRequest): Observable<CourtResponse> {
    return this.http.patch<CourtResponse>(`${this.apiUrl}/${tournamentId}/courts/${courtId}`, payload);
  }

  deleteCourt(tournamentId: string, courtId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${tournamentId}/courts/${courtId}`);
  }

  getEventCatalog(): Observable<TournamentEventCatalogItem[]> {
    return this.http.get<TournamentEventCatalogItem[]>(this.eventCatalogUrl);
  }

  saveTournamentEvents(tournamentId: string, payload: TournamentEventsConfigRequest): Observable<TournamentResponse> {
    return this.http.post<TournamentResponse>(`${this.apiUrl}/${tournamentId}/events`, payload);
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

  generateDraws(tournamentId: string, eventId: string): Observable<TournamentResponse> {
    return this.http.post<TournamentResponse>(
      `${this.apiUrl}/${tournamentId}/events/${eventId}/generate-draws`,
      {}
    );
  }

  submitMatchResult(
    tournamentId: string,
    matchId: string,
    payload: { winnerId: string; scoreString: string }
  ): Observable<MatchResponse> {
    return this.http.post<MatchResponse>(`${this.apiUrl}/${tournamentId}/matches/${matchId}/result`, payload);
  }

  scheduleMatch(
    tournamentId: string,
    matchId: string,
    payload: MatchScheduleRequest
  ): Observable<MatchResponse> {
    return this.http.patch<MatchResponse>(`${this.apiUrl}/${tournamentId}/matches/${matchId}/schedule`, payload);
  }

  private get apiUrl(): string {
    return `${AppSettings.API_URL}/tournaments`;
  }

  private get eventCatalogUrl(): string {
    return `${AppSettings.API_URL}/age-categories`;
  }

  private get calendarTournamentsUrl(): string {
    return `${AppSettings.API_URL}/calendar/tournaments`;
  }

  private get myMatchesCalendarUrl(): string {
    return `${AppSettings.API_URL}/calendar/my-matches`;
  }

  private toCalendarParams(filters: TournamentCalendarFilters): HttpParams {
    let params = new HttpParams();

    if (filters.from) {
      params = params.set('from', filters.from);
    }
    if (filters.to) {
      params = params.set('to', filters.to);
    }
    if (filters.surface) {
      params = params.set('surface', filters.surface);
    }
    if (filters.location?.trim()) {
      params = params.set('location', filters.location.trim());
    }
    if (filters.name?.trim()) {
      params = params.set('name', filters.name.trim());
    }
    if (filters.professionalTournament !== null && filters.professionalTournament !== undefined) {
      params = params.set('professionalTournament', String(filters.professionalTournament));
    }
    if (filters.status) {
      params = params.set('status', filters.status);
    }

    return params;
  }
}
