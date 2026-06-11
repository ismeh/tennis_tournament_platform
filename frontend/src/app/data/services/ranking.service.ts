import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppSettings } from '../../shared/constants';
import {
  ProfessionalRankingResponse,
  RankingFilters,
  RankingPageResponse,
  RankingTournamentResponse,
  TournamentRankingResponse
} from '../interfaces/ranking.model';

@Injectable({
  providedIn: 'root'
})
export class RankingService {
  private readonly http = inject(HttpClient);

  getProfessionalRanking(filters: RankingFilters = {}): Observable<RankingPageResponse<ProfessionalRankingResponse>> {
    return this.http.get<RankingPageResponse<ProfessionalRankingResponse>>(`${this.apiUrl}/professionals`, {
      params: this.toProfessionalParams(filters)
    });
  }

  getRankingTournaments(): Observable<RankingTournamentResponse[]> {
    return this.http.get<RankingTournamentResponse[]>(`${this.apiUrl}/tournaments`);
  }

  getTournamentRanking(
    tournamentId: string,
    filters: RankingFilters = {}
  ): Observable<RankingPageResponse<TournamentRankingResponse>> {
    return this.http.get<RankingPageResponse<TournamentRankingResponse>>(`${this.apiUrl}/tournaments/${tournamentId}`, {
      params: this.toTournamentParams(filters)
    });
  }

  private get apiUrl(): string {
    return `${AppSettings.API_URL}/rankings`;
  }

  private toProfessionalParams(filters: RankingFilters): HttpParams {
    let params = new HttpParams();

    if (filters.gender) {
      params = params.set('gender', filters.gender);
    }
    if (filters.category?.trim()) {
      params = params.set('category', filters.category.trim());
    }
    params = this.appendPageParams(params, filters);

    return params;
  }

  private toTournamentParams(filters: RankingFilters): HttpParams {
    let params = new HttpParams();

    if (filters.gender) {
      params = params.set('gender', filters.gender);
    }
    if (filters.categoryId) {
      params = params.set('categoryId', filters.categoryId);
    }
    params = this.appendPageParams(params, filters);

    return params;
  }

  private appendPageParams(params: HttpParams, filters: RankingFilters): HttpParams {
    if (filters.page != null) {
      params = params.set('page', filters.page);
    }
    if (filters.size != null) {
      params = params.set('size', filters.size);
    }
    if (filters.sortBy?.trim()) {
      params = params.set('sortBy', filters.sortBy.trim());
    }
    if (filters.sortDirection) {
      params = params.set('sortDirection', filters.sortDirection);
    }

    return params;
  }
}
