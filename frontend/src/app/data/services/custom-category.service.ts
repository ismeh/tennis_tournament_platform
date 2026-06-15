import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppSettings } from '../../shared/constants';
import { TournamentEventCatalogItem } from '../interfaces/tournament.model';

@Injectable({
  providedIn: 'root'
})
export class CustomCategoryService {
  private readonly http = inject(HttpClient);

  getMyCategories(): Observable<TournamentEventCatalogItem[]> {
    return this.http.get<TournamentEventCatalogItem[]>(this.apiUrl);
  }

  createCategory(name: string): Observable<TournamentEventCatalogItem> {
    return this.http.post<TournamentEventCatalogItem>(this.apiUrl, { name });
  }

  updateCategory(id: number, name: string): Observable<TournamentEventCatalogItem> {
    return this.http.put<TournamentEventCatalogItem>(`${this.apiUrl}/${id}`, { name });
  }

  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  private get apiUrl(): string {
    return `${AppSettings.API_URL}/custom-age-categories`;
  }
}
