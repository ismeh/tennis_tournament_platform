import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
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
  ParticipantPointsUpdateRequest,
  PlayerMatchCalendarResponse,
  ScheduleConfigRequest,
  ScheduleConfigResponse,
  TournamentCalendarFilters,
  TournamentCalendarPageResponse,
  TournamentCalendarResponse,
  TournamentCreateRequest,
  TournamentEventCatalogItem,
  TournamentEventsConfigRequest,
  TournamentGeneralInfoUpdateRequest,
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

  getPublishedTournamentCalendar(filters: TournamentCalendarFilters = {}): Observable<TournamentCalendarPageResponse> {
    return this.http.get<TournamentCalendarPageResponse>(this.calendarTournamentsUrl, {
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

  getMyTournamentCalendar(filters: TournamentCalendarFilters = {}): Observable<TournamentCalendarResponse[]> {
    return this.http.get<TournamentCalendarResponse[]>(this.myTournamentsCalendarUrl, {
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

  getEventCatalogAll(): Observable<TournamentEventCatalogItem[]> {
    return this.http.get<TournamentEventCatalogItem[]>(this.eventCatalogAllUrl);
  }

  saveTournamentEvents(tournamentId: string, payload: TournamentEventsConfigRequest): Observable<TournamentResponse> {
    return this.http.post<TournamentResponse>(`${this.apiUrl}/${tournamentId}/events`, payload);
  }

  updateTournamentStatus(tournamentId: string, payload: TournamentStatusUpdateRequest): Observable<TournamentResponse> {
    return this.http.patch<TournamentResponse>(`${this.apiUrl}/${tournamentId}/status`, payload);
  }

  updateTournamentGeneralInfo(tournamentId: string, payload: TournamentGeneralInfoUpdateRequest): Observable<TournamentResponse> {
    return this.http.put<TournamentResponse>(`${this.apiUrl}/${tournamentId}/general-info`, payload);
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
    payload: { winnerId?: string | null; scoreString: string }
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

  exportTournamentPdf(tournamentId: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${tournamentId}/export/pdf`, {
      responseType: 'blob',
      observe: 'response'
    }).pipe(
      map((response: HttpResponse<Blob>) => response.body as Blob)
    );
  }

  updateParticipantsPoints(tournamentId: string, updates: ParticipantPointsUpdateRequest[]): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${tournamentId}/participants/points`, updates);
  }

  getScheduleConfig(tournamentId: string): Observable<ScheduleConfigResponse> {
    return this.http.get<ScheduleConfigResponse>(`${this.apiUrl}/${tournamentId}/schedule-config`);
  }

  saveScheduleConfig(tournamentId: string, payload: ScheduleConfigRequest): Observable<ScheduleConfigResponse> {
    return this.http.put<ScheduleConfigResponse>(`${this.apiUrl}/${tournamentId}/schedule-config`, payload);
  }

  private get apiUrl(): string {
    return `${AppSettings.API_URL}/tournaments`;
  }

  private get eventCatalogUrl(): string {
    return `${AppSettings.API_URL}/age-categories`;
  }

  private get eventCatalogAllUrl(): string {
    return `${AppSettings.API_URL}/age-categories/all`;
  }

  private get calendarTournamentsUrl(): string {
    return `${AppSettings.API_URL}/calendar/tournaments`;
  }

  private get myMatchesCalendarUrl(): string {
    return `${AppSettings.API_URL}/calendar/my-matches`;
  }

  private get myTournamentsCalendarUrl(): string {
    return `${AppSettings.API_URL}/calendar/my-tournaments`;
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
    if (filters.page !== undefined && filters.page !== null) {
      params = params.set('page', String(filters.page));
    }
    if (filters.size !== undefined && filters.size !== null) {
      params = params.set('size', String(filters.size));
    }

    return params;
  }
}
