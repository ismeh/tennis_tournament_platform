import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppSettings } from '../../shared/constants';
import { TournamentCreateRequest, TournamentResponse } from '../interfaces/tournament.model';

@Injectable({
  providedIn: 'root'
})
export class TournamentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${AppSettings.API_URL}/tournaments`;

  createTournament(payload: TournamentCreateRequest): Observable<TournamentResponse> {
    return this.http.post<TournamentResponse>(this.apiUrl, payload);
  }
}