import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppSettings } from '../../shared/constants';
import { ProPlayerSearchResponse } from '../interfaces/pro-player.model';

@Injectable({
  providedIn: 'root'
})
export class ProPlayerService {
  private readonly http = inject(HttpClient);

  searchProPlayers(query: string): Observable<ProPlayerSearchResponse[]> {
    const normalizedQuery = query.trim();
    const queryParam = normalizedQuery ? `?query=${encodeURIComponent(normalizedQuery)}` : '';

    return this.http.get<ProPlayerSearchResponse[]>(`${this.apiUrl}${queryParam}`);
  }

  private get apiUrl(): string {
    return `${AppSettings.API_URL}/pro-players`;
  }
}
