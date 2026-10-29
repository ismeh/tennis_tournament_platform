import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppSettings } from '../../shared/constants';
import { ClubResponse } from '../interfaces/club.model';

@Injectable({
  providedIn: 'root'
})
export class ClubService {
  private readonly http = inject(HttpClient);

  searchClubs(query: string): Observable<ClubResponse[]> {
    return this.http.get<ClubResponse[]>(`${AppSettings.API_URL}/clubs`, {
      params: { q: query }
    });
  }
}
