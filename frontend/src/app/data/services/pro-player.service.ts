import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppSettings } from '../../shared/constants';
import { ProPlayerSearchResponse } from '../interfaces/pro-player.model';

export interface ProPlayerSearchFilters {
  gender?: string | null;
  category?: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class ProPlayerService {
  private readonly http = inject(HttpClient);

  searchProPlayers(query: string, filters: ProPlayerSearchFilters = {}): Observable<ProPlayerSearchResponse[]> {
    const params = new URLSearchParams();
    const normalizedQuery = query.trim();
    const normalizedGender = filters.gender?.trim();
    const normalizedCategory = filters.category?.trim();

    if (normalizedQuery) {
      params.set('query', normalizedQuery);
    }

    if (normalizedGender) {
      params.set('gender', normalizedGender);
    }

    if (normalizedCategory) {
      params.set('category', normalizedCategory);
    }

    const serializedParams = params.toString();
    const queryParam = serializedParams ? `?${serializedParams}` : '';

    return this.http.get<ProPlayerSearchResponse[]>(`${this.apiUrl}${queryParam}`);
  }

  private get apiUrl(): string {
    return `${AppSettings.API_URL}/pro-players`;
  }
}
