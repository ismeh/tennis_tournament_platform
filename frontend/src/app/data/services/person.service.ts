import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppSettings } from '../../shared/constants';
import { PersonSearchResponse } from '../interfaces/person.model';

@Injectable({
  providedIn: 'root'
})
export class PersonService {
  private readonly http = inject(HttpClient);

  searchPersons(query: string): Observable<PersonSearchResponse[]> {
    const normalizedQuery = query.trim();
    const queryParam = normalizedQuery ? `?query=${encodeURIComponent(normalizedQuery)}` : '';

    return this.http.get<PersonSearchResponse[]>(`${this.apiUrl}${queryParam}`);
  }

  private get apiUrl(): string {
    return `${AppSettings.API_URL}/persons`;
  }
}
